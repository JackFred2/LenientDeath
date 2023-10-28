package red.jackf.lenientdeath;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import red.jackf.lenientdeath.command.Formatting;
import red.jackf.lenientdeath.config.LenientDeathConfig;

public class DeathCoordinates {
    private DeathCoordinates() {}

    public static void onPlayerDeath(ServerPlayer deadPlayer) {
        var config = LenientDeathConfig.INSTANCE.get().deathCoordinates;
        MinecraftServer server = deadPlayer.server;

        BlockPos coordinates = deadPlayer.blockPosition();

        //noinspection resource
        Component playerMessage = Formatting.errorLine(
                Component.translatable("lenientdeath.deathCoordinates",
                                       Formatting.variable(coordinates.above().toShortString()),
                                       Formatting.variable(deadPlayer.level().dimension().location().toString()))
        );

        if (config.sendToDeadPlayer) deadPlayer.sendSystemMessage(playerMessage);

        Component othersMessage = Component.translatable(
                "chat.type.admin",
                deadPlayer.getDisplayName(),
                playerMessage
        ).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);

        if (config.sendToServerLog) server.sendSystemMessage(othersMessage);

        if (config.sendToOtherAdmins) {
            for (var otherPlayer : server.getPlayerList().getPlayers()) {
                if (otherPlayer != deadPlayer && server.getPlayerList().isOp(otherPlayer.getGameProfile())) {
                    otherPlayer.sendSystemMessage(othersMessage);
                }
            }
        }
    }
}
