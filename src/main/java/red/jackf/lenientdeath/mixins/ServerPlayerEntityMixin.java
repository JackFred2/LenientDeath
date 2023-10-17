package red.jackf.lenientdeath.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.PerPlayerDuck;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin extends Player implements PerPlayerDuck {
    @Unique
    private boolean perPlayerEnabledForMe = LenientDeathConfig.INSTANCE.get().perPlayer.defaultEnabledForPlayer;

    public ServerPlayerEntityMixin(
            Level level,
            BlockPos pos,
            float yRot,
            GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    @Override
    public boolean lenientdeath$isPerPlayerEnabled() {
        return perPlayerEnabledForMe;
    }

    @Override
    public void lenientdeath$setPerPlayerEnabled(boolean newValue) {
        this.perPlayerEnabledForMe = newValue;
    }

    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void lenientdeath$readPerPlayerToData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(PerPlayerDuck.PER_PLAYER_TAG_KEY, Tag.TAG_BYTE))
            this.perPlayerEnabledForMe = tag.getBoolean(PerPlayerDuck.PER_PLAYER_TAG_KEY);
    }

    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void lenientdeath$addPerPlayerToData(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean(PerPlayerDuck.PER_PLAYER_TAG_KEY, this.perPlayerEnabledForMe);
    }
}
