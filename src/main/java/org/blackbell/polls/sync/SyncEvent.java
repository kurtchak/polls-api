package org.blackbell.polls.sync;

import java.time.Instant;

public record SyncEvent(
        long id,
        Instant timestamp,
        String level,   // "INFO", "WARN", "ERROR", "SUCCESS"
        String town,    // nullable
        String season,  // nullable
        String phase,   // "start", "seasons", "members", "meetings", "meeting", "complete", "error"
        String message
) {}
