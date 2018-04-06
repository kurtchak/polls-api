package org.blackbell.polls.data.repositories;

import org.blackbell.polls.meetings.model.InstitutionType;
import org.blackbell.polls.meetings.model.Poll;
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
            "select p from Poll p " +
                    "join fetch p.agendaItem a " +
                    "join fetch a.meeting m " +
                    "join fetch m.season s " +
                    "join fetch s.town t " +
                    "join fetch a.attachments at " +
                "where s.town.ref = :town " +
                    "and s.ref = :season " +
                    "and s.institution = :institution " +
                    "and (:dateFrom is null and :dateTo is null " +
                            "or m.date between :dateFrom and :dateTo)")
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
                        "join fetch cm.partyNominees pn " +
                        "join fetch pn.party pt " +
                        "join fetch cm.clubMembers cbm " +
                        "join fetch cbm.club c " +
                    "where p.ref = :ref")
    Poll getByRef(@Param(value = "ref") String pollRef);
}
