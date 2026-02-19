package org.blackbell.polls.source.crawler;

import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.config.CrawlerProperties;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.source.DataImport;
import org.blackbell.polls.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DataImport implementation for Trnava city council via web scraping.
 * Full scraper: members + meetings + voting data.
 */
@Component
public class TrnavaWebImport implements DataImport {

    private static final Logger log = LoggerFactory.getLogger(TrnavaWebImport.class);

    private static final List<String> KNOWN_SEASONS = List.of("2022-2026");

    private final CrawlerProperties crawlerProperties;

    public TrnavaWebImport(CrawlerProperties crawlerProperties) {
        this.crawlerProperties = crawlerProperties;
    }

    @Override
    public Source getSource() {
        return Source.TRNAVA_WEB;
    }

    @Override
    public List<Season> loadSeasons(Town town) throws Exception {
        log.info("Loading Trnava seasons...");
        return KNOWN_SEASONS.stream().map(ref -> {
            Season s = new Season();
            s.setRef(ref);
            s.setName(ref);
            return s;
        }).toList();
    }

    @Override
    public List<Meeting> loadMeetings(Town town, Season season, Institution institution) throws Exception {
        if (institution.getType() != InstitutionType.ZASTUPITELSTVO) {
            return new ArrayList<>();
        }
        TrnavaScraper scraper = createScraper();
        return scraper.scrapeMeetingList(town, season, institution);
    }

    @Override
    public void loadMeetingDetails(Meeting meeting, String externalMeetingId) throws Exception {
        TrnavaScraper scraper = createScraper();
        scraper.scrapeMeetingDetails(meeting);
    }

    @Override
    public void loadPollDetails(Poll poll, Map<String, CouncilMember> membersMap) throws Exception {
        matchVotesToMembers(poll, membersMap);
    }

    @Override
    public List<CouncilMember> loadMembers(Town town, Season season, Institution institution) {
        TrnavaScraper scraper = createScraper();
        List<CouncilMember> members = scraper.scrapeMembers(town, season, institution);
        return members != null && !members.isEmpty() ? members : null;
    }

    private TrnavaScraper createScraper() {
        return new TrnavaScraper(
                crawlerProperties.getTrnavaMembersUrl(),
                crawlerProperties.getTrnavaMeetingsUrl(),
                crawlerProperties.getTrnavaMeetingDetailUrl(),
                crawlerProperties.getTimeoutMs());
    }

    private void matchVotesToMembers(Poll poll, Map<String, CouncilMember> membersMap) {
        if (poll.getVotes() == null) return;
        int matched = 0;
        for (Vote vote : poll.getVotes()) {
            if (vote.getCouncilMember() != null || vote.getVoterName() == null) continue;

            String key = PollsUtils.toSimpleNameWithoutAccents(vote.getVoterName());
            CouncilMember member = membersMap.get(key);

            // Try reversed name order
            if (member == null) {
                String[] parts = key.split("\\s", 2);
                if (parts.length == 2) {
                    member = membersMap.get(parts[1] + " " + parts[0]);
                }
            }

            if (member != null) {
                vote.setCouncilMember(member);
                matched++;
            } else {
                log.debug("No council member match for Trnava voter: {} (key: {})", vote.getVoterName(), key);
            }
        }
        log.debug("Matched {}/{} votes for poll '{}'", matched, poll.getVotes().size(), poll.getName());
    }
}
