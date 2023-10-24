package red.jackf.lenientdeath.mixinutil;

import org.jetbrains.annotations.Nullable;

public interface LDDeathContextHolder {
    @Nullable DeathContext lenientdeath$getDeathContext();
}
