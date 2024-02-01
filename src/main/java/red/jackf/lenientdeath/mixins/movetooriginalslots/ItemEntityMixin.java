package red.jackf.lenientdeath.mixins.movetooriginalslots;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.mixinutil.LDRemembersSlot;

import java.util.OptionalInt;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements LDRemembersSlot {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Unique
    private OptionalInt slot = OptionalInt.empty();

    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Unique
    private static boolean isValidSlot(Inventory inventory, int slot) {
        int sum = 0;
        for (var compartment : ((InventoryAccessor) inventory).getCompartments()) sum += compartment.size();
        return slot >= 0 && slot < sum;
    }

    @Override
    public void ld$setSlot(int slot) {
        this.slot = OptionalInt.of(slot);
    }

    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void lenientdeath$getModData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(LD_REMEMBERED_SLOT, Tag.TAG_INT)) {
            this.slot = OptionalInt.of(tag.getInt(LD_REMEMBERED_SLOT));
        }
    }

    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void lenientdeath$addModData(CompoundTag tag, CallbackInfo ci) {
        if (slot.isPresent()) {
            tag.putInt(LD_REMEMBERED_SLOT, slot.getAsInt());
        }
    }

    @WrapOperation(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean lenientdeath$moveToSpecificSlot(Inventory inventory, ItemStack stack, Operation<Boolean> original) {
        // only move if part of item NBT and the specified slot is empty
        if (LenientDeath.CONFIG.instance().moveToOriginalSlots.enabled
                && this.slot.isPresent() // has a remembered slot
                && inventory.getItem(this.slot.getAsInt()).isEmpty() // remembered slot is empty
                && isValidSlot(inventory, this.slot.getAsInt())) { // not out of range
            // delegate to items specific method so we don't have to worry about stack splitting
            if (this.slot.getAsInt() < inventory.items.size()) {
                return inventory.add(this.slot.getAsInt(), stack);
            } else {
                // item pickup method uses an empty stack to know when to remove the item entity; it restores the count afterwards
                inventory.setItem(slot.getAsInt(), stack.copyAndClear());
                stack.setCount(0);
                return true;
            }
        }

        return original.call(inventory, stack);
    }
}
