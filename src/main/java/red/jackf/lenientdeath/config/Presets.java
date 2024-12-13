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
        map.put("disabled", Presets::makeDisabled);
        map.put("onlyRandom", Presets::makeOnlyRandom);
        map.put("onlyVisuals", Presets::makeOnlyVisuals);
        map.put("generous", Presets::makeGenerous);

        return map;
    }

    // nothing enabled
    private static LenientDeathConfig makeDisabled() {
        var config = new LenientDeathConfig();

        config.preserveItemsOnDeath.enabled = LenientDeathConfig.PerPlayerEnabled.no;

        config.droppedItemGlow.enabled = false;

        config.itemResilience.voidRecovery.mode = LenientDeathConfig.ItemResilience.VoidRecovery.Mode.disabled;
        config.itemResilience.lavaRecovery.mode = LenientDeathConfig.ItemResilience.LavaRecovery.Mode.disabled;

        config.deathCoordinates.sendToServerLog = false;
        config.deathCoordinates.sendToDeadPlayer = false;

        return config;
    }

    // extends item despawn time, enables XP saving, makes items immune to explosions and fire
    private static LenientDeathConfig makeGenerous() {
        var config = new LenientDeathConfig();

        config.extendedDeathItemLifetime.enabled = true;
        config.extendedDeathItemLifetime.deathDropItemLifetimeSeconds = 10 * 60;

        config.preserveExperienceOnDeath.enabled = LenientDeathConfig.PerPlayerEnabled.yes;

        config.itemResilience.allDeathItemsAreExplosionProof = true;
        config.itemResilience.allDeathItemsAreFireProof = true;

        return config;
    }

    // only uses the "show item glow feature"
    private static LenientDeathConfig makeOnlyVisuals() {
        var config = new LenientDeathConfig();

        config.preserveItemsOnDeath.byItemType.enabled = false;

        config.itemResilience.voidRecovery.mode = LenientDeathConfig.ItemResilience.VoidRecovery.Mode.disabled;
        config.itemResilience.lavaRecovery.mode = LenientDeathConfig.ItemResilience.LavaRecovery.Mode.disabled;

        return config;
    }

    // only uses the randomizer feature for keeping drops
    private static LenientDeathConfig makeOnlyRandom() {
        var config = new LenientDeathConfig();

        config.preserveItemsOnDeath.byItemType.enabled = false;
        config.preserveItemsOnDeath.randomizer.enabled = true;

        config.itemResilience.voidRecovery.mode = LenientDeathConfig.ItemResilience.VoidRecovery.Mode.disabled;
        config.itemResilience.lavaRecovery.mode = LenientDeathConfig.ItemResilience.LavaRecovery.Mode.disabled;

        return config;
    }
}
