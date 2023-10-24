package red.jackf.lenientdeath;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.mixinutil.DeathContext;
import red.jackf.lenientdeath.mixinutil.LDDeathContextHolder;
import red.jackf.lenientdeath.mixinutil.LDGroundedPosHolder;

import static red.jackf.lenientdeath.config.LenientDeathConfig.ItemResilience.VoidRecoveryMode;

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
        if (LenientDeathConfig.INSTANCE.get().itemResilience.voidRecoveryMode == VoidRecoveryMode.last_grounded_position
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
}
