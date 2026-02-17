package org.blackbell.polls.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "crawler")
public class CrawlerProperties {

    private String presovMembersUrl = "https://www.presov.sk/poslanci-msz.html";
    private String popradMembersUrl = "https://www.poprad.sk/kontakty?SearchModel.SearchText=&SearchModel.Department=Poslanci_MsZ";
    private int timeoutMs = 30000;

    public String getPresovMembersUrl() {
        return presovMembersUrl;
    }

    public void setPresovMembersUrl(String presovMembersUrl) {
        this.presovMembersUrl = presovMembersUrl;
    }

    public String getPopradMembersUrl() {
        return popradMembersUrl;
    }

    public void setPopradMembersUrl(String popradMembersUrl) {
        this.popradMembersUrl = popradMembersUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
