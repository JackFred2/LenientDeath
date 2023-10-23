package red.jackf.lenientdeath.mixinutil;

import net.minecraft.world.damagesource.DamageSource;

public record DeathContext(DamageSource source) {
}
