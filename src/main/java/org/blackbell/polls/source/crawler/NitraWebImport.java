package org.blackbell.polls.source.crawler;

import org.blackbell.polls.config.CrawlerProperties;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.source.DataImport;
import org.blackbell.polls.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DataImport implementation for Nitra city council via web scraping.
 * Phase 1: Members only. Meetings and voting data (PDF) deferred to Phase 2.
 */
@Component
public class NitraWebImport implements DataImport {

    private static final Logger log = LoggerFactory.getLogger(NitraWebImport.class);

    private static final List<String> KNOWN_SEASONS = List.of("2022-2026");

    private final CrawlerProperties crawlerProperties;

    public NitraWebImport(CrawlerProperties crawlerProperties) {
        this.crawlerProperties = crawlerProperties;
    }

    @Override
    public Source getSource() {
        return Source.NITRA_WEB;
    }

    @Override
    public List<Season> loadSeasons(Town town) throws Exception {
        log.info("Loading Nitra seasons...");
        return KNOWN_SEASONS.stream().map(ref -> {
            Season s = new Season();
            s.setRef(ref);
            s.setName(ref);
            return s;
        }).toList();
    }

    @Override
    public List<Meeting> loadMeetings(Town town, Season season, Institution institution) {
        // Phase 2: implement meeting list scraping
        return null;
    }

    @Override
    public void loadMeetingDetails(Meeting meeting, String externalMeetingId) {
        // Phase 2: implement PDF voting parsing
    }

    @Override
    public void loadPollDetails(Poll poll, Map<String, CouncilMember> membersMap) {
        // Phase 2: implement PDF voting parsing
    }

    @Override
    public List<CouncilMember> loadMembers(Town town, Season season, Institution institution) {
        log.info("Loading Nitra council members via web scraper for season {}", season.getRef());
        NitraScraper scraper = new NitraScraper(
                crawlerProperties.getNitraMembersUrl(), crawlerProperties.getTimeoutMs());
        List<CouncilMember> members = scraper.scrapeMembers(town, season, institution);
        return members != null && !members.isEmpty() ? members : null;
    }
}
