package red.jackf.lenientdeath.mixins.restoreinventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.lenientdeath.restoreinventory.RestoreInventory;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    // Used instead of the FAPI event so as to get the damage message and inventory before clearing
    @Inject(method = "die", at = @At("HEAD"))
    private void saveDeathRecordBeforeClearing(DamageSource source, CallbackInfo ci) {
        RestoreInventory.INSTANCE.onDeath((ServerPlayer) (Object) this);
    }
}
