package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */
public class Season {
    @JsonIgnore
    private long id;
    private String name;

    private Map<Integer, Meeting> meetings;

    private Map<Integer, CouncilMember> members;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Integer, Meeting> getMeetings() {
        return meetings;
    }

    public void setMeetings(Map<Integer, Meeting> meeting) {
        this.meetings = meeting;
    }

    public Map<Integer, CouncilMember> getMembers() {
        return members;
    }

    public void setMembers(Map<Integer, CouncilMember> members) {
        this.members = members;
    }
}
