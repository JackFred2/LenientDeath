package red.jackf.lenientdeath.mixins.itemresilience;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import red.jackf.lenientdeath.mixinutil.LDDeathContextHolder;
import red.jackf.lenientdeath.mixinutil.LDGroundedPosHolder;

@Mixin(Player.class)
public class PlayerMixin {

    @ModifyExpressionValue(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;level()Lnet/minecraft/world/level/Level;"))
    private Level lenientdeath$changeSpawnLevel(Level original) {
        //noinspection ConstantValue
        if (this instanceof LDDeathContextHolder contextHolder
                && this instanceof LDGroundedPosHolder groundedPosHolder
                && ((Object) this) instanceof ServerPlayer serverPlayer) {
            var context = contextHolder.lenientdeath$getDeathContext();
            var groundPos = groundedPosHolder.lenientdeath$getLastGroundedPosition();
            if (context != null && groundPos != null && context.source().is(DamageTypes.FELL_OUT_OF_WORLD)) {
                var targetLevel = serverPlayer.server.getLevel(groundPos.dimension());
                if (targetLevel != null) return targetLevel;
            }
        }
        return original;
    }

    @ModifyReturnValue(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At(value = "RETURN"))
    private ItemEntity lenientdeath$moveIfNeeded(@Nullable ItemEntity returned) {
        if (this instanceof LDDeathContextHolder contextHolder
                && this instanceof LDGroundedPosHolder groundedPosHolder
                && returned != null) {
            var ctx = contextHolder.lenientdeath$getDeathContext();
            if (ctx != null && ctx.source().is(DamageTypes.FELL_OUT_OF_WORLD)) {
                var targetPos = groundedPosHolder.lenientdeath$getLastGroundedPosition();
                if (targetPos != null) {
                    var vec3 = targetPos.pos().above().getCenter();
                    returned.teleportTo(
                            vec3.x,
                            vec3.y,
                            vec3.z
                    );
                    returned.setDeltaMovement(Vec3.ZERO);
                }
            }
        }
        return returned;
    }
}
