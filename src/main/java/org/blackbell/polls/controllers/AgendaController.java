package org.blackbell.polls.controllers;

/**
 * Created by Ján Korčák on 21.4.2018.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.common.Constants;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.model.Club;
import org.blackbell.polls.model.Meeting;
import org.blackbell.polls.model.common.BaseEntity;
import org.blackbell.polls.model.enums.InstitutionType;
import org.blackbell.polls.repositories.AgendaRepository;
import org.blackbell.polls.repositories.ClubRepository;
import org.blackbell.polls.repositories.MeetingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@RestController
public class AgendaController {
    private static final Logger log = LoggerFactory.getLogger(AgendaController.class);

    private AgendaRepository agendaRepository;

    public AgendaController(AgendaRepository agendaRepository) {
        this.agendaRepository = agendaRepository;
    }

    @JsonView(value = Views.Agenda.class)
    @RequestMapping({"/meetings/{meeting_ref}/agenda",
                     "/{city}/{institution}/meeting/{meeting_ref}/agenda"})
    public Collection<BaseEntity> agenda(@PathVariable(value = "meeting_ref") String meetingRef) throws Exception {
        return agendaRepository.getByMeeting(meetingRef);
    }

    @JsonView(value = Views.AgendaItem.class)
    @RequestMapping({"/agenda/{ref}",
                     "/{city}/{institution}/agenda/{ref}"})
    public BaseEntity agendaItem(@PathVariable(value="ref") String ref) throws Exception {
        return agendaRepository.getByRef(ref);
    }
}
