package org.blackbell.polls.domain.repositories;

import org.blackbell.polls.domain.model.Politician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Politician entity.
 * Supports tracking politicians across electoral seasons.
 */
@Repository
public interface PoliticianRepository extends JpaRepository<Politician, Long> {

    /**
     * Find politician by exact name.
     */
    Optional<Politician> findByName(String name);

    /**
     * Find politician by reference.
     */
    Optional<Politician> findByRef(String ref);

    /**
     * Find all politicians with their council members and party nominations.
     * Useful for tracking "prezliekači" (party switchers).
     */
    @Query(value =
            "select distinct p from Politician p " +
                    "left join fetch p.councilMembers cm " +
                    "left join fetch cm.season s " +
                    "left join fetch cm.town t " +
                    "left join fetch cm.clubMembers clm " +
                    "left join fetch clm.club cl " +
                    "left join fetch p.partyNominees pn " +
                    "left join fetch pn.party pa " +
                    "left join fetch pn.season ps " +
                    "where p.name = :name")
    Optional<Politician> findByNameWithHistory(@Param("name") String name);

    /**
     * Find all politicians who were council members in a specific town.
     */
    @Query(value =
            "select distinct p from Politician p " +
                    "join p.councilMembers cm " +
                    "join cm.town t " +
                    "where t.ref = :town")
    List<Politician> findByTown(@Param("town") String townRef);

    /**
     * Find politicians who changed parties between seasons.
     * Returns politicians with multiple different party nominations.
     */
    @Query(value =
            "select distinct p from Politician p " +
                    "left join fetch p.partyNominees pn " +
                    "left join fetch pn.party pa " +
                    "left join fetch pn.season s " +
                    "where p.id in (" +
                    "  select p2.id from Politician p2 " +
                    "  join p2.partyNominees pn2 " +
                    "  group by p2.id " +
                    "  having count(distinct pn2.party.id) > 1" +
                    ")")
    List<Politician> findPartySwitchers();

    /**
     * Find politicians who changed clubs between seasons.
     */
    @Query(value =
            "select distinct p from Politician p " +
                    "left join fetch p.councilMembers cm " +
                    "left join fetch cm.clubMembers clm " +
                    "left join fetch clm.club cl " +
                    "left join fetch cm.season s " +
                    "where p.id in (" +
                    "  select p2.id from Politician p2 " +
                    "  join p2.councilMembers cm2 " +
                    "  join cm2.clubMembers clm2 " +
                    "  group by p2.id " +
                    "  having count(distinct clm2.club.id) > 1" +
                    ")")
    List<Politician> findClubSwitchers();
}