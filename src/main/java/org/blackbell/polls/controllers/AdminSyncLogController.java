package org.blackbell.polls.controllers;

import org.blackbell.polls.domain.model.SyncLog;
import org.blackbell.polls.domain.repositories.SyncLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminSyncLogController {

    private final SyncLogRepository syncLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AdminSyncLogController(SyncLogRepository syncLogRepository) {
        this.syncLogRepository = syncLogRepository;
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
                "seasons", seasonStats.stream().map(r -> Map.of(
                        "town", str(r[0]), "season", str(r[1]), "source", str(r[2]))).toList(),
                "meetings", meetingStats.stream().map(r -> Map.of(
                        "town", str(r[0]), "season", str(r[1]), "source", str(r[2]), "count", ((Number) r[3]).longValue())).toList(),
                "members", memberStats.stream().map(r -> Map.of(
                        "town", str(r[0]), "season", str(r[1]), "source", str(r[2]), "count", ((Number) r[3]).longValue())).toList(),
                "polls", pollStats.stream().map(r -> Map.of(
                        "town", str(r[0]), "season", str(r[1]), "source", str(r[2]), "count", ((Number) r[3]).longValue())).toList()
        );
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }
}
