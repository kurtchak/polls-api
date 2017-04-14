package org.blackbell.polls.meetings.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.meetings.model.CouncilMember;

import java.io.IOException;

/**
 * Created by Ján Korčák on 14.4.2017.
 * email: korcak@esten.sk
 */
public class CouncilMemberSerializer extends StdSerializer<CouncilMember> {

    public CouncilMemberSerializer() {
        this(null);
    }

    public CouncilMemberSerializer(Class<CouncilMember> t) {
        super(t);
    }

    @Override
    public void serialize(
            CouncilMember value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jgen.writeStartObject();
        jgen.writeStringField("season", value.getSeason().getName());
        jgen.writeEndObject();
    }
}