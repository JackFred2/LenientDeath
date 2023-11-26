package red.jackf.lenientdeath.command.subcommand;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.PermissionKeys;
import red.jackf.lenientdeath.command.Formatting;
import red.jackf.lenientdeath.command.LenientDeathCommand;
import red.jackf.lenientdeath.command.PermissionsExt;
import red.jackf.lenientdeath.config.LenientDeathConfig.PerPlayerEnabled;
import red.jackf.lenientdeath.mixinutil.LDPerPlayer;

import java.util.function.Predicate;

import static net.minecraft.network.chat.Component.translatable;

public class PerPlayer {
    private PerPlayer() {}

    private static boolean isEnabled() {
        var config = LenientDeath.CONFIG.instance();
        return config.preserveExperienceOnDeath.enabled == PerPlayerEnabled.per_player || config.preserveItemsOnDeath.enabled == PerPlayerEnabled.per_player;
    }

    private static boolean playersCanChangeTheirOwnSetting() {
        return LenientDeath.CONFIG.instance().perPlayer.playersCanChangeTheirOwnSetting;
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
                ctx.getSource().sendFailure(Formatting.errorLine(
                        translatable("lenientdeath.command.perPlayer.noPermissionToChangeSelf")
                ));
            } else {
                ctx.getSource().sendFailure(Formatting.errorLine(
                        translatable("lenientdeath.command.perPlayer.noPermissionToChangeOthers")
                ));
            }
            return 0;
        }

        if (LDPerPlayer.isHandledByPermission(player)) {
            ctx.getSource().sendFailure(Formatting.errorLine(
                    translatable("lenientdeath.command.perPlayer.handledByPermissions",
                                 Formatting.player(player))
            ));

            return 0;
        }

        boolean isEnabled = ((LDPerPlayer) player).lenientdeath$isPerPlayerEnabled();
        if (isEnabled) {
            ctx.getSource().sendFailure(Formatting.infoLine(
                    translatable("lenientdeath.command.perPlayer.setPlayerAlreadyEnabled",
                                 Formatting.player(player))
            ));
            return 0;
        }

        ((LDPerPlayer) player).lenientdeath$setPerPlayerEnabled(true);

        var message = Formatting.successLine(
                translatable("lenientdeath.command.perPlayer.setPlayerEnabled",
                             Formatting.player(player))
        );

        ctx.getSource().sendSuccess(message, true);
        if (ctx.getSource().getPlayer() != player) player.sendSystemMessage(message);
        return 1;
    }

    private static int disableFor(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        if (!canChangeSettingFor(ctx, player)) {
            if (ctx.getSource().getPlayer() == player) {
                ctx.getSource().sendFailure(Formatting.errorLine(
                        translatable("lenientdeath.command.perPlayer.noPermissionToChangeSelf")
                ));
            } else {
                ctx.getSource().sendFailure(Formatting.errorLine(
                        translatable("lenientdeath.command.perPlayer.noPermissionToChangeOthers")
                ));
            }
            return 0;
        }

        if (LDPerPlayer.isHandledByPermission(player)) {
            ctx.getSource().sendFailure(Formatting.errorLine(
                    translatable("lenientdeath.command.perPlayer.handledByPermissions",
                                 Formatting.player(player))
            ));

            return 0;
        }

        boolean isEnabled = ((LDPerPlayer) player).lenientdeath$isPerPlayerEnabled();
        if (!isEnabled) {
            ctx.getSource().sendFailure(Formatting.infoLine(
                    translatable("lenientdeath.command.perPlayer.setPlayerAlreadyDisabled",
                                 Formatting.player(player))
            ));
            return 0;
        }

        ((LDPerPlayer) player).lenientdeath$setPerPlayerEnabled(false);

        var message = Formatting.errorLine(
                translatable("lenientdeath.command.perPlayer.setPlayerDisabled",
                             Formatting.player(player))
        );

        ctx.getSource().sendSuccess(message, true);
        if (ctx.getSource().getPlayer() != player) player.sendSystemMessage(message);
        return 1;
    }

    private static int checkFor(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        boolean perPlayerStatus = LDPerPlayer.isEnabledFor(player);

        if (perPlayerStatus) {
            ctx.getSource().sendSuccess(Formatting.successLine(
                    translatable("lenientdeath.command.perPlayer.playerEnabled",
                                 Formatting.player(player))
            ), false);
        } else {
            ctx.getSource().sendSuccess(Formatting.errorLine(
                    translatable("lenientdeath.command.perPlayer.playerDisabled",
                                 Formatting.player(player))
            ), false);
        }

        boolean isShownPlayerCommandSource = player == ctx.getSource().getPlayer();
        if (LDPerPlayer.isHandledByPermission(player)) {
            ctx.getSource().sendSuccess(Formatting.infoLine(
                translatable("lenientdeath.command.perPlayer.handledByPermissions",
                    Formatting.player(player))
            ), false);
        } else {
            boolean shouldShowModifyButtons = canChangeSettingFor(ctx, player) && ctx.getSource().isPlayer();

            if (shouldShowModifyButtons) {
                String rootCommand = ctx.getInput().split(" ", 2)[0];
                String baseCommand = "/" + rootCommand + " perPlayer ";

                String playerSuffix = isShownPlayerCommandSource ? "" : " " + player.getDisplayName().getString();

                Component disableButton = Formatting.commandButton(Formatting.ERROR,
                                                                   translatable("lenientdeath.command.perPlayer.disableButton"),
                                                                   baseCommand + "disable" + playerSuffix);

                Component enableButton = Formatting.commandButton(Formatting.SUCCESS,
                                                                   translatable("lenientdeath.command.perPlayer.enableButton"),
                                                                   baseCommand + "enable" + playerSuffix);

                ctx.getSource().sendSuccess(Formatting.infoLine(
                        Component.empty()
                                 .append(disableButton)
                                 .append(CommonComponents.SPACE)
                                 .append(enableButton)
                ), false);
            }
        }

        return 1;
    }
}
