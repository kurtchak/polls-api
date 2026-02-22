package org.blackbell.polls.source.crawler;

import org.blackbell.polls.config.CrawlerProperties;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.DataImport;
import org.blackbell.polls.domain.model.enums.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * DataImport implementation for Prešov council members via web scraping.
 * Only provides members — other operations return null.
 */
@Component
public class PresovMemberImport implements DataImport {

    private static final Logger log = LoggerFactory.getLogger(PresovMemberImport.class);

    private final CrawlerProperties crawlerProperties;

    public PresovMemberImport(CrawlerProperties crawlerProperties) {
        this.crawlerProperties = crawlerProperties;
    }

    @Override
    public Source getSource() {
        return Source.PRESOV_WEB;
    }

    @Override
    public List<Season> loadSeasons(Town town) {
        return null;
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
        log.info("Loading Prešov council members via web scraper for season {}", season.getRef());
        PresovCouncilMemberCrawlerV2 crawler = new PresovCouncilMemberCrawlerV2(
                crawlerProperties.getPresovMembersUrl(), crawlerProperties.getTimeoutMs());
        Set<CouncilMember> members = crawler.getCouncilMembers(
                town, institution, season, Map.of(), new HashMap<>());
        if (members != null && !members.isEmpty()) {
            return new ArrayList<>(members);
        }
        return null;
    }
}
