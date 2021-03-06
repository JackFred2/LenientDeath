package red.jackf.lenientdeath;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import red.jackf.lenientdeath.utils.DatapackGenerator;
import red.jackf.lenientdeath.utils.LenientDeathPerPlayerMixinInterface;

import static red.jackf.lenientdeath.LenientDeath.*;

public class LenientDeathCommand {
    private static final Formatting SUCCESS = Formatting.GREEN;
    private static final Formatting INFO = Formatting.YELLOW;
    private static final Formatting ERROR = Formatting.RED;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        var rootNode = CommandManager.literal("ld")
            .executes(ctx -> {
                if (CONFIG.perPlayer) {
                    ctx.getSource().sendFeedback(Text.translatable("lenientdeath.command.perPlayer.personal." + (((LenientDeathPerPlayerMixinInterface) ctx.getSource().getPlayer()).isItemSavingEnabled() ? "enabled" : "disabled").formatted(INFO)), false);
                } else {
                    ctx.getSource().sendFeedback(Text.translatable("lenientdeath.command.perPlayer.isDisabled").formatted(INFO), false);
                }
                return 1;
            })
            .build();

        var resetErroredTagsNode = CommandManager.literal("erroredTags")
            .executes(context -> {
                context.getSource().sendFeedback(Text.translatable("lenientdeath.command.listErroredTags").formatted(INFO), false);
                ERRORED_TAGS.forEach(str -> context.getSource().sendFeedback(Text.literal(" - " + str), false));
                return 1;
            })
            .then(CommandManager.literal("reset")
                .executes(context -> {
                    ERRORED_TAGS.clear();
                    context.getSource().sendFeedback(Text.translatable("lenientdeath.command.resetErroredTags").formatted(SUCCESS), true);
                    return 1;
                })
            )
            .requires(source -> source.hasPermissionLevel(4))
            .build();

        var autoDetectNode = CommandManager.literal("autoDetect")
            .executes(ctx -> updateAutoDetect(ctx, true))
            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                .requires(source -> source.hasPermissionLevel(4))
                .executes(ctx -> updateAutoDetect(ctx, false)))
            .build();

        var trinketsNode = CommandManager.literal("trinketsSafe")
            .executes(ctx -> updateTrinketsSafe(ctx, true))
            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                .requires(source -> source.hasPermissionLevel(4))
                .executes(ctx -> updateTrinketsSafe(ctx, false)))
            .build();

        var perPlayerNode = CommandManager.literal("perPlayer")
            .executes(ctx -> updatePerPlayer(ctx, true))
            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                .requires(source -> source.hasPermissionLevel(4))
                .executes(ctx -> updatePerPlayer(ctx, false)))
            .build();

        var enablePerPlayerNode = CommandManager.literal("enableForMe")
            .executes(ctx -> {
                ((LenientDeathPerPlayerMixinInterface) ctx.getSource().getPlayer()).setItemSavingEnabled(true);
                if (CONFIG.perPlayer) {
                    ctx.getSource().sendFeedback(Text.translatable("lenientdeath.command.perPlayer.personal.nowEnabled").formatted(SUCCESS), false);
                } else {
                    ctx.getSource().sendFeedback(Text.translatable("lenientdeath.command.perPlayer.isDisabled").formatted(ERROR), false);
                }
                return 1;
            })
            .build();

        var disablePerPlayerNode = CommandManager.literal("disableForMe")
            .executes(ctx -> {
                ((LenientDeathPerPlayerMixinInterface) ctx.getSource().getPlayer()).setItemSavingEnabled(false);
                if (CONFIG.perPlayer) {
                    ctx.getSource().sendFeedback(Text.translatable("lenientdeath.command.perPlayer.personal.nowDisabled").formatted(SUCCESS), false);
                } else {
                    ctx.getSource().sendFeedback(Text.translatable("lenientdeath.command.perPlayer.isDisabled").formatted(ERROR), false);
                }
                return 1;
            })
            .build();

        var generateNode = CommandManager.literal("generate")
            .requires(source -> source.hasPermissionLevel(4))
            .executes(LenientDeathCommand::generateDatapack)
            .build();

        var listNode = CommandManager.literal("list")
            .executes(LenientDeathCommand::listFilters)
            .build();

        var listTagItems = CommandManager.literal("listTagItems")
            .then(CommandManager.argument("tag", StringArgumentType.greedyString()).suggests((context, builder) ->
                CommandSource.suggestMatching(Registry.ITEM.streamTags()
                    .map(tagKey -> "#" + tagKey.id().toString()), builder)
                ).executes(LenientDeathCommand::listTagItems).build())
            .requires(source -> source.hasPermissionLevel(4))
            .build();

        var addNode = CommandManager.literal("add")
            .then(CommandManager.literal("hand")
                .executes(LenientDeathCommand::addHand).build()
            ).then(CommandManager.literal("item")
                .then(CommandManager.argument("item", StringArgumentType.greedyString()).suggests((context, builder) ->
                    CommandSource.suggestMatching(Registry.ITEM.getIds().stream().map(Identifier::toString).filter(id -> !CONFIG.items.contains(id)), builder)
                ).executes(LenientDeathCommand::addItem).build())
            ).then(CommandManager.literal("tag")
                .then(CommandManager.argument("tag", StringArgumentType.greedyString()).suggests((context, builder) ->
                    CommandSource.suggestMatching(Registry.ITEM.streamTags().map(tagKey -> tagKey.id().toString()).filter(id -> !CONFIG.tags.contains(id)), builder)
                ).executes(LenientDeathCommand::addTag).build())
            )
            .requires(source -> source.hasPermissionLevel(4))
            .build();

        var removeNode = CommandManager.literal("remove")
            .then(CommandManager.literal("hand")
                .executes(LenientDeathCommand::removeHand).build()
            ).then(CommandManager.literal("item")
                .then(CommandManager.argument("item", StringArgumentType.greedyString()).suggests((context, builder) ->
                    CommandSource.suggestMatching(CONFIG.items.stream(), builder)
                ).executes(LenientDeathCommand::removeItem).build())
            ).then(CommandManager.literal("tag")
                .then(CommandManager.argument("tag", StringArgumentType.greedyString()).suggests((context, builder) ->
                    CommandSource.suggestMatching(CONFIG.tags.stream(), builder)
                ).executes(LenientDeathCommand::removeTag).build())
            )
            .requires(source -> source.hasPermissionLevel(4))
            .build();

        dispatcher.getRoot().addChild(rootNode);
        rootNode.addChild(generateNode);
        rootNode.addChild(resetErroredTagsNode);
        rootNode.addChild(listTagItems);
        rootNode.addChild(listNode);
        rootNode.addChild(addNode);
        rootNode.addChild(removeNode);
        rootNode.addChild(autoDetectNode);
        rootNode.addChild(trinketsNode);
        rootNode.addChild(perPlayerNode);
        rootNode.addChild(enablePerPlayerNode);
        rootNode.addChild(disablePerPlayerNode);
    }

    private static int listTagItems(CommandContext<ServerCommandSource> context) {
        var arg = context.getArgument("tag", String.class);
        if (arg.charAt(0) == '#') arg = arg.substring(1); // strip #
        var id = Identifier.tryParse(arg);
        if (id == null) {
            context.getSource().sendFeedback(Text.translatable("lenientdeath.command.error.unknownIdentifier", arg).formatted(ERROR), false);
            return 0;
        } else {
            var tag = Registry.ITEM.streamTagsAndEntries().filter(key -> key.getFirst().id().equals(id)).findFirst();
            if (tag.isEmpty()) {
                context.getSource().sendFeedback(Text.translatable("lenientdeath.command.error.unknownTag", "#" + arg).formatted(ERROR), false);
                return 0;
            } else {
                context.getSource().sendFeedback(Text.translatable("lenientdeath.command.listTagItems", "#" + arg).formatted(INFO), false);
                tag.get().getSecond().stream().forEach(itemEntry -> {
                    context.getSource().sendFeedback(Text.literal(" - " + Registry.ITEM.getId(itemEntry.value())), false);
                });
            }
        }
        return 1;
    }

    private static int updateAutoDetect(CommandContext<ServerCommandSource> context, boolean query) {
        if (query) {
            context.getSource().sendFeedback(Text.translatable(
                "lenientdeath.command.autoDetect." + (CONFIG.detectAutomatically ? "isEnabled" : "isDisabled")
            ).formatted(INFO), false);
        } else {
            var newValue = context.getArgument("enabled", Boolean.class);
            CONFIG.detectAutomatically = newValue;
            LenientDeath.saveConfig();
            context.getSource().sendFeedback(Text.translatable(
                "lenientdeath.command.autoDetect." + (newValue ? "enabled" : "disabled")
            ).formatted(SUCCESS), true);
        }

        return 1;
    }

    private static int updateTrinketsSafe(CommandContext<ServerCommandSource> context, boolean query) {
        if (query) {
            context.getSource().sendFeedback(Text.translatable(
                "lenientdeath.command.trinketsSafe." + (CONFIG.trinketsSafe ? "isEnabled" : "isDisabled")
            ).formatted(INFO), false);
        } else {
            var newValue = context.getArgument("enabled", Boolean.class);
            CONFIG.trinketsSafe = newValue;
            LenientDeath.saveConfig();
            context.getSource().sendFeedback(Text.translatable(
                "lenientdeath.command.trinketsSafe." + (newValue ? "enabled" : "disabled")
            ).formatted(SUCCESS), true);
        }

        if (!FabricLoader.getInstance().isModLoaded("trinkets"))
            context.getSource().sendFeedback(Text.translatable("lenientdeath.command.trinketsSafe.notLoaded").formatted(INFO), false);

        return 1;
    }

    private static int updatePerPlayer(CommandContext<ServerCommandSource> context, boolean query) {
        if (query) {
            context.getSource().sendFeedback(Text.translatable(
                "lenientdeath.command.perPlayer." + (CONFIG.perPlayer ? "isEnabled" : "isDisabled")
            ).formatted(INFO), false);
        } else {
            var newValue = context.getArgument("enabled", Boolean.class);
            CONFIG.perPlayer = newValue;
            LenientDeath.saveConfig();
            context.getSource().sendFeedback(Text.translatable(
                "lenientdeath.command.perPlayer." + (newValue ? "enabled" : "disabled")
            ).formatted(SUCCESS), true);
        }

        return 1;
    }

    private static int removeHand(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        PlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendFeedback(Text.translatable("permissions.requires.player").formatted(ERROR), false);
            return 0;
        }

        var handStack = player.getStackInHand(Hand.MAIN_HAND);
        if (!handStack.isEmpty()) {
            var handId = Registry.ITEM.getId(handStack.getItem()).toString();

            if (CONFIG.items.contains(handId)) {
                CONFIG.items.remove(handId);
                LenientDeath.saveConfig();
                source.sendFeedback(Text.translatable("lenientdeath.command.success.itemRemoved", handId).formatted(SUCCESS), true);
                return 1;
            } else {
                source.sendFeedback(Text.translatable("lenientdeath.command.error.itemNotInConfig", handId).formatted(ERROR), false);
                return 0;
            }
        } else {
            source.sendFeedback(Text.translatable("lenientdeath.command.error.nothingInHand").formatted(ERROR), false);
            return 0;
        }
    }

    private static int removeItem(CommandContext<ServerCommandSource> context) {
        var argument = context.getArgument("item", String.class);
        var source = context.getSource();

        if (CONFIG.items.contains(argument)) {
            CONFIG.items.remove(argument);
            LenientDeath.saveConfig();
            source.sendFeedback(Text.translatable("lenientdeath.command.success.itemRemoved", argument).formatted(SUCCESS), true);
            return 1;
        } else {
            source.sendFeedback(Text.translatable("lenientdeath.command.error.itemNotInConfig", argument).formatted(ERROR), false);
            return 0;
        }
    }

    private static int removeTag(CommandContext<ServerCommandSource> context) {
        var argument = context.getArgument("tag", String.class);
        var source = context.getSource();

        if (CONFIG.tags.contains(argument)) {
            CONFIG.tags.remove(argument);
            LenientDeath.saveConfig();
            source.sendFeedback(Text.translatable("lenientdeath.command.success.tagRemoved", argument).formatted(SUCCESS), true);
            return 1;
        } else {
            source.sendFeedback(Text.translatable("lenientdeath.command.error.tagNotInConfig", argument).formatted(ERROR), false);
            return 0;
        }
    }

    private static int addHand(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        PlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendFeedback(Text.translatable("permissions.requires.player").formatted(ERROR), false);
            return 0;
        }

        var handStack = player.getStackInHand(Hand.MAIN_HAND);
        if (!handStack.isEmpty()) {
            var handId = Registry.ITEM.getId(handStack.getItem()).toString();

            if (!CONFIG.items.contains(handId)) {
                CONFIG.items.add(handId);
                LenientDeath.saveConfig();
                source.sendFeedback(Text.translatable("lenientdeath.command.success.itemAdded", handId).formatted(SUCCESS), true);
                return 1;
            } else {
                source.sendFeedback(Text.translatable("lenientdeath.command.error.itemAlreadyInConfig", handId).formatted(ERROR), false);
                return 0;
            }
        } else {
            source.sendFeedback(Text.translatable("lenientdeath.command.error.nothingInHand").formatted(ERROR), false);
            return 0;
        }
    }

    private static int addItem(CommandContext<ServerCommandSource> context) {
        var argument = context.getArgument("item", String.class);
        var source = context.getSource();

        var id = Identifier.tryParse(argument);
        if (id != null) {
            if (id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) argument = "minecraft:" + id.getPath();
            if (Registry.ITEM.containsId(id)) {
                if (!CONFIG.items.contains(argument)) {
                    CONFIG.items.add(argument);
                    LenientDeath.saveConfig();
                    source.sendFeedback(Text.translatable("lenientdeath.command.success.itemAdded", argument).formatted(SUCCESS), true);
                    return 1;
                } else {
                    source.sendFeedback(Text.translatable("lenientdeath.command.error.itemAlreadyInConfig", argument).formatted(ERROR), false);
                    return 0;
                }
            } else {
                unknownItem(argument, source);
                return 0;
            }
        } else {
            invalidIdentifier(argument, source);
            return 0;
        }
    }

    private static int addTag(CommandContext<ServerCommandSource> context) {
        var argument = context.getArgument("tag", String.class);
        var source = context.getSource();

        var tagId = Identifier.tryParse(argument);
        if (tagId != null) {
            if (Registry.ITEM.containsTag(TagKey.of(Registry.ITEM_KEY, tagId))) {
                if (!CONFIG.tags.contains(tagId.toString())) {
                    CONFIG.tags.add(tagId.toString());
                    LenientDeath.saveConfig();
                    source.sendFeedback(Text.translatable("lenientdeath.command.success.tagAdded", argument).formatted(SUCCESS), true);
                    return 1;
                } else {
                    source.sendFeedback(Text.translatable("lenientdeath.command.error.tagAlreadyInConfig", argument).formatted(ERROR), false);
                    return 0;
                }
            } else {
                unknownTag(argument, source);
                return 0;
            }
        } else {
            invalidIdentifier(argument, source);
            return 0;
        }
    }

    private static int listFilters(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(Text.translatable("lenientdeath.command.list.tags").formatted(Formatting.YELLOW), false);
        CONFIG.tags.forEach(s -> context.getSource().sendFeedback(Text.literal(" - " + s), false));
        context.getSource().sendFeedback(Text.translatable("lenientdeath.command.list.items").formatted(Formatting.YELLOW), false);
        CONFIG.items.forEach(s -> context.getSource().sendFeedback(Text.literal(" - " + s), false));
        return 1;
    }

    private static void invalidIdentifier(String id, ServerCommandSource source) {
        source.sendFeedback(Text.translatable("lenientdeath.command.error.unknownIdentifier", id).formatted(ERROR), false);
    }

    private static void unknownItem(String itemId, ServerCommandSource source) {
        source.sendFeedback(Text.translatable("lenientdeath.command.error.unknownItem", itemId).formatted(ERROR), false);
    }

    private static void unknownTag(String tagId, ServerCommandSource source) {
        source.sendFeedback(Text.translatable("lenientdeath.command.error.unknownTag", tagId).formatted(ERROR), false);
    }

    private static int generateDatapack(CommandContext<ServerCommandSource> context) {
        info("Generating tags, requested by " + context.getSource().getName());
        var result = DatapackGenerator.generateDatapack();
        if (result.getLeft()) {
            context.getSource().sendFeedback(Text.translatable("lenientdeath.command.generate.success", result.getRight()).formatted(SUCCESS), true);
            context.getSource().sendFeedback(Text.translatable("lenientdeath.command.generate.success2", result.getRight()).formatted(SUCCESS), false);
            return 1;
        } else {
            context.getSource().sendFeedback(Text.translatable("lenientdeath.command.generate.error").formatted(ERROR), false);
            return 0;
        }
    }
}
