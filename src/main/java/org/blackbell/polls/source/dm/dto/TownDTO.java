package org.blackbell.polls.source.dm.dto;

/**
 * Created by kurtcha on 25.2.2018.
 */
public class TownDTO {
    private int organizaciaId = 279;
    private long obecId = 524140;
    private String nazov = "Mesto Prešov";
    private String nazovObec = "Prešov";
    private int typ = 1;
    private long kod = 524140000;
    private String hashTag = "mesto-presov";
    private String hashTagObec = "mesto-presov";

    public int getOrganizaciaId() {
        return organizaciaId;
    }

    public void setOrganizaciaId(int organizaciaId) {
        this.organizaciaId = organizaciaId;
    }

    public long getObecId() {
        return obecId;
    }

    public void setObecId(long obecId) {
        this.obecId = obecId;
    }

    public String getNazov() {
        return nazov;
    }

    public void setNazov(String nazov) {
        this.nazov = nazov;
    }

    public String getNazovObec() {
        return nazovObec;
    }

    public void setNazovObec(String nazovObec) {
        this.nazovObec = nazovObec;
    }

    public int getTyp() {
        return typ;
    }

    public void setTyp(int typ) {
        this.typ = typ;
    }

    public long getKod() {
        return kod;
    }

    public void setKod(long kod) {
        this.kod = kod;
    }

    public String getHashTag() {
        return hashTag;
    }

    public void setHashTag(String hashTag) {
        this.hashTag = hashTag;
    }

    public String getHashTagObec() {
        return hashTagObec;
    }

    public void setHashTagObec(String hashTagObec) {
        this.hashTagObec = hashTagObec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TownDTO townDTO = (TownDTO) o;

        return obecId == townDTO.obecId;

    }

    @Override
    public int hashCode() {
        return (int) (obecId ^ (obecId >>> 32));
    }
}
