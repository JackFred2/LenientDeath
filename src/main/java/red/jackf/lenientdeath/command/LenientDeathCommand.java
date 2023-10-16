package red.jackf.lenientdeath.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.preserveitems.LenientDeathServerPlayerDuck;

import static net.minecraft.network.chat.Component.translatable;
import static red.jackf.lenientdeath.command.CommandFormatting.*;

public class LenientDeathCommand {
    //////////////////
    // BASE COMMAND //
    //////////////////
    public LenientDeathCommand(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext buildCtx,
            Commands.CommandSelection selection) {
        var commandNames = LenientDeathConfig.INSTANCE.get().command.commandNames;
        if (commandNames.isEmpty()) return;

        var root = Commands.literal(commandNames.get(0));
        root.then(perPlayer$root(buildCtx));

        var builtRoot = dispatcher.register(root);

        for (int i = 1; i < commandNames.size(); i++) {
            var aliasNode = Commands.literal(commandNames.get(i))
                    .redirect(builtRoot);

            dispatcher.register(aliasNode);
        }
    }

    ////////////////
    // PER PLAYER //
    ////////////////
    private ArgumentBuilder<CommandSourceStack, ?> perPlayer$root(CommandBuildContext buildCtx) {
        return Commands.literal("perPlayer")
                .executes(ctx -> this.perPlayer$printStatus(ctx, ctx.getSource().getPlayerOrException()));
    }

    private int perPlayer$printStatus(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        if (!LenientDeathConfig.INSTANCE.get().preserveItemsOnDeath.perPlayer.enabled) {
            ctx.getSource().sendFailure(error(translatable("lenientdeath.command.perPlayer.disabled")));
            return 0;
        }
        var perPlayerStatus = ((LenientDeathServerPlayerDuck) player).lenientdeath$isPerPlayerEnabled();
        ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                variable(player.getDisplayName().getString()),
                symbol(": "),
                text(translatable(perPlayerStatus ? "lenientdeath.command.perPlayer.playerEnabled" : "lenientdeath.command.perPlayer.playerDisabled"))
        ), false);
        return 1;
    }
}
