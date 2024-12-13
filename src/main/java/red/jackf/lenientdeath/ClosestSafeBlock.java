package red.jackf.lenientdeath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;


public class ClosestSafeBlock {

    public static GlobalPos find(ServerLevel level, GlobalPos startPos)
    {
        var config = LenientDeath.CONFIG.instance().itemResilience;

        if (level == null) {
            return null; // If there's no world available (e.g., client is null)
        }

        BlockPos closestPos = null;
        double closestDistanceSq = Double.MAX_VALUE; // Initialize to a large value

        double immediateProximitySq = 30 * 30; // 30 meters, squared

        BlockPos startBlockPos = startPos.pos();
        int xCenter = startBlockPos.getX();
        int yCenter = startBlockPos.getY();
        int zCenter = startBlockPos.getZ();

        // Perform a spiral search
        for (int radius = 0; radius <= config.closestSafeBlockSearchRange; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        // Skip positions not on the edge of the current radius
                        continue;
                    }

                    for (int dy = -radius; dy <= radius; dy++) { // Include height variation
                        BlockPos checkPos = new BlockPos(xCenter + dx, yCenter + dy, zCenter + dz);

                        if (isValidPosition(level, checkPos)) {
                            double distanceSq = startBlockPos.distSqr(checkPos);

                            // Return immediately if within immediate proximity
                            if (distanceSq <= immediateProximitySq) {
                                return GlobalPos.of(level.dimension(), checkPos.above());
                            }

                            // Otherwise, track the closest position found
                            if (distanceSq < closestDistanceSq) {
                                closestDistanceSq = distanceSq;
                                closestPos = checkPos;
                            }
                        }
                    }
                }
            }
        }

        // Return the closest valid position found or fallback to spawn
        if (closestPos != null) {
            return GlobalPos.of(level.dimension(), closestPos.above());
        }

        return GlobalPos.of(level.dimension(), level.getSharedSpawnPos());
    }

    public static boolean isValidPosition(ServerLevel level, BlockPos pos) {
        var checkPosBlockState = level.getBlockState(pos);
        var aboveCheckPosBlockState = level.getBlockState(pos.above());
        var aboveCheckPosFluidState = level.getFluidState(pos.above());

        var isSolid = checkPosBlockState.isSolid();
        var isAirAbove = aboveCheckPosBlockState.isAir();
        var isLavaAbove = aboveCheckPosBlockState.is(Blocks.LAVA) || aboveCheckPosFluidState.is(Fluids.LAVA);

        return isSolid && isAirAbove && !isLavaAbove;
    }



}
