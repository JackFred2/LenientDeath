package red.jackf.lenientdeath.command.subcommand;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import red.jackf.lenientdeath.mixinutil.LDServerPlayerDuck;
import red.jackf.lenientdeath.PermissionKeys;
import red.jackf.lenientdeath.command.CommandFormatting;
import red.jackf.lenientdeath.command.LenientDeathCommand;
import red.jackf.lenientdeath.command.PermissionsExt;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.config.LenientDeathConfig.PerPlayerEnabled;

import java.util.function.Predicate;

import static net.minecraft.network.chat.Component.translatable;
import static red.jackf.lenientdeath.command.CommandFormatting.*;

public class PerPlayer {
    private PerPlayer() {}

    private static boolean isEnabled() {
        var config = LenientDeathConfig.INSTANCE.get();
        return config.preserveExperienceOnDeath.enabled == PerPlayerEnabled.per_player || config.preserveItemsOnDeath.enabled == PerPlayerEnabled.per_player;
    }

    private static boolean playersCanChangeTheirOwnSetting() {
        return LenientDeathConfig.INSTANCE.get().perPlayer.playersCanChangeTheirOwnSetting;
    }

    private static final Predicate<CommandSourceStack> CHANGE_OTHERS_PREDICATE = Permissions.require(
            PermissionKeys.PER_PLAYER_CHANGE_OTHERS,
            4
    ).or(LenientDeathCommand.IS_INTEGRATED_HOST_PREDICATE);

    private static final Predicate<CommandSourceStack> CHANGE_SELF_PREDICATE = PermissionsExt.requireBool(
            PermissionKeys.PER_PLAYER_CHANGE_SELF,
            PerPlayer::playersCanChangeTheirOwnSetting
    ).or(CHANGE_OTHERS_PREDICATE);

    private static final Predicate<CommandSourceStack> CHECK_OTHERS_PREDICATE = Permissions.require(
            PermissionKeys.PER_PLAYER_CHECK_OTHERS,
            4
    ).or(CHANGE_OTHERS_PREDICATE);

    public static final Predicate<CommandSourceStack> CHECK_SELF_PREDICATE = Permissions.require(
            PermissionKeys.PER_PLAYER_CHECK_SELF,
            true
    ).or(CHECK_OTHERS_PREDICATE).or(CHANGE_SELF_PREDICATE);

    public static LiteralArgumentBuilder<CommandSourceStack> createCommandNode() {
        return Commands.literal("perPlayer")
            .requires(stack -> isEnabled() && CHECK_SELF_PREDICATE.test(stack)) // all other permissions require check self
            .then(Commands.literal("check")
                .requires(CHECK_SELF_PREDICATE)
                .executes(ctx -> checkFor(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(CHECK_OTHERS_PREDICATE)
                    .executes(ctx -> checkFor(ctx, EntityArgument.getPlayer(ctx, "player")))
                )
            ).then(Commands.literal("enable")
                .requires(CHANGE_SELF_PREDICATE)
                .executes(ctx -> enableFor(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(CHANGE_OTHERS_PREDICATE)
                    .executes(ctx -> enableFor(ctx, EntityArgument.getPlayer(ctx, "player")))
                )
            ).then(Commands.literal("disable")
                .requires(CHANGE_SELF_PREDICATE)
                .executes(ctx -> disableFor(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(CHANGE_OTHERS_PREDICATE)
                    .executes(ctx -> disableFor(ctx, EntityArgument.getPlayer(ctx, "player")))
                )
            );
    }

    private static boolean canChangeSettingFor(CommandContext<CommandSourceStack> ctx, ServerPlayer targetPlayer) {
        if (ctx.getSource().getPlayer() == targetPlayer) {
            return CHANGE_SELF_PREDICATE.test(ctx.getSource());
        } else {
            return CHANGE_OTHERS_PREDICATE.test(ctx.getSource());
        }
    }

    private static int enableFor(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        if (!canChangeSettingFor(ctx, player)) {
            if (ctx.getSource().getPlayer() == player) {
                ctx.getSource().sendFailure(CommandFormatting.error(translatable("lenientdeath.command.perPlayer.noPermissionToChangeSelf")));
            } else {
                ctx.getSource().sendFailure(CommandFormatting.error(translatable("lenientdeath.command.perPlayer.noPermissionToChangeOthers")));
            }
            return 0;
        }

        if (LDServerPlayerDuck.isHandledByPermission(player)) {
            ctx.getSource().sendFailure(CommandFormatting.error(
                translatable("lenientdeath.command.perPlayer.handledByPermissions",
                    CommandFormatting.player(player).resolve(TextType.BLANK))));

            return 0;
        }

        boolean isEnabled = ((LDServerPlayerDuck) player).lenientdeath$isPerPlayerEnabled();
        if (isEnabled) {
            ctx.getSource().sendFailure(CommandFormatting.info(
                variable(player.getDisplayName().getString()),
                symbol(": "),
                text(translatable("lenientdeath.command.perPlayer.setPlayerAlreadyEnabled"))
            ));
            return 0;
        }

        ((LDServerPlayerDuck) player).lenientdeath$setPerPlayerEnabled(true);

        Component message = CommandFormatting.success(
                variable(player.getDisplayName().getString()),
                symbol(": "),
                text(translatable("lenientdeath.command.perPlayer.setPlayerEnabled"))
        );
        ctx.getSource().sendSuccess(() -> message, true);
        if (ctx.getSource().getPlayer() != player) player.sendSystemMessage(message);
        return 1;
    }

    private static int disableFor(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        if (!canChangeSettingFor(ctx, player)) {
            if (ctx.getSource().getPlayer() == player) {
                ctx.getSource().sendFailure(CommandFormatting.error(translatable("lenientdeath.command.perPlayer.noPermissionToChangeSelf")));
            } else {
                ctx.getSource().sendFailure(CommandFormatting.error(translatable("lenientdeath.command.perPlayer.noPermissionToChangeOthers")));
            }
            return 0;
        }

        if (LDServerPlayerDuck.isHandledByPermission(player)) {
            ctx.getSource().sendFailure(CommandFormatting.error(
                translatable("lenientdeath.command.perPlayer.handledByPermissions",
                    CommandFormatting.player(player).resolve(TextType.BLANK))));

            return 0;
        }

        boolean isEnabled = ((LDServerPlayerDuck) player).lenientdeath$isPerPlayerEnabled();
        if (!isEnabled) {
            ctx.getSource().sendFailure(CommandFormatting.info(
                    variable(player.getDisplayName().getString()),
                    symbol(": "),
                    text(translatable("lenientdeath.command.perPlayer.setPlayerAlreadyDisabled"))
            ));
            return 0;
        }

        ((LDServerPlayerDuck) player).lenientdeath$setPerPlayerEnabled(false);

        Component message = CommandFormatting.error(
                variable(player.getDisplayName().getString()),
                symbol(": "),
                text(translatable("lenientdeath.command.perPlayer.setPlayerDisabled"))
        );
        ctx.getSource().sendSuccess(() -> message, true);
        if (ctx.getSource().getPlayer() != player) player.sendSystemMessage(message);
        return 1;
    }

    private static int checkFor(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        boolean perPlayerStatus = LDServerPlayerDuck.isEnabledFor(player);
        ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                variable(player.getDisplayName().getString()),
                symbol(": "),
                text(translatable(perPlayerStatus ? "lenientdeath.command.perPlayer.playerEnabled" : "lenientdeath.command.perPlayer.playerDisabled"))
        ), false);

        boolean isShownPlayerCommandSource = player == ctx.getSource().getPlayer();
        if (LDServerPlayerDuck.isHandledByPermission(player)) {
            ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                translatable("lenientdeath.command.perPlayer.handledByPermissions",
                    CommandFormatting.player(player).resolve(TextType.BLANK))
            ), false);
        } else {
            boolean shouldShowModifyButtons = canChangeSettingFor(ctx, player)
                    && ctx.getSource().isPlayer();

            if (shouldShowModifyButtons) {
                String rootCommand = ctx.getInput().split(" ", 2)[0];
                String baseCommand = "/" + rootCommand + " perPlayer ";

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
        }

        return 1;
    }
}
