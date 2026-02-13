package org.blackbell.polls.source.bratislava.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ArcGisVoteRecord {

    @JsonProperty("Volebné_obdobie")
    private int electoralPeriod;

    @JsonProperty("Zasadnutie")
    private String sessionName;

    @JsonProperty("Poradie_hlasovania")
    private int pollSequence;

    @JsonProperty("Bod")
    private String agendaItemName;

    @JsonProperty("Dátum")
    private String date;

    @JsonProperty("Čas")
    private String time;

    @JsonProperty("ID")
    private int memberId;

    @JsonProperty("Karta")
    private int cardNumber;

    @JsonProperty("Meno")
    private String firstName;

    @JsonProperty("Priezvisko")
    private String lastName;

    @JsonProperty("Poslanecký_klub")
    private String club;

    @JsonProperty("Hlasovanie")
    private String vote;

    @JsonProperty("Poradovník")
    private int ordinal;

    @JsonProperty("ObjectId")
    private int objectId;

    public int getElectoralPeriod() { return electoralPeriod; }
    public String getSessionName() { return sessionName; }
    public int getPollSequence() { return pollSequence; }
    public String getAgendaItemName() { return agendaItemName; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public int getMemberId() { return memberId; }
    public int getCardNumber() { return cardNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getClub() { return club; }
    public String getVote() { return vote; }
    public int getOrdinal() { return ordinal; }
    public int getObjectId() { return objectId; }
}
