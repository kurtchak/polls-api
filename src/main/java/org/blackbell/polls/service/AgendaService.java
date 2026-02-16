package org.blackbell.polls.service;

import org.blackbell.polls.domain.model.AgendaItem;
import org.blackbell.polls.domain.repositories.AgendaRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class AgendaService {

    private final AgendaRepository agendaRepository;

    public AgendaService(AgendaRepository agendaRepository) {
        this.agendaRepository = agendaRepository;
    }

    public Collection<AgendaItem> getAgenda(String meetingRef) {
        return agendaRepository.getByMeeting(meetingRef);
    }

    public AgendaItem getAgendaItem(String ref) {
        return agendaRepository.getByRef(ref);
    }
}
