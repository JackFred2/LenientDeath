package red.jackf.lenientdeath.mixins.preserveexperience;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.PreserveExperience;

/**
 * Modify the amount of a player's dropped experience, to match reduction of xp preservation config
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyArg(method = "dropExperience",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private int possiblyReduceXpAmount(int originalAmount) {
        // casting shenanigans
        // noinspection ConstantValue
        if (((Object) this) instanceof Player player && LenientDeath.CONFIG.instance().preserveExperienceOnDeath.enabled.test(player)) {
            return PreserveExperience.reduceXpOrbExperience(originalAmount);
        }
        return originalAmount;
    }
}
