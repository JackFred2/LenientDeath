package red.jackf.lenientdeath;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;

public class LenientDeath implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerPlayerEvents.COPY_FROM.register(LenientDeath::copyOldInventory);
    }

    /**
     * Called at {@link ServerPlayer#restoreFrom}
     */
    private static void copyOldInventory(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean keepEverything) {
        // dont do anything if existing checks happened
        if (keepEverything) return;
        //noinspection resource
        if (newPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || oldPlayer.isSpectator()) return;
        newPlayer.getInventory().replaceWith(oldPlayer.getInventory());
    }

    public static boolean shouldKeepOnDeath(ItemStack stack) {
        return stack.is(Items.DIAMOND);
    }
}