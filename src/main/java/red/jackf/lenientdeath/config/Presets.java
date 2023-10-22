package red.jackf.lenientdeath.config;

import red.jackf.jackfredlib.api.base.Memoizer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Presets {
    public static final Supplier<Map<String, Supplier<LenientDeathConfig>>> PRESETS = Memoizer.of(Presets::generate);

    private static Map<String, Supplier<LenientDeathConfig>> generate() {
        Map<String, Supplier<LenientDeathConfig>> map = new HashMap<>();

        map.put("default", LenientDeathConfig::new);
        map.put("onlyRandom", Presets::makeOnlyRandom);
        map.put("onlyVisuals", Presets::makeOnlyVisuals);
        map.put("generous", Presets::makeGenerous);

        return map;
    }

    // extends item despawn time, and enables XP saving
    private static LenientDeathConfig makeGenerous() {
        var config = new LenientDeathConfig();

        config.extendedDeathItemLifetime.enabled = true;
        config.extendedDeathItemLifetime.deathDropItemLifetimeSeconds = 10 * 60;

        config.preserveExperienceOnDeath.enabled = LenientDeathConfig.PerPlayerEnabled.yes;

        return config;
    }

    // only uses the "show item glow feature"
    private static LenientDeathConfig makeOnlyVisuals() {
        var config = new LenientDeathConfig();

        config.preserveItemsOnDeath.byItemType.enabled = false;

        return config;
    }

    // only uses the randomizer feature for keeping drops
    private static LenientDeathConfig makeOnlyRandom() {
        var config = new LenientDeathConfig();

        config.preserveItemsOnDeath.byItemType.enabled = false;
        config.preserveItemsOnDeath.randomizer.enabled = true;

        return config;
    }
}
