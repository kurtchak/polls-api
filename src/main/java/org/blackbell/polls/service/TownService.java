package org.blackbell.polls.service;

import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.repositories.TownRepository;
import org.blackbell.polls.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TownService {
    private static final Logger log = LoggerFactory.getLogger(TownService.class);

    private final TownRepository townRepository;

    public TownService(TownRepository townRepository) {
        this.townRepository = townRepository;
    }

    public List<Town> getAllTowns() {
        return townRepository.findAll();
    }

    public Town findByRef(String ref) {
        return townRepository.findByRef(ref);
    }

    @Transactional
    public Town addTown(String ref, String name, String source) {
        Town town = new Town();
        town.setRef(ref);
        town.setName(name);
        town.setSource(source != null ? Source.valueOf(source) : Source.DM);
        townRepository.save(town);
        log.info("Added new town: {}", town);
        return town;
    }
}
