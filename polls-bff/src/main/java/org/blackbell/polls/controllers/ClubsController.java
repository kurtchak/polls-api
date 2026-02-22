package org.blackbell.polls.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Club;
import org.blackbell.polls.service.ClubService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class ClubsController {

    private final ClubService clubService;

    public ClubsController(ClubService clubService) {
        this.clubService = clubService;
    }

    @JsonView(value = Views.Clubs.class)
    @RequestMapping("/{city}/{season}/clubs")
    public Collection<Club> clubs(@PathVariable String city,
                                  @PathVariable String season) {
        return clubService.getClubs(city, season);
    }

    @JsonView(value = Views.Club.class)
    @RequestMapping("/clubs/{ref}")
    public Club club(@PathVariable String ref) {
        return clubService.getClubDetail(ref);
    }
}
