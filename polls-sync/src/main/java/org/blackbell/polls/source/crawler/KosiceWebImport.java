package org.blackbell.polls.source.crawler;

import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.config.CrawlerProperties;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.CouncilMemberRepository;
import org.blackbell.polls.domain.DataImport;
import org.blackbell.polls.domain.model.enums.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * DataImport implementation for Košice city council via web scraping.
 * Full scraper: members + meetings + voting data from member profiles.
 *
 * Voting strategy: scrapes individual votes from each member's profile page
 * (#tab_votes table) and aggregates them into Poll objects per meeting.
 */
@Component
public class KosiceWebImport implements DataImport {

    private static final Logger log = LoggerFactory.getLogger(KosiceWebImport.class);

    private static final List<String> KNOWN_SEASONS = List.of("2022-2026");

    private final CrawlerProperties crawlerProperties;
    private final CouncilMemberRepository councilMemberRepository;

    private String currentSeasonRef;
    private Map<String, String> memberSlugToName;
    private Map<String, List<KosiceScraper.MemberVoteRecord>> cachedVoteRecords;

    public KosiceWebImport(CrawlerProperties crawlerProperties,
                           CouncilMemberRepository councilMemberRepository) {
        this.crawlerProperties = crawlerProperties;
        this.councilMemberRepository = councilMemberRepository;
    }

    @Override
    public Source getSource() {
        return Source.KOSICE_WEB;
    }

    @Override
    public List<Season> loadSeasons(Town town) throws Exception {
        log.info("Loading Košice seasons...");
        return KNOWN_SEASONS.stream().map(ref -> {
            Season s = new Season();
            s.setRef(ref);
            s.setName(ref);
            return s;
        }).toList();
    }

    @Override
    public List<Meeting> loadMeetings(Town town, Season season, Institution institution) throws Exception {
        if (institution.getType() != InstitutionType.ZASTUPITELSTVO) {
            return new ArrayList<>();
        }
        KosiceScraper scraper = createScraper();
        return scraper.scrapeMeetingList(town, season, institution);
    }

    @Override
    public void loadMeetingDetails(Meeting meeting, String externalMeetingId) throws Exception {
        KosiceScraper scraper = createScraper();
        scraper.scrapeMeetingDetails(meeting);
        ensureMemberSlugToNameLoaded(meeting.getTown(), meeting.getSeason());
        if (memberSlugToName != null && !memberSlugToName.isEmpty()) {
            ensureVotesLoaded(meeting.getSeason().getRef(), scraper);
            String meetingId = meeting.getExtId().replace("kosice-web:", "");
            long matchingRecords = cachedVoteRecords.values().stream()
                    .flatMap(List::stream)
                    .filter(r -> meetingId.equals(r.meetingId()))
                    .count();
            log.info("Building polls for meeting '{}' (id={}): {} matching vote records from cache",
                    meeting.getName(), meetingId, matchingRecords);
            scraper.buildPollsFromMemberVotes(meeting, cachedVoteRecords, memberSlugToName);
            boolean hasPolls = meeting.getAgendaItems() != null && meeting.getAgendaItems().stream()
                    .anyMatch(ai -> ai.getPolls() != null && !ai.getPolls().isEmpty());
            log.info("After buildPolls: meeting '{}' hasPolls={}", meeting.getName(), hasPolls);
        } else {
            log.warn("memberSlugToName is {} for meeting '{}' — skipping vote integration",
                    memberSlugToName == null ? "null" : "empty", meeting.getName());
        }
    }

    @Override
    public void loadPollDetails(Poll poll, Map<String, CouncilMember> membersMap) throws Exception {
        matchVotesToMembers(poll, membersMap);
    }

    @Override
    public List<CouncilMember> loadMembers(Town town, Season season, Institution institution) {
        KosiceScraper scraper = createScraper();
        List<CouncilMember> members = scraper.scrapeMembers(town, season, institution);
        if (members != null && !members.isEmpty()) {
            currentSeasonRef = season.getRef();
            memberSlugToName = new LinkedHashMap<>();
            for (CouncilMember m : members) {
                if (m.getExtId() != null) {
                    memberSlugToName.put(m.getExtId(), m.getPolitician().getName());
                }
            }
            cachedVoteRecords = null;
            log.info("Cached {} member slug→name mappings for vote scraping", memberSlugToName.size());
            return members;
        }
        return null;
    }

    private void ensureMemberSlugToNameLoaded(Town town, Season season) {
        // Reset cache when switching seasons
        if (currentSeasonRef != null && !currentSeasonRef.equals(season.getRef())) {
            log.info("Season changed from {} to {} — resetting member cache", currentSeasonRef, season.getRef());
            memberSlugToName = null;
            cachedVoteRecords = null;
            currentSeasonRef = season.getRef();
        }

        if (memberSlugToName != null && !memberSlugToName.isEmpty()) return;

        // Populate from DB
        Set<CouncilMember> members = councilMemberRepository
                .getByTownAndSeasonAndInstitution(town.getRef(), season.getRef(), InstitutionType.ZASTUPITELSTVO);
        if (members == null || members.isEmpty()) {
            log.warn("No members in DB for {} season {} — cannot build memberSlugToName",
                    town.getRef(), season.getRef());
            return;
        }

        currentSeasonRef = season.getRef();
        memberSlugToName = new LinkedHashMap<>();
        for (CouncilMember m : members) {
            if (m.getExtId() != null) {
                memberSlugToName.put(m.getExtId(), m.getPolitician().getName());
            }
        }
        cachedVoteRecords = null;
        log.info("Populated {} member slug→name mappings from DB for season {}",
                memberSlugToName.size(), season.getRef());
    }

    private void ensureVotesLoaded(String seasonRef, KosiceScraper scraper) {
        if (cachedVoteRecords != null) {
            log.debug("Vote cache already loaded ({} members)", cachedVoteRecords.size());
            return;
        }
        log.info("Starting vote scraping for {} member profiles...", memberSlugToName.size());
        cachedVoteRecords = new LinkedHashMap<>();
        for (String slug : memberSlugToName.keySet()) {
            List<KosiceScraper.MemberVoteRecord> records = scraper.scrapeMemberVotes(seasonRef, slug);
            cachedVoteRecords.put(slug, records);
            if (!records.isEmpty()) {
                log.debug("Member {}: {} vote records (first meetingId={})",
                        slug, records.size(), records.get(0).meetingId());
            }
        }
        log.info("Scraped {} vote records from {} members",
                cachedVoteRecords.values().stream().mapToInt(List::size).sum(),
                memberSlugToName.size());
    }

    private KosiceScraper createScraper() {
        return new KosiceScraper(
                crawlerProperties.getKosiceMembersUrl(),
                crawlerProperties.getKosiceMeetingsUrl(),
                crawlerProperties.getKosiceMemberDetailUrl(),
                crawlerProperties.getTimeoutMs());
    }

    private void matchVotesToMembers(Poll poll, Map<String, CouncilMember> membersMap) {
        if (poll.getVotes() == null) return;
        int matched = 0;
        for (Vote vote : poll.getVotes()) {
            if (vote.getCouncilMember() != null || vote.getVoterName() == null) continue;

            String key = PollsUtils.toSimpleNameWithoutAccents(vote.getVoterName());
            CouncilMember member = membersMap.get(key);

            if (member == null) {
                String[] parts = key.split("\\s", 2);
                if (parts.length == 2) {
                    member = membersMap.get(parts[1] + " " + parts[0]);
                }
            }

            if (member != null) {
                vote.setCouncilMember(member);
                matched++;
            }
        }
        log.debug("Matched {}/{} votes for poll '{}'", matched, poll.getVotes().size(), poll.getName());
    }
}
