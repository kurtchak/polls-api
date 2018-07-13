package org.blackbell.polls.domain.api.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.blackbell.polls.domain.model.Institution;

import java.io.IOException;

/**
 * Created by kurtcha on 8.5.2018.
 */
public class InstitutionPropertySerializer extends StdSerializer<Institution> {
    public InstitutionPropertySerializer() {
        this(null);
    }

    public InstitutionPropertySerializer(Class<Institution> t) {
        super(t);
    }

    @Override
    public void serialize(Institution value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.getType().name());
    }
}
