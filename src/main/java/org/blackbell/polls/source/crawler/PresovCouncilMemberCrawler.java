package org.blackbell.polls.source.crawler;

import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.ClubFunction;
import org.blackbell.polls.domain.model.relate.ClubMember;
import org.blackbell.polls.domain.model.relate.ClubParty;
import org.blackbell.polls.domain.model.relate.PartyNominee;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kurtcha on 10.3.2018.
 */
public class PresovCouncilMemberCrawler {
    private static final Logger log = LoggerFactory.getLogger(PresovCouncilMemberCrawler.class);

    private static final String PRESOV_MSZ_MEMBERS_ROOT = "http://www.presov.sk/poslanci-msz.html";

    private static final String MEMBER_LINK_RE = "<a href=\"javascript:osoba_podrobnosti\\((?<id>\\d+),1\\)\" title=\"[^\"]+\"> <img src=\"(?<image>\\/portals_pictures\\/i_\\d+\\/i_\\d+.jpg)\" width=\"\\d+\" height=\"\\d+\" title=\"(?<name>[^\"]+)\" alt=\"[^\"]+\"><\\/a>";

    private static final String MEMBER_DETAIL_URL = "http://www.presov.sk/admin_new/modules/osoba_v_novom_okne.php?id_osoba=%s&all_data=1";

    private static final String MEMBER_DETAIL_RE = "<div class=\"float_left\\\">\\s*<p>(?<phone>[^:]+:\\s*(?<phonenumber>[^<]+))<\\/p>\\s*<p>E-mail: <a href=\"mailto:(?<email>[^\\\"]+)\" title=\"Odoslať mail\">[^<]+<\\/a>\\s*(<a href=\"mailto:(?<email2>[^\\\"]+)\" title=\"Odoslať mail\">[^<]+<\\/a>\\s*)?(<\\/p>)?\\s*<p>(?<address>[^<]+)(\\s*<br(\\s*\\/)?>\\s*)*(?<partycandidate>Kandidát(ka)? politickej strany (?<candidateparty>[^<]+))?\\s*(?<partiescandidate>((Kandidát(ka)? koalície politických strán|Kandidát(ka)? politickej koalície)\\s*(?<candidateparties>[^<]+))|Nezávislá kandidátka|Nezávislý kandidát)?(\\s*<br(\\s*\\/)?>\\s*)?<\\/p>\\s*<p>((?<clubmember>((Člen(ka)?|P(odp)?redseda|P(odp)?redsedníčka) Poslaneckého klubu (?<clubparties>.*?))|Nezaradená poslankyňa|Nezaradený poslanec)\\s*<br(\\s*\\/)?>)?\\s*((?<functions>.*(<br(\\s*\\/)?>|<\\/p>)\\s*)+)\\s*<\\/div>";
    private static final Pattern MEMBER_DETAIL_PATTERN = Pattern.compile(MEMBER_DETAIL_RE);

    private static final Pattern CHAIRMAN_PATTERN = Pattern.compile("Predseda|Predsedkyňa");
    private static final Pattern VICECHAIRMAN_PATTERN = Pattern.compile("Podpredseda|Podpredsedkyňa");
    private static final Pattern MEMBER_PATTERN = Pattern.compile("Člen|Členka");

    private static final String PORTAL_BASE_URL = "http://www.presov.sk";

    private Map<String, Club> clubsMap = new HashMap<>();

    public Set<CouncilMember> getCouncilMembers(Town town, Institution institution, Season season, Map<String, Party> partiesMap, Map<String, CouncilMember> councilMembersMap) {
        if (partiesMap == null) {
            partiesMap = new HashMap<>();
        }
        Set<CouncilMember> members = new HashSet<>();
        try {
            Document document = Jsoup.connect(PRESOV_MSZ_MEMBERS_ROOT).get();
            Elements linksOnPage = document.select("a[href^=javascript:osoba_podrobnosti]");

            Pattern memberPattern = Pattern.compile(MEMBER_LINK_RE);
            for (Element page : linksOnPage) {
                log.info(page.toString());
                Matcher matcher = memberPattern.matcher(page.toString());
                if (matcher.find()) {
                    String id = matcher.group("id");
                    String image = matcher.group("image");
                    String name = matcher.group("name");

                    // Check if not exists already
                    String simpleName = PollsUtils.getSimpleName(name);
                    if (councilMembersMap.containsKey(simpleName)) {
                        log.info("Council member '" + simpleName + "' already known.");
                        continue;
                    }

                    CouncilMember member = new CouncilMember();
                    //TODO: Politician<->CouncilMember
                    Politician politician = new Politician();
                    politician.setRef(PollsUtils.generateUniqueKeyReference());
                    politician.setName(simpleName);
                    politician.setTitles(PollsUtils.getTitles(name));
                    politician.setPicture(PORTAL_BASE_URL + image);
                    politician.setExtId(id);
                    member.setRef(PollsUtils.generateUniqueKeyReference());
                    member.setPolitician(politician);
                    member.setSeason(season);
                    member.setExtId(id);
                    member.setTown(town);
                    member.setInstitution(institution);

                    loadCouncilMemberDetails(member, member.getExtId(), partiesMap);

                    members.add(member);
                    councilMembersMap.put(PollsUtils.getSimpleName(member.getPolitician().getName()), member);
                } else {
                    log.info("No match");
                }
//                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return members;
    }

    //TODO: update from CouncilMember to Politician
    private void loadCouncilMemberDetails(CouncilMember m, String id, Map<String, Party> partiesMap) {
        try {
            log.info(m.getPolitician().getName() + " :: DETAILS: ");
            Document document = Jsoup.connect(String.format(MEMBER_DETAIL_URL, id)).get();
            Element detail = document.select("div[class=\"float_left\"]").first();
            log.info(detail.toString());
            Matcher matcher = MEMBER_DETAIL_PATTERN.matcher(detail.toString());
//            log.info("Groups found: " + matcher.groupCount());
            if (matcher.find()) {
                //log.info("Phone: " + matcher.group("phone"));
                //log.info("Phone number: " + matcher.group("phonenumber"));
                String emails = "";
                if (matcher.group("email") != null && !matcher.group("email").isEmpty()) {
                    emails = matcher.group("email");
                }
                if (matcher.group("email2") != null && !matcher.group("email2").isEmpty()) {
                    emails += ", " + matcher.group("email2");
                }
                //TODO: commented out before update
                m.getPolitician().setEmail(emails);
                m.getPolitician().setPhone(matcher.group("phonenumber"));
                String partyCandidate = matcher.group("candidateparty");
                if (partyCandidate != null && !partyCandidate.isEmpty()) {
                    Set<PartyNominee> partyNominees = new HashSet<>();
                    PartyNominee nominee = new PartyNominee();
                    String partyName = PollsUtils.cleanAndTrim(partyCandidate);
                    if (!partiesMap.containsKey(partyName)) {
                        partiesMap.put(partyName, introduceParty(partyName));
                    }
                    //TODO: comented out before update
                    nominee.setPolitician(m.getPolitician());
                    nominee.setParty(partiesMap.get(partyName));
                    nominee.setSeason(m.getSeason());
                    partyNominees.add(nominee);
                    //TODO: commented out before update
                    m.getPolitician().setPartyNominees(partyNominees);
                }
                String partiesCandidate = matcher.group("candidateparties");
                if (partiesCandidate != null && !partiesCandidate.isEmpty()) {
                    Set<PartyNominee> partyNominees = new HashSet<>();
                    List<String> partyList = PollsUtils.splitCleanAndTrim(partiesCandidate);
                    for (String partyName : partyList) {
                        if (!partiesMap.containsKey(partyName)) {
                            partiesMap.put(partyName, introduceParty(partyName));
                        }
                        PartyNominee nominee = new PartyNominee();
                        //TODO: commented out before update
                        nominee.setPolitician(m.getPolitician());
                        nominee.setParty(partiesMap.get(partyName));
                        nominee.setSeason(m.getSeason());
                        partyNominees.add(nominee);
                    }
                    //TODO: commented out before update
                    m.getPolitician().setPartyNominees(partyNominees);
                }

                String clubMemberString = matcher.group("clubmember");
                if (clubMemberString != null && !clubMemberString.isEmpty()) {
                    ClubFunction clubFunction;
                    if (CHAIRMAN_PATTERN.matcher(clubMemberString).find()) {
                        clubFunction = ClubFunction.CHAIRMAN;
                    } else if (VICECHAIRMAN_PATTERN.matcher(clubMemberString).find()) {
                        clubFunction = ClubFunction.VICECHAIRMAN;
                    } else {
                        clubFunction = ClubFunction.MEMBER;
                    }

                    String clubPartiesString = matcher.group("clubparties");
                    if (clubPartiesString != null && !clubPartiesString.isEmpty()) {
                        List<String> partyList = PollsUtils.splitCleanAndTrim(clubPartiesString);
                        String clubName = PollsUtils.generateClubName(partyList);
                        if (!clubsMap.containsKey(clubName)) {
                            log.info(":NEW CLUB: " + clubName);
                            Club club = new Club();
                            //TODO: commented out before update
                            club.setTown(m.getTown()); //TODO: proxy
                            club.setSeason(m.getSeason());
                            club.setRef(PollsUtils.generateUniqueKeyReference());
                            club.setName(clubName);
                            Set<ClubParty> clubParties1 = new HashSet<>();
                            for (String partyName : partyList) {
                                log.info(":PARTY NAME: |" + partyName + "| => partiesMap.containsKey(partyName): " + partiesMap.containsKey(partyName));
                                if (!partiesMap.containsKey(partyName)) {
                                    partiesMap.put(partyName, introduceParty(partyName));
                                }
                                clubParties1.add(introduceClubParty(m.getSeason(), club, partiesMap.get(partyName)));
                            }
                            //TODO: commented out before update
                            club.setClubParties(clubParties1);
                            clubsMap.put(club.getName(), club);
                        }
                        Club club = clubsMap.get(clubName);
                        log.info("CLUB["+clubName+"]: NEW CLUB MEMBER: " + clubName);
                        ClubMember clubMember = new ClubMember();
                        clubMember.setCouncilMember(m);
                        clubMember.setClubFunction(clubFunction);
                        m.addClubMember(clubMember);
                        club.addClubMember(clubMember);
                    }
                }

                String functionsString = matcher.group("functions");
                if (functionsString != null && !functionsString.isEmpty()) {
                    String[] functions = functionsString.split("\\s*(<br(\\s*\\/)?>|<\\/p>)\\s*");
                    functionsString = String.join(", ", functions);
                    m.setOtherFunctions(functionsString);
                }
            } else {
                log.info("No match");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Party introduceParty(String partyName) {
        log.info(":NEW PARTY: " + partyName);
        Party party = new Party();
        party.setRef(partyName);
        party.setName(partyName);
        return party;
    }

    private static ClubParty introduceClubParty(Season season, Club club, Party party) {
        ClubParty clubParty = new ClubParty();
        log.info("CLUB["+club.getRef()+"]: NEW CLUB PARTY: " + party.getName());
        clubParty.setParty(party);
        clubParty.setSeason(season);
        clubParty.setClub(club);
        return clubParty;
    }

//    public static void main(String[] args) {
//    	new PresovCouncilMemberCrawler().getCouncilMembers();
//    }
}
