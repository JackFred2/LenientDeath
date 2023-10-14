package red.jackf.lenientdeath.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import red.jackf.lenientdeath.LenientDeath;

import java.io.IOException;
import java.nio.file.*;

public class ConfigHandler {
    protected static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("lenientdeath.json5");
    private static final Jankson JANKSON = Jankson.builder().build();
    private static final JsonGrammar GRAMMAR = JsonGrammar.JANKSON;
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

    protected void load() {
        if (Files.exists(PATH)) {
            LOGGER.debug("Loading config");
            try {
                JsonObject json = JANKSON.load(PATH.toFile());
                instance = JANKSON.fromJson(json, LenientDeathConfig.class);

                // check for config update and save if new
                if (!JANKSON.toJson(instance).equals(json)) {
                    LOGGER.debug("Saving updated config");
                    save();
                }
            } catch (IOException ex) {
                LOGGER.error("IO error reading config, restoring default", ex);
                instance = new LenientDeathConfig();

                try {
                    Files.move(PATH, PATH.resolveSibling("lenientdeath.json5.corrupt"));
                } catch (IOException ex2) {
                    LOGGER.error("Couldn't move corrupt file!", ex2);
                }

                save();
            } catch (SyntaxError ex) {
                LOGGER.error("Syntax error in config, restoring default", ex);
                instance = new LenientDeathConfig();

                try {
                    Files.move(PATH, PATH.resolveSibling("lenientdeath.json5.corrupt"));
                } catch (IOException ex2) {
                    LOGGER.error("Couldn't move corrupt file!", ex2);
                }

                save();
            }
        } else {
            LOGGER.debug("Creating new config");
            instance = new LenientDeathConfig();
            this.save();
        }

        instance.onLoad();
    }

    public void save() {
        LenientDeathConfig config = get();
        JsonElement json = JANKSON.toJson(config);
        try {
            LOGGER.debug("Saving config");
            Files.writeString(PATH, json.toJson(GRAMMAR));
        } catch (IOException ex) {
            LOGGER.error("Error saving config", ex);
        }
    }
}
