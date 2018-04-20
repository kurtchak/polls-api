package org.blackbell.polls.meetings.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.meetings.model.PartyNominee;

import java.io.IOException;
import java.util.Set;

/**
 * Created by Ján Korčák on 14.4.2017.
 * email: korcak@esten.sk
 */
public class PoliticianPartyNomineesSerializer extends StdSerializer<Set<PartyNominee>> {

    public PoliticianPartyNomineesSerializer() {
        this(null);
    }

    public PoliticianPartyNomineesSerializer(Class<Set<PartyNominee>> t) {
        super(t);
    }

    @Override
    public void serialize(Set<PartyNominee> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException {
        jgen.writeStartArray();
        for (PartyNominee pn : value) {
            jgen.writeString(pn.getParty().getName());
        }
        jgen.writeEndArray();
    }
}