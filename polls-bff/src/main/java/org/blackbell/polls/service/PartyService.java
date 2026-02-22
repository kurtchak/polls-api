package org.blackbell.polls.service;

import org.blackbell.polls.domain.model.Party;
import org.blackbell.polls.domain.model.relate.PartyNominee;
import org.blackbell.polls.domain.repositories.PartyRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class PartyService {

    private final PartyRepository partyRepository;

    public PartyService(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    public Collection<Party> getParties(String city, String season) {
        return partyRepository.getByTownAndSeasonAndInstitution(city, season);
    }

    public Collection<PartyNominee> getPartyNominees(String city, String season, String ref) {
        return partyRepository.getPartyNomineesByTownAndSeason(city, season, ref);
    }
}
