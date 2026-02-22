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
 * DataImport implementation for Trenčín city council via web scraping.
 * Phase 1: Members only. Voting data (PDF in zápisnice) deferred.
 */
@Component
public class TrencinWebImport implements DataImport {

    private static final Logger log = LoggerFactory.getLogger(TrencinWebImport.class);

    private static final List<String> KNOWN_SEASONS = List.of("2022-2026");

    private final CrawlerProperties crawlerProperties;

    public TrencinWebImport(CrawlerProperties crawlerProperties) {
        this.crawlerProperties = crawlerProperties;
    }

    @Override
    public Source getSource() {
        return Source.TRENCIN_WEB;
    }

    @Override
    public List<Season> loadSeasons(Town town) throws Exception {
        log.info("Loading Trenčín seasons...");
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
        log.info("Loading Trenčín council members via web scraper for season {}", season.getRef());
        TrencinScraper scraper = new TrencinScraper(
                crawlerProperties.getTrencinMembersUrl(), crawlerProperties.getTimeoutMs());
        List<CouncilMember> members = scraper.scrapeMembers(town, season, institution);
        return members != null && !members.isEmpty() ? members : null;
    }
}
