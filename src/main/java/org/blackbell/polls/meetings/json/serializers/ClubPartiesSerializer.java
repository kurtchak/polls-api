package org.blackbell.polls.meetings.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.meetings.model.relate.ClubParty;

import java.io.IOException;
import java.util.Set;

/**
 * Created by Ján Korčák on 14.4.2017.
 * email: korcak@esten.sk
 */
public class ClubPartiesSerializer extends StdSerializer<Set<ClubParty>> {

    public ClubPartiesSerializer() {
        this(null);
    }

    public ClubPartiesSerializer(Class<Set<ClubParty>> t) {
        super(t);
    }

    @Override
    public void serialize(Set<ClubParty> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        if (value != null) {
            jgen.writeStartArray();
            for (ClubParty cp : value) {
                jgen.writeString(cp.getParty().getName());
            }
            jgen.writeEndArray();
        }
    }
}