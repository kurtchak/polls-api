package org.blackbell.polls.domain.api.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.domain.model.CouncilMember;
import org.blackbell.polls.domain.model.relate.ClubMember;
import org.blackbell.polls.domain.model.relate.PartyNominee;

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
        jgen.writeStringField("ref", value.getPolitician().getRef());
        jgen.writeStringField("name", value.getPolitician().getName());
        jgen.writeStringField("title", value.getPolitician().getTitles());
        jgen.writeStringField("picture", value.getPolitician().getPicture());
        jgen.writeStringField("email", value.getPolitician().getEmail());
        jgen.writeStringField("phone", value.getPolitician().getPhone());
        jgen.writeStringField("otherFunctions", value.getOtherFunctions());

        jgen.writeFieldName("nominee");
        jgen.writeStartArray();
        if (value.getPartyNominees() != null) {
            for (PartyNominee nominee : value.getPartyNominees()) {
                jgen.writeString(nominee.getParty().getName());
            }
        }
        jgen.writeEndArray();

        if (value.getActualClubMember() != null) {
            ClubMember clubMember = value.getActualClubMember();
            jgen.writeFieldName("club");
            jgen.writeStartObject();
            jgen.writeStringField("ref", clubMember.getClub().getRef());
            jgen.writeStringField("name", clubMember.getClub().getName());
            jgen.writeStringField("position", clubMember.getClubFunction().name());
//            provider.findValueSerializer(Club.class).serialize(value.getActualClubMember().getClub(), jgen, provider);
            jgen.writeEndObject();
        }
        jgen.writeEndObject();
    }
}