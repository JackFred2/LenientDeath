package red.jackf.lenientdeath.mixins.compat;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.lenientdeath.LenientDeath;

@Mixin(value = LivingEntity.class, priority = 1200)
@Pseudo
public class LivingEntityTrinketsMixin {

    @Inject(method = "Lnet/minecraft/world/entity/LivingEntity;dropFromEntity(Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(DDD)V"),
            require = 0)
    private void lenientdeath$handleTrinketsItemEntities(ItemStack stack, CallbackInfo ci, @Local ItemEntity itemEntity) {
        if (((Object) this) instanceof ServerPlayer serverPlayer)
            LenientDeath.handleItemEntity(serverPlayer, itemEntity, null);
    }
}
