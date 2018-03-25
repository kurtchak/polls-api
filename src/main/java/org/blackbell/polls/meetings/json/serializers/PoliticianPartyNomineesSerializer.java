package org.blackbell.polls.meetings.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.meetings.model.PartyNominee;

import java.io.IOException;
import java.util.List;

/**
 * Created by Ján Korčák on 14.4.2017.
 * email: korcak@esten.sk
 */
public class PoliticianPartyNomineesSerializer extends StdSerializer<List<PartyNominee>> {

    public PoliticianPartyNomineesSerializer() {
        this(null);
    }

    public PoliticianPartyNomineesSerializer(Class<List<PartyNominee>> t) {
        super(t);
    }

    @Override
    public void serialize(List<PartyNominee> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        if (value != null) {
            jgen.writeStartArray();
            for (PartyNominee pn : value) {
                jgen.writeString(pn.getParty().getName());
            }
            jgen.writeEndArray();
        }
    }
}