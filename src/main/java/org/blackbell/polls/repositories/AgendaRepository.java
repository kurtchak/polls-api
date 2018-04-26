package org.blackbell.polls.repositories;

import org.blackbell.polls.model.common.BaseEntity;
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
public interface AgendaRepository extends JpaRepository<BaseEntity, Long> {
    @Query(value =
            "select a from AgendaItem a " +
                    "join fetch a.meeting m " +
                    "join fetch a.polls p " +
                    "where a.meeting.ref = :meetingRef")
    List<BaseEntity> getByMeeting(@Param(value = "meetingRef") String meetingRef);

    @Query(value =
            "select a from AgendaItem a " +
                    "join fetch a.meeting m " +
                    "join fetch a.polls p " +
                    "where m.ref = :ref")
    BaseEntity getByRef(@Param(value = "ref") String ref);
}
