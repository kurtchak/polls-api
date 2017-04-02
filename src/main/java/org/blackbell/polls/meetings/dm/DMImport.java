package org.blackbell.polls.meetings.dm;

import org.blackbell.polls.common.PollDateUtils;
import org.blackbell.polls.meetings.dm.dto.*;
import org.blackbell.polls.meetings.model.*;
import org.blackbell.polls.meetings.model.vote.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
public class DMImport {

    public static final String DM_VOTE_FOR = "Za";
    public static final String DM_VOTE_AGAINST = "Proti";
    public static final String DM_NO_VOTE = "Nehlasoval";
    public static final String DM_ABSTAIN = "Zdržal sa";
    public static final String DM_ABSENT = "Chýbal na hlasovaní";
    private static Map<String, CouncilMember> members = new HashMap<>();
    private static Map<String, Party> parties = new HashMap<>();

    private static final Logger log = LoggerFactory.getLogger(DMImport.class);

    public static Meeting parseMeeting(Season season, MeetingDTO meetingDTO) {
        try {
            Meeting meeting = new Meeting();
            meeting.setRef(generateUniqueKeyReference());
            meeting.setName(meetingDTO.getName());
            meeting.setDate(PollDateUtils.parseMeetingDate(meetingDTO));
            meeting.setSeason(season);
            if (meetingDTO.getChildren() != null) {
                for (MeetingComponentDTO dto : meetingDTO.getChildren()) {
                    if (AgendaDTO.class.equals(dto.getClass())) {
                        meeting.setAgendaItems(parseAgenda(season, meeting, (AgendaDTO) dto));
                    } else if (AttachmentsDTO.class.equals(dto.getClass())) {
                        meeting.setAttachments(parseMeetingAttachmens(meeting, (AttachmentsDTO) dto));
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

    private static List<MeetingAttachment> parseMeetingAttachmens(Meeting meeting, AttachmentsDTO attachmentsDTO) {
        List<MeetingAttachment> attachments = new ArrayList<>();
        for (AttachmentDTO attDTO : attachmentsDTO.getAttachmentDTOs()) {
            attachments.add(new MeetingAttachment(attDTO.getName(), meeting, generateUniqueKeyReference(), attDTO.getSource()));
        }
        return attachments;
    }

    private static List<AgendaItem> parseAgenda(Season season, Meeting meeting, AgendaDTO agendaDTO) {
        List<AgendaItem> items = new ArrayList<>();
        for (AgendaItemDTO itemDTO : agendaDTO.getAgendaItemDTOs()) {
            items.add(parseAgendaItem(season, meeting, itemDTO));
        }
        return items;
    }

    private static AgendaItem parseAgendaItem(Season season, Meeting meeting, AgendaItemDTO itemDTO) {
        AgendaItem item = new AgendaItem();
        item.setName(itemDTO.getName());
        item.setRef(generateUniqueKeyReference());
        item.setMeeting(meeting);
        for (AgendaItemComponentDTO componentDTO : itemDTO.getChildren()) {
            if (PollsDTO.class.equals(componentDTO.getClass())) {
                item.setPolls(parsePolls(season, item, (PollsDTO) componentDTO));
            } else if (ProspectsDTO.class.equals(componentDTO.getClass())) {
                item.setAttachments(parseAgendaItemAttachments(item, (ProspectsDTO) componentDTO));
            } else {
                log.warn("parseAgendaItem: Unknown agendaItems item component class: class[" + (itemDTO != null ? itemDTO.getClass() : "unknown") + "]");
            }
        }
        return item;
    }

    private static List<AgendaItemAttachment> parseAgendaItemAttachments(AgendaItem item, ProspectsDTO prospectsDTO) {
        List<AgendaItemAttachment> agendaItemAttachments = new ArrayList<>();
        if (prospectsDTO.getProspectDTOs() != null) {
            for (ProspectDTO prospectDTO : prospectsDTO.getProspectDTOs()) {
                agendaItemAttachments.add(new AgendaItemAttachment(prospectDTO.getName(), item, generateUniqueKeyReference(), prospectDTO.getSource()));
            }
        }
        return agendaItemAttachments;
    }

    private static List<Poll> parsePolls(Season season, AgendaItem item, PollsDTO pollsDTO) {
        List<Poll> polls = new ArrayList<>();
        if (pollsDTO.getPollDTOs() != null) {
            for (PollDTO pollDTO : pollsDTO.getPollDTOs()) {
                polls.add(parsePoll(season, item, pollDTO));
            }
        }
        return polls;
    }

    private static Poll parsePoll(Season season, AgendaItem item, PollDTO pollDTO) {
        Poll poll = new Poll();
        poll.setName(pollDTO.getName());
        poll.setRef(generateUniqueKeyReference());
        poll.setAgendaItem(item);
        if (pollDTO.getPollChoiceDTOs() != null) {
            for (PollChoiceDTO choice : pollDTO.getPollChoiceDTOs()) {
                switch (choice.getName()) {
                    case DM_VOTE_FOR: poll.setVotesFor(parseVotesFor(season, poll, choice.getMembers())); break;
                    case DM_VOTE_AGAINST: poll.setVotesAgainst(parseVotesAgainst(season, poll, choice.getMembers())); break;
                    case DM_NO_VOTE: poll.setNoVotes(parseNoVotes(season, poll, choice.getMembers())); break;
                    case DM_ABSTAIN: poll.setAbstains(parseAbstains(season, poll, choice.getMembers())); break;
                    case DM_ABSENT: poll.setAbsents(parseAbsents(season, poll, choice.getMembers())); break;
                }
            }
        }
        return poll;
    }

    public static String generateUniqueKeyReference() {
        return "" + System.nanoTime();
    }

    private static List<VoteFor> parseVotesFor(Season season, Poll poll, List<CouncilMemberDTO> membersDTO) {
        List<VoteFor> votes = new ArrayList<>();
        for (CouncilMemberDTO memberDTO : membersDTO) {
            votes.add(new VoteFor(poll, findOrIntroduceCouncilMember(season, memberDTO)));
        }
        return votes;
    }

    private static List<VoteAgainst> parseVotesAgainst(Season season, Poll poll, List<CouncilMemberDTO> membersDTO) {
        List<VoteAgainst> votes = new ArrayList<>();
        for (CouncilMemberDTO memberDTO : membersDTO) {
            votes.add(new VoteAgainst(poll, findOrIntroduceCouncilMember(season, memberDTO)));
        }
        return votes;
    }

    private static List<NoVote> parseNoVotes(Season season, Poll poll, List<CouncilMemberDTO> membersDTO) {
        List<NoVote> votes = new ArrayList<>();
        for (CouncilMemberDTO memberDTO : membersDTO) {
            votes.add(new NoVote(poll, findOrIntroduceCouncilMember(season, memberDTO)));
        }
        return votes;
    }

    private static List<Abstain> parseAbstains(Season season, Poll poll, List<CouncilMemberDTO> membersDTO) {
        List<Abstain> votes = new ArrayList<>();
        for (CouncilMemberDTO memberDTO : membersDTO) {
            votes.add(new Abstain(poll, findOrIntroduceCouncilMember(season, memberDTO)));
        }
        return votes;
    }

    private static List<Absent> parseAbsents(Season season, Poll poll, List<CouncilMemberDTO> membersDTO) {
        List<Absent> votes = new ArrayList<>();
        for (CouncilMemberDTO memberDTO : membersDTO) {
            votes.add(new Absent(poll, findOrIntroduceCouncilMember(season, memberDTO)));
        }
        return votes;
    }

    private static CouncilMember findOrIntroduceCouncilMember(Season season, CouncilMemberDTO memberDTO) {
        CouncilMember cm = null;
        if (members.containsKey(memberDTO.getName())) {
            cm = members.get(memberDTO.getName());
        } else {
            cm = introduceCouncilMember(season, memberDTO);
            members.put(cm.getName(), cm);
        }
        return cm;
    }

    private static CouncilMember introduceCouncilMember(Season season, CouncilMemberDTO memberDTO) {
        CouncilMember cm;
        cm = new CouncilMember();
        cm.setName(memberDTO.getName());
        cm.setRef(generateUniqueKeyReference());
        switch (cm.getName()) {
            case "Ahlers Ján, MUDr.": cm.setPicture("/portals_pictures/i_003938/i_3938808.jpg"); cm.setEmail("jan.ahlers@centrum.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SIEŤ"), cm, season)); break;
            case "Andraščíková Štefánia, doc. PhDr.": cm.setPicture("/portals_pictures/i_003938/i_3938813.jpg"); cm.setEmail("stefania.andrascikova@unipo.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Cvengroš Peter, MUDr.": cm.setPicture("/portals_pictures/i_003938/i_3938822.jpg"); cm.setEmail("peter.cvengros@post.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SIEŤ"), cm, season)); break;
            case "Dupkala Rudolf, PhDr.": cm.setPicture("/portals_pictures/i_003938/i_3938837.jpg"); cm.setEmail("rudolf.dupkala@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Komanický Mikuláš, PhDr.": cm.setPicture("/portals_pictures/i_003939/i_3939026.jpg"); cm.setEmail("mikulas.komanicky@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season)); break;
            case "Antolová Marcela, PhDr.": cm.setPicture("/portals_pictures/i_003938/i_3938985.jpg"); cm.setEmail("marcela.holingova@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Drobňáková Valéria, ": cm.setPicture("/portals_pictures/i_003938/i_3938825.jpg"); cm.setEmail("valeriadrobnakova@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Drutarovský Richard, Ing.": cm.setPicture("/portals_pictures/i_003938/i_3938834.jpg"); cm.setEmail("richard.drutarovsky@presov.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NOVA"), cm, season)); break;
            case "Benko Miroslav, PaedDr.": cm.setPicture("/portals_pictures/i_003938/i_3938819.jpg"); cm.setEmail("miroslavbenko@post.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season)); break;
            case "Bednárová Zuzana, RNDr.": cm.setPicture("/portals_pictures/i_003938/i_3938814.jpg"); cm.setEmail("zuzana.bednarova@presov.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Ferenc Stanislav, Mgr.": cm.setPicture("/portals_pictures/i_003938/i_3938969.jpg"); cm.setEmail("ferenc.st@centrum.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Langová Janette, Mgr.": cm.setPicture("/portals_pictures/i_003939/i_3939040.jpg"); cm.setEmail("jlangova63@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Malaga Ľudovít, Ing.": cm.setPicture("/portals_pictures/i_003939/i_3939044.jpg"); cm.setEmail("ludo.malaga@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season)); break;
            case "Mrouahová Daniela, MUDr.": cm.setPicture("/portals_pictures/i_003939/i_3939053.jpg"); cm.setEmail("mrouahova@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Pucher René, JUDr.": cm.setPicture("/portals_pictures/i_003939/i_3939056.jpg"); cm.setEmail("pucher.rene@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Ďurišin Martin, PhDr.": cm.setPicture("/portals_pictures/i_003938/i_3938961.jpg"); cm.setEmail("durisin.martin@gmail.com, martin.durisin@presov.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SIEŤ"), cm, season)); break;
            case "Fedorčíková Renáta, Ing.": cm.setPicture("/portals_pictures/i_003938/i_3938962.jpg"); cm.setEmail("renafedorcikova@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Hermanovský Štefan, ": cm.setPicture("/portals_pictures/i_003938/i_3938976.jpg"); cm.setEmail("stefan.hermanovsky@presov.sk, hermanovsky.stefan@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Janko Vasiľ, MUDr.": cm.setPicture("/portals_pictures/i_003938/i_3938991.jpg"); cm.setEmail("vasil.janko@presov.sk, dr.janko.vasil@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Kollárová Marta, Ing.": cm.setPicture("/portals_pictures/i_003939/i_3939019.jpg"); cm.setEmail("m.koll@pobox.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Mochnacký Rastislav, Ing.": cm.setPicture("/portals_pictures/i_003939/i_3939048.jpg"); cm.setEmail("rastislav.mochnacky@presov.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Szidor Štefan, Ing.": cm.setPicture("/portals_pictures/i_003939/i_3939059.jpg"); cm.setEmail("stefan.szidor@presov.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Kutajová Jaroslava, Mgr.": cm.setPicture("/portals_pictures/i_003939/i_3939032.jpg"); cm.setEmail("jarka.kutajova@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Hudáč Juraj, Ing.": cm.setPicture("/portals_pictures/i_003938/i_3938986.jpg"); cm.setEmail("juraj.hudac@presov.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season)); break;
            case "Lipka Martin, PhDr.": cm.setPicture("/portals_pictures/i_003939/i_3939043.jpg"); cm.setEmail("lipkaglobal@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("SMER-SD"), cm, season)); break;
            case "Matejka Martin, Mgr.": cm.setPicture("/portals_pictures/i_003939/i_3939047.jpg"); cm.setEmail("mato.matejka@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Tkáčová Zuzana, ": cm.setPicture("/portals_pictures/i_003939/i_3939064.jpg"); cm.setEmail("tkacovaz20@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Ďurčanská Katarína, JUDr.": cm.setPicture("/portals_pictures/i_003938/i_3938868.jpg"); cm.setEmail("katarinadurcanska@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("NEZ"), cm, season)); break;
            case "Kahanec Stanislav, Ing.": cm.setPicture("/portals_pictures/i_003938/i_3938994.jpg"); cm.setEmail("stanislav.kahanec@gmail.com"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Kužma Štefan, Ing.": cm.setPicture("/portals_pictures/i_003939/i_3939037.jpg"); cm.setEmail("stefan.kuzma@presov.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
            case "Krajňák Peter, Mgr.": cm.setPicture("/portals_pictures/i_003939/i_3939027.jpg"); cm.setEmail("peter.krajnak@presov.sk, peterkraj@centrum.sk"); cm.setPartyNominees(getPartyNominees(findOrIntroduceParties("KDH", "SDKÚ-DS", "MOST-HÍD"), cm, season)); break;
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
        List<Meeting> meetingsMap = new ArrayList<>();
        for (MeetingDTO meetingDTO : seasonDTO.getMeetingDTOs()) {
            meetingsMap.add(parseMeeting(season, meetingDTO));
        }
        season.setMeetings(meetingsMap);
        return season;
    }
}
