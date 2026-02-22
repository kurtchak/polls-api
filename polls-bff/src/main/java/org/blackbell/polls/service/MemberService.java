package org.blackbell.polls.service;

import org.blackbell.polls.domain.model.CouncilMember;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.CouncilMemberRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class MemberService {

    private final CouncilMemberRepository councilMemberRepository;

    public MemberService(CouncilMemberRepository councilMemberRepository) {
        this.councilMemberRepository = councilMemberRepository;
    }

    public Collection<CouncilMember> getMembers(String city, String institution, String season) {
        return councilMemberRepository.getByTownAndSeasonAndInstitution(
                city, season, InstitutionType.fromRef(institution));
    }

    public CouncilMember getMemberDetail(String ref) {
        return councilMemberRepository.findByRef(ref);
    }

    public Collection<CouncilMember> getFreeMembers(String city, String season) {
        return councilMemberRepository.getFreeCouncilMembers(city, season);
    }
}
