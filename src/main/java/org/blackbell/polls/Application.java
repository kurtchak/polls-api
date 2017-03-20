package org.blackbell.polls;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import org.blackbell.polls.api.MeetingsResponse;
import org.blackbell.polls.meetings.dto.*;
import org.blackbell.polls.meetings.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static final String GET_TOWN_COUNCIL_DATA_URL = "https://mesto-{city}.digitalnemesto.sk/DmApi/GetDZZasadnutie/mz/mesto-{city}";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    private static Meeting parseMeeting(Season season, String ref, MeetingDTO meetingDTO) {
        try {
            Meeting meeting = new Meeting();
            meeting.setRef(ref);
            meeting.setName(meetingDTO.getName());
            meeting.setDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(meetingDTO.getDate().substring(0,19)));
            meeting.setSeason(season);
            if (meetingDTO.getChildren() != null) {
                for (MeetingComponentDTO dto : meetingDTO.getChildren()) {
                    if (AgendaDTO.class.equals(dto.getClass())) {
                        meeting.setAgendaItems(parseAgenda(season, meeting, (AgendaDTO) dto));
                    } else if (AttachmentsDTO.class.equals(dto.getClass())) {
                        meeting.setAttachments(parseMeetingAttachmens(season, meeting, (AttachmentsDTO) dto));
                    } else {
                        log.warn("parseMeeting: Unknown meeting component class: class[" + (dto != null ? dto.getClass() : "unknown") + "]");
                    }
                }
            }
            return meeting;
        } catch (ParseException e) {
            e.printStackTrace();
            return null; //TODO
        }
    }

    private static List<MeetingAttachment> parseMeetingAttachmens(Season season, Meeting meeting, AttachmentsDTO attachmentsDTO) {
        List<MeetingAttachment> attachments = new ArrayList<>();
        for (AttachmentDTO attDTO : attachmentsDTO.getAttachmentDTOs()) {
            String ref = generateUniqueKeyReference();
            attachments.add(new MeetingAttachment(attDTO.getName(), meeting, ref, attDTO.getSource()));
        }
        return attachments;
    }

    private static List<AgendaItem> parseAgenda(Season season, Meeting meeting, AgendaDTO agendaDTO) {
        List<AgendaItem> items = new ArrayList<>();
        for (AgendaItemDTO itemDTO : agendaDTO.getAgendaItemDTOs()) {
            String ref = generateUniqueKeyReference();
            items.add(parseAgendaItem(season, meeting, ref, itemDTO));
        }
        return items;
    }

    private static AgendaItem parseAgendaItem(Season season, Meeting meeting, String ref, AgendaItemDTO itemDTO) {
        AgendaItem item = new AgendaItem();
        item.setName(itemDTO.getName());
        item.setRef(ref);
        item.setMeeting(meeting);
        for (AgendaItemComponentDTO componentDTO : itemDTO.getChildren()) {
            if (PollsDTO.class.equals(componentDTO.getClass())) {
                item.setPolls(parsePolls(season, item, (PollsDTO) componentDTO));
            } else if (ProspectsDTO.class.equals(componentDTO.getClass())) {
                item.setAttachments(parseAgendaItemAttachments(season, item, (ProspectsDTO) componentDTO));
            } else {
                log.warn("parseAgendaItem: Unknown agendaItems item component class: class[" + (itemDTO != null ? itemDTO.getClass() : "unknown") + "]");
            }
        }
        return item;
    }

    private static List<AgendaItemAttachment> parseAgendaItemAttachments(Season season, AgendaItem item, ProspectsDTO prospectsDTO) {
        List<AgendaItemAttachment> agendaItemAttachments = new ArrayList<>();
        if (prospectsDTO.getProspectDTOs() != null) {
            for (ProspectDTO prospectDTO : prospectsDTO.getProspectDTOs()) {
                agendaItemAttachments.add(new AgendaItemAttachment(prospectDTO.getName(), item, prospectDTO.getSource()));
            }
        }
        return agendaItemAttachments;
    }

    private static List<Poll> parsePolls(Season season, AgendaItem item, PollsDTO pollsDTO) {
        List<Poll> polls = new ArrayList<>();
        if (pollsDTO.getPollDTOs() != null) {
            for (PollDTO pollDTO : pollsDTO.getPollDTOs()) {
                String ref = generateUniqueKeyReference();
                polls.add(parsePoll(season, item, ref, pollDTO));
            }
        }
        return polls;
    }

    private static Poll parsePoll(Season season, AgendaItem item, String ref, PollDTO pollDTO) {
        Poll poll = new Poll();
        poll.setName(pollDTO.getName());
        poll.setRef(ref);
        poll.setAgendaItem(item);
        if (pollDTO.getPollChoiceDTOs() != null) {
            List<Vote> votes = new ArrayList<>();
            for (PollChoiceDTO choice : pollDTO.getPollChoiceDTOs()) {
                votes.addAll(parseMembers(season, poll, choice, choice.getMembers()));
//                switch (choice.getName()) {
//                    case "Za": poll.setVotedFor(parseMembers(season, poll, choice, choice.getMembers())); break;
//                    case "Proti": poll.setVotedAgainst(parseMembers(season, poll, choice, choice.getMembers())); break;
//                    case "Nehlasoval": poll.setNotVoted(parseMembers(season, poll, choice, choice.getMembers())); break;
//                    case "Zdržal sa": poll.setNotVoted(parseMembers(season, poll, choice, choice.getMembers())); break;
//                    case "Chýbal na hlasovaní": poll.setAbsent(parseMembers(season, poll, choice, choice.getMembers())); break;
//                }
            }
            poll.setVotes(votes);
        }
        return poll;
    }

    private static String generateUniqueKeyReference() {
        return "" + System.nanoTime();
    }

    private static List<Vote> parseMembers(Season season, Poll poll, PollChoiceDTO choice, List<CouncilMemberDTO> membersDTO) {
        List<Vote> votes = new ArrayList<>();
        for (CouncilMemberDTO memberDTO : membersDTO) {
            CouncilMember cm = new CouncilMember();
            cm.setName(memberDTO.getName());
            String ref = generateUniqueKeyReference();
            cm.setRef(ref);
            if (season.getMembers() == null) {
                season.setMembers(new ArrayList<>());
            }
            if (!season.getMembers().contains(cm)) {
                season.getMembers().add(cm);
            }
            Vote vote = new Vote();
            vote.setPoll(poll);
            vote.setCouncilMember(cm);
            switch (choice.getName()) {
                case "votedFor": vote.setVoted(VoteEnum.VOTED_FOR); break;
                case "votedAgainst": vote.setVoted(VoteEnum.VOTED_AGAINST); break;
                case "abstain": vote.setVoted(VoteEnum.NOT_VOTED); break;
                case "notVoted": vote.setVoted(VoteEnum.ABSTAIN); break;
                case "absentMembers": vote.setVoted(VoteEnum.ABSENT); break;
            }
        }
        return votes;
    }

    public static Town loadTownData(String city) {
        String url = GET_TOWN_COUNCIL_DATA_URL.replace("{city}", city);
        MeetingsResponse meetingsResponse = new RestTemplate().getForObject(url, MeetingsResponse.class);
        Town town = new Town();
        town.setRef(city);
        town.setName(city);
        List<Season> seasonMap = new ArrayList<>();
        for (SeasonDTO seasonDTO : meetingsResponse.getSeasonDTOs()) {
            Season season = new Season();
            season.setName(seasonDTO.getName());
            season.setTown(town);
            List<Meeting> meetingsMap = new ArrayList<>();
            for (MeetingDTO meetingDTO : seasonDTO.getMeetingDTOs()) {
                String ref = generateUniqueKeyReference();
                meetingsMap.add(parseMeeting(season, ref, meetingDTO));
            }
            season.setMeetings(meetingsMap);
            seasonMap.add(season);
        }
        town.setSeasons(seasonMap);
        return town;
    }

}