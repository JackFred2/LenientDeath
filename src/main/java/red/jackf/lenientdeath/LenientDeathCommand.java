package red.jackf.lenientdeath;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.tag.ServerTagManagerHolder;
import net.minecraft.tag.Tag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import red.jackf.lenientdeath.utils.UnknownTagException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static red.jackf.lenientdeath.LenientDeath.*;

public class LenientDeathCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicatedServer) {
        var rootNode = CommandManager.literal("ld")
            .requires(source -> source.hasPermissionLevel(4))
            .build();

        var resetErroredTagsNode = CommandManager.literal("resetErroredTags")
            .executes(context -> {
                ERRORED_TAGS.clear();
                context.getSource().sendFeedback(new TranslatableText("lenientdeath.command.resetErroredTags"), true);
                return 0;
            })
            .build();

        var autoDetectNode = CommandManager.literal("autoDetect")
            .executes(ctx -> updateAutoDetect(ctx, true))
            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                .executes(ctx -> updateAutoDetect(ctx, false)))
            .build();

        var trinketsNode = CommandManager.literal("trinketsSafe")
            .executes(ctx -> updateTrinketsSafe(ctx, true))
            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                .executes(ctx -> updateTrinketsSafe(ctx, false)))
            .build();

        var generateNode = CommandManager.literal("generate")
            .executes(LenientDeathCommand::generateTags)
            .build();

        var listNode = CommandManager.literal("list")
            .executes(LenientDeathCommand::listFilters)
            .build();

        var addNode = CommandManager.literal("add")
            .then(CommandManager.argument("item", StringArgumentType.greedyString()).suggests((context, builder) -> {
                var suggestions = Stream.concat(
                    ServerTagManagerHolder.getTagManager().getOrCreateTagGroup(Registry.ITEM_KEY).getTagIds().stream()
                        .filter(id -> !CONFIG.tags.contains(id.toString()))
                        .map(id -> "#" + id),
                Registry.ITEM.getIds().stream()
                        .map(Identifier::toString)
                        .filter(id -> !CONFIG.items.contains(id)));
                return CommandSource.suggestMatching(suggestions, builder);
                }).executes(LenientDeathCommand::addValue).build())
            .build();

        var removeNode = CommandManager.literal("remove")
            .then(CommandManager.argument("item", StringArgumentType.greedyString()).suggests((context, builder) -> CommandSource.suggestMatching(Stream.concat(
                CONFIG.tags.stream().map(s -> "#" + s),
                CONFIG.items.stream()
            ), builder)).executes(LenientDeathCommand::removeValue).build())
            .build();

        dispatcher.getRoot().addChild(rootNode);
        rootNode.addChild(generateNode);
        rootNode.addChild(resetErroredTagsNode);
        rootNode.addChild(listNode);
        rootNode.addChild(addNode);
        rootNode.addChild(removeNode);
        rootNode.addChild(autoDetectNode);
        rootNode.addChild(trinketsNode);
    }

    private static int updateAutoDetect(CommandContext<ServerCommandSource> context, boolean query) {
        if (query) {
            context.getSource().sendFeedback(new TranslatableText(
                "lenientdeath.command.autoDetect." + (CONFIG.detectAutomatically ? "isEnabled" : "isDisabled")
            ), false);
        } else {
            var newValue = context.getArgument("enabled", Boolean.class);
            CONFIG.detectAutomatically = newValue;
            LenientDeath.saveConfig();
            context.getSource().sendFeedback(new TranslatableText(
                "lenientdeath.command.autoDetect." + (newValue ? "enabled" : "disabled")
            ), true);
        }

        return 1;
    }

    private static int updateTrinketsSafe(CommandContext<ServerCommandSource> context, boolean query) {
        if (query) {
            context.getSource().sendFeedback(new TranslatableText(
                "lenientdeath.command.trinketsSafe." + (CONFIG.trinketsSafe ? "isEnabled" : "isDisabled")
            ), false);
        } else {
            var newValue = context.getArgument("enabled", Boolean.class);
            CONFIG.trinketsSafe = newValue;
            LenientDeath.saveConfig();
            context.getSource().sendFeedback(new TranslatableText(
                "lenientdeath.command.trinketsSafe." + (newValue ? "enabled" : "disabled")
            ), true);
        }

        if (!FabricLoader.getInstance().isModLoaded("trinkets"))
            context.getSource().sendFeedback(new TranslatableText("lenientdeath.command.trinketsSafe.notLoaded"), false);

        return 1;
    }

    private static int removeValue(CommandContext<ServerCommandSource> context) {
        var argument = context.getArgument("item", String.class);
        var source = context.getSource();
        if (argument.charAt(0) == '#') { // tag
            var idSubstr = argument.substring(1);
                if (CONFIG.tags.contains(idSubstr)) {
                    CONFIG.tags.remove(idSubstr);
                    LenientDeath.saveConfig();
                    source.sendFeedback(new TranslatableText("lenientdeath.command.success.tagRemoved", argument), true);
                    return 1;
                } else {
                    source.sendError(new TranslatableText("lenientdeath.command.error.tagNotInConfig", argument));
                }
        } else {
            if (CONFIG.items.contains(argument)) {
                CONFIG.items.remove(argument);
                LenientDeath.saveConfig();
                source.sendFeedback(new TranslatableText("lenientdeath.command.success.itemRemoved", argument), true);
                return 1;
            } else {
                source.sendError(new TranslatableText("lenientdeath.command.error.itemNotInConfig", argument));
            }
        }
        return 0;
    }

    private static int addValue(CommandContext<ServerCommandSource> context) {
        var argument = context.getArgument("item", String.class);
        var source = context.getSource();
        if (argument.charAt(0) == '#') { // tag
            var idSubstr = argument.substring(1);
            var tagId = Identifier.tryParse(idSubstr);
            if (tagId != null) {
                if (tagId.getNamespace().equals("minecraft")) {
                    idSubstr = "minecraft:" + tagId.getPath();
                    argument = "#minecraft:" + tagId.getPath();
                }
                try {
                    ServerTagManagerHolder.getTagManager().getTag(Registry.ITEM_KEY, tagId, UnknownTagException::new);

                    // tag exists
                    if (!CONFIG.tags.contains(idSubstr)) {
                        CONFIG.tags.add(idSubstr);
                        LenientDeath.saveConfig();
                        source.sendFeedback(new TranslatableText("lenientdeath.command.success.tagAdded", argument), true);
                        return 1;
                    } else {
                        source.sendError(new TranslatableText("lenientdeath.command.error.tagAlreadyInConfig", argument));
                    }
                } catch (UnknownTagException ex) {
                    unknownTag(argument, source);
                }
            } else {
                invalidIdentifier(idSubstr, source);
            }
        } else {
            var itemId = Identifier.tryParse(argument);
            if (itemId != null) {
                if (itemId.getNamespace().equals("minecraft")) argument = "minecraft:" + itemId.getPath();
                if (Registry.ITEM.containsId(itemId)) {
                    if (!CONFIG.items.contains(argument)) {
                        CONFIG.items.add(argument);
                        LenientDeath.saveConfig();
                        source.sendFeedback(new TranslatableText("lenientdeath.command.success.itemAdded", argument), true);
                        return 1;
                    } else {
                        source.sendError(new TranslatableText("lenientdeath.command.error.itemAlreadyInConfig", argument));
                    }
                } else {
                    unknownItem(argument, source);
                }
            } else {
                invalidIdentifier(argument, source);
            }
        }
        return 0;
    }

    private static int listFilters(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(new TranslatableText("lenientdeath.command.list.tags").formatted(Formatting.YELLOW), false);
        CONFIG.tags.forEach(s -> context.getSource().sendFeedback(new LiteralText(" - #" + s), false));
        context.getSource().sendFeedback(new TranslatableText("lenientdeath.command.list.items").formatted(Formatting.YELLOW), false);
        CONFIG.items.forEach(s -> context.getSource().sendFeedback(new LiteralText(" - " + s), false));
        return 1;
    }

    private static void invalidIdentifier(String id, ServerCommandSource source) {
        source.sendError(new TranslatableText("lenientdeath.command.error.unknownIdentifier", id));
    }

    private static void unknownItem(String itemId, ServerCommandSource source) {
        source.sendError(new TranslatableText("lenientdeath.command.error.unknownItem", itemId));
    }

    private static void unknownTag(String tagId, ServerCommandSource source) {
        source.sendError(new TranslatableText("lenientdeath.command.error.unknownTag", tagId));
    }

    private static int generateTags(CommandContext<ServerCommandSource> context) {
        info("Generating tags, requested by " + context.getSource().getName());

        try {
            var dir = Files.createDirectories(Path.of("lenientdeath"));
            var safeItems = new ArrayList<Identifier>();

            var vanillaItems = new ArrayList<Identifier>();
            var modItems = new ArrayList<Identifier>();

            var safeTags = List.of(
                FabricToolTags.AXES,
                FabricToolTags.HOES,
                FabricToolTags.PICKAXES,
                FabricToolTags.SHEARS,
                FabricToolTags.SHOVELS,
                FabricToolTags.SWORDS
            );

            Registry.ITEM.getIds().stream().sorted(Comparator.comparing(Identifier::getNamespace).thenComparing(Identifier::getPath)).forEach(id -> {
                if (id.getNamespace().equals("minecraft")) vanillaItems.add(id);
                else modItems.add(id);
            });

            Stream.concat(vanillaItems.stream(), modItems.stream()).forEach(id -> {
                var item = Registry.ITEM.get(id);
                if (safeTags.stream().anyMatch(tag -> tag.contains(item))) return;
                if (LenientDeath.validSafeFoods(item)
                 || LenientDeath.validSafeArmor(item)
                 || LenientDeath.validSafeEquipment(item)) safeItems.add(id);
            });
            writeToFile(dir.resolve("safe.json"), safeItems, safeTags);
            context.getSource().sendFeedback(new TranslatableText("lenientdeath.command.generate.success", safeItems.size(), safeTags.size()), true);
        } catch (Exception ex) {
            context.getSource().sendError(new TranslatableText("lenientdeath.command.generate.error"));
            error("Error generating tags", ex);
            return 0;
        }

        return 1;
    }

    private static void writeToFile(Path file, List<Identifier> ids, List<Tag<Item>> tags) throws IOException {
        var fileContents = new StringBuilder("""
			{
			  "values": [
			""");
        tags.forEach(tag -> {
            if (tag instanceof Tag.Identified<Item> identified) {
                var tagId = identified.getId();
                fileContents.append("    \"#")
                    .append(tagId)
                    .append("\",\n");
            } else {
                LOG.warn("Could not get ID for tag");
            }
        });
        ids.forEach(id -> {
            if (id.getNamespace().equals("minecraft"))
                fileContents.append("    \"")
                    .append(id)
                    .append("\",\n");
            else
                fileContents.append("    {\n      \"id\": \"") // modded items, mark not required
                    .append(id)
                    .append("\",\n      \"required\": false\n    },\n");
        });

        //trim last comma and newline
        fileContents.delete(fileContents.length() - 2, fileContents.length());
        fileContents.append("""
			    
			  ]
			}""");
        Files.write(file, fileContents.toString().lines().collect(Collectors.toList()));
    }
}
