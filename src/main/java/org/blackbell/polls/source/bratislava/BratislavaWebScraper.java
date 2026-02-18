package org.blackbell.polls.source.bratislava;

import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.embeddable.VotesCount;
import org.blackbell.polls.domain.model.enums.ClubFunction;
import org.blackbell.polls.domain.model.enums.VoteChoice;
import org.blackbell.polls.source.Source;
import org.blackbell.polls.domain.model.relate.ClubMember;
import org.blackbell.polls.domain.model.relate.ClubParty;
import org.blackbell.polls.domain.model.relate.PartyNominee;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.blackbell.polls.common.PollsUtils.*;

/**
 * Web scraper for zastupitelstvo.bratislava.sk - Bratislava city council 2022-2026 season.
 *
 * Scrapes:
 * - Council members from the static members page
 * - Meeting list from the paginated sessions table
 * - Agenda items + individual voting data (menovité hlasovanie) from meeting detail pages
 *
 * Voting data trick: The CMS stores tab selection in PHP session. When we fetch
 * a bod (agenda item) page with ?bod-typ-{artId}=hlasovania and follow redirects,
 * the server sets the Hlasovanie tab in session, redirects back, and returns
 * the full voting HTML with all voter names in static HTML.
 */
@Component
public class BratislavaWebScraper {

    private static final Logger log = LoggerFactory.getLogger(BratislavaWebScraper.class);

    private static final String BASE_URL = "https://zastupitelstvo.bratislava.sk";
    private static final String MEMBERS_URL_TEMPLATE = BASE_URL + "/mestske-zastupitelstvo-hlavneho-mesta-sr-bratislavy-%s/";
    private static final String SESSIONS_URL = BASE_URL + "/zasadnutia/";
    private static final int TIMEOUT_MS = 30_000;
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    // "Poslanecký klub: Team Bratislava, PS, SaS - podpredsedníčka"
    private static final Pattern CLUB_PATTERN = Pattern.compile(
            "Poslanecký klub:?\\s*(.+?)\\s*-\\s*(predsed(?:a|níčka)|podpredsed(?:a|níčka)|člen(?:ka)?)\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    // Fallback without function
    private static final Pattern CLUB_NAME_ONLY_PATTERN = Pattern.compile(
            "Poslanecký klub:?\\s+(.+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final Map<String, Club> clubsMap = new HashMap<>();

    // --- Members scraping ---

    /**
     * Scrape council members from the static members page.
     * Fetches detail pages for each member to get email, phone, club, party nominations.
     */
    public List<CouncilMember> scrapeMembers(Town town, Season season, Institution institution) {
        String membersUrl = String.format(MEMBERS_URL_TEMPLATE, season.getRef());
        log.info("Scraping Bratislava {} council members from {}", season.getRef(), membersUrl);
        List<CouncilMember> members = new ArrayList<>();
        Map<String, Party> partiesMap = new HashMap<>();
        clubsMap.clear();

        try {
            Document doc = fetchDocument(membersUrl);
            Elements memberDivs = doc.select("div.team-wrapper div.col-md-6");

            log.info("Found {} member elements", memberDivs.size());
            for (Element memberDiv : memberDivs) {
                try {
                    CouncilMember member = parseMemberElement(memberDiv, town, season, institution, partiesMap);
                    if (member != null) {
                        members.add(member);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing member element: {}", e.getMessage());
                }
            }

            log.info("Scraped {} Bratislava {} council members", members.size(), season.getRef());
        } catch (IOException e) {
            log.error("Failed to fetch Bratislava members page: {}", e.getMessage());
        }

        return members;
    }

    private CouncilMember parseMemberElement(Element memberDiv, Town town, Season season,
                                               Institution institution, Map<String, Party> partiesMap) {
        // Name from h3.title
        Element nameEl = memberDiv.selectFirst("h3.title");
        if (nameEl == null) return null;

        String fullName = nameEl.text().trim();
        if (fullName.isEmpty()) return null;

        // Photo from figure img
        String imageUrl = null;
        Element imgEl = memberDiv.selectFirst("figure img");
        if (imgEl != null) {
            imageUrl = imgEl.attr("src");
            if (imageUrl != null && !imageUrl.startsWith("http")) {
                imageUrl = BASE_URL + imageUrl;
            }
        }

        // Party from p.prislusnost
        String partyInfo = null;
        Element partyEl = memberDiv.selectFirst("p.prislusnost");
        if (partyEl != null) {
            partyInfo = partyEl.text().trim();
        }

        // ExtId and detail URL from detail link
        String extId = null;
        String detailUrl = null;
        Element detailLink = memberDiv.selectFirst("figure a[href]");
        if (detailLink != null) {
            String href = detailLink.attr("href");
            // URL pattern: /212609-sk/antalova-plavuchova-lenka/
            if (href.matches(".*/\\d+-sk/.*")) {
                extId = href.replaceAll(".*/?(\\d+)-sk/.*", "$1");
                detailUrl = href.startsWith("http") ? href : BASE_URL + href;
            }
        }

        // Create Politician
        Politician politician = new Politician();
        politician.setRef(generateUniqueKeyReference());
        politician.setName(toSimpleName(fullName));
        politician.setTitles(getTitles(fullName));
        politician.setPicture(imageUrl);
        politician.setExtId(extId);

        // Create CouncilMember
        CouncilMember member = new CouncilMember();
        member.setRef(generateUniqueKeyReference());
        member.setPolitician(politician);
        member.setSeason(season);
        member.setTown(town);
        member.setInstitution(institution);
        member.setDescription(partyInfo);

        // Fetch detail page for email, phone, club, party nominations
        if (detailUrl != null) {
            parseMemberDetailPage(member, detailUrl, partiesMap);
        }

        log.debug("Parsed Bratislava member: {} (party: {}, email: {}, club: {})",
                deAccent(politician.getName()), partyInfo, politician.getEmail(),
                member.getClubMember() != null ? member.getClubMember().getClub().getName() : "none");
        return member;
    }

    /**
     * Fetch and parse a member's detail page for email, phone, club, party nominations, district.
     */
    private void parseMemberDetailPage(CouncilMember member, String detailUrl, Map<String, Party> partiesMap) {
        try {
            log.info("Fetching member detail page: {}", detailUrl);
            Document doc = fetchDocument(detailUrl);

            Element container = doc.selectFirst(".details");
            if (container == null) {
                container = doc.selectFirst(".field-items");
            }
            if (container == null) {
                container = doc.body();
            }

            if (container != null) {
                parseDetailFields(member, container, partiesMap);
            }
        } catch (IOException e) {
            log.warn("Failed to fetch detail page for {}: {}", deAccent(member.getPolitician().getName()), e.getMessage());
        }
    }

    /**
     * Parse detail fields from the member detail page container.
     * All fields are in a single {@code <p>} separated by {@code <br>} tags.
     * Club info is in a nested {@code <div>} inside the {@code <p>}.
     */
    private void parseDetailFields(CouncilMember member, Element container, Map<String, Party> partiesMap) {
        Politician politician = member.getPolitician();

        // Decode Cloudflare-obfuscated email
        String email = decodeCloudflareEmail(container);
        if (email != null) {
            politician.setEmail(email);
        }

        // All fields are in a single <p> with <br> separators
        Element p = container.selectFirst("p");
        if (p == null) return;

        // Split inner HTML by <br> to get individual field lines
        String[] lines = p.html().split("<br\\s*/?>");
        for (String line : lines) {
            Document lineDoc = Jsoup.parseBodyFragment(line.trim());
            Element strong = lineDoc.selectFirst("strong");
            if (strong == null) continue;

            String label = strong.text().trim();
            String value = lineDoc.body().text().replace(label, "").trim();

            if (label.startsWith("Kandidoval") && !value.isEmpty()) {
                parsePartyNominations(member, value, partiesMap);
            } else if (label.startsWith("Volebný obvod") && !value.isEmpty()) {
                member.setDescription("Volebný obvod: " + value);
            } else if (label.startsWith("Telefón") && !value.isEmpty()) {
                politician.setPhone(value);
            } else if ((label.startsWith("E-mail") || label.startsWith("Email"))
                    && politician.getEmail() == null && !value.isEmpty()) {
                politician.setEmail(value);
            }
        }

        // Club info: HTML has <div> inside <p>, but jsoup auto-closes <p> before <div>,
        // so the club div becomes a sibling of <p> inside the container.
        // Use ownText() to match only the innermost div with direct club text.
        for (Element div : container.select("div")) {
            if (div.ownText().contains("Poslanecký klub")) {
                parseClubInfo(member, div, partiesMap);
                break;
            }
        }
    }

    /**
     * Decode Cloudflare-obfuscated email from data-cfemail attribute.
     * Cloudflare uses XOR encoding: first byte is the key, remaining bytes are XOR'd with it.
     */
    private String decodeCloudflareEmail(Element container) {
        Element cfEmail = container.selectFirst("[data-cfemail]");
        if (cfEmail == null) return null;

        String encoded = cfEmail.attr("data-cfemail");
        if (encoded.isEmpty() || encoded.length() < 4) return null;

        try {
            int key = Integer.parseInt(encoded.substring(0, 2), 16);
            StringBuilder decoded = new StringBuilder();
            for (int i = 2; i < encoded.length(); i += 2) {
                int charCode = Integer.parseInt(encoded.substring(i, i + 2), 16) ^ key;
                decoded.append((char) charCode);
            }
            String email = decoded.toString();
            log.debug("Decoded Cloudflare email: {}", email);
            return email;
        } catch (NumberFormatException e) {
            log.warn("Failed to decode Cloudflare email: {}", encoded);
            return null;
        }
    }

    /**
     * Parse party nominations from "Kandidoval(a) za:" text.
     * Creates Party + PartyNominee entities (pattern from PresovCouncilMemberCrawlerV2).
     */
    private void parsePartyNominations(CouncilMember member, String text, Map<String, Party> partiesMap) {
        String partyString = text.trim();
        if (partyString.isEmpty()) return;

        List<String> partyNames = PollsUtils.splitCleanAndTrim(partyString);
        for (String partyName : partyNames) {
            if (partyName.isEmpty()) continue;

            Party party = partiesMap.get(partyName);
            if (party == null) {
                party = new Party();
                party.setRef(partyName);
                party.setName(partyName);
                partiesMap.put(partyName, party);
                log.info("Created new party: {}", partyName);
            }

            PartyNominee nominee = new PartyNominee();
            nominee.setParty(party);
            nominee.setSeason(member.getSeason());
            nominee.setTown(member.getTown());
            member.getPolitician().addPartyNominee(nominee);

            log.debug("Added party nomination: {} -> {}", deAccent(member.getPolitician().getName()), partyName);
        }
    }

    /**
     * Parse club info from the nested {@code <div>} element.
     * The div contains lines like "Poslanecký klub: Team Bratislava, PS, SaS - podpredsedníčka"
     * separated by {@code <br>} tags.
     */
    private void parseClubInfo(CouncilMember member, Element clubDiv, Map<String, Party> partiesMap) {
        String[] lines = clubDiv.html().split("<br\\s*/?>");
        for (String line : lines) {
            String text = Jsoup.parse(line.trim()).text().trim();
            if (!text.contains("Poslanecký klub")) continue;

            Matcher clubMatcher = CLUB_PATTERN.matcher(text);
            if (clubMatcher.find()) {
                String clubName = clubMatcher.group(1).trim();
                ClubFunction clubFunction = parseClubFunction(clubMatcher.group(2));
                addClubMembership(member, clubName, clubFunction, partiesMap);
                return;
            }

            // Fallback: club name without function
            Matcher nameOnlyMatcher = CLUB_NAME_ONLY_PATTERN.matcher(text);
            if (nameOnlyMatcher.find()) {
                String clubName = nameOnlyMatcher.group(1).trim();
                if (!clubName.isEmpty()) {
                    addClubMembership(member, clubName, ClubFunction.MEMBER, partiesMap);
                }
                return;
            }
        }
    }

    /**
     * Add club membership to the council member (pattern from PresovCouncilMemberCrawlerV2).
     */
    private void addClubMembership(CouncilMember member, String clubName, ClubFunction clubFunction,
                                   Map<String, Party> partiesMap) {
        Club club = clubsMap.get(clubName);
        if (club == null) {
            club = new Club();
            club.setRef(generateUniqueKeyReference());
            club.setName(clubName);
            club.setTown(member.getTown());
            club.setSeason(member.getSeason());

            // Extract party names from club name and create ClubParty relationships
            String[] parts = clubName.split("\\s*,\\s*");
            for (String part : parts) {
                String partyName = part.trim();
                if (partyName.isEmpty()) continue;

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

        ClubMember clubMember = new ClubMember();
        clubMember.setClub(club);
        clubMember.setCouncilMember(member);
        clubMember.setClubFunction(clubFunction);

        club.addClubMember(clubMember);
        member.addClubMember(clubMember);

        log.debug("Added {} as {} of club: {}",
                deAccent(member.getPolitician().getName()), clubFunction, clubName);
    }

    private ClubFunction parseClubFunction(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("podpredsed")) {
            return ClubFunction.VICECHAIRMAN;
        } else if (lower.contains("predsed")) {
            return ClubFunction.CHAIRMAN;
        }
        return ClubFunction.MEMBER;
    }

    // --- Meeting list scraping ---

    /**
     * Scrape MZ meeting list from the paginated sessions table.
     * Only returns meetings for "Mestské zastupiteľstvo" (filters out commissions and city council board).
     */
    public List<Meeting> scrapeMeetingList(Town town, Season season, Institution institution) {
        String seasonRef = season.getRef();
        String seasonSpaced = seasonRef.replace("-", " - "); // "2022-2026" → "2022 - 2026"
        String organParam = "Mestské zastupiteľstvo hlavného mesta SR Bratislavy (" + seasonSpaced + ")";
        String slugPrefix = "mestske-zastupitelstvo-hlavneho-mesta-sr-bratislavy-" + seasonRef + "-zasadnutie";

        log.info("Scraping Bratislava {} meeting list (organ={})", seasonRef, organParam);
        List<Meeting> meetings = new ArrayList<>();

        try {
            for (int page = 0; page <= 20; page++) { // safety limit
                String url = SESSIONS_URL + "?organ=" + urlEncode(organParam) + "&page=" + page;
                log.debug("Fetching sessions page {}: {}", page, url);

                Document doc = fetchDocument(url);
                Elements rows = doc.select("table.table-striped tbody tr");

                if (rows.isEmpty()) {
                    log.debug("No more sessions on page {}", page);
                    break;
                }

                for (Element row : rows) {
                    Elements cells = row.select("td");
                    if (cells.size() < 2) continue;

                    Element nameLink = cells.get(1).selectFirst("a");
                    if (nameLink == null) continue;

                    String meetingName = nameLink.text().trim();
                    String meetingUrl = nameLink.attr("href");

                    // Filter for MZ meetings of this season only
                    if (!meetingUrl.contains(slugPrefix)) {
                        continue;
                    }

                    // Parse date
                    String dateText = cells.get(0).text().trim();
                    // Handle date range like "10.02.2026 - 13.02.2026" → take first date
                    if (dateText.contains(" - ")) {
                        dateText = dateText.split(" - ")[0].trim();
                    }

                    Date date;
                    try {
                        synchronized (DATE_FORMAT) {
                            date = DATE_FORMAT.parse(dateText);
                        }
                    } catch (ParseException e) {
                        log.warn("Could not parse date '{}' for meeting '{}'", dateText, meetingName);
                        continue;
                    }

                    // Extract slug from URL
                    String slug = extractSlugFromUrl(meetingUrl);

                    Meeting meeting = new Meeting();
                    meeting.setName(meetingName + " - Zasadnutie " + dateText);
                    meeting.setExtId("ba-web:" + slug);
                    meeting.setDate(date);
                    meeting.setRef(generateUniqueKeyReference());
                    meeting.setTown(town);
                    meeting.setSeason(season);
                    meeting.setInstitution(institution);

                    meetings.add(meeting);
                    log.debug("Found MZ meeting: {} ({})", meetingName, dateText);
                }

                // Check if there are more pages
                Element nextPage = doc.selectFirst("ul.pagination li.page-item a[aria-label=Nasledujuca-strana]");
                if (nextPage == null) {
                    break;
                }
            }

            log.info("Scraped {} Bratislava MZ meetings for {}", meetings.size(), seasonRef);
        } catch (IOException e) {
            log.error("Failed to scrape Bratislava meeting list: {}", e.getMessage());
        }

        return meetings;
    }

    // --- Meeting details scraping ---

    /**
     * Scrape meeting details: agenda items and voting data from the meeting detail page.
     * For each agenda item that has a bod-typ ID, fetches the Hlasovanie tab to get
     * individual (menovité) voting data.
     */
    public void scrapeMeetingDetails(Meeting meeting) {
        String slug = meeting.getExtId().replace("ba-web:", "");
        String meetingUrl = BASE_URL + "/" + slug + "/";
        log.info("Scraping meeting details from: {}", meetingUrl);

        try {
            Document doc = fetchDocument(meetingUrl);

            // Parse agenda items from the program list
            Elements programItems = doc.select("ol.program-list > li");
            log.info("Found {} agenda items for meeting '{}'", programItems.size(), meeting.getName());

            for (Element item : programItems) {
                try {
                    AgendaItem agendaItem = parseAgendaItem(item);
                    if (agendaItem != null) {
                        meeting.addAgendaItem(agendaItem);

                        // Scrape voting data for this agenda item
                        String bodUrl = extractBodUrl(item);
                        String bodTypId = agendaItem.getExtId();
                        if (bodUrl != null && bodTypId != null) {
                            scrapeAgendaItemVoting(agendaItem, bodUrl, bodTypId);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error parsing agenda item: {}", e.getMessage());
                }
            }

            int agendaCount = meeting.getAgendaItems() != null ? meeting.getAgendaItems().size() : 0;
            int pollCount = meeting.getAgendaItems() != null
                    ? meeting.getAgendaItems().stream()
                        .mapToInt(ai -> ai.getPolls() != null ? ai.getPolls().size() : 0).sum()
                    : 0;
            log.info("Loaded {} agenda items with {} polls for Bratislava meeting '{}'",
                    agendaCount, pollCount, meeting.getName());

        } catch (IOException e) {
            log.error("Failed to scrape meeting details from {}: {}", meetingUrl, e.getMessage());
            meeting.setSyncError("Failed to scrape: " + e.getMessage());
        }
    }

    private AgendaItem parseAgendaItem(Element itemEl) {
        // Get item number and title
        Element pointEl = itemEl.selectFirst("span.item-point");
        Element titleEl = itemEl.selectFirst("a.item-title");

        String itemNumber = pointEl != null ? pointEl.text().replace(".", "").trim() : "";
        String title = titleEl != null ? titleEl.text().trim() : "";

        if (title.isEmpty()) {
            // Try h3 directly
            Element h3 = itemEl.selectFirst("h3");
            if (h3 != null) {
                title = h3.text().trim();
                // Remove the item-point text from the beginning
                if (!itemNumber.isEmpty() && title.startsWith(itemNumber + ".")) {
                    title = title.substring(itemNumber.length() + 1).trim();
                }
            }
        }

        if (title.isEmpty()) return null;

        String agendaName = itemNumber.isEmpty() ? title : itemNumber + ". " + title;

        // Extract bod-typ ID from any link containing bod-typ- pattern
        String bodTypId = extractBodTypId(itemEl);

        AgendaItem agendaItem = new AgendaItem();
        agendaItem.setName(agendaName);
        agendaItem.setRef(generateUniqueKeyReference());
        agendaItem.setExtId(bodTypId);

        return agendaItem;
    }

    /**
     * Extract the bod-typ-{artId} from any link in the agenda item element.
     * Found in uznesenia links, e.g.: ?bod-typ-343554=uznesenia
     */
    private String extractBodTypId(Element itemEl) {
        Elements links = itemEl.select("a[href*=bod-typ-]");
        for (Element link : links) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("bod-typ-(\\d+)").matcher(link.attr("href"));
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    /**
     * Extract the bod page URL slug from the item-title link.
     * e.g. /mestske-zastupitelstvo-...-zasadnutie-11122025/bod-2/
     */
    private String extractBodUrl(Element itemEl) {
        Element titleLink = itemEl.selectFirst("a.item-title");
        if (titleLink != null) {
            String href = titleLink.attr("href");
            if (!href.isEmpty()) {
                return href.startsWith("http") ? href : BASE_URL + href;
            }
        }
        return null;
    }

    // --- Voting data scraping ---

    /**
     * Scrape voting data for a single agenda item.
     * Fetches the Hlasovanie tab by requesting ?bod-typ-{artId}=hlasovania with
     * follow redirects. The CMS stores the tab selection in PHP session, redirects,
     * and returns full voting HTML.
     */
    private void scrapeAgendaItemVoting(AgendaItem agendaItem, String bodUrl, String bodTypId) {
        String votingUrl = bodUrl + (bodUrl.endsWith("/") ? "" : "/")
                + "?bod-typ-" + bodTypId + "=hlasovania";
        log.debug("Fetching voting data from: {}", votingUrl);

        try {
            // Must use cookie jar so the session tab selection persists across the redirect
            Connection.Response response = Jsoup.connect(votingUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .execute();
            Document doc = response.parse();

            // Find all voting panels: div.panel.hlasovania contains the voting panels
            Elements votingPanels = doc.select("div.panel-heading[id^=hlasovanie-]");
            if (votingPanels.isEmpty()) {
                log.debug("No voting data found for agenda item: {}", agendaItem.getName());
                return;
            }

            log.debug("Found {} voting panels for '{}'", votingPanels.size(), agendaItem.getName());

            for (Element panelHeading : votingPanels) {
                try {
                    Poll poll = parseVotingPanel(panelHeading, agendaItem.getName());
                    if (poll != null) {
                        poll.setExtAgendaItemId("ba-web:" + bodTypId);
                        poll.setDataSource(Source.BA_WEB);
                        agendaItem.addPoll(poll);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing voting panel: {}", e.getMessage());
                }
            }

        } catch (IOException e) {
            log.warn("Failed to fetch voting data for '{}': {}", agendaItem.getName(), e.getMessage());
        }
    }

    /**
     * Parse a single voting panel into a Poll with Votes.
     *
     * HTML structure:
     * <div class="panel-heading panel-handler" id="hlasovanie-320312">
     *   <h3 class="panel-title">Hlasovanie číslo 6</h3>
     *   <h6><b>Poznámka:</b> alternatíva č.1</h6>
     * </div>
     * <div class="panel-body" id="uz-320312">
     *   <div class="hlasovanie-za"><ul class="poslanci"><li><a href="...">Name</a></li>...</ul></div>
     *   <div class="hlasovanie-proti">...</div>
     *   <div class="hlasovanie-zdrzali">...</div>
     *   <div class="hlasovanie-nehlasovali">...</div>
     *   <div class="hlasovanie-nepritomni">...</div>
     * </div>
     */
    private Poll parseVotingPanel(Element panelHeading, String agendaItemName) {
        // Extract poll number and note
        Element titleEl = panelHeading.selectFirst("h3.panel-title");
        String pollTitle = titleEl != null ? titleEl.text().trim() : "";
        // Extract number: "Hlasovanie číslo 6" → "6"
        String pollNumber = pollTitle.replaceAll(".*?(\\d+).*", "$1");

        Element noteEl = panelHeading.selectFirst("h6");
        String note = null;
        if (noteEl != null) {
            note = noteEl.text().replace("Poznámka:", "").trim();
        }

        String hlasovanieId = panelHeading.id(); // "hlasovanie-320312"

        // Find the panel-body sibling that contains the voting data
        Element panelBody = panelHeading.nextElementSibling();
        if (panelBody == null || !panelBody.hasClass("panel-body")) {
            log.debug("No panel-body found for {}", hlasovanieId);
            return null;
        }

        // Parse votes from each category
        Set<Vote> votes = new HashSet<>();
        VotesCount votesCount = new VotesCount();

        parseVoteCategory(panelBody, "div.hlasovanie-za ul.poslanci li a", VoteChoice.VOTED_FOR, votes);
        parseVoteCategory(panelBody, "div.hlasovanie-proti ul.poslanci li a", VoteChoice.VOTED_AGAINST, votes);
        parseVoteCategory(panelBody, "div.hlasovanie-zdrzali ul.poslanci li a", VoteChoice.ABSTAIN, votes);
        parseVoteCategory(panelBody, "div.hlasovanie-nehlasovali ul.poslanci li a", VoteChoice.NOT_VOTED, votes);
        parseVoteCategory(panelBody, "div.hlasovanie-nepritomni ul.poslanci li a", VoteChoice.ABSENT, votes);

        // Count votes per category
        for (Vote vote : votes) {
            switch (vote.getVoted()) {
                case VOTED_FOR -> votesCount.setVotedFor(votesCount.getVotedFor() + 1);
                case VOTED_AGAINST -> votesCount.setVotedAgainst(votesCount.getVotedAgainst() + 1);
                case ABSTAIN -> votesCount.setAbstain(votesCount.getAbstain() + 1);
                case NOT_VOTED -> votesCount.setNotVoted(votesCount.getNotVoted() + 1);
                case ABSENT -> votesCount.setAbsent(votesCount.getAbsent() + 1);
            }
        }

        // Create Poll
        String pollName = agendaItemName + " (" + pollTitle + ")";
        if (note != null && !note.isEmpty()) {
            pollName += " - " + note;
        }

        Poll poll = new Poll();
        poll.setRef(generateUniqueKeyReference());
        poll.setName(pollName);
        poll.setExtPollRouteId(hlasovanieId.replace("hlasovanie-", ""));
        poll.setNote(note);
        poll.setVoters(votes.size());
        poll.setVotesCount(votesCount);
        poll.setVotes(votes);

        // Set poll reference on each vote
        for (Vote vote : votes) {
            vote.setPoll(poll);
        }

        log.debug("Parsed poll '{}': za={}, proti={}, zdržali={}, nehlasovali={}, neprítomní={}",
                pollTitle, votesCount.getVotedFor(), votesCount.getVotedAgainst(),
                votesCount.getAbstain(), votesCount.getNotVoted(), votesCount.getAbsent());

        return poll;
    }

    /**
     * Parse voter names from a voting category section and add Vote objects.
     * Each voter is a link like: <a href="/297336-sk/jakubkovic-jana/">JUDr. Jakubkovič Jana</a>
     */
    private void parseVoteCategory(Element panelBody, String cssSelector, VoteChoice choice, Set<Vote> votes) {
        Elements voterLinks = panelBody.select(cssSelector);
        for (Element link : voterLinks) {
            String voterName = link.text().trim();
            if (voterName.isEmpty()) continue;

            Vote vote = new Vote();
            vote.setVoted(choice);
            vote.setVoterName(voterName);
            // CouncilMember will be matched later in PoliticianMatchingService.createMissingMembersFromVotes()
            votes.add(vote);
        }
    }

    // --- Utility methods ---

    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .get();
    }

    private static String extractSlugFromUrl(String url) {
        // URL: https://zastupitelstvo.bratislava.sk/mestske-zastupitelstvo-...-zasadnutie-11122025/
        // Extract: mestske-zastupitelstvo-...-zasadnutie-11122025
        String path = url.replaceAll("https?://[^/]+/", "");
        return path.replaceAll("/$", "");
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }
}
