package red.jackf.lenientdeath.mixinutil;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.player.Player;
import red.jackf.lenientdeath.PermissionKeys;

public interface LDPerPlayer {
    String PER_PLAYER_TAG_KEY = "LenientDeathPerPlayer";

    boolean lenientdeath$isPerPlayerEnabled();

    void lenientdeath$setPerPlayerEnabled(boolean newValue);

    static boolean isEnabledFor(Player player) {
        return Permissions.check(player, PermissionKeys.PER_PLAYER, ((LDPerPlayer) player).lenientdeath$isPerPlayerEnabled());
    }

    static boolean isHandledByPermission(Player player) {
        return Permissions.getPermissionValue(player, PermissionKeys.PER_PLAYER) != TriState.DEFAULT;
    }
}
