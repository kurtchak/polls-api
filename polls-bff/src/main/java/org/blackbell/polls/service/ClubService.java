package org.blackbell.polls.service;

import org.blackbell.polls.domain.model.Club;
import org.blackbell.polls.domain.repositories.ClubRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class ClubService {

    private final ClubRepository clubRepository;

    public ClubService(ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

    public Collection<Club> getClubs(String city, String season) {
        return clubRepository.getByTownAndSeason(city, season);
    }

    public Club getClubDetail(String ref) {
        return clubRepository.findByRef(ref);
    }
}
