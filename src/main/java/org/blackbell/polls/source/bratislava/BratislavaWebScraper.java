package org.blackbell.polls.source.bratislava;

import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.embeddable.VotesCount;
import org.blackbell.polls.domain.model.enums.DataSourceType;
import org.blackbell.polls.domain.model.enums.VoteChoice;
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
    private static final String MEMBERS_URL = BASE_URL + "/mestske-zastupitelstvo-hlavneho-mesta-sr-bratislavy-2022-2026/";
    private static final String SESSIONS_URL = BASE_URL + "/zasadnutia/";
    private static final String MZ_ORGAN_PARAM = "Mestské zastupiteľstvo hlavného mesta SR Bratislavy (2022 - 2026)";
    private static final String MZ_SLUG_PREFIX = "mestske-zastupitelstvo-hlavneho-mesta-sr-bratislavy-2022-2026-zasadnutie";
    private static final int TIMEOUT_MS = 30_000;
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    // --- Members scraping ---

    /**
     * Scrape council members from the static members page.
     */
    public List<CouncilMember> scrapeMembers(Season season) {
        log.info("Scraping Bratislava 2022-2026 council members from {}", MEMBERS_URL);
        List<CouncilMember> members = new ArrayList<>();

        try {
            Document doc = fetchDocument(MEMBERS_URL);
            Elements memberDivs = doc.select("div.team-wrapper div.col-md-6");

            log.info("Found {} member elements", memberDivs.size());
            for (Element memberDiv : memberDivs) {
                try {
                    CouncilMember member = parseMemberElement(memberDiv, season);
                    if (member != null) {
                        members.add(member);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing member element: {}", e.getMessage());
                }
            }

            log.info("Scraped {} Bratislava 2022-2026 council members", members.size());
        } catch (IOException e) {
            log.error("Failed to fetch Bratislava members page: {}", e.getMessage());
        }

        return members;
    }

    private CouncilMember parseMemberElement(Element memberDiv, Season season) {
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

        // ExtId from detail link URL
        String extId = null;
        Element detailLink = memberDiv.selectFirst("figure a[href]");
        if (detailLink != null) {
            String href = detailLink.attr("href");
            // URL pattern: /212609-sk/antalova-plavuchova-lenka/
            if (href.matches(".*/\\d+-sk/.*")) {
                extId = href.replaceAll(".*/?(\\d+)-sk/.*", "$1");
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
        member.setDescription(partyInfo);

        log.debug("Parsed Bratislava member: {} (party: {})", deAccent(politician.getName()), partyInfo);
        return member;
    }

    // --- Meeting list scraping ---

    /**
     * Scrape MZ meeting list from the paginated sessions table.
     * Only returns meetings for "Mestské zastupiteľstvo" (filters out commissions and city council board).
     */
    public List<Meeting> scrapeMeetingList(Town town, Season season, Institution institution) {
        log.info("Scraping Bratislava 2022-2026 meeting list");
        List<Meeting> meetings = new ArrayList<>();

        try {
            for (int page = 0; page <= 20; page++) { // safety limit
                String url = SESSIONS_URL + "?organ=" + urlEncode(MZ_ORGAN_PARAM) + "&page=" + page;
                log.debug("Fetching sessions page {}: {}", page, url);

                Document doc = fetchDocument(url);
                Elements rows = doc.select("table.table-striped tbody tr");

                if (rows.isEmpty()) {
                    log.debug("No more sessions on page {}", page);
                    break;
                }

                boolean foundMzMeeting = false;
                for (Element row : rows) {
                    Elements cells = row.select("td");
                    if (cells.size() < 2) continue;

                    Element nameLink = cells.get(1).selectFirst("a");
                    if (nameLink == null) continue;

                    String meetingName = nameLink.text().trim();
                    String meetingUrl = nameLink.attr("href");

                    // Filter for MZ meetings only (skip commissions, city council board, etc.)
                    if (!meetingUrl.contains(MZ_SLUG_PREFIX)) {
                        continue;
                    }
                    foundMzMeeting = true;

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

            log.info("Scraped {} Bratislava MZ meetings for 2022-2026", meetings.size());
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
                        poll.setDataSource(DataSourceType.BA_WEB);
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
            // CouncilMember will be matched later in SyncAgent.createMissingMembersFromVotes()
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
