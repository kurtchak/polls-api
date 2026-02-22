package org.blackbell.polls.source.crawler;

import org.blackbell.polls.config.CrawlerProperties;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.DataImport;
import org.blackbell.polls.domain.model.enums.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DataImport implementation for Banská Bystrica city council via web scraping.
 * Phase 1: Members only. Meetings and voting data (eGov PDF) deferred.
 */
@Component
public class BanskaBystricaWebImport implements DataImport {

    private static final Logger log = LoggerFactory.getLogger(BanskaBystricaWebImport.class);

    private static final List<String> KNOWN_SEASONS = List.of("2022-2026");

    private final CrawlerProperties crawlerProperties;

    public BanskaBystricaWebImport(CrawlerProperties crawlerProperties) {
        this.crawlerProperties = crawlerProperties;
    }

    @Override
    public Source getSource() {
        return Source.BB_WEB;
    }

    @Override
    public List<Season> loadSeasons(Town town) throws Exception {
        log.info("Loading Banská Bystrica seasons...");
        return KNOWN_SEASONS.stream().map(ref -> {
            Season s = new Season();
            s.setRef(ref);
            s.setName(ref);
            return s;
        }).toList();
    }

    @Override
    public List<Meeting> loadMeetings(Town town, Season season, Institution institution) {
        return null;
    }

    @Override
    public void loadMeetingDetails(Meeting meeting, String externalMeetingId) {
    }

    @Override
    public void loadPollDetails(Poll poll, Map<String, CouncilMember> membersMap) {
    }

    @Override
    public List<CouncilMember> loadMembers(Town town, Season season, Institution institution) {
        log.info("Loading Banská Bystrica council members via web scraper for season {}", season.getRef());
        BanskaBystricaScraper scraper = new BanskaBystricaScraper(
                crawlerProperties.getBbMembersUrl(), crawlerProperties.getTimeoutMs());
        List<CouncilMember> members = scraper.scrapeMembers(town, season, institution);
        return members != null && !members.isEmpty() ? members : null;
    }
}
