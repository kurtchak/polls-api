package org.blackbell.polls.controllers;

/**
 * Created by Ján Korčák on 21.4.2018.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.repositories.TownRepository;
import org.blackbell.polls.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cities")
public class TownsController {
    private static final Logger log = LoggerFactory.getLogger(TownsController.class);

    private final TownRepository townRepository;

    public TownsController(TownRepository townRepository) {
        this.townRepository = townRepository;
    }

    @JsonView(value = Views.Towns.class)
    @RequestMapping({"/",""})
    public List<Town> towns() throws Exception {
        return townRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> addTown(@RequestBody Map<String, String> body) {
        String ref = body.get("ref");
        String name = body.get("name");
        String source = body.get("source");

        if (ref == null || ref.isBlank() || name == null || name.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Both 'ref' and 'name' are required"));
        }

        if (townRepository.findByRef(ref) != null) {
            return ResponseEntity.status(409)
                    .body(Map.of("error", "Town with ref '" + ref + "' already exists"));
        }

        Town town = new Town();
        town.setRef(ref);
        town.setName(name);
        town.setSource(source != null ? Source.valueOf(source) : Source.DM);
        townRepository.save(town);
        log.info("Added new town: {}", town);

        return ResponseEntity.ok(Map.of(
                "status", "created",
                "ref", town.getRef(),
                "name", town.getName(),
                "source", town.getSource().name()
        ));
    }
}
