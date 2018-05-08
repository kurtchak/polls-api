package org.blackbell.polls.controllers;

/**
 * Created by Ján Korčák on 21.4.2018.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Party;
import org.blackbell.polls.domain.model.relate.PartyNominee;
import org.blackbell.polls.domain.repositories.PartyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/{city}/{season}/parties")
public class PartiesController {
    private static final Logger log = LoggerFactory.getLogger(PartiesController.class);

    private PartyRepository partyRepository;

    public PartiesController(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @JsonView(value = Views.Parties.class)
    @RequestMapping({"/",""})
    public Collection<Party> parties(@PathVariable(value="city") String city,
                                     @PathVariable(value="season") String season) throws Exception {
        return partyRepository.getByTownAndSeasonAndInstitution(city, season);
    }

    @JsonView(value = Views.PartyNominees.class)
    @RequestMapping("/{ref}/members")
    public Collection<PartyNominee> partyNominees(@PathVariable(value="city") String city,
                                                  @PathVariable(value="season") String season,
                                                  @PathVariable(value="ref") String ref) throws Exception {
        return partyRepository.getPartyNomineesByTownAndSeason(city, season, ref);
    }
}
