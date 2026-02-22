package org.blackbell.polls.service;

import org.blackbell.polls.domain.model.Poll;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.PollRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PollService {

    private final Pattern irrelevantAgendaPattern;
    private final PollRepository pollRepository;

    public PollService(PollRepository pollRepository,
                       @Value("${polls.irrelevant-agenda-pattern}") String irrelevantAgendaPattern) {
        this.pollRepository = pollRepository;
        this.irrelevantAgendaPattern = Pattern.compile(irrelevantAgendaPattern);
    }

    public Collection<Poll> getRelevantPolls(String city, String institution, String season,
                                              Date dateFrom, Date dateTo) {
        List<Poll> polls = pollRepository.getByTownAndSeasonAndInstitution(
                city, season, InstitutionType.fromRef(institution), dateFrom, dateTo);
        return polls.stream()
                .filter(poll -> poll.getAgendaItem() == null || poll.getAgendaItem().getName() == null
                        || !irrelevantAgendaPattern.matcher(poll.getAgendaItem().getName()).find())
                .collect(Collectors.toList());
    }

    public Collection<Poll> getIrrelevantPolls(String city, String institution, String season,
                                                Date dateFrom, Date dateTo) {
        List<Poll> polls = pollRepository.getByTownAndSeasonAndInstitution(
                city, season, InstitutionType.fromRef(institution), dateFrom, dateTo);
        return polls.stream()
                .filter(poll -> poll.getAgendaItem() != null && poll.getAgendaItem().getName() != null
                        && irrelevantAgendaPattern.matcher(poll.getAgendaItem().getName()).find())
                .collect(Collectors.toList());
    }

    public Poll getPollDetail(String ref) {
        return pollRepository.getByRef(ref);
    }

    @Transactional
    public void markAsIrrelevant(String ref) {
        Poll poll = pollRepository.getByRef(ref);
        if (poll != null) {
            poll.setMarkedAsIrrelevant(true);
        }
    }
}
