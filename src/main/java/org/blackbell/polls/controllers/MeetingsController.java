package org.blackbell.polls.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.common.Constants;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Meeting;
import org.blackbell.polls.service.MeetingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
public class MeetingsController {

    private final MeetingService meetingService;

    public MeetingsController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @JsonView(value = Views.Meetings.class)
    @RequestMapping({"/{city}/{institution}/{season}/meetings",
                     "/{city}/{institution}/{season}/meetings/{dateFrom}/{dateTo}",
                     "/{city}/{institution}/meetings/{season}/{dateFrom}/{dateTo}"})
    public List<Meeting> meetings(@PathVariable String city,
                                  @PathVariable String institution,
                                  @PathVariable String season,
                                  @PathVariable(required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateFrom,
                                  @PathVariable(required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateTo) {
        return meetingService.getMeetings(city, institution, season, dateFrom, dateTo);
    }

    @JsonView(value = Views.Meeting.class)
    @RequestMapping({"/meetings/{ref}",
                     "/{city}/{institution}/meeting/{ref}"})
    public Meeting meeting(@PathVariable String ref) {
        return meetingService.getMeetingDetail(ref);
    }

    @JsonView(value = Views.Meetings.class)
    @RequestMapping("/meetings/failed")
    public List<Meeting> failedMeetings() {
        return meetingService.getFailedMeetings();
    }
}
