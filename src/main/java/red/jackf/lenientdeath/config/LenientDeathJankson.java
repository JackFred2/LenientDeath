package red.jackf.lenientdeath.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.DeserializationException;
import blue.endless.jankson.api.Marshaller;
import net.minecraft.resources.ResourceLocation;

public class LenientDeathJankson {
    protected static final Jankson JANKSON = Jankson.builder()
            .registerSerializer(ResourceLocation.class, LenientDeathJankson::serializeResLoc)
            .registerDeserializer(JsonPrimitive.class, ResourceLocation.class, LenientDeathJankson::deserializeResLoc)
            .build();

    private static ResourceLocation deserializeResLoc(JsonPrimitive jsonPrimitive, Marshaller marshaller) throws DeserializationException {
        if (jsonPrimitive.getValue() instanceof String s) {
            var parsed = ResourceLocation.tryParse(s);
            if (parsed == null) {
                throw new DeserializationException("Invalid Resource Location: " + s);
            } else {
                return parsed;
            }
        }
        throw new DeserializationException("Resource location must be a string in the form namespace:path");
    }

    private static JsonElement serializeResLoc(ResourceLocation resourceLocation, Marshaller marshaller) {
        return new JsonPrimitive(resourceLocation.toString());
    }
}