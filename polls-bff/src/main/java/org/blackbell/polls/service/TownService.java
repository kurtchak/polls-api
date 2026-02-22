package org.blackbell.polls.service;

import org.blackbell.polls.domain.model.Season;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.repositories.SeasonRepository;
import org.blackbell.polls.domain.repositories.TownRepository;
import org.blackbell.polls.domain.model.enums.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TownService {
    private static final Logger log = LoggerFactory.getLogger(TownService.class);

    private static final String CURRENT_SEASON = "2022-2026";

    private final TownRepository townRepository;
    private final SeasonRepository seasonRepository;

    public TownService(TownRepository townRepository, SeasonRepository seasonRepository) {
        this.townRepository = townRepository;
        this.seasonRepository = seasonRepository;
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

        // Link current season
        Season currentSeason = seasonRepository.findByRef(CURRENT_SEASON);
        if (currentSeason == null) {
            currentSeason = new Season();
            currentSeason.setRef(CURRENT_SEASON);
            currentSeason.setName(CURRENT_SEASON);
            seasonRepository.save(currentSeason);
        }
        town.addSeason(currentSeason);

        townRepository.save(town);
        log.info("Added new town: {} with season {}", town, CURRENT_SEASON);
        return town;
    }
}
