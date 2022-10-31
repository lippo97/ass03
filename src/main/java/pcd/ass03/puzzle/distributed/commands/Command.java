package pcd.ass03.puzzle.distributed.commands;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Initialize.class, name = "initialize"),
    @JsonSubTypes.Type(value = Click.class, name = "click")
})
public class Command {

    static class Serializer extends StdSerializer<Command> {

        Serializer() {
            super(Command.class);
        }

        @Override
        public void serializeWithType(Command command, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
            if (command instanceof Initialize) {
                final var initialize = (Initialize) command;
                gen.writeStartObject();
                gen.writeStringField("type", "initialize");
                gen.writeArrayFieldStart("positions");
                for (final var p : initialize.positions) {
                    gen.writeNumber(p);
                }
                gen.writeEndArray();
                gen.writeEndObject();
                return;
            }
            final var click = (Click) command;
            gen.writeStartObject();
            gen.writeStringField("type", "click");
            gen.writeNumberField("position", click.position);
            gen.writeEndObject();
        }

        @Override
        public void serialize(Command command, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (command instanceof Initialize) {
                final var initialize = (Initialize) command;
                gen.writeStartObject();
                gen.writeStringField("type", "initialize");
                gen.writeArrayFieldStart("positions");
                for (final var p : initialize.positions) {
                    gen.writeNumber(p);
                }
                gen.writeEndArray();
                gen.writeEndObject();
                return;
            }
            final var click = (Click) command;
            gen.writeStartObject();
            gen.writeStringField("type", "click");
            gen.writeNumberField("sourceId", click.sourceId);
            gen.writeNumberField("position", click.position);
            gen.writeEndObject();
        }
    }

    public static JsonSerializer<Command> serializer = new Serializer();
}
