package red.jackf.lenientdeath.preserveitems;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import red.jackf.lenientdeath.config.LenientDeathConfig;

public class PreserveItems {
    public static final PreserveItems INSTANCE = new PreserveItems();
    private PreserveItems() {}

    /**
     * Called at {@link ServerPlayer#restoreFrom}
     */
    public static void copyOldInventory(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean keepEverything) {
        // dont do anything if existing checks happened
        if (keepEverything) return;
        //noinspection resource
        if (newPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || oldPlayer.isSpectator()) return;
        newPlayer.getInventory().replaceWith(oldPlayer.getInventory());
    }

    public static boolean shouldKeepOnDeath(Player player, ItemStack stack) {
        var config = LenientDeathConfig.INSTANCE.get().preserveItemsOnDeath;
        if (config.enabled.test(player)) return INSTANCE.shouldPreserve(stack);
        return false;
    }

    public void setup() {
        ManualAllowAndBlocklist.INSTANCE.setup();
    }

    /**
     * Whether a given ItemStack should be kept within a player's inventory on death.
     * @param stack ItemStack to check
     * @return If the ItemStack should be kept in a player's inventory.
     */
    public boolean shouldPreserve(ItemStack stack) {
        var nbtPreserveTest = NbtChecker.INSTANCE.shouldKeep(stack);
        if (nbtPreserveTest != null) return nbtPreserveTest;

        var configPreserveTest = ManualAllowAndBlocklist.INSTANCE.shouldKeep(stack);
        if (configPreserveTest != null) return configPreserveTest;

        var itemTypeTest = ItemTypeChecker.INSTANCE.shouldKeep(stack);
        if (itemTypeTest != null) return itemTypeTest;

        return false;
    }
}
