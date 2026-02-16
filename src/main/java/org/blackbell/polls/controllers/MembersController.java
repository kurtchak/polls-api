package org.blackbell.polls.controllers;

/**
 * Created by Ján Korčák on 21.4.2018.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.CouncilMember;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.CouncilMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class MembersController {
    private static final Logger log = LoggerFactory.getLogger(MembersController.class);

    private CouncilMemberRepository councilMemberRepository;

    public MembersController(CouncilMemberRepository councilMemberRepository) {
        this.councilMemberRepository = councilMemberRepository;
    }

    @JsonView(value = Views.CouncilMembers.class)
    @RequestMapping({"/{city}/{institution}/{season}/members",
                     "/{city}/{institution}/members/{season}"})
    public Collection<CouncilMember> members(@PathVariable(value="city") String city,
                                             @PathVariable(value="institution") String institution,
                                             @PathVariable(value="season") String season) throws Exception {
        return councilMemberRepository.getByTownAndSeasonAndInstitution(city, season, InstitutionType.fromRef(institution));
    }

    @JsonView(value = Views.CouncilMember.class)
    @RequestMapping({"/members/{ref}",
                     "/{city}/{institution}/member/{ref}"})
    public CouncilMember member(@PathVariable(value="ref") String ref) throws Exception {
        return councilMemberRepository.findByRef(ref);
    }

    @JsonView(value = {Views.CouncilMembers.class, Views.Club.class})
    @RequestMapping("/{city}/{season}/clubs/free")
    public Collection<CouncilMember> freeMembers(@PathVariable(value="city") String city,
                                                 @PathVariable(value="season") String season) throws Exception {
        return councilMemberRepository.getFreeCouncilMembers(city, season);
    }
}
