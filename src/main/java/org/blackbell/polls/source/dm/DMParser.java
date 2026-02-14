package org.blackbell.polls.source.dm;

import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.embeddable.VotesCount;
import org.blackbell.polls.domain.model.enums.VoteChoice;
import org.blackbell.polls.source.dm.api.response.DMMeetingResponse;
import org.blackbell.polls.source.dm.api.response.DMMeetingsResponse;
import org.blackbell.polls.source.dm.api.response.DMPollDetailResponse;
import org.blackbell.polls.source.dm.api.response.DMSeasonsResponse;
import org.blackbell.polls.source.dm.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.*;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
public class DMParser {

    private static final Logger log = LoggerFactory.getLogger(DMParser.class);

    private static Season parseSeason(SeasonDTO seasonDTO) {
        Season season = new Season();
        season.setName(seasonDTO.getName());
        season.setRef(seasonDTO.getName());
        return season;
    }

    public static Meeting parseMeetingResponse(Meeting meeting, DMMeetingResponse meetingResponse) throws Exception {
        if (meetingResponse == null || meetingResponse.getChildren() == null || meetingResponse.getChildren().isEmpty()) {
            log.warn("Meeting response is empty for meeting: {}", meeting.getRef());
            return meeting;
        }

        AgendaDTO agendaDTO = null;
        AttachmentsDTO attachmentsDTO = null;

        // Find AgendaDTO and AttachmentsDTO from children
        for (MeetingComponentDTO component : meetingResponse.getChildren()) {
            if (component instanceof AgendaDTO) {
                agendaDTO = (AgendaDTO) component;
            } else if (component instanceof AttachmentsDTO) {
                attachmentsDTO = (AttachmentsDTO) component;
            }
        }

        // Agenda
        if (agendaDTO != null) {
            log.info("-> parseAgenda: " + agendaDTO.getName());
            loadAgenda(meeting, agendaDTO);
        } else {
            log.warn("No agenda found for meeting: {}", meeting.getRef());
        }

        // Attachments
        if (attachmentsDTO != null) {
            log.info("-> parseMeetingAttachments: " + attachmentsDTO.getName());
            loadMeetingAttachments(meeting, attachmentsDTO);
        }

        return meeting;
    }

    private static void loadAgenda(Meeting meeting, AgendaDTO agendaDTO) {
        log.info("loadAgenda for meeting[{}]", meeting.getRef());
        if (agendaDTO.getAgendaItemDTOs() == null || agendaDTO.getAgendaItemDTOs().isEmpty()) {
            log.warn("No agenda items for meeting: {}", meeting.getRef());
            return;
        }

        for (AgendaItemDTO agendaItemDTO : agendaDTO.getAgendaItemDTOs()) {
            AgendaItem item = new AgendaItem();
            item.setName(agendaItemDTO.getName());
            item.setRef(PollsUtils.generateUniqueKeyReference());
            meeting.addAgendaItem(item);

            if (agendaItemDTO.getChildren() != null && !agendaItemDTO.getChildren().isEmpty()) {
                PollsDTO pollsDTO = null;
                ProspectsDTO prospectsDTO = null;

                // Find PollsDTO and ProspectsDTO from children
                for (AgendaItemComponentDTO component : agendaItemDTO.getChildren()) {
                    if (component instanceof PollsDTO) {
                        pollsDTO = (PollsDTO) component;
                    } else if (component instanceof ProspectsDTO) {
                        prospectsDTO = (ProspectsDTO) component;
                    }
                }

                if (pollsDTO != null) {
                    loadAgendaItemPolls(item, pollsDTO);
                }
                if (prospectsDTO != null) {
                    loadAgendaItemAttachments(item, prospectsDTO);
                }
            }
        }
    }

    private static void loadAgendaItemPolls(AgendaItem item, PollsDTO pollsDTO) {
        log.info("loadAgendaItemPolls for item[{}]", item.getRef());
        if (pollsDTO.getPollDTOs() == null || pollsDTO.getPollDTOs().isEmpty()) {
            log.debug("No polls for agenda item: {}", item.getRef());
            return;
        }

        log.info(">> pollsDTO: {}", pollsDTO);
        item.setExtId(pollsDTO.getPollDTOs().getFirst().getAgendaItemId());

        for (PollDTO pollDTO : pollsDTO.getPollDTOs()) {
            log.debug("-- pollDTO: {}", pollDTO);
            Poll poll = new Poll();
            poll.setRef(PollsUtils.generateUniqueKeyReference());
            poll.setName(pollDTO.getName());
            poll.setExtAgendaItemId(pollDTO.getAgendaItemId());
            poll.setExtPollRouteId(pollDTO.getPollRoute());
            VotesCount vc = new VotesCount();
            vc.setVotedFor(pollDTO.getVotedFor());
            vc.setVotedAgainst(pollDTO.getVotedAgainst());
            vc.setNotVoted(pollDTO.getNotVoted());
            vc.setAbstain(pollDTO.getAbstain());
            vc.setAbsent(pollDTO.getAbsent());
            poll.setVotesCount(vc);
            poll.setVoters(pollDTO.getVoters());
            poll.setNote(pollDTO.getNote());
            //TODO: members...
            item.addPoll(poll);
        }
    }

    private static void loadAgendaItemAttachments(AgendaItem item, ProspectsDTO prospectsDTO) {
        log.info(String.format("loadAgendaItemAttachments for item[%s]", item.getRef()));
        if (prospectsDTO != null && prospectsDTO.getProspectDTOs() != null) {
            for (ProspectDTO prospectDTO : prospectsDTO.getProspectDTOs()) {
                AgendaItemAttachment attachment = new AgendaItemAttachment();
                attachment.setName(prospectDTO.getName());
                attachment.setRef(PollsUtils.generateUniqueKeyReference());
                attachment.setSource(prospectDTO.getSource());
                item.addAgendaItemAttachment(attachment);
            }
        }
    }

    private static void loadMeetingAttachments(Meeting meeting, AttachmentsDTO attachmentsDTO) {
        if (attachmentsDTO != null && attachmentsDTO.getAttachmentDTOs() != null) {
            for (AttachmentDTO attDTO : attachmentsDTO.getAttachmentDTOs()) {
                MeetingAttachment attachment = new MeetingAttachment();
                attachment.setName(attDTO.getName());
                attachment.setRef(PollsUtils.generateUniqueKeyReference());
                attachment.setSource(attDTO.getSource());
                meeting.addAttachment(attachment);
            }
        }
    }

    public static Poll parsePollDetail(Poll poll, Map<String, CouncilMember> membersMap, DMPollDetailResponse pollDetailResponse) {
        if (pollDetailResponse.getChildren() != null) {
            Set<Vote> votes = new HashSet<>();
            for (VoterDTO voterDTO : pollDetailResponse.getChildren()) {
                String name = PollsUtils.startWithFirstname(PollsUtils.toSimpleNameWithoutAccents(voterDTO.getName()));
                log.info("Voter: " + voterDTO.getName() + "\t => \t" + "Simple name: " + name);
                Vote vote = new Vote();
                vote.setVoterName(voterDTO.getName());
                vote.setCouncilMember(membersMap.get(name));
                vote.setPoll(poll);
                if (voterDTO.isVotedFor()) {
                    vote.setVoted(VoteChoice.VOTED_FOR);
                } else if (voterDTO.isVotedAgainst()) {
                    vote.setVoted(VoteChoice.VOTED_AGAINST);
                } else if (voterDTO.isNotVoted()) {
                    vote.setVoted(VoteChoice.NOT_VOTED);
                } else if (voterDTO.isAbstain()) {
                    vote.setVoted(VoteChoice.ABSTAIN);
                } else if (voterDTO.isAbsent()) {
                    vote.setVoted(VoteChoice.ABSENT);
                } else {
                    log.error("Unknown VoteChoice for " + voterDTO);
                }
                log.info("Vote: " + vote);
                votes.add(vote);
            }
            poll.setVotes(votes);
        }
        return poll;
    }

    public static List<Season> parseSeasonsResponse(DMSeasonsResponse seasonsResponse) {
        List<Season> seasons = new ArrayList<>();
        if (seasonsResponse.getSeasonDTOs() != null) {
            for (SeasonDTO seasonDTO : seasonsResponse.getSeasonDTOs()) {
                log.info("seasonDTO: " + seasonDTO);
                seasons.add(parseSeason(seasonDTO));
            }
        }
        return seasons;
    }

    public static List<Meeting> parseMeetingsResponse(Town town, Season season, Institution institution, DMMeetingsResponse meetingsResponse) throws ParseException {
        List<Meeting> meetings = new ArrayList<>();

        if (meetingsResponse == null || meetingsResponse.getSeasonMeetingsDTOs() == null
                || meetingsResponse.getSeasonMeetingsDTOs().isEmpty()) {
            log.warn("No meetings found for town: {}, season: {}", town.getRef(), season.getRef());
            return meetings;
        }

        SeasonMeetingsDTO seasonMeetingsDTO = meetingsResponse.getSeasonMeetingsDTOs().getFirst();
        if (seasonMeetingsDTO.getSeasonMeetingDTOs() == null || seasonMeetingsDTO.getSeasonMeetingDTOs().isEmpty()) {
            log.warn("No season meetings found for town: {}, season: {}", town.getRef(), season.getRef());
            return meetings;
        }

        for (SeasonMeetingDTO seasonMeetingDTO : seasonMeetingsDTO.getSeasonMeetingDTOs()) {
            Meeting meeting = new Meeting();
            meeting.setName(seasonMeetingDTO.getName());
            meeting.setExtId(seasonMeetingDTO.getId());
            meeting.setDate(PollsUtils.parseDMDate(seasonMeetingDTO.getDate()));
            meeting.setRef(PollsUtils.generateUniqueKeyReference()); // TODO: use stable reference
            meeting.setTown(town);
            meeting.setSeason(season);
            meeting.setInstitution(institution);
            meetings.add(meeting);
        }
        return meetings;
    }
}
