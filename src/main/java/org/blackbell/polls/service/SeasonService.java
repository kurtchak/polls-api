package org.blackbell.polls.service;

import org.blackbell.polls.domain.model.Season;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.repositories.MeetingRepository;
import org.blackbell.polls.domain.repositories.TownRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SeasonService {

    private final TownRepository townRepository;
    private final MeetingRepository meetingRepository;

    public SeasonService(TownRepository townRepository, MeetingRepository meetingRepository) {
        this.townRepository = townRepository;
        this.meetingRepository = meetingRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSeasonsWithCounts(String city) {
        Town town = townRepository.findByRefWithSeasons(city);
        if (town == null) return List.of();

        Set<Season> townSeasons = town.getSeasons();
        if (townSeasons.isEmpty()) return List.of();

        List<Object[]> counts = meetingRepository.countMeetingsAndPollsByTown(city);
        Map<String, long[]> countsMap = new HashMap<>();
        for (Object[] row : counts) {
            countsMap.put((String) row[0], new long[]{(long) row[1], (long) row[2]});
        }

        return townSeasons.stream()
                .sorted(Comparator.comparing(Season::getRef))
                .map(s -> {
                    long[] c = countsMap.getOrDefault(s.getRef(), new long[]{0, 0});
                    long incomplete = meetingRepository.countIncompleteMeetings(city, s.getRef());
                    Map<String, Object> map = new HashMap<>();
                    map.put("ref", s.getRef());
                    map.put("name", s.getName());
                    map.put("meetingCount", c[0]);
                    map.put("pollCount", c[1]);
                    map.put("incompleteMeetings", incomplete);
                    return map;
                }).toList();
    }
}
