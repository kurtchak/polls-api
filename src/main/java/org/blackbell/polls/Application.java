package org.blackbell.polls;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import org.blackbell.polls.meetings.model.CouncilMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class Application {

    private static Map<String, CouncilMember> members = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static final String GET_TOWN_COUNCIL_DATA_URL = "https://mesto-{city}.digitalnemesto.sk/DmApi/GetDZZasadnutie/{institution}/mesto-{city}";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

//    private static Meeting parseMeeting(Season season, String ref, MeetingDTO meetingDTO) {
//        try {
//            Meeting meeting = new Meeting();
//            meeting.setRef(ref);
//            meeting.setName(meetingDTO.getName());
//            meeting.setDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(meetingDTO.getDate().substring(0,19)));
//            meeting.setSeason(season);
//            if (meetingDTO.getChildren() != null) {
//                for (MeetingComponentDTO dto : meetingDTO.getChildren()) {
//                    if (AgendaDTO.class.equals(dto.getClass())) {
//                        meeting.setAgendaItems(parseAgenda(season, meeting, (AgendaDTO) dto));
//                    } else if (AttachmentsDTO.class.equals(dto.getClass())) {
//                        meeting.setAttachments(parseMeetingAttachmens(season, meeting, (AttachmentsDTO) dto));
//                    } else {
//                        log.warn("parseMeeting: Unknown meeting component class: class[" + (dto != null ? dto.getClass() : "unknown") + "]");
//                    }
//                }
//            }
//            return meeting;
//        } catch (ParseException e) {
//            e.printStackTrace();
//            return null; //TODO
//        }
//    }
//
//    private static List<MeetingAttachment> parseMeetingAttachmens(Season season, Meeting meeting, AttachmentsDTO attachmentsDTO) {
//        List<MeetingAttachment> attachments = new ArrayList<>();
//        for (AttachmentDTO attDTO : attachmentsDTO.getAttachmentDTOs()) {
//            String ref = generateUniqueKeyReference();
//            attachments.add(new MeetingAttachment(attDTO.getName(), meeting, ref, attDTO.getSource()));
//        }
//        return attachments;
//    }
//
//    private static List<AgendaItem> parseAgenda(Season season, Meeting meeting, AgendaDTO agendaDTO) {
//        List<AgendaItem> items = new ArrayList<>();
//        for (AgendaItemDTO itemDTO : agendaDTO.getAgendaItemDTOs()) {
//            String ref = generateUniqueKeyReference();
//            items.add(parseAgendaItem(season, meeting, ref, itemDTO));
//        }
//        return items;
//    }
//
//    private static AgendaItem parseAgendaItem(Season season, Meeting meeting, String ref, AgendaItemDTO itemDTO) {
//        AgendaItem item = new AgendaItem();
//        item.setName(itemDTO.getName());
//        item.setRef(ref);
//        item.setMeeting(meeting);
//        for (AgendaItemComponentDTO componentDTO : itemDTO.getChildren()) {
//            if (PollsDTO.class.equals(componentDTO.getClass())) {
//                item.setPolls(parsePolls(season, item, (PollsDTO) componentDTO));
//            } else if (ProspectsDTO.class.equals(componentDTO.getClass())) {
//                item.setAttachments(parseAgendaItemAttachments(season, item, (ProspectsDTO) componentDTO));
//            } else {
//                log.warn("parseAgendaItem: Unknown agendaItems item component class: class[" + (itemDTO != null ? itemDTO.getClass() : "unknown") + "]");
//            }
//        }
//        return item;
//    }
//
//    private static List<AgendaItemAttachment> parseAgendaItemAttachments(Season season, AgendaItem item, ProspectsDTO prospectsDTO) {
//        List<AgendaItemAttachment> agendaItemAttachments = new ArrayList<>();
//        if (prospectsDTO.getProspectDTOs() != null) {
//            for (ProspectDTO prospectDTO : prospectsDTO.getProspectDTOs()) {
//                agendaItemAttachments.add(new AgendaItemAttachment(prospectDTO.getName(), item, prospectDTO.getSource()));
//            }
//        }
//        return agendaItemAttachments;
//    }
//
//    private static List<Poll> parsePolls(Season season, AgendaItem item, PollsDTO pollsDTO) {
//        List<Poll> polls = new ArrayList<>();
//        if (pollsDTO.getPollDTOs() != null) {
//            for (PollDTO pollDTO : pollsDTO.getPollDTOs()) {
//                String ref = generateUniqueKeyReference();
//                polls.add(parsePoll(season, item, ref, pollDTO));
//            }
//        }
//        return polls;
//    }
//
//    private static Poll parsePoll(Season season, AgendaItem item, String ref, PollDTO pollDTO) {
//        Poll poll = new Poll();
//        poll.setName(pollDTO.getName());
//        poll.setRef(ref);
//        poll.setAgendaItem(item);
//        if (pollDTO.getPollChoiceDTOs() != null) {
//            List<Vote> votes = new ArrayList<>();
//            for (PollChoiceDTO choice : pollDTO.getPollChoiceDTOs()) {
//                votes.addAll(parseMembers(season, poll, choice, choice.getMembers()));
////                switch (choice.getName()) {
////                    case "Za": poll.setVotesFor(parseMembers(season, poll, choice, choice.getMembers())); break;
////                    case "Proti": poll.setVotesAgainst(parseMembers(season, poll, choice, choice.getMembers())); break;
////                    case "Nehlasoval": poll.setNoVotes(parseMembers(season, poll, choice, choice.getMembers())); break;
////                    case "Zdržal sa": poll.setAbstains(parseMembers(season, poll, choice, choice.getMembers())); break;
////                    case "Chýbal na hlasovaní": poll.setAbsents(parseMembers(season, poll, choice, choice.getMembers())); break;
////                }
//            }
////            poll.setVotes(votes);
//        }
//        return poll;
//    }
//
//    private static String generateUniqueKeyReference() {
//        return "" + System.nanoTime();
//    }
//
//    private static List<Vote> parseMembers(Season season, Poll poll, PollChoiceDTO choice, List<CouncilMemberDTO> membersDTO) {
//        List<Vote> votes = new ArrayList<>();
//        for (CouncilMemberDTO memberDTO : membersDTO) {
//            CouncilMember cm = findOrIntroduceCouncilMember(season, memberDTO);
////            Vote vote = new Vote();
////            vote.setPoll(poll);
////            vote.setCouncilMember(cm);
////            switch (choice.getName()) {
////                case "Za": vote.setVoted(VoteChoiceEnum.VOTED_FOR); break;
////                case "Proti": vote.setVoted(VoteChoiceEnum.VOTED_AGAINST); break;
////                case "Zdržal sa": vote.setVoted(VoteChoiceEnum.NOT_VOTED); break;
////                case "Nehlasoval": vote.setVoted(VoteChoiceEnum.ABSTAIN); break;
////                case "Chýbal na hlasovaní": vote.setVoted(VoteChoiceEnum.ABSENT); break;
////                default:
////                    vote.setVoted(VoteChoiceEnum.NOT_VOTED);
////            }
////            votes.add(vote);
//        }
//        return votes;
//    }
//
//    private static CouncilMember findOrIntroduceCouncilMember(Season season, CouncilMemberDTO memberDTO) {
//        CouncilMember cm = null;
//        if (members.containsKey(memberDTO.getName())) {
//            cm = members.get(memberDTO.getName());
//        } else {
//            cm = introduceCouncilMember(season, memberDTO);
//            members.put(cm.getName(), cm);
//        }
//        return cm;
//    }
//
//    private static CouncilMember introduceCouncilMember(Season season, CouncilMemberDTO memberDTO) {
//        CouncilMember cm;
//        cm = new CouncilMember();
//        cm.setName(memberDTO.getName());
//        cm.setRef(generateUniqueKeyReference());
//        switch (cm.getName()) {
//            case "Cvengroš Peter, MUDr.": cm.setPicture("/portals_pictures/i_003938/i_3938822.jpg"); break;
//            case "Dupkala Rudolf, PhDr.": cm.setPicture("/portals_pictures/i_003938/i_3938837.jpg"); break;
//            case "Komanický Mikuláš, PhDr.": cm.setPicture("/portals_pictures/i_003939/i_3939026.jpg"); break;
//            case "Langová Janette, Mgr.": cm.setPicture("/portals_pictures/i_003939/i_3939040.jpg"); break;
//            case "Malaga Ľudovít, Ing.": cm.setPicture("/portals_pictures/i_003939/i_3939044.jpg"); break;
//            case "Mrouahová Daniela, MUDr.": cm.setPicture("/portals_pictures/i_003939/i_3939053.jpg"); break;
//            case "Pucher René, JUDr.": cm.setPicture("/portals_pictures/i_003939/i_3939056.jpg"); break;
//            case "Ahlers Ján, MUDr.": cm.setPicture("/portals_pictures/i_003938/i_3938808.jpg"); break;
//            case "Antolová Marcela, PhDr.": cm.setPicture("/portals_pictures/i_003938/i_3938985.jpg"); break;
//            case "Drobňáková Valéria, ": cm.setPicture("/portals_pictures/i_003938/i_3938825.jpg"); break;
//            case "Drutarovský Richard, Ing.": cm.setPicture("/portals_pictures/i_003938/i_3938834.jpg"); break;
//            case "Ďurišin Martin, PhDr.": cm.setPicture("/portals_pictures/i_003938/i_3938961.jpg"); break;
//            case "Fedorčíková Renáta, Ing.": cm.setPicture("/portals_pictures/i_003938/i_3938962.jpg"); break;
//            case "Hermanovský Štefan, ": cm.setPicture("/portals_pictures/i_003938/i_3938976.jpg"); break;
//            case "Janko Vasiľ, MUDr.": cm.setPicture("/portals_pictures/i_003938/i_3938991.jpg"); break;
//            case "Kollárová Marta, Ing.": cm.setPicture("/portals_pictures/i_003939/i_3939019.jpg"); break;
//            case "Mochnacký Rastislav, Ing.": cm.setPicture("/portals_pictures/i_003939/i_3939048.jpg"); break;
//            case "Szidor Štefan, Ing.": cm.setPicture("/portals_pictures/i_003939/i_3939059.jpg"); break;
//            case "Kutajová Jaroslava, Mgr.": cm.setPicture("/portals_pictures/i_003939/i_3939032.jpg"); break;
//            case "Andraščíková Štefánia, doc. PhDr.": cm.setPicture("/portals_pictures/i_003938/i_3938813.jpg"); break;
//            case "Benko Miroslav, PaedDr.": cm.setPicture("/portals_pictures/i_003938/i_3938819.jpg"); break;
//            case "Hudáč Juraj, Ing.": cm.setPicture("/portals_pictures/i_003938/i_3938986.jpg"); break;
//            case "Lipka Martin, PhDr.": cm.setPicture("/portals_pictures/i_003939/i_3939043.jpg"); break;
//            case "Matejka Martin, Mgr.": cm.setPicture("/portals_pictures/i_003939/i_3939047.jpg"); break;
//            case "Tkáčová Zuzana, ": cm.setPicture("/portals_pictures/i_003939/i_3939064.jpg"); break;
//            case "Bednárová Zuzana, RNDr.": cm.setPicture("/portals_pictures/i_003938/i_3938814.jpg"); break;
//            case "Ďurčanská Katarína, JUDr.": cm.setPicture("/portals_pictures/i_003938/i_3938868.jpg"); break;
//            case "Ferenc Stanislav, Mgr.": cm.setPicture("/portals_pictures/i_003938/i_3938969.jpg"); break;
//            case "Kahanec Stanislav, Ing.": cm.setPicture("/portals_pictures/i_003938/i_3938994.jpg"); break;
//            case "Kužma Štefan, Ing.": cm.setPicture("/portals_pictures/i_003939/i_3939037.jpg"); break;
//            case "Krajňák Peter, Mgr.": cm.setPicture("/portals_pictures/i_003939/i_3939027.jpg"); break;
//        }
//        cm.setSeason(season);
//        if (season.getMembers() == null) {
//            season.setMembers(new ArrayList<>());
//        }
//        if (!season.getMembers().contains(cm)) {
//            season.getMembers().add(cm);
//        }
//        return cm;
//    }
//
//    public static Town loadTownData(String city, String institution) throws Exception {
//        String url = GET_TOWN_COUNCIL_DATA_URL.replace("{city}", city).replace("{institution}", institution);
//        MeetingsResponse meetingsResponse = new RestTemplate().getForObject(url, MeetingsResponse.class);
//        if (meetingsResponse == null) {
//            throw new Exception("No town loaded");
//        }
//        Town town = new Town();
//        town.setRef(city);
//        town.setName(city);
//        List<Season> seasons = new ArrayList<>();
//        for (SeasonDTO seasonDTO : meetingsResponse.getSeasonDTOs()) {
//            Season season = new Season();
//            season.setName(seasonDTO.getName());
//            season.setRef(seasonDTO.getName());
//            season.setTown(town);
//            List<Meeting> meetingsMap = new ArrayList<>();
//            for (MeetingDTO meetingDTO : seasonDTO.getMeetingDTOs()) {
//                String ref = generateUniqueKeyReference();
//                meetingsMap.add(parseMeeting(season, ref, meetingDTO));
//            }
//            season.setMeetings(meetingsMap);
//            seasons.add(season);
//        }
//        town.setSeasons(seasons);
//        return town;
//    }

}