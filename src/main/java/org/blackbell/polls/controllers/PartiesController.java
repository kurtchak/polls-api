package org.blackbell.polls.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Party;
import org.blackbell.polls.domain.model.relate.PartyNominee;
import org.blackbell.polls.service.PartyService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/{city}/{season}/parties")
public class PartiesController {

    private final PartyService partyService;

    public PartiesController(PartyService partyService) {
        this.partyService = partyService;
    }

    @JsonView(value = Views.Parties.class)
    @RequestMapping({"/",""})
    public Collection<Party> parties(@PathVariable String city,
                                     @PathVariable String season) {
        return partyService.getParties(city, season);
    }

    @JsonView(value = Views.PartyNominees.class)
    @RequestMapping("/{ref}/members")
    public Collection<PartyNominee> partyNominees(@PathVariable String city,
                                                  @PathVariable String season,
                                                  @PathVariable String ref) {
        return partyService.getPartyNominees(city, season, ref);
    }
}
