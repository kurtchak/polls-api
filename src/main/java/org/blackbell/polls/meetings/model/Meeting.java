package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */
public class Meeting {
    @JsonIgnore
    private long id;
    private int order;
    private String name;

    private Date date;

    private Agenda agenda;

    private Map<Integer, MeetingAttachment> attachments;

    public Serializable getId() {
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Agenda getAgenda() {
        return agenda;
    }

    public void setAgenda(Agenda agenda) {
        this.agenda = agenda;
    }

    public Map<Integer, MeetingAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<Integer, MeetingAttachment> attachments) {
        this.attachments = attachments;
    }
}
