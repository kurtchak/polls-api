package org.blackbell.polls.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.common.NamedEntity;
import org.blackbell.polls.source.Source;

import jakarta.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Meeting extends NamedEntity {

    private String extId;

    @JsonIgnore
    @ManyToOne @JoinColumn(name = "season_id")
    private Season season;

    @JsonIgnore
    @ManyToOne @JoinColumn(name = "town_id")
    private Town town;

    @JsonIgnore
    @ManyToOne @JoinColumn(name = "institution_id")
    private Institution institution;

    @JsonView(value = {Views.Meetings.class, Views.Meeting.class, Views.Poll.class, Views.Polls.class, Views.Votes.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @JsonFormat(pattern = "dd.MM.yyyy")
    @Temporal(TemporalType.DATE)
    private Date date;

    @JsonView(value = {Views.Meeting.class})
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL)
    private Set<AgendaItem> agendaItems;

    @JsonView(value = {Views.Meeting.class})
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL)
    private Set<MeetingAttachment> attachments;

    @Column(length = 1000)
    private String syncError;

    @Column(columnDefinition = "boolean default false")
    private boolean syncComplete;

    @Column(columnDefinition = "integer default 0")
    private int syncRetryCount;

    @Enumerated(EnumType.STRING)
    private Source dataSource;

    @JsonView(value = {Views.Meetings.class, Views.Poll.class, Views.Polls.class, Views.Votes.class, Views.AgendaItem.class})
    public String getRef() {
        return ref;
    }

    @JsonView(value = {Views.Meetings.class, Views.Meeting.class, Views.Poll.class, Views.Polls.class, Views.Votes.class, Views.AgendaItem.class})
    public String getName() {
        return name;
    }

    @JsonIgnore
    public Season getSeason() {
        return season;
    }

    @JsonView(value = {Views.Meetings.class, Views.Meeting.class, Views.Poll.class, Views.AgendaItem.class})
    @JsonProperty("season")
    public String getSeasonName() {
        return season != null ? season.getName() : null;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    @JsonIgnore
    public Town getTown() {
        return town;
    }

    @JsonView(value = {Views.Meeting.class, Views.Poll.class, Views.AgendaItem.class})
    @JsonProperty("town")
    public String getTownRef() {
        return town != null ? town.getRef() : null;
    }

    public void setTown(Town town) {
        this.town = town;
    }

    @JsonIgnore
    public Institution getInstitution() {
        return institution;
    }

    @JsonView(value = {Views.Meeting.class, Views.Poll.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @JsonProperty("institution")
    public String getInstitutionType() {
        return institution != null ? institution.getType().name() : null;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Set<AgendaItem> getAgendaItems() {
        return agendaItems;
    }

    public void setAgendaItems(Set<AgendaItem> agendaItems) {
        this.agendaItems = agendaItems;
    }

    public Set<MeetingAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Set<MeetingAttachment> attachments) {
        this.attachments = attachments;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    public String getExtId() {
        return extId;
    }

    public String getSyncError() {
        return syncError;
    }

    public void setSyncError(String syncError) {
        this.syncError = syncError;
    }

    public boolean isSyncComplete() {
        return syncComplete;
    }

    public void setSyncComplete(boolean syncComplete) {
        this.syncComplete = syncComplete;
    }

    public Source getDataSource() {
        return dataSource;
    }

    public void setDataSource(Source dataSource) {
        this.dataSource = dataSource;
    }

    public int getSyncRetryCount() {
        return syncRetryCount;
    }

    public void setSyncRetryCount(int syncRetryCount) {
        this.syncRetryCount = syncRetryCount;
    }

    public void addAgendaItem(AgendaItem agendaItem) {
        if (agendaItems == null) {
            agendaItems = new HashSet<>();
        }
        agendaItem.setMeeting(this);
        agendaItems.add(agendaItem);
    }

    public void addAttachment(MeetingAttachment attachment) {
        if (attachments == null) {
            attachments = new HashSet<>();
        }
        attachment.setMeeting(this);
        attachments.add(attachment);
    }

    public boolean hasPolls() {
        return agendaItems != null && agendaItems.stream()
                .anyMatch(ai -> ai.getPolls() != null && !ai.getPolls().isEmpty());
    }

    public boolean hasVotes() {
        return agendaItems != null && agendaItems.stream()
                .filter(ai -> ai.getPolls() != null)
                .flatMap(ai -> ai.getPolls().stream())
                .anyMatch(p -> p.getVotes() != null && !p.getVotes().isEmpty());
    }

    public boolean hasUnmatchedVotes() {
        return agendaItems != null && agendaItems.stream()
                .filter(ai -> ai.getPolls() != null)
                .flatMap(ai -> ai.getPolls().stream())
                .filter(p -> p.getVotes() != null)
                .flatMap(p -> p.getVotes().stream())
                .anyMatch(v -> v.getCouncilMember() == null);
    }

    /**
     * A meeting is complete if:
     * - No sync error
     * - Has agenda items (empty agenda = failed sync)
     * - Has polls (meeting without votes is not complete)
     * - All votes are matched to council members
     */
    public boolean isComplete() {
        if (syncError != null) return false;
        if (agendaItems == null || agendaItems.isEmpty()) return false;
        if (!hasPolls()) return false;
        return !hasUnmatchedVotes();
    }

    @JsonIgnore
    public String getIncompleteReason() {
        if (syncError != null) return "sync error: " + syncError;
        if (agendaItems == null || agendaItems.isEmpty()) return "no agenda items";
        if (!hasPolls()) return "no polls";
        if (hasUnmatchedVotes()) return "unmatched votes";
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Meeting)) return false;

        Meeting meeting = (Meeting) o;

        if (!Objects.equals(getSeason(), meeting.getSeason())) return false;
        if (!Objects.equals(getTown(), meeting.getTown())) return false;
        if (!Objects.equals(getInstitution(), meeting.getInstitution())) return false;
        return Objects.equals(getDate(), meeting.getDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSeason(), getTown(), getInstitution(), getDate());
    }

    @Override
    public String toString() {
        return "Meeting{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", name='" + name + '\'' +
                ", season=" + season +
                ", town=" + town +
                ", institution=" + institution +
                ", date=" + date +
                ", agendaItems=" + agendaItems +
                ", attachments=" + attachments +
                ", extId='" + extId + '\'' +
                ", agendaItems count = '" + (getAgendaItems() != null ? getAgendaItems().size() : 0) + '\'' +
                '}';
    }
}
