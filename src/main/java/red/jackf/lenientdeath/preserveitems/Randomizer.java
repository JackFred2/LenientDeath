package red.jackf.lenientdeath.preserveitems;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import red.jackf.lenientdeath.config.LenientDeathConfig;

public class Randomizer {
    private Randomizer() {}

    public static Randomizer INSTANCE = new Randomizer();

    /**
     * Get the chance that a player keeps an item, accounting for luck if needed.
     * @param player Player to test
     * @return Float value from 0 to 1 that an item should be kept.
     */
    public float getChanceToKeep(@Nullable Player player) {
        var config = LenientDeathConfig.INSTANCE.get().preserveItemsOnDeath.randomizer;
        if (!config.enabled) return 0;
        float chance = config.preservedPercentage / 100f;
        if (player == null) return chance;

        float luck = player.getLuck();
        float luckAddFactor = config.luckAdditiveFactor / 100f;
        float luckMultiFactor = config.luckMultiplierFactor;

        return Mth.clamp(chance * (1 + (luckMultiFactor * luck)) + (luckAddFactor * luck), 0, 1);
    }

    public @Nullable Boolean shouldKeep(ItemStack ignored, Player player) {
        var chance = getChanceToKeep(player);
        if (chance == 1) return true;
        if (chance > 0 && player.getRandom().nextFloat() < chance) return true;
        return null;
    }
}
