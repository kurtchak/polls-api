package org.blackbell.polls.integrity;

public record DataIntegrityIssue(
        Severity severity,
        String category,
        String town,
        String season,
        String description,
        EntityRef entity
) {
    public enum Severity { ERROR, WARNING, INFO }

    public record EntityRef(String type, long id, String ref, String name) {}
}
