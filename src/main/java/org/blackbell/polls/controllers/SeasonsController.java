package org.blackbell.polls.controllers;

/**
 * Created by Ján Korčák on 21.4.2018.
 * email: korcak@esten.sk
 */

import org.blackbell.polls.domain.model.Season;
import org.blackbell.polls.domain.repositories.MeetingRepository;
import org.blackbell.polls.domain.repositories.SeasonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SeasonsController {
    private static final Logger log = LoggerFactory.getLogger(SeasonsController.class);

    private final SeasonRepository seasonRepository;
    private final MeetingRepository meetingRepository;

    public SeasonsController(SeasonRepository seasonRepository, MeetingRepository meetingRepository) {
        this.seasonRepository = seasonRepository;
        this.meetingRepository = meetingRepository;
    }

    @RequestMapping("/{city}/{institution}/seasons")
    public List<Map<String, Object>> seasons(@PathVariable(value="city") String city,
                                              @PathVariable(value="institution") String institution) {
        List<Season> seasons = seasonRepository.findAll();
        List<Object[]> counts = meetingRepository.countMeetingsAndPollsByTown(city);

        Map<String, long[]> countsMap = new HashMap<>();
        for (Object[] row : counts) {
            countsMap.put((String) row[0], new long[]{(long) row[1], (long) row[2]});
        }

        return seasons.stream().map(s -> {
            long[] c = countsMap.getOrDefault(s.getRef(), new long[]{0, 0});
            Map<String, Object> map = new HashMap<>();
            map.put("ref", s.getRef());
            map.put("name", s.getName());
            map.put("meetingCount", c[0]);
            map.put("pollCount", c[1]);
            return map;
        }).toList();
    }
}
