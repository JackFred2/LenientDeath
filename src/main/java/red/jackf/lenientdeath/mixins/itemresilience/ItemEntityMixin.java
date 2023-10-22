package red.jackf.lenientdeath.mixins.itemresilience;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.mixinutil.LDItemEntityDuck;

@Mixin(ItemEntity.class)
public class ItemEntityMixin implements LDItemEntityDuck {
    @Unique
    private boolean isDeathDropItem = false;

    @Override
    public void lenientdeath$markDeathDropItem() {
        this.isDeathDropItem = true;
    }

    @Override
    public boolean lenientdeath$isDeathDropItem() {
        return this.isDeathDropItem;
    }

    // persistence over chunk load/unload
    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void lenientdeath$saveIsDeathDrop(CompoundTag tag, CallbackInfo ci) {
        this.isDeathDropItem = tag.getBoolean(LDItemEntityDuck.IS_DEATH_DROP_ITEM);
    }

    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void lenientdeath$addPerPlayerToData(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean(LDItemEntityDuck.IS_DEATH_DROP_ITEM, this.isDeathDropItem);
    }

    // dont merge non-death drop item with death drop item
    @ModifyExpressionValue(method = "tryToMerge(Lnet/minecraft/world/entity/item/ItemEntity;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/item/ItemEntity;areMergable(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean lenientdeath$onlyMergeIfBothDeathDrops(boolean original, ItemEntity other) {
        return original && this.isDeathDropItem == ((LDItemEntityDuck) other).lenientdeath$isDeathDropItem();
    }

    // features

    @ModifyReturnValue(method = "fireImmune()Z", at = @At("RETURN"))
    private boolean lenientdeath$forceFireImmuneIfNeeded(boolean original) {
        return this.isDeathDropItem && LenientDeathConfig.INSTANCE.get().itemResilience.allDeathItemsAreFireproof || original;
    }
}
