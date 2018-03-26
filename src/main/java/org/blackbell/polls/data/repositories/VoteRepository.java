package org.blackbell.polls.data.repositories;

import org.blackbell.polls.meetings.model.Institution;
import org.blackbell.polls.meetings.model.Poll;
import org.blackbell.polls.meetings.model.vote.Vote;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Created by Ján Korčák on 18.3.2017.
 * email: korcak@esten.sk
 */
@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    @Query(value = "select v from Vote v where v.councilMember.ref = :memberRef")
    List<Vote> findByCouncilMemberRef(@Param(value = "memberRef") String memberRef, Pageable page);

    @Query(value = "select count(v) from Vote v where v.councilMember.ref = :memberRef")
    int getVotesCountByCouncilMemberRef(@Param(value = "memberRef") String memberRef);

}
