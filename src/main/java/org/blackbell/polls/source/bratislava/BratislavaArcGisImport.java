package org.blackbell.polls.source.bratislava;

import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.embeddable.VotesCount;
import org.blackbell.polls.domain.model.enums.VoteChoice;
import org.blackbell.polls.source.DataImport;
import org.blackbell.polls.source.bratislava.api.ArcGisVoteRecord;
import org.blackbell.polls.source.bratislava.api.BratislavaServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Import module for Bratislava ArcGIS data (periods 7 and 8: 2014-2022).
 */
@Component
public class BratislavaArcGisImport implements DataImport {

    private static final Logger log = LoggerFactory.getLogger(BratislavaArcGisImport.class);

    private static final SimpleDateFormat BA_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    private static final Map<Integer, String> PERIOD_TO_SEASON = Map.of(
            7, "2014-2018",
            8, "2018-2022"
    );

    @Override
    public List<Season> loadSeasons(Town town) throws Exception {
        log.info("Loading ArcGIS seasons for Bratislava...");
        List<ArcGisVoteRecord> records = BratislavaServiceClient.queryDistinctPeriods();
        List<Season> seasons = new ArrayList<>();
        for (ArcGisVoteRecord record : records) {
            String seasonName = PERIOD_TO_SEASON.get(record.getElectoralPeriod());
            if (seasonName != null) {
                Season season = new Season();
                season.setRef(seasonName);
                season.setName(seasonName);
                seasons.add(season);
                log.info("Found ArcGIS Bratislava season: {} (period {})", seasonName, record.getElectoralPeriod());
            }
        }
        return seasons;
    }

    @Override
    public List<Meeting> loadMeetings(Town town, Season season, Institution institution) throws Exception {
        if (!PERIOD_TO_SEASON.containsValue(season.getRef())) {
            log.debug("Season {} is not an ArcGIS Bratislava season, skipping", season.getRef());
            return null;
        }
        int period = seasonToPeriod(season.getRef());
        log.info("Loading meetings for Bratislava ArcGIS, season: {} (period {})", season.getName(), period);

        List<ArcGisVoteRecord> records = BratislavaServiceClient.queryDistinctDatesForPeriod(period);
        List<Meeting> meetings = new ArrayList<>();
        for (ArcGisVoteRecord record : records) {
            String dateStr = record.getDate();
            Meeting meeting = new Meeting();
            meeting.setName("Zasadnutie MsZ " + dateStr);
            meeting.setExtId("ba:" + period + ":" + dateStr);
            meeting.setDate(BA_DATE_FORMAT.parse(dateStr));
            meeting.setRef(PollsUtils.generateUniqueKeyReference());
            meeting.setTown(town);
            meeting.setSeason(season);
            meeting.setInstitution(institution);
            meetings.add(meeting);
        }

        log.info("Found {} meetings for Bratislava ArcGIS period {}", meetings.size(), period);
        return meetings;
    }

    @Override
    public void loadMeetingDetails(Meeting meeting, String externalMeetingId) throws Exception {
        String[] parts = externalMeetingId.split(":");
        int period = Integer.parseInt(parts[1]);
        String date = parts[2];
        log.info("Loading ArcGIS meeting details for Bratislava: period={} date={}", period, date);

        List<ArcGisVoteRecord> records = BratislavaServiceClient.queryRecordsForDate(period, date);
        if (records.isEmpty()) {
            log.warn("No records found for Bratislava ArcGIS meeting: {}", externalMeetingId);
            return;
        }

        // Group by Bod (agenda item name)
        Map<String, List<ArcGisVoteRecord>> byAgendaItem = records.stream()
                .collect(Collectors.groupingBy(
                        ArcGisVoteRecord::getAgendaItemName,
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<String, List<ArcGisVoteRecord>> entry : byAgendaItem.entrySet()) {
            String agendaItemName = entry.getKey();
            List<ArcGisVoteRecord> agendaRecords = entry.getValue();

            AgendaItem agendaItem = new AgendaItem();
            agendaItem.setName(agendaItemName);
            agendaItem.setRef(PollsUtils.generateUniqueKeyReference());
            meeting.addAgendaItem(agendaItem);

            // Group by Poradie_hlasovania (poll sequence) within this agenda item
            Map<Integer, List<ArcGisVoteRecord>> byPoll = agendaRecords.stream()
                    .collect(Collectors.groupingBy(
                            ArcGisVoteRecord::getPollSequence,
                            TreeMap::new,
                            Collectors.toList()));

            for (Map.Entry<Integer, List<ArcGisVoteRecord>> pollEntry : byPoll.entrySet()) {
                int pollSeq = pollEntry.getKey();
                List<ArcGisVoteRecord> pollRecords = pollEntry.getValue();

                Poll poll = new Poll();
                poll.setRef(PollsUtils.generateUniqueKeyReference());
                poll.setName(agendaItemName + " (hlasovanie " + pollSeq + ")");
                poll.setExtAgendaItemId("ba:" + period + ":" + date);
                poll.setExtPollRouteId(String.valueOf(pollSeq));
                poll.setVoters(pollRecords.size());
                poll.setVotesCount(countVotes(pollRecords));
                agendaItem.addPoll(poll);
            }
        }

        int agendaCount = meeting.getAgendaItems() != null ? meeting.getAgendaItems().size() : 0;
        log.info("Loaded {} agenda items for Bratislava ArcGIS meeting {}", agendaCount, date);
    }

    @Override
    public void loadPollDetails(Poll poll, Map<String, CouncilMember> membersMap) throws Exception {
        String meetingId = poll.getExtAgendaItemId();
        int pollSeq = Integer.parseInt(poll.getExtPollRouteId());
        String[] parts = meetingId.split(":");
        int period = Integer.parseInt(parts[1]);
        String date = parts[2];

        log.info("Loading ArcGIS poll details for Bratislava: date={} poll={}", date, pollSeq);
        List<ArcGisVoteRecord> records = BratislavaServiceClient.queryRecordsForPoll(period, date, pollSeq);

        Set<Vote> votes = new HashSet<>();
        for (ArcGisVoteRecord record : records) {
            String fullName = record.getFirstName() + " " + record.getLastName();
            String key = PollsUtils.toSimpleNameWithoutAccents(fullName);

            Vote vote = new Vote();
            vote.setCouncilMember(membersMap.get(key));
            vote.setPoll(poll);
            vote.setVoted(mapVoteChoice(record.getVote()));

            if (vote.getCouncilMember() == null) {
                log.warn("Council member not found for: {} (key: {})", fullName, key);
            }

            votes.add(vote);
        }
        poll.setVotes(votes);
        log.info("Loaded {} votes for ArcGIS poll {}", votes.size(), poll.getName());
    }

    @Override
    public List<CouncilMember> loadMembers(Town town, Season season, Institution institution) {
        if (!PERIOD_TO_SEASON.containsValue(season.getRef())) {
            log.debug("Season {} is not an ArcGIS Bratislava season, skipping", season.getRef());
            return null;
        }
        int period = seasonToPeriod(season.getRef());
        log.info("Loading ArcGIS council members for Bratislava, season: {} (period {})", season.getName(), period);

        try {
            List<ArcGisVoteRecord> records = BratislavaServiceClient.queryDistinctMembersForPeriod(period);
            List<CouncilMember> members = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            for (ArcGisVoteRecord record : records) {
                String fullName = record.getFirstName() + " " + record.getLastName();
                String key = PollsUtils.toSimpleNameWithoutAccents(fullName);
                if (seen.contains(key)) {
                    continue;
                }
                seen.add(key);

                Politician politician = new Politician();
                politician.setName(fullName);
                politician.setRef(PollsUtils.toSimpleNameWithoutAccents(fullName).toLowerCase().replace(" ", "-"));

                CouncilMember member = new CouncilMember();
                member.setRef(PollsUtils.generateUniqueKeyReference());
                member.setPolitician(politician);
                member.setSeason(season);
                member.setTown(town);
                member.setInstitution(institution);
                member.setDescription(record.getClub());
                members.add(member);

                log.debug("Found ArcGIS Bratislava council member: {} ({})", fullName, record.getClub());
            }

            log.info("Loaded {} distinct council members for Bratislava ArcGIS period {}", members.size(), period);
            return members;
        } catch (Exception e) {
            log.error("Error loading Bratislava ArcGIS council members: {}", e.getMessage());
            return null;
        }
    }

    static VotesCount countVotes(List<ArcGisVoteRecord> records) {
        VotesCount vc = new VotesCount();
        for (ArcGisVoteRecord record : records) {
            VoteChoice choice = mapVoteChoice(record.getVote());
            switch (choice) {
                case VOTED_FOR -> vc.setVotedFor(vc.getVotedFor() + 1);
                case VOTED_AGAINST -> vc.setVotedAgainst(vc.getVotedAgainst() + 1);
                case ABSTAIN -> vc.setAbstain(vc.getAbstain() + 1);
                case NOT_VOTED -> vc.setNotVoted(vc.getNotVoted() + 1);
                case ABSENT -> vc.setAbsent(vc.getAbsent() + 1);
            }
        }
        return vc;
    }

    static VoteChoice mapVoteChoice(String hlasovanie) {
        if (hlasovanie == null) {
            return VoteChoice.ABSENT;
        }
        return switch (hlasovanie) {
            case "ZA" -> VoteChoice.VOTED_FOR;
            case "PROTI" -> VoteChoice.VOTED_AGAINST;
            case "ZDRŽAL SA", "ZDRŽALA SA", "ZDRŽAL SA/ZDRŽALA SA" -> VoteChoice.ABSTAIN;
            case "NEHLASOVAL", "NEHLASOVALA", "NEHLASOVAL/NEHLASOVALA" -> VoteChoice.NOT_VOTED;
            case "NEPRÍTOMNÝ", "NEPRÍTOMNÁ", "NEPRÍTOMNÝ/NEPRÍTOMNÁ" -> VoteChoice.ABSENT;
            default -> {
                log.warn("Unknown vote value: '{}', defaulting to ABSENT", hlasovanie);
                yield VoteChoice.ABSENT;
            }
        };
    }

    private static int seasonToPeriod(String seasonRef) {
        for (Map.Entry<Integer, String> entry : PERIOD_TO_SEASON.entrySet()) {
            if (entry.getValue().equals(seasonRef)) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Unknown ArcGIS Bratislava season: " + seasonRef);
    }
}
