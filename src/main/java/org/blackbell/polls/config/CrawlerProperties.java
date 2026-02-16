package org.blackbell.polls.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "crawler")
public class CrawlerProperties {

    private String presovMembersUrl = "https://www.presov.sk/poslanci-msz.html";
    private int timeoutMs = 30000;

    public String getPresovMembersUrl() {
        return presovMembersUrl;
    }

    public void setPresovMembersUrl(String presovMembersUrl) {
        this.presovMembersUrl = presovMembersUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
