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
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.mixinutil.LDDeathDropMarkable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements LDDeathDropMarkable {
    @Unique private boolean isDeathDropItem = false;

    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    // death drop set/get
    @Override
    public void lenientdeath$markDeathDropItem() {
        this.isDeathDropItem = true;
    }

    @Override
    public boolean lenientdeath$isDeathDropItem() {
        return this.isDeathDropItem;
    }

    // read grounded position
    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void lenientdeath$readModData(CompoundTag tag, CallbackInfo ci) {
        this.isDeathDropItem = tag.getBoolean(IS_DEATH_DROP_ITEM);
    }

    // save grounded position
    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void lenientdeath$addModData(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean(IS_DEATH_DROP_ITEM, this.isDeathDropItem);
    }

    // dont merge non-death drop item with death drop item
    @ModifyExpressionValue(method = "tryToMerge(Lnet/minecraft/world/entity/item/ItemEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;areMergable(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean lenientdeath$onlyMergeIfBothDeathDrops(boolean original, ItemEntity other) {
        return original && this.isDeathDropItem == ((LDDeathDropMarkable) other).lenientdeath$isDeathDropItem();
    }

    //////////////
    // FEATURES //
    //////////////

    // prevent cactus, explosion, or tag blacklisted damages
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void lenientdeath$makeImmuneToDamage(
            DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.isDeathDropItem) {
            var config = LenientDeath.CONFIG.instance().itemResilience;
            if (config.allDeathItemsAreCactusProof && source.is(DamageTypes.CACTUS) || config.allDeathItemsAreExplosionProof && source.is(DamageTypeTags.IS_EXPLOSION) || ItemResilience.areItemsImmuneTo(source)) {
                cir.setReturnValue(false);
            }
        }
    }

    // prevent fire damage
    @ModifyReturnValue(method = "fireImmune()Z", at = @At("RETURN"))
    private boolean lenientdeath$forceFireImmuneIfNeeded(boolean original) {
        return this.isDeathDropItem && LenientDeath.CONFIG.instance().itemResilience.allDeathItemsAreFireProof || original;
    }
}
