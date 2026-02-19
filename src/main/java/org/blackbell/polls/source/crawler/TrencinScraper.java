package org.blackbell.polls.source.crawler;

import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.relate.PartyNominee;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.blackbell.polls.common.PollsUtils.*;

/**
 * Web scraper for trencin.sk - Trenčín city council members.
 *
 * The members page uses inline-styled section elements with role="region".
 * Members are grouped under h2 headings for each electoral district.
 *
 * Parses: name, photo, party, phone, email, electoral district, commissions.
 * No club information available (Trenčín shows VMČ and commissions instead).
 *
 * Phase 1: Members only. Voting data (PDF in zápisnice) deferred.
 */
public class TrencinScraper {

    private static final Logger log = LoggerFactory.getLogger(TrencinScraper.class);

    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";

    private final String membersUrl;
    private final int timeoutMs;

    private final Map<String, Party> partiesMap = new HashMap<>();

    public TrencinScraper(String membersUrl, int timeoutMs) {
        this.membersUrl = membersUrl;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Scrape council members from the Trenčín members page.
     *
     * HTML structure:
     * - h2 elements: "Volebný obvod č. N – mestská časť NAME"
     * - section[role=region]: one per member
     *   - figure img: photo
     *   - div: contains paragraphs:
     *     - p[id$=-heading]: name (bold, 18pt)
     *     - p > strong: party
     *     - p: commissions/VMČ text
     *     - p: tel + email links
     */
    public List<CouncilMember> scrapeMembers(Town town, Season season, Institution institution) {
        log.info("Scraping Trenčín council members from {}", membersUrl);
        List<CouncilMember> members = new ArrayList<>();
        partiesMap.clear();

        try {
            Document doc = fetchDocument(membersUrl);

            // Find content area
            Element contentArea = doc.selectFirst("div.article-content");
            if (contentArea == null) {
                contentArea = doc.selectFirst("main, article, div.content");
            }
            if (contentArea == null) {
                contentArea = doc.body();
            }

            // Walk through children to track current district
            String currentDistrict = null;
            Elements children = contentArea.children();

            for (Element child : children) {
                // Check for district heading
                if (child.tagName().equals("h2")) {
                    String text = child.text().trim();
                    if (text.contains("Volebný obvod")) {
                        currentDistrict = text;
                    }
                    continue;
                }

                // Check for member section
                if (child.tagName().equals("section") && "region".equals(child.attr("role"))) {
                    CouncilMember member = parseMemberSection(child, currentDistrict,
                            town, season, institution);
                    if (member != null) {
                        members.add(member);
                    }
                    continue;
                }

                // Some pages may nest sections in other containers
                Elements sections = child.select("section[role=region]");
                for (Element section : sections) {
                    CouncilMember member = parseMemberSection(section, currentDistrict,
                            town, season, institution);
                    if (member != null) {
                        members.add(member);
                    }
                }
            }

            // If no sections found with this approach, try broader selector
            if (members.isEmpty()) {
                Elements allSections = doc.select("section[role=region]");
                log.info("Fallback: found {} section[role=region] elements", allSections.size());
                for (Element section : allSections) {
                    CouncilMember member = parseMemberSection(section, null,
                            town, season, institution);
                    if (member != null) {
                        members.add(member);
                    }
                }
            }

            log.info("Scraped {} Trenčín council members", members.size());
        } catch (IOException e) {
            log.error("Failed to scrape Trenčín members: {}", e.getMessage());
        }

        return members;
    }

    private CouncilMember parseMemberSection(Element section, String currentDistrict,
                                              Town town, Season season, Institution institution) {
        // Name from p[id] with heading
        Element nameParagraph = section.selectFirst("p[id]");
        if (nameParagraph == null) {
            // Try first bold paragraph
            nameParagraph = section.selectFirst("p[style*=font-weight]");
        }
        if (nameParagraph == null) return null;

        String fullName = nameParagraph.text().trim();
        if (fullName.isEmpty()) return null;

        // Photo
        String photo = null;
        Element imgEl = section.selectFirst("figure img");
        if (imgEl == null) {
            imgEl = section.selectFirst("img");
        }
        if (imgEl != null) {
            photo = imgEl.attr("src");
        }

        // Parse the div with data paragraphs
        Element dataDiv = section.selectFirst("div[style]");
        String partyText = null;
        String commissions = null;
        String phone = null;
        String email = null;

        if (dataDiv != null) {
            Elements paragraphs = dataDiv.select("> p");
            for (int i = 0; i < paragraphs.size(); i++) {
                Element p = paragraphs.get(i);

                // Skip the name paragraph (already parsed)
                if (p.id() != null && !p.id().isEmpty()) continue;

                // Check for email/phone paragraph
                Element emailLink = p.selectFirst("a[href^=mailto:]");
                Element phoneLink = p.selectFirst("a[href^=tel:]");

                if (emailLink != null) {
                    email = emailLink.attr("href").replace("mailto:", "");
                }
                if (phoneLink != null) {
                    phone = phoneLink.text().trim();
                }

                if (emailLink != null || phoneLink != null) continue;

                // Check for party (strong element, typically second paragraph)
                Element strong = p.selectFirst("strong");
                if (strong != null && partyText == null) {
                    String strongText = strong.text().trim();
                    // The party paragraph typically just has strong text
                    if (p.text().trim().equals(strongText)) {
                        partyText = strongText;
                        continue;
                    }
                }

                // Commissions/VMČ text
                String pText = p.text().trim();
                if (!pText.isEmpty() && commissions == null && !pText.equals(fullName)) {
                    commissions = pText;
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
        if (currentDistrict != null) {
            member.setDescription(currentDistrict);
        }
        if (commissions != null) {
            member.setOtherFunctions(commissions);
        }

        // Add party nomination
        if (partyText != null && !partyText.isEmpty()) {
            addPartyNomination(member, partyText);
        }

        log.debug("Parsed Trenčín member: {} (party: {}, email: {}, district: {})",
                deAccent(politician.getName()), partyText, email, currentDistrict);
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

    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(timeoutMs)
                .followRedirects(true)
                .get();
    }
}
