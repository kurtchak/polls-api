package org.blackbell.polls.domain.repositories;

import org.blackbell.polls.domain.model.Poll;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Created by Ján Korčák on 18.3.2017.
 * email: korcak@esten.sk
 */
@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {
    @Query(value =
            "select distinct p from Poll p " +
                    "join fetch p.agendaItem a " +
                    "join fetch a.meeting m " +
                    "join fetch m.season s " +
                    "join fetch m.town t " +
                    "left join fetch a.attachments at " +
                "where t.ref = :town " +
                    "and s.ref = :season " +
                    "and m.institution.type = :institution " +
                    "and (:dateFrom is null and :dateTo is null " +
                            "or m.date between :dateFrom and :dateTo) " +
                "order by m.date")
    List<Poll> getByTownAndSeasonAndInstitution(@Param(value = "town") String town,
                                                @Param(value = "season") String season,
                                                @Param(value = "institution") InstitutionType institution,
                                                @Param(value = "dateFrom") Date dateFrom,
                                                @Param(value = "dateTo") Date dateTo);

    @Query(value = "select p from Poll p " +
                        "join fetch p.agendaItem a " +
                        "join fetch a.meeting m " +
                        "join fetch m.season s " +
                        "join fetch p.votes v " +
                        "join fetch v.councilMember cm " +
                        "left join fetch cm.politician pl " +
                        "left join fetch pl.partyNominees pn " +
                        "left join fetch pn.party pt " +
                        "left join fetch cm.clubMembers cbm " +
                        "left join fetch cbm.club c " +
                    "where p.ref = :ref")
    Poll getByRef(@Param(value = "ref") String pollRef);
}
