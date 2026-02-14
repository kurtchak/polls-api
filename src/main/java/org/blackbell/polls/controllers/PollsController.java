package org.blackbell.polls.controllers;

/**
 * Created by Ján Korčák on 21.4.2018.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.blackbell.polls.common.Constants;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Poll;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.PollRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PollsController {
    private static final Logger log = LoggerFactory.getLogger(PollsController.class);

    private static final Pattern IRRELEVANT_AGENDA_PATTERN = Pattern.compile("((O|o)tvorenie)|((U|u)rčenie\\s*zapisovateľ(.){0,2})|(overovateľ(.){0,2}\\szápisnice)|((S|s)chválenie\\s*(návrhu)?\\s*programu)");

    private PollRepository pollRepository;

    public PollsController(PollRepository pollRepository) {
        this.pollRepository = pollRepository;
    }

    @JsonView(value = Views.Polls.class)
    @RequestMapping({"/{city}/{institution}/{season}/polls",
                     "/{city}/{institution}/{season}/polls/{dateFrom}/{dateTo}",
                     "/{city}/{institution}/polls/{season}/{dateFrom}/{dateTo}"})
    public Collection<Poll> polls(@PathVariable(value = "city") String city,
                                  @PathVariable(value = "institution") String institution,
                                  @PathVariable(value = "season") String season,
                                  @PathVariable(value = "dateFrom", required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateFrom,
                                  @PathVariable(value = "dateTo", required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateTo) throws Exception {
        List<Poll> polls = pollRepository.getByTownAndSeasonAndInstitution(city, season, InstitutionType.fromRef(institution), dateFrom, dateTo);
        return polls.stream()
            .filter(poll -> poll.getAgendaItem() == null || poll.getAgendaItem().getName() == null
                    || !IRRELEVANT_AGENDA_PATTERN.matcher(poll.getAgendaItem().getName()).find())
            .collect(Collectors.toList());
    }

    @JsonView(value = Views.Poll.class)
    @RequestMapping({"/polls/{ref}",
                     "/{city}/{institution}/poll/{ref}"})
    public Poll poll(@PathVariable(value="ref") String ref) throws Exception {
        return pollRepository.getByRef(ref);
    }

    @RequestMapping({"/{city}/{institution}/{season}/polls/{ref}/markAsIrrelevant","/polls/{ref}/markAsIrrelevant"})
    public void markAsIrrelevant(@PathVariable(value="ref") String ref) throws Exception {
        Poll poll = pollRepository.getByRef(ref);
        poll.setMarkedAsIrrelevant(true);
    }

    @JsonView(value = Views.Polls.class)
    @RequestMapping({"/{city}/{institution}/{season}/polls/irrelevant",
                    "/{city}/{institution}/{season}/polls/irrelevant/{dateFrom}/{dateTo}",
                    "/{city}/{institution}/polls/irrelevant/{season}/{dateFrom}/{dateTo}"})
    public Collection<Poll> irrelevant(@PathVariable(value = "city") String city,
        @PathVariable(value = "institution") String institution,
        @PathVariable(value = "season") String season,
        @PathVariable(value = "dateFrom", required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateFrom,
        @PathVariable(value = "dateTo", required = false) @DateTimeFormat(pattern = Constants.DATE_FORMAT_PATTERN) Date dateTo) throws Exception {
        List<Poll> polls = pollRepository.getByTownAndSeasonAndInstitution(city, season, InstitutionType.fromRef(institution), dateFrom, dateTo);
        return polls.stream()
            .filter(poll -> poll.getAgendaItem() != null && poll.getAgendaItem().getName() != null
                    && IRRELEVANT_AGENDA_PATTERN.matcher(poll.getAgendaItem().getName()).find())
            .collect(Collectors.toList());
    }


}
