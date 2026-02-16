package org.blackbell.polls.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Politician;
import org.blackbell.polls.service.PoliticianService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/politicians")
public class PoliticiansController {

    private final PoliticianService politicianService;

    public PoliticiansController(PoliticianService politicianService) {
        this.politicianService = politicianService;
    }

    @JsonView(Views.CouncilMember.class)
    @GetMapping("/{town}")
    public List<Politician> getPoliticiansByTown(@PathVariable String town) {
        return politicianService.getPoliticiansByTown(town);
    }

    @JsonView(Views.CouncilMember.class)
    @GetMapping("/name/{name}")
    public Politician getPoliticianByName(@PathVariable String name) {
        return politicianService.getPoliticianByName(name);
    }

    @JsonView(Views.CouncilMember.class)
    @GetMapping("/party-switchers")
    public List<Politician> getPartySwitchers() {
        return politicianService.getPartySwitchers();
    }

    @JsonView(Views.CouncilMember.class)
    @GetMapping("/{town}/party-switchers")
    public List<Politician> getPartySwitchersByTown(@PathVariable String town) {
        return politicianService.getPartySwitchersByTown(town);
    }

    @JsonView(Views.CouncilMember.class)
    @GetMapping("/club-switchers")
    public List<Politician> getClubSwitchers() {
        return politicianService.getClubSwitchers();
    }

    @JsonView(Views.CouncilMember.class)
    @GetMapping("/{town}/club-switchers")
    public List<Politician> getClubSwitchersByTown(@PathVariable String town) {
        return politicianService.getClubSwitchersByTown(town);
    }
}