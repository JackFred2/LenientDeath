package red.jackf.lenientdeath.mixins;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import red.jackf.lenientdeath.LenientDeath;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    /**
     * Check if the item should be kept, either due to it being nothing (the vanilla check) or by being a safe item.
     */
    @ModifyVariable(method = "dropAll", at=@At(value = "INVOKE_ASSIGN", target = "Ljava/util/List;get(I)Ljava/lang/Object;", shift = At.Shift.BY, by = 2), ordinal = 0)
    private ItemStack lenientdeath$checkStackSafe(ItemStack original) {
        if (LenientDeath.isSafe(original.getItem())) {
            return ItemStack.EMPTY;
        }
        return original;
    }
}
