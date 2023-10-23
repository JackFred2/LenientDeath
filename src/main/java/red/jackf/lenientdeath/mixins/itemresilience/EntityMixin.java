package red.jackf.lenientdeath.mixins.itemresilience;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.lenientdeath.ItemResilience;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.mixinutil.LDItemEntityDuck;

@Mixin(Entity.class)
public abstract class EntityMixin {
    // if an item entity hits the void and a death drop item, either do nothing or teleport to safety if needed.
    @Inject(method = "onBelowWorld", at = @At("HEAD"), cancellable = true)
    private void lenientdeath$checkIfDeathDropForRecovery(CallbackInfo ci) {
        if (this instanceof LDItemEntityDuck ldItemEntityDuck
                && ldItemEntityDuck.lenientdeath$isDeathDropItem()
                && LenientDeathConfig.INSTANCE.get().itemResilience.voidRecoveryMode == LenientDeathConfig.ItemResilience.VoidRecoveryMode.last_grounded_position) {
            //noinspection DataFlowIssue
            if (ItemResilience.moveToSafety((ItemEntity) (Object) this)) ci.cancel();
        }
    }
}
