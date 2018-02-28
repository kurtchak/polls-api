package org.blackbell.polls.data.repositories;

import org.blackbell.polls.meetings.model.Institution;
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
    @Query(value = "select m from Meeting m where m.season.town.name = :town and m.season.name = :season and m.season.institution = :institution")
    List<Meeting> getByTownAndInstitutionAndSeason(@Param(value = "town") String town, @Param(value = "institution") Institution institution, @Param(value = "season") String season);

    @Query(value = "select m from Meeting m where m.ref = :ref")
    Meeting getByRef(@Param(value = "ref") String ref);

    @Query(value = "select max(m.date) from Meeting m where m.season.town = :town and m.season.name = :season and m.season.institution = :institution")
    Date getLatestMeetingDate(@Param(value = "town") Town town, @Param(value = "institution") Institution institution, @Param(value = "season") String season);
}
