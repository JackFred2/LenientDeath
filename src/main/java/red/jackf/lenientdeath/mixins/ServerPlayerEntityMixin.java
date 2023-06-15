package red.jackf.lenientdeath.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.lenientdeath.utils.LenientDeathPerPlayerMixinInterface;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements LenientDeathPerPlayerMixinInterface {

    @Shadow public abstract ServerWorld getServerWorld();

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    private boolean isItemSavingEnabled = true;

    @Override
    public boolean isItemSavingEnabled() {
        return isItemSavingEnabled;
    }

    @Override
    public void setItemSavingEnabled(boolean enabled) {
        this.isItemSavingEnabled = enabled;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    public void lenientdeath$addPerPlayerToNbt (NbtCompound tag, CallbackInfo info) {
        tag.putBoolean("isItemSavingEnabled", isItemSavingEnabled);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    public void lenientdeath$getPerPlayerFromNbt(NbtCompound tag, CallbackInfo info) {
        isItemSavingEnabled = tag.getBoolean("isItemSavingEnabled");
    }

    /**
     * Copy the old inventory to new upon player entity reconstruction
     */
    @Inject(method = "copyFrom", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerPlayerEntity;enchantmentTableSeed:I", opcode = Opcodes.PUTFIELD))
    public void lenientdeath$copyInvFromOld(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        if (!alive && !this.getServerWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY) && !oldPlayer.isSpectator()) {
            this.getInventory().clone(oldPlayer.getInventory());
        }
        this.isItemSavingEnabled = ((LenientDeathPerPlayerMixinInterface) oldPlayer).isItemSavingEnabled();
    }
}
