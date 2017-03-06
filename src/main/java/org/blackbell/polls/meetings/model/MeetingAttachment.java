package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by Ján Korčák on 4.3.2017.
 * email: korcak@esten.sk
 */
//@Entity
public class MeetingAttachment {
    @JsonIgnore
    private long id;
    private int order;
    private String name;
    private String source;

    public MeetingAttachment(String name, int order, String source) {
        this.order = order;
        this.name = name;
        this.source = source;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
