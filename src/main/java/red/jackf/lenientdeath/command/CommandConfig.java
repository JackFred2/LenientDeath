package red.jackf.lenientdeath.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import red.jackf.lenientdeath.config.LenientDeathConfig;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

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
                })
            );
    }

    static ArgumentBuilder<CommandSourceStack, ?> createCommandNode(CommandBuildContext context) {
        var root = Commands.literal("config")
                .requires(CHANGE_CONFIG_PREDICATE);

        root.then(createMetaNode());

        return root;
    }

    static ArgumentBuilder<CommandSourceStack, ?> createMetaNode() {
        var root = Commands.literal("config")
                .then(makeBoolean("enableFileWatcher",
                                  "config.enableFileWatcher",
                                  config -> config.config.enableFileWatcher,
                                  (config, newVal) -> config.config.enableFileWatcher = newVal))
                .then(makeBoolean("stripComments",
                                  "config.stripComments",
                                  config -> config.config.stripComments,
                                  (config, newVal) -> config.config.stripComments = newVal));

        return root;
    }
}
