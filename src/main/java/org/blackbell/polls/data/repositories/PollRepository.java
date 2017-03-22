package org.blackbell.polls.data.repositories;

import org.blackbell.polls.meetings.model.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by Ján Korčák on 18.3.2017.
 * email: korcak@esten.sk
 */
@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {
    @Query(value = "select p from Poll p where p.agendaItem.meeting.season.town.name = :town")
    List<Poll> getByTown(@Param(value = "town") String town);

    @Query(value = "select p from Poll p where p.ref = :ref")
    Poll getByRef(@Param(value = "ref") String pollRef);
}
