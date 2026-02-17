package org.blackbell.polls.source.crawler;

import org.blackbell.polls.config.CrawlerProperties;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.source.DataImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DataImport implementation for Poprad council members via web scraping.
 * Only provides members -- other operations return null (handled by DM fallback).
 */
@Component
public class PopradMemberImport implements DataImport {

    private static final Logger log = LoggerFactory.getLogger(PopradMemberImport.class);

    private final CrawlerProperties crawlerProperties;

    public PopradMemberImport(CrawlerProperties crawlerProperties) {
        this.crawlerProperties = crawlerProperties;
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
        log.info("Loading Poprad council members via web scraper for season {}", season.getRef());
        PopradCouncilMemberScraper scraper = new PopradCouncilMemberScraper(
                crawlerProperties.getPopradMembersUrl(), crawlerProperties.getTimeoutMs());
        List<CouncilMember> members = scraper.scrapeMembers(town, season, institution);
        return members != null && !members.isEmpty() ? members : null;
    }
}
