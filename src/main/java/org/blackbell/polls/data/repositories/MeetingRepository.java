package org.blackbell.polls.data.repositories;

import org.blackbell.polls.meetings.model.InstitutionType;
import org.blackbell.polls.meetings.model.Meeting;
import org.blackbell.polls.meetings.model.Town;
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
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    @Query(value = "select m from Meeting m " +
                            "join fetch m.season s " +
                            "join fetch m.town t " +
                            "join fetch m.institution i " +
                        "where m.town.ref = :town " +
                            "and m.season.ref = :season " +
                            "and m.institution.type = :institution " +
                            "and (:dateFrom is null and :dateTo is null " +
                                "or m.date between :dateFrom and :dateTo)")
    List<Meeting> getByTownAndInstitutionAndSeason(@Param(value = "town") String town,
                                                   @Param(value = "institution") InstitutionType institution,
                                                   @Param(value = "season") String season,
                                                   @Param(value = "dateFrom") Date dateFrom,
                                                   @Param(value = "dateTo") Date dateTo);

    @Query(value =
            "select m from Meeting m " +
                    "left join fetch m.season s " +
                    "left join fetch m.town t " +
                    "left join fetch m.agendaItems a " +
                    "left join fetch a.attachments aa " +
                    "left join fetch m.attachments at " +
                "where m.ref = :ref")
    Meeting getByRef(@Param(value = "ref") String ref);

    @Query(value = "select max(m.date) from Meeting m " +
                        "where m.town = :town " +
                            "and m.season.ref = :season " +
                            "and m.institution.type = :institution")
    Date getLatestMeetingDate(@Param(value = "town") Town town,
                              @Param(value = "institution") InstitutionType institution,
                              @Param(value = "season") String season);

    @Query(value = "select distinct m.season from Meeting m where m.town = :town")
    Date getSeasonsWithMeetingsForTown(@Param(value = "town") Town town);
}
