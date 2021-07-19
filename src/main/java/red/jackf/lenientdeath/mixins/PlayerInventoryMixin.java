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

    @Redirect(method = "dropAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z"))
    public boolean lenientdeath$checkEmptyOrSafe(ItemStack itemStack) {
        var empty = itemStack.isEmpty();
        if (empty) return true;
        var safe = LenientDeath.isSafe(itemStack.getItem());
        return safe;
    }
}
