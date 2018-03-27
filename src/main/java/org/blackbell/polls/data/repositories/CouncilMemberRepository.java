package org.blackbell.polls.data.repositories;

import org.blackbell.polls.meetings.model.CouncilMember;
import org.blackbell.polls.meetings.model.Institution;
import org.blackbell.polls.meetings.model.Season;
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
    @Query(value =
            "select m from CouncilMember m " +
                    "join fetch m.clubMembers cm " +
                    "join fetch m.partyNominees pn " +
                "where m.season.town.ref = :town " +
                    "and m.season.ref = :season " +
                    "and m.season.institution = :institution")
    List<CouncilMember> getByTownAndSeasonAndInstitution(@Param(value = "town") String town, @Param(value = "season") String season, @Param(value = "institution") Institution institution);

    @Query(value =
            "select m from CouncilMember m " +
                    "join fetch m.clubMembers cm " +
                    "join fetch cm.club c " +
                    "join fetch c.season s " +
                    "join fetch c.clubParties cp " +
                    "join fetch cp.party p " +
                    "join fetch m.partyNominees pn " +
                "where m.ref = :ref")
    CouncilMember findByRef(@Param(value = "ref") String memberRef);

    @Query(value = "select m from CouncilMember m where m.season = :season")
    List<CouncilMember> findBySeason(@Param(value = "season") Season season);
}
