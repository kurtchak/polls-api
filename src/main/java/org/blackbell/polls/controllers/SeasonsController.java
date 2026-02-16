package org.blackbell.polls.controllers;

import org.blackbell.polls.service.SeasonService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SeasonsController {

    private final SeasonService seasonService;

    public SeasonsController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @RequestMapping("/{city}/{institution}/seasons")
    public List<Map<String, Object>> seasons(@PathVariable String city,
                                              @PathVariable String institution) {
        return seasonService.getSeasonsWithCounts(city);
    }
}
