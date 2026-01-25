package org.blackbell.polls.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Politician;
import org.blackbell.polls.domain.repositories.PoliticianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for politician-related endpoints.
 * Supports tracking politicians across electoral seasons ("prezliekači").
 */
@RestController
@RequestMapping("/politicians")
public class PoliticiansController {
    private static final Logger log = LoggerFactory.getLogger(PoliticiansController.class);

    private final PoliticianRepository politicianRepository;

    public PoliticiansController(PoliticianRepository politicianRepository) {
        this.politicianRepository = politicianRepository;
    }

    /**
     * Get all politicians who have been council members in a specific town.
     */
    @JsonView(Views.CouncilMember.class)
    @GetMapping("/{town}")
    public List<Politician> getPoliticiansByTown(@PathVariable String town) {
        log.info("Getting politicians for town: {}", town);
        return politicianRepository.findByTown(town);
    }

    /**
     * Get politician by name with full history (all seasons, parties, clubs).
     */
    @JsonView(Views.CouncilMember.class)
    @GetMapping("/name/{name}")
    public Politician getPoliticianByName(@PathVariable String name) {
        log.info("Getting politician by name: {}", name);
        return politicianRepository.findByNameWithHistory(name).orElse(null);
    }

    /**
     * Get "prezliekači" - politicians who changed political parties between seasons.
     * Useful for tracking political loyalty/opportunism.
     */
    @JsonView(Views.CouncilMember.class)
    @GetMapping("/party-switchers")
    public List<Politician> getPartySwitchers() {
        log.info("Getting party switchers (prezliekači)");
        List<Politician> switchers = politicianRepository.findPartySwitchers();
        log.info("Found {} party switchers", switchers.size());
        return switchers;
    }

    /**
     * Get politicians who changed clubs between seasons.
     */
    @JsonView(Views.CouncilMember.class)
    @GetMapping("/club-switchers")
    public List<Politician> getClubSwitchers() {
        log.info("Getting club switchers");
        List<Politician> switchers = politicianRepository.findClubSwitchers();
        log.info("Found {} club switchers", switchers.size());
        return switchers;
    }
}