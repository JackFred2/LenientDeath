package red.jackf.lenientdeath.mixins;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.lenientdeath.mixinutil.LDItemEntityDuck;

@Mixin(Entity.class)
public class EntityMixin {
    // if an item entity hits the void and a death drop item, either do nothing or teleport to safety if needed.
    @Inject(method = "onBelowWorld", at = @At("HEAD"), cancellable = true)
    private void lenientdeath$checkIfDeathDropForRecovery(CallbackInfo ci) {
        if (this instanceof LDItemEntityDuck ldItemEntityDuck && ldItemEntityDuck.lenientdeath$isDeathDropItem()) {
            ci.cancel();
        }
    }
}
