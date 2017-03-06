package org.blackbell.polls.meetings.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetingDTO {
    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "termin")
    private String date;

    @JsonProperty(value = "children")
    private List<MeetingComponentDTO> children;

    public MeetingDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<MeetingComponentDTO> getChildren() {
        return children;
    }

    public void setChildren(List<MeetingComponentDTO> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "Meeting{" +
                "name='" + name + '\'' +
                ", date='" + date + '\'' +
                ", children=" + children +
                '}';
    }
}
