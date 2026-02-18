package org.blackbell.polls.domain.repositories;

import org.blackbell.polls.domain.model.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

    List<SyncLog> findByTownRefOrderByTimestampDesc(String townRef);

    List<SyncLog> findByTownRefAndSeasonRefOrderByTimestampDesc(String townRef, String seasonRef);

    List<SyncLog> findBySuccessTrueOrderByTownRefAscSeasonRefAsc();
}
