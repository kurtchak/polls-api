package org.blackbell.polls.data.repositories;

import org.blackbell.polls.meetings.model.Club;
import org.blackbell.polls.meetings.model.ClubMember;
import org.blackbell.polls.meetings.model.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Created by Ján Korčák on 16.3.2018.
 * email: korcak@esten.sk
 */
@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {
    @Query(value =
            "select c from Club c " +
                    "join fetch c.season s " +
                "where s.town.ref = :town " +
                    "and s.ref = :season")
    List<Club> getByTownAndSeason(@Param(value = "town") String town,
                                  @Param(value = "season") String season);

    @Query(value =
            "select c from Club c " +
                    "join fetch c.season s " +
                    "join fetch s.town t " +
                    "join fetch c.clubMembers cm " +
                    "join fetch cm.councilMember cmb " +
                    "join fetch cmb.partyNominees pn " +
                    "join fetch c.clubParties cp " +
                    "join fetch cp.party p " +
                "where c.ref = :ref")
    Club findByRef(@Param(value = "ref") String ref);

}
