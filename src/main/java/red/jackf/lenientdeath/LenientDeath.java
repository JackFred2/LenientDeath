package red.jackf.lenientdeath;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.preserveitems.PreserveItems;

public class LenientDeath implements ModInitializer {
    public static Logger getLogger(String suffix) {
        return LoggerFactory.getLogger("red.jackf.lenientdeath.Lenient Death" + (suffix.isBlank() ? "" : "/" + suffix));
    }
    public static final Logger LOGGER = getLogger("");
    public static final String MODID = "lenientdeath";

    @Override
    public void onInitialize() {
        LenientDeathConfig.INSTANCE.setup();
        PreserveItems.INSTANCE.setup();

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
        if (LenientDeathConfig.INSTANCE.get().preserveItemsOnDeath.enabled)
            return PreserveItems.INSTANCE.shouldPreserve(stack);
        return false;
    }

    public static void handleItem(ServerPlayer serverPlayer, ItemEntity item) {
        ItemGlow.addItemGlow(serverPlayer, item);
        ItemLifeExtender.extendItemLifetime(item);
    }
}