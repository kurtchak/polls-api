package org.blackbell.polls.meetings.source.dm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeasonMeetingDTO {
    @JsonProperty(value = "Id")
    private String id;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "termin")
    private String date;

    public SeasonMeetingDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    @Override
    public String toString() {
        return "SeasonMeetingDTO{" +
                "name='" + name + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}
