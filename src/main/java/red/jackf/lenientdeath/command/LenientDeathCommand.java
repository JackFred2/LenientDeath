package red.jackf.lenientdeath.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
                .executes(ctx -> this.perPlayer$printStatus(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("check")
                      .requires(ctx -> ctx.hasPermission(4))
                      .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> this.perPlayer$printStatus(ctx, EntityArgument.getPlayer(ctx, "player")))
                      )
                ).then(Commands.literal("enable")
                               .executes(ctx -> this.perPlayer$enable(ctx, ctx.getSource().getPlayerOrException()))
                               .then(Commands.argument("player", EntityArgument.player())
                                             .requires(ctx -> ctx.hasPermission(4))
                                             .executes(ctx -> this.perPlayer$enable(ctx, EntityArgument.getPlayer(ctx, "player")))
                               )
                ).then(Commands.literal("disable")
                               .executes(ctx -> this.perPlayer$disable(ctx, ctx.getSource().getPlayerOrException()))
                               .then(Commands.argument("player", EntityArgument.player())
                                             .requires(ctx -> ctx.hasPermission(4))
                                             .executes(ctx -> this.perPlayer$disable(ctx, EntityArgument.getPlayer(ctx, "player")))
                               )
                );
    }

    private int perPlayer$enable(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        return 0;
    }

    private int perPlayer$disable(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        return 0;
    }

    private int perPlayer$printStatus(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        var config = LenientDeathConfig.INSTANCE.get().preserveItemsOnDeath.perPlayer;

        if (!config.enabled) {
            ctx.getSource().sendFailure(error(translatable("lenientdeath.command.perPlayer.disabled")));
            return 0;
        }
        boolean perPlayerStatus = ((LenientDeathServerPlayerDuck) player).lenientdeath$isPerPlayerEnabled();
        ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                variable(player.getDisplayName().getString()),
                symbol(": "),
                text(translatable(perPlayerStatus ? "lenientdeath.command.perPlayer.playerEnabled" : "lenientdeath.command.perPlayer.playerDisabled"))
        ), false);

        boolean isShownPlayerCommandSource = player == ctx.getSource().getPlayer();
        boolean shouldShowModifyButtons = (isShownPlayerCommandSource && config.playersCanChangeTheirOwnSetting) || ctx.getSource().hasPermission(4);

        System.out.println(ctx.getRootNode());

        if (shouldShowModifyButtons) {
            String baseCommand = "/ld perPlayer ";
            String playerSuffix = isShownPlayerCommandSource ? "" : " " + player.getDisplayName().getString();
            Style disableStyle = suggests(Style.EMPTY.withColor(ERROR_COLOUR), baseCommand + "disable" + playerSuffix);
            MutableComponent disableButton = Component.empty().withStyle(disableStyle)
                    .append("[")
                    .append(translatable("lenientdeath.command.perPlayer.disableButton"))
                    .append("]");

            Style enableStyle = suggests(Style.EMPTY.withColor(SUCCESS_COLOUR), baseCommand + "enable" + playerSuffix);
            MutableComponent enableButton = Component.empty().withStyle(enableStyle)
                    .append("[")
                    .append(translatable("lenientdeath.command.perPlayer.enableButton"))
                    .append("]");

            ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                    text(disableButton),
                    symbol(" "),
                    text(enableButton)
            ), false);
        }

        return 1;
    }
}
