package red.jackf.lenientdeath.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import red.jackf.lenientdeath.LenientDeath;

@Mixin(Inventory.class)
public class InventoryMixin {

    /**
     * Prevents dropping items if on Lenient Death's whitelist
     * @param stack Item Stack being checked
     * @param original The original check being made; in vanilla it's if the ItemStack is EMPTY.
     * @return If this ItemStack should not be dropped.
     */
    @WrapOperation(
            method = "dropAll()V",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private boolean onlyDropIfNotSafe(ItemStack stack, Operation<Boolean> original) {
        return LenientDeath.shouldKeepOnDeath(stack) || original.call(stack);
    }
}
