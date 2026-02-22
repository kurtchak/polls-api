package org.blackbell.polls.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "integrity")
public class IntegrityProperties {

    /**
     * Expected number of council members per town:season.
     * Key format: "townRef:seasonRef", e.g. "bratislava:2022-2026"
     * Value: expected count.
     */
    private Map<String, Integer> expectedMembers = new HashMap<>();

    public Map<String, Integer> getExpectedMembers() {
        return expectedMembers;
    }

    public void setExpectedMembers(Map<String, Integer> expectedMembers) {
        this.expectedMembers = expectedMembers;
    }
}
