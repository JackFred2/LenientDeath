package red.jackf.lenientdeath.mixins.itemresilience;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.jackf.lenientdeath.ItemResilience;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.mixinutil.LDItemEntityDuck;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements LDItemEntityDuck {
    @Unique
    private boolean isDeathDropItem = false;

    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

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

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void lenientdeath$makeImmuneToDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.isDeathDropItem) {
            var config = LenientDeathConfig.INSTANCE.get().itemResilience;
            if (config.allDeathItemsAreCactusProof && source.is(DamageTypes.CACTUS)
                    || config.allDeathItemsAreExplosionProof && source.is(DamageTypeTags.IS_EXPLOSION)
                    || ItemResilience.areItemsImmuneTo(source)) {
                cir.setReturnValue(false);
            }
        }
    }

    @ModifyReturnValue(method = "fireImmune()Z", at = @At("RETURN"))
    private boolean lenientdeath$forceFireImmuneIfNeeded(boolean original) {
        return this.isDeathDropItem && LenientDeathConfig.INSTANCE.get().itemResilience.allDeathItemsAreFireProof || original;
    }
}
