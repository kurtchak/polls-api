package org.blackbell.polls.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.AgendaItem;
import org.blackbell.polls.service.AgendaService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @JsonView(value = Views.Agenda.class)
    @RequestMapping({"/meetings/{meetingRef}/agenda",
                     "/{city}/{institution}/meeting/{meetingRef}/agenda"})
    public Collection<AgendaItem> agenda(@PathVariable String meetingRef) {
        return agendaService.getAgenda(meetingRef);
    }

    @JsonView(value = Views.AgendaItem.class)
    @RequestMapping({"/agenda/{ref}",
                     "/{city}/{institution}/agenda/{ref}"})
    public AgendaItem agendaItem(@PathVariable String ref) {
        return agendaService.getAgendaItem(ref);
    }
}
