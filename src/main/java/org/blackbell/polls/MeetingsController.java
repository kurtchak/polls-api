package org.blackbell.polls;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.data.repositories.CouncilMemberRepository;
import org.blackbell.polls.data.repositories.PollRepository;
import org.blackbell.polls.data.repositories.TownRepository;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@RestController
public class MeetingsController {
    private static final Logger log = LoggerFactory.getLogger(MeetingsController.class);

    @Autowired
    private TownRepository townRepository;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private CouncilMemberRepository councilMemberRepository;

    public void checkLoaded(String city, String institution) throws Exception {
//        ApplicationContext context = ApplicationContext.getInstance();
        Town town = townRepository.findByRef(city);
        if (town == null) {
            log.info("No town with name `"+city+"`. Loading from external WebService...");
            town = Application.loadTownData(city, institution);
            if (town != null) {
                List<Season> seasons = town.getSeasons();
                log.info("Loaded town `" + town.getName() + "` with data for " + (seasons != null ? seasons.size() : 0) + " seasons");
                townRepository.save(town);
                log.info(town.getName() + "`s data saved.");
            } else {
                log.info("No data found for town `" + city + "`.");
            }
        }
    }

//    @RequestMapping("/{city}/{season}/meetings")
//    public Collection<Meeting> meetings(@PathVariable(value="city") String city,
//                                        @PathVariable(value="season") String season) {
//        checkLoaded(city);
//        return ApplicationContext.getInstance().getMeetings(city, season);
//    }
//
//    @RequestMapping("/{city}/{season}/meeting/{order}")
//    public Meeting meeting(@PathVariable(value="city") String city,
//                           @PathVariable(value="season") String season,
//                           @PathVariable(value="order") Integer order) {
//        checkLoaded(city);
//        return ApplicationContext.getInstance().getMeeting(city, season, order);
//    }
//
////    @RequestMapping("/{city}/{season}/meeting/{order}/agenda")
////    public Agenda agenda(@PathVariable(value="city") String city,
////                         @PathVariable(value="season") String season,
////                         @PathVariable(value="order") Integer order) {
////        Application.checkLoaded(city);
////        return ApplicationContext.getInstance().getAgenda(city, season, order);
////    }
////
////    @RequestMapping("/{city}/{season}/meeting/{order}/agenda/{item}")
////    public AgendaItem agendaItem(@PathVariable(value="city") String city,
////                                 @PathVariable(value="season") String season,
////                                 @PathVariable(value="order") Integer order,
////                                 @PathVariable(value="item") Integer item) {
////        Application.checkLoaded(city);
////        return ApplicationContext.getInstance().getAgendaItem(city, season, order, item);
////    }
//
//    @RequestMapping("/{city}/{season}/meeting/{order}/attachments")
//    public Collection<MeetingAttachment> atachments(@PathVariable(value="city") String city,
//                                              @PathVariable(value="season") String season,
//                                              @PathVariable(value="order") Integer order) {
//        checkLoaded(city);
//        return ApplicationContext.getInstance().getAttachments(city, season, order);
//    }
//
//    @RequestMapping("/{city}/{season}/meeting/{order}/attachment/{item}")
//    public MeetingAttachment attachment(@PathVariable(value="city") String city,
//                                 @PathVariable(value="season") String season,
//                                 @PathVariable(value="order") Integer order,
//                                 @PathVariable(value="item") Integer item) {
//        checkLoaded(city);
//        return ApplicationContext.getInstance().getMeetingAttachment(city, season, order, item);
//    }
//
    @JsonView(value = Views.CouncilMember.class)
    @RequestMapping("/{city}/{institution}/member/{member_ref}")
    public CouncilMember member(@PathVariable(value="city") String city,
                     @PathVariable(value="institution") String institution,
                     @PathVariable(value="member_ref") String memberRef) throws Exception {
        checkLoaded(city, institution);
        return councilMemberRepository.findByRef(memberRef);
    }

    @JsonView(value = Views.CouncilMembers.class)
    @RequestMapping("/{city}/{institution}/{season}/members")
    public Collection<CouncilMember> members(@PathVariable(value="city") String city,
                                             @PathVariable(value="institution") String institution,
                                             @PathVariable(value="season") String season) throws Exception {
        checkLoaded(city, institution);
        List<CouncilMember> members = councilMemberRepository.getBySeason(season);
        return members;
    }

    @JsonView(value = Views.Polls.class)
    @RequestMapping("/{city}/{institution}")
    public Collection<Poll> polls(@PathVariable(value="city") String city,
                                  @PathVariable(value="institution") String institution) throws Exception {
        checkLoaded(city, institution);
        List<Poll> polls = pollRepository.getByTown(city);
        log.info((polls != null ? polls.size() : 0) + " found polls");
        return polls;
//        return ApplicationContext.getInstance().getPolls(city, institution);
    }

    @JsonView(value = Views.Poll.class)
    @RequestMapping("/{city}/{institution}/{poll_ref}")
    public Poll poll(@PathVariable(value="city") String city,
                     @PathVariable(value="institution") String institution,
                     @PathVariable(value="poll_ref") String pollRef) throws Exception {
        checkLoaded(city, institution);
        Poll poll = pollRepository.getByRef(pollRef);
        if (poll != null) {
            for (Vote vote : poll.getVotes()) {
                switch (vote.getVoted()) {
                    case VOTED_FOR:
                        poll.setVotedFor(poll.getVotedFor() + 1);
                        break;
                    case VOTED_AGAINST:
                        poll.setVotedAgainst(poll.getVotedAgainst() + 1);
                        break;
                    case NOT_VOTED:
                        poll.setNotVoted(poll.getNotVoted() + 1);
                        break;
                    case ABSTAIN:
                        poll.setAbstain(poll.getAbstain() + 1);
                        break;
                    case ABSENT:
                        poll.setAbsent(poll.getAbsent() + 1);
                        break;
                    default:
                        log.info("Unknown vote choice found: " + vote.getVoted());
                }
            }
            long membersCount = councilMemberRepository.count();
            if (poll.getVotedFor() > membersCount / 2) {
                poll.setResult(VoteResult.PASSED);
            } else {
                poll.setResult(VoteResult.REJECTED);
            }
        }
        log.info("poll: " + poll);
        return poll;
//        return ApplicationContext.getInstance().getPolls(city, institution);
    }

//    @RequestMapping("/{city}/{institution}/{poll_number}")
//    public Poll poll(@PathVariable(value="city") String city,
//                                  @PathVariable(value="institution") String institution,
//                                  @PathVariable(value="poll_number") String pollNumber) {
//        checkLoaded(city);
//        return ApplicationContext.getInstance().getPoll(city, institution, pollNumber);
//    }

}