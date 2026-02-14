package org.blackbell.polls.domain.repositories;

import org.blackbell.polls.domain.model.Meeting;
import org.blackbell.polls.domain.model.Town;
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
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    @Query(value = "select m from Meeting m " +
                            "join fetch m.season s " +
                            "join fetch m.town t " +
                            "join fetch m.institution i " +
                        "where m.town.ref = :town " +
                            "and m.season.ref = :season " +
                            "and m.institution.type = :institutionType " +
                            "and (:dateFrom is null and :dateTo is null " +
                                "or m.date between :dateFrom and :dateTo) " +
                        "order by m.date")
    List<Meeting> getByTownAndInstitutionAndSeason(@Param(value = "town") String town,
                                                   @Param(value = "institutionType") InstitutionType institutionType,
                                                   @Param(value = "season") String season,
                                                   @Param(value = "dateFrom") Date dateFrom,
                                                   @Param(value = "dateTo") Date dateTo);

    @Query(value =
            "select m from Meeting m " +
                    "left join fetch m.season s " +
                    "left join fetch m.institution i " +
                    "left join fetch m.town t " +
                    "left join fetch m.agendaItems a " +
                    "left join fetch a.attachments aa " +
                    "left join fetch m.attachments at " +
                "where m.ref = :ref " +
                "order by m.date")
    Meeting getByRef(@Param(value = "ref") String ref);

    @Query(value = "select max(m.date) from Meeting m " +
                        "where m.town = :town " +
                            "and m.season.ref = :season " +
                            "and m.institution.type = :institutionType")
    Date getLatestMeetingDate(@Param(value = "town") Town town,
                              @Param(value = "institutionType") InstitutionType institutionType,
                              @Param(value = "season") String season);

    @Query("SELECT m FROM Meeting m LEFT JOIN FETCH m.agendaItems WHERE m.extId = :extId")
    Meeting findByExtId(@Param("extId") String extId);

    @Query(value =
            "select distinct m.season from Meeting m " +
                "where m.town = :town " +
                "order by m.date")
    Date getSeasonsWithMeetingsForTown(@Param(value = "town") Town town);

    @Query("SELECT m FROM Meeting m JOIN FETCH m.town JOIN FETCH m.season JOIN FETCH m.institution WHERE m.syncError IS NOT NULL ORDER BY m.town.ref, m.season.ref, m.date")
    List<Meeting> findFailedMeetings();
}
