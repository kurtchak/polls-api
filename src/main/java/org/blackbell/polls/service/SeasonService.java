package org.blackbell.polls.service;

import org.blackbell.polls.domain.model.Season;
import org.blackbell.polls.domain.repositories.MeetingRepository;
import org.blackbell.polls.domain.repositories.SeasonRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final MeetingRepository meetingRepository;

    public SeasonService(SeasonRepository seasonRepository, MeetingRepository meetingRepository) {
        this.seasonRepository = seasonRepository;
        this.meetingRepository = meetingRepository;
    }

    public List<Map<String, Object>> getSeasonsWithCounts(String city) {
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
