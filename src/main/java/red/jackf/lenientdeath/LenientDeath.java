package red.jackf.lenientdeath;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.jackf.lenientdeath.command.LenientDeathCommand;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.preserveitems.PreserveItems;

public class LenientDeath implements ModInitializer {
    public static Logger getLogger(String suffix) {
        return LoggerFactory.getLogger("red.jackf.lenientdeath.Lenient Death" + (suffix.isBlank() ? "" : "/" + suffix));
    }
    public static final Logger LOGGER = getLogger("");
    public static final String MODID = "lenientdeath";
    public static final String PER_PLAYER_TAG_KEY = "LenientDeathPerPlayer";

    private static @Nullable MinecraftServer currentServer = null;

    @Override
    public void onInitialize() {
        LenientDeathConfig.INSTANCE.setup();
        PreserveItems.INSTANCE.setup();

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, keepEverything) -> {
            copyOldInventory(oldPlayer, newPlayer, keepEverything);

            ((PerPlayerDuck) newPlayer).lenientdeath$setPerPlayerEnabled(
                    ((PerPlayerDuck) oldPlayer).lenientdeath$isPerPlayerEnabled()
            );
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);

        CommandRegistrationCallback.EVENT.register(LenientDeathCommand::new);
    }

    public static @Nullable MinecraftServer getCurrentServer() {
        return currentServer;
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

    public static boolean shouldKeepOnDeath(Player player, ItemStack stack) {
        var config = LenientDeathConfig.INSTANCE.get().preserveItemsOnDeath;
        if (config.enabled.test(player)) return PreserveItems.INSTANCE.shouldPreserve(stack);
        return false;
    }

    public static void handleItem(ServerPlayer serverPlayer, ItemEntity item) {
        ItemGlow.addItemGlow(serverPlayer, item);
        ItemLifeExtender.extendItemLifetime(item);
    }
}