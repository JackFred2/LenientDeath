package red.jackf.lenientdeath.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.lenientdeath.ItemResilience;
import red.jackf.lenientdeath.api.LenientDeathAPI;

@Mixin(Inventory.class)
public class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    /**
     * Prevents dropping items if on Lenient Death's whitelist
     *
     * @param stack    Item Stack being checked
     * @param original The item check being made; in vanilla it's if the ItemStack is EMPTY.
     * @return If this ItemStack should not be dropped.
     */
    @WrapOperation(
            method = "dropAll()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private boolean onlyDropIfNotSafe(ItemStack stack, Operation<Boolean> original, @Share("ldSlotCount") LocalIntRef slot) {
        if (!(this.player instanceof ServerPlayer deadPlayer)) return original.call(stack);

        int preserved = LenientDeathAPI.INSTANCE.howManyToPreserve(deadPlayer, stack);
        if (preserved == 0) {
            return original.call(stack);
        } else if (preserved == stack.getCount() || ItemResilience.shouldForceKeep(deadPlayer)) {
            return true;
        } else {
            ItemStack dropped = stack.split(stack.getCount() - preserved);
            LenientDeathAPI.INSTANCE.markDeathItem(deadPlayer, deadPlayer.drop(dropped, true, false), slot.get());
            return true;
        }
    }

    @Inject(method = "dropAll", at = @At("HEAD"))
    private void startCount(CallbackInfo ci, @Share("ldSlotCount") LocalIntRef slot) {
        slot.set(-1); // prevent off by one errors
    }

    @Inject(method = "dropAll", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;", shift = At.Shift.BEFORE))
    private void incrementCount(CallbackInfo ci, @Share("ldSlotCount") LocalIntRef slot) {
        slot.set(slot.get() + 1);
    }

    @ModifyExpressionValue(
            method = "dropAll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity handleItemEntity(ItemEntity item, @Share("ldSlotCount") LocalIntRef slot) {
        if (this.player instanceof ServerPlayer serverPlayer && item != null) {
            LenientDeathAPI.INSTANCE.markDeathItem(serverPlayer, item, slot.get());
        }
        return item;
    }
}
