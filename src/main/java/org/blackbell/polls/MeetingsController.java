package org.blackbell.polls;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.data.repositories.*;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.*;
import org.blackbell.polls.meetings.source.SyncAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

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
    private SyncAgent syncAgent;

    @JsonView(value = Views.Towns.class)
    @RequestMapping("/cities")
    public List<Town> towns() throws Exception {
        return townRepository.findAll();
    }

    @JsonView(value = Views.Seasons.class)
    @RequestMapping("/{city}/{institution}/seasons")
    public List<Season> seasons(@PathVariable(value="city") String city,
                                @PathVariable(value="institution") String institution) throws Exception {
//        syncAgent.syncSeasons();
        return seasonRepository.findByTown(city);
    }

    @JsonView(value = Views.Meetings.class)
    @RequestMapping("/{city}/{institution}/meetings/{season}")
    public List<Meeting> meetings(@PathVariable(value="city") String city,
                                  @PathVariable(value="institution") String institution,
                                  @PathVariable(value="season") String season) throws Exception {
        syncAgent.syncMeetings(city, institution, season);
        return meetingRepository.getByTownAndInstitutionAndSeason(city, Institution.valueOfDM(institution), season);
    }

    @JsonView(value = Views.Meeting.class)
    @RequestMapping("/{city}/{institution}/meeting/{ref}")
    public Meeting meeting(@PathVariable(value="city") String city,
                           @PathVariable(value="institution") String institution,
                           @PathVariable(value="ref") String ref) throws Exception {
        return meetingRepository.getByRef(ref);
    }

    @JsonView(value = Views.CouncilMembers.class)
    @RequestMapping("/{city}/{institution}/members/{season}")
    public Collection<CouncilMember> members(@PathVariable(value="city") String city,
                                             @PathVariable(value="institution") String institution,
                                             @PathVariable(value="season") String season) throws Exception {
//TODO:        checkLoaded(city, Institution.valueOfDM(institution));
        return councilMemberRepository.getByTownAndSeasonAndInstitution(city, season, Institution.valueOfDM(institution));
    }

    @JsonView(value = Views.CouncilMember.class)
    @RequestMapping("/{city}/{institution}/member/{ref}")
    public CouncilMember member(@PathVariable(value="city") String city,
                                @PathVariable(value="institution") String institution,
                                @PathVariable(value="ref") String ref) throws Exception {
        return councilMemberRepository.findByRef(ref);
    }

    @JsonView(value = Views.Polls.class)
    @RequestMapping("/{city}/{institution}/polls/{season}")
    public Collection<Poll> polls(@PathVariable(value = "city") String city,
                                  @PathVariable(value = "institution") String institution,
                                  @PathVariable(value = "season") String season) throws Exception {
        //checkLoaded(city, Institution.valueOfDM(institution));
        return pollRepository.getByTownAndInstitutionAndSeason(city, Institution.valueOfDM(institution), season);
    }

    @JsonView(value = Views.Poll.class)
    @RequestMapping("/{city}/{institution}/poll/{ref}")
    public Poll poll(@PathVariable(value="city") String city,
                     @PathVariable(value="institution") String institution,
                     @PathVariable(value="ref") String ref) throws Exception {
//        checkLoaded(city, Institution.valueOfDM(institution));
        return pollRepository.getByRef(ref);
    }

    @JsonView(value = Views.Agenda.class)
    @RequestMapping("/{city}/{institution}/{meeting_ref}/agenda")
    public Collection<AgendaItem> agenda(@PathVariable(value = "city") String city,
                                          @PathVariable(value = "institution") String institution,
                                          @PathVariable(value = "meeting_ref") String meetingRef) throws Exception {
        //checkLoaded(city, Institution.valueOfDM(institution));
        return agendaRepository.getByMeeting(meetingRef);
    }

    @JsonView(value = Views.AgendaItem.class)
    @RequestMapping("/{city}/{institution}/agenda/{ref}")
    public AgendaItem agendaItem(@PathVariable(value="city") String city,
                     @PathVariable(value="institution") String institution,
                     @PathVariable(value="ref") String ref) throws Exception {
        //checkLoaded(city, Institution.valueOfDM(institution));
        return agendaRepository.getByRef(ref);
    }

    private void checkDataLoaded() throws Exception {
        if (DataContext.getTowns() == null) {
            List<Town> towns = townRepository.findAll();
            DataContext.addTowns(towns);
            for (Town town : towns) {
                for (Season season : town.getSeasons()) {
                    for (CouncilMember member : season.getMembers()) {
                        DataContext.addMember(season, member);
                    }
                }
            }
        }
    }

}