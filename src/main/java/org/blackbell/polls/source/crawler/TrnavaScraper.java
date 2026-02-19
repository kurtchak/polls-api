package org.blackbell.polls.source.crawler;

import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.embeddable.VotesCount;
import org.blackbell.polls.domain.model.enums.VoteChoice;
import org.blackbell.polls.source.Source;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.blackbell.polls.common.PollsUtils.*;

/**
 * Web scraper for trnava.sk - Trnava city council.
 *
 * Scrapes:
 * - Council members from /stranka/mestske-zastupitelstvo
 * - Meeting list from /zasadnutia/msz
 * - Meeting details + individual votes from /zasadnutie/{id}
 *
 * Votes are inline in the meeting detail page as text "Name [Za]", "Name [Proti]" etc.
 * inside accordion sections with IDs like accordion-hlasovanie-body-{id}.
 */
public class TrnavaScraper {

    private static final Logger log = LoggerFactory.getLogger(TrnavaScraper.class);

    private static final String BASE_URL = "https://www.trnava.sk";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";
    private static final SimpleDateFormat DATE_FORMAT_DOT = new SimpleDateFormat("d. M. yyyy");
    private static final SimpleDateFormat DATE_FORMAT_DOT2 = new SimpleDateFormat("dd.MM.yyyy");

    private static final Pattern VOTE_PATTERN = Pattern.compile(
            "(.+?)\\s+\\[(Za|Proti|Zdržal\\(a\\) sa|Chýbal\\(a\\)|Nehlasoval\\(a\\))\\]");

    private static final Pattern SUMMARY_PATTERN = Pattern.compile(
            "Za:\\s*(\\d+).*?Proti:\\s*(\\d+).*?Zdržal\\(a\\) sa:\\s*(\\d+).*?Chýbal\\(a\\):\\s*(\\d+).*?Nehlasoval\\(a\\):\\s*(\\d+)");

    private final String membersUrl;
    private final String meetingsUrl;
    private final String meetingDetailUrlTemplate;
    private final int timeoutMs;

    public TrnavaScraper(String membersUrl, String meetingsUrl, String meetingDetailUrlTemplate, int timeoutMs) {
        this.membersUrl = membersUrl;
        this.meetingsUrl = meetingsUrl;
        this.meetingDetailUrlTemplate = meetingDetailUrlTemplate;
        this.timeoutMs = timeoutMs;
    }

    // --- Members scraping ---

    /**
     * Scrape council members from the members page.
     * Members are grouped by electoral district (h3 headings).
     * Each member has: img, h3 name, p phone, p a[mailto] email.
     */
    public List<CouncilMember> scrapeMembers(Town town, Season season, Institution institution) {
        log.info("Scraping Trnava council members from {}", membersUrl);
        List<CouncilMember> members = new ArrayList<>();

        try {
            Document doc = fetchDocument(membersUrl);

            // Members are under h3 district headings, with img + h3 + p elements per member
            // Walk through the content to find district headings and member blocks
            String currentDistrict = null;

            // Find all h3 elements - some are district headings, some are member names
            Elements allElements = doc.select("h3, img[src*=MZ], img[src*=foto]");

            // Alternative approach: find all member images and their sibling elements
            Elements memberImages = doc.select("img[src*=MZ%20foto], img[src*=foto%202022]");
            if (memberImages.isEmpty()) {
                // Try broader selector
                memberImages = doc.select("img[src*=content/images]");
            }

            log.info("Found {} member images", memberImages.size());

            // Parse district headings first
            Elements h3Elements = doc.select("h3");
            Map<Element, String> districtMap = new LinkedHashMap<>();
            for (Element h3 : h3Elements) {
                String text = h3.text().trim();
                if (text.contains("Volebný obvod")) {
                    currentDistrict = text;
                }
            }

            // Reset and walk through content sequentially
            currentDistrict = null;
            Element contentArea = doc.selectFirst("div.page-content, div.content, main, article");
            if (contentArea == null) contentArea = doc.body();

            Elements contentElements = contentArea.getAllElements();
            String pendingName = null;
            String pendingPhone = null;
            String pendingEmail = null;
            String pendingPhoto = null;

            for (Element el : doc.select("h3")) {
                String text = el.text().trim();
                if (text.contains("Volebný obvod")) {
                    currentDistrict = text;
                    continue;
                }

                // This h3 is likely a member name
                String fullName = text;
                if (fullName.isEmpty()) continue;

                // Look for photo (preceding img)
                String photo = null;
                Element prev = el.previousElementSibling();
                while (prev != null) {
                    if (prev.tagName().equals("img") || prev.selectFirst("img") != null) {
                        Element img = prev.tagName().equals("img") ? prev : prev.selectFirst("img");
                        String src = img.attr("src");
                        if (src.contains("content/images") || src.contains("foto")) {
                            photo = src.startsWith("http") ? src : BASE_URL + "/" + src;
                            break;
                        }
                    }
                    if (prev.tagName().equals("h3")) break; // Previous member
                    prev = prev.previousElementSibling();
                }

                // Look for phone and email in following p elements
                String phone = null;
                String email = null;
                Element next = el.nextElementSibling();
                for (int i = 0; i < 4 && next != null; i++) {
                    if (next.tagName().equals("h3")) break; // Next member
                    String nextText = next.text().trim();

                    // Check for email link
                    Element emailLink = next.selectFirst("a[href^=mailto:]");
                    if (emailLink != null && email == null) {
                        email = emailLink.attr("href").replace("mailto:", "");
                    }

                    // Check for phone number
                    if (phone == null && nextText.matches(".*\\+?421.*|.*\\d{3}\\s*\\d{3}\\s*\\d{3}.*")) {
                        phone = nextText.trim();
                    }

                    next = next.nextElementSibling();
                }

                // Create member
                CouncilMember member = createMember(fullName, photo, phone, email, currentDistrict,
                        town, season, institution);
                if (member != null) {
                    members.add(member);
                }
            }

            log.info("Scraped {} Trnava council members", members.size());
        } catch (IOException e) {
            log.error("Failed to scrape Trnava members: {}", e.getMessage());
        }

        return members;
    }

    private CouncilMember createMember(String fullName, String photo, String phone, String email,
                                       String district, Town town, Season season, Institution institution) {
        if (fullName == null || fullName.isEmpty()) return null;
        // Skip non-member h3 headings
        if (fullName.contains("Volebný obvod") || fullName.contains("Mestské zastupiteľstvo")) return null;

        Politician politician = new Politician();
        politician.setRef(generateUniqueKeyReference());
        politician.setName(toSimpleName(fullName));
        politician.setTitles(getTitles(fullName));
        politician.setPicture(photo);
        politician.setPhone(phone);
        politician.setEmail(email);

        CouncilMember member = new CouncilMember();
        member.setRef(generateUniqueKeyReference());
        member.setPolitician(politician);
        member.setSeason(season);
        member.setTown(town);
        member.setInstitution(institution);
        if (district != null) {
            member.setDescription(district);
        }

        log.debug("Parsed Trnava member: {} (email: {}, district: {})",
                deAccent(politician.getName()), email, district);
        return member;
    }

    // --- Meeting list scraping ---

    /**
     * Scrape meeting list from the meetings page.
     * Table with columns: name (link), date.
     * Paginated with ?page=N.
     */
    public List<Meeting> scrapeMeetingList(Town town, Season season, Institution institution) {
        log.info("Scraping Trnava meeting list from {}", meetingsUrl);
        List<Meeting> meetings = new ArrayList<>();

        try {
            for (int page = 1; page <= 20; page++) {
                String url = meetingsUrl + (page > 1 ? "?page=" + page : "");
                Document doc = fetchDocument(url);

                Elements rows = doc.select("table tbody tr");
                if (rows.isEmpty()) {
                    // Try broader selector
                    rows = doc.select("tr");
                }

                boolean foundAny = false;
                for (Element row : rows) {
                    Element link = row.selectFirst("a[href*=/zasadnutie/]");
                    if (link == null) continue;

                    String meetingName = link.text().trim();
                    String href = link.attr("href");

                    // Extract meeting ID from URL
                    String meetingId = href.replaceAll(".*/zasadnutie/(\\d+).*", "$1");
                    if (meetingId.equals(href)) continue; // No match

                    // Parse date from another cell
                    Elements cells = row.select("td");
                    String dateText = null;
                    for (Element cell : cells) {
                        String cellText = cell.text().trim();
                        if (cellText.matches("\\d+\\.\\s*\\d+\\.\\s*\\d{4}")) {
                            dateText = cellText;
                            break;
                        }
                    }

                    Date date = parseDate(dateText);

                    Meeting meeting = new Meeting();
                    meeting.setName(meetingName);
                    meeting.setExtId("trnava-web:" + meetingId);
                    meeting.setDate(date);
                    meeting.setRef(generateUniqueKeyReference());
                    meeting.setTown(town);
                    meeting.setSeason(season);
                    meeting.setInstitution(institution);
                    meeting.setDataSource(Source.TRNAVA_WEB);

                    meetings.add(meeting);
                    foundAny = true;
                    log.debug("Found Trnava meeting: {} ({})", meetingName, dateText);
                }

                if (!foundAny) break;

                // Check for next page
                Element nextPage = doc.selectFirst("a[href*=page=" + (page + 1) + "]");
                if (nextPage == null) break;
            }

            log.info("Scraped {} Trnava meetings", meetings.size());
        } catch (IOException e) {
            log.error("Failed to scrape Trnava meeting list: {}", e.getMessage());
        }

        return meetings;
    }

    // --- Meeting details + voting scraping ---

    /**
     * Scrape meeting details including agenda items and voting data.
     * The voting data is in accordion sections with individual votes as text.
     */
    public void scrapeMeetingDetails(Meeting meeting) {
        String meetingId = meeting.getExtId().replace("trnava-web:", "");
        String url = meetingDetailUrlTemplate.replace("{id}", meetingId);
        log.info("Scraping Trnava meeting details from: {}", url);

        try {
            Document doc = fetchDocument(url);

            // Parse agenda items - look for numbered items in the program tab
            Elements agendaElements = doc.select("[id^=accordion-hlasovanie-body-]");

            if (agendaElements.isEmpty()) {
                // Try broader approach - find all voting accordion sections
                agendaElements = doc.select("div[id*=hlasovanie]");
            }

            log.info("Found {} voting sections for meeting '{}'", agendaElements.size(), meeting.getName());

            // Create a single aggregate agenda item for each voting section
            // or try to match them to program items
            Elements programItems = doc.select("div[id^=accordion-] h4, div[id^=accordion-] h5, " +
                    "div.accordion-item h4, div.accordion-item h5");

            // Parse voting sections
            for (Element votingSection : agendaElements) {
                String sectionId = votingSection.id();
                // accordion-hlasovanie-body-6391
                String voteId = sectionId.replace("accordion-hlasovanie-body-", "");

                // Find the heading for this section
                String headingId = sectionId.replace("-body-", "-heading-");
                Element heading = doc.getElementById(headingId);
                if (heading == null) {
                    // Try parent or sibling
                    heading = votingSection.previousElementSibling();
                }

                String agendaName = heading != null ? heading.text().trim() : "Hlasovanie " + voteId;

                // Parse votes from the section text
                String sectionText = votingSection.text();
                parseVotingSection(meeting, agendaName, voteId, sectionText);
            }

            // If no accordion sections found, try parsing the entire voting tab
            if (agendaElements.isEmpty()) {
                Element votingTab = doc.selectFirst("[data-tab-target*=hlasovanie], #hlasovanie, .tab-pane:has([id*=hlasovanie])");
                if (votingTab != null) {
                    parseVotingTab(meeting, votingTab);
                }
            }

            int agendaCount = meeting.getAgendaItems() != null ? meeting.getAgendaItems().size() : 0;
            int pollCount = meeting.getAgendaItems() != null
                    ? meeting.getAgendaItems().stream()
                    .mapToInt(ai -> ai.getPolls() != null ? ai.getPolls().size() : 0).sum()
                    : 0;
            log.info("Loaded {} agenda items with {} polls for Trnava meeting '{}'",
                    agendaCount, pollCount, meeting.getName());

        } catch (IOException e) {
            log.error("Failed to scrape Trnava meeting details: {}", e.getMessage());
            meeting.setSyncError("Failed to scrape: " + e.getMessage());
        }
    }

    /**
     * Parse a voting section text that contains lines like "Name [Za]" and a summary line.
     */
    private void parseVotingSection(Meeting meeting, String agendaName, String voteId, String sectionText) {
        Set<Vote> votes = new HashSet<>();

        Matcher voteMatcher = VOTE_PATTERN.matcher(sectionText);
        while (voteMatcher.find()) {
            String voterName = voteMatcher.group(1).trim();
            String voteStr = voteMatcher.group(2);

            VoteChoice choice = parseVoteChoice(voteStr);

            Vote vote = new Vote();
            vote.setVoted(choice);
            vote.setVoterName(voterName);
            votes.add(vote);
        }

        if (votes.isEmpty()) {
            log.debug("No votes found in section: {}", agendaName);
            return;
        }

        // Parse summary
        VotesCount votesCount = new VotesCount();
        Matcher summaryMatcher = SUMMARY_PATTERN.matcher(sectionText);
        if (summaryMatcher.find()) {
            votesCount.setVotedFor(Integer.parseInt(summaryMatcher.group(1)));
            votesCount.setVotedAgainst(Integer.parseInt(summaryMatcher.group(2)));
            votesCount.setAbstain(Integer.parseInt(summaryMatcher.group(3)));
            votesCount.setAbsent(Integer.parseInt(summaryMatcher.group(4)));
            votesCount.setNotVoted(Integer.parseInt(summaryMatcher.group(5)));
        } else {
            // Count from votes
            for (Vote vote : votes) {
                switch (vote.getVoted()) {
                    case VOTED_FOR -> votesCount.setVotedFor(votesCount.getVotedFor() + 1);
                    case VOTED_AGAINST -> votesCount.setVotedAgainst(votesCount.getVotedAgainst() + 1);
                    case ABSTAIN -> votesCount.setAbstain(votesCount.getAbstain() + 1);
                    case ABSENT -> votesCount.setAbsent(votesCount.getAbsent() + 1);
                    case NOT_VOTED -> votesCount.setNotVoted(votesCount.getNotVoted() + 1);
                }
            }
        }

        // Create agenda item
        AgendaItem agendaItem = new AgendaItem();
        agendaItem.setName(agendaName);
        agendaItem.setRef(generateUniqueKeyReference());
        agendaItem.setExtId("trnava:" + voteId);

        // Create poll
        Poll poll = new Poll();
        poll.setRef(generateUniqueKeyReference());
        poll.setName(agendaName);
        poll.setExtAgendaItemId("trnava:" + voteId);
        poll.setVoters(votes.size());
        poll.setVotesCount(votesCount);
        poll.setVotes(votes);
        poll.setDataSource(Source.TRNAVA_WEB);

        for (Vote vote : votes) {
            vote.setPoll(poll);
        }

        meeting.addAgendaItem(agendaItem);  // must be before addPoll — Poll.hashCode() needs agendaItem.meeting
        agendaItem.addPoll(poll);

        log.debug("Parsed Trnava vote '{}': za={}, proti={}, zdržali={}, chýbali={}, nehlasovali={}",
                agendaName, votesCount.getVotedFor(), votesCount.getVotedAgainst(),
                votesCount.getAbstain(), votesCount.getAbsent(), votesCount.getNotVoted());
    }

    /**
     * Fallback: parse the entire voting tab to find all voting sections.
     */
    private void parseVotingTab(Meeting meeting, Element votingTab) {
        // Split by headings or numbered sections
        Elements headings = votingTab.select("h4, h5, h3, .accordion-header, [id^=accordion-hlasovanie-heading]");

        for (Element heading : headings) {
            String agendaName = heading.text().trim();
            if (agendaName.isEmpty()) continue;

            // Find the body content after this heading
            Element body = heading.nextElementSibling();
            if (body != null) {
                String bodyText = body.text();
                String voteId = heading.id() != null ? heading.id().replaceAll(".*-(\\d+)$", "$1") : generateUniqueKeyReference();
                parseVotingSection(meeting, agendaName, voteId, bodyText);
            }
        }
    }

    private VoteChoice parseVoteChoice(String text) {
        return switch (text) {
            case "Za" -> VoteChoice.VOTED_FOR;
            case "Proti" -> VoteChoice.VOTED_AGAINST;
            case "Zdržal(a) sa" -> VoteChoice.ABSTAIN;
            case "Chýbal(a)" -> VoteChoice.ABSENT;
            case "Nehlasoval(a)" -> VoteChoice.NOT_VOTED;
            default -> VoteChoice.NOT_VOTED;
        };
    }

    private Date parseDate(String dateText) {
        if (dateText == null) return null;
        dateText = dateText.trim();
        try {
            synchronized (DATE_FORMAT_DOT) {
                return DATE_FORMAT_DOT.parse(dateText);
            }
        } catch (ParseException e) {
            try {
                synchronized (DATE_FORMAT_DOT2) {
                    return DATE_FORMAT_DOT2.parse(dateText);
                }
            } catch (ParseException e2) {
                log.warn("Could not parse date: '{}'", dateText);
                return null;
            }
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
