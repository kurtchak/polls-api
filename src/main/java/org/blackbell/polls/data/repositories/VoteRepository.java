package org.blackbell.polls.data.repositories;

import org.blackbell.polls.meetings.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by Ján Korčák on 18.3.2017.
 * email: korcak@esten.sk
 */
@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    @Query(value = "select distinct v from Vote v " +
            "join fetch v.poll p " +
            "join fetch p.agendaItem a " +
            "join fetch a.meeting m " +
            "join fetch v.councilMember cm " +
            "where cm.ref = :memberRef")
    List<Vote> findByCouncilMemberRef(@Param(value = "memberRef") String memberRef);
}
