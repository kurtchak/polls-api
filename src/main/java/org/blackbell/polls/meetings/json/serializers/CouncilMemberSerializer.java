package org.blackbell.polls.meetings.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.Club;
import org.blackbell.polls.meetings.model.CouncilMember;
import org.blackbell.polls.meetings.model.PartyNominee;

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
        jgen.writeStringField("ref", value.getRef());
        jgen.writeStringField("name", value.getPolitician().getName());
        jgen.writeStringField("title", value.getPolitician().getTitles());
        jgen.writeStringField("picture", value.getPolitician().getPicture());
        jgen.writeStringField("email", value.getPolitician().getEmail());
        jgen.writeStringField("otherFunctions", value.getOtherFunctions());
        jgen.writeStringField("position", value.getMemberType().name());
        //TODO:
//        if (value.getActualClubMember() != null) {
//            provider.findValueSerializer(Club.class).serialize(value.getActualClubMember().getClub(), jgen, provider);
//        }
//        new PoliticianPartyNomineesSerializer().serialize(value.getPartyNominees(), jgen, provider);
        jgen.writeEndObject();
    }
}