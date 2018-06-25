package org.blackbell.polls.domain.api.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.domain.model.CouncilMember;

import java.io.IOException;

/**
 * Created by Ján Korčák on 25.3.2018.
 * email: korcak@esten.sk
 */
public class ActualCouncilMemberSerializer extends StdSerializer<CouncilMember> {

    public ActualCouncilMemberSerializer() {
        this(null);
    }

    public ActualCouncilMemberSerializer(Class<CouncilMember> t) {
        super(t);
    }

    @Override
    public void serialize(CouncilMember value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        if (value != null) {
            jgen.writeString(value.getActualClubMember().getClub().getName());
        }
    }
}