package org.blackbell.polls.source.crawler;

import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.ClubFunction;
import org.blackbell.polls.domain.model.relate.ClubMember;
import org.blackbell.polls.domain.model.relate.PartyNominee;
import org.blackbell.polls.domain.model.enums.Source;
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
 * Web scraper for nitra.sk - Nitra city council.
 *
 * Scrapes council members from the WordPress-based members page.
 * Members are in wp-block-group containers with data in a single &lt;p&gt; element
 * split by &lt;br&gt; tags.
 *
 * Phase 1: Members only. Voting data (PDF) deferred to Phase 2.
 */
public class NitraScraper {

    private static final Logger log = LoggerFactory.getLogger(NitraScraper.class);

    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";

    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?421[\\d\\s]+|0\\d{3}\\s*\\d{3}\\s*\\d{3}");
    private static final Pattern CLUB_ROLE_PATTERN = Pattern.compile(
            "(predseda|podpredseda|podpredsedníčka|predsedníčka|člen|členka)\\s+poslaneckého klubu",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final String membersUrl;
    private final int timeoutMs;

    private final Map<String, Club> clubsMap = new HashMap<>();
    private final Map<String, Party> partiesMap = new HashMap<>();

    public NitraScraper(String membersUrl, int timeoutMs) {
        this.membersUrl = membersUrl;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Scrape council members from the Nitra members page.
     *
     * HTML structure: WordPress Gutenberg blocks.
     * Each member is in a wp-block-group__inner-container with:
     * - figure.wp-block-image img (photo)
     * - p[style*=font-size] containing all data lines separated by br tags:
     *   Line 1: <strong>Name</strong>
     *   Line 2: kandidát/kandidátka: <strong>Party</strong>
     *   Line 3: člen/členka poslaneckého klubu: <strong>Club</strong>
     *   Line 4: phone number (plain text)
     *   Line 5: <a href="mailto:...">email</a>
     *   Line 6: <a href="https://nitra.sk/vmc-N/">VMČ č. N</a>
     */
    public List<CouncilMember> scrapeMembers(Town town, Season season, Institution institution) {
        log.info("Scraping Nitra council members from {}", membersUrl);
        List<CouncilMember> members = new ArrayList<>();
        clubsMap.clear();
        partiesMap.clear();

        try {
            Document doc = fetchDocument(membersUrl);

            // Find member containers
            Elements containers = doc.select("div.wp-block-group__inner-container");
            log.info("Found {} potential member containers", containers.size());

            for (Element container : containers) {
                try {
                    CouncilMember member = parseMemberContainer(container, town, season, institution);
                    if (member != null) {
                        members.add(member);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing Nitra member container: {}", e.getMessage());
                }
            }

            log.info("Scraped {} Nitra council members", members.size());
        } catch (IOException e) {
            log.error("Failed to scrape Nitra members: {}", e.getMessage());
        }

        return members;
    }

    private CouncilMember parseMemberContainer(Element container, Town town, Season season, Institution institution) {
        // Find the data paragraph (small font-size)
        Element dataParagraph = container.selectFirst("p[style*=font-size]");
        if (dataParagraph == null) return null;

        // Split by <br> to get individual lines
        String[] lines = dataParagraph.html().split("<br\\s*/?>");
        if (lines.length < 2) return null;

        // Line 1: Name in <strong>
        String fullName = null;
        Document line1Doc = Jsoup.parseBodyFragment(lines[0].trim());
        Element strongEl = line1Doc.selectFirst("strong");
        if (strongEl != null) {
            fullName = strongEl.text().trim();
        }
        if (fullName == null || fullName.isEmpty()) return null;

        // Photo
        String photo = null;
        Element imgEl = container.selectFirst("figure.wp-block-image img");
        if (imgEl != null) {
            photo = imgEl.attr("src");
        }

        // Parse remaining lines
        String partyText = null;
        String clubName = null;
        ClubFunction clubFunction = ClubFunction.MEMBER;
        String phone = null;
        String email = null;
        String district = null;

        for (int i = 1; i < lines.length; i++) {
            String lineHtml = lines[i].trim();
            if (lineHtml.isEmpty()) continue;

            Document lineDoc = Jsoup.parseBodyFragment(lineHtml);
            String lineText = lineDoc.body().text().trim();

            // Check for email
            Element emailLink = lineDoc.selectFirst("a[href^=mailto:]");
            if (emailLink != null) {
                email = emailLink.attr("href").replace("mailto:", "");
                continue;
            }

            // Check for district link
            Element districtLink = lineDoc.selectFirst("a[href*=/vmc-], a[href*=/vmc]");
            if (districtLink != null) {
                district = districtLink.text().trim();
                continue;
            }

            // Check for party (kandidát/kandidátka:)
            if (lineText.toLowerCase().contains("kandidát")) {
                Element partyStrong = lineDoc.selectFirst("strong");
                if (partyStrong != null) {
                    partyText = partyStrong.text().trim();
                }
                continue;
            }

            // Check for club (poslaneckého klubu:)
            if (lineText.toLowerCase().contains("poslaneckého klubu")) {
                Element clubStrong = lineDoc.selectFirst("strong");
                if (clubStrong != null) {
                    clubName = clubStrong.text().trim();
                }
                Matcher roleMatcher = CLUB_ROLE_PATTERN.matcher(lineText);
                if (roleMatcher.find()) {
                    clubFunction = parseClubFunction(roleMatcher.group(1).toLowerCase());
                }
                continue;
            }

            // Check for phone number
            Matcher phoneMatcher = PHONE_PATTERN.matcher(lineText);
            if (phoneMatcher.find() && phone == null) {
                phone = lineText.trim();
                if (phone.equals("--") || phone.equals("-")) {
                    phone = null;
                }
                continue;
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

        // Add party nomination
        if (partyText != null && !partyText.isEmpty()) {
            addPartyNomination(member, partyText);
        }

        // Add club membership
        if (clubName != null && !clubName.isEmpty()) {
            addClubMembership(member, clubName, clubFunction, town, season);
        }

        log.debug("Parsed Nitra member: {} (party: {}, club: {}, email: {})",
                deAccent(politician.getName()), partyText, clubName, email);
        return member;
    }

    private void addPartyNomination(CouncilMember member, String partyText) {
        if (partyText.toLowerCase().contains("nezávisl")) return;

        List<String> partyNames = splitCleanAndTrim(partyText);
        for (String partyName : partyNames) {
            if (partyName.isEmpty()) continue;

            Party party = partiesMap.get(partyName);
            if (party == null) {
                party = new Party();
                party.setRef(partyName);
                party.setName(partyName);
                partiesMap.put(partyName, party);
            }

            PartyNominee nominee = new PartyNominee();
            nominee.setParty(party);
            nominee.setSeason(member.getSeason());
            nominee.setTown(member.getTown());
            member.getPolitician().addPartyNominee(nominee);
        }
    }

    private void addClubMembership(CouncilMember member, String clubName, ClubFunction function,
                                   Town town, Season season) {
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
