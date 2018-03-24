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
                "where p.agendaItem.meeting.town.ref = :town " +
                    "and p.agendaItem.meeting.season.ref = :season " +
                    "and p.agendaItem.meeting.institution.type = :institution " +
                    "and p.agendaItem.meeting.date between :dateFrom and :dateTo")
    List<Poll> getByTownAndSeasonAndInstitution(@Param(value = "town") String town,
                                                @Param(value = "season") String season,
                                                @Param(value = "institution") InstitutionType institution,
                                                @Param(value = "dateFrom") Date dateFrom,
                                                @Param(value = "dateTo") Date dateTo);

    @Query(value = "select p from Poll p where p.ref = :ref")
    Poll getByRef(@Param(value = "ref") String pollRef);
}
