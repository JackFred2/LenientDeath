package red.jackf.lenientdeath.mixinutil;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Used to keep track of when an entity was last on the ground; in LD its a place for death drop items to teleport when
 * a void death occurs.
 */
public interface LDGroundedPosHolder {
    String LAST_GROUNDED_POS = "LenientDeathLastGroundedPos";

    void lenientdeath$setLastGroundedPosition(@Nullable GlobalPos pos);

    @Nullable GlobalPos lenientdeath$getLastGroundedPosition();

    static void toPlayer(ServerPlayer player, @Nullable GlobalPos groundedPos) {
        ((LDGroundedPosHolder) player).lenientdeath$setLastGroundedPosition(groundedPos);
    }
}
