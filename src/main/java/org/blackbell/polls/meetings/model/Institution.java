package org.blackbell.polls.meetings.model;

/**
 * Created by Ján Korčák on 2.4.2017.
 * email: korcak@esten.sk
 */
public enum Institution {
    ZASTUPITELSTVO,
    MESTSKA_RADA,
    KOMISIA, season;

    public static final String DM_MZ = "mz";
    public static final String DM_MR = "mr";
    public static final String DM_K = "k";

    public static Institution valueOfDM(String institution) {
        switch(institution) {
            case DM_MZ: return ZASTUPITELSTVO;
            case DM_MR: return MESTSKA_RADA;
            case DM_K: return KOMISIA;
            default:return ZASTUPITELSTVO;
        }
    }

    public String getDMValue() {
        switch(this) {
            case ZASTUPITELSTVO: return DM_MZ;
            case MESTSKA_RADA: return DM_MR;
            case KOMISIA: return DM_K;
            default:return DM_MZ;
        }
    }
}
