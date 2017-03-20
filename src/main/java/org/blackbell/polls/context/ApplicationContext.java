package org.blackbell.polls.context;

import org.blackbell.polls.meetings.model.*;

import java.util.*;

/**
 * Created by Ján Korčák on 5.3.2017.
 * email: korcak@esten.sk
 */
public class ApplicationContext {
    private Map<String, Town> townsMap;
    private Map<String, List<Poll>> pollsMap;

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

    public Map<String, List<Poll>> getPollsMap() {
        return pollsMap;
    }

    public void setPollsMap(Map<String, List<Poll>> pollsMap) {
        this.pollsMap = pollsMap;
    }

    public void putPolls(String city, Collection<Poll> polls) {
        if (pollsMap == null) {
            pollsMap = new HashMap<>();
        }
//        if (!pollsMap.containsKey(city) || pollsMap.get(city) == null) {
//            pollsMap.put(city, new ArrayList<>());
//        }
        this.pollsMap.get(city).addAll(polls);
    }

//    public Meeting getMeeting(String city, String season, Integer order) {
//        if (townsMap == null) {
//            return null;
//        } else if (!townsMap.containsKey(city)) {
//            return null;
//        } else if (townsMap.get(city).getSeasons() == null) {
//            return null;
//        } else if (!townsMap.get(city).getSeasons().containsKey(season)) {
//            return null;
//        } else if (townsMap.get(city).getSeasons().get(season).getMeetings() == null) {
//            return null;
//        } else if (!townsMap.get(city).getSeasons().get(season).getMeetings().containsKey(order)) {
//            return null;
//        } else {
//            return townsMap.get(city).getSeasons().get(season).getMeetings().get(order);
//        }
//    }

//    public Collection<Meeting> getMeetings(String city, String season) {
//        if (townsMap == null) {
//            return null;
//        } else if (!townsMap.containsKey(city)) {
//            return null;
//        } else if (townsMap.get(city).getSeasons() == null) {
//            return null;
//        } else if (!townsMap.get(city).getSeasons().containsKey(season)) {
//            return null;
//        } else if (townsMap.get(city).getSeasons().get(season).getMeetings() == null) {
//            return null;
//        } else {
//            return townsMap.get(city).getSeasons().get(season).getMeetings().values();
//        }
//    }

//    public Collection<CouncilMember> getMembers(String city, String season) {
//        if (townsMap == null) {
//            return null;
//        } else if (!townsMap.containsKey(city)) {
//            return null;
//        } else if (townsMap.get(city).getSeasons() == null) {
//            return null;
//        } else if (!townsMap.get(city).getSeasons().containsKey(season)) {
//            return null;
//        } else if (townsMap.get(city).getSeasons().get(season).getMembers() == null) {
//            return null;
//        } else {
//            return townsMap.get(city).getSeasons().get(season).getMembers().values();
//        }
//    }

//    public Collection<MeetingAttachment> getAttachments(String city, String season, Integer order) {
//        Meeting meeting = getMeeting(city, season, order);
//        if (meeting != null && meeting.getAttachments() != null) {
//            return meeting.getAttachments().values();
//        }
//        return null;
//    }
//
//    public MeetingAttachment getMeetingAttachment(String city, String season, Integer order, Integer item) {
//        Meeting meeting = getMeeting(city, season, order);
//        if (meeting != null && meeting.getAttachments() != null && meeting.getAttachments().containsKey(item)) {
//            return meeting.getAttachments().get(item);
//        }
//        return null;
//    }

//    public List<Poll> getPolls(String city, String season) {
//        return getPolls(city);
//    }
//
//    public List<Poll> getPolls(String city) {
//        if (pollsMap == null) {
//            pollsMap = new HashMap<>();
//        }
//        if (pollsMap.isEmpty() || !pollsMap.containsKey(city)) {
//            List<Poll> polls = new ArrayList<>();
//            for (Meeting meeting : townsMap.get(city).getSeasons().get("2014-2018").getMeetings().values()) {
//                for (AgendaItem item : meeting.getAgendaItems()) {
//                    polls.addAll(item.getPolls().values());
//                }
//            }
//            pollsMap.put(city, polls);
//        }
//        return pollsMap.get(city);
//    }

//    public Poll getPoll(String city, String institution, String pollNumber) {
//        List<Poll> polls = getPolls(city);
//        if (polls != null) {
//            for (Poll poll : polls) {
//                if (pollNumber.equals(poll.getRef())) {
//                    return poll;
//                }
//            }
//        }
//        return null;
//    }
}
