package org.blackbell.polls.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.CouncilMember;
import org.blackbell.polls.service.MemberService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class MembersController {

    private final MemberService memberService;

    public MembersController(MemberService memberService) {
        this.memberService = memberService;
    }

    @JsonView(value = Views.CouncilMembers.class)
    @RequestMapping({"/{city}/{institution}/{season}/members",
                     "/{city}/{institution}/members/{season}"})
    public Collection<CouncilMember> members(@PathVariable String city,
                                             @PathVariable String institution,
                                             @PathVariable String season) {
        return memberService.getMembers(city, institution, season);
    }

    @JsonView(value = Views.CouncilMember.class)
    @RequestMapping({"/members/{ref}",
                     "/{city}/{institution}/member/{ref}"})
    public CouncilMember member(@PathVariable String ref) {
        return memberService.getMemberDetail(ref);
    }

    @JsonView(value = {Views.CouncilMembers.class, Views.Club.class})
    @RequestMapping("/{city}/{season}/clubs/free")
    public Collection<CouncilMember> freeMembers(@PathVariable String city,
                                                 @PathVariable String season) {
        return memberService.getFreeMembers(city, season);
    }
}
