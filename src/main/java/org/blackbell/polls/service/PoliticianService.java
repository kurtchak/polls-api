package org.blackbell.polls.service;

import org.blackbell.polls.domain.model.Politician;
import org.blackbell.polls.domain.repositories.PoliticianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PoliticianService {
    private static final Logger log = LoggerFactory.getLogger(PoliticianService.class);

    private final PoliticianRepository politicianRepository;

    public PoliticianService(PoliticianRepository politicianRepository) {
        this.politicianRepository = politicianRepository;
    }

    public List<Politician> getPoliticiansByTown(String town) {
        log.info("Getting politicians for town: {}", town);
        return politicianRepository.findByTown(town);
    }

    public Politician getPoliticianByName(String name) {
        log.info("Getting politician by name: {}", name);
        return politicianRepository.findByNameWithHistory(name).orElse(null);
    }

    public List<Politician> getPartySwitchers() {
        log.info("Getting party switchers (prezliekači)");
        List<Politician> switchers = politicianRepository.findPartySwitchers();
        log.info("Found {} party switchers", switchers.size());
        return switchers;
    }

    public List<Politician> getPartySwitchersByTown(String town) {
        log.info("Getting party switchers for town: {}", town);
        List<Politician> switchers = politicianRepository.findPartySwitchersByTown(town);
        log.info("Found {} party switchers for {}", switchers.size(), town);
        return switchers;
    }

    public List<Politician> getClubSwitchers() {
        log.info("Getting club switchers");
        List<Politician> switchers = politicianRepository.findClubSwitchers();
        log.info("Found {} club switchers", switchers.size());
        return switchers;
    }

    public List<Politician> getClubSwitchersByTown(String town) {
        log.info("Getting club switchers for town: {}", town);
        List<Politician> switchers = politicianRepository.findClubSwitchersByTown(town);
        log.info("Found {} club switchers for {}", switchers.size(), town);
        return switchers;
    }
}
