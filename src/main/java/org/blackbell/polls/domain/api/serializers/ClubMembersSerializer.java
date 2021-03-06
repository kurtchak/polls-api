package org.blackbell.polls.domain.api.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.domain.model.CouncilMember;
import org.blackbell.polls.domain.model.relate.ClubMember;

import java.io.IOException;
import java.util.Set;

/**
 * Created by Ján Korčák on 14.4.2017.
 * email: korcak@esten.sk
 */
public class ClubMembersSerializer extends StdSerializer<Set<ClubMember>> {

    public ClubMembersSerializer() {
        this(null);
    }

    public ClubMembersSerializer(Class<Set<ClubMember>> t) {
        super(t);
    }

    @Override
    public void serialize(Set<ClubMember> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        if (value != null) {
            JsonSerializer<Object> memberSerializer = provider.findValueSerializer(CouncilMember.class);
            jgen.writeStartArray();
            for (ClubMember cm : value) {
                if (cm != null) {
                    memberSerializer.serialize(cm.getCouncilMember(), jgen, provider);
                }
            }
            jgen.writeEndArray();
        }
    }
}