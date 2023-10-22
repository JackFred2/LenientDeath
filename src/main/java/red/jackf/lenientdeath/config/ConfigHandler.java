package red.jackf.lenientdeath.config;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.DeserializationException;
import blue.endless.jankson.api.SyntaxError;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import red.jackf.lenientdeath.LenientDeath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static red.jackf.lenientdeath.config.LenientDeathJankson.JANKSON;

public class ConfigHandler {
    protected static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("lenientdeath.json5");
    private static final JsonGrammar GRAMMAR = JsonGrammar.builder()
            .bareSpecialNumerics(true)
            .printUnquotedKeys(true)
            .withComments(true)
            .build();
    private static final JsonGrammar GRAMMAR_NO_COMMENTS = JsonGrammar.builder()
            .bareSpecialNumerics(true)
            .printUnquotedKeys(true)
            .withComments(false)
            .build();
    protected static final Logger LOGGER = LenientDeath.getLogger("Config");

    ConfigHandler() {}

    private LenientDeathConfig instance = null;

    public void setup() {
        get();
    }

    public LenientDeathConfig get() {
        if (instance == null) load();
        return instance;
    }

    public void set(LenientDeathConfig newInstance) {
        LOGGER.debug("Loading preset");

        newInstance.verify();
        var old = instance;
        instance = newInstance;
        instance.onLoad(old);

        save();
    }

    protected void load() {
        LenientDeathConfig old = instance;

        if (Files.exists(PATH)) {
            LOGGER.debug("Loading config");

            try {
                JsonObject json = JANKSON.load(PATH.toFile());
                instance = JANKSON.fromJsonCarefully(json, LenientDeathConfig.class);

                instance.verify();

                // check for config update and save if new
                JsonElement copy = JANKSON.toJson(instance);
                // if during serialization there's missing keys,
                if (copy instanceof JsonObject copyObj && !copyObj.getDelta(json).isEmpty()) {
                    LOGGER.debug("Saving updated config");
                    save();
                }
            } catch (IOException ex) {
                LOGGER.error("IO error reading config", ex);
            } catch (SyntaxError ex) {
                LOGGER.error(ex.getMessage());
                LOGGER.error(ex.getLineMessage());
            } catch (DeserializationException ex) {
                LOGGER.error("Syntax error in config", ex);
            } finally {
                if (instance == null) {
                    LOGGER.error("Using default config temporarily");
                    instance = new LenientDeathConfig();
                }
            }
        } else {
            LOGGER.debug("Creating new config");
            instance = new LenientDeathConfig();
            this.save();
        }

        if (instance != old) instance.onLoad(old);
    }

    public void save() {
        LenientDeathConfig config = get();
        JsonElement json = JANKSON.toJson(config);
        try {
            LOGGER.debug("Saving config");
            Files.writeString(PATH, json.toJson(config.config.stripComments ? GRAMMAR_NO_COMMENTS : GRAMMAR));
        } catch (IOException ex) {
            LOGGER.error("Error saving config", ex);
        }
        ConfigChangeListener.INSTANCE.skipNext();
    }
}
