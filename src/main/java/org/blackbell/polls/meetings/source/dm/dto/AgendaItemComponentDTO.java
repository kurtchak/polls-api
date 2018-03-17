package org.blackbell.polls.meetings.source.dm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by Ján Korčák on 4.3.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "name",
        defaultImpl = PollsDTO.class,
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PollsDTO.class, name = "Hlasovania"),
        @JsonSubTypes.Type(value = ProspectsDTO.class, name = "Materialy"),
})
public abstract class AgendaItemComponentDTO {

    @JsonProperty(value = "name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "AgendaItemComponentDTO{" +
                "name='" + name + '\'' +
                '}';
    }
}
