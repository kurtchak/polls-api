package org.blackbell.polls.meetings.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.meetings.model.CouncilMember;
import org.blackbell.polls.meetings.model.PartyNominee;
import org.blackbell.polls.meetings.model.Votes;
import org.blackbell.polls.meetings.model.vote.Vote;
import org.blackbell.polls.meetings.model.vote.VoteFor;

import java.io.IOException;
import java.util.List;

/**
 * Created by kurtcha on 24.3.2018.
 */
public class VoteListSerializer extends StdSerializer<List<Vote>> {

    public VoteListSerializer() {
        this(null);
    }

    public VoteListSerializer(Class<List<Vote>> t) {
        super(t);
    }

    @Override
    public void serialize(List<Vote> votes, JsonGenerator jgen, SerializerProvider provider) throws IOException {
//        jgen.writeString("votes..");
        jgen.writeStartObject();
        jgen.writeFieldName("members");
        jgen.writeStartArray(votes != null ? votes.size() : 0);
        int count = 0;
        JsonSerializer<Object> memberSerializer = provider.findValueSerializer(CouncilMember.class);
        if (votes != null) {
            for (Vote v : votes) {
//                System.out.println("vote: " + v);
//                String member = v.getCouncilMember().toString();
                //TODO: CHYBAJU DATA!!!
                if (v.getCouncilMember() != null) {
                    memberSerializer.serialize(v.getCouncilMember(), jgen, provider);
                }
//                break;
                count += 1;
            }
        }
        jgen.writeEndArray();
        jgen.writeNumberField("count", count);
        jgen.writeEndObject();
    }

}
