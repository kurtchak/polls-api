package org.blackbell.polls.meetings.source.dm;

import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.meetings.source.dm.api.response.*;
import org.blackbell.polls.meetings.source.dm.dto.*;
import org.blackbell.polls.meetings.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.*;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
public class DMParser {

    private static Map<String, CouncilMember> members = new HashMap<>();
    private static Map<String, Party> parties = new HashMap<>();

    private static final Logger log = LoggerFactory.getLogger(DMParser.class);

    private static AgendaItem parseAgendaItem(AgendaItemDTO itemDTO) {
        System.out.println("--> parseAgendaItem: " + itemDTO.getName());
        AgendaItem item = new AgendaItem();
        item.setName(itemDTO.getName());
        item.setRef(PollsUtils.generateUniqueKeyReference());
        for (AgendaItemComponentDTO componentDTO : itemDTO.getChildren()) {
            if (PollsDTO.class.equals(componentDTO.getClass())) {
                item.setPolls(parsePolls((PollsDTO) componentDTO));
            } else if (ProspectsDTO.class.equals(componentDTO.getClass())) {
                item.setAttachments(parseAgendaItemAttachments((ProspectsDTO) componentDTO));
            } else {
                log.warn("parseAgendaItem: Unknown agendaItems item component class: class[" + (itemDTO != null ? itemDTO.getClass() : "unknown") + "]");
            }
        }
        return item;
    }

    private static List<AgendaItemAttachment> parseAgendaItemAttachments(ProspectsDTO prospectsDTO) {
        System.out.println("---> parseAgendaItemAttachments: " + prospectsDTO.getName());
        List<AgendaItemAttachment> agendaItemAttachments = new ArrayList<>();
        if (prospectsDTO.getProspectDTOs() != null) {
            for (ProspectDTO prospectDTO : prospectsDTO.getProspectDTOs()) {
                agendaItemAttachments.add(parseAgendaItemAttachment(prospectDTO));
            }
        }
        return agendaItemAttachments;
    }

    private static AgendaItemAttachment parseAgendaItemAttachment(ProspectDTO prospectDTO) {
        System.out.println("----> parseAgendaItemAttachment: " + prospectDTO.getName());
        AgendaItemAttachment attachment = new AgendaItemAttachment();
        attachment.setName(prospectDTO.getName());
        attachment.setRef(PollsUtils.generateUniqueKeyReference());
        attachment.setSource(prospectDTO.getSource());
        return attachment;
    }

    private static List<Poll> parsePolls(PollsDTO pollsDTO) {
        System.out.println("---> parsePolls: " + pollsDTO.getName());
        List<Poll> polls = new ArrayList<>();
        if (pollsDTO.getPollDTOs() != null) {
            for (PollDTO pollDTO : pollsDTO.getPollDTOs()) {
                polls.add(parsePoll(pollDTO));
            }
        }
        return polls;
    }

    private static Poll parsePoll(PollDTO pollDTO) {
        System.out.println("----> parsePoll: " + pollDTO.getName());
        Poll poll = new Poll();
        poll.setName(pollDTO.getName());
        poll.setRef(PollsUtils.generateUniqueKeyReference());
        poll.setExtAgendaItemId(pollDTO.getAgendaItemId());
        poll.setExtPollRouteId(pollDTO.getPollRoute());
        // TODO: set counts
        return poll;
    }

    private static CouncilMember findOrIntroduceCouncilMember(Season season, String name, Map<String, CouncilMember> membersMap) {
        CouncilMember cm = findCouncilMember(name, membersMap);
        if (cm == null) {
            cm = introduceCouncilMember(season, name);
            members.put(cm.getName(), cm);
        }
        return cm;
    }

    private static CouncilMember findCouncilMember(String searchName, Map<String, CouncilMember> membersMap) {
        if (members.containsKey(searchName)) {
            return members.get(searchName);
        } else {
            for (String storedName : members.keySet()) {
                if (isSamePerson(searchName, storedName)) {
                    log.info("Already found the member with name '" + searchName + "' => merging with '" + storedName + "'");
                    return members.get(storedName);
                }
            }
        }
        return null;
    }

    private static boolean isSamePerson(String s1, String s2) {
        Set<String> a1 = new HashSet<>(Arrays.asList(s1.replaceAll(",", "").split(" ")));
        int a1size = a1.size();
        Set<String> a2 = new HashSet<>(Arrays.asList(s2.replaceAll(",", "").split(" ")));
        int a2size = a2.size();
        if (a1size > a2size) {
            a1.retainAll(a2);
            return a1.size() == a1size || a1.size() == a2size;
        } else {
            a2.retainAll(a1);
            return a2.size() == a1size || a2.size() == a2size;
        }
    }

    private static CouncilMember introduceCouncilMember(Season season, String name) {
        CouncilMember cm = new CouncilMember();
        cm.setName(name);
        cm.setRef(PollsUtils.generateUniqueKeyReference());
        switch (cm.getName()) {
            case "Ahlers Ján, MUDr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938808.jpg");
                cm.setEmail("jan.ahlers@centrum.sk");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SIEŤ"), cm, season));
                break;
            case "Andraščíková Štefánia, doc. PhDr.":
                cm.setPicture("http://www.presov.sk//portals_pictures/i_003938/i_3938813.jpg");
                cm.setEmail("stefania.andrascikova@unipo.sk");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season));
                break;
            case "Cvengroš Peter, MUDr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938822.jpg");
                cm.setEmail("peter.cvengros@post.sk");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SIEŤ"), cm, season));
                break;
            case "Dupkala Rudolf, PhDr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938837.jpg");
                cm.setEmail("rudolf.dupkala@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season));
                break;
            case "Komanický Mikuláš, PhDr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939026.jpg");
                cm.setEmail("mikulas.komanicky@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season));
                break;
            case "Antolová Marcela, PhDr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938985.jpg");
                cm.setEmail("marcela.holingova@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season));
                break;
            case "Drobňáková Valéria, ":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938825.jpg");
                cm.setEmail("valeriadrobnakova@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season));
                break;
            case "Drutarovský Richard, Ing.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938834.jpg");
                cm.setEmail("richard.drutarovsky@presov.sk");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NOVA"), cm, season));
                break;
            case "Benko Miroslav, PaedDr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938819.jpg");
                cm.setEmail("miroslavbenko@post.sk");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season));
                break;
            case "Bednárová Zuzana, RNDr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938814.jpg");
                cm.setEmail("zuzana.bednarova@presov.sk");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season));
                break;
            case "Ferenc Stanislav, Mgr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938969.jpg");
                cm.setEmail("ferenc.st@centrum.sk");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season));
                break;
            case "Langová Janette, Mgr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939040.jpg");
                cm.setEmail("jlangova63@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season));
                break;
            case "Malaga Ľudovít, Ing.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939044.jpg");
                cm.setEmail("ludo.malaga@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season));
                break;
            case "Mrouahová Daniela, MUDr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939053.jpg");
                cm.setEmail("mrouahova@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season));
                break;
            case "Pucher René, JUDr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939056.jpg");
                cm.setEmail("pucher.rene@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season));
                break;
            case "Ďurišin Martin, PhDr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938961.jpg");
                cm.setEmail("durisin.martin@gmail.com, martin.durisin@presov.sk");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SIEŤ"), cm, season));
                break;
            case "Fedorčíková Renáta, Ing.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938962.jpg");
                cm.setEmail("renafedorcikova@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season));
                break;
            case "Hermanovský Štefan, ":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938976.jpg");
                cm.setEmail("stefan.hermanovsky@presov.sk, hermanovsky.stefan@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season));
                break;
            case "Janko Vasiľ, MUDr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938991.jpg");
                cm.setEmail("vasil.janko@presov.sk, dr.janko.vasil@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season));
                break;
            case "Kollárová Marta, Ing.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939019.jpg");
                cm.setEmail("m.koll@pobox.sk");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season));
                break;
            case "Mochnacký Rastislav, Ing.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939048.jpg");
                cm.setEmail("rastislav.mochnacky@presov.sk");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season));
                break;
            case "Szidor Štefan, Ing.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939059.jpg");
                cm.setEmail("stefan.szidor@presov.sk");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season));
                break;
            case "Kutajová Jaroslava, Mgr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939032.jpg");
                cm.setEmail("jarka.kutajova@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season));
                break;
            case "Hudáč Juraj, Ing.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938986.jpg");
                cm.setEmail("juraj.hudac@presov.sk");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season));
                break;
            case "Lipka Martin, PhDr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939043.jpg");
                cm.setEmail("lipkaglobal@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season));
                break;
            case "Matejka Martin, Mgr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939047.jpg");
                cm.setEmail("mato.matejka@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season));
                break;
            case "Tkáčová Zuzana, ":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939064.jpg");
                cm.setEmail("tkacovaz20@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season));
                break;
            case "Ďurčanská Katarína, JUDr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938868.jpg");
                cm.setEmail("katarinadurcanska@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season));
                break;
            case "Kahanec Stanislav, Ing.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938994.jpg");
                cm.setEmail("stanislav.kahanec@gmail.com");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season));
                break;
            case "Kužma Štefan, Ing.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939037.jpg");
                cm.setEmail("stefan.kuzma@presov.sk");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season));
                break;
            case "Krajňák Peter, Mgr.":
                cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939027.jpg");
                cm.setEmail("peter.krajnak@presov.sk, peterkraj@centrum.sk");
                cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season));
                break;
        }
        cm.setSeason(season);
        if (season.getMembers() == null) {
            season.setMembers(new ArrayList<>());
        }
        if (!season.getMembers().contains(cm)) {
            season.getMembers().add(cm);
        }
        return cm;
    }

    private static List<PartyNominee> getPartyNominees(List<Party> parties, CouncilMember member, Season season) {
        List<PartyNominee> partyNominees = new ArrayList<>();
        for (Party party : parties) {
            partyNominees.add(new PartyNominee(party, member, season));
        }
        return partyNominees;
    }

    private static List<Party> findOrIntroduceParties(String... names) {
        List<Party> parties = new ArrayList<>();
        for (String name : names) {
            parties.add(findOrIntroduceParty(name));
        }
        return parties;
    }

    private static Party findOrIntroduceParty(String name) {
        if (!parties.containsKey(name)) {
            parties.put(name, introduceParty(name));
        }
        return parties.get(name);
    }

    private static Party introduceParty(String name) {
        return new Party(name);
    }

    public static Town parseTown(TownDTO townDTO) {
        Town town = new Town();
        town.setName(townDTO.getNazov());
        town.setRef(PollsUtils.generateUniqueKeyReference());
        return town;
    }

//    public static List<Season> parseSeasons(Institution institution, List<SeasonDTO> seasonDTOs) {
//        List<Season> seasons = new ArrayList<>();
//        for (SeasonDTO seasonDTO : seasonDTOs) {
//            seasons.add(parseSeason(institution, seasonDTO));
//        }
//        return seasons;
//    }

    private static Season parseSeason(Town town, Institution institution, SeasonDTO seasonDTO) {
        Season season = new Season();
        season.setTown(town);
        season.setName(seasonDTO.getName());
        season.setRef(town.getName() + "_" + institution + "_" + seasonDTO.getName());
        season.setInstitution(institution);
        return season;
    }

    private static List<Meeting> parseMeetings(List<MeetingDTO> meetingDTOs) {
        List<Meeting> meetings = new ArrayList<>();
        for (MeetingDTO meetingDTO : meetingDTOs) {
            try {
                Meeting meeting = new Meeting();
                meeting.setName(meetingDTO.getName());
                meeting.setDate(PollsUtils.parseSimpleDate(meetingDTO.getDate()));
                meeting.setExtId(meetingDTO.getId());
                meetings.add(meeting);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("MeetingComponent for meeting id: " + meetingDTO.getId() + " and name: " + meetingDTO.getName() + " was not loaded. Details: " + e.getMessage());
            }
        }
        return meetings;
    }

    public static Meeting parseMeetingResponse(Meeting meeting, DMMeetingResponse meetingResponse) throws Exception {
        // Agenda
        AgendaDTO agendaDTO =
                meetingResponse.getChildren().get(0).getClass().equals(AgendaDTO.class)
                        ? (AgendaDTO) meetingResponse.getChildren().get(0)
                        : (AgendaDTO) meetingResponse.getChildren().get(1);
        System.out.println("-> parseAgenda: " + agendaDTO.getName());
        for (AgendaItemDTO itemDTO : agendaDTO.getAgendaItemDTOs()) {
            syncMeetingAgendaItem(meeting, itemDTO);
//            meeting.addAgendaItem(parseAgendaItem(itemDTO));
        }

        // Attachments
        AttachmentsDTO attachmentsDTO =
                meetingResponse.getChildren().get(1).getClass().equals(AttachmentsDTO.class)
                        ? (AttachmentsDTO) meetingResponse.getChildren().get(1)
                        : (AttachmentsDTO) meetingResponse.getChildren().get(0);
        System.out.println("-> parseMeetingAttachmens: " + attachmentsDTO.getName());
        for (AttachmentDTO attDTO : attachmentsDTO.getAttachmentDTOs()) {
            syncMeetingAttachment(meeting, attDTO);
//            meeting.addAttachment(parseAttachment(attDTO));
        }
        return meeting;
    }

    private static void syncMeetingAgendaItem(Meeting meeting, AgendaItemDTO itemDTO) {
        for (AgendaItem item : meeting.getAgendaItems()) {
            if (item.getName().equals(itemDTO.getName()) && itemDTO.getChildren() != null && PollsDTO.class.equals(itemDTO.getChildren().get(0))) {
                PollsDTO pollsDTO = (PollsDTO) itemDTO.getChildren().get(0);
                item.setExtId(pollsDTO.getPollDTOs().get(0).getAgendaItemId());
                for (PollDTO pollDTO : ((PollsDTO) itemDTO.getChildren().get(0)).getPollDTOs()) {
                    for (Poll poll : item.getPolls()) {
                        if (poll.getName().equals(pollDTO.getName())) {
                            poll.setVotedFor(pollDTO.getVotedFor());
                            poll.setNotVoted(pollDTO.getNotVoted());
                            poll.setVoters(pollDTO.getVoters());
                            poll.setNote(pollDTO.getNote());
                            poll.setAbstain(pollDTO.getAbstain());
                            poll.setAbsent(pollDTO.getAbsent());
                            poll.setVotedAgainst(pollDTO.getVotedAgainst());
                        }
                    }
                }
            }
        }
    }

    private static void syncMeetingAttachment(Meeting meeting, AttachmentDTO attDTO) {
    }

    private static MeetingAttachment parseAttachment(AttachmentDTO attDTO) {
        MeetingAttachment attachment = new MeetingAttachment();
        attachment.setName(attDTO.getName());
        attachment.setRef(PollsUtils.generateUniqueKeyReference());
        attachment.setSource(attDTO.getSource());
        return attachment;
    }

    public static Poll parsePollDetail(Poll poll, Map<String, CouncilMember> membersMap, DMPollDetailResponse pollDetailResponse) {
        if (pollDetailResponse.getChildren() != null) {
            for (VoterDTO voterDTO : pollDetailResponse.getChildren()) {
                CouncilMember member = membersMap.get(PollsUtils.getSimpleName(voterDTO.getName()));
                if (voterDTO.isVotedFor()) {
                    poll.addVoteFor(member);
                } else if (voterDTO.isVotedAgainst()) {
                    poll.addVoteAgainst(member);
                } else if (voterDTO.isNotVoted()) {
                    poll.addNoVote(member);
                } else if (voterDTO.isAbstain()) {
                    poll.addAbstain(member);
                } else if (voterDTO.isAbsent()) {
                    poll.addAbsent(member);
                }
            }
        }
        return poll;
    }

    public static List<Town> parseTownsResponse(DMTownsResponse townsResponse) {
        List<Town> towns = new ArrayList<>();
        for (TownDTO townDTO : townsResponse.getTownDTOs()) {
            towns.add(parseTown(townDTO));
        }
        return towns;
    }

    public static List<Season> parseSeasonsResponse(Town town, DMSeasonsResponse seasonsResponse) {
        List<Season> seasons = new ArrayList<>();
        for (SeasonDTO seasonDTO : seasonsResponse.getSeasonDTOs()) {
            for (Institution institution : Institution.values()) {
                seasons.add(parseSeason(town, institution, seasonDTO));
            }
        }
        return seasons;
    }

    public static List<Meeting> parseMeetingsResponse(Season season, DMMeetingsResponse meetingsResponse) throws ParseException {
        List<Meeting> meetings = new ArrayList<>();
        for (SeasonMeetingDTO seasonMeetingDTO : meetingsResponse.getSeasonMeetingsDTOs().get(0).getSeasonMeetingDTOs()) {
            Meeting meeting = new Meeting();
            meeting.setName(seasonMeetingDTO.getName());
            meeting.setDate(PollsUtils.parseDMDate(seasonMeetingDTO.getDate()));
            meeting.setRef(PollsUtils.generateUniqueKeyReference()); // TODO:
            meeting.setSeason(season);
            meetings.add(meeting);
        }
        return meetings;
    }
}
