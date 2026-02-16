package org.blackbell.polls.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sync")
public class SyncProperties {

    private long fixedRateMs = 86400000;
    private long initialDelayMs = 5000;

    public long getFixedRateMs() {
        return fixedRateMs;
    }

    public void setFixedRateMs(long fixedRateMs) {
        this.fixedRateMs = fixedRateMs;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }
}
