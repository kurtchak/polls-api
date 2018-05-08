package org.blackbell.polls.domain.api.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.domain.model.Season;
import org.blackbell.polls.domain.model.Town;

import java.io.IOException;

/**
 * Created by kurtcha on 8.5.2018.
 */
public class TownPropertySerializer extends StdSerializer<Town> {
    public TownPropertySerializer() {
        this(null);
    }

    public TownPropertySerializer(Class<Town> t) {
        super(t);
    }

    @Override
    public void serialize(Town value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.getRef());
    }
}
