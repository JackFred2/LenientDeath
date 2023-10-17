package red.jackf.lenientdeath.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import red.jackf.lenientdeath.config.LenientDeathConfig;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("ExtractMethodRecommender")
public class CommandConfig {
    private CommandConfig() {}

    protected static final Predicate<CommandSourceStack> CHANGE_CONFIG_PREDICATE = Permissions.require(
            PermissionKeys.CONFIG,
            4
    );

    private static LenientDeathConfig getConfig() {
        return LenientDeathConfig.INSTANCE.get();
    }

    private static void verifyAndSave() {
        getConfig().verify();
        LenientDeathConfig.INSTANCE.save();
    }

    //////////////
    // BUILDERS //
    //////////////
    private static LiteralArgumentBuilder<CommandSourceStack> makeBoolean(String name,
                                                                          String fullName,
                                                                          Function<LenientDeathConfig, Boolean> get,
                                                                          BiConsumer<LenientDeathConfig, Boolean> set) {
        return Commands.literal(name)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                    CommandFormatting.variable(fullName),
                    CommandFormatting.symbol(": "),
                    CommandFormatting.bool(get.apply(getConfig()))
                ), false);

                return 1;
            }).then(Commands.literal("true")
                .executes(ctx -> {
                    if (get.apply(getConfig())) {
                        ctx.getSource().sendFailure(CommandFormatting.info(
                            CommandFormatting.variable(fullName),
                            CommandFormatting.symbol(": "),
                            CommandFormatting.bool(true),
                            CommandFormatting.symbol(" "),
                            CommandFormatting.text(Component.translatable("lenientdeath.command.config.unchanged"))
                        ));

                        return 0;
                    } else {
                        set.accept(getConfig(), true);
                        verifyAndSave();
                        ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                            CommandFormatting.variable(fullName),
                            CommandFormatting.symbol(": "),
                            CommandFormatting.bool(false),
                            CommandFormatting.symbol(" -> "),
                            CommandFormatting.bool(true)
                        ), true);

                        return 1;
                    }
                }
            )).then(Commands.literal("false")
                .executes(ctx -> {
                    if (!get.apply(getConfig())) {
                        ctx.getSource().sendFailure(CommandFormatting.info(
                            CommandFormatting.variable(fullName),
                            CommandFormatting.symbol(": "),
                            CommandFormatting.bool(false),
                            CommandFormatting.symbol(" "),
                            CommandFormatting.text(Component.translatable("lenientdeath.command.config.unchanged"))
                        ));

                        return 0;
                    } else {
                        set.accept(getConfig(), false);
                        verifyAndSave();
                        ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                            CommandFormatting.variable(fullName),
                            CommandFormatting.symbol(": "),
                            CommandFormatting.bool(true),
                            CommandFormatting.symbol(" -> "),
                            CommandFormatting.bool(false)
                        ), true);

                        return 1;
                    }
                }
            ));
    }

    private static <E extends Enum<E>> LiteralArgumentBuilder<CommandSourceStack> makeEnum(String name,
                                                                          String fullName,
                                                                          Class<E> enumClass,
                                                                          Function<LenientDeathConfig, E> get,
                                                                          BiConsumer<LenientDeathConfig, E> set) {
        var node = Commands.literal(name)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                    CommandFormatting.variable(fullName),
                    CommandFormatting.symbol(": "),
                    CommandFormatting.variable(get.apply(getConfig()).name())
                ), false);

                return 1;
            });

        for (E constant : enumClass.getEnumConstants()) {
            node.then(Commands.literal(constant.name())
                .executes(ctx -> {
                    var old = get.apply(getConfig());
                    if (old == constant) {
                        ctx.getSource().sendFailure(CommandFormatting.info(
                            CommandFormatting.variable(fullName),
                            CommandFormatting.symbol(": "),
                            CommandFormatting.variable(constant.name()),
                            CommandFormatting.symbol(" "),
                            CommandFormatting.text(Component.translatable("lenientdeath.command.config.unchanged"))
                        ));

                        return 0;
                    } else {
                        set.accept(getConfig(), constant);
                        verifyAndSave();
                        ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                            CommandFormatting.variable(fullName),
                            CommandFormatting.symbol(": "),
                            CommandFormatting.variable(old.name()),
                            CommandFormatting.symbol(" -> "),
                            CommandFormatting.variable(constant.name())
                        ), true);

                        return 1;
                    }
                })
            );
        }

        return node;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> makeIntRange(String name,
                                                                           String fullName,
                                                                           int min,
                                                                           int max,
                                                                           Function<LenientDeathConfig, Integer> get,
                                                                           BiConsumer<LenientDeathConfig, Integer> set) {
        return Commands.literal(name)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                    CommandFormatting.variable(fullName),
                    CommandFormatting.symbol(": "),
                    CommandFormatting.variable(String.valueOf(get.apply(getConfig())))
                ), false);

                return 1;
            }).then(Commands.argument(name, IntegerArgumentType.integer(min, max))
                .executes(ctx -> {
                    var old = get.apply(getConfig());
                    var newValue = IntegerArgumentType.getInteger(ctx, name);
                    if (old == newValue) {
                        ctx.getSource().sendFailure(CommandFormatting.info(
                            CommandFormatting.variable(fullName),
                            CommandFormatting.symbol(": "),
                            CommandFormatting.variable(String.valueOf(newValue)),
                            CommandFormatting.symbol(" "),
                            CommandFormatting.text(Component.translatable("lenientdeath.command.config.unchanged"))
                        ));

                        return 0;
                    } else {
                        set.accept(getConfig(), newValue);
                        verifyAndSave();
                        ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                            CommandFormatting.variable(fullName),
                            CommandFormatting.symbol(": "),
                            CommandFormatting.variable(String.valueOf(old)),
                            CommandFormatting.symbol(" -> "),
                            CommandFormatting.variable(String.valueOf(newValue))
                        ), true);

                        return 1;
                    }
                })
            );
    }

    ///////////
    // NODES //
    ///////////

    static LiteralArgumentBuilder<CommandSourceStack> createCommandNode(CommandBuildContext context) {
        var root = Commands.literal("config")
                .requires(CHANGE_CONFIG_PREDICATE);

        root.then(createConfigNode());
        root.then(createMetaNode());
        root.then(createPerPlayerNode());
        root.then(createDroppedItemGlowNode());
        root.then(createExtendedDeathItemLifetime());

        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createConfigNode() {
        return Commands.literal("config")
                .then(makeBoolean("enableFileWatcher",
                                  "config.enableFileWatcher",
                                  config -> config.config.enableFileWatcher,
                                  (config, newVal) -> config.config.enableFileWatcher = newVal))
                .then(makeBoolean("stripComments",
                                  "config.stripComments",
                                  config -> config.config.stripComments,
                                  (config, newVal) -> config.config.stripComments = newVal));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createMetaNode() {
        var root = Commands.literal("command");

        var names = Commands.literal("commandNames");

        names.executes(ctx -> {
            ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                CommandFormatting.variable("command.commandNames"),
                CommandFormatting.symbol(":")
            ), false);
            for (String name : getConfig().command.commandNames) {
                ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                    CommandFormatting.symbol(" • "),
                    CommandFormatting.variable(name)
                ), false);
            }

            return 1;
        });

        var add = Commands.literal("add")
            .then(Commands.argument("commandName", StringArgumentType.word())
                .executes(ctx -> {
                    var name = StringArgumentType.getString(ctx, "commandName");
                    var config = getConfig().command;
                    if (config.commandNames.contains(name)) {
                        ctx.getSource().sendFailure(CommandFormatting.error(
                            CommandFormatting.variable("command.commandNames"),
                            CommandFormatting.symbol(": "),
                            CommandFormatting.text(
                                Component.translatable("lenientdeath.command.config.alreadyInList",
                                    CommandFormatting.variable(name).resolve(CommandFormatting.TextType.BLANK)))
                        ));
                        return 0;
                    } else {
                        config.commandNames.add(name);
                        ctx.getSource().sendSuccess(() -> CommandFormatting.success(
                            CommandFormatting.variable("command.commandNames"),
                            CommandFormatting.symbol(": "),
                            CommandFormatting.text(
                                Component.translatable("lenientdeath.command.config.added",
                                    CommandFormatting.variable(name).resolve(CommandFormatting.TextType.BLANK)))
                        ), true);
                        ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                                Component.translatable("lenientdeath.command.config.requiresWorldReload")
                        ), false);
                        verifyAndSave();
                        return 1;
                    }
                })
            );

        var remove = Commands.literal("remove")
            .then(Commands.argument("commandName", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(getConfig().command.commandNames, builder))
                .executes(ctx -> {
                    var name = StringArgumentType.getString(ctx, "commandName");
                    var config = getConfig().command;
                    if (!config.commandNames.contains(name)) {
                        ctx.getSource().sendFailure(CommandFormatting.error(
                            CommandFormatting.variable("command.commandNames"),
                            CommandFormatting.symbol(": "),
                            CommandFormatting.text(
                                Component.translatable("lenientdeath.command.config.notInList",
                                    CommandFormatting.variable(name).resolve(CommandFormatting.TextType.BLANK)))
                        ));
                        return 0;
                    } else {
                        config.commandNames.remove(name);
                        ctx.getSource().sendSuccess(() -> CommandFormatting.success(
                            CommandFormatting.variable("command.commandNames"),
                            CommandFormatting.symbol(": "),
                            CommandFormatting.text(
                                Component.translatable("lenientdeath.command.config.removed",
                                    CommandFormatting.variable(name).resolve(CommandFormatting.TextType.BLANK)))
                        ), true);
                        ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                                Component.translatable("lenientdeath.command.config.requiresWorldReload")
                        ), false);
                        verifyAndSave();
                        return 1;
                    }
                })
            );

        names.then(add);
        names.then(remove);

        root.then(names);

        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createPerPlayerNode() {
        return Commands.literal("perPlayer")
            .then(makeBoolean("defaultEnabledForPlayer",
                "perPlayer.defaultEnabledForPlayer",
                config -> config.perPlayer.defaultEnabledForPlayer,
                (config, newVal) -> config.perPlayer.defaultEnabledForPlayer = newVal))
            .then(makeBoolean("playersCanChangeTheirOwnSetting",
                "perPlayer.playersCanChangeTheirOwnSetting",
                config -> config.perPlayer.playersCanChangeTheirOwnSetting,
                (config, newVal) -> config.perPlayer.playersCanChangeTheirOwnSetting = newVal));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createDroppedItemGlowNode() {
        return Commands.literal("droppedItemGlow")
                .then(makeBoolean("enabled",
                                  "droppedItemGlow.enabled",
                                  config -> config.droppedItemGlow.enabled,
                                  (config, newVal) -> config.droppedItemGlow.enabled = newVal))
                .then(makeEnum("glowVisibility",
                               "droppedItemGlow.glowVisibility",
                               LenientDeathConfig.DroppedItemGlow.Visibility.class,
                               config -> config.droppedItemGlow.glowVisibility,
                               (config, newVal) -> config.droppedItemGlow.glowVisibility = newVal));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createExtendedDeathItemLifetime() {
        return Commands.literal("extendedDeathItemLifetime")
                .then(makeBoolean("enabled",
                                  "extendedDeathItemLifetime.enabled",
                                  config -> config.extendedDeathItemLifetime.enabled,
                                  (config, newVal) -> config.extendedDeathItemLifetime.enabled = newVal))
                .then(makeIntRange("deathDropItemLifetimeSeconds",
                                   "extendedDeathItemLifetime.deathDropItemLifetimeSeconds",
                                   0,
                                   1800,
                                   config -> config.extendedDeathItemLifetime.deathDropItemLifetimeSeconds,
                                   (config, newVal) -> config.extendedDeathItemLifetime.deathDropItemLifetimeSeconds = newVal))
                .then(makeBoolean("deathDropItemsNeverDespawn",
                                  "extendedDeathItemLifetime.deathDropItemsNeverDespawn",
                                  config -> config.extendedDeathItemLifetime.deathDropItemsNeverDespawn,
                                  (config, newVal) -> config.extendedDeathItemLifetime.deathDropItemsNeverDespawn = newVal));
    }
}
