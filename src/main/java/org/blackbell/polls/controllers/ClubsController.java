package org.blackbell.polls.controllers;

/**
 * Created by Ján Korčák on 21.4.2018.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Club;
import org.blackbell.polls.domain.repositories.ClubRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

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
