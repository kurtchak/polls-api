package org.blackbell.polls.meetings.source.dm.dto;

/**
 * Created by Jano on 5. 3. 2018.
 */
public enum VoteChoiceEnum {
    DM_VOTE_FOR("Za"),
    DM_VOTE_AGAINST("Proti"),
    DM_NO_VOTE("Nehlasoval"),
    DM_ABSTAIN("Zdržal sa"),
    DM_ABSENT("Chýbal na hlasovaní");

    private final String name;

    VoteChoiceEnum(String name) {
        this.name = name;
    }
}
