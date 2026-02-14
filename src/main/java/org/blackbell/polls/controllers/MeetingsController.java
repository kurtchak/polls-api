package org.blackbell.polls.controllers;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.common.Constants;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Meeting;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.MeetingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
public class MeetingsController {
    private static final Logger log = LoggerFactory.getLogger(MeetingsController.class);

    private MeetingRepository meetingRepository;

    public MeetingsController(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    @JsonView(value = Views.Meetings.class)
    @RequestMapping({"/{city}/{institution}/{season}/meetings",
                     "/{city}/{institution}/{season}/meetings/{dateFrom}/{dateTo}",
                     "/{city}/{institution}/meetings/{season}/{dateFrom}/{dateTo}"})
    public List<Meeting> meetings(@PathVariable(value="city") String city,
                                  @PathVariable(value="institution") String institution,
                                  @PathVariable(value="season") String season,
                                  @PathVariable(value="dateFrom", required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateFrom,
                                  @PathVariable(value="dateTo", required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateTo) throws Exception {
        return meetingRepository.getByTownAndInstitutionAndSeason(city, InstitutionType.fromRef(institution), season, dateFrom, dateTo);
    }

    @JsonView(value = Views.Meeting.class)
    @RequestMapping({"/meetings/{ref}",
                     "/{city}/{institution}/meeting/{ref}"})
    public Meeting meeting(@PathVariable(value="ref") String ref) throws Exception {
        return meetingRepository.getByRef(ref);
    }

    @JsonView(value = Views.Meetings.class)
    @RequestMapping("/meetings/failed")
    public List<Meeting> failedMeetings() {
        return meetingRepository.findFailedMeetings();
    }
}
