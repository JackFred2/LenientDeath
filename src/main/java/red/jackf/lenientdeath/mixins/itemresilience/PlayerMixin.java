package red.jackf.lenientdeath.mixins.itemresilience;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import red.jackf.lenientdeath.ItemResilience;

@Mixin(Player.class)
public class PlayerMixin {

    @ModifyArg(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"))
    private Level lenientdeath$changeSpawnLevel(Level original) {
        var targetLevel = ItemResilience.ifHandledVoidDeath(this, (ctx, groundedPos, player) -> {
            if (!groundedPos.dimension().equals(original.dimension())) {
                return player.server.getLevel(groundedPos.dimension());
            } else {
                return null;
            }
        });
        if (targetLevel != null) return targetLevel;
        return original;
    }

    @ModifyReturnValue(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At(value = "RETURN"))
    private ItemEntity lenientdeath$moveIfNeeded(@Nullable ItemEntity returned) {
        if (returned != null) {
            ItemResilience.ifHandledVoidDeath(this, (ctx, groundedPos, player) -> {
                var vec3 = groundedPos.pos().above().getCenter();
                returned.teleportTo(
                        vec3.x,
                        vec3.y,
                        vec3.z
                );
                returned.setDeltaMovement(Vec3.ZERO);
                return null;
            });
        }
        return returned;
    }
}
