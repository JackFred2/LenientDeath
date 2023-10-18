package red.jackf.lenientdeath;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.player.Player;

public interface PerPlayerDuck {
    String PER_PLAYER_TAG_KEY = "LenientDeathPerPlayer";

    boolean lenientdeath$isPerPlayerEnabled();

    void lenientdeath$setPerPlayerEnabled(boolean newValue);

    static boolean isEnabledFor(Player player) {
        return Permissions.check(player, PermissionKeys.PER_PLAYER, ((PerPlayerDuck) player).lenientdeath$isPerPlayerEnabled());
    }

    static boolean isHandledByPermission(Player player) {
        return Permissions.getPermissionValue(player, PermissionKeys.PER_PLAYER) != TriState.DEFAULT;
    }
}
