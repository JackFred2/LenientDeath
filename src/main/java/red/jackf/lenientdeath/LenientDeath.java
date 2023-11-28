package red.jackf.lenientdeath;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.jackf.jackfredlib.api.config.ConfigHandler;
import red.jackf.lenientdeath.apiimpl.LenientDeathAPIImpl;
import red.jackf.lenientdeath.command.LenientDeathCommand;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.config.LenientDeathConfigMigrator;
import red.jackf.lenientdeath.config.LenientDeathJankson;
import red.jackf.lenientdeath.mixinutil.LDGroundedPosHolder;
import red.jackf.lenientdeath.mixinutil.LDPerPlayer;
import red.jackf.lenientdeath.preserveitems.PreserveItems;

public class LenientDeath implements ModInitializer {
    public static Logger getLogger(String suffix) {
        return LoggerFactory.getLogger("red.jackf.lenientdeath.Lenient Death" + (suffix.isBlank() ? "" : "/" + suffix));
    }
    public static final Logger LOGGER = getLogger("");
    public static final String MODID = "lenientdeath";

    public static final ConfigHandler<LenientDeathConfig> CONFIG = ConfigHandler.builder(LenientDeathConfig.class)
            .fileName("lenientdeath")
            .modifyJankson(LenientDeathJankson::setup)
            .withMigrator(LenientDeathConfigMigrator.create())
            .build();

    private static @Nullable MinecraftServer currentServer = null;

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.debug("Setup Lenient Death");
        
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
            //if (LenientDeath.CONFIG.instance().preserveItemsOnDeath.enabled.test(oldPlayer))
            PreserveItems.copyOldInventory(oldPlayer, newPlayer);

            if (LenientDeath.CONFIG.instance().preserveExperienceOnDeath.enabled.test(oldPlayer))
                PreserveExperience.copyExperience(oldPlayer, newPlayer);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);

        ServerTickEvents.END_WORLD_TICK.register(level -> {
            for (ServerPlayer player : level.players()) {
                var baseAABB = player.getBoundingBox();
                var supportBlockAABB = new AABB(
                        baseAABB.minX,
                        baseAABB.minY - 1.0E-6,
                        baseAABB.minZ,
                        baseAABB.maxX,
                        baseAABB.minY,
                        baseAABB.maxZ
                );
                level.findSupportingBlock(player, supportBlockAABB).ifPresent(pos -> LDGroundedPosHolder.toPlayer(player, GlobalPos.of(level.dimension(), pos)));
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer serverPlayer) {
                DeathCoordinates.onPlayerDeath(serverPlayer);
                ItemResilience.onPlayerDeath(serverPlayer);
            }
        });

        CommandRegistrationCallback.EVENT.register(LenientDeathCommand::new);

        LenientDeathAPIImpl.setup();
    }

    public static @Nullable MinecraftServer getCurrentServer() {
        return currentServer;
    }
}