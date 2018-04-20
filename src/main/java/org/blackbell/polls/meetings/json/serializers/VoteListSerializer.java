package org.blackbell.polls.meetings.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.meetings.model.CouncilMember;
import org.blackbell.polls.meetings.model.enums.VoteChoice;
import org.blackbell.polls.meetings.model.Vote;

import java.io.IOException;
import java.util.*;

/**
 * Created by kurtcha on 24.3.2018.
 */
public class VoteListSerializer extends StdSerializer<Set<Vote>> {

    public VoteListSerializer() {
        this(null);
    }

    public VoteListSerializer(Class<Set<Vote>> t) {
        super(t);
    }

    @Override
    public void serialize(Set<Vote> votes, JsonGenerator jgen, SerializerProvider provider) throws IOException {
//        jgen.writeString("votes..");
        jgen.writeStartObject();
//        jgen.writeFieldName("members");
//        jgen.writeStartArray(votes != null ? votes.size() : 0);
        int count = 0;
        JsonSerializer<Object> memberSerializer = provider.findValueSerializer(CouncilMember.class);
        if (votes != null) {
            Map<VoteChoice, List<CouncilMember>> votersMap = new HashMap<>();
            for (VoteChoice voteChoice : VoteChoice.values()) {
                votersMap.put(voteChoice, new ArrayList<>());
            }
            for (Vote v : votes) {
                votersMap.get(v.getVoted()).add(v.getCouncilMember());
            }
            for (VoteChoice voteChoice : VoteChoice.values()) {
                String label = "";
                switch (voteChoice) {
                    case VOTED_FOR: label = "for"; break;
                    case VOTED_AGAINST: label = "against"; break;
                    case NOT_VOTED: label = "not"; break;
                    case ABSTAIN: label = "abstain"; break;
                    case ABSENT: label = "absent"; break;
                }
                jgen.writeFieldName(label);
                jgen.writeStartObject();
                jgen.writeFieldName("voters");
                jgen.writeStartArray(votersMap.get(voteChoice).size());
                for (CouncilMember cm : votersMap.get(voteChoice)) {
//                System.out.println("vote: " + v);
//                String member = v.getCouncilMember().toString();
                    //TODO: CHYBAJU DATA!!!
                    if (cm != null) {
                        memberSerializer.serialize(cm, jgen, provider);
                    }
//                break;
//                    count += 1;
                }
                jgen.writeEndArray();
                jgen.writeNumberField("count", votersMap.get(voteChoice).size());
                jgen.writeEndObject();
            }
        }
        jgen.writeEndObject();
    }

}
