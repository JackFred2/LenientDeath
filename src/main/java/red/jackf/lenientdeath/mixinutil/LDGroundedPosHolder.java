package red.jackf.lenientdeath.mixinutil;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Used to keep track of when an entity was last on the ground; in LD its a place for death drop items to teleport when
 * a void death occurs.
 */
public interface LDGroundedPosHolder {
    String LAST_GROUNDED_POS = "LenientDeathLastGroundedPos";

    void lenientdeath$setLastGroundedPosition(@Nullable GlobalPos pos);

    @Nullable GlobalPos lenientdeath$getLastGroundedPosition();

    static @Nullable GlobalPos fromPlayer(ServerPlayer player) {
        return ((LDGroundedPosHolder) player).lenientdeath$getLastGroundedPosition();
    }

    static @Nullable GlobalPos fromItem(ItemEntity item) {
        return ((LDGroundedPosHolder) item).lenientdeath$getLastGroundedPosition();
    }

    static void toPlayer(ServerPlayer player, @Nullable GlobalPos groundedPos) {
        ((LDGroundedPosHolder) player).lenientdeath$setLastGroundedPosition(groundedPos);
    }

    static void toItem(ItemEntity item, @Nullable GlobalPos groundedPos) {
        ((LDGroundedPosHolder) item).lenientdeath$setLastGroundedPosition(groundedPos);
    }
}
