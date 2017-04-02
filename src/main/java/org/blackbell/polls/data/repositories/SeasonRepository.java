package org.blackbell.polls.data.repositories;

import org.blackbell.polls.meetings.model.Season;
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
public interface SeasonRepository extends JpaRepository<Town, Long> {
    @Query(value = "select s from Season s where s.ref = :ref")
    Season findByRef(@Param(value = "ref") String ref);

    @Query(value = "select s from Season s where s.town = :town and s.institution = :institution")
    Season findByTownAndInstitution(@Param(value = "town") String town, @Param(value = "institution") String institution);
}