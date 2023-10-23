package red.jackf.lenientdeath.mixins.itemresilience;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.mixinutil.DeathContext;
import red.jackf.lenientdeath.mixinutil.LDServerPlayerDuck;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements LDServerPlayerDuck {
    /**
     * The last position that an entity was on the ground. Actively updated for a player, only copied for an item entity.
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

    // last grounded pos
    @Override
    public @Nullable GlobalPos lenientdeath$getLastGroundedPosition() {
        return this.lastGroundedPos;
    }

    @Override
    public void lenientdeath$setLastGroundedPosition(@Nullable GlobalPos pos) {
        this.lastGroundedPos = pos;
    }

    @Override
    public @Nullable DeathContext lenientdeath$getDeathContext() {
        return deathContext;
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void lenientdeath$rememberDeathSource(DamageSource damageSource, CallbackInfo ci) {
        this.deathContext = new DeathContext(damageSource);
    }

    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void lenientdeath$getModData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(LAST_GROUNDED_POS, Tag.TAG_COMPOUND))
            this.lastGroundedPos = GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.getCompound(LAST_GROUNDED_POS))
                                                  .resultOrPartial(LenientDeath.LOGGER::error)
                                                  .orElse(null);
    }

    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void lenientdeath$addModData(CompoundTag tag, CallbackInfo ci) {
        if (this.lastGroundedPos != null)
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, this.lastGroundedPos)
                           .resultOrPartial(LenientDeath.LOGGER::error)
                           .ifPresent(encoded -> tag.put(LAST_GROUNDED_POS, encoded));
    }
}
