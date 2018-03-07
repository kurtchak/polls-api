package org.blackbell.polls.meetings.source.dm;

import org.blackbell.polls.common.PollDateUtils;
import org.blackbell.polls.meetings.source.SyncAgent;
import org.blackbell.polls.meetings.source.dm.api.response.DMMeetingResponse;
import org.blackbell.polls.meetings.source.dm.api.response.DMMeetingsResponse;
import org.blackbell.polls.meetings.source.dm.api.response.DMPollDetailResponse;
import org.blackbell.polls.meetings.source.dm.dto.*;
import org.blackbell.polls.meetings.model.*;
import org.blackbell.polls.meetings.model.vote.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
public class DMParser {

    private static Map<String, CouncilMember> members = new HashMap<>();
    private static Map<String, Party> parties = new HashMap<>();

    private static final Logger log = LoggerFactory.getLogger(DMParser.class);

    private static void parseMeetingAttachmens(Meeting meeting, AttachmentsDTO attachmentsDTO) {
        System.out.println("-> parseMeetingAttachmens: " + attachmentsDTO.getName());
        for (AttachmentDTO attDTO : attachmentsDTO.getAttachmentDTOs()) {
            meeting.addAttachment(new MeetingAttachment(attDTO.getName(), meeting, SyncAgent.generateUniqueKeyReference(), attDTO.getSource()));
        }
    }

    private static void parseAgenda(Meeting meeting, AgendaDTO agendaDTO) {
        System.out.println("-> parseAgenda: " + agendaDTO.getName());
        for (AgendaItemDTO itemDTO : agendaDTO.getAgendaItemDTOs()) {
            meeting.addAgendaItem(parseAgendaItem(itemDTO));
        }
    }

    private static AgendaItem parseAgendaItem(AgendaItemDTO itemDTO) {
        System.out.println("--> parseAgendaItem: " + itemDTO.getName());
        AgendaItem item = new AgendaItem();
        item.setName(itemDTO.getName());
        item.setRef(SyncAgent.generateUniqueKeyReference());
        for (AgendaItemComponentDTO componentDTO : itemDTO.getChildren()) {
            if (PollsDTO.class.equals(componentDTO.getClass())) {
                item.setPolls(parsePolls(item, (PollsDTO) componentDTO));
            } else if (ProspectsDTO.class.equals(componentDTO.getClass())) {
                item.setAttachments(parseAgendaItemAttachments(item, (ProspectsDTO) componentDTO));
            } else {
                log.warn("parseAgendaItem: Unknown agendaItems item component class: class[" + (itemDTO != null ? itemDTO.getClass() : "unknown") + "]");
            }
        }
        return item;
    }

    private static List<AgendaItemAttachment> parseAgendaItemAttachments(AgendaItem item, ProspectsDTO prospectsDTO) {
        System.out.println("---> parseAgendaItemAttachments: " + prospectsDTO.getName());
        List<AgendaItemAttachment> agendaItemAttachments = new ArrayList<>();
        if (prospectsDTO.getProspectDTOs() != null) {
            for (ProspectDTO prospectDTO : prospectsDTO.getProspectDTOs()) {
                agendaItemAttachments.add(new AgendaItemAttachment(prospectDTO.getName(), item, SyncAgent.generateUniqueKeyReference(), prospectDTO.getSource()));
            }
        }
        return agendaItemAttachments;
    }

    private static List<Poll> parsePolls(AgendaItem item, PollsDTO pollsDTO) {
        System.out.println("---> parsePolls: " + pollsDTO.getName());
        List<Poll> polls = new ArrayList<>();
        if (pollsDTO.getPollDTOs() != null) {
            for (PollDTO pollDTO : pollsDTO.getPollDTOs()) {
                polls.add(parsePoll(item, pollDTO));
            }
        }
        return polls;
    }

    private static Poll parsePoll(AgendaItem item, PollDTO pollDTO) {
        System.out.println("----> parsePoll: " + pollDTO.getName());
        Poll poll = new Poll();
        poll.setName(pollDTO.getName());
        poll.setRef(SyncAgent.generateUniqueKeyReference());
        poll.setExtAgendaItemId(pollDTO.getAgendaItemId());
        poll.setExtPollRouteId(pollDTO.getPollRoute());
        poll.setAgendaItem(item);
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
        cm.setRef(SyncAgent.generateUniqueKeyReference());
        switch (cm.getName()) {
            case "Ahlers Ján, MUDr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938808.jpg"); cm.setEmail("jan.ahlers@centrum.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SIEŤ"), cm, season)); break;
            case "Andraščíková Štefánia, doc. PhDr.": cm.setPicture("http://www.presov.sk//portals_pictures/i_003938/i_3938813.jpg"); cm.setEmail("stefania.andrascikova@unipo.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Cvengroš Peter, MUDr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938822.jpg"); cm.setEmail("peter.cvengros@post.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SIEŤ"), cm, season)); break;
            case "Dupkala Rudolf, PhDr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938837.jpg"); cm.setEmail("rudolf.dupkala@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Komanický Mikuláš, PhDr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939026.jpg"); cm.setEmail("mikulas.komanicky@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season)); break;
            case "Antolová Marcela, PhDr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938985.jpg"); cm.setEmail("marcela.holingova@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Drobňáková Valéria, ": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938825.jpg"); cm.setEmail("valeriadrobnakova@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Drutarovský Richard, Ing.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938834.jpg"); cm.setEmail("richard.drutarovsky@presov.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NOVA"), cm, season)); break;
            case "Benko Miroslav, PaedDr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938819.jpg"); cm.setEmail("miroslavbenko@post.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season)); break;
            case "Bednárová Zuzana, RNDr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938814.jpg"); cm.setEmail("zuzana.bednarova@presov.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Ferenc Stanislav, Mgr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938969.jpg"); cm.setEmail("ferenc.st@centrum.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Langová Janette, Mgr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939040.jpg"); cm.setEmail("jlangova63@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Malaga Ľudovít, Ing.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939044.jpg"); cm.setEmail("ludo.malaga@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season)); break;
            case "Mrouahová Daniela, MUDr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939053.jpg"); cm.setEmail("mrouahova@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Pucher René, JUDr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939056.jpg"); cm.setEmail("pucher.rene@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Ďurišin Martin, PhDr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938961.jpg"); cm.setEmail("durisin.martin@gmail.com, martin.durisin@presov.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SIEŤ"), cm, season)); break;
            case "Fedorčíková Renáta, Ing.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938962.jpg"); cm.setEmail("renafedorcikova@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Hermanovský Štefan, ": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938976.jpg"); cm.setEmail("stefan.hermanovsky@presov.sk, hermanovsky.stefan@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Janko Vasiľ, MUDr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938991.jpg"); cm.setEmail("vasil.janko@presov.sk, dr.janko.vasil@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Kollárová Marta, Ing.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939019.jpg"); cm.setEmail("m.koll@pobox.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Mochnacký Rastislav, Ing.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939048.jpg"); cm.setEmail("rastislav.mochnacky@presov.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Szidor Štefan, Ing.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939059.jpg"); cm.setEmail("stefan.szidor@presov.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Kutajová Jaroslava, Mgr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939032.jpg"); cm.setEmail("jarka.kutajova@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Hudáč Juraj, Ing.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938986.jpg"); cm.setEmail("juraj.hudac@presov.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season)); break;
            case "Lipka Martin, PhDr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939043.jpg"); cm.setEmail("lipkaglobal@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season)); break;
            case "Matejka Martin, Mgr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939047.jpg"); cm.setEmail("mato.matejka@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Tkáčová Zuzana, ": cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939064.jpg"); cm.setEmail("tkacovaz20@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Ďurčanská Katarína, JUDr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938868.jpg"); cm.setEmail("katarinadurcanska@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Kahanec Stanislav, Ing.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003938/i_3938994.jpg"); cm.setEmail("stanislav.kahanec@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Kužma Štefan, Ing.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939037.jpg"); cm.setEmail("stefan.kuzma@presov.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Krajňák Peter, Mgr.": cm.setPicture("http://www.presov.sk/portals_pictures/i_003939/i_3939027.jpg"); cm.setEmail("peter.krajnak@presov.sk, peterkraj@centrum.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
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

    public static void parseTown(Town town, Institution institution, DMMeetingsResponse meetingsResponse) {
        parseSeasons(town, institution, meetingsResponse.getSeasonDTOs());
    }

    public static void parseSeasons(Town town, Institution institution, List<SeasonDTO> seasonDTOs) {
        List<Season> seasons = new ArrayList<>();
        for (SeasonDTO seasonDTO : seasonDTOs) {
            seasons.add(parseSeason(institution, town, seasonDTO));
        }
        town.addSeasons(seasons);
    }

    private static Season parseSeason(Institution institution, Town town, SeasonDTO seasonDTO) {
        Season season = new Season();
        season.setName(seasonDTO.getName());
        season.setRef(seasonDTO.getName());
        season.setInstitution(institution);
        season.setTown(town);
        parseMeetings(seasonDTO, season);
        return season;
    }

    private static void parseMeetings(SeasonDTO seasonDTO, Season season) {
        List<Meeting> meetings = new ArrayList<>();
        for (MeetingDTO meetingDTO : seasonDTO.getMeetingDTOs()) {
            try {
                Meeting meeting = new Meeting();
                meeting.setName(meetingDTO.getName());
                meeting.setDate(PollDateUtils.parseSimpleDate(meetingDTO.getDate()));
                meeting.setExtId(meetingDTO.getId());
                meetings.add(meeting);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("MeetingComponent for meeting id: " + meetingDTO.getId() + " and name: " + meetingDTO.getName() + " was not loaded. Details: " + e.getMessage());
            }
        }
        season.setMeetings(meetings);
    }

    public static Meeting parseMeetingResponse(Meeting meeting, DMMeetingResponse meetingResponse) throws Exception {
        // Agenda
        AgendaDTO agendaDTO =
                meetingResponse.getChildren().get(0).getClass().equals(AgendaDTO.class)
                        ? (AgendaDTO) meetingResponse.getChildren().get(0)
                        : (AgendaDTO) meetingResponse.getChildren().get(1);
        DMParser.parseAgenda(meeting, agendaDTO); // TODO:

        // Attachments
        AttachmentsDTO attachmentsDTO =
                meetingResponse.getChildren().get(1).getClass().equals(AttachmentsDTO.class)
                        ? (AttachmentsDTO) meetingResponse.getChildren().get(1)
                        : (AttachmentsDTO) meetingResponse.getChildren().get(0);
        DMParser.parseMeetingAttachmens(meeting, attachmentsDTO); // TODO:
        return meeting;
    }

    public static Poll parsePollDetail(Season season, Poll poll, Map<String, CouncilMember> membersMap, DMPollDetailResponse pollDetailResponse) {
        if (pollDetailResponse.getChildren() != null) {
            for (VoterDTO voterDTO : pollDetailResponse.getChildren()) {
                CouncilMember member = findOrIntroduceCouncilMember(season, voterDTO.getName(), membersMap);
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
}
