package org.blackbell.polls;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import org.blackbell.polls.api.MeetingsResponse;
import org.blackbell.polls.context.ApplicationContext;
import org.blackbell.polls.meetings.dto.*;
import org.blackbell.polls.meetings.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);
    public static final String GET_TOWN_COUNCIL_DATA_URL = "https://mesto-{city}.digitalnemesto.sk/DmApi/GetDZZasadnutie/mz/mesto-{city}";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

//    @Bean
//    public RestTemplate restTemplate(RestTemplateBuilder builder) {
//        return builder.build();
//    }
//
//    @Bean
//    public CommandLineRunner run() throws Exception {
//        return args -> {
//        };
//    }
//
    private static Meeting parseMeeting(Season season, int order, MeetingDTO meetingDTO) {
        try {
            Meeting meeting = new Meeting();
            meeting.setOrder(order);
            meeting.setName(meetingDTO.getName());
            meeting.setDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(meetingDTO.getDate().substring(0,19)));
            if (meetingDTO.getChildren() != null) {
                for (MeetingComponentDTO dto : meetingDTO.getChildren()) {
                    if (AgendaDTO.class.equals(dto.getClass())) {
                        meeting.setAgenda(parseAgenda(season, (AgendaDTO) dto));
                    } else if (AttachmentsDTO.class.equals(dto.getClass())) {
                        meeting.setAttachments(parseMeetingAttachmens(season, (AttachmentsDTO) dto));
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

    private static Map<Integer, MeetingAttachment> parseMeetingAttachmens(Season season, AttachmentsDTO attachmentsDTO) {
        Map<Integer, MeetingAttachment> attachments = new HashMap<>();
        int order = 1;
        for (AttachmentDTO attDTO : attachmentsDTO.getAttachmentDTOs()) {
            attachments.put(order, new MeetingAttachment(attDTO.getName(), order, attDTO.getSource()));
            order++;
        }
        return attachments;
    }

    private static Agenda parseAgenda(Season season, AgendaDTO agendaDTO) {
        Agenda agenda = new Agenda();
        Map<Integer, AgendaItem> items = new HashMap<>();
        int order = 1;
        for (AgendaItemDTO itemDTO : agendaDTO.getAgendaItemDTOs()) {
            items.put(order, parseAgendaItem(season, order, itemDTO));
            order++;
        }
        agenda.setItems(items);
        return agenda;
    }

    private static AgendaItem parseAgendaItem(Season season, int order, AgendaItemDTO itemDTO) {
        AgendaItem item = new AgendaItem();
        item.setName(itemDTO.getName());
        item.setOrder(order);
        for (AgendaItemComponentDTO componentDTO : itemDTO.getChildren()) {
            if (PollsDTO.class.equals(componentDTO.getClass())) {
                item.setPolls(parsePolls(season, (PollsDTO) componentDTO));
            } else if (ProspectsDTO.class.equals(componentDTO.getClass())) {
                item.setAttachments(parseAgendaItemAttachments(season, (ProspectsDTO) componentDTO));
            } else {
                log.warn("parseAgendaItem: Unknown agenda item component class: class[" + (itemDTO != null ? itemDTO.getClass() : "unknown") + "]");
            }
        }
        return item;
    }

    private static Map<String, AgendaItemAttachment> parseAgendaItemAttachments(Season season, ProspectsDTO prospectsDTO) {
        Map<String, AgendaItemAttachment> agendaItemAttachments = new HashMap<>();
        if (prospectsDTO.getProspectDTOs() != null) {
            for (ProspectDTO prospectDTO : prospectsDTO.getProspectDTOs()) {
                agendaItemAttachments.put(prospectDTO.getName(), new AgendaItemAttachment(prospectDTO.getName(), prospectDTO.getSource()));
            }
        }
        return agendaItemAttachments;
    }

    private static Map<Integer, Poll> parsePolls(Season season, PollsDTO pollsDTO) {
        Map<Integer, Poll> polls = new HashMap<>();
        if (pollsDTO.getPollDTOs() != null) {
            int order = 1;
            for (PollDTO pollDTO : pollsDTO.getPollDTOs()) {
                polls.put(order, parsePoll(season, order, pollDTO));
                order++;
            }
        }
        return polls;
    }

    private static Poll parsePoll(Season season, int order, PollDTO pollDTO) {
        Poll poll = new Poll();
        poll.setName(pollDTO.getName());
        poll.setOrder(order);
        if (pollDTO.getPollChoiceDTOs() != null) {
            for (PollChoiceDTO choice : pollDTO.getPollChoiceDTOs()) {
                switch (choice.getName()) {
                    case "Za": poll.setVotedFor(parseMembers(season, choice.getMembers())); break;
                    case "Proti": poll.setVotedAgainst(parseMembers(season, choice.getMembers())); break;
                    case "Nehlasoval": poll.setNotVoted(parseMembers(season, choice.getMembers())); break;
                    case "Zdržal sa": poll.setNotVoted(parseMembers(season, choice.getMembers())); break;
                    case "Chýbal na hlasovaní": poll.setAbsent(parseMembers(season, choice.getMembers())); break;
                }
            }
        }
        return poll;
    }

    private static List<CouncilMember> parseMembers(Season season, List<CouncilMemberDTO> membersDTO) {
        List<CouncilMember> members = new ArrayList<>();
        int order = 1;
        for (CouncilMemberDTO memberDTO : membersDTO) {
            CouncilMember cm = new CouncilMember();
            cm.setName(memberDTO.getName());
            cm.setOrder(order);
            if (season.getMembers() == null) {
                season.setMembers(new HashMap<>());
            }
            if (!season.getMembers().containsKey(order)) {
                season.getMembers().put(order, cm);
            }
            order++;
            members.add(cm);
        }
        return members;
    }

    private static Town loadTownData(String city) {
        String url = GET_TOWN_COUNCIL_DATA_URL.replace("{city}", city);
        MeetingsResponse meetingsResponse = new RestTemplate().getForObject(url, MeetingsResponse.class);
        Town town = new Town();
        town.setName(city);
        Map<String, Season> seasonMap = new HashMap<>();
        for (SeasonDTO seasonDTO : meetingsResponse.getSeasonDTOs()) {
            Season season = new Season();
            season.setName(seasonDTO.getName());
            Map<Integer, Meeting> meetingsMap = new HashMap<>();
            int order = 1;
            for (MeetingDTO meetingDTO : seasonDTO.getMeetingDTOs()) {
                meetingsMap.put(order, parseMeeting(season, order, meetingDTO));
                order++;
            }
            season.setMeetings(meetingsMap);
            seasonMap.put(seasonDTO.getName(), season);
        }
        town.setSeasons(seasonMap);
        return town;
    }

    public static void checkLoaded(String city) {
        ApplicationContext context = ApplicationContext.getInstance();
        if (!context.getTownsMap().containsKey(city)) {
            Town town = Application.loadTownData(city);
            ApplicationContext.getInstance().getTownsMap().put(city, town);
        }
    }
}