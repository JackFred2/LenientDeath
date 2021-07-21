package red.jackf.lenientdeath.mixins;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.lenientdeath.LenientDeath;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin  {

    /**
     * Check if the item should be kept, either due to it being nothing (the vanilla check) or by being a safe item.
     */
    @Redirect(method = "dropAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z"))
    public boolean lenientdeath$checkEmptyOrSafe(ItemStack itemStack) {
        return itemStack.isEmpty() || LenientDeath.isSafe(itemStack.getItem());
    }
}
