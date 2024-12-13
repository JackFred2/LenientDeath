package red.jackf.lenientdeath;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import red.jackf.lenientdeath.command.Formatting;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.mixinutil.DeathContext;
import red.jackf.lenientdeath.mixinutil.LDDeathContextHolder;
import red.jackf.lenientdeath.mixinutil.LDGroundedPosHolder;


public class ItemResilience {
    private static final TagKey<DamageType> ITEMS_IMMUNE_TO = TagKey.create(
            Registries.DAMAGE_TYPE,
            LenientDeath.id("items_immune_to")
    );

    private ItemResilience() {}

    public static boolean areItemsImmuneTo(DamageSource source) {
        return source.is(ITEMS_IMMUNE_TO);
    }

    public static <T> @Nullable T ifHandledVoidDeath(
            Object player,
            TriFunction<DeathContext, GlobalPos, ServerPlayer, T> ifTrue) {
        if (LenientDeath.CONFIG.instance().itemResilience.voidRecovery.mode == LenientDeathConfig.ItemResilience.VoidRecovery.Mode.last_grounded_position
                && player instanceof ServerPlayer serverPlayer) {
            var deathContextHolder = (LDDeathContextHolder) serverPlayer;
            var groundedPosHolder = (LDGroundedPosHolder) serverPlayer;
            var ctx = deathContextHolder.lenientdeath$getDeathContext();
            var groundedPos = groundedPosHolder.lenientdeath$getLastGroundedPosition();

            //If the groundedPos is not valid (e.g Air or Lava) then find a safe block.
            if(!ClosestSafeBlock.isValidPosition(serverPlayer.serverLevel(),groundedPos.pos()))
            {
                //If there is no LastGroundedPosition or if the groundedPos is too far away from the death location, use the actual player location
                if(groundedPos == null || groundedPos.pos().distSqr(serverPlayer.getOnPos()) >= 100)
                    groundedPos = GlobalPos.of(serverPlayer.serverLevel().dimension(), serverPlayer.getOnPos());

                groundedPos = ClosestSafeBlock.find(serverPlayer.serverLevel(),groundedPos);
            }

            if (ctx != null && groundedPos != null && ctx.source().is(DamageTypes.FELL_OUT_OF_WORLD)) {
                return ifTrue.apply(ctx, groundedPos, serverPlayer);
            }
        }
        return null;
    }

    public static <T> @Nullable T ifHandledLavaDeath(
            Object player,
            TriFunction<DeathContext, GlobalPos, ServerPlayer, T> ifTrue) {
        if (LenientDeath.CONFIG.instance().itemResilience.lavaRecovery.mode == LenientDeathConfig.ItemResilience.LavaRecovery.Mode.closest_safe_location
                && player instanceof ServerPlayer serverPlayer) {
            var deathContextHolder = (LDDeathContextHolder) serverPlayer;
            var groundedPosHolder = (LDGroundedPosHolder) serverPlayer;
            var ctx = deathContextHolder.lenientdeath$getDeathContext();
            var groundedPos = groundedPosHolder.lenientdeath$getLastGroundedPosition();

            //If there is no LastGroundedPosition or if the groundedPos is too far away from the death location, use the actual player location
            if(groundedPos == null || groundedPos.pos().distSqr(serverPlayer.getOnPos()) >= 30)
                groundedPos = GlobalPos.of(serverPlayer.serverLevel().dimension(), serverPlayer.getOnPos());

            //Determine the closet safe block
            var safePos = ClosestSafeBlock.find(serverPlayer.serverLevel(), groundedPos);

            if (ctx != null && safePos != null && ctx.source().is(DamageTypes.LAVA)) {
                return ifTrue.apply(ctx, safePos, serverPlayer);
            }
        }
        return null;
    }

    public static boolean shouldForceKeep(ServerPlayer player) {
        if (LenientDeath.CONFIG.instance().itemResilience.voidRecovery.mode == LenientDeathConfig.ItemResilience.VoidRecovery.Mode.preserve) {
            var deathContext = ((LDDeathContextHolder) player).lenientdeath$getDeathContext();
            return deathContext != null && deathContext.source().is(DamageTypes.FELL_OUT_OF_WORLD);
        }
        if (LenientDeath.CONFIG.instance().itemResilience.lavaRecovery.mode == LenientDeathConfig.ItemResilience.LavaRecovery.Mode.preserve) {
            var deathContext = ((LDDeathContextHolder) player).lenientdeath$getDeathContext();
            return deathContext != null && deathContext.source().is(DamageTypes.LAVA);
        }
        return false;
    }

    public static void onPlayerDeath(ServerPlayer serverPlayer) {
        if (LenientDeath.CONFIG.instance().itemResilience.voidRecovery.announce) {
            ifHandledVoidDeath(serverPlayer, (ctx, groundedPos, serverPlayer1) -> {
                serverPlayer1.sendSystemMessage(Formatting.infoLine(
                        Component.translatable("lenientdeath.itemResilience.announce",
                                               Formatting.variable(groundedPos.pos().above().toShortString()),
                                               Formatting.variable(groundedPos.dimension().location().toString()))
                ));
                return null;
            });
        }

        if (LenientDeath.CONFIG.instance().itemResilience.lavaRecovery.announce) {
            ifHandledLavaDeath(serverPlayer, (ctx, groundedPos, serverPlayer1) -> {
                serverPlayer1.sendSystemMessage(Formatting.infoLine(
                        Component.translatable("lenientdeath.itemResilience.announce",
                                Formatting.variable(groundedPos.pos().above().toShortString()),
                                Formatting.variable(groundedPos.dimension().location().toString()))
                ));
                return null;
            });
        }
    }
}
