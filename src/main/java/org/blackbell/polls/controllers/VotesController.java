package org.blackbell.polls.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Vote;
import org.blackbell.polls.service.VoteService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class VotesController {

    private final VoteService voteService;

    public VotesController(VoteService voteService) {
        this.voteService = voteService;
    }

    @JsonView(value = Views.Votes.class)
    @RequestMapping({"/members/{memberRef}/votes",
                     "/{city}/{institution}/member/{memberRef}/votes"})
    public List<Vote> memberVotes(@PathVariable String memberRef) {
        return voteService.getMemberVotes(memberRef);
    }
}
