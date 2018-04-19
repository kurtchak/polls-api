package org.blackbell.polls.meetings.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.meetings.model.ClubMember;

import java.io.IOException;

/**
 * Created by Ján Korčák on 25.3.2018.
 * email: korcak@esten.sk
 */
public class PoliticianClubSerializer extends StdSerializer<ClubMember> {

    public PoliticianClubSerializer() {
        this(null);
    }

    public PoliticianClubSerializer(Class<ClubMember> t) {
        super(t);
    }

    @Override
    public void serialize(ClubMember value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        if (value != null) {
            jgen.writeStartObject();
            jgen.writeStringField("ref", value.getClub().getRef());
            jgen.writeStringField("name", value.getClub().getName());
            jgen.writeStringField("position", value.getClubFunction().name());
            jgen.writeEndObject();
        }
    }
}