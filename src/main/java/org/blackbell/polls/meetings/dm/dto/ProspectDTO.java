package org.blackbell.polls.meetings.dm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Ján Korčák on 4.3.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProspectDTO {

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "href")
    private String source;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;

    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

}
