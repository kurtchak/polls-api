package org.blackbell.polls.domain.repositories;

import org.blackbell.polls.domain.model.Town;
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

    @Query("SELECT t FROM Town t LEFT JOIN FETCH t.seasons WHERE t.ref = :ref")
    Town findByRefWithSeasons(@Param("ref") String ref);
}
