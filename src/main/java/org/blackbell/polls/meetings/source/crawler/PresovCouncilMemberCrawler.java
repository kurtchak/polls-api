package org.blackbell.polls.meetings.source.crawler;

import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.meetings.model.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kurtcha on 10.3.2018.
 */
public class PresovCouncilMemberCrawler {

    private static final String PRESOV_MSZ_MEMBERS_ROOT = "http://www.presov.sk/poslanci-msz.html";
    private static final String MEMBER_LINK_RE = "<a href=\"javascript:osoba_podrobnosti\\((?<id>\\d+),1\\)\" title=\"[^\"]+\"> <img src=\"(?<image>\\/portals_pictures\\/i_\\d+\\/i_\\d+.jpg)\" width=\"\\d+\" height=\"\\d+\" title=\"(?<name>[^\"]+)\" alt=\"[^\"]+\"><\\/a>";
    private static final String MEMBER_DETAIL_URL = "http://www.presov.sk/admin_new/modules/osoba_v_novom_okne.php?id_osoba=%s&all_data=1";
    //private static final String MEMBER_DETAIL_RE = "<p>[^:]+:\\s*(?<phonenumber>[^<]+)<\\/p>\\s*<p>E-mail: <a href=\"mailto:(?<email>[^\"]+)\" title=\"Odoslať mail\">[^<]+<\\/a>\\s+<\\/p>(?<functions>.*?<br>\\s*)?";
    private static final String MEMBER_DETAIL_RE = "<div class=\"float_left\">\\s*<p>(?<phone>[^:]+:\\s*(?<phonenumber>[^<]+))<\\/p>\\s*<p>E-mail: <a href=\"mailto:(?<email>[^\"]+)\" title=\"Odoslať mail\">[^<]+<\\/a>\\s*<\\/p>\\s*<p>(?<address>[^<]+)(<br>\\s*)*(?<partycandidate>Kandidát(ka)? politickej strany (?<candidateparty>[^<]+))?\\s*(?<partiescandidate>(Kandidát(ka)? koalície politických strán (?<candidateparties>[^<]+))|Nezávislá kandidátka|Nezávislý kandidát)?<\\/p>\\s*<p>((?<clubmember>((Člen(ka)?|P(odp)?redseda|P(odp)?redsedníčka) Poslaneckého klubu (?<clubparties>.*?))|Nezaradená poslankyňa|Nezaradený poslanec)\\s*<br>)?\\s*((?<functions>.*(<br>|<\\/p>)\\s*)+)\\s*<\\/div>";
    private static final Pattern MEMBER_DETAIL_PATTERN = Pattern.compile(MEMBER_DETAIL_RE);
    private static final Pattern CHAIRMAN_PATTERN = Pattern.compile("Predseda|Predsedkyňa");
    private static final Pattern VICECHAIRMAN_PATTERN = Pattern.compile("Podpredseda|Podpredsedkyňa");
    private static final Pattern MEMBER_PATTERN = Pattern.compile("Člen|Členka");
    private static final String PORTAL_BASE_URL = "http://www.presov.sk";

    private Map<String, Club> clubsMap = new HashMap<>();

    public List<CouncilMember> getCouncilMembers(Season season, Map<String, Party> partiesMap) {
        List<CouncilMember> members = new ArrayList<>();
        try {
            Document document = Jsoup.connect(PRESOV_MSZ_MEMBERS_ROOT).get();
            Elements linksOnPage = document.select("a[href^=javascript:osoba_podrobnosti]");

            Pattern memberPattern = Pattern.compile(MEMBER_LINK_RE);
            for (Element page : linksOnPage) {
                System.out.println(page.toString());
                Matcher matcher = memberPattern.matcher(page.toString());
                if (matcher.find()) {
                    String id = matcher.group("id");
                    String image = matcher.group("image");
                    String name = matcher.group("name");

                    CouncilMember member = new CouncilMember();
                    member.setName(PollsUtils.getSimpleName(name));
                    member.setTitles(PollsUtils.getTitles(name));
                    member.setPicture(PORTAL_BASE_URL + image);
                    member.setExtId(id);
                    member.setRef(PollsUtils.generateUniqueKeyReference());
                    member.setSeason(season);

                    loadCouncilMemberDetails(member, member.getExtId(), partiesMap);

                    members.add(member);
                } else {
                    System.out.println("No match");
                }
//                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return members;
    }

    private void loadCouncilMemberDetails(CouncilMember m, String id, Map<String, Party> partiesMap) {
        try {
            System.out.println(m.getName() + " :: DETAILS: ");
            Document document = Jsoup.connect(String.format(MEMBER_DETAIL_URL, id)).get();
            Element detail = document.select("div[class=\"float_left\"]").first();
            System.out.println(detail.toString());
            Matcher matcher = MEMBER_DETAIL_PATTERN.matcher(detail.toString());
            System.out.println("Groups found: " + matcher.groupCount());
            if (matcher.find()) {
                //System.out.println("Phone: " + matcher.group("phone"));
                //System.out.println("Phone number: " + matcher.group("phonenumber"));
                m.setEmail(matcher.group("email"));
                m.setPhone(matcher.group("phonenumber"));
                String partyCandidate = matcher.group("candidateparty");
                if (partyCandidate != null && !partyCandidate.isEmpty()) {
                    List<PartyNominee> partyNominees = new ArrayList<>();
                    PartyNominee nominee = new PartyNominee();
                    nominee.setCouncilMember(m);
                    nominee.setParty(partiesMap.get(partyCandidate));
                    nominee.setSeason(m.getSeason());
                    partyNominees.add(nominee);
                    m.setPartyNominees(partyNominees);
                }
                String partiesCandidate = matcher.group("candidateparties");
                if (partiesCandidate != null && !partiesCandidate.isEmpty()) {
                    List<PartyNominee> partyNominees = new ArrayList<>();
                    String[] parties = partiesCandidate.split("\\s*,\\s*");
                    for (String party : parties) {
                        PartyNominee nominee = new PartyNominee();
                        nominee.setCouncilMember(m);
                        nominee.setParty(partiesMap.get(party));
                        nominee.setSeason(m.getSeason());
                        partyNominees.add(nominee);
                    }
                    m.setPartyNominees(partyNominees);
                }

                String clubMemberString = matcher.group("clubmember");
                if (clubMemberString != null && !clubMemberString.isEmpty()) {
                    if (CHAIRMAN_PATTERN.matcher(clubMemberString).find()) {
                        m.setClubFunction(ClubFunction.CHAIRMAN);
                    } else if (VICECHAIRMAN_PATTERN.matcher(clubMemberString).find()) {
                        m.setClubFunction(ClubFunction.VICECHAIRMAN);
                    } else {
                        m.setClubFunction(ClubFunction.MEMBER);
                    }
                }

                String clubPartiesString = matcher.group("clubparties");
                if (clubPartiesString != null && !clubPartiesString.isEmpty()) {
                    String[] clubParties = clubPartiesString.split("\\s*,\\s*");
                    String clubName = String.join("-", clubParties);
                    if (!clubsMap.containsKey(clubName)) {
                        Club club = new Club();
                        List<ClubParty> clubParties1 = new ArrayList<>();
                        for (String party : clubParties) {
                            ClubParty clubParty = new ClubParty();
                            clubParty.setParty(partiesMap.get(party));
                            clubParty.setSeason(m.getSeason());
                            clubParty.setClub(club);
                            clubParties1.add(clubParty);
                        }
                        club.setParties(clubParties1);
                        List<ClubMember> clubMembers = new ArrayList<>();
                        ClubMember clubMember = new ClubMember();
                        clubMember.setCouncilMember(m);
                        clubMember.setClub(club);
                        clubMembers.add(clubMember);
                        m.setClubMembers(clubMembers);
                        club.addMember(clubMember);
                    }
                }

                String functionsString = matcher.group("functions");
                if (functionsString != null && !functionsString.isEmpty()) {
                    String[] functions = functionsString.split("\\s*(<br>|<\\/p>)\\s*");
                    functionsString = String.join(", ", functions);
                    m.setOtherFunctions(functionsString);
                }
            } else {
                System.out.println("No match");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public static void main(String[] args) {
//    	new PresovCouncilMemberCrawler().getCouncilMembers();
//    }
}
