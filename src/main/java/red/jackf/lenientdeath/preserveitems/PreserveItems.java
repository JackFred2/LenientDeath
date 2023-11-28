package red.jackf.lenientdeath.preserveitems;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import red.jackf.lenientdeath.LenientDeath;

public class PreserveItems {
    public static final PreserveItems INSTANCE = new PreserveItems();

    private PreserveItems() {}

    /**
     * Called at {@link ServerPlayer#restoreFrom}
     */
    public static void copyOldInventory(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        newPlayer.getInventory().replaceWith(oldPlayer.getInventory());
    }

    public static boolean shouldKeepOnDeath(Player player, ItemStack stack) {
        var config = LenientDeath.CONFIG.instance().preserveItemsOnDeath;
        if (config.enabled.test(player)) {
            var test = INSTANCE.shouldPreserve(player, stack, false);
            if (test != null) return test;
        }
        return false;
    }

    public void setup() {
        ManualAllowAndBlocklist.INSTANCE.setup();
        if (FabricLoader.getInstance().isModLoaded("trinkets")) TrinketsCompat.setup();
    }

    /**
     * Whether a given ItemStack should be kept within a player's inventory on death.
     *
     * @param player     Player to test against. If null, skips the random roll.
     * @param stack      ItemStack to check.
     * @param skipRandom Whether to use the randomizer check.
     * @return If the ItemStack should be kept in a player's inventory. <code>null</code> if no answer.
     */
    public @Nullable Boolean shouldPreserve(Player player, ItemStack stack, boolean skipRandom) {
        var nbtPreserveTest = NbtChecker.INSTANCE.shouldKeep(stack);
        if (nbtPreserveTest != null) return nbtPreserveTest;

        var configPreserveTest = ManualAllowAndBlocklist.INSTANCE.shouldKeep(stack);
        if (configPreserveTest != null) return configPreserveTest;

        var itemTypeTest = ItemTypeChecker.INSTANCE.shouldKeep(player, stack);
        if (itemTypeTest != null) return itemTypeTest;

        if (!skipRandom) return Randomizer.INSTANCE.shouldKeep(stack, player);

        return null;
    }
}
