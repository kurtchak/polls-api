package org.blackbell.polls.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.service.TownService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cities")
public class TownsController {

    private final TownService townService;

    public TownsController(TownService townService) {
        this.townService = townService;
    }

    @JsonView(value = Views.Towns.class)
    @RequestMapping({"/",""})
    public List<Town> towns() {
        return townService.getAllTowns();
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

        if (townService.findByRef(ref) != null) {
            return ResponseEntity.status(409)
                    .body(Map.of("error", "Town with ref '" + ref + "' already exists"));
        }

        Town town = townService.addTown(ref, name, source);

        return ResponseEntity.ok(Map.of(
                "status", "created",
                "ref", town.getRef(),
                "name", town.getName(),
                "source", town.getSource().name()
        ));
    }
}
