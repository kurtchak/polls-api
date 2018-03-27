package org.blackbell.polls.meetings.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.meetings.model.ClubMember;

import java.io.IOException;
import java.util.Calendar;
import java.util.Set;

/**
 * Created by Ján Korčák on 25.3.2018.
 * email: korcak@esten.sk
 */
public class PoliticianClubSerializer extends StdSerializer<Set<ClubMember>> {

    public PoliticianClubSerializer() {
        this(null);
    }

    public PoliticianClubSerializer(Class<Set<ClubMember>> t) {
        super(t);
    }

    @Override
    public void serialize(Set<ClubMember> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        if (value != null) {
            for (ClubMember cm : value) {
                Calendar cal = Calendar.getInstance();
                String[] range = cm.getClub().getSeason().getName().split("-");
                if (Integer.valueOf(range[0]) <= cal.get(Calendar.YEAR)
                        && Integer.valueOf(range[1]) >= cal.get(Calendar.YEAR)) {
                    provider.findValueSerializer(ClubMember.class).serialize(cm, jgen, provider);
                    break;
                }
            }
        }
    }
}