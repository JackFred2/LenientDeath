package red.jackf.lenientdeath;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.tag.ServerTagManagerHolder;
import net.minecraft.tag.Tag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static red.jackf.lenientdeath.LenientDeath.*;

public class LenientDeathCommand {
    private static final Formatting SUCCESS = Formatting.GREEN;
    private static final Formatting INFO = Formatting.YELLOW;
    private static final Formatting ERROR = Formatting.RED;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicatedServer) {
        CommandNode<ServerCommandSource> rootNode = CommandManager.literal("ld")
            .requires(source -> source.hasPermissionLevel(4))
            .build();

        CommandNode<ServerCommandSource> resetErroredTagsNode = CommandManager.literal("resetErroredTags")
            .executes(context -> {
                ERRORED_TAGS.clear();
                context.getSource().sendFeedback(new TranslatableText("lenientdeath.command.resetErroredTags").formatted(SUCCESS), true);
                return 0;
            })
            .build();

        CommandNode<ServerCommandSource> autoDetectNode = CommandManager.literal("autoDetect")
            .executes(ctx -> updateAutoDetect(ctx, true))
            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                .executes(ctx -> updateAutoDetect(ctx, false)))
            .build();

        /*CommandNode<ServerCommandSource> trinketsNode = CommandManager.literal("trinketsSafe")
            .executes(ctx -> updateTrinketsSafe(ctx, true))
            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                .executes(ctx -> updateTrinketsSafe(ctx, false)))
            .build();*/

        CommandNode<ServerCommandSource> generateNode = CommandManager.literal("generate")
            .executes(LenientDeathCommand::generateTags)
            .build();

        CommandNode<ServerCommandSource> listNode = CommandManager.literal("list")
            .executes(LenientDeathCommand::listFilters)
            .build();

        CommandNode<ServerCommandSource> listTagItems = CommandManager.literal("listTagItems")
            .then(CommandManager.argument("tag", StringArgumentType.greedyString()).suggests((context, builder) ->
                CommandSource.suggestMatching(ServerTagManagerHolder.getTagManager().getItems().getTagIds().stream()
                    .map(id -> "#" + id), builder)
            ).executes(LenientDeathCommand::listTagItems).build())
            .build();

        CommandNode<ServerCommandSource> addNode = CommandManager.literal("add")
            .then(CommandManager.argument("item", StringArgumentType.greedyString()).suggests((context, builder) -> {
                Stream<String> suggestions = Stream.concat(Stream.concat(
                    Stream.of("hand"),
                    ServerTagManagerHolder.getTagManager().getItems().getTagIds().stream()
                        .filter(id -> !CONFIG.tags.contains(id.toString()))
                        .map(id -> "#" + id)),
                    Registry.ITEM.getIds().stream()
                        .map(Identifier::toString)
                        .filter(id -> !CONFIG.items.contains(id)));
                return CommandSource.suggestMatching(suggestions, builder);
            }).executes(LenientDeathCommand::addValue).build())
            .build();

        CommandNode<ServerCommandSource> removeNode = CommandManager.literal("remove")
            .then(CommandManager.argument("item", StringArgumentType.greedyString()).suggests((context, builder) -> CommandSource.suggestMatching(Stream.concat(Stream.concat(
                Stream.of("hand"),
                CONFIG.tags.stream().map(s -> "#" + s)),
                CONFIG.items.stream()
            ), builder)).executes(LenientDeathCommand::removeValue).build())
            .build();

        dispatcher.getRoot().addChild(rootNode);
        rootNode.addChild(generateNode);
        rootNode.addChild(resetErroredTagsNode);
        rootNode.addChild(listTagItems);
        rootNode.addChild(listNode);
        rootNode.addChild(addNode);
        rootNode.addChild(removeNode);
        rootNode.addChild(autoDetectNode);
        //rootNode.addChild(trinketsNode);
    }

    private static int listTagItems(CommandContext<ServerCommandSource> context) {
        String arg = context.getArgument("tag", String.class);
        if (arg.charAt(0) == '#') arg = arg.substring(1); // strip #
        @Nullable Identifier id = Identifier.tryParse(arg);
        if (id == null) {
            context.getSource().sendFeedback(new TranslatableText("lenientdeath.command.error.unknownIdentifier", arg).formatted(ERROR), false);
            return 0;
        } else {
            Tag<Item> tag = ServerTagManagerHolder.getTagManager().getItems().getTag(id);
            if (tag == null) {
                context.getSource().sendFeedback(new TranslatableText("lenientdeath.command.error.unknownTag", "#" + arg).formatted(ERROR), false);
                return 0;
            } else {
                context.getSource().sendFeedback(new TranslatableText("lenientdeath.command.listTagItems", "#" + arg).formatted(INFO), false);
                tag.values().forEach(item -> context.getSource().sendFeedback(new LiteralText(" - " + Registry.ITEM.getId(item)), false));
            }
        }
        return 1;
    }

    private static int updateAutoDetect(CommandContext<ServerCommandSource> context, boolean query) {
        if (query) {
            context.getSource().sendFeedback(new TranslatableText(
                "lenientdeath.command.autoDetect." + (CONFIG.detectAutomatically ? "isEnabled" : "isDisabled")
            ).formatted(INFO), false);
        } else {
            Boolean newValue = context.getArgument("enabled", Boolean.class);
            CONFIG.detectAutomatically = newValue;
            LenientDeath.saveConfig();
            context.getSource().sendFeedback(new TranslatableText(
                "lenientdeath.command.autoDetect." + (newValue ? "enabled" : "disabled")
            ).formatted(SUCCESS), true);
        }

        return 1;
    }

    /*private static int updateTrinketsSafe(CommandContext<ServerCommandSource> context, boolean query) {
        if (query) {
            context.getSource().sendFeedback(new TranslatableText(
                "lenientdeath.command.trinketsSafe." + (CONFIG.trinketsSafe ? "isEnabled" : "isDisabled")
            ).formatted(INFO), false);
        } else {
            var newValue = context.getArgument("enabled", Boolean.class);
            CONFIG.trinketsSafe = newValue;
            LenientDeath.saveConfig();
            context.getSource().sendFeedback(new TranslatableText(
                "lenientdeath.command.trinketsSafe." + (newValue ? "enabled" : "disabled")
            ).formatted(SUCCESS), true);
        }

        if (!FabricLoader.getInstance().isModLoaded("trinkets"))
            context.getSource().sendFeedback(new TranslatableText("lenientdeath.command.trinketsSafe.notLoaded").formatted(INFO), false);

        return 1;
    }*/

    private static int removeValue(CommandContext<ServerCommandSource> context) {
        String argument = context.getArgument("item", String.class);
        ServerCommandSource source = context.getSource();

        if ("hand".equals(argument)) {
            try {
                ItemStack handStack = source.getPlayer().getStackInHand(Hand.MAIN_HAND);
                if (!handStack.isEmpty())
                    argument = Registry.ITEM.getId(handStack.getItem()).toString();
            } catch (CommandSyntaxException ignored) {
            }
        }

        if (argument.charAt(0) == '#') { // tag
            String idSubstr = argument.substring(1);
            if (CONFIG.tags.contains(idSubstr)) {
                CONFIG.tags.remove(idSubstr);
                LenientDeath.saveConfig();
                source.sendFeedback(new TranslatableText("lenientdeath.command.success.tagRemoved", argument).formatted(SUCCESS), true);
                return 1;
            } else {
                source.sendFeedback(new TranslatableText("lenientdeath.command.error.tagNotInConfig", argument).formatted(ERROR), false);
            }
        } else {
            if (CONFIG.items.contains(argument)) {
                CONFIG.items.remove(argument);
                LenientDeath.saveConfig();
                source.sendFeedback(new TranslatableText("lenientdeath.command.success.itemRemoved", argument).formatted(SUCCESS), true);
                return 1;
            } else {
                source.sendFeedback(new TranslatableText("lenientdeath.command.error.itemNotInConfig", argument).formatted(ERROR), false);
            }
        }
        return 0;
    }

    private static int addValue(CommandContext<ServerCommandSource> context) {
        String argument = context.getArgument("item", String.class);
        ServerCommandSource source = context.getSource();

        if ("hand".equals(argument)) {
            try {
                ItemStack handStack = source.getPlayer().getStackInHand(Hand.MAIN_HAND);
                if (!handStack.isEmpty())
                    argument = Registry.ITEM.getId(handStack.getItem()).toString();
            } catch (CommandSyntaxException ignored) {
            }
        }

        if (argument.charAt(0) == '#') { // tag
            String idSubstr = argument.substring(1);
            @Nullable Identifier tagId = Identifier.tryParse(idSubstr);
            if (tagId != null) {
                if (tagId.getNamespace().equals("minecraft")) {
                    idSubstr = "minecraft:" + tagId.getPath();
                    argument = "#minecraft:" + tagId.getPath();
                }
                Tag<Item> tag = ServerTagManagerHolder.getTagManager().getItems().getTag(tagId);

                if (tag == null) {
                    unknownTag("" + tagId, source);
                } else {
                    if (!CONFIG.tags.contains(idSubstr)) {
                        CONFIG.tags.add(idSubstr);
                        LenientDeath.saveConfig();
                        source.sendFeedback(new TranslatableText("lenientdeath.command.success.tagAdded", argument).formatted(SUCCESS), true);
                        return 1;
                    } else {
                        source.sendFeedback(new TranslatableText("lenientdeath.command.error.tagAlreadyInConfig", argument).formatted(ERROR), false);
                    }
                }
            } else {
                invalidIdentifier(idSubstr, source);
            }
        } else {
            @Nullable Identifier itemId = Identifier.tryParse(argument);
            if (itemId != null) {
                if (itemId.getNamespace().equals("minecraft")) argument = "minecraft:" + itemId.getPath();
                if (Registry.ITEM.containsId(itemId)) {
                    if (!CONFIG.items.contains(argument)) {
                        CONFIG.items.add(argument);
                        LenientDeath.saveConfig();
                        source.sendFeedback(new TranslatableText("lenientdeath.command.success.itemAdded", argument).formatted(SUCCESS), true);
                        return 1;
                    } else {
                        source.sendFeedback(new TranslatableText("lenientdeath.command.error.itemAlreadyInConfig", argument).formatted(ERROR), false);
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
        source.sendFeedback(new TranslatableText("lenientdeath.command.error.unknownIdentifier", id).formatted(ERROR), false);
    }

    private static void unknownItem(String itemId, ServerCommandSource source) {
        source.sendFeedback(new TranslatableText("lenientdeath.command.error.unknownItem", itemId).formatted(ERROR), false);
    }

    private static void unknownTag(String tagId, ServerCommandSource source) {
        source.sendFeedback(new TranslatableText("lenientdeath.command.error.unknownTag", tagId).formatted(ERROR), false);
    }

    private static int generateTags(CommandContext<ServerCommandSource> context) {
        info("Generating tags, requested by " + context.getSource().getName());

        try {
            Path dir = Files.createDirectories(Paths.get("lenientdeath"));
            List<Identifier> safeItems = new ArrayList<>();

            List<Identifier> vanillaItems = new ArrayList<>();
            List<Identifier> modItems = new ArrayList<>();

            List<Tag<Item>> safeTags = Arrays.asList(
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
                Item item = Registry.ITEM.get(id);
                if (safeTags.stream().anyMatch(tag -> tag.contains(item))) return;
                if (LenientDeath.validSafeFoods(item)
                    || LenientDeath.validSafeArmor(item)
                    || LenientDeath.validSafeEquipment(item)) safeItems.add(id);
            });
            writeToFile(dir.resolve("safe.json"), safeItems, safeTags);
            context.getSource().sendFeedback(new TranslatableText("lenientdeath.command.generate.success", safeItems.size(), safeTags.size()).formatted(SUCCESS), true);
        } catch (Exception ex) {
            context.getSource().sendFeedback(new TranslatableText("lenientdeath.command.generate.error").formatted(ERROR), false);
            error("Error generating tags", ex);
            return 0;
        }

        return 1;
    }

    private static void writeToFile(Path file, List<Identifier> ids, List<Tag<Item>> tags) throws IOException {
        StringBuilder fileContents = new StringBuilder("{\n  [\n");
        tags.forEach(tag -> {
            if (tag instanceof Tag.Identified) {
                Tag.Identified<Item> identified = (Tag.Identified<Item>) tag;
                Identifier tagId = identified.getId();
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
        fileContents.append("\n  ]\n}");
        Files.write(file, Arrays.asList(fileContents.toString().split(System.getProperty("line.separator"))));
    }
}
