package org.blackbell.polls.source.crawler;

import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.ClubFunction;
import org.blackbell.polls.domain.model.relate.ClubMember;
import org.blackbell.polls.domain.model.relate.ClubParty;
import org.blackbell.polls.domain.model.relate.PartyNominee;
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
 * Updated crawler for new presov.sk website structure (2024+).
 * Replaces PresovCouncilMemberCrawler which used old JavaScript-based structure.
 *
 * Parses:
 * - Basic info (name, phone, email, photo)
 * - Party nominations (kandidát)
 * - Club membership and function (predseda/podpredseda/člen)
 * - Commission memberships
 * - Electoral district
 * - Other functions
 */
public class PresovCouncilMemberCrawlerV2 {
    private static final Logger log = LoggerFactory.getLogger(PresovCouncilMemberCrawlerV2.class);

    private static final String PORTAL_BASE_URL = "https://www.presov.sk";

    private final String membersUrl;
    private final int timeoutMs;

    // Patterns for parsing club membership
    private static final Pattern CLUB_MEMBER_PATTERN = Pattern.compile(
            "(predseda|podpredseda|podpredsedníčka|predsedkyňa|člen|členka)\\s+Poslaneckého klubu\\s+(.+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern COMMISSION_PATTERN = Pattern.compile(
            "(predseda|podpredseda|podpredsedníčka|predsedkyňa|člen|členka)\\s+(Komisie|Mestskej rady|Výboru)\\s+(.+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern DISTRICT_PATTERN = Pattern.compile(
            "za volebný obvod č\\.\\s*(\\d+)\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Map of clubs by name for reuse
    private final Map<String, Club> clubsMap = new HashMap<>();

    public PresovCouncilMemberCrawlerV2(String membersUrl, int timeoutMs) {
        this.membersUrl = membersUrl;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Fetches council members from presov.sk website.
     *
     * @param town        the town entity
     * @param institution the institution entity
     * @param season      the season entity
     * @param partiesMap  map of existing parties (will be updated with new ones)
     * @param existingMembersMap map of existing council members by normalized name
     * @return set of new council members
     */
    public Set<CouncilMember> getCouncilMembers(
            Town town,
            Institution institution,
            Season season,
            Map<String, Party> partiesMap,
            Map<String, CouncilMember> existingMembersMap) {

        Set<CouncilMember> members = new HashSet<>();

        try {
            log.info("Fetching council members from: {}", membersUrl);
            Document document = Jsoup.connect(membersUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(timeoutMs)
                    .get();

            Elements personElements = document.select("div.persons-detail-envelope");
            log.info("Found {} person elements", personElements.size());

            for (Element personEl : personElements) {
                try {
                    CouncilMember member = parsePersonElement(personEl, town, institution, season, partiesMap, existingMembersMap);
                    if (member != null) {
                        members.add(member);
                        String keyName = toSimpleNameWithoutAccents(member.getPolitician().getName());
                        existingMembersMap.put(keyName, member);
                    }
                } catch (Exception e) {
                    log.error("Error parsing person element: {}", e.getMessage(), e);
                }
            }

            log.info("Successfully parsed {} new council members", members.size());

        } catch (IOException e) {
            log.error("Failed to fetch council members from {}: {}", membersUrl, e.getMessage());
        }

        return members;
    }

    private CouncilMember parsePersonElement(
            Element personEl,
            Town town,
            Institution institution,
            Season season,
            Map<String, Party> partiesMap,
            Map<String, CouncilMember> existingMembersMap) {

        // Get name from img title or h2
        Element imgEl = personEl.selectFirst("img");
        Element h2El = personEl.selectFirst("h2");

        String name = null;
        String imageUrl = null;

        if (imgEl != null) {
            name = imgEl.attr("title").trim();
            imageUrl = imgEl.attr("src");
            if (imageUrl != null && !imageUrl.startsWith("http")) {
                imageUrl = PORTAL_BASE_URL + imageUrl;
            }
        }

        if ((name == null || name.isEmpty()) && h2El != null) {
            name = h2El.text().trim();
        }

        if (name == null || name.isEmpty()) {
            log.warn("Could not extract name from person element");
            return null;
        }

        if (!looksLikePersonName(name)) {
            log.warn("Skipping non-person entry: '{}'", name);
            return null;
        }

        // Check if already exists
        String keyName = toSimpleNameWithoutAccents(name);
        if (existingMembersMap.containsKey(keyName)) {
            log.debug("Council member '{}' already exists, skipping", name);
            return null;
        }

        log.info("Parsing new council member: {}", name);

        // Get basic contact info from the main card
        String phone = extractFieldValue(personEl, "dd.mobil");
        String email = extractEmail(personEl);
        String candidateInfo = extractFieldValue(personEl, "dd.short-text");

        // Get detail page URL
        Element detailLink = personEl.selectFirst("a.person-detail-trigger");
        String detailUrl = null;
        String personExtId = null;
        if (detailLink != null) {
            String href = detailLink.attr("href");
            if (href != null && !href.isEmpty()) {
                detailUrl = href.startsWith("http") ? href : PORTAL_BASE_URL + href;
                // Extract person ID from URL like /poslanci-msz/mid/498738/ma0/16364/.html
                Matcher idMatcher = Pattern.compile("/ma0/(\\d+)/").matcher(href);
                if (idMatcher.find()) {
                    personExtId = idMatcher.group(1);
                }
            }
        }

        // Create Politician
        Politician politician = new Politician();
        politician.setRef(generateUniqueKeyReference());
        politician.setName(toSimpleName(name));
        politician.setTitles(getTitles(name));
        politician.setPicture(imageUrl);
        politician.setPhone(phone);
        politician.setEmail(email);
        politician.setExtId(personExtId);

        // Create CouncilMember
        CouncilMember member = new CouncilMember();
        member.setRef(generateUniqueKeyReference());
        member.setPolitician(politician);
        member.setSeason(season);
        member.setTown(town);
        member.setInstitution(institution);
        member.setExtId(personExtId);

        // Parse party nominations from candidate info
        if (candidateInfo != null && !candidateInfo.isEmpty()) {
            parsePartyNominations(member, candidateInfo, partiesMap);
        }

        // Parse detail page for additional info (club, commissions, etc.)
        if (detailUrl != null) {
            parseDetailPage(member, detailUrl, partiesMap);
        } else {
            // Try to parse from show-more-target in the same page
            parseInlineDetails(member, personEl, partiesMap);
        }

        log.info("Created council member: {} (email: {}, phone: {}, club: {})",
                deAccent(politician.getName()), email, phone,
                member.getClubMember() != null ? member.getClubMember().getClub().getName() : "none");

        return member;
    }

    /**
     * Parse detail page for additional member info.
     */
    private void parseDetailPage(CouncilMember member, String detailUrl, Map<String, Party> partiesMap) {
        try {
            log.debug("Fetching detail page: {}", detailUrl);
            Document detailDoc = Jsoup.connect(detailUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(timeoutMs)
                    .get();

            // Find the detail section
            Element detailSection = detailDoc.selectFirst("div.show-more-target");
            if (detailSection == null) {
                detailSection = detailDoc.selectFirst("div.persons-detail-envelope");
            }

            if (detailSection != null) {
                parseDetailSection(member, detailSection, partiesMap);
            }

        } catch (IOException e) {
            log.warn("Failed to fetch detail page {}: {}", detailUrl, e.getMessage());
        }
    }

    /**
     * Parse inline details from the same page (show-more-target section).
     */
    private void parseInlineDetails(CouncilMember member, Element personEl, Map<String, Party> partiesMap) {
        Element detailSection = personEl.selectFirst("div.show-more-target");
        if (detailSection != null) {
            parseDetailSection(member, detailSection, partiesMap);
        }
    }

    /**
     * Parse detail section for club membership, commissions, district, etc.
     */
    private void parseDetailSection(CouncilMember member, Element detailSection, Map<String, Party> partiesMap) {
        Elements listItems = detailSection.select("li p, li");
        List<String> otherFunctions = new ArrayList<>();

        for (Element item : listItems) {
            String text = item.text().trim();
            if (text.isEmpty()) continue;

            // Check for club membership
            Matcher clubMatcher = CLUB_MEMBER_PATTERN.matcher(text);
            if (clubMatcher.find()) {
                String functionStr = clubMatcher.group(1).toLowerCase();
                String clubName = clubMatcher.group(2).trim();

                ClubFunction clubFunction = parseClubFunction(functionStr);
                addClubMembership(member, clubName, clubFunction, partiesMap);
                continue;
            }

            // Check for electoral district
            Matcher districtMatcher = DISTRICT_PATTERN.matcher(text);
            if (districtMatcher.find()) {
                String districtNum = districtMatcher.group(1);
                String districtName = districtMatcher.group(2);
                member.setDescription("Volebný obvod č. " + districtNum + " (" + districtName + ")");
                continue;
            }

            // Check for commission/council membership
            Matcher commissionMatcher = COMMISSION_PATTERN.matcher(text);
            if (commissionMatcher.find()) {
                otherFunctions.add(text);
                continue;
            }

            // Skip address line
            if (text.toLowerCase().contains("adresa na doručovanie")) {
                continue;
            }

            // Add other functions
            if (!text.isEmpty() && !text.startsWith("kandidát")) {
                otherFunctions.add(text);
            }
        }

        if (!otherFunctions.isEmpty()) {
            member.setOtherFunctions(String.join(", ", otherFunctions));
        }
    }

    /**
     * Add club membership to the council member.
     */
    private void addClubMembership(CouncilMember member, String clubName, ClubFunction clubFunction, Map<String, Party> partiesMap) {
        // Get or create club
        Club club = clubsMap.get(clubName);
        if (club == null) {
            club = new Club();
            club.setRef(generateUniqueKeyReference());
            club.setName(clubName);
            club.setTown(member.getTown());
            club.setSeason(member.getSeason());

            // Try to extract party names from club name and create ClubParty relationships
            List<String> partyNames = extractPartyNamesFromClubName(clubName);
            for (String partyName : partyNames) {
                Party party = partiesMap.get(partyName);
                if (party == null) {
                    party = new Party();
                    party.setRef(partyName);
                    party.setName(partyName);
                    partiesMap.put(partyName, party);
                    log.info("Created new party from club: {}", partyName);
                }

                ClubParty clubParty = new ClubParty();
                clubParty.setParty(party);
                clubParty.setSeason(member.getSeason());
                clubParty.setClub(club);
                club.addClubParty(clubParty);
            }

            clubsMap.put(clubName, club);
            log.info("Created new club: {}", clubName);
        }

        // Create club member relationship
        ClubMember clubMember = new ClubMember();
        clubMember.setClub(club);
        clubMember.setCouncilMember(member);
        clubMember.setClubFunction(clubFunction);

        club.addClubMember(clubMember);
        member.addClubMember(clubMember);

        log.debug("Added {} as {} of club: {}",
                deAccent(member.getPolitician().getName()), clubFunction, clubName);
    }

    /**
     * Extract party names from club name.
     * E.g., "Nezávislí - Progresívne Slovensko - Demokrati" -> ["Progresívne Slovensko", "Demokrati"]
     */
    private List<String> extractPartyNamesFromClubName(String clubName) {
        List<String> parties = new ArrayList<>();
        // Split by common delimiters
        String[] parts = clubName.split("\\s*[-–,]\\s*");
        for (String part : parts) {
            String trimmed = part.trim();
            // Skip generic words
            if (!trimmed.isEmpty() &&
                !trimmed.equalsIgnoreCase("nezávislí") &&
                !trimmed.equalsIgnoreCase("nezávislý") &&
                !trimmed.equalsIgnoreCase("nezávislá") &&
                !trimmed.equalsIgnoreCase("nezaradení") &&
                !trimmed.equalsIgnoreCase("nezaradený") &&
                !trimmed.equalsIgnoreCase("poslanci") &&
                !trimmed.equalsIgnoreCase("poslanec") &&
                !trimmed.equalsIgnoreCase("a")) {
                parties.add(trimmed);
            }
        }
        return parties;
    }

    /**
     * Parse club function from text.
     */
    private ClubFunction parseClubFunction(String functionStr) {
        if (functionStr.contains("predseda") && !functionStr.contains("podpredseda")) {
            return ClubFunction.CHAIRMAN;
        } else if (functionStr.contains("podpredseda")) {
            return ClubFunction.VICECHAIRMAN;
        } else {
            return ClubFunction.MEMBER;
        }
    }

    private String extractFieldValue(Element parent, String selector) {
        Element el = parent.selectFirst(selector);
        if (el != null) {
            String text = el.text().trim();
            // Remove "Zobraziť podrobnosti" link text if present
            int idx = text.indexOf("Zobraziť");
            if (idx > 0) {
                text = text.substring(0, idx).trim();
            }
            return text.isEmpty() ? null : text;
        }
        return null;
    }

    private String extractEmail(Element parent) {
        Element emailLink = parent.selectFirst("dd.mail a[href^=mailto:]");
        if (emailLink != null) {
            String href = emailLink.attr("href");
            if (href.startsWith("mailto:")) {
                return href.substring(7);
            }
        }
        return null;
    }

    private void parsePartyNominations(CouncilMember member, String candidateInfo, Map<String, Party> partiesMap) {
        // Remove "kandidát:" or "kandidátka:" prefix
        String partyString = candidateInfo
                .replaceFirst("(?i)kandidát(ka)?\\s*:?\\s*", "")
                .trim();

        if (partyString.isEmpty() ||
            partyString.equalsIgnoreCase("nezávislý kandidát") ||
            partyString.equalsIgnoreCase("nezávislá kandidátka")) {
            log.debug("Independent candidate: {}", member.getPolitician().getName());
            return;
        }

        // Split by comma and clean up
        List<String> partyNames = splitCleanAndTrim(partyString);

        for (String partyName : partyNames) {
            if (partyName.isEmpty()) continue;

            // Get or create party
            Party party = partiesMap.get(partyName);
            if (party == null) {
                party = new Party();
                party.setRef(partyName);
                party.setName(partyName);
                partiesMap.put(partyName, party);
                log.info("Created new party: {}", partyName);
            }

            // Create party nominee
            PartyNominee nominee = new PartyNominee();
            nominee.setParty(party);
            nominee.setSeason(member.getSeason());
            nominee.setTown(member.getTown());
            member.getPolitician().addPartyNominee(nominee);

            log.debug("Added party nomination: {} -> {}", deAccent(member.getPolitician().getName()), partyName);
        }
    }

    private boolean looksLikePersonName(String text) {
        if (text == null || text.length() < 5 || text.length() > 80) return false;
        String[] words = text.trim().split("\\s+");
        if (words.length < 2) return false;
        // All-uppercase text is likely an institution name, not a person
        if (text.equals(text.toUpperCase())) return false;
        String lower = text.toLowerCase();
        String[] nonNameWords = {"mesto", "správa", "uznesenie", "dokument", "zmluva",
                "rozpočet", "program", "zápisnica", "hlasovanie", "bezbariérov",
                "výbor", "komisia", "materiál", "príloha", "zoznam", "prešov", "košice"};
        for (String word : nonNameWords) {
            if (lower.contains(word)) return false;
        }
        return true;
    }

    /**
     * Get the map of created clubs (for saving to DB).
     */
    public Map<String, Club> getClubsMap() {
        return clubsMap;
    }
}