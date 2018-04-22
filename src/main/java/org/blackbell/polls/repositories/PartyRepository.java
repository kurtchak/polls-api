package org.blackbell.polls.repositories;

import org.blackbell.polls.model.Party;
import org.blackbell.polls.model.relate.PartyNominee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * Created by Ján Korčák on 18.3.2017.
 * email: korcak@esten.sk
 */
@Repository
public interface PartyRepository extends JpaRepository<Party, Long> {
    @Query(value = "select p from Party p where p.ref = :ref")
    Party getByRef(@Param(value = "ref") String ref);

    @Query(value = "select p from Party p where p.name = :name")
    Party getByName(@Param(value = "name") String name);

    @Query(value = "select distinct c from Party c, PartyNominee p " +
                        "where p.party.id = c.id " +
                            "and p.town.ref = :town " +
                            "and p.season.ref = :season")
    Collection<Party> getByTownAndSeasonAndInstitution(@Param(value = "town") String town,
                                                       @Param(value = "season") String season);

    @Query(value = "select c from PartyNominee c " +
                        "where c.town.ref = :town " +
                            "and c.season.ref = :season " +
                            "and c.party.ref = :ref")
    Collection<PartyNominee> getPartyNomineesByTownAndSeason(@Param(value = "town") String town,
                                                             @Param(value = "season") String season,
                                                             @Param(value = "ref") String ref);
}
