package org.blackbell.polls.source.crawler;

import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.embeddable.VotesCount;
import org.blackbell.polls.domain.model.enums.ClubFunction;
import org.blackbell.polls.domain.model.enums.VoteChoice;
import org.blackbell.polls.domain.model.relate.ClubMember;
import org.blackbell.polls.domain.model.relate.PartyNominee;
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
 * Web scraper for kosice.sk - Košice city council.
 *
 * Scrapes:
 * - Council members from /mesto/samosprava/mestske-zastupitelstvo/{season}/poslanci
 * - Member details (club, commissions, photo, email) from individual member profiles
 * - Voting data from member profile #tab_votes tables
 * - Meeting list from the season overview page
 * - Meeting agenda items from meeting detail pages
 *
 * Strategy: members + votes from profile pages, meetings from overview page,
 * agenda items from meeting detail pages.
 */
public class KosiceScraper {

    private static final Logger log = LoggerFactory.getLogger(KosiceScraper.class);

    private static final String BASE_URL = "https://www.kosice.sk";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    private static final Pattern MEMBER_SLUG_PATTERN = Pattern.compile("/poslanec/(\\d+-[\\w-]+)");

    private final String membersUrl;
    private final String meetingsUrl;
    private final String memberDetailUrl;
    private final int timeoutMs;

    private final Map<String, Club> clubsMap = new HashMap<>();
    private final Map<String, Party> partiesMap = new HashMap<>();

    public KosiceScraper(String membersUrl, String meetingsUrl, String memberDetailUrl, int timeoutMs) {
        this.membersUrl = membersUrl;
        this.meetingsUrl = meetingsUrl;
        this.memberDetailUrl = memberDetailUrl;
        this.timeoutMs = timeoutMs;
    }

    // --- Members scraping ---

    /**
     * Scrape council members from the members page and their detail pages.
     *
     * HTML structure on kosice.sk:
     * - Club sections: div.membership > h6 > strong (club name)
     * - Chairman: span.small > strong (chairman name)
     * - Members: div.members > div.assignRow > div.name > a[href*=poslanec] (name + link)
     * - Party/email/photo/district are on individual detail pages
     */
    public List<CouncilMember> scrapeMembers(Town town, Season season, Institution institution) {
        String url = membersUrl.replace("{season}", season.getRef());
        log.info("Scraping Košice council members from {}", url);
        List<CouncilMember> members = new ArrayList<>();
        clubsMap.clear();
        partiesMap.clear();

        try {
            Document doc = fetchDocument(url);

            // Members are grouped by club in div.membership sections
            Set<String> seenSlugs = new HashSet<>();
            Elements membershipDivs = doc.select("div.membership");
            log.info("Found {} club sections", membershipDivs.size());

            for (Element clubDiv : membershipDivs) {
                // Club name from h6 > strong
                Element h6 = clubDiv.selectFirst("h6 strong");
                String clubName = h6 != null ? h6.text().trim() : null;

                // Chairman name from span.small
                Element chairmanSpan = clubDiv.selectFirst("span.small");
                String chairmanText = chairmanSpan != null ? chairmanSpan.text() : "";
                // Extract chairman name after "Predseda:" or "Predsedníčka:"
                String chairmanName = chairmanText.replaceAll(".*?:\\s*", "").trim();

                // Each member: div.assignRow > div.name > a
                Elements assignRows = clubDiv.select("div.assignRow");
                for (Element row : assignRows) {
                    try {
                        Element nameLink = row.selectFirst("div.name a[href*=poslanec]");
                        if (nameLink == null) continue;

                        String fullName = nameLink.text().trim();
                        String href = nameLink.attr("href");

                        // Extract slug from URL
                        Matcher slugMatcher = MEMBER_SLUG_PATTERN.matcher(href);
                        String slug = slugMatcher.find() ? slugMatcher.group(1) : null;

                        // Skip duplicates (member might appear in multiple sections)
                        if (slug != null && !seenSlugs.add(slug)) continue;

                        // Create base member
                        Politician politician = new Politician();
                        politician.setRef(generateUniqueKeyReference());
                        politician.setName(toSimpleName(fullName));
                        politician.setTitles(getTitles(fullName));
                        politician.setExtId(slug);

                        CouncilMember member = new CouncilMember();
                        member.setRef(generateUniqueKeyReference());
                        member.setPolitician(politician);
                        member.setSeason(season);
                        member.setTown(town);
                        member.setInstitution(institution);
                        member.setExtId(slug);

                        // Add club membership
                        if (clubName != null && !clubName.isEmpty()) {
                            boolean isChairman = toSimpleName(fullName).equals(toSimpleName(chairmanName));
                            ClubFunction function = isChairman ? ClubFunction.CHAIRMAN : ClubFunction.MEMBER;
                            addClubMembership(member, clubName, function);
                        }

                        // Fetch detail page for photo, email, party, district
                        if (slug != null) {
                            fetchMemberDetails(member, season.getRef(), slug);
                        }

                        members.add(member);
                        log.debug("Parsed Košice member: {} (club: {})",
                                deAccent(politician.getName()),
                                clubName != null ? clubName : "none");
                    } catch (Exception e) {
                        log.warn("Error parsing Košice member row: {}", e.getMessage());
                    }
                }
            }

            log.info("Scraped {} Košice council members", members.size());
        } catch (IOException e) {
            log.error("Failed to scrape Košice members: {}", e.getMessage());
        }

        return members;
    }

    /**
     * Fetch member detail page for photo, email, party, district.
     *
     * HTML structure:
     * - Photo: img[src*=static.kosice.sk]
     * - Email: span containing "e-mail:" text
     * - Party: div.info containing "Kandidoval(a) za:" > span value
     * - District: div.info containing "Volebný obvod:" text
     */
    private void fetchMemberDetails(CouncilMember member, String seasonRef, String slug) {
        String url = memberDetailUrl.replace("{season}", seasonRef).replace("{slug}", slug);

        try {
            Document doc = fetchDocument(url);
            Politician politician = member.getPolitician();

            // Photo
            Element imgEl = doc.selectFirst("img[src*=static.kosice.sk]");
            if (imgEl != null) {
                politician.setPicture(imgEl.attr("src"));
            }

            // Email — may be in a mailto link or as text "e-mail: xxx"
            Element emailLink = doc.selectFirst("a[href^=mailto:]");
            if (emailLink != null) {
                politician.setEmail(emailLink.attr("href").replace("mailto:", ""));
            } else {
                // Fallback: look for text containing "e-mail:"
                Elements spans = doc.select("span");
                for (Element span : spans) {
                    String text = span.text().trim();
                    if (text.startsWith("e-mail:")) {
                        String email = text.replace("e-mail:", "").trim();
                        if (!email.isEmpty()) {
                            politician.setEmail(email);
                        }
                        break;
                    }
                }
            }

            // Info fields: div.info containing label spans
            Elements infoFields = doc.select("div.info");
            for (Element field : infoFields) {
                String text = field.text().trim();

                if (text.contains("Volebný obvod")) {
                    String value = text.replaceAll(".*Volebný obvod:?\\s*", "").trim();
                    if (!value.isEmpty()) {
                        member.setDescription("Volebný obvod: " + value);
                    }
                }

                if (text.contains("Kandidoval") && text.contains("za:")) {
                    // "Kandidoval(a) za: SMER-SD" — extract party name
                    String partyText = text.replaceAll(".*za:\\s*", "").trim();
                    if (!partyText.isEmpty()) {
                        addPartyNomination(member, partyText);
                    }
                }
            }

            log.debug("Fetched details for {}: photo={}, email={}, district={}",
                    deAccent(politician.getName()), politician.getPicture() != null,
                    politician.getEmail(), member.getDescription());

        } catch (IOException e) {
            log.warn("Failed to fetch Košice member detail for {}: {}",
                    deAccent(member.getPolitician().getName()), e.getMessage());
        }
    }

    // --- Voting data scraping from member profiles ---

    /**
     * Scrape voting records from a member's profile page (#tab_votes table).
     * Returns a map of (meetingRef + voteNumber) -> VoteChoice.
     *
     * Table columns: Zasadnutie | Hlasovanie | Bod hlasovania | Stanovisko
     */
    public List<MemberVoteRecord> scrapeMemberVotes(String seasonRef, String memberSlug) {
        String url = memberDetailUrl.replace("{season}", seasonRef).replace("{slug}", memberSlug);
        List<MemberVoteRecord> records = new ArrayList<>();

        try {
            Document doc = fetchDocument(url);
            Elements voteRows = doc.select("#tab_votes table.table tbody tr, table.votes tbody tr");

            for (Element row : voteRows) {
                Elements cells = row.select("td");
                if (cells.size() < 4) continue;

                // Column 1: Meeting info (link + date + institution)
                Element meetingLink = cells.get(0).selectFirst("a");
                String meetingName = meetingLink != null ? meetingLink.text().trim() : "";
                String meetingHref = meetingLink != null ? meetingLink.attr("href") : "";
                Elements smalls = cells.get(0).select("small");
                String dateStr = smalls.size() > 0 ? smalls.get(0).text().trim() : "";

                // Column 2: Vote number (e.g., "č.15")
                String voteNumberText = cells.get(1).text().trim();
                String voteNumber = voteNumberText.replaceAll("[^\\d]", "");

                // Column 3: Agenda item title
                String agendaTitle = cells.get(2).text().trim();

                // Column 4: Vote stance
                String stance = cells.get(3).text().trim().toUpperCase();
                VoteChoice choice = parseKosiceVoteChoice(stance);

                // Extract meeting ID from href
                String meetingId = meetingHref.replaceAll(".*/zasadnutie/(\\d+).*", "$1");

                MemberVoteRecord record = new MemberVoteRecord(
                        meetingId, meetingName, dateStr, voteNumber, agendaTitle, choice);
                records.add(record);
            }

            log.debug("Scraped {} vote records for member {}", records.size(), memberSlug);
        } catch (IOException e) {
            log.warn("Failed to scrape votes for member {}: {}", memberSlug, e.getMessage());
        }

        return records;
    }

    /**
     * Record of a single member's vote from their profile page.
     */
    public record MemberVoteRecord(
            String meetingId,
            String meetingName,
            String dateStr,
            String voteNumber,
            String agendaTitle,
            VoteChoice choice
    ) {}

    // --- Meeting list scraping ---

    /**
     * Scrape meeting list from the season overview page.
     * Meetings are listed as links in the content area.
     */
    public List<Meeting> scrapeMeetingList(Town town, Season season, Institution institution) {
        String url = meetingsUrl.replace("{season}", season.getRef());
        log.info("Scraping Košice meeting list from {}", url);
        List<Meeting> meetings = new ArrayList<>();

        try {
            Document doc = fetchDocument(url);

            // Find all meeting links
            Elements meetingLinks = doc.select("a[href*=/zasadnutie/]");
            Set<String> seenIds = new HashSet<>();

            for (Element link : meetingLinks) {
                String href = link.attr("href");
                // Skip resolution/uznesenie links
                if (href.contains("/uzneseni")) continue;

                String meetingId = href.replaceAll(".*/zasadnutie/(\\d+).*", "$1");
                if (meetingId.equals(href) || seenIds.contains(meetingId)) continue;
                seenIds.add(meetingId);

                String meetingName = link.text().trim();
                if (meetingName.isEmpty()) continue;

                Meeting meeting = new Meeting();
                meeting.setName(meetingName);
                meeting.setExtId("kosice-web:" + meetingId);
                meeting.setRef(generateUniqueKeyReference());
                meeting.setTown(town);
                meeting.setSeason(season);
                meeting.setInstitution(institution);
                meeting.setDataSource(Source.KOSICE_WEB);

                meetings.add(meeting);
                log.debug("Found Košice meeting: {} (id: {})", meetingName, meetingId);
            }

            log.info("Scraped {} Košice meetings", meetings.size());
        } catch (IOException e) {
            log.error("Failed to scrape Košice meeting list: {}", e.getMessage());
        }

        return meetings;
    }

    // --- Meeting details scraping ---

    /**
     * Scrape meeting detail page for date and agenda items.
     */
    public void scrapeMeetingDetails(Meeting meeting) {
        String meetingId = meeting.getExtId().replace("kosice-web:", "");
        String url = meetingsUrl.replace("{season}", meeting.getSeason().getRef())
                + "/zasadnutie/" + meetingId;
        log.info("Scraping Košice meeting details from: {}", url);

        try {
            Document doc = fetchDocument(url);

            // Parse date
            Elements infoFields = doc.select("div.info.fix-span");
            for (Element field : infoFields) {
                Element label = field.selectFirst("span");
                if (label != null && label.text().contains("Dátum konania")) {
                    String dateStr = field.text().replace(label.text(), "").trim();
                    Date date = parseDate(dateStr);
                    if (date != null) {
                        meeting.setDate(date);
                    }
                }
            }

            // Parse agenda items from table
            Elements agendaRows = doc.select("table.table tbody tr[data-point]");
            if (agendaRows.isEmpty()) {
                agendaRows = doc.select("table.table tbody tr");
            }

            for (Element row : agendaRows) {
                Element titleEl = row.selectFirst("td.title span");
                if (titleEl == null) {
                    // Fallback: last td
                    Elements cells = row.select("td");
                    titleEl = cells.size() >= 3 ? cells.get(2) : null;
                }
                if (titleEl == null) continue;

                String title = titleEl.text().trim();
                String dataPoint = row.attr("data-point");

                AgendaItem item = new AgendaItem();
                item.setName(title);
                item.setRef(generateUniqueKeyReference());
                item.setExtId(dataPoint.isEmpty() ? null : "kosice:" + meetingId + ":" + dataPoint);

                meeting.addAgendaItem(item);
            }

            int agendaCount = meeting.getAgendaItems() != null ? meeting.getAgendaItems().size() : 0;
            log.info("Loaded {} agenda items for Košice meeting '{}'", agendaCount, meeting.getName());

        } catch (IOException e) {
            log.error("Failed to scrape Košice meeting details: {}", e.getMessage());
            meeting.setSyncError("Failed to scrape: " + e.getMessage());
        }
    }

    /**
     * Build polls from aggregated member vote records.
     * Groups vote records by (meetingId + voteNumber) and creates Poll objects with Votes.
     */
    public void buildPollsFromMemberVotes(Meeting meeting,
                                          Map<String, List<MemberVoteRecord>> votesByKey,
                                          Map<String, String> memberNames) {
        String meetingId = meeting.getExtId().replace("kosice-web:", "");

        // Get all vote keys for this meeting
        Map<String, List<Map.Entry<String, MemberVoteRecord>>> pollVotes = new LinkedHashMap<>();

        for (Map.Entry<String, List<MemberVoteRecord>> entry : votesByKey.entrySet()) {
            String memberSlug = entry.getKey();
            for (MemberVoteRecord record : entry.getValue()) {
                if (!meetingId.equals(record.meetingId())) continue;

                String pollKey = record.meetingId() + ":" + record.voteNumber();
                pollVotes.computeIfAbsent(pollKey, k -> new ArrayList<>())
                        .add(Map.entry(memberSlug, record));
            }
        }

        for (Map.Entry<String, List<Map.Entry<String, MemberVoteRecord>>> entry : pollVotes.entrySet()) {
            List<Map.Entry<String, MemberVoteRecord>> memberVotes = entry.getValue();
            if (memberVotes.isEmpty()) continue;

            MemberVoteRecord sample = memberVotes.get(0).getValue();

            // Find or create agenda item
            AgendaItem agendaItem = findOrCreateAgendaItem(meeting, sample.agendaTitle());

            // Create poll
            Set<Vote> votes = new HashSet<>();
            VotesCount votesCount = new VotesCount();

            for (Map.Entry<String, MemberVoteRecord> mv : memberVotes) {
                String memberSlug = mv.getKey();
                MemberVoteRecord record = mv.getValue();

                String memberName = memberNames.get(memberSlug);

                Vote vote = new Vote();
                vote.setVoted(record.choice());
                vote.setVoterName(memberName != null ? memberName : memberSlug);
                votes.add(vote);

                switch (record.choice()) {
                    case VOTED_FOR -> votesCount.setVotedFor(votesCount.getVotedFor() + 1);
                    case VOTED_AGAINST -> votesCount.setVotedAgainst(votesCount.getVotedAgainst() + 1);
                    case ABSTAIN -> votesCount.setAbstain(votesCount.getAbstain() + 1);
                    case NOT_VOTED -> votesCount.setNotVoted(votesCount.getNotVoted() + 1);
                    case ABSENT -> votesCount.setAbsent(votesCount.getAbsent() + 1);
                }
            }

            String pollName = sample.agendaTitle() + " (Hlasovanie č. " + sample.voteNumber() + ")";

            Poll poll = new Poll();
            poll.setRef(generateUniqueKeyReference());
            poll.setName(pollName);
            poll.setExtAgendaItemId("kosice:" + sample.meetingId() + ":" + sample.voteNumber());
            poll.setVoters(votes.size());
            poll.setVotesCount(votesCount);
            poll.setVotes(votes);
            poll.setDataSource(Source.KOSICE_WEB);

            for (Vote vote : votes) {
                vote.setPoll(poll);
            }

            agendaItem.addPoll(poll);
        }
    }

    private AgendaItem findOrCreateAgendaItem(Meeting meeting, String title) {
        if (meeting.getAgendaItems() != null) {
            for (AgendaItem item : meeting.getAgendaItems()) {
                if (item.getName() != null && item.getName().equals(title)) {
                    return item;
                }
            }
        }

        AgendaItem item = new AgendaItem();
        item.setName(title);
        item.setRef(generateUniqueKeyReference());
        meeting.addAgendaItem(item);
        return item;
    }

    // --- Helper methods ---

    private void addPartyNomination(CouncilMember member, String partyText) {
        if (partyText.isEmpty() ||
                partyText.equalsIgnoreCase("nezávislý kandidát") ||
                partyText.equalsIgnoreCase("nezávislá kandidátka")) {
            return;
        }

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

    private void addClubMembership(CouncilMember member, String clubName, ClubFunction function) {
        Club club = clubsMap.get(clubName);
        if (club == null) {
            club = new Club();
            club.setRef(generateUniqueKeyReference());
            club.setName(clubName);
            club.setTown(member.getTown());
            club.setSeason(member.getSeason());
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

    private VoteChoice parseKosiceVoteChoice(String stance) {
        if (stance.contains("ZA") && !stance.contains("ZDRŽAL")) return VoteChoice.VOTED_FOR;
        if (stance.contains("PROTI")) return VoteChoice.VOTED_AGAINST;
        if (stance.contains("ZDRŽAL")) return VoteChoice.ABSTAIN;
        if (stance.contains("NEHLASOVAL")) return VoteChoice.NOT_VOTED;
        return VoteChoice.NOT_VOTED;
    }

    private Date parseDate(String dateText) {
        if (dateText == null) return null;
        try {
            synchronized (DATE_FORMAT) {
                return DATE_FORMAT.parse(dateText.trim());
            }
        } catch (ParseException e) {
            log.warn("Could not parse date: '{}'", dateText);
            return null;
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
