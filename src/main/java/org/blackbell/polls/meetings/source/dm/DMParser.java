package org.blackbell.polls.meetings.source.dm;

import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.meetings.model.*;
import org.blackbell.polls.meetings.model.embeddable.VotesCount;
import org.blackbell.polls.meetings.source.dm.api.response.DMMeetingResponse;
import org.blackbell.polls.meetings.source.dm.api.response.DMMeetingsResponse;
import org.blackbell.polls.meetings.source.dm.api.response.DMPollDetailResponse;
import org.blackbell.polls.meetings.source.dm.api.response.DMSeasonsResponse;
import org.blackbell.polls.meetings.source.dm.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
public class DMParser {

    private static final Logger log = LoggerFactory.getLogger(DMParser.class);

    private static Season parseSeason(Town town, Institution institution, SeasonDTO seasonDTO) {
        Season season = new Season();
//        season.setTown(town);
        season.setName(seasonDTO.getName());
        season.setRef(seasonDTO.getName());
        //season.setRef(town.getName() + "_" + institution + "_" + seasonDTO.getName());
//        season.setInstitution(institution);
        return season;
    }

    public static Meeting parseMeetingResponse(Meeting meeting, DMMeetingResponse meetingResponse) throws Exception {
        // Agenda
        AgendaDTO agendaDTO =
                meetingResponse.getChildren().get(0).getClass().equals(AgendaDTO.class)
                        ? (AgendaDTO) meetingResponse.getChildren().get(0)
                        : (AgendaDTO) meetingResponse.getChildren().get(1);
        log.info("-> parseAgenda: " + agendaDTO.getName());
        loadAgenda(meeting, agendaDTO);

        // Attachments
        AttachmentsDTO attachmentsDTO =
                meetingResponse.getChildren().get(1).getClass().equals(AttachmentsDTO.class)
                        ? (AttachmentsDTO) meetingResponse.getChildren().get(1)
                        : (AttachmentsDTO) meetingResponse.getChildren().get(0);
        log.info("-> parseMeetingAttachmens: " + attachmentsDTO.getName());
        loadMeetingAttachments(meeting, attachmentsDTO);
        return meeting;
    }

    private static void loadAgenda(Meeting meeting, AgendaDTO agendaDTO) {
        log.info(String.format("loadAgenda for meeting[%s]", meeting.getRef()));
        for (AgendaItemDTO agendaItemDTO : agendaDTO.getAgendaItemDTOs()) {
            AgendaItem item = new AgendaItem();
            item.setName(agendaItemDTO.getName());
            item.setRef(PollsUtils.generateUniqueKeyReference());
            if (agendaItemDTO != null && agendaItemDTO.getChildren() != null
                    && !agendaItemDTO.getChildren().isEmpty()) {
                PollsDTO pollsDTO = PollsDTO.class.equals(agendaItemDTO.getChildren().get(0).getClass())
                        ? (PollsDTO) agendaItemDTO.getChildren().get(0)
                        : (PollsDTO) agendaItemDTO.getChildren().get(1);
                loadAgendaItemPolls(item, pollsDTO);

                ProspectsDTO prospectsDTO = ProspectsDTO.class.equals(agendaItemDTO.getChildren().get(0).getClass())
                        ? (ProspectsDTO) agendaItemDTO.getChildren().get(0)
                        : (ProspectsDTO) agendaItemDTO.getChildren().get(1);
                loadAgendaItemAttachments(item, prospectsDTO);
            }
            meeting.addAgendaItem(item);
        }
    }

    private static void loadAgendaItemPolls(AgendaItem item, PollsDTO pollsDTO) {
        log.info(String.format("loadAgendaItemPolls for item[%s]", item.getRef()));
        if (pollsDTO.getPollDTOs() != null
                && pollsDTO.getPollDTOs() != null
                && !pollsDTO.getPollDTOs().isEmpty()) {
            log.info(">> pollsDTO: " + pollsDTO.toString());
            item.setExtId(pollsDTO.getPollDTOs().get(0).getAgendaItemId());
            for (PollDTO pollDTO : pollsDTO.getPollDTOs()) {
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
            for (VoterDTO voterDTO : pollDetailResponse.getChildren()) {
                String name = PollsUtils.startWithFirstname(PollsUtils.getSimpleName(voterDTO.getName()));
//                log.info("Voter: " + voterDTO.getName() + "\t => \t" + "Simple name: " + name);
                CouncilMember member = membersMap.get(name);
                //TODO: prerobit
//                Votes votes = new Votes();
//                if (voterDTO.isVotedFor()) {
//                    votes.addVoteFor(member);
//                } else if (voterDTO.isVotedAgainst()) {
//                    votes.addVoteAgainst(member);
//                } else if (voterDTO.isNotVoted()) {
//                    votes.addNoVote(member);
//                } else if (voterDTO.isAbstain()) {
//                    votes.addAbstain(member);
//                } else if (voterDTO.isAbsent()) {
//                    votes.addAbsent(member);
//                }
//                poll.setVotesMap(votes);
            }
        }
        return poll;
    }

    public static List<Season> parseSeasonsResponse(DMSeasonsResponse seasonsResponse) {
        List<Season> seasons = new ArrayList<>();
        if (seasonsResponse.getSeasonDTOs() != null) {
            for (SeasonDTO seasonDTO : seasonsResponse.getSeasonDTOs()) {
//                log.info("seasonDTO: " + seasonDTO);
//TODO:odkomentovat                for (InstitutionType institution : InstitutionType.values()) {
//                    seasons.add(parseSeason(town, InstitutionType.ZASTUPITELSTVO, seasonDTO));
//                }
            }
        }
        return seasons;
    }

    public static List<Meeting> parseMeetingsResponse(Season season, DMMeetingsResponse meetingsResponse) throws ParseException {
        List<Meeting> meetings = new ArrayList<>();
        for (SeasonMeetingDTO seasonMeetingDTO : meetingsResponse.getSeasonMeetingsDTOs().get(0).getSeasonMeetingDTOs()) {
            Meeting meeting = new Meeting();
            meeting.setName(seasonMeetingDTO.getName());
            meeting.setExtId(seasonMeetingDTO.getId());
            meeting.setDate(PollsUtils.parseDMDate(seasonMeetingDTO.getDate()));
            meeting.setRef(PollsUtils.generateUniqueKeyReference()); // TODO:
            meeting.setSeason(season);
            meetings.add(meeting);
        }
        return meetings;
    }
}
