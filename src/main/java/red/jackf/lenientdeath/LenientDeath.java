package red.jackf.lenientdeath;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.jackf.lenientdeath.command.LenientDeathCommand;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.mixinutil.LDGroundedPosHolder;
import red.jackf.lenientdeath.mixinutil.LDDeathDropMarkable;
import red.jackf.lenientdeath.mixinutil.LDPerPlayer;
import red.jackf.lenientdeath.preserveitems.PreserveItems;

public class LenientDeath implements ModInitializer {
    public static Logger getLogger(String suffix) {
        return LoggerFactory.getLogger("red.jackf.lenientdeath.Lenient Death" + (suffix.isBlank() ? "" : "/" + suffix));
    }
    public static final Logger LOGGER = getLogger("");
    public static final String MODID = "lenientdeath";

    private static @Nullable MinecraftServer currentServer = null;

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.debug("Setup Lenient Death");

        LenientDeathConfig.INSTANCE.setup();
        PreserveItems.INSTANCE.setup();

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, keepEverything) -> {
            ((LDPerPlayer) newPlayer).lenientdeath$setPerPlayerEnabled(
                ((LDPerPlayer) oldPlayer).lenientdeath$isPerPlayerEnabled()
            );

            // skip if vanilla handled it
            // noinspection resource
            if (keepEverything || newPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || oldPlayer.isSpectator())
                return;

            // check is done on the item preservation side, don't double check in case setting changes between respawn
            //if (LenientDeathConfig.INSTANCE.get().preserveItemsOnDeath.enabled.test(oldPlayer))
            PreserveItems.copyOldInventory(oldPlayer, newPlayer);

            if (LenientDeathConfig.INSTANCE.get().preserveExperienceOnDeath.enabled.test(oldPlayer))
                PreserveExperience.copyExperience(oldPlayer, newPlayer);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);

        ServerTickEvents.END_WORLD_TICK.register(level -> {
            for (ServerPlayer player : level.players())
                player.mainSupportingBlockPos.ifPresent(pos ->
                    LDGroundedPosHolder.toPlayer(player, GlobalPos.of(level.dimension(), pos)));
        });

        CommandRegistrationCallback.EVENT.register(LenientDeathCommand::new);
    }

    public static @Nullable MinecraftServer getCurrentServer() {
        return currentServer;
    }

    // Handles items that are dropped; i.e those that didn't pass the item preservation check
    public static void handleItemEntity(ServerPlayer serverPlayer, ItemEntity item) {
        ItemGlow.addItemGlow(serverPlayer, item);
        ItemLifeExtender.extendItemLifetime(item);
        ((LDDeathDropMarkable) item).lenientdeath$markDeathDropItem();
    }
}