package red.jackf.lenientdeath.preserveitems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import red.jackf.lenientdeath.config.LenientDeathConfig;

public class Randomizer {
    private Randomizer() {}

    public static Randomizer INSTANCE = new Randomizer();

    public @Nullable Boolean shouldKeep(ItemStack ignored, Player player) {
        var config = LenientDeathConfig.INSTANCE.get().preserveItemsOnDeath.randomizer;
        if (!config.enabled) return null;
        var factor = config.preservedPercentage / 100f;
        if (player.getRandom().nextFloat() < factor) return true;
        return null;
    }
}
