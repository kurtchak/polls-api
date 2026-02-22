package org.blackbell.polls.integrity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DataIntegrityReport(
        Instant timestamp,
        String townFilter,
        long durationMs,
        IntegritySummary summary,
        List<DataIntegrityIssue> issues
) {
    public record IntegritySummary(
            int totalIssues,
            int errors,
            int warnings,
            int infos,
            Map<String, Integer> byCategory
    ) {}
}
