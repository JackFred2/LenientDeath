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
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigHandler {
    public static ConfigHandler INSTANCE = new ConfigHandler();

    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("lenientdeath.json5");
    private static final Jankson JANKSON = Jankson.builder().build();
    private static final JsonGrammar GRAMMAR = JsonGrammar.JANKSON;
    private static final Logger LOGGER = LenientDeath.getLogger("Config");

    private ConfigHandler() {}

    private LenientDeathConfig instance = null;

    public LenientDeathConfig get() {
        if (instance == null) load();
        return instance;
    }

    private void load() {
        if (Files.exists(PATH)) {
            LOGGER.debug("Loading config");
            try {
                JsonObject json = JANKSON.load(PATH.toFile());
                instance = JANKSON.fromJson(json, LenientDeathConfig.class);
            } catch (IOException ex) {
                LOGGER.error("IO error reading config, restoring default", ex);
                instance = new LenientDeathConfig();
            } catch (SyntaxError ex) {
                LOGGER.error("Syntax error in config, restoring default", ex);
                instance = new LenientDeathConfig();
            }
        } else {
            LOGGER.debug("Creating new config");
            instance = new LenientDeathConfig();
        }

        this.save();
    }

    public void save() {
        LenientDeathConfig config = get();
        JsonElement json = JANKSON.toJson(config);
        try {
            Files.writeString(PATH, json.toJson(GRAMMAR));
        } catch (IOException ex) {
            LOGGER.error("Error saving config", ex);
        }
    }
}
