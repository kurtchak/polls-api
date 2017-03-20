package org.blackbell.polls.data.repositories;

import org.blackbell.polls.meetings.model.Town;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Created by Ján Korčák on 18.3.2017.
 * email: korcak@esten.sk
 */
@Repository
public interface TownRepository extends JpaRepository<Town, Long> {
    @Query(value = "select t from Town t where t.ref = :ref")
    Town findByRef(@Param(value = "ref") String town);
}
