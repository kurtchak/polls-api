package org.blackbell.polls.integrity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.config.IntegrityProperties;
import org.blackbell.polls.integrity.DataIntegrityIssue.EntityRef;
import org.blackbell.polls.integrity.DataIntegrityIssue.Severity;
import org.blackbell.polls.integrity.DataIntegrityReport.IntegritySummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataIntegrityService {

    @PersistenceContext
    private EntityManager em;

    private final IntegrityProperties integrityProperties;

    public DataIntegrityService(IntegrityProperties integrityProperties) {
        this.integrityProperties = integrityProperties;
    }

    @Transactional(readOnly = true)
    public DataIntegrityReport runAllChecks(String townFilter) {
        long start = System.currentTimeMillis();

        List<DataIntegrityIssue> issues = new ArrayList<>();
        issues.addAll(checkMeetingWrongSeason(townFilter));
        issues.addAll(checkDuplicateMeeting(townFilter));
        issues.addAll(checkGhostCouncilMember(townFilter));
        issues.addAll(checkNonPersonName(townFilter));
        issues.addAll(checkCrossSeasonVotes(townFilter));
        issues.addAll(checkDuplicatePolitician(townFilter));
        issues.addAll(checkMissingPhoto(townFilter));
        issues.addAll(checkOrphanCouncilMember(townFilter));
        issues.addAll(checkMemberCountMismatch(townFilter));

        long durationMs = System.currentTimeMillis() - start;

        Map<String, Integer> byCategory = issues.stream()
                .collect(Collectors.groupingBy(DataIntegrityIssue::category,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        IntegritySummary summary = new IntegritySummary(
                issues.size(),
                (int) issues.stream().filter(i -> i.severity() == Severity.ERROR).count(),
                (int) issues.stream().filter(i -> i.severity() == Severity.WARNING).count(),
                (int) issues.stream().filter(i -> i.severity() == Severity.INFO).count(),
                byCategory
        );

        return new DataIntegrityReport(Instant.now(), townFilter, durationMs, summary, issues);
    }

    @SuppressWarnings("unchecked")
    private List<DataIntegrityIssue> checkMeetingWrongSeason(String townFilter) {
        String sql = "SELECT m.id, m.ref, m.name, m.date, s.name, s.ref, t.ref" +
                " FROM meeting m" +
                " JOIN season s ON m.season_id = s.id" +
                " JOIN town t ON m.town_id = t.id" +
                " WHERE s.name ~ '^\\d{4}-\\d{4}$'" +
                "   AND (EXTRACT(YEAR FROM m.date) < CAST(SPLIT_PART(s.name, '-', 1) AS INTEGER)" +
                "    OR EXTRACT(YEAR FROM m.date) >= CAST(SPLIT_PART(s.name, '-', 2) AS INTEGER))";
        if (townFilter != null) sql += " AND t.ref = :town";

        var q = em.createNativeQuery(sql);
        if (townFilter != null) q.setParameter("town", townFilter);

        List<Object[]> rows = q.getResultList();
        return rows.stream().map(r -> new DataIntegrityIssue(
                Severity.ERROR,
                "MEETING_WRONG_SEASON",
                str(r[6]),
                str(r[5]),
                String.format("Meeting '%s' (date %s) is outside season %s", r[2], r[3], r[4]),
                new EntityRef("Meeting", num(r[0]), str(r[1]), str(r[2]))
        )).toList();
    }

    @SuppressWarnings("unchecked")
    private List<DataIntegrityIssue> checkDuplicateMeeting(String townFilter) {
        String sql = "SELECT t.ref, m.date, COUNT(DISTINCT m.season_id) AS season_count," +
                " STRING_AGG(DISTINCT s.name, ', ' ORDER BY s.name) AS seasons" +
                " FROM meeting m" +
                " JOIN town t ON m.town_id = t.id" +
                " JOIN season s ON m.season_id = s.id";
        if (townFilter != null) sql += " WHERE t.ref = :town";
        sql += " GROUP BY t.ref, m.date HAVING COUNT(DISTINCT m.season_id) > 1";

        var q = em.createNativeQuery(sql);
        if (townFilter != null) q.setParameter("town", townFilter);

        List<Object[]> rows = q.getResultList();
        return rows.stream().map(r -> new DataIntegrityIssue(
                Severity.ERROR,
                "DUPLICATE_MEETING",
                str(r[0]),
                null,
                String.format("Meeting on %s exists in %s seasons: %s", r[1], r[2], r[3]),
                null
        )).toList();
    }

    @SuppressWarnings("unchecked")
    private List<DataIntegrityIssue> checkGhostCouncilMember(String townFilter) {
        String sql = "SELECT cm.id, cm.ref, p.name, s.name, t.ref, cm.data_source" +
                " FROM council_member cm" +
                " JOIN politician p ON cm.politician_id = p.id" +
                " JOIN season s ON cm.season_id = s.id" +
                " JOIN town t ON cm.town_id = t.id" +
                " LEFT JOIN vote v ON v.council_member_id = cm.id" +
                " WHERE v.id IS NULL";
        if (townFilter != null) sql += " AND t.ref = :town";

        var q = em.createNativeQuery(sql);
        if (townFilter != null) q.setParameter("town", townFilter);

        List<Object[]> rows = q.getResultList();
        return rows.stream().map(r -> new DataIntegrityIssue(
                Severity.WARNING,
                "GHOST_COUNCIL_MEMBER",
                str(r[4]),
                str(r[3]),
                String.format("Council member '%s' has 0 votes (source: %s)", r[2], r[5]),
                new EntityRef("CouncilMember", num(r[0]), str(r[1]), str(r[2]))
        )).toList();
    }

    @SuppressWarnings("unchecked")
    private List<DataIntegrityIssue> checkNonPersonName(String townFilter) {
        String sql = "SELECT DISTINCT p.id, p.ref, p.name, t.ref, s.name" +
                " FROM politician p" +
                " JOIN council_member cm ON cm.politician_id = p.id" +
                " JOIN town t ON cm.town_id = t.id" +
                " JOIN season s ON cm.season_id = s.id" +
                " WHERE p.name NOT LIKE '%% %%'" +
                "    OR LENGTH(p.name) > 60" +
                "    OR LOWER(p.name) SIMILAR TO '%%(mapa|mesta|uznesenie|http|www|oznámenie|zápisnica|správa|výsledok)%%'";
        if (townFilter != null) sql += " AND t.ref = :town";

        var q = em.createNativeQuery(sql);
        if (townFilter != null) q.setParameter("town", townFilter);

        List<Object[]> rows = q.getResultList();
        return rows.stream().map(r -> new DataIntegrityIssue(
                Severity.ERROR,
                "NON_PERSON_NAME",
                str(r[3]),
                str(r[4]),
                String.format("Politician name '%s' does not look like a person name", r[2]),
                new EntityRef("Politician", num(r[0]), str(r[1]), str(r[2]))
        )).toList();
    }

    @SuppressWarnings("unchecked")
    private List<DataIntegrityIssue> checkCrossSeasonVotes(String townFilter) {
        String sql = "SELECT cm.id, cm.ref, p.name, s_cm.name AS cm_season, s_m.name AS meeting_season," +
                " t.ref, COUNT(*) AS vote_count" +
                " FROM vote v" +
                " JOIN council_member cm ON v.council_member_id = cm.id" +
                " JOIN politician p ON cm.politician_id = p.id" +
                " JOIN season s_cm ON cm.season_id = s_cm.id" +
                " JOIN poll pl ON v.poll_id = pl.id" +
                " JOIN agenda_item ai ON pl.agenda_item_id = ai.id" +
                " JOIN meeting m ON ai.meeting_id = m.id" +
                " JOIN season s_m ON m.season_id = s_m.id" +
                " JOIN town t ON cm.town_id = t.id" +
                " WHERE s_cm.id <> s_m.id";
        if (townFilter != null) sql += " AND t.ref = :town";
        sql += " GROUP BY cm.id, cm.ref, p.name, s_cm.name, s_m.name, t.ref";

        var q = em.createNativeQuery(sql);
        if (townFilter != null) q.setParameter("town", townFilter);

        List<Object[]> rows = q.getResultList();
        return rows.stream().map(r -> new DataIntegrityIssue(
                Severity.ERROR,
                "CROSS_SEASON_VOTES",
                str(r[5]),
                str(r[3]),
                String.format("Member '%s' (season %s) has %s votes in meetings of season %s",
                        r[2], r[3], r[6], r[4]),
                new EntityRef("CouncilMember", num(r[0]), str(r[1]), str(r[2]))
        )).toList();
    }

    @SuppressWarnings("unchecked")
    private List<DataIntegrityIssue> checkDuplicatePolitician(String townFilter) {
        String sql = "SELECT p.id, p.ref, p.name FROM politician p";
        if (townFilter != null) {
            sql += " JOIN council_member cm ON cm.politician_id = p.id" +
                   " JOIN town t ON cm.town_id = t.id" +
                   " WHERE t.ref = :town";
        }

        var q = em.createNativeQuery(sql);
        if (townFilter != null) q.setParameter("town", townFilter);

        List<Object[]> rows = q.getResultList();

        // Build map: normalized name -> list of (id, ref, originalName)
        Map<String, List<Object[]>> byNormalized = new HashMap<>();
        for (Object[] r : rows) {
            String name = str(r[2]);
            if (name == null || name.isBlank()) continue;
            String key = PollsUtils.toSimpleNameWithoutAccents(name).toLowerCase();
            byNormalized.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        // Also check reversed names
        Map<String, List<Object[]>> byReversed = new HashMap<>();
        for (Object[] r : rows) {
            String name = str(r[2]);
            if (name == null || name.isBlank()) continue;
            String reversed = PollsUtils.startWithFirstname(name);
            String key = PollsUtils.toSimpleNameWithoutAccents(reversed).toLowerCase();
            byReversed.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        // Merge: find groups where normalized or reversed name matches
        Set<Long> reported = new HashSet<>();
        List<DataIntegrityIssue> issues = new ArrayList<>();

        // Direct duplicates (same normalized name)
        for (var entry : byNormalized.entrySet()) {
            if (entry.getValue().size() > 1) {
                List<Object[]> group = entry.getValue();
                for (Object[] r : group) {
                    long id = num(r[0]);
                    if (reported.add(id)) {
                        String others = group.stream()
                                .filter(o -> num(o[0]) != id)
                                .map(o -> String.format("'%s' (id=%d)", o[2], num(o[0])))
                                .collect(Collectors.joining(", "));
                        issues.add(new DataIntegrityIssue(
                                Severity.WARNING,
                                "DUPLICATE_POLITICIAN",
                                null, null,
                                String.format("Politician '%s' may be duplicate of: %s", r[2], others),
                                new EntityRef("Politician", id, str(r[1]), str(r[2]))
                        ));
                    }
                }
            }
        }

        // Reversed-name matches across different normalized keys
        for (var entry : byNormalized.entrySet()) {
            String normalizedKey = entry.getKey();
            List<Object[]> reversedMatches = byReversed.getOrDefault(normalizedKey, List.of());
            for (Object[] reversed : reversedMatches) {
                long reversedId = num(reversed[0]);
                String reversedNormalized = PollsUtils.toSimpleNameWithoutAccents(str(reversed[2])).toLowerCase();
                // Skip if same normalized key (already caught above)
                if (reversedNormalized.equals(normalizedKey)) continue;
                for (Object[] original : entry.getValue()) {
                    long originalId = num(original[0]);
                    if (originalId == reversedId) continue;
                    if (reported.contains(reversedId) && reported.contains(originalId)) continue;
                    reported.add(reversedId);
                    reported.add(originalId);
                    issues.add(new DataIntegrityIssue(
                            Severity.WARNING,
                            "DUPLICATE_POLITICIAN",
                            null, null,
                            String.format("Politician '%s' may be reversed-name duplicate of '%s'",
                                    reversed[2], original[2]),
                            new EntityRef("Politician", reversedId, str(reversed[1]), str(reversed[2]))
                    ));
                }
            }
        }

        return issues;
    }

    @SuppressWarnings("unchecked")
    private List<DataIntegrityIssue> checkMissingPhoto(String townFilter) {
        int currentYear = LocalDate.now().getYear();
        String sql = "SELECT cm.id, cm.ref, p.name, s.name, t.ref" +
                " FROM council_member cm" +
                " JOIN politician p ON cm.politician_id = p.id" +
                " JOIN season s ON cm.season_id = s.id" +
                " JOIN town t ON cm.town_id = t.id" +
                " WHERE (p.picture IS NULL OR p.picture = '')" +
                "   AND s.name ~ '^\\d{4}-\\d{4}$'" +
                "   AND CAST(SPLIT_PART(s.name, '-', 1) AS INTEGER) <= :currentYear" +
                "   AND CAST(SPLIT_PART(s.name, '-', 2) AS INTEGER) >= :currentYear";
        if (townFilter != null) sql += " AND t.ref = :town";

        var q = em.createNativeQuery(sql);
        q.setParameter("currentYear", currentYear);
        if (townFilter != null) q.setParameter("town", townFilter);

        List<Object[]> rows = q.getResultList();
        return rows.stream().map(r -> new DataIntegrityIssue(
                Severity.INFO,
                "MISSING_PHOTO",
                str(r[4]),
                str(r[3]),
                String.format("Council member '%s' has no photo", r[2]),
                new EntityRef("CouncilMember", num(r[0]), str(r[1]), str(r[2]))
        )).toList();
    }

    @SuppressWarnings("unchecked")
    private List<DataIntegrityIssue> checkOrphanCouncilMember(String townFilter) {
        String sql = "SELECT cm.id, cm.ref, p.name" +
                " FROM council_member cm" +
                " LEFT JOIN politician p ON cm.politician_id = p.id" +
                " WHERE cm.town_id IS NULL OR cm.institution_id IS NULL";
        // townFilter not applicable for orphans with NULL town_id

        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        return rows.stream().map(r -> new DataIntegrityIssue(
                Severity.ERROR,
                "ORPHAN_COUNCIL_MEMBER",
                null, null,
                String.format("Council member '%s' has NULL town_id or institution_id", r[2]),
                new EntityRef("CouncilMember", num(r[0]), str(r[1]), str(r[2]))
        )).toList();
    }

    @SuppressWarnings("unchecked")
    private List<DataIntegrityIssue> checkMemberCountMismatch(String townFilter) {
        Map<String, Integer> expected = integrityProperties.getExpectedMembers();
        if (expected.isEmpty()) return List.of();

        String sql = "SELECT t.ref, s.name, COUNT(cm.id)" +
                " FROM council_member cm" +
                " JOIN town t ON cm.town_id = t.id" +
                " JOIN season s ON cm.season_id = s.id";
        if (townFilter != null) sql += " WHERE t.ref = :town";
        sql += " GROUP BY t.ref, s.name";

        var q = em.createNativeQuery(sql);
        if (townFilter != null) q.setParameter("town", townFilter);

        List<Object[]> rows = q.getResultList();
        List<DataIntegrityIssue> issues = new ArrayList<>();
        for (Object[] r : rows) {
            String town = str(r[0]);
            String season = str(r[1]);
            int actual = ((Number) r[2]).intValue();
            String key = town + "|" + season;
            Integer exp = expected.get(key);
            if (exp != null && actual != exp) {
                Severity severity = actual > exp ? Severity.ERROR : Severity.WARNING;
                issues.add(new DataIntegrityIssue(
                        severity,
                        "MEMBER_COUNT_MISMATCH",
                        town,
                        season,
                        String.format("Expected %d council members in %s %s, found %d",
                                exp, town, season, actual),
                        null
                ));
            }
        }
        return issues;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }

    private static long num(Object o) {
        return ((Number) o).longValue();
    }
}
