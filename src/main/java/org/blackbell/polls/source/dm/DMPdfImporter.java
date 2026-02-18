package org.blackbell.polls.source.dm;

import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.embeddable.VotesCount;
import org.blackbell.polls.domain.model.enums.VoteChoice;
import org.blackbell.polls.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.*;

import static org.blackbell.polls.common.PollsUtils.toSimpleNameWithoutAccents;

/**
 * Orchestrates downloading a voting PDF from DM API and mapping
 * parsed records to Poll + Vote entities on the Meeting.
 */
public class DMPdfImporter {

    private static final Logger log = LoggerFactory.getLogger(DMPdfImporter.class);
    private static final String DM_BASE_URL = "https://www.digitalnemesto.sk";

    private final DMPdfVotingParser parser = new DMPdfVotingParser();

    public void importVotesFromPdf(Meeting meeting, String pdfUrl,
                                   Map<String, CouncilMember> membersMap) {
        // Resolve relative URLs against DM base URL
        String fullUrl = pdfUrl.startsWith("http") ? pdfUrl : DM_BASE_URL + pdfUrl;
        log.info("Downloading voting PDF: {}", fullUrl);

        List<DMPdfVotingParser.PdfVoteRecord> records;
        try (InputStream is = URI.create(fullUrl).toURL().openStream()) {
            records = parser.parsePdf(is);
        } catch (Exception e) {
            log.error("Failed to download/parse voting PDF for meeting '{}': {}",
                    meeting.getName(), e.getMessage());
            return;
        }

        if (records.isEmpty()) {
            log.warn("No voting records parsed from PDF for meeting '{}'", meeting.getName());
            return;
        }

        log.info("Parsed {} voting records from PDF for meeting '{}'", records.size(), meeting.getName());

        for (DMPdfVotingParser.PdfVoteRecord record : records) {
            AgendaItem agendaItem = findOrCreateAgendaItem(meeting, record.agendaItemNumber, record.description);

            Poll poll = new Poll();
            poll.setRef(PollsUtils.generateUniqueKeyReference());
            poll.setName(record.description != null ? record.description : "Hlasovanie " + record.voteNumber);
            poll.setDataSource(Source.DM_PDF);
            poll.setVoters(record.present);

            VotesCount vc = new VotesCount();
            vc.setVotedFor(record.votedFor);
            vc.setVotedAgainst(record.votedAgainst);
            vc.setAbstain(record.abstained);
            vc.setNotVoted(record.notVoted);
            vc.setAbsent(0); // PDF doesn't have absent count directly; absent = voters - present
            poll.setVotesCount(vc);

            // Create Vote entities from names
            Set<Vote> votes = new HashSet<>();
            addVotes(votes, poll, record.forNames, VoteChoice.VOTED_FOR, membersMap);
            addVotes(votes, poll, record.againstNames, VoteChoice.VOTED_AGAINST, membersMap);
            addVotes(votes, poll, record.abstainNames, VoteChoice.ABSTAIN, membersMap);
            addVotes(votes, poll, record.notVotedNames, VoteChoice.NOT_VOTED, membersMap);
            poll.setVotes(votes);

            agendaItem.addPoll(poll);
        }

        log.info("Created {} polls from PDF for meeting '{}'", records.size(), meeting.getName());
    }

    private AgendaItem findOrCreateAgendaItem(Meeting meeting, String itemNumber, String description) {
        // Try to match by agenda item number prefix in existing items
        if (itemNumber != null && meeting.getAgendaItems() != null) {
            String prefix = itemNumber + ".";
            for (AgendaItem existing : meeting.getAgendaItems()) {
                if (existing.getName() != null && existing.getName().startsWith(prefix)) {
                    return existing;
                }
            }
            // Also try exact match (e.g. item name is just "1" or "10")
            for (AgendaItem existing : meeting.getAgendaItems()) {
                if (existing.getName() != null && existing.getName().equals(itemNumber)) {
                    return existing;
                }
            }
        }

        // Create new agenda item
        AgendaItem newItem = new AgendaItem();
        String name = itemNumber != null
                ? itemNumber + ". " + (description != null ? description : "")
                : (description != null ? description : "Bod programu");
        newItem.setName(name.trim());
        newItem.setRef(PollsUtils.generateUniqueKeyReference());
        meeting.addAgendaItem(newItem);
        return newItem;
    }

    private void addVotes(Set<Vote> votes, Poll poll, List<String> names,
                          VoteChoice choice, Map<String, CouncilMember> membersMap) {
        for (String pdfName : names) {
            Vote vote = new Vote();
            vote.setVoterName(pdfName);
            vote.setVoted(choice);
            vote.setPoll(poll);
            vote.setCouncilMember(matchVoter(pdfName, membersMap));
            votes.add(vote);
        }
    }

    private CouncilMember matchVoter(String pdfName, Map<String, CouncilMember> membersMap) {
        if (membersMap == null || membersMap.isEmpty()) {
            return null;
        }
        String nameKey = toSimpleNameWithoutAccents(pdfName);
        CouncilMember member = membersMap.get(nameKey);
        if (member == null) {
            // Try reversed name order
            String[] parts = nameKey.split("\\s", 2);
            if (parts.length == 2) {
                member = membersMap.get(parts[1] + " " + parts[0]);
            }
        }
        if (member == null) {
            log.debug("Unmatched PDF voter: '{}' (normalized: '{}')", pdfName, nameKey);
        }
        return member;
    }
}
