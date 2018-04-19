package org.blackbell.polls.data.repositories;

import org.blackbell.polls.meetings.model.Club;
import org.blackbell.polls.meetings.model.ClubMember;
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
            "select distinct c from Club c " +
                    "join fetch c.town t " +
                    "join fetch c.season s " +
                "where t.ref = :town " +
                    "and s.ref = :season")
    List<Club> getByTownAndSeason(@Param(value = "town") String town,
                                  @Param(value = "season") String season);

    @Query(value =
            "select c from Club c " +
                    "left join fetch c.season s " +
                    "left join fetch c.town t " +
                    "left join fetch c.clubMembers cm " +
                    "left join fetch cm.councilMember cmb " +
                    "left join fetch cmb.politician pl " +
                    "left join fetch pl.partyNominees pn " +
                    "left join fetch c.clubParties cp " +
                    "left join fetch cp.party p " +
                "where c.ref = :ref")
    Club findByRef(@Param(value = "ref") String ref);

    @Query(value = "select c from ClubMember c where c.club.town.ref = :town and c.club.season.ref = :season and c.club.ref = :ref")
    Collection<ClubMember> getClubMembersByTownAndSeasonAndRef(@Param(value = "town") String town, @Param(value = "season") String season, @Param(value = "ref") String ref);
}
