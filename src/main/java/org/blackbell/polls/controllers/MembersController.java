package org.blackbell.polls.controllers;

/**
 * Created by Ján Korčák on 21.4.2018.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.common.Constants;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.model.Club;
import org.blackbell.polls.model.CouncilMember;
import org.blackbell.polls.model.Meeting;
import org.blackbell.polls.model.Vote;
import org.blackbell.polls.model.common.BaseEntity;
import org.blackbell.polls.model.enums.InstitutionType;
import org.blackbell.polls.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Date;
import java.util.List;
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

    //TODO:mixed routes
    @JsonView(value = {Views.CouncilMembers.class, Views.Club.class})
    @RequestMapping("/{city}/{season}/clubs/free")
    public Collection<CouncilMember> freeMembers(@PathVariable(value="city") String city,
                                                 @PathVariable(value="season") String season) throws Exception {
        return councilMemberRepository.getFreeCouncilMembers(city, season);
    }
}
