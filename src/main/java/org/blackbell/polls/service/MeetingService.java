package org.blackbell.polls.service;

import org.blackbell.polls.domain.model.Meeting;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.MeetingRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;

    public MeetingService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    public List<Meeting> getMeetings(String city, String institution, String season,
                                     Date dateFrom, Date dateTo) {
        return meetingRepository.getByTownAndInstitutionAndSeason(
                city, InstitutionType.fromRef(institution), season, dateFrom, dateTo);
    }

    public Meeting getMeetingDetail(String ref) {
        return meetingRepository.getByRef(ref);
    }

    public List<Meeting> getFailedMeetings() {
        return meetingRepository.findFailedMeetings();
    }
}
