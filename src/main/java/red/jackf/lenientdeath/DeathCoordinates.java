package red.jackf.lenientdeath;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import red.jackf.lenientdeath.command.Formatting;
import red.jackf.lenientdeath.config.LenientDeathConfig;

public class DeathCoordinates {
    private DeathCoordinates() {}

    public static void onPlayerDeath(ServerPlayer serverPlayer) {
        if (LenientDeathConfig.INSTANCE.get().deathCoordinates.enabled) {
            BlockPos coordinates = serverPlayer.blockPosition();

            //noinspection resource
            serverPlayer.sendSystemMessage(Formatting.errorLine(
                    Component.translatable("lenientdeath.deathCoordinates",
                                           Formatting.variable(coordinates.above().toShortString()),
                                           Formatting.variable(serverPlayer.level().dimension().location().toString()))
            ));
        }
    }
}
