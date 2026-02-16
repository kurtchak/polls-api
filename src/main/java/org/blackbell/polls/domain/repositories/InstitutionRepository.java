package org.blackbell.polls.domain.repositories;

import org.blackbell.polls.domain.model.Institution;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Created by Ján Korčák on 28.4.2018.
 * email: korcak@esten.sk
 */
@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {
    @Query(value = "select i from Institution i where i.type = :type")
    Institution findByType(@Param(value = "type") InstitutionType type);

    @Query(value = "select i from Institution i where i.type = :type")
    List<Institution> findMore(@Param(value = "type") InstitutionType type);
}
