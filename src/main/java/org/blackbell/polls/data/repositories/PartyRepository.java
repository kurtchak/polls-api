package org.blackbell.polls.data.repositories;

import org.blackbell.polls.meetings.model.CouncilMember;
import org.blackbell.polls.meetings.model.Party;
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
public interface PartyRepository extends JpaRepository<CouncilMember, Long> {
    @Query(value = "select p from Party p where p.ref = :ref")
    Party getByRef(@Param(value = "ref") String ref);

    @Query(value = "select p from Party p where p.name = :name")
    Party getByName(@Param(value = "name") String name);
}
