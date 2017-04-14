package org.blackbell.polls.meetings.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.meetings.model.PartyNominee;

import java.io.IOException;

/**
 * Created by Ján Korčák on 14.4.2017.
 * email: korcak@esten.sk
 */
public class PartyNomineeSerializer extends StdSerializer<PartyNominee> {

    public PartyNomineeSerializer() {
        this(null);
    }

    public PartyNomineeSerializer(Class<PartyNominee> t) {
        super(t);
    }

    @Override
    public void serialize(PartyNominee value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        jgen.writeString(value.getParty().getName());
    }
}