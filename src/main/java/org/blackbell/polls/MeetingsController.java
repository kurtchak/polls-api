package org.blackbell.polls;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.data.repositories.*;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.*;
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
    private PollRepository pollRepository;

    @Autowired
    private CouncilMemberRepository councilMemberRepository;

    //TODO: update to match institution
    public void checkLoaded(String city, Institution institution) throws Exception {
        Town town = townRepository.findByRef(city);
        if (town == null) {
            log.info("No town with name `"+city+"`. Loading from external WebService...");
            town = new Town(city, city);
        }
        List<Season> seasons;
        if (town.getSeasons() == null || town.getSeasons(institution) == null) {
            Application.loadMeetingsData(town, institution);
            seasons = town.getSeasons(institution);
            log.info("Loaded " + (seasons != null ? seasons.size() : 0) + " seasons for `" + town.getName() + "`");
            townRepository.save(town);
            log.info(town.getName() + "`s data saved.");
        }
    }

    @JsonView(value = Views.Towns.class)
    @RequestMapping("/cities")
    public List<Town> towns() throws Exception {
        return townRepository.findAll();
    }

    @JsonView(value = Views.Seasons.class)
    @RequestMapping("/{city}/{institution}/seasons")
    public List<Season> seasons(@PathVariable(value="city") String city,
                                @PathVariable(value="institution") String institution) throws Exception {
        checkLoaded(city, Institution.valueOfDM(institution));
        return seasonRepository.findByTownAndInstitution(city, Institution.valueOfDM(institution));
    }

    @JsonView(value = Views.Meetings.class)
    @RequestMapping("/{city}/{institution}/meetings/{season}")
    public List<Meeting> meetings(@PathVariable(value="city") String city,
                                  @PathVariable(value="institution") String institution,
                                  @PathVariable(value="season") String season) throws Exception {
        checkLoaded(city, Institution.valueOfDM(institution));
        return meetingRepository.getByTownAndInstitution(city, Institution.valueOfDM(institution));
    }

    @JsonView(value = Views.Meeting.class)
    @RequestMapping("/{city}/{institution}/meeting/{ref}")
    public Meeting meeting(@PathVariable(value="city") String city,
                           @PathVariable(value="institution") String institution,
                           @PathVariable(value="ref") String ref) throws Exception {
        checkLoaded(city, Institution.valueOfDM(institution));
        return meetingRepository.getByRef(ref);
    }

    @JsonView(value = Views.CouncilMembers.class)
    @RequestMapping("/{city}/{institution}/members/{season}")
    public Collection<CouncilMember> members(@PathVariable(value="city") String city,
                                             @PathVariable(value="institution") String institution,
                                             @PathVariable(value="season") String season) throws Exception {
        checkLoaded(city, Institution.valueOfDM(institution));
        return councilMemberRepository.getByTownAndSeasonAndInstitution(city, season, Institution.valueOfDM(institution));
    }

    @JsonView(value = Views.CouncilMember.class)
    @RequestMapping("/{city}/{institution}/member/{ref}")
    public CouncilMember member(@PathVariable(value="city") String city,
                                @PathVariable(value="institution") String institution,
                                @PathVariable(value="ref") String ref) throws Exception {
        checkLoaded(city, Institution.valueOfDM(institution));
        return councilMemberRepository.findByRef(ref);
    }

    @JsonView(value = Views.Polls.class)
    @RequestMapping("/{city}/{institution}/polls/{season}")
    public Collection<Poll> polls(@PathVariable(value = "city") String city,
                                  @PathVariable(value = "institution") String institution,
                                  @PathVariable(value = "season") String season) throws Exception {
        checkLoaded(city, Institution.valueOfDM(institution));
        System.out.println(">> polls");
        Collection<Poll> polls = pollRepository.getByTownAndInstitutionAndSeason(city, Institution.valueOfDM(institution), season);
        System.out.println(">> count: " + (polls != null ? polls.size() : 0));
        return polls;
    }

    @JsonView(value = Views.Poll.class)
    @RequestMapping("/{city}/{institution}/poll/{ref}")
    public Poll poll(@PathVariable(value="city") String city,
                     @PathVariable(value="institution") String institution,
                     @PathVariable(value="ref") String ref) throws Exception {
        checkLoaded(city, Institution.valueOfDM(institution));
        return pollRepository.getByRef(ref);
    }
}