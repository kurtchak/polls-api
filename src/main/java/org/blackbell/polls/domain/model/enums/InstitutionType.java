package org.blackbell.polls.domain.model.enums;

import org.blackbell.polls.common.Constants;

/**
 * Created by Ján Korčák on 2.4.2017.
 * email: korcak@esten.sk
 */
public enum InstitutionType {
    ZASTUPITELSTVO(Constants.ZASTUPITELSTVO),
    RADA(Constants.RADA),
    KOMISIA(Constants.KOMISIA);

    private String ref;

    InstitutionType(String ref) {
        this.ref = ref;
    }

    public static InstitutionType valueOfDM(String institution) {
        switch(institution) {
            case Constants.DM_ZASTUPITELSTVO: return ZASTUPITELSTVO;
            case Constants.DM_RADA: return RADA;
            case Constants.DM_KOMISIA: return KOMISIA;
            default:return ZASTUPITELSTVO;
        }
    }

    public String toDMValue() {
        switch(this) {
            case ZASTUPITELSTVO: return Constants.DM_ZASTUPITELSTVO;
            case RADA: return Constants.DM_RADA;
            case KOMISIA: return Constants.DM_KOMISIA;
            default:return Constants.DM_ZASTUPITELSTVO;
        }
    }

    public static InstitutionType fromRef(String ref) {
        switch (ref) {
            case Constants.ZASTUPITELSTVO:
            case Constants.DM_ZASTUPITELSTVO: return ZASTUPITELSTVO;
            case Constants.RADA:
            case Constants.DM_RADA: return RADA;
            case Constants.KOMISIA: return KOMISIA;
            default:return ZASTUPITELSTVO;
        }
    }

}
