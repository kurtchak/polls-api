package org.blackbell.polls.controllers;

/**
 * Created by Ján Korčák on 21.4.2018.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.AgendaItem;
import org.blackbell.polls.domain.repositories.AgendaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

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
    public Collection<AgendaItem> agenda(@PathVariable(value = "meeting_ref") String meetingRef) throws Exception {
        return agendaRepository.getByMeeting(meetingRef);
    }

    @JsonView(value = Views.AgendaItem.class)
    @RequestMapping({"/agenda/{ref}",
                     "/{city}/{institution}/agenda/{ref}"})
    public AgendaItem agendaItem(@PathVariable(value="ref") String ref) throws Exception {
        return agendaRepository.getByRef(ref);
    }
}
