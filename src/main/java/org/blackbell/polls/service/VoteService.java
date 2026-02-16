package org.blackbell.polls.service;

import org.blackbell.polls.domain.model.Vote;
import org.blackbell.polls.domain.repositories.VoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoteService {

    private final VoteRepository voteRepository;

    public VoteService(VoteRepository voteRepository) {
        this.voteRepository = voteRepository;
    }

    public List<Vote> getMemberVotes(String memberRef) {
        return voteRepository.findByCouncilMemberRef(memberRef);
    }
}
