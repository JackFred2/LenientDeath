package red.jackf.lenientdeath;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import red.jackf.lenientdeath.command.CommandFormatting;
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
        if (LenientDeathConfig.INSTANCE.get().itemResilience.voidRecovery.mode == LenientDeathConfig.ItemResilience.VoidRecovery.Mode.last_grounded_position
                && player instanceof ServerPlayer serverPlayer) {
            var deathContextHolder = (LDDeathContextHolder) serverPlayer;
            var groundedPosHolder = (LDGroundedPosHolder) serverPlayer;
            var ctx = deathContextHolder.lenientdeath$getDeathContext();
            var groundedPos = groundedPosHolder.lenientdeath$getLastGroundedPosition();
            if (ctx != null && groundedPos != null && ctx.source().is(DamageTypes.FELL_OUT_OF_WORLD)) {
                return ifTrue.apply(ctx, groundedPos, serverPlayer);
            }
        }
        return null;
    }

    public static boolean shouldForceKeep(Player player) {
        if (LenientDeathConfig.INSTANCE.get().itemResilience.voidRecovery.mode == LenientDeathConfig.ItemResilience.VoidRecovery.Mode.preserve) {
            var deathContext = ((LDDeathContextHolder) player).lenientdeath$getDeathContext();
            return deathContext != null && deathContext.source().is(DamageTypes.FELL_OUT_OF_WORLD);
        }
        return false;
    }

    public static void onPlayerDeath(ServerPlayer serverPlayer) {
        if (LenientDeathConfig.INSTANCE.get().itemResilience.voidRecovery.announce) {
            ifHandledVoidDeath(serverPlayer, (ctx, groundedPos, serverPlayer1) -> {
                serverPlayer1.sendSystemMessage(CommandFormatting.info(
                        Component.translatable("lenientdeath.itemResilience.announce",
                                               CommandFormatting.variable(groundedPos.pos().toShortString()).resolve(CommandFormatting.TextType.BLANK),
                                               CommandFormatting.variable(groundedPos.dimension().location().toString()).resolve(CommandFormatting.TextType.BLANK))
                ));
                return null;
            });
        }
    }
}
