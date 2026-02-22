package org.blackbell.polls.source.bratislava;

import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.DataImport;
import org.blackbell.polls.domain.model.enums.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Import module for Bratislava web scraping (all seasons from zastupitelstvo.bratislava.sk).
 * Delegates to BratislavaWebScraper for actual scraping.
 */
@Component
public class BratislavaWebImport implements DataImport {

    private static final Logger log = LoggerFactory.getLogger(BratislavaWebImport.class);

    /** Seasons known to exist on zastupitelstvo.bratislava.sk */
    private static final List<String> KNOWN_SEASONS = List.of(
            "2002-2006", "2006-2010", "2010-2014", "2014-2018", "2018-2022", "2022-2026"
    );

    private final BratislavaWebScraper webScraper;

    public BratislavaWebImport(BratislavaWebScraper webScraper) {
        this.webScraper = webScraper;
    }

    @Override
    public Source getSource() {
        return Source.BA_WEB;
    }

    @Override
    public List<Season> loadSeasons(Town town) throws Exception {
        log.info("Loading web seasons for Bratislava...");
        return KNOWN_SEASONS.stream().map(ref -> {
            Season s = new Season();
            s.setRef(ref);
            s.setName(ref);
            return s;
        }).toList();
    }

    @Override
    public List<Meeting> loadMeetings(Town town, Season season, Institution institution) throws Exception {
        // Web scraping only has MZ data, not mestská rada or komisie
        if (institution.getType() != InstitutionType.ZASTUPITELSTVO) {
            return new ArrayList<>();
        }
        return webScraper.scrapeMeetingList(town, season, institution);
    }

    @Override
    public void loadMeetingDetails(Meeting meeting, String externalMeetingId) throws Exception {
        webScraper.scrapeMeetingDetails(meeting);
    }

    @Override
    public void loadPollDetails(Poll poll, Map<String, CouncilMember> membersMap) throws Exception {
        matchVotesToMembers(poll, membersMap);
    }

    @Override
    public List<CouncilMember> loadMembers(Town town, Season season, Institution institution) {
        return webScraper.scrapeMembers(town, season, institution);
    }

    /**
     * Match web-scraped votes (which have voterName but no councilMember) to existing council members.
     */
    private void matchVotesToMembers(Poll poll, Map<String, CouncilMember> membersMap) {
        if (poll.getVotes() == null) return;
        int matched = 0;
        for (Vote vote : poll.getVotes()) {
            if (vote.getCouncilMember() != null || vote.getVoterName() == null) continue;

            String key = PollsUtils.toSimpleNameWithoutAccents(vote.getVoterName());
            CouncilMember member = membersMap.get(key);

            // Try reversed name order (web has "Lastname Firstname" format)
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
                log.debug("No council member match for web voter: {} (key: {})", vote.getVoterName(), key);
            }
        }
        log.debug("Matched {}/{} votes for poll '{}'", matched, poll.getVotes().size(), poll.getName());
    }
}
