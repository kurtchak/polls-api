package org.blackbell.polls.domain.repositories;

import org.blackbell.polls.domain.model.CouncilMember;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Created by Ján Korčák on 18.3.2017.
 * email: korcak@esten.sk
 */
@Repository
public interface CouncilMemberRepository extends JpaRepository<CouncilMember, Long> {
    @Query(value =
            "select m from CouncilMember m " +
                    "left join fetch m.clubMembers cm " +
                        "left join fetch cm.club c " +
                            "left join fetch c.clubParties cp " +
                                "left join fetch cp.party cpp " +
                    "left join fetch m.season s " +
                    "left join fetch m.politician pl " +
                        "left join fetch pl.partyNominees pn " +
                            "left join fetch pn.party pnp " +
                    "where m.ref = :ref")
    CouncilMember findByRef(@Param(value = "ref") String memberRef);

    @Query(value =
            "select distinct m from CouncilMember m " +
                    "left join fetch m.clubMembers cm " +
                        "left join fetch cm.club c " +
                            "left join fetch c.clubParties cp " +
                                "left join fetch cp.party cpp " +
                    "join fetch m.institution i " +
                    "join fetch m.season s " +
                    "join fetch m.town t " +
                    "join fetch m.politician pl " +
                        "left join fetch pl.partyNominees pn " +
                            "left join fetch pn.party p " +
                "where m.town.ref = :town " +
                    "and m.season.ref = :season " +
                    "and m.institution.type = :institution")
    Set<CouncilMember> getByTownAndSeasonAndInstitution(@Param(value = "town") String town,
                                                         @Param(value = "season") String season,
                                                         @Param(value = "institution") InstitutionType institution);

    @Query(value =
            "select distinct m from CouncilMember m " +
                    "left join fetch m.clubMembers cm " +
                    "join fetch m.politician pl " +
                        "left join fetch pl.partyNominees pn " +
                "where m.town.ref = :town " +
                    "and not exists (select cm.id from ClubMember cm " +
                                        "where cm.councilMember.id = m.id " +
                                        "and cm.club.season.ref = :season)")
    List<CouncilMember> getFreeCouncilMembers(@Param(value = "town") String town, @Param(value = "season") String season);

    /**
     * Fix orphan members that were saved without town_id or institution_id.
     * Uses native SQL because these columns have updatable=false in JPA mapping.
     */
    @Modifying
    @Query(value = "UPDATE council_member SET town_id = :townId, institution_id = :instId " +
                   "WHERE season_id = :seasonId AND town_id IS NULL",
           nativeQuery = true)
    int fixOrphanMembers(@Param("townId") long townId,
                         @Param("seasonId") long seasonId,
                         @Param("instId") long instId);

}
