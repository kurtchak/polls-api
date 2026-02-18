package org.blackbell.polls.source;

import org.blackbell.polls.domain.model.SyncLog;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.SyncLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class SyncLogService {

    private final SyncLogRepository syncLogRepository;

    public SyncLogService(SyncLogRepository syncLogRepository) {
        this.syncLogRepository = syncLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(String townRef, String seasonRef, InstitutionType institution,
                           DataOperation operation, Source source, int recordCount, long durationMs) {
        SyncLog entry = new SyncLog();
        entry.setTownRef(townRef);
        entry.setSeasonRef(seasonRef);
        entry.setInstitution(institution);
        entry.setOperation(operation);
        entry.setSource(source);
        entry.setSuccess(true);
        entry.setRecordCount(recordCount);
        entry.setDurationMs(durationMs);
        entry.setTimestamp(Instant.now());
        syncLogRepository.save(entry);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(String townRef, String seasonRef, InstitutionType institution,
                           DataOperation operation, Source source, String errorMessage, long durationMs) {
        SyncLog entry = new SyncLog();
        entry.setTownRef(townRef);
        entry.setSeasonRef(seasonRef);
        entry.setInstitution(institution);
        entry.setOperation(operation);
        entry.setSource(source);
        entry.setSuccess(false);
        entry.setErrorMessage(errorMessage != null && errorMessage.length() > 2000
                ? errorMessage.substring(0, 2000) : errorMessage);
        entry.setDurationMs(durationMs);
        entry.setTimestamp(Instant.now());
        syncLogRepository.save(entry);
    }
}
