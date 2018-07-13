package org.blackbell.polls.source.dm.dto;

/**
 * Created by Jano on 5. 3. 2018.
 */
public enum VoteChoiceEnum {
    DM_VOTE_FOR("za"),
    DM_VOTE_AGAINST("proti"),
    DM_NO_VOTE("nehlasoval"),
    DM_ABSTAIN("zdrzalSa"),
    DM_ABSENT("nepritomny");

    private final String name;

    VoteChoiceEnum(String name) {
        this.name = name;
    }

    public static VoteChoiceEnum fromString(String name) {
        switch (name) {
            case "za": return DM_VOTE_FOR;
            case "proti": return DM_VOTE_AGAINST;
            case "nehlasoval": return DM_NO_VOTE;
            case "zdrzalSa": return DM_ABSTAIN;
            case "nepritomny": return DM_ABSENT;
            default: return null;
        }
    }

}
