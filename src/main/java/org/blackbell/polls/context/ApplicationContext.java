package org.blackbell.polls.context;

import org.blackbell.polls.meetings.model.*;

import java.util.*;

/**
 * Created by Ján Korčák on 5.3.2017.
 * email: korcak@esten.sk
 */
public class ApplicationContext {
    private Map<String, Town> townsMap;

    private static ApplicationContext instance;

    private ApplicationContext() {
        townsMap = new HashMap<>();
    }

    public static ApplicationContext getInstance() {
        if (instance == null) {
            instance = new ApplicationContext();
        }
        return instance;
    }

    public Map<String, Town> getTownsMap() {
        return townsMap;
    }

    public void setTownsMap(Map<String, Town> townsMap) {
        this.townsMap = townsMap;
    }

    public Meeting getMeeting(String city, String season, Integer order) {
        if (townsMap == null) {
            return null;
        } else if (!townsMap.containsKey(city)) {
            return null;
        } else if (townsMap.get(city).getSeasons() == null) {
            return null;
        } else if (!townsMap.get(city).getSeasons().containsKey(season)) {
            return null;
        } else if (townsMap.get(city).getSeasons().get(season).getMeetings() == null) {
            return null;
        } else if (!townsMap.get(city).getSeasons().get(season).getMeetings().containsKey(order)) {
            return null;
        } else {
            return townsMap.get(city).getSeasons().get(season).getMeetings().get(order);
        }
    }

    public Collection<Meeting> getMeetings(String city, String season) {
        if (townsMap == null) {
            return null;
        } else if (!townsMap.containsKey(city)) {
            return null;
        } else if (townsMap.get(city).getSeasons() == null) {
            return null;
        } else if (!townsMap.get(city).getSeasons().containsKey(season)) {
            return null;
        } else if (townsMap.get(city).getSeasons().get(season).getMeetings() == null) {
            return null;
        } else {
            return townsMap.get(city).getSeasons().get(season).getMeetings().values();
        }
    }

    public Collection<CouncilMember> getMembers(String city, String season) {
        if (townsMap == null) {
            return null;
        } else if (!townsMap.containsKey(city)) {
            return null;
        } else if (townsMap.get(city).getSeasons() == null) {
            return null;
        } else if (!townsMap.get(city).getSeasons().containsKey(season)) {
            return null;
        } else if (townsMap.get(city).getSeasons().get(season).getMembers() == null) {
            return null;
        } else {
            return townsMap.get(city).getSeasons().get(season).getMembers().values();
        }
    }

    public Agenda getAgenda(String city, String season, Integer order) {
        Meeting meeting = getMeeting(city, season, order);
        if (meeting != null) {
            return meeting.getAgenda();
        }
        return null;
    }

    public AgendaItem getAgendaItem(String city, String season, Integer order, Integer item) {
        Agenda agenda = getAgenda(city, season, order);
        if (agenda != null && agenda.getItems() != null && agenda.getItems().containsKey(item)) {
            return agenda.getItems().get(item);
        }
        return null;
    }

    public Collection<MeetingAttachment> getAttachments(String city, String season, Integer order) {
        Meeting meeting = getMeeting(city, season, order);
        if (meeting != null && meeting.getAttachments() != null) {
            return meeting.getAttachments().values();
        }
        return null;
    }

    public MeetingAttachment getMeetingAttachment(String city, String season, Integer order, Integer item) {
        Meeting meeting = getMeeting(city, season, order);
        if (meeting != null && meeting.getAttachments() != null && meeting.getAttachments().containsKey(item)) {
            return meeting.getAttachments().get(item);
        }
        return null;
    }
}
