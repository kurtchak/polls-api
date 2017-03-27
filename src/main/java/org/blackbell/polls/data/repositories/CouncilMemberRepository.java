package org.blackbell.polls.data.repositories;

import org.blackbell.polls.meetings.model.CouncilMember;
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
public interface CouncilMemberRepository extends JpaRepository<CouncilMember, Long> {
    @Query(value = "select m from CouncilMember m where m.season.ref = :season")
    List<CouncilMember> getBySeason(@Param(value = "season") String season);

    @Query(value = "select m from CouncilMember m where m.ref = :ref")
    CouncilMember findByRef(@Param(value = "ref") String memberRef);
}
