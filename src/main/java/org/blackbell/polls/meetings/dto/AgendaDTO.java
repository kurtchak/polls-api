package org.blackbell.polls.meetings.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by Ján Korčák on 19.2.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgendaDTO extends MeetingComponentDTO {

    @JsonProperty(value = "children")
    private List<AgendaItemDTO> agendaItemDTOs;

    public List<AgendaItemDTO> getAgendaItemDTOs() {
        return agendaItemDTOs;
    }

    public void setAgendaItemDTOs(List<AgendaItemDTO> agendaItemDTOs) {
        this.agendaItemDTOs = agendaItemDTOs;
    }

    @Override
    public String toString() {
        return "AgendaDTO{" +
                "agendaItemDTOs=" + agendaItemDTOs +
                '}';
    }
}
