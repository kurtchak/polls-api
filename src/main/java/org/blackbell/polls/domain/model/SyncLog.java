package org.blackbell.polls.domain.model;

import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.source.DataOperation;
import org.blackbell.polls.source.Source;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(indexes = {
        @Index(name = "idx_sync_log_town", columnList = "townRef"),
        @Index(name = "idx_sync_log_town_season", columnList = "townRef, seasonRef"),
        @Index(name = "idx_sync_log_source", columnList = "source")
})
public class SyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String townRef;

    private String seasonRef;

    @Enumerated(EnumType.STRING)
    private InstitutionType institution;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataOperation operation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    @Column(nullable = false)
    private boolean success;

    private int recordCount;

    @Column(length = 2000)
    private String errorMessage;

    private long durationMs;

    @Column(nullable = false)
    private Instant timestamp;

    public long getId() {
        return id;
    }

    public String getTownRef() {
        return townRef;
    }

    public void setTownRef(String townRef) {
        this.townRef = townRef;
    }

    public String getSeasonRef() {
        return seasonRef;
    }

    public void setSeasonRef(String seasonRef) {
        this.seasonRef = seasonRef;
    }

    public InstitutionType getInstitution() {
        return institution;
    }

    public void setInstitution(InstitutionType institution) {
        this.institution = institution;
    }

    public DataOperation getOperation() {
        return operation;
    }

    public void setOperation(DataOperation operation) {
        this.operation = operation;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
