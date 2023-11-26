package red.jackf.lenientdeath.util;

import com.google.common.collect.AbstractIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Backported from 1.20.1, <code>net.minecraft.world.level.BlockCollisions</code>
 */
public class BlockCollisions extends AbstractIterator<BlockPos> {
    private final AABB box;
    private final CollisionContext context;
    private final Cursor3D cursor;
    private final BlockPos.MutableBlockPos pos;
    private final VoxelShape entityShape;
    private final CollisionGetter collisionGetter;
    private final boolean onlySuffocatingBlocks;
    @Nullable
    private BlockGetter cachedBlockGetter;
    private long cachedBlockGetterPos;

    public BlockCollisions(
            CollisionGetter collisionGetter,
            @Nullable Entity entity,
            AABB box,
            boolean onlySuffocatingBlocks
    ) {
        this.context = entity == null ? CollisionContext.empty() : CollisionContext.of(entity);
        this.pos = new BlockPos.MutableBlockPos();
        this.entityShape = Shapes.create(box);
        this.collisionGetter = collisionGetter;
        this.box = box;
        this.onlySuffocatingBlocks = onlySuffocatingBlocks;
        int i = Mth.floor(box.minX - 1.0E-7) - 1;
        int j = Mth.floor(box.maxX + 1.0E-7) + 1;
        int k = Mth.floor(box.minY - 1.0E-7) - 1;
        int l = Mth.floor(box.maxY + 1.0E-7) + 1;
        int m = Mth.floor(box.minZ - 1.0E-7) - 1;
        int n = Mth.floor(box.maxZ + 1.0E-7) + 1;
        this.cursor = new Cursor3D(i, k, m, j, l, n);
    }

    @Nullable
    private BlockGetter getChunk(int x, int z) {
        int i = SectionPos.blockToSectionCoord(x);
        int j = SectionPos.blockToSectionCoord(z);
        long l = ChunkPos.asLong(i, j);
        if (this.cachedBlockGetter != null && this.cachedBlockGetterPos == l) {
            return this.cachedBlockGetter;
        } else {
            BlockGetter blockGetter = this.collisionGetter.getChunkForCollisions(i, j);
            this.cachedBlockGetter = blockGetter;
            this.cachedBlockGetterPos = l;
            return blockGetter;
        }
    }

    @Override
    protected BlockPos computeNext() {
        while(this.cursor.advance()) {
            int i = this.cursor.nextX();
            int j = this.cursor.nextY();
            int k = this.cursor.nextZ();
            int l = this.cursor.getNextType();
            if (l != 3) {
                BlockGetter blockGetter = this.getChunk(i, k);
                if (blockGetter != null) {
                    this.pos.set(i, j, k);
                    BlockState blockState = blockGetter.getBlockState(this.pos);
                    if ((!this.onlySuffocatingBlocks || blockState.isSuffocating(blockGetter, this.pos))
                            && (l != 1 || blockState.hasLargeCollisionShape())
                            && (l != 2 || blockState.is(Blocks.MOVING_PISTON))) {
                        VoxelShape voxelShape = blockState.getCollisionShape(this.collisionGetter, this.pos, this.context);
                        if (voxelShape == Shapes.block()) {
                            if (this.box.intersects(i, j, k, (double)i + 1.0, (double)j + 1.0, (double)k + 1.0)) {
                                return this.pos;
                            }
                        } else {
                            VoxelShape voxelShape2 = voxelShape.move(i, j, k);
                            if (!voxelShape2.isEmpty() && Shapes.joinIsNotEmpty(voxelShape2, this.entityShape, BooleanOp.AND)) {
                                return this.pos;
                            }
                        }
                    }
                }
            }
        }

        return this.endOfData();
    }

    public static Optional<BlockPos> findSupportingBlock(CollisionGetter getter, Entity entity, AABB box) {
        BlockPos blockPos = null;
        double d = Double.MAX_VALUE;
        BlockCollisions blockCollisions = new BlockCollisions(getter, entity, box, false);

        while(blockCollisions.hasNext()) {
            BlockPos blockPos2 = blockCollisions.next();
            double e = blockPos2.distToCenterSqr(entity.position());
            if (e < d || e == d && (blockPos == null || blockPos.compareTo(blockPos2) < 0)) {
                blockPos = blockPos2.immutable();
                d = e;
            }
        }

        return Optional.ofNullable(blockPos);
    }
}
