package org.blackbell.polls.controllers;

/**
 * Created by Ján Korčák on 21.4.2018.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Season;
import org.blackbell.polls.domain.repositories.SeasonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SeasonsController {
    private static final Logger log = LoggerFactory.getLogger(SeasonsController.class);

    private SeasonRepository seasonRepository;

    public SeasonsController(SeasonRepository seasonRepository) {
        this.seasonRepository = seasonRepository;
    }

    @JsonView(value = Views.Seasons.class)
    @RequestMapping("/{city}/{institution}/seasons")
    public List<Season> seasons(@PathVariable(value="city") String city,
                                @PathVariable(value="institution") String institution) throws Exception {
        return seasonRepository.findAll(); // TODO: by meetings
    }
}
