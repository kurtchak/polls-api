package org.blackbell.polls.meetings.source.dm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeasonDTO {

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "children")
    private List<MeetingDTO> meetingDTOs;

    private Map<String, MeetingDTO> meetingsMap;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MeetingDTO> getMeetingDTOs() {
        return meetingDTOs;
    }

    public void setMeetingDTOs(List<MeetingDTO> meetingDTOs) {
        this.meetingDTOs = meetingDTOs;
    }

    public Map<String, MeetingDTO> getMeetingsMap() {
        return meetingsMap;
    }

    public void setMeetingsMap(Map<String, MeetingDTO> meetingsMap) {
        this.meetingsMap = meetingsMap;
    }

    @Override
    public String toString() {
        return "Season{" +
                "name='" + name + '\'' +
                ", meetingDTOs=" + meetingDTOs +
                '}';
    }
}
