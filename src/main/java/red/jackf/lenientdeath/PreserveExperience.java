package red.jackf.lenientdeath;

import net.minecraft.server.level.ServerPlayer;
import red.jackf.lenientdeath.config.LenientDeathConfig;

public class PreserveExperience {
    private PreserveExperience() {}

    private static float getFactor() {
        return LenientDeathConfig.INSTANCE.get().preserveExperienceOnDeath.preservedPercentage / 100f;
    }

    public static void copyExperience(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        float factor = getFactor();
        int awarded = (int) (oldPlayer.totalExperience * factor);
        newPlayer.giveExperiencePoints(awarded);
    }

    public static int reduceXpOrbExperience(int originalAmount) {
        float factor = getFactor();
        float reverse = 1f - factor;
        return (int) (originalAmount * reverse);
    }
}
