package org.blackbell.polls.model.enums;

/**
 * Created by Ján Korčák on 2.4.2017.
 * email: korcak@esten.sk
 */
public enum InstitutionType {
    ZASTUPITELSTVO("mz"),
    RADA("rada"),
    KOMISIA("komisia");

    public static final String DM_MZ = "mz";
    public static final String DM_MR = "mr";
    public static final String DM_K = "k";

    private String ref;

    InstitutionType(String ref) {
        this.ref = ref;
    }

    public static InstitutionType valueOfDM(String institution) {
        switch(institution) {
            case DM_MZ: return ZASTUPITELSTVO;
            case DM_MR: return RADA;
            case DM_K: return KOMISIA;
            default:return ZASTUPITELSTVO;
        }
    }

    public String toDMValue() {
        switch(this) {
            case ZASTUPITELSTVO: return DM_MZ;
            case RADA: return DM_MR;
            case KOMISIA: return DM_K;
            default:return DM_MZ;
        }
    }

    public static InstitutionType fromRef(String ref) {
        switch (ref) {
            case "mz": return ZASTUPITELSTVO;
            case "rada": return RADA;
            case "komisia": return KOMISIA;
            default:return ZASTUPITELSTVO;
        }
    }

}
