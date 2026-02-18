package org.blackbell.polls.controllers;

import org.blackbell.polls.domain.model.SyncLog;
import org.blackbell.polls.domain.repositories.SyncLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/sync-log")
public class AdminSyncLogController {

    private final SyncLogRepository syncLogRepository;

    public AdminSyncLogController(SyncLogRepository syncLogRepository) {
        this.syncLogRepository = syncLogRepository;
    }

    @GetMapping
    public List<SyncLog> getAll() {
        return syncLogRepository.findAll();
    }

    @GetMapping("/{town}")
    public List<SyncLog> getByTown(@PathVariable String town) {
        return syncLogRepository.findByTownRefOrderByTimestampDesc(town);
    }

    @GetMapping("/{town}/{season}")
    public List<SyncLog> getByTownAndSeason(@PathVariable String town, @PathVariable String season) {
        return syncLogRepository.findByTownRefAndSeasonRefOrderByTimestampDesc(town, season);
    }
}
