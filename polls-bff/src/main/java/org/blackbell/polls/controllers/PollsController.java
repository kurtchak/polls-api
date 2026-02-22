package org.blackbell.polls.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Collection;
import java.util.Date;
import org.blackbell.polls.common.Constants;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Poll;
import org.blackbell.polls.service.PollService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PollsController {

    private final PollService pollService;

    public PollsController(PollService pollService) {
        this.pollService = pollService;
    }

    @JsonView(value = Views.Polls.class)
    @RequestMapping({"/{city}/{institution}/{season}/polls",
                     "/{city}/{institution}/{season}/polls/{dateFrom}/{dateTo}",
                     "/{city}/{institution}/polls/{season}/{dateFrom}/{dateTo}"})
    public Collection<Poll> polls(@PathVariable String city,
                                  @PathVariable String institution,
                                  @PathVariable String season,
                                  @PathVariable(required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateFrom,
                                  @PathVariable(required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateTo) {
        return pollService.getRelevantPolls(city, institution, season, dateFrom, dateTo);
    }

    @JsonView(value = Views.Poll.class)
    @RequestMapping({"/polls/{ref}",
                     "/{city}/{institution}/poll/{ref}"})
    public Poll poll(@PathVariable String ref) {
        return pollService.getPollDetail(ref);
    }

    @RequestMapping({"/{city}/{institution}/{season}/polls/{ref}/markAsIrrelevant","/polls/{ref}/markAsIrrelevant"})
    public void markAsIrrelevant(@PathVariable String ref) {
        pollService.markAsIrrelevant(ref);
    }

    @JsonView(value = Views.Polls.class)
    @RequestMapping({"/{city}/{institution}/{season}/polls/irrelevant",
                    "/{city}/{institution}/{season}/polls/irrelevant/{dateFrom}/{dateTo}",
                    "/{city}/{institution}/polls/irrelevant/{season}/{dateFrom}/{dateTo}"})
    public Collection<Poll> irrelevant(@PathVariable String city,
        @PathVariable String institution,
        @PathVariable String season,
        @PathVariable(required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateFrom,
        @PathVariable(required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateTo) {
        return pollService.getIrrelevantPolls(city, institution, season, dateFrom, dateTo);
    }
}
