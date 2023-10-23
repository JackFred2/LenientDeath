package red.jackf.lenientdeath.mixinutil;

import org.jetbrains.annotations.Nullable;

/**
 * Used to keep track of a player's per-player status.
 */
public interface LDServerPlayerDuck extends LDGroundedPosHolder {

    @Nullable DeathContext lenientdeath$getDeathContext();
}
