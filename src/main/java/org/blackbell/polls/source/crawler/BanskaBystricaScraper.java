package org.blackbell.polls.source.crawler;

import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.ClubFunction;
import org.blackbell.polls.domain.model.relate.ClubMember;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.blackbell.polls.common.PollsUtils.*;

/**
 * Web scraper for banskabystrica.sk - Banská Bystrica city council members.
 *
 * Members page uses clean BEM-style CSS classes (ShortPerson).
 * Use ?per_page=72 to get all 31 members on a single page.
 *
 * Parses: name, photo, email, phone, electoral district, club.
 * No party affiliation available on this page.
 *
 * Phase 1: Members only. Meetings and voting data (eGov PDF) deferred.
 */
public class BanskaBystricaScraper {

    private static final Logger log = LoggerFactory.getLogger(BanskaBystricaScraper.class);

    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";

    private static final Pattern CLUB_ROLE_PATTERN = Pattern.compile(
            "(.+?)\\s*-\\s*(predseda|podpredseda|podpredsedníčka|predsedníčka)\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final String membersUrl;
    private final int timeoutMs;

    private final Map<String, Club> clubsMap = new HashMap<>();

    public BanskaBystricaScraper(String membersUrl, int timeoutMs) {
        this.membersUrl = membersUrl;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Scrape council members from the BB members page.
     *
     * HTML structure (BEM classes):
     * div.ShortPerson
     *   div.ShortPerson__image img (photo, check data-lazy-src)
     *   h3.ShortPerson__name (full name)
     *   div.ShortPerson__position (district + club separated by br)
     *   li.ShortPerson__email a[href^=mailto:] (email)
     *   li.ShortPerson__phone a[href^=tel:] (phone)
     */
    public List<CouncilMember> scrapeMembers(Town town, Season season, Institution institution) {
        log.info("Scraping Banská Bystrica council members from {}", membersUrl);
        List<CouncilMember> members = new ArrayList<>();
        clubsMap.clear();

        try {
            Document doc = fetchDocument(membersUrl);

            Elements memberCards = doc.select("div.ShortPerson");
            log.info("Found {} member cards", memberCards.size());

            for (Element card : memberCards) {
                try {
                    CouncilMember member = parseMemberCard(card, town, season, institution);
                    if (member != null) {
                        members.add(member);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing BB member card: {}", e.getMessage());
                }
            }

            log.info("Scraped {} Banská Bystrica council members", members.size());
        } catch (IOException e) {
            log.error("Failed to scrape BB members: {}", e.getMessage());
        }

        return members;
    }

    private CouncilMember parseMemberCard(Element card, Town town, Season season, Institution institution) {
        // Name
        Element nameEl = card.selectFirst("h3.ShortPerson__name");
        if (nameEl == null) return null;
        String fullName = nameEl.text().trim();
        if (fullName.isEmpty()) return null;

        // Photo (check data-lazy-src first, then src)
        String photo = null;
        Element imgEl = card.selectFirst("div.ShortPerson__image img");
        if (imgEl != null) {
            photo = imgEl.attr("data-lazy-src");
            if (photo == null || photo.isEmpty()) {
                photo = imgEl.attr("src");
            }
            // Skip SVG placeholder
            if (photo != null && photo.contains("data:image")) {
                // Try noscript fallback
                Element noscript = card.selectFirst("noscript img");
                if (noscript != null) {
                    photo = noscript.attr("src");
                }
            }
        }

        // Email
        String email = null;
        Element emailLink = card.selectFirst("li.ShortPerson__email a[href^=mailto:]");
        if (emailLink != null) {
            email = emailLink.attr("href").replace("mailto:", "");
        }

        // Phone
        String phone = null;
        Element phoneLink = card.selectFirst("li.ShortPerson__phone a[href^=tel:]");
        if (phoneLink != null) {
            phone = phoneLink.text().trim();
        }

        // Position: district + club
        String district = null;
        String clubText = null;
        Element positionEl = card.selectFirst("div.ShortPerson__position");
        if (positionEl != null) {
            String[] parts = positionEl.html().split("<br\\s*/?>");
            for (String part : parts) {
                String text = Jsoup.parse(part.trim()).text().trim();
                if (text.contains("Volebný obvod")) {
                    district = text;
                } else if (text.startsWith("Klub:")) {
                    clubText = text.substring("Klub:".length()).trim();
                }
            }
        }

        // Create Politician
        Politician politician = new Politician();
        politician.setRef(generateUniqueKeyReference());
        politician.setName(toSimpleName(fullName));
        politician.setTitles(getTitles(fullName));
        politician.setPicture(photo);
        politician.setPhone(phone);
        politician.setEmail(email);

        // Create CouncilMember
        CouncilMember member = new CouncilMember();
        member.setRef(generateUniqueKeyReference());
        member.setPolitician(politician);
        member.setSeason(season);
        member.setTown(town);
        member.setInstitution(institution);
        if (district != null) {
            member.setDescription(district);
        }

        // Add club membership
        if (clubText != null && !clubText.isEmpty()
                && !clubText.equalsIgnoreCase("bez klubovej príslušnosti")
                && !clubText.equalsIgnoreCase("bez klubovej prislusnosti")) {
            parseAndAddClub(member, clubText, town, season);
        }

        log.debug("Parsed BB member: {} (email: {}, club: {}, district: {})",
                deAccent(politician.getName()), email, clubText, district);
        return member;
    }

    /**
     * Parse club text which may include role.
     * Examples:
     * - "Klub nezávislých poslancov"
     * - "HLAS - sociálna demokracia, SME RODINA a nezávislí - predseda"
     * - "Bystrica má na viac - podpredsedníčka"
     */
    private void parseAndAddClub(CouncilMember member, String clubText, Town town, Season season) {
        String clubName = clubText;
        ClubFunction function = ClubFunction.MEMBER;

        Matcher roleMatcher = CLUB_ROLE_PATTERN.matcher(clubText);
        if (roleMatcher.find()) {
            clubName = roleMatcher.group(1).trim();
            function = parseClubFunction(roleMatcher.group(2).toLowerCase());
        }

        Club club = clubsMap.get(clubName);
        if (club == null) {
            club = new Club();
            club.setRef(generateUniqueKeyReference());
            club.setName(clubName);
            club.setTown(town);
            club.setSeason(season);
            clubsMap.put(clubName, club);
        }

        ClubMember clubMember = new ClubMember();
        clubMember.setClub(club);
        clubMember.setCouncilMember(member);
        clubMember.setClubFunction(function);

        club.addClubMember(clubMember);
        member.addClubMember(clubMember);
    }

    private ClubFunction parseClubFunction(String text) {
        if (text.contains("predseda") && !text.contains("podpredseda")) {
            return ClubFunction.CHAIRMAN;
        } else if (text.contains("podpredseda")) {
            return ClubFunction.VICECHAIRMAN;
        }
        return ClubFunction.MEMBER;
    }

    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(timeoutMs)
                .followRedirects(true)
                .get();
    }
}
