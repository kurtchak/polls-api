package org.blackbell.polls.controllers;

/**
 * Created by Ján Korčák on 21.4.2018.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.repositories.TownRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cities")
public class TownsController {
    private static final Logger log = LoggerFactory.getLogger(TownsController.class);

    private TownRepository townRepository;

    public TownsController(TownRepository townRepository) {
        this.townRepository = townRepository;
    }

    @JsonView(value = Views.Towns.class)
    @RequestMapping({"/",""})
    public List<Town> towns() throws Exception {
        return townRepository.findAll();

    }

}
