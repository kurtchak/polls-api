package org.blackbell.polls.source;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.MeetingRepository;
import org.blackbell.polls.domain.repositories.SeasonRepository;
import org.blackbell.polls.source.dm.DMPdfImporter;
import org.blackbell.polls.sync.SyncProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles synchronization of meetings and their details (agenda items, polls, votes).
 * Uses TransactionTemplate for per-meeting transactions instead of self-injection.
 */
@Component
public class MeetingSyncService {
    private static final Logger log = LoggerFactory.getLogger(MeetingSyncService.class);

    private final DataSourceResolver resolver;
    private final MeetingRepository meetingRepository;
    private final SeasonRepository seasonRepository;
    private final CouncilMemberSyncService councilMemberSyncService;
    private final PoliticianMatchingService politicianMatchingService;
    private final SyncProgress syncProgress;
    private final SyncCacheManager cacheManager;
    private final TransactionTemplate txTemplate;

    public MeetingSyncService(DataSourceResolver resolver,
                              MeetingRepository meetingRepository,
                              SeasonRepository seasonRepository,
                              CouncilMemberSyncService councilMemberSyncService,
                              PoliticianMatchingService politicianMatchingService,
                              SyncProgress syncProgress,
                              SyncCacheManager cacheManager,
                              PlatformTransactionManager txManager) {
        this.resolver = resolver;
        this.meetingRepository = meetingRepository;
        this.seasonRepository = seasonRepository;
        this.councilMemberSyncService = councilMemberSyncService;
        this.politicianMatchingService = politicianMatchingService;
        this.syncProgress = syncProgress;
        this.cacheManager = cacheManager;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    public void syncSeasonMeetings(Town town, Season season, Map<InstitutionType, List<Institution>> institutionsMap) {
        if (isHistoricalSeason(season) && isSeasonFullySynced(town, season)) {
            log.info(Constants.MarkerSync, "{}: Skipping fully synced historical season {}", town.getName(), season.getRef());
            return;
        }
        log.info(Constants.MarkerSync, "{}: syncSeasonMeetings {}", town.getName(), season.getRef());

        institutionsMap.keySet()
                .forEach(type -> syncSeasonMeetings(town, season, institutionsMap.get(type).get(0)));
    }

    private void syncSeasonMeetings(Town town, Season season, Institution institution) {
        log.debug("syncSeasonMeetings -> {} - {} - {}", town, season, institution);
        if (InstitutionType.KOMISIA.equals(institution.getType())) {
            return;
        }
        log.info(Constants.MarkerSync, "{}:{}:{}: syncSeasonMeetings", town.getName(), season.getName(), institution.getName());
        try {
            DataSourceResolver.SourcedResult<List<Meeting>> result = resolver.resolveAndLoad(
                    town, season.getRef(), institution.getType(), DataOperation.MEETINGS,
                    di -> di.loadMeetings(town, season, institution));
            if (result != null) {
                List<Meeting> meetings = result.data();
                Source meetingsSource = result.source();
                DataImport detailsImport = resolveDetailImport(town, season, institution);
                syncProgress.startSeason(town.getRef(), season.getRef(), meetings.size());
                for (Meeting meeting : meetings) {
                    meeting.setDataSource(meetingsSource);
                    try {
                        txTemplate.executeWithoutResult(status -> {
                            try {
                                syncSingleMeeting(meeting, detailsImport);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (Exception e) {
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        log.error(Constants.MarkerSync, "Failed to sync meeting '{}': {}", meeting.getName(), cause.getMessage(), cause);
                    } finally {
                        syncProgress.meetingProcessed();
                    }
                }
            } else {
                Season s = cacheManager.getSeason(season.getRef());
                if (s != null) {
                    s.setSyncError("No data source available for meetings");
                    seasonRepository.save(s);
                }
            }
        } catch (Exception e) {
            log.error(Constants.MarkerSync, "An error occured during the {} meetings synchronization.", season.getRef(), e);
        }
    }

    private void syncSingleMeeting(Meeting meeting, DataImport dataImport) throws Exception {
        Meeting existing = meetingRepository.findByExtId(meeting.getExtId());
        if (existing != null) {
            if (existing.isSyncComplete()) {
                log.debug(Constants.MarkerSync, "Skipping complete meeting: {}", meeting.getName());
                return;
            }
            if (existing.getSyncError() != null) {
                log.info(Constants.MarkerSync, "Retrying previously failed meeting: {} (error: {})",
                        meeting.getName(), existing.getSyncError());
            } else if (existing.isComplete()) {
                existing.setSyncComplete(true);
                meetingRepository.save(existing);
                log.debug(Constants.MarkerSync, "Marked meeting as syncComplete: {}", meeting.getName());
                return;
            } else if (existing.hasVotes() && existing.hasUnmatchedVotes()) {
                log.info(Constants.MarkerSync, "Re-loading meeting with unmatched votes: {}", meeting.getName());
            } else if (!existing.hasVotes() && existing.hasPolls()) {
                log.info(Constants.MarkerSync, "Re-loading meeting without individual votes: {}", meeting.getName());
            } else {
                log.info(Constants.MarkerSync, "Re-loading incomplete meeting (0 agenda items): {}", meeting.getName());
            }
            meetingRepository.delete(existing);
            meetingRepository.flush();
        }
        try {
            loadMeeting(meeting, dataImport);
            meeting.setSyncError(null);
            boolean complete = meeting.isComplete();
            meeting.setSyncComplete(complete);
            if (!complete) {
                log.info(Constants.MarkerSync, "Meeting '{}' incomplete: {}", meeting.getName(), meeting.getIncompleteReason());
            }
        } catch (Exception e) {
            log.error(Constants.MarkerSync, "Error loading meeting '{}': {}", meeting.getName(), e.getMessage(), e);
            meeting.setSyncError(e.getMessage());
            meeting.setSyncComplete(false);
        }
        meetingRepository.save(meeting);
    }

    private void loadMeeting(Meeting meeting, DataImport dataImport) throws Exception {
        log.debug(Constants.MarkerSync, "loadMeeting: {}", meeting);
        dataImport.loadMeetingDetails(meeting, meeting.getExtId());
        if (meeting.getAgendaItems() != null) {
            Map<String, CouncilMember> membersMap = councilMemberSyncService.getMembersMap(
                    meeting.getTown(), meeting.getSeason(), meeting.getInstitution());

            boolean hasPolls = meeting.getAgendaItems().stream()
                    .anyMatch(ai -> ai.getPolls() != null && !ai.getPolls().isEmpty());

            int attachmentCount = meeting.getAttachments() != null ? meeting.getAttachments().size() : 0;
            log.info(Constants.MarkerSync, "Meeting '{}': {} agenda items, hasPolls={}, {} attachments",
                    meeting.getName(), meeting.getAgendaItems().size(), hasPolls, attachmentCount);

            if (hasPolls) {
                for (AgendaItem item : meeting.getAgendaItems()) {
                    if (item.getPolls() != null) {
                        for (Poll poll : item.getPolls()) {
                            if (poll.getDataSource() == null) {
                                poll.setDataSource(Source.DM);
                            }
                            log.debug(Constants.MarkerSync, ">> poll: {}", poll);
                            if (poll.getExtAgendaItemId() == null || poll.getExtPollRouteId() == null) {
                                log.warn(Constants.MarkerSync, "Skipping poll details - missing ext IDs for poll '{}'", poll.getName());
                                continue;
                            }
                            try {
                                dataImport.loadPollDetails(poll, membersMap);
                            } catch (Exception e) {
                                log.warn(Constants.MarkerSync, "Could not load poll details (votes) for poll '{}' - saving poll with vote counts only: {}",
                                        poll.getName(), e.getMessage());
                            }
                        }
                    }
                }
            } else {
                String pdfUrl = findVotingPdfUrl(meeting);
                log.info(Constants.MarkerSync, "PDF fallback for '{}': pdfUrl={}", meeting.getName(), pdfUrl);
                if (pdfUrl != null) {
                    log.info(Constants.MarkerSync, "Loading votes from PDF for meeting: {}", meeting.getName());
                    new DMPdfImporter().importVotesFromPdf(meeting, pdfUrl, membersMap);
                } else {
                    log.info(Constants.MarkerSync, "No voting PDF found for meeting: {} (attachments: {})",
                            meeting.getName(), meeting.getAttachments() != null ?
                                    meeting.getAttachments().stream().map(MeetingAttachment::getName)
                                            .collect(Collectors.joining(", ")) : "null");
                }
            }

            politicianMatchingService.createMissingMembersFromVotes(meeting, membersMap);
        }
        log.debug(Constants.MarkerSync, "NEW MEETING: {}", meeting);
    }

    private boolean isHistoricalSeason(Season season) {
        try {
            String ref = season.getRef();
            int endYear = Integer.parseInt(ref.substring(ref.indexOf('-') + 1));
            return endYear < java.time.Year.now().getValue();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSeasonFullySynced(Town town, Season season) {
        long total = meetingRepository.countMeetingsByTownAndSeason(town.getRef(), season.getRef());
        if (total == 0) return false;
        long incomplete = meetingRepository.countIncompleteMeetings(town.getRef(), season.getRef());
        return incomplete == 0;
    }

    private DataImport resolveDetailImport(Town town, Season season, Institution institution) {
        List<DataImport> imports = resolver.resolve(town, season.getRef(),
                institution.getType(), DataOperation.MEETING_DETAILS);
        return imports.isEmpty() ? null : imports.get(0);
    }

    private String findVotingPdfUrl(Meeting meeting) {
        if (meeting.getAttachments() == null) return null;
        return meeting.getAttachments().stream()
                .filter(a -> a.getSource() != null && a.getName() != null
                        && a.getName().toLowerCase().contains("hlasovan"))
                .map(MeetingAttachment::getSource)
                .findFirst().orElse(null);
    }
}
