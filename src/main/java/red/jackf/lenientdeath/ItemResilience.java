package red.jackf.lenientdeath;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import red.jackf.lenientdeath.mixinutil.LDGroundedPosHolder;

import java.util.Collections;

public class ItemResilience {
    private static final TagKey<DamageType> ITEMS_IMMUNE_TO = TagKey.create(
            Registries.DAMAGE_TYPE,
            LenientDeath.id("items_immune_to")
    );

    private ItemResilience() {}

    public static boolean areItemsImmuneTo(DamageSource source) {
        return source.is(ITEMS_IMMUNE_TO);
    }

    public static void handle(ServerPlayer player, ItemEntity item) {
        LDGroundedPosHolder.toItem(item, LDGroundedPosHolder.fromPlayer(player));
    }

    // in
    public static boolean moveToSafety(ItemEntity item) {
        var safePos = LDGroundedPosHolder.fromItem(item);
        //noinspection resource
        if (safePos != null && item.level() instanceof ServerLevel serverLevel) {
            var targetLevel = serverLevel.getServer().getLevel(safePos.dimension());
            if (targetLevel != null) {
                Vec3 targetPos = safePos.pos().above().getCenter();
                item.setDeltaMovement(Vec3.ZERO);
                item.teleportTo(targetLevel,
                                targetPos.x,
                                targetPos.y,
                                targetPos.z,
                                Collections.emptySet(),
                                item.getYRot(),
                                item.getXRot());
                return true;
            }
        }

        return false;
    }
}
