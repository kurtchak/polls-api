package org.blackbell.polls.controllers;

import org.blackbell.polls.domain.model.SyncLog;
import org.blackbell.polls.domain.repositories.MeetingRepository;
import org.blackbell.polls.domain.repositories.SyncLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminSyncLogController {

    private final SyncLogRepository syncLogRepository;
    private final MeetingRepository meetingRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AdminSyncLogController(SyncLogRepository syncLogRepository, MeetingRepository meetingRepository) {
        this.syncLogRepository = syncLogRepository;
        this.meetingRepository = meetingRepository;
    }

    @GetMapping("/sync-log")
    public List<SyncLog> getAll() {
        return syncLogRepository.findAll();
    }

    @GetMapping("/sync-log/{town}")
    public List<SyncLog> getByTown(@PathVariable String town) {
        return syncLogRepository.findByTownRefOrderByTimestampDesc(town);
    }

    @GetMapping("/sync-log/{town}/{season}")
    public List<SyncLog> getByTownAndSeason(@PathVariable String town, @PathVariable String season) {
        return syncLogRepository.findByTownRefAndSeasonRefOrderByTimestampDesc(town, season);
    }

    @Transactional
    @PostMapping("/reset-meetings/{town}/{season}")
    public ResponseEntity<Map<String, Object>> resetMeetings(@PathVariable String town, @PathVariable String season) {
        int updated = meetingRepository.resetSyncComplete(town, season);
        return ResponseEntity.ok(Map.of(
                "town", town,
                "season", season,
                "meetingsReset", updated
        ));
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/data-sources")
    public Map<String, Object> getDataSourceSummary() {
        List<Object[]> seasonStats = entityManager.createNativeQuery(
                "SELECT t.ref AS town, s.ref AS season, s.data_source AS source" +
                " FROM season s" +
                " JOIN meeting m ON m.season_id = s.id" +
                " JOIN town t ON m.town_id = t.id" +
                " GROUP BY t.ref, s.ref, s.data_source" +
                " ORDER BY t.ref, s.ref").getResultList();

        List<Object[]> meetingStats = entityManager.createNativeQuery(
                "SELECT t.ref AS town, s.ref AS season, m.data_source AS source, COUNT(*) AS cnt" +
                " FROM meeting m" +
                " JOIN town t ON m.town_id = t.id" +
                " JOIN season s ON m.season_id = s.id" +
                " GROUP BY t.ref, s.ref, m.data_source" +
                " ORDER BY t.ref, s.ref").getResultList();

        List<Object[]> memberStats = entityManager.createNativeQuery(
                "SELECT t.ref AS town, s.ref AS season, cm.data_source AS source, COUNT(*) AS cnt" +
                " FROM council_member cm" +
                " JOIN town t ON cm.town_id = t.id" +
                " JOIN season s ON cm.season_id = s.id" +
                " GROUP BY t.ref, s.ref, cm.data_source" +
                " ORDER BY t.ref, s.ref").getResultList();

        List<Object[]> pollStats = entityManager.createNativeQuery(
                "SELECT t.ref AS town, s.ref AS season, p.data_source AS source, COUNT(*) AS cnt" +
                " FROM poll p" +
                " JOIN agenda_item ai ON p.agenda_item_id = ai.id" +
                " JOIN meeting m ON ai.meeting_id = m.id" +
                " JOIN town t ON m.town_id = t.id" +
                " JOIN season s ON m.season_id = s.id" +
                " GROUP BY t.ref, s.ref, p.data_source" +
                " ORDER BY t.ref, s.ref").getResultList();

        return Map.of(
                "seasons", seasonStats.stream().map(r -> row(r[0], r[1], r[2], null)).toList(),
                "meetings", meetingStats.stream().map(r -> row(r[0], r[1], r[2], r[3])).toList(),
                "members", memberStats.stream().map(r -> row(r[0], r[1], r[2], r[3])).toList(),
                "polls", pollStats.stream().map(r -> row(r[0], r[1], r[2], r[3])).toList()
        );
    }

    private static Map<String, Object> row(Object town, Object season, Object source, Object count) {
        Map<String, Object> map = new HashMap<>();
        map.put("town", town != null ? town.toString() : null);
        map.put("season", season != null ? season.toString() : null);
        map.put("source", source != null ? source.toString() : null);
        if (count != null) {
            map.put("count", ((Number) count).longValue());
        }
        return map;
    }
}
