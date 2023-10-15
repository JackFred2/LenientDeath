package red.jackf.lenientdeath.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import red.jackf.lenientdeath.ItemGlow;
import red.jackf.lenientdeath.LenientDeath;

@Mixin(Inventory.class)
public class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    /**
     * Prevents dropping items if on Lenient Death's whitelist
     *
     * @param stack    Item Stack being checked
     * @param original The original check being made; in vanilla it's if the ItemStack is EMPTY.
     * @return If this ItemStack should not be dropped.
     */
    @WrapOperation(
            method = "dropAll()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private boolean onlyDropIfNotSafe(ItemStack stack, Operation<Boolean> original) {
        return LenientDeath.shouldKeepOnDeath(stack) || original.call(stack);
    }

    @ModifyExpressionValue(
            method = "dropAll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity addOutlineToEntity(ItemEntity original) {
        if (this.player instanceof ServerPlayer serverPlayer) {
            ItemGlow.addItemGlow(serverPlayer, original);
            original.setCustomName(Component.literal("name"));
        }
        return original;
    }
}
