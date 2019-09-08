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
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.blackbell.polls.common.PollsUtils.*;

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

    public Set<CouncilMember> getCouncilMembers(
            Town town,
            Institution institution,
            Season season,
            Map<String, Party> partiesMap,
            Map<String, CouncilMember> councilMembersMap) {

        Set<CouncilMember> members = new HashSet<>();
        try {
            Document document = Jsoup.connect(PRESOV_MSZ_MEMBERS_ROOT).get();
            PollsUtils.saveToFile("presov_msz_members.html", document.outerHtml());

            Elements linksOnPage = document.select("a[href^=javascript:osoba_podrobnosti]");

            Pattern memberPattern = Pattern.compile(MEMBER_LINK_RE);
            for (Element page : linksOnPage) {
                String pageContent = page.toString();
                log.info(pageContent);
                Matcher matcher = memberPattern.matcher(pageContent);
                if (matcher.find()) {
                    String id = matcher.group("id");
                    String image = matcher.group("image");
                    String name = matcher.group("name");

                    PollsUtils.saveToFile("presov_msz_member_" + PollsUtils.toFilenameForm(name) + "_brief.html", pageContent);

                    // Check if not exists already
                    String keyName = PollsUtils.toSimpleNameWithoutAccents(name);
                    if (councilMembersMap.containsKey(keyName)) {
                        log.info("Council member '{}' already known.", keyName);
                        continue;
                    } else {
                        log.info("Council member '{}' not known yet. ASCII form: {}", keyName);
                    }

                    //TODO: Politician<->CouncilMember
                    Politician politician = introducePolitician(id, image, name);
                    CouncilMember member = introduceCouncilMember(town, institution, season, id, politician);

                    loadCouncilMemberDetails(member, member.getExtId(), partiesMap);

                    log.info("POLITICIAN: {} -> NOMINEE OF: {}", deAccent(politician.getName()), politician.getPartyNominees());
                    if (member.getPolitician().getPartyNominees() != null) {
                        log.info("\t{} ->> {}",
                                member.getPolitician().getPartyNominees() != null ? member.getPolitician().getPartyNominees().size() : 0,
                                politician.getPartyNominees().stream()
                                        .map(partyNominee -> partyNominee.getParty().getName()).collect(Collectors.joining(",")));
                    }
                    log.info("COUNCIL MEMBER: {} -> CLUB MEMBER OF: {}",
                            deAccent(member.getPolitician().getName()),
                            member.getClubMember() != null ? member.getClubMember().getClub().getName() : null);
                    if (member.getClubMember() != null) {
                        log.info("\t{} ->> {}",
                                member.getClubMember().getClub().getClubParties() != null
                                        ? member.getClubMember().getClub().getClubParties().size() : 0,
                                member.getClubMember().getClub().getClubParties().stream()
                                        .map(clubParty -> clubParty.getParty().getName()).collect(Collectors.joining(",")));
                    }

                    members.add(member);
                    councilMembersMap.put(keyName, member);
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

    private void loadCouncilMemberDetails(CouncilMember councilMember, String id, Map<String, Party> partiesMap) {
        try {
            log.info("{} :: DETAILS", deAccent(councilMember.getPolitician().getName()));
            Document document = Jsoup.connect(String.format(MEMBER_DETAIL_URL, id)).get();
            Element detail = document.select("div[class=\"float_left\"]").first();
            String content = detail.toString();
            log.info(content);
            Matcher matcher = MEMBER_DETAIL_PATTERN.matcher(content);

            if (matcher.find()) {
                PollsUtils.saveToFile("presov_msz_member_" + PollsUtils.toFilenameForm(councilMember.getPolitician().getName()) + "_detail.html", content);

                councilMember.getPolitician().setEmail(PresovCouncilMemberRegexpMatcher.loadValue(matcher,"email"));
                councilMember.getPolitician().setPhone(PresovCouncilMemberRegexpMatcher.loadValue(matcher,"phonenumber"));

                // Party Nominees
                String partiesCandidates = PresovCouncilMemberRegexpMatcher.loadValue(matcher,"candidateparties");
                if (!isNullOrEmpty(partiesCandidates)) {

                    List<String> partyNames = splitCleanAndTrim(partiesCandidates);
                    Map<String, Party> newParties = partyNames.stream()
                            .filter(partyName -> !partiesMap.containsKey(partyName))
                            .collect(Collectors.toMap(
                                    partyName -> partyName,
                                    PresovCouncilMemberCrawler::introduceParty));
                    partiesMap.putAll(newParties);

                    partiesMap.keySet().forEach(partyName -> log.info("---> PARTY: {} -> {}", partyName, partiesMap.get(partyName)));

                    Set<PartyNominee> partyNominees =
                            partyNames.stream()
                                    .map(partyName -> introducePartyNominee(councilMember, partiesMap, partyName))
                                    .collect(Collectors.toSet());
                    partyNominees.forEach(partyNominee -> log.info("--> partyNominee: {}", partyNominee));
                    councilMember.getPolitician().setPartyNominees(partyNominees);
                }

                // Club Members
                String clubMemberString = PresovCouncilMemberRegexpMatcher.loadValue(matcher,"clubmember");
                if (clubMemberString != null && !clubMemberString.isEmpty()) {

                    ClubFunction clubFunction = recognizeClubFunction(clubMemberString);

                    String clubPartiesString = PresovCouncilMemberRegexpMatcher.loadValue(matcher,"clubparties");
                    if (!isNullOrEmpty(clubPartiesString)) {
                        List<String> partyList = splitCleanAndTrim(clubPartiesString);
                        String clubName = generateClubName(partyList);
                        if (!clubsMap.containsKey(clubName)) {
                            Club club = introduceClub(councilMember.getTown(), councilMember.getSeason(), clubName);

                            Map<String, Party> newParties =
                                    partyList.stream()
                                            .filter(partyName -> !partiesMap.containsKey(partyName))
                                            .collect(Collectors.toMap(
                                                    partyName -> partyName,
                                                    PresovCouncilMemberCrawler::introduceParty));
                            log.info("NEW PARTIES: {}", String.join(", ", newParties.keySet()));
                            partiesMap.putAll(newParties);
                            log.info("ALL PARTIES: {}", String.join(", ", partiesMap.keySet()));

                            club.setClubParties(
                                    partyList.stream()
                                            .map(partyName -> introduceClubParty(councilMember.getSeason(), club, partiesMap.get(partyName)))
                                            .collect(Collectors.toSet()));

                            log.info("CLUB PARTIES: {}", club.getClubParties() != null ? club.getClubParties().size() : 0);
                            clubsMap.put(club.getName(), club);
                        }
                        Club club = clubsMap.get(clubName);
                        ClubMember clubMember = introduceClubMember(club, councilMember, clubFunction);
                        club.addClubMember(clubMember);
                    }
                }

                String functionsString = PresovCouncilMemberRegexpMatcher.loadValue(matcher,"functions");
                if (!isNullOrEmpty(functionsString)) {
                    councilMember.setOtherFunctions(
                            String.join(", ", functionsString.split("\\s*(<br(\\s*\\/)?>|<\\/p>)\\s*")));
                }
            } else {
                log.info("No match");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ClubFunction recognizeClubFunction(String clubMemberString) {
        ClubFunction clubFunction;
        if (CHAIRMAN_PATTERN.matcher(clubMemberString).find()) {
            clubFunction = ClubFunction.CHAIRMAN;
        } else if (VICECHAIRMAN_PATTERN.matcher(clubMemberString).find()) {
            clubFunction = ClubFunction.VICECHAIRMAN;
        } else {
            clubFunction = ClubFunction.MEMBER;
        }
        return clubFunction;
    }

    private static CouncilMember introduceCouncilMember(Town town, Institution institution, Season season, String extId, Politician politician) {
        log.info(":NEW COUNCIL MEMBER: {}", deAccent(politician.getName()));
        CouncilMember member = new CouncilMember();
        member.setRef(generateUniqueKeyReference());
        member.setPolitician(politician);
        member.setSeason(season);
        member.setTown(town);
        member.setInstitution(institution);
        member.setExtId(extId);

        return member;
    }

    private static Politician introducePolitician(String extId, String image, String name) {
        log.info(":NEW POLITICIAN: {}", deAccent(name));
        Politician politician = new Politician();
        politician.setRef(generateUniqueKeyReference());
        politician.setName(toSimpleName(name));
        politician.setTitles(getTitles(name));
        politician.setPicture(PORTAL_BASE_URL + image);
        politician.setExtId(extId);

        return politician;
    }

    private static PartyNominee introducePartyNominee(CouncilMember councilMember, Map<String, Party> partiesMap, String partyName) {
        log.info(":NEW COUNCIL MEMBER: {} - PARTY NOMINEE: {}", deAccent(councilMember.getPolitician().getName()), partyName);
        PartyNominee nominee = new PartyNominee();
        //TODO: commented out before update
        nominee.setParty(partiesMap.get(partyName));
        nominee.setSeason(councilMember.getSeason());
        nominee.setTown(councilMember.getTown());
        councilMember.getPolitician().addPartyNominee(nominee);
        return nominee;
    }

    private static ClubMember introduceClubMember(Club club, CouncilMember councilMember, ClubFunction clubFunction) {
        log.info(":NEW CLUB MEMBER: [{}]: {}", clubFunction.name(), deAccent(councilMember.getPolitician().getName()));
        ClubMember clubMember = new ClubMember();
        clubMember.setClubFunction(clubFunction);

        clubMember.setClub(club);
        clubMember.setCouncilMember(councilMember);

        club.addClubMember(clubMember);
        councilMember.addClubMember(clubMember);

        return clubMember;
    }

    private static Club introduceClub(Town town, Season season, String clubName) {
        log.info(":NEW CLUB: {}", clubName);
        Club club = new Club();
        club.setName(clubName);
        club.setTown(town); //TODO: proxy
        club.setSeason(season);
        club.setRef(generateUniqueKeyReference());
        return club;
    }

    public static Party introduceParty(String partyName) {
        log.info(":NEW PARTY: {}", partyName);
        Party party = new Party();
        party.setRef(partyName);
        party.setName(partyName);
        return party;
    }

    private static ClubParty introduceClubParty(Season season, Club club, Party party) {
        log.info(":NEW CLUB[{}] PARTY: {}", club.getName(), party.getName());
        ClubParty clubParty = new ClubParty();
        clubParty.setParty(party);
        clubParty.setSeason(season);
        clubParty.setClub(club);
        return clubParty;
    }

//    public static void main(String[] args) {
//    	new PresovCouncilMemberCrawler().getCouncilMembers();
//    }

    private static class PresovCouncilMemberRegexpMatcher {
        static String loadValue(Matcher matcher, String name) {
            String result = "";
            switch(name) {
                case "email":
                    for (String group : new String[] {"email", "email2"}) {
                        if (!isNullOrEmpty(matcher.group(group))) {
                            result += readGroup(matcher, group) + ", ";
                        }
                    }
                    result = result.substring(0, result.length() > 1 ? result.length()-2 : result.length());
                    break;
                case "candidateparties":
                    if (matcher.group("candidateparties") != null && !matcher.group("candidateparties").isEmpty()) {
                        result = readGroup(matcher, "candidateparties");
                    } else if (matcher.group("candidateparty") != null && !matcher.group("candidateparty").isEmpty()) {
                        result = readGroup(matcher, "candidateparty");
                    }
                    break;
                default:
                    result = readGroup(matcher, name);
            }
            log.info("LOAD VALUE: {} -> {}", name, result);
            return result;
        }

        static String readGroup(Matcher matcher, String name) {
            if (matcher != null && !isNullOrEmpty(matcher.group(name))) {
                return matcher.group(name);
            }
            return null;
        }

    }
}
