package org.blackbell.polls.controllers;

/**
 * Created by Ján Korčák on 21.4.2018.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.model.Vote;
import org.blackbell.polls.repositories.VoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class VotesController {
    private static final Logger log = LoggerFactory.getLogger(VotesController.class);

    private VoteRepository voteRepository;

    public VotesController(VoteRepository voteRepository) {
        this.voteRepository = voteRepository;
    }

    @JsonView(value = Views.Votes.class)
    @RequestMapping({"/members/{ref}/votes",
                     "/{city}/{institution}/member/{ref}/votes"})
    public List<Vote> memberVotes(@PathVariable(value="ref") String memberRef) throws Exception {
        return voteRepository.findByCouncilMemberRef(memberRef);
    }
}
