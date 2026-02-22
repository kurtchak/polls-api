package org.blackbell.polls.source.dm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Ján Korčák on 10.3.2018.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeasonDTO {

    @JsonProperty(value = "VolebneObdobie")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Season{" +
                "name='" + name + '\'' +
                '}';
    }
}
