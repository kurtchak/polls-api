package org.blackbell.polls.meetings.dm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by Ján Korčák on 19.2.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgendaItemDTO {

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "children")
    private List<AgendaItemComponentDTO> children;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AgendaItemComponentDTO> getChildren() {
        return children;
    }

    public void setChildren(List<AgendaItemComponentDTO> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "AgendaItem{" +
                "name='" + name + '\'' +
                ", children=" + children +
                '}';
    }
}
