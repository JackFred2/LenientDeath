package red.jackf.lenientdeath.mixins.itemresilience;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.lenientdeath.ItemResilience;
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.mixinutil.DeathContext;
import red.jackf.lenientdeath.mixinutil.LDDeathContextHolder;
import red.jackf.lenientdeath.mixinutil.LDGroundedPosHolder;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements LDGroundedPosHolder, LDDeathContextHolder {
    @Shadow
    @Final
    public MinecraftServer server;

    /**
     * The last position that an entity was on the ground.
     */
    @Unique
    private @Nullable GlobalPos lastGroundedPos = null;
    /**
     * Not serialized. Used to pass damage-related info up until the inventory drop calls
     */
    @Unique
    private @Nullable DeathContext deathContext = null;

    public ServerPlayerMixin(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    // last grounded pos set/get
    @Override
    public @Nullable GlobalPos lenientdeath$getLastGroundedPosition() {
        return this.lastGroundedPos;
    }

    @Override
    public void lenientdeath$setLastGroundedPosition(@Nullable GlobalPos pos) {
        this.lastGroundedPos = pos;
    }

    // add death source for future calls
    @Inject(method = "die", at = @At("HEAD"))
    private void addDeathContext(DamageSource damageSource, CallbackInfo ci) {
        this.deathContext = new DeathContext(damageSource);
    }

    @Override
    public @Nullable DeathContext lenientdeath$getDeathContext() {
        return deathContext;
    }

    // read grounded position
    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void loadGroundedPos(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(LAST_GROUNDED_POS, Tag.TAG_COMPOUND))
            this.lastGroundedPos = GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.getCompound(LAST_GROUNDED_POS))
                    .resultOrPartial(LenientDeath.LOGGER::error)
                    .orElse(null);
    }

    // save grounded position
    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void saveGroundedPos(CompoundTag tag, CallbackInfo ci) {
        if (this.lastGroundedPos != null)
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, this.lastGroundedPos)
                    .resultOrPartial(LenientDeath.LOGGER::error)
                    .ifPresent(encoded -> tag.put(LAST_GROUNDED_POS, encoded));
    }

    // change added dimension if not the same
    @ModifyReceiver(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private Level lenientdeath$moveIfVoidedAndEnabled(Level original, Entity entity) {
        ServerLevel targetLevel = null;

        // Handling void death scenario
        var voidTargetLevel = ItemResilience.ifHandledVoidDeath(this, (deathContext, lastGroundedPos, player) -> {
            if (!lastGroundedPos.dimension().equals(original.dimension())) {
                return this.server.getLevel(lastGroundedPos.dimension());
            } else {
                return null;
            }
        });

        // Handling lava death scenario
        var lavaTargetLevel = ItemResilience.ifHandledLavaDeath(this, (deathContext, lastGroundedPos, player) -> {
            if (!lastGroundedPos.dimension().equals(original.dimension())) {
                return this.server.getLevel(lastGroundedPos.dimension());
            } else {
                return null;
            }
        });

        // Assigning the target level if any of the scenarios is met
        if (voidTargetLevel != null) {
            targetLevel = voidTargetLevel;
        } else if (lavaTargetLevel != null) {
            targetLevel = lavaTargetLevel;
        }

        if (targetLevel != null) return targetLevel;
        return original;
    }

    @WrapOperation(method = "createItemStackToDrop", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity spawnDeathItemAtDifferentPosition(Level level, double posX, double posY, double posZ, ItemStack itemStack, Operation<ItemEntity> original) {

        ItemEntity item = null;

        // Handling void death scenario
        var voidDeathItem = ItemResilience.ifHandledVoidDeath(this, (deathContext, groundPos, player) -> {
            Vec3 pos = groundPos.pos().getCenter();
            return original.call(this.server.getLevel(groundPos.dimension()), pos.x(), pos.y() + 1, pos.z(), itemStack);
        });

        // Handling lava death scenario
        var lavaDeathItem = ItemResilience.ifHandledLavaDeath(this, (deathContext, groundPos, player) -> {
            Vec3 pos = groundPos.pos().getCenter();
            return original.call(this.server.getLevel(groundPos.dimension()), pos.x(), pos.y() + 1, pos.z(), itemStack);
        });

        // Assigning the item if any of the scenarios is met
        if (voidDeathItem != null) {
            item = voidDeathItem;
        } else if (lavaDeathItem != null) {
            item = lavaDeathItem;
        }

        if (item == null) item = original.call(level, posX, posY, posZ, itemStack);

        return item;
    }

    @ModifyReturnValue(method = "createItemStackToDrop", at = @At("RETURN"))
    private ItemEntity killDeathItemVelocity(ItemEntity original) {

        // Handling void death scenario
        ItemResilience.ifHandledVoidDeath(this, (deathContext, groundPos, player) -> {
            if (original != null) original.setDeltaMovement(Vec3.ZERO);
            return null;
        });

        // Handling lava death scenario
        ItemResilience.ifHandledLavaDeath(this, (deathContext, groundPos, player) -> {
            if (original != null) original.setDeltaMovement(Vec3.ZERO);
            return null;
        });

        return original;
    }
}
