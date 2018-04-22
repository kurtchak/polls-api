package org.blackbell.polls.controllers;

/**
 * Created by Ján Korčák on 21.4.2018.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.common.Constants;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.model.Club;
import org.blackbell.polls.model.Meeting;
import org.blackbell.polls.model.Vote;
import org.blackbell.polls.model.common.BaseEntity;
import org.blackbell.polls.model.enums.InstitutionType;
import org.blackbell.polls.repositories.AgendaRepository;
import org.blackbell.polls.repositories.ClubRepository;
import org.blackbell.polls.repositories.MeetingRepository;
import org.blackbell.polls.repositories.VoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@RestController
public class ClubsController {
    private static final Logger log = LoggerFactory.getLogger(ClubsController.class);

    private ClubRepository clubRepository;

    public ClubsController(ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
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
}
