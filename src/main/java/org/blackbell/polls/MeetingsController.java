package org.blackbell.polls;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.common.Constants;
import org.blackbell.polls.data.repositories.*;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.*;
import org.blackbell.polls.meetings.model.vote.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class MeetingsController {
    private static final Logger log = LoggerFactory.getLogger(MeetingsController.class);

    @Autowired
    private TownRepository townRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private CouncilMemberRepository councilMemberRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private VoteRepository voteRepository;

    @JsonView(value = Views.Towns.class)
    @RequestMapping("/cities")
    public List<Town> towns() throws Exception {
        return townRepository.findAll();

    }

    @JsonView(value = Views.Seasons.class)
    @RequestMapping("/{city}/{institution}/seasons")
    public List<Season> seasons(@PathVariable(value="city") String city,
                                @PathVariable(value="institution") String institution) throws Exception {
        return seasonRepository.findByTown(city);
    }

    @JsonView(value = Views.Meetings.class)
    @RequestMapping({"/{city}/{institution}/{season}/meetings",
                     "/{city}/{institution}/{season}/meetings/{dateFrom}/{dateTo}",
                     "/{city}/{institution}/meetings/{season}/{dateFrom}/{dateTo}"})
    public List<Meeting> meetings(@PathVariable(value="city") String city,
                                  @PathVariable(value="institution") String institution,
                                  @PathVariable(value="season") String season,
                                  @PathVariable(value="dateFrom", required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateFrom,
                                  @PathVariable(value="dateTo", required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateTo) throws Exception {
        return meetingRepository.getByTownAndInstitutionAndSeason(city, Institution.valueOfDM(institution), season, dateFrom, dateTo);
    }

    @JsonView(value = Views.CouncilMembers.class)
    @RequestMapping({"/{city}/{institution}/{season}/members",
                     "/{city}/{institution}/members/{season}"})
    public Collection<CouncilMember> members(@PathVariable(value="city") String city,
                                             @PathVariable(value="institution") String institution,
                                             @PathVariable(value="season") String season) throws Exception {
        return councilMemberRepository.getByTownAndSeasonAndInstitution(city, season, Institution.valueOfDM(institution));
    }

    @JsonView(value = Views.Polls.class)
    @RequestMapping({"/{city}/{institution}/{season}/polls",
                     "/{city}/{institution}/{season}/polls/{dateFrom}/{dateTo}",
                     "/{city}/{institution}/polls/{season}/{dateFrom}/{dateTo}"})
    public Collection<Poll> polls(@PathVariable(value = "city") String city,
                                  @PathVariable(value = "institution") String institution,
                                  @PathVariable(value = "season") String season,
                                  @PathVariable(value = "dateFrom", required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateFrom,
                                  @PathVariable(value = "dateTo", required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateTo) throws Exception {
        return pollRepository.getByTownAndSeasonAndInstitution(city, season, Institution.valueOfDM(institution), dateFrom, dateTo);
    }

    @JsonView(value = Views.Meeting.class)
    @RequestMapping({"/meetings/{ref}",
                     "/{city}/{institution}/meeting/{ref}"})
    public Meeting meeting(@PathVariable(value="ref") String ref) throws Exception {
        return meetingRepository.getByRef(ref);
    }

    @JsonView(value = Views.CouncilMember.class)
    @RequestMapping({"/members/{ref}",
                     "/{city}/{institution}/member/{ref}"})
    public CouncilMember member(@PathVariable(value="ref") String ref) throws Exception {
        return councilMemberRepository.findByRef(ref);
    }

    @JsonView(value = Views.Votes.class)
    @RequestMapping({"/members/{ref}/votes",
                     "/{city}/{institution}/member/{ref}/votes"})
    public List<Vote> memberVotes(@PathVariable(value="ref") String memberRef) throws Exception {
        return voteRepository.findByCouncilMemberRef(memberRef);
    }

    @JsonView(value = Views.Poll.class)
    @RequestMapping({"/polls/{ref}",
                     "/{city}/{institution}/poll/{ref}"})
    public Poll poll(@PathVariable(value="ref") String ref) throws Exception {
        return pollRepository.getByRef(ref);
    }

    @JsonView(value = Views.Agenda.class)
    @RequestMapping({"/meetings/{meeting_ref}/agenda",
                     "/{city}/{institution}/meeting/{meeting_ref}/agenda"})
    public Collection<AgendaItem> agenda(@PathVariable(value = "meeting_ref") String meetingRef) throws Exception {
        return agendaRepository.getByMeeting(meetingRef);
    }

    @JsonView(value = Views.AgendaItem.class)
    @RequestMapping({"/agenda/{ref}",
                     "/{city}/{institution}/agenda/{ref}"})
    public AgendaItem agendaItem(@PathVariable(value="ref") String ref) throws Exception {
        return agendaRepository.getByRef(ref);
    }

    @JsonView(value = Views.Clubs.class)
    @RequestMapping("/{city}/{season}/clubs")
    public Collection<Club> clubs(@PathVariable(value="city") String city,
                                  @PathVariable(value="season") String season) throws Exception {
        return clubRepository.getByTownAndSeason(city, season);
    }

    @JsonView(value = Views.Club.class)
    @RequestMapping("/clubs/{ref}")
    public Club club(@PathVariable(value="ref") String ref) throws Exception {
        return clubRepository.findByRef(ref);
    }

    @JsonView(value = Views.ClubMembers.class)
    @RequestMapping("/clubs/{ref}/members")
    public Collection<ClubMember> clubMembers(@PathVariable(value="ref") String ref) throws Exception {
        return clubRepository.getClubMembersByClubRef(ref);
    }

    @JsonView(value = Views.Parties.class)
    @RequestMapping("/{city}/{season}/parties")
    public Collection<Party> parties(@PathVariable(value="city") String city,
                                     @PathVariable(value="season") String season) throws Exception {
        return partyRepository.getByTownAndSeasonAndInstitution(city, season);
    }

    @JsonView(value = Views.PartyNominees.class)
    @RequestMapping("/{city}/{season}/parties/{ref}/members")
    public Collection<PartyNominee> partyNominees(@PathVariable(value="city") String city,
                                                  @PathVariable(value="season") String season,
                                                  @PathVariable(value="ref") String ref) throws Exception {
        return partyRepository.getPartyNomineesByTownAndSeason(city, season, ref);
    }
}
