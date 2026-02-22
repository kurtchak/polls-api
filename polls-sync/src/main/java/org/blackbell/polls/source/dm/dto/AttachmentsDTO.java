package org.blackbell.polls.source.dm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by Ján Korčák on 19.2.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachmentsDTO extends MeetingComponentDTO {

    @JsonProperty(value = "children")
    private List<AttachmentDTO> attachmentDTOs;

    public List<AttachmentDTO> getAttachmentDTOs() {
        return attachmentDTOs;
    }

    public void setAttachmentDTOs(List<AttachmentDTO> attachmentDTOs) {
        this.attachmentDTOs = attachmentDTOs;
    }

    @Override
    public String toString() {
        return "AttachmentsDTO{" +
                "attachmentDTOs=" + attachmentDTOs +
                '}';
    }
}
