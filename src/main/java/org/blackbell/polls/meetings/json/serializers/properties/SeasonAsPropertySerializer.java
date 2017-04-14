package org.blackbell.polls.meetings.json.serializers.properties;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.meetings.model.Season;

import java.io.IOException;

/**
 * Created by Ján Korčák on 14.4.2017.
 * email: korcak@esten.sk
 */
public class SeasonAsPropertySerializer extends StdSerializer<Season> {
    public SeasonAsPropertySerializer() {
        this(null);
    }

    public SeasonAsPropertySerializer(Class<Season> t) {
        super(t);
    }

    @Override
    public void serialize(Season value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        gen.writeString(value.getName());
    }
}
