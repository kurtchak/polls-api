package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import java.util.Map;

/**
 * Created by Ján Korčák on 19.2.2017.
 * email: korcak@esten.sk
 */
@Entity
public class AgendaItem {
    @JsonIgnore
    private long id;
    private Integer order;
    private String name;
    private Map<Integer, Poll> polls;
    private Map<String, AgendaItemAttachment> attachments;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Integer, Poll> getPolls() {
        return polls;
    }

    public void setPolls(Map<Integer, Poll> polls) {
        this.polls = polls;
    }

    public Map<String, AgendaItemAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, AgendaItemAttachment> attachments) {
        this.attachments = attachments;
    }
}
