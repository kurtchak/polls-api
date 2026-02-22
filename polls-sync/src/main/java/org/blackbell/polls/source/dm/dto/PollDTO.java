package org.blackbell.polls.source.dm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by Ján Korčák on 22.2.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollDTO {

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "idBodProgramu")
    private String agendaItemId;

    @JsonProperty(value = "route")
    private String pollRoute;

    @JsonProperty(value = "note")
    private String note;

    @JsonProperty(value = "numberOfMembers")
    private int voters;

    @JsonProperty(value = "absentMembers")
    private int absent;

    @JsonProperty(value = "votedFor")
    private int votedFor;

    @JsonProperty(value = "votedAgainst")
    private int votedAgainst;

    @JsonProperty(value = "abstain")
    private int abstain;

    @JsonProperty(value = "notVoted")
    private int notVoted;

    @JsonProperty(value = "children")
    private List<PollChoiceDTO> pollChoiceDTOs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAgendaItemId() {
        return agendaItemId;
    }

    public void setAgendaItemId(String agendaItemId) {
        this.agendaItemId = agendaItemId;
    }

    public String getPollRoute() {
        return pollRoute;
    }

    public void setPollRoute(String pollRoute) {
        this.pollRoute = pollRoute;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getVoters() {
        return voters;
    }

    public void setVoters(int voters) {
        this.voters = voters;
    }

    public int getAbsent() {
        return absent;
    }

    public void setAbsent(int absent) {
        this.absent = absent;
    }

    public int getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(int votedFor) {
        this.votedFor = votedFor;
    }

    public int getVotedAgainst() {
        return votedAgainst;
    }

    public void setVotedAgainst(int votedAgainst) {
        this.votedAgainst = votedAgainst;
    }

    public int getAbstain() {
        return abstain;
    }

    public void setAbstain(int abstain) {
        this.abstain = abstain;
    }

    public int getNotVoted() {
        return notVoted;
    }

    public void setNotVoted(int notVoted) {
        this.notVoted = notVoted;
    }

    public List<PollChoiceDTO> getPollChoiceDTOs() {
        return pollChoiceDTOs;
    }

    public void setPollChoiceDTOs(List<PollChoiceDTO> pollChoiceDTOs) {
        this.pollChoiceDTOs = pollChoiceDTOs;
    }

    @Override
    public String toString() {
        return "PollDTO{" +
                "name='" + name + '\'' +
                ", agendaItemId='" + agendaItemId + '\'' +
                ", pollRoute='" + pollRoute + '\'' +
                ", note='" + note + '\'' +
                ", voters=" + voters +
                ", absent=" + absent +
                ", votedFor=" + votedFor +
                ", votedAgainst=" + votedAgainst +
                ", abstain=" + abstain +
                ", notVoted=" + notVoted +
                ", pollChoiceDTOs=" + pollChoiceDTOs +
                '}';
    }
}
