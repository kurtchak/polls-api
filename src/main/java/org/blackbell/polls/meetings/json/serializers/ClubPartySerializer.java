package org.blackbell.polls.meetings.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.meetings.model.ClubParty;

import java.io.IOException;

/**
 * Created by Ján Korčák on 18.3.2018.
 * email: korcak@esten.sk
 */
public class ClubPartySerializer extends StdSerializer<ClubParty> {

    public ClubPartySerializer() {
        this(null);
    }

    public ClubPartySerializer(Class<ClubParty> t) {
        super(t);
    }

    @Override
    public void serialize(ClubParty value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        jgen.writeString(value.getParty().getName());
    }
}