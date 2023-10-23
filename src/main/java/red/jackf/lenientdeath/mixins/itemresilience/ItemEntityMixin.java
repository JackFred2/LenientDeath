package red.jackf.lenientdeath.mixins.itemresilience;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.jackf.lenientdeath.ItemResilience;
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.mixinutil.LDItemEntityDuck;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements LDItemEntityDuck {
    @Unique private boolean isDeathDropItem = false;
    @Unique
    private @Nullable GlobalPos lastGroundedPos = null;

    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    // mark as death drop
    @Override
    public void lenientdeath$markDeathDropItem() {
        this.isDeathDropItem = true;
    }

    @Override
    public boolean lenientdeath$isDeathDropItem() {
        return this.isDeathDropItem;
    }

    // last grounded pos
    @Override
    public @Nullable GlobalPos lenientdeath$getLastGroundedPosition() {
        return this.lastGroundedPos;
    }

    @Override
    public void lenientdeath$setLastGroundedPosition(@Nullable GlobalPos pos) {
        this.lastGroundedPos = pos;
    }

    // persistence over chunk load/unload
    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void lenientdeath$saveModData(CompoundTag tag, CallbackInfo ci) {
        this.isDeathDropItem = tag.getBoolean(IS_DEATH_DROP_ITEM);
        if (tag.contains(LAST_GROUNDED_POS, Tag.TAG_COMPOUND))
            this.lastGroundedPos = GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.getCompound(LAST_GROUNDED_POS))
                                                  .resultOrPartial(LenientDeath.LOGGER::error)
                                                  .orElse(null);
    }

    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void lenientdeath$addModData(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean(IS_DEATH_DROP_ITEM, this.isDeathDropItem);
        if (this.lastGroundedPos != null)
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, this.lastGroundedPos)
                           .resultOrPartial(LenientDeath.LOGGER::error)
                           .ifPresent(encoded -> tag.put(LAST_GROUNDED_POS, encoded));
    }

    // dont merge non-death drop item with death drop item
    @ModifyExpressionValue(method = "tryToMerge(Lnet/minecraft/world/entity/item/ItemEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;areMergable(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean lenientdeath$onlyMergeIfBothDeathDrops(boolean original, ItemEntity other) {
        return original && this.isDeathDropItem == ((LDItemEntityDuck) other).lenientdeath$isDeathDropItem();
    }

    // item damages
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void lenientdeath$makeImmuneToDamage(
            DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.isDeathDropItem) {
            var config = LenientDeathConfig.INSTANCE.get().itemResilience;
            if (config.allDeathItemsAreCactusProof && source.is(DamageTypes.CACTUS) || config.allDeathItemsAreExplosionProof && source.is(DamageTypeTags.IS_EXPLOSION) || ItemResilience.areItemsImmuneTo(source)) {
                cir.setReturnValue(false);
            }
        }
    }

    @ModifyReturnValue(method = "fireImmune()Z", at = @At("RETURN"))
    private boolean lenientdeath$forceFireImmuneIfNeeded(boolean original) {
        return this.isDeathDropItem && LenientDeathConfig.INSTANCE.get().itemResilience.allDeathItemsAreFireProof || original;
    }
}
