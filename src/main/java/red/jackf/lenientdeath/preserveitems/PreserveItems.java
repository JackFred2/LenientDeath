package red.jackf.lenientdeath.preserveitems;

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
            var test = INSTANCE.shouldPreserve(player, stack);
            if (test != null) return test;
        }
        return false;
    }

    public void setup() {
        ManualAllowAndBlocklist.INSTANCE.setup();
    }

    /**
     * Whether a given ItemStack should be kept within a player's inventory on death.
     *
     * @param player   Player to test against. If null, skips the random roll.
     * @param stack    ItemStack to check.
     * @return If the ItemStack should be kept in a player's inventory.
     */
    public @Nullable Boolean shouldPreserve(@Nullable Player player, ItemStack stack) {
        var nbtPreserveTest = NbtChecker.INSTANCE.shouldKeep(stack);
        if (nbtPreserveTest != null) return nbtPreserveTest;

        var configPreserveTest = ManualAllowAndBlocklist.INSTANCE.shouldKeep(stack);
        if (configPreserveTest != null) return configPreserveTest;

        var itemTypeTest = ItemTypeChecker.INSTANCE.shouldKeep(stack);
        if (itemTypeTest != null) return itemTypeTest;

        if (player != null) return Randomizer.INSTANCE.shouldKeep(stack, player);

        return null;
    }
}
