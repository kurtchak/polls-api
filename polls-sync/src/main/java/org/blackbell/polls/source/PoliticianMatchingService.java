package org.blackbell.polls.source;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.repositories.CouncilMemberRepository;
import org.blackbell.polls.domain.repositories.PoliticianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.blackbell.polls.common.PollsUtils.toSimpleNameWithoutAccents;

/**
 * Handles politician matching across seasons — reuse existing politicians,
 * find by name, auto-create from vote data.
 */
@Component
public class PoliticianMatchingService {
    private static final Logger log = LoggerFactory.getLogger(PoliticianMatchingService.class);

    private final PoliticianRepository politicianRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final SyncCacheManager cacheManager;

    public PoliticianMatchingService(PoliticianRepository politicianRepository,
                                     CouncilMemberRepository councilMemberRepository,
                                     SyncCacheManager cacheManager) {
        this.politicianRepository = politicianRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Reuse existing politicians across seasons and set town/institution on members.
     */
    public void reuseExistingPoliticians(List<CouncilMember> members, Town town, Institution institution) {
        Map<String, Politician> politiciansMap = cacheManager.getPoliticiansMap();
        for (CouncilMember cm : members) {
            if (cm.getTown() == null) cm.setTown(town);
            if (cm.getInstitution() == null) cm.setInstitution(institution);

            String politicianKey = toSimpleNameWithoutAccents(cm.getPolitician().getName());
            Politician existingPolitician = politiciansMap != null ? politiciansMap.get(politicianKey) : null;

            if (existingPolitician != null) {
                // Re-fetch to ensure managed in current transaction
                // (may be detached from a previous town's sync)
                if (existingPolitician.getId() != 0) {
                    existingPolitician = politicianRepository.findById(existingPolitician.getId())
                            .orElse(existingPolitician);
                    cacheManager.putPolitician(politicianKey, existingPolitician);
                }

                log.info("Reusing existing politician: {} (ID: {})",
                        PollsUtils.deAccent(existingPolitician.getName()), existingPolitician.getId());

                if (cm.getPolitician().getEmail() != null) {
                    existingPolitician.setEmail(cm.getPolitician().getEmail());
                }
                if (cm.getPolitician().getPhone() != null) {
                    existingPolitician.setPhone(cm.getPolitician().getPhone());
                }
                if (cm.getPolitician().getPicture() != null) {
                    existingPolitician.setPicture(cm.getPolitician().getPicture());
                }

                if (cm.getPolitician().getPartyNominees() != null) {
                    // Ensure partyNominees is initialized — cached politician
                    // may have a lazy proxy from a closed session
                    try {
                        if (existingPolitician.getPartyNominees() != null) {
                            existingPolitician.getPartyNominees().size(); // force init
                        }
                    } catch (org.hibernate.LazyInitializationException e) {
                        existingPolitician.setPartyNominees(new HashSet<>());
                    }
                    for (var nominee : cm.getPolitician().getPartyNominees()) {
                        nominee.setPolitician(existingPolitician);
                        existingPolitician.addPartyNominee(nominee);
                    }
                }

                cm.setPolitician(existingPolitician);
            } else {
                log.info("NEW POLITICIAN: {}", PollsUtils.deAccent(cm.getPolitician().getName()));
                cacheManager.putPolitician(politicianKey, cm.getPolitician());
            }
        }
    }

    public Politician findPoliticianByName(String nameKey) {
        Map<String, Politician> politiciansMap = cacheManager.getPoliticiansMap();
        if (politiciansMap == null) return null;
        Politician politician = politiciansMap.get(nameKey);
        if (politician == null) {
            String[] parts = nameKey.split("\\s", 2);
            if (parts.length == 2) {
                politician = politiciansMap.get(parts[1] + " " + parts[0]);
            }
        }
        if (politician != null && politician.getId() != 0) {
            return politicianRepository.findById(politician.getId()).orElse(politician);
        }
        return politician;
    }

    /**
     * Auto-create Politician + CouncilMember records for DM API voters that couldn't be matched
     * to existing council members.
     */
    public void createMissingMembersFromVotes(Meeting meeting, Map<String, CouncilMember> membersMap) {
        int created = 0;
        for (AgendaItem item : meeting.getAgendaItems()) {
            if (item.getPolls() == null) continue;
            for (Poll poll : item.getPolls()) {
                if (poll.getVotes() == null) continue;
                for (Vote vote : poll.getVotes()) {
                    if (vote.getCouncilMember() != null || vote.getVoterName() == null) continue;

                    String voterName = vote.getVoterName();
                    String nameKey = toSimpleNameWithoutAccents(voterName);

                    CouncilMember member = membersMap.get(nameKey);
                    if (member == null) {
                        String[] parts = nameKey.split("\\s", 2);
                        if (parts.length == 2) {
                            member = membersMap.get(parts[1] + " " + parts[0]);
                        }
                    }
                    if (member != null) {
                        vote.setCouncilMember(member);
                        continue;
                    }

                    Politician politician = findPoliticianByName(nameKey);
                    if (politician == null) {
                        String simpleName = PollsUtils.toSimpleName(voterName);
                        String titles = PollsUtils.getTitles(voterName);
                        politician = new Politician();
                        politician.setName(simpleName);
                        politician.setRef(PollsUtils.generateUniqueKeyReference());
                        politician.setTitles(titles);
                        politician = politicianRepository.save(politician);
                        cacheManager.putPolitician(nameKey, politician);
                        log.info(Constants.MarkerSync, "Auto-created politician from DM voter: '{}'", simpleName);
                    }

                    CouncilMember newMember = new CouncilMember();
                    newMember.setRef(PollsUtils.generateUniqueKeyReference());
                    newMember.setPolitician(politician);
                    newMember.setTown(meeting.getTown());
                    newMember.setSeason(meeting.getSeason());
                    newMember.setInstitution(meeting.getInstitution());
                    newMember = councilMemberRepository.save(newMember);

                    membersMap.put(nameKey, newMember);
                    String[] parts = nameKey.split("\\s", 2);
                    if (parts.length == 2) {
                        membersMap.put(parts[1] + " " + parts[0], newMember);
                    }

                    vote.setCouncilMember(newMember);
                    created++;
                }
            }
        }
        if (created > 0) {
            log.info(Constants.MarkerSync, "Auto-created {} council member(s) for meeting '{}' (season: {})",
                    created, meeting.getName(), meeting.getSeason().getRef());
        }
    }
}
