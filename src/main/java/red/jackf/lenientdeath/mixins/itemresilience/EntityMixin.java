package red.jackf.lenientdeath.mixins.itemresilience;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public class EntityMixin {

    /*
    @Inject(method = "onBelowWorld", at = @At("HEAD"), cancellable = true)
    private void lenientdeath$checkIfDeathDropForRecovery(CallbackInfo ci) {
        if (this instanceof LDItemEntityDuck ldItemEntityDuck && ldItemEntityDuck.lenientdeath$isDeathDropItem()) {
            ci.cancel();
        }
    }*/
}
