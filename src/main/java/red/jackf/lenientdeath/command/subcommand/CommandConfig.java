package red.jackf.lenientdeath.command.subcommand;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.PermissionKeys;
import red.jackf.lenientdeath.command.Formatting;
import red.jackf.lenientdeath.command.LenientDeathCommand;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.config.Presets;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static net.minecraft.network.chat.Component.literal;
import static net.minecraft.network.chat.Component.translatable;

@SuppressWarnings({"SameParameterValue"})
public class CommandConfig {
    private CommandConfig() {}

    private static final String BASE_WIKI_URL = "https://github.com/JackFred2/LenientDeath/wiki/";

    private static String makeWikiLink(String basePage, String optionName) {
        return BASE_WIKI_URL + basePage + "#" + optionName.toLowerCase(Locale.ROOT).replace(".", "");
    }

    private interface WikiPage {
        String ITEM_RESILIENCE = "Item-Resilience";
        String CONFIG = "Home";
        String COMMAND = "Command";
        String RESTORE_INVENTORY = "Restore-Inventory";
        String DEATH_COORDINATES = "Death-Coordinates";
        String PER_PLAYER = "Per-Player";
        String DROPPED_ITEM_GLOW = "Dropped-Item-Glow";
        String EXTENDED_DEATH_ITEM_LIFETIME = "Extended-Death-Item-Lifetime";
        String MOVE_TO_ORIGINAL_SLOTS = "Move-To-Original-Slots";
        String PRESERVE_EXPERIENCE_ON_DEATH = "Preserve-Experience-on-Death";
        String PRESERVE_ITEMS_ON_DEATH = "Preserve-Items-on-Death";
    }

    public static final Predicate<CommandSourceStack> CHANGE_CONFIG_PREDICATE = Permissions.require(
            PermissionKeys.CONFIG,
            4
    );

    private static LenientDeathConfig getConfig() {
        return LenientDeath.CONFIG.instance();
    }

    private static void verifySafeAndLoad() {
        getConfig().validate();
        LenientDeath.CONFIG.save();
        getConfig().onLoad(null);
    }

    private static Component optionTitle(String name, String fullName, String baseWikiPage) {
        return Formatting.variable(literal(name).withStyle(Style.EMPTY.withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                               Component.empty()
                                        .append(Formatting.variable("$." + fullName))
                                        .append(CommonComponents.NEW_LINE)
                                        .append(translatable("lenientdeath.command.config.clickToOpenWiki")))
        ).withClickEvent(
                new ClickEvent(ClickEvent.Action.OPEN_URL, makeWikiLink(baseWikiPage, fullName))
        )));
    }

    //////////////
    // BUILDERS //
    //////////////
    private static LiteralArgumentBuilder<CommandSourceStack> makeBoolean(String name,
                                                                          String fullName,
                                                                          String baseWikiPage,
                                                                          Function<LenientDeathConfig, Boolean> get,
                                                                          BiConsumer<LenientDeathConfig, Boolean> set) {
        return Commands.literal(name)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                        translatable("lenientdeath.command.config.check",
                                     optionTitle(name, fullName, baseWikiPage),
                                     Formatting.bool(get.apply(getConfig())))
                ), false);

                return 1;
            }).then(Commands.literal("true")
                .executes(ctx -> {
                    if (get.apply(getConfig())) {
                        ctx.getSource().sendFailure(Formatting.infoLine(
                            translatable("lenientdeath.command.config.unchanged",
                                         optionTitle(name, fullName, baseWikiPage),
                                         Formatting.bool(true))
                        ));

                        return 0;
                    } else {
                        set.accept(getConfig(), true);
                        verifySafeAndLoad();
                        ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                                translatable("lenientdeath.command.config.change",
                                             optionTitle(name, fullName, baseWikiPage),
                                             Formatting.bool(false),
                                             Formatting.bool(true))
                        ), true);

                        return 1;
                    }
                }
            )).then(Commands.literal("false")
                .executes(ctx -> {
                    if (!get.apply(getConfig())) {
                        ctx.getSource().sendFailure(Formatting.infoLine(
                                translatable("lenientdeath.command.config.unchanged",
                                             optionTitle(name, fullName, baseWikiPage),
                                             Formatting.bool(false))
                        ));

                        return 0;
                    } else {
                        set.accept(getConfig(), false);
                        verifySafeAndLoad();
                        ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                                translatable("lenientdeath.command.config.change",
                                             optionTitle(name, fullName, baseWikiPage),
                                             Formatting.bool(true),
                                             Formatting.bool(false))
                        ), true);

                        return 1;
                    }
                }
            ));
    }

    private static <E extends Enum<E>> LiteralArgumentBuilder<CommandSourceStack> makeEnum(String name,
                                                                          String fullName,
                                                                          String baseWikiPage,
                                                                          Class<E> enumClass,
                                                                          Function<LenientDeathConfig, E> get,
                                                                          BiConsumer<LenientDeathConfig, E> set) {
        var node = Commands.literal(name)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                        translatable("lenientdeath.command.config.check",
                                     optionTitle(name, fullName, baseWikiPage),
                                     Formatting.string(get.apply(getConfig()).name()))
                ), false);

                return 1;
            });

        for (E constant : enumClass.getEnumConstants()) {
            node.then(Commands.literal(constant.name())
                .executes(ctx -> {
                    var old = get.apply(getConfig());
                    if (old == constant) {
                        ctx.getSource().sendFailure(Formatting.infoLine(
                                translatable("lenientdeath.command.config.unchanged",
                                             optionTitle(name, fullName, baseWikiPage),
                                             Formatting.string(constant.name()))
                        ));

                        return 0;
                    } else {
                        set.accept(getConfig(), constant);
                        verifySafeAndLoad();
                        ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                                translatable("lenientdeath.command.config.change",
                                             optionTitle(name, fullName, baseWikiPage),
                                             Formatting.string(old.name()),
                                             Formatting.string(constant.name()))
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
                                                                           String baseWikiPage,
                                                                           int min,
                                                                           int max,
                                                                           Function<LenientDeathConfig, Integer> get,
                                                                           BiConsumer<LenientDeathConfig, Integer> set) {
        return Commands.literal(name)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                        translatable("lenientdeath.command.config.check",
                                     optionTitle(name, fullName, baseWikiPage),
                                     Formatting.integer(get.apply(getConfig())))
                ), false);

                return 1;
            }).then(Commands.argument(name, IntegerArgumentType.integer(min, max))
                .executes(ctx -> {
                    var old = get.apply(getConfig());
                    var newValue = IntegerArgumentType.getInteger(ctx, name);
                    if (old == newValue) {
                        ctx.getSource().sendFailure(Formatting.infoLine(
                                translatable("lenientdeath.command.config.unchanged",
                                             optionTitle(name, fullName, baseWikiPage),
                                             Formatting.integer(old))
                        ));

                        return 0;
                    } else {
                        set.accept(getConfig(), newValue);
                        verifySafeAndLoad();
                        ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                                translatable("lenientdeath.command.config.change",
                                             optionTitle(name, fullName, baseWikiPage),
                                             Formatting.integer(old),
                                             Formatting.integer(newValue))
                        ), true);

                        return 1;
                    }
                })
            );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> makeFloatRange(String name,
                                                                             String fullName,
                                                                             String baseWikiPage,
                                                                             float min,
                                                                             float max,
                                                                             Function<LenientDeathConfig, Float> get,
                                                                             BiConsumer<LenientDeathConfig, Float> set) {
        return Commands.literal(name)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                        translatable("lenientdeath.command.config.check",
                                     optionTitle(name, fullName, baseWikiPage),
                                     Formatting.floating(get.apply(getConfig())))
                ), false);

                return 1;
            }).then(Commands.argument(name, FloatArgumentType.floatArg(min, max))
                .executes(ctx -> {
                    var old = get.apply(getConfig());
                    var newValue = FloatArgumentType.getFloat(ctx, name);
                    if (old == newValue) {
                        ctx.getSource().sendFailure(Formatting.infoLine(
                                translatable("lenientdeath.command.config.unchanged",
                                             optionTitle(name, fullName, baseWikiPage),
                                             Formatting.floating(old))
                        ));

                        return 0;
                    } else {
                        set.accept(getConfig(), newValue);
                        verifySafeAndLoad();
                        ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                                translatable("lenientdeath.command.config.change",
                                             optionTitle(name, fullName, baseWikiPage),
                                             Formatting.floating(old),
                                             Formatting.floating(newValue))
                        ), true);

                        return 1;
                    }
                })
            );
    }


    private static LiteralArgumentBuilder<CommandSourceStack> makeWord(String name,
                                                                       String fullName,
                                                                       String baseWikiPage,
                                                                       Function<LenientDeathConfig, String> get,
                                                                       BiConsumer<LenientDeathConfig, String> set) {
        return Commands.literal(name)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                        translatable("lenientdeath.command.config.check",
                                     optionTitle(name, fullName, baseWikiPage),
                                     Formatting.string(get.apply(getConfig())))
                ), false);

                return 1;
            }).then(Commands.argument(name, StringArgumentType.word())
                .executes(ctx -> {
                    var old = get.apply(getConfig());
                    var newValue = StringArgumentType.getString(ctx, name);
                    if (old.equals(newValue)) {
                        ctx.getSource().sendFailure(Formatting.infoLine(
                                translatable("lenientdeath.command.config.unchanged",
                                             optionTitle(name, fullName, baseWikiPage),
                                             Formatting.string(old))
                        ));

                        return 0;
                    } else {
                        set.accept(getConfig(), newValue);
                        verifySafeAndLoad();

                        ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                                translatable("lenientdeath.command.config.change",
                                             optionTitle(name, fullName, baseWikiPage),
                                             Formatting.string(old),
                                             Formatting.string(newValue))
                        ), true);

                        return 1;
                    }
                })
            );
    }

    ///////////
    // NODES //
    ///////////

    public static LiteralArgumentBuilder<CommandSourceStack> createCommandNode(CommandBuildContext context) {
        var root = Commands.literal("config")
                .requires(CHANGE_CONFIG_PREDICATE.or(LenientDeathCommand.IS_INTEGRATED_HOST_PREDICATE));

        root.then(createConfigNode());
        root.then(createMetaNode());
        root.then(createDeathCoordinatesNode());
        root.then(createPerPlayerNode());
        root.then(createDroppedItemGlowNode());
        root.then(createExtendedDeathItemLifetime());
        root.then(createMoveToOriginalSlotMode());
        root.then(createPreserveExperienceOnDeathNode());
        root.then(createPreserveItemsOnDeath(context));
        root.then(createItemResilience());
        root.then(createRestoreInventory());

        root.then(createPresetsNode());

        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createRestoreInventory() {
        var root = Commands.literal("restoreInventory");

        root.then(makeIntRange("maxInventoriesSaved",
                "restoreInventory.maxInventoriesSaved",
                WikiPage.RESTORE_INVENTORY,
                0,
                25,
                config -> config.restoreInventory.maxInventoriesSaved,
                (config, newVal) -> config.restoreInventory.maxInventoriesSaved = newVal));

        root.then(makeBoolean("restoreExperience",
                "restoreInventory.restoreExperience",
                WikiPage.RESTORE_INVENTORY,
                config -> config.restoreInventory.restoreExperience,
                (config, newVal) -> config.restoreInventory.restoreExperience = newVal));

        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createDeathCoordinatesNode() {
        var root = Commands.literal("deathCoordinates");

        root.then(makeBoolean(
                "sendToDeadPlayer",
                "deathCoordinates.sendToDeadPlayer",
                WikiPage.DEATH_COORDINATES,
                config -> config.deathCoordinates.sendToDeadPlayer,
                (config, newValue) -> config.deathCoordinates.sendToDeadPlayer = newValue
        ));

        root.then(makeBoolean(
                "sendToServerLog",
                "deathcoordinates.sendToServerLog",
                WikiPage.DEATH_COORDINATES,
                config -> config.deathCoordinates.sendToServerLog,
                (config, newValue) -> config.deathCoordinates.sendToServerLog = newValue
        ));

        root.then(makeBoolean(
                "sendToOtherAdmins",
                "deathcoordinates.sendToOtherAdmins",
                WikiPage.DEATH_COORDINATES,
                config -> config.deathCoordinates.sendToOtherAdmins,
                (config, newValue) -> config.deathCoordinates.sendToOtherAdmins = newValue
        ));

        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createItemResilience() {
        var root = Commands.literal("itemResilience");

        root.then(makeBoolean(
                "allDeathItemsAreFireProof",
                "itemResilience.allDeathItemsAreFireProof",
                WikiPage.ITEM_RESILIENCE,
                config -> config.itemResilience.allDeathItemsAreFireProof,
                (config, newValue) -> config.itemResilience.allDeathItemsAreFireProof = newValue
        ));

        root.then(makeBoolean(
                "allDeathItemsAreCactusProof",
                "itemResilience.allDeathItemsAreCactusProof",
                WikiPage.ITEM_RESILIENCE,
                config -> config.itemResilience.allDeathItemsAreCactusProof,
                (config, newValue) -> config.itemResilience.allDeathItemsAreCactusProof = newValue
        ));

        root.then(makeBoolean(
                "allDeathItemsAreExplosionProof",
                "itemResilience.allDeathItemsAreExplosionProof",
                WikiPage.ITEM_RESILIENCE,
                config -> config.itemResilience.allDeathItemsAreExplosionProof,
                (config, newValue) -> config.itemResilience.allDeathItemsAreExplosionProof = newValue
        ));

        root.then(makeVoidRecoveryNode());

        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> makeVoidRecoveryNode() {
        var root = Commands.literal("voidRecovery");

        root.then(makeEnum(
                "mode",
                "itemResilience.voidRecovery.mode",
                WikiPage.ITEM_RESILIENCE,
                LenientDeathConfig.ItemResilience.VoidRecovery.Mode.class,
                config -> config.itemResilience.voidRecovery.mode,
                (config, newValue) -> config.itemResilience.voidRecovery.mode = newValue
        ));

        root.then(makeBoolean(
                "announce",
                "itemResilience.voidRecovery.announce",
                WikiPage.ITEM_RESILIENCE,
                config -> config.itemResilience.voidRecovery.announce,
                (config, newValue) -> config.itemResilience.voidRecovery.announce = newValue
        ));

        return root;
    }


    private static LiteralArgumentBuilder<CommandSourceStack> createPresetsNode() {
        var root = Commands.literal("presets");

        for (var preset : Presets.PRESETS.get().entrySet()) {
            var node = Commands.literal(preset.getKey())
                    .executes(ctx -> {
                        LenientDeath.CONFIG.setInstance(preset.getValue().get());
                        ctx.getSource().sendSuccess(() -> Formatting.successLine(
                            translatable("lenientdeath.command.config.presetApplied",
                                Formatting.string(preset.getKey()))
                        ), true);
                        return 1;
                    });
            root.then(node);
        }

        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createConfigNode() {
        return Commands.literal("config")
                .then(makeBoolean("enableFileWatcher",
                                  "config.enableFileWatcher",
                                  WikiPage.CONFIG,
                                  config -> config.config.enableFileWatcher,
                                  (config, newVal) -> config.config.enableFileWatcher = newVal))
                .then(makeBoolean("stripComments",
                                  "config.stripComments",
                                  WikiPage.CONFIG,
                                  config -> config.config.stripComments,
                                  (config, newVal) -> config.config.stripComments = newVal));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createMetaNode() {
        var root = Commands.literal("command");
        var names = Commands.literal("commandNames");

        String name = "commandNames";
        String fullName = "command.commandNames";

        names.executes(ctx -> {
            ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                    Component.empty()
                             .append(optionTitle(name, fullName, WikiPage.COMMAND))
                             .append(literal(":"))
            ), false);

            if (getConfig().command.commandNames.isEmpty()) {
                ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                        translatable("lenientdeath.command.config.list.empty")
                ), false);
            } else {
                for (String commandName : getConfig().command.commandNames) {
                    ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                            Formatting.listItem(Formatting.string(commandName))
                    ), false);
                }
            }

            return 1;
        });

        var add = Commands.literal("add")
            .then(Commands.argument("commandName", StringArgumentType.word())
                .executes(ctx -> {
                    var commandName = StringArgumentType.getString(ctx, "commandName");
                    var config = getConfig().command;
                    if (config.commandNames.contains(commandName)) {
                        ctx.getSource().sendFailure(Formatting.errorLine(
                                translatable("lenientdeath.command.config.list.alreadyContains",
                                             optionTitle(name, fullName, WikiPage.COMMAND),
                                             Formatting.string(commandName))
                        ));

                        return 0;
                    } else {
                        config.commandNames.add(commandName);

                        ctx.getSource().sendSuccess(() -> Formatting.successLine(
                                translatable("lenientdeath.command.config.list.added",
                                             optionTitle(name, fullName, WikiPage.COMMAND),
                                             Formatting.string(commandName))
                        ), true);

                        ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                                translatable("lenientdeath.command.config.requiresWorldReload")
                        ), false);

                        verifySafeAndLoad();
                        return 1;
                    }
                })
            );

        var remove = Commands.literal("remove")
            .then(Commands.argument("commandName", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(getConfig().command.commandNames, builder))
                .executes(ctx -> {
                    var commandName = StringArgumentType.getString(ctx, "commandName");
                    var config = getConfig().command;
                    if (!config.commandNames.contains(commandName)) {
                        ctx.getSource().sendFailure(Formatting.errorLine(
                                translatable("lenientdeath.command.config.list.doesNotContain",
                                             optionTitle(name, fullName, WikiPage.COMMAND),
                                             Formatting.string(commandName))
                        ));

                        return 0;
                    } else {
                        config.commandNames.remove(commandName);

                        ctx.getSource().sendSuccess(() -> Formatting.successLine(
                                translatable("lenientdeath.command.config.list.removed",
                                             optionTitle(name, fullName, WikiPage.COMMAND),
                                             Formatting.string(commandName))
                        ), true);

                        ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                                translatable("lenientdeath.command.config.requiresWorldReload")
                        ), false);

                        verifySafeAndLoad();
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
                WikiPage.PER_PLAYER,
                config -> config.perPlayer.defaultEnabledForPlayer,
                (config, newVal) -> config.perPlayer.defaultEnabledForPlayer = newVal))
            .then(makeBoolean("playersCanChangeTheirOwnSetting",
                "perPlayer.playersCanChangeTheirOwnSetting",
                WikiPage.PER_PLAYER,
                config -> config.perPlayer.playersCanChangeTheirOwnSetting,
                (config, newVal) -> config.perPlayer.playersCanChangeTheirOwnSetting = newVal));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createDroppedItemGlowNode() {
        return Commands.literal("droppedItemGlow")
            .then(makeBoolean("enabled",
                "droppedItemGlow.enabled",
                WikiPage.DROPPED_ITEM_GLOW,
                config -> config.droppedItemGlow.enabled,
                (config, newVal) -> config.droppedItemGlow.enabled = newVal))
            .then(makeEnum("glowVisibility",
                "droppedItemGlow.glowVisibility",
                WikiPage.DROPPED_ITEM_GLOW,
                LenientDeathConfig.DroppedItemGlow.Visibility.class,
                config -> config.droppedItemGlow.glowVisibility,
                (config, newVal) -> config.droppedItemGlow.glowVisibility = newVal))
            .then(makeBoolean("noTeamIsValidTeam",
                "droppedItemGlow.noTeamIsValidTeam",
                WikiPage.DROPPED_ITEM_GLOW,
                config -> config.droppedItemGlow.noTeamIsValidTeam,
                (config, newVal) -> config.droppedItemGlow.noTeamIsValidTeam = newVal));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createExtendedDeathItemLifetime() {
        return Commands.literal("extendedDeathItemLifetime")
            .then(makeBoolean("enabled",
                "extendedDeathItemLifetime.enabled",
                WikiPage.EXTENDED_DEATH_ITEM_LIFETIME,
                config -> config.extendedDeathItemLifetime.enabled,
                (config, newVal) -> config.extendedDeathItemLifetime.enabled = newVal))
            .then(makeIntRange("deathDropItemLifetimeSeconds",
                "extendedDeathItemLifetime.deathDropItemLifetimeSeconds",
                WikiPage.EXTENDED_DEATH_ITEM_LIFETIME,
                0,
                1800,
                config -> config.extendedDeathItemLifetime.deathDropItemLifetimeSeconds,
                (config, newVal) -> config.extendedDeathItemLifetime.deathDropItemLifetimeSeconds = newVal))
            .then(makeBoolean("deathDropItemsNeverDespawn",
                "extendedDeathItemLifetime.deathDropItemsNeverDespawn",
                WikiPage.EXTENDED_DEATH_ITEM_LIFETIME,
                config -> config.extendedDeathItemLifetime.deathDropItemsNeverDespawn,
                (config, newVal) -> config.extendedDeathItemLifetime.deathDropItemsNeverDespawn = newVal));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createMoveToOriginalSlotMode() {
        return Commands.literal("moveToOriginalSlot")
                .then(makeBoolean("enabled",
                                  "moveToOriginalSlot.enabled",
                                  WikiPage.MOVE_TO_ORIGINAL_SLOTS,
                                  config -> config.moveToOriginalSlots.enabled,
                                  (config, newVal) -> config.moveToOriginalSlots.enabled = newVal));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createPreserveExperienceOnDeathNode() {
        return Commands.literal("preserveExperienceOnDeath")
            .then(makeEnum("enabled",
                "preserveExperienceOnDeath.enabled",
                WikiPage.PRESERVE_EXPERIENCE_ON_DEATH,
                LenientDeathConfig.PerPlayerEnabled.class,
                config -> config.preserveExperienceOnDeath.enabled,
                (config, newVal) -> config.preserveExperienceOnDeath.enabled = newVal))
            .then(makeIntRange("preservedPercentage",
                "preserveExperienceOnDeath.preservedPercentage",
                WikiPage.PRESERVE_EXPERIENCE_ON_DEATH,
                0,
                100,
                config -> config.preserveExperienceOnDeath.preservedPercentage,
                (config, newVal) -> config.preserveExperienceOnDeath.preservedPercentage = newVal)
            );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> makeItemTypeNode(
            String typeName,
            Function<LenientDeathConfig.PreserveItemsOnDeath.ByItemType, LenientDeathConfig.PreserveItemsOnDeath.ByItemType.TypeBehavior> get,
            BiConsumer<LenientDeathConfig.PreserveItemsOnDeath.ByItemType, LenientDeathConfig.PreserveItemsOnDeath.ByItemType.TypeBehavior> set) {
        return makeEnum(typeName,
                        "preserveItemsOnDeath.byItemType." + typeName,
                        WikiPage.PRESERVE_ITEMS_ON_DEATH,
                        LenientDeathConfig.PreserveItemsOnDeath.ByItemType.TypeBehavior.class,
                        config -> get.apply(config.preserveItemsOnDeath.byItemType),
                        (config, newVal) -> set.accept(config.preserveItemsOnDeath.byItemType, newVal));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createItemTagNodes(
            CommandBuildContext context,
            String name,
            Function<LenientDeathConfig.PreserveItemsOnDeath, List<ResourceLocation>> itemListGet,
            Function<LenientDeathConfig.PreserveItemsOnDeath, List<ResourceLocation>> tagListGet) {
        var root = Commands.literal(name);

        Supplier<Stream<ResourceLocation>> itemIdSupplier = () -> context.lookupOrThrow(Registries.ITEM)
                                                                         .listElementIds()
                                                                         .map(ResourceKey::location);

        Supplier<Stream<ResourceLocation>> tagSupplier = () -> context.lookupOrThrow(Registries.ITEM)
                                                                      .listTagIds()
                                                                      .map(TagKey::location);

        var items = createPreserveItemListNode(itemIdSupplier, itemListGet, "item", "items", "preserveItemsOnDeath." + name + ".items");
        var tags = createPreserveItemListNode(tagSupplier, tagListGet, "tag", "tags", "preserveItemsOnDeath." + name + ".tags");

        root.then(items);
        root.then(tags);

        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createPreserveItemListNode(
            Supplier<Stream<ResourceLocation>> resourceIds,
            Function<LenientDeathConfig.PreserveItemsOnDeath, List<ResourceLocation>> listGet,
            String singular,
            String name,
            String fullName) {
        return Commands.literal(name)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                        Component.empty()
                                 .append(optionTitle(name, fullName, WikiPage.PRESERVE_ITEMS_ON_DEATH))
                                 .append(literal(":"))
                ), false);

                var list = listGet.apply(getConfig().preserveItemsOnDeath);

                if (list.isEmpty()) {
                    ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                            translatable("lenientdeath.command.config.list.empty")
                    ), false);
                } else {
                    for (var item : list) {
                        ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                                Formatting.listItem(Formatting.variable(item.toString()))
                        ), false);
                    }
                }

                return 1;
            }).then(Commands.literal("add")
                .then(Commands.argument(singular, ResourceLocationArgument.id())
                    .suggests((ctx, builder) -> {
                        var existing = listGet.apply(getConfig().preserveItemsOnDeath);
                        return SharedSuggestionProvider.suggestResource(resourceIds.get().filter(id -> !existing.contains(id)), builder);
                    })
                    .executes(ctx -> {
                        var id = ResourceLocationArgument.getId(ctx, singular);

                        // if not a valid ID
                        if (resourceIds.get().filter(existing -> existing.equals(id)).findAny().isEmpty()) {
                            ctx.getSource().sendFailure(Formatting.errorLine(
                                    translatable("lenientdeath.command.config.unknownId",
                                                 Formatting.variable(id.toString()))
                            ));

                            return 0;
                        }

                        var list = listGet.apply(getConfig().preserveItemsOnDeath);
                        if (list.contains(id)) {
                            ctx.getSource().sendFailure(Formatting.errorLine(
                                    translatable("lenientdeath.command.config.list.alreadyContains",
                                                 optionTitle(name, fullName, WikiPage.PRESERVE_ITEMS_ON_DEATH),
                                                 Formatting.variable(id.toString()))
                            ));

                            return 0;
                        } else {
                            list.add(id);
                            verifySafeAndLoad();

                            ctx.getSource().sendSuccess(() -> Formatting.successLine(
                                    translatable("lenientdeath.command.config.list.added",
                                                 optionTitle(name, fullName, WikiPage.PRESERVE_ITEMS_ON_DEATH),
                                                 Formatting.variable(id.toString()))
                            ), true);

                            return 1;
                        }
                    })
                )
            ).then(Commands.literal("remove")
                .then(Commands.argument(singular, ResourceLocationArgument.id())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(listGet.apply(getConfig().preserveItemsOnDeath), builder))
                    .executes(ctx -> {
                        var id = ResourceLocationArgument.getId(ctx, singular);
                        var list = listGet.apply(getConfig().preserveItemsOnDeath);

                        if (!list.contains(id)) {
                            ctx.getSource().sendFailure(Formatting.errorLine(
                                    translatable("lenientdeath.command.config.list.doesNotContain",
                                                 optionTitle(name, fullName, WikiPage.PRESERVE_ITEMS_ON_DEATH),
                                                 Formatting.variable(id.toString()))
                            ));

                             return 0;
                        } else {
                            list.remove(id);
                            verifySafeAndLoad();

                            ctx.getSource().sendSuccess(() -> Formatting.successLine(
                                    translatable("lenientdeath.command.config.list.removed",
                                                 optionTitle(name, fullName, WikiPage.PRESERVE_ITEMS_ON_DEATH),
                                                 Formatting.variable(id.toString()))
                            ), true);

                            return 1;
                        }
                    })
                )
            );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createPreserveItemsOnDeath(CommandBuildContext context) {
        var root = Commands.literal("preserveItemsOnDeath")
            .then(makeEnum("enabled",
                "preserveItemsOnDeath.enabled",
                WikiPage.PRESERVE_ITEMS_ON_DEATH,
                LenientDeathConfig.PerPlayerEnabled.class,
                config -> config.preserveItemsOnDeath.enabled,
                (config, newVal) -> config.preserveItemsOnDeath.enabled = newVal));

        var nbt = Commands.literal("nbt")
            .then(makeBoolean("enabled",
                "preserveItemsOnDeath.nbt.enabled",
                WikiPage.PRESERVE_ITEMS_ON_DEATH,
                config -> config.preserveItemsOnDeath.nbt.enabled,
                (config, newVal) -> config.preserveItemsOnDeath.nbt.enabled = newVal))
            .then(makeWord("nbtKey",
                "preserveItemsOnDeath.nbt.nbtKey",
                WikiPage.PRESERVE_ITEMS_ON_DEATH,
                config -> config.preserveItemsOnDeath.nbt.nbtKey,
                (config, newVal) -> config.preserveItemsOnDeath.nbt.nbtKey = newVal));

        var alwaysDropped = createItemTagNodes(context,
            "alwaysDropped",
            config -> config.alwaysDropped.items,
            config -> config.alwaysDropped.tags);

        var alwaysPreserved = createItemTagNodes(context,
            "alwaysPreserved",
            config -> config.alwaysPreserved.items,
            config -> config.alwaysPreserved.tags);

        var itemType = Commands.literal("byItemType")
            .then(makeBoolean("enabled",
                "preserveItemsOnDeath.byItemType.enabled",
                WikiPage.PRESERVE_ITEMS_ON_DEATH,
                config -> config.preserveItemsOnDeath.byItemType.enabled,
                (config, newVal) -> config.preserveItemsOnDeath.byItemType.enabled = newVal));

        var trinkets = Commands.literal("trinkets")
            .then(makeBoolean("overrideDestroyRule",
                "preserveItemsOnDeath.trinkets.overrideDestroyRule",
                WikiPage.PRESERVE_ITEMS_ON_DEATH,
                config -> config.preserveItemsOnDeath.trinkets.overrideDestroyRule,
                (config, newVal) -> config.preserveItemsOnDeath.trinkets.overrideDestroyRule = newVal));

        itemType.then(makeItemTypeNode("helmets", types -> types.helmets, (config, newVal) -> config.helmets = newVal));
        itemType.then(makeItemTypeNode("chestplates", types -> types.chestplates, (config, newVal) -> config.chestplates = newVal));
        itemType.then(makeItemTypeNode("elytras", types -> types.elytras, (config, newVal) -> config.elytras = newVal));
        itemType.then(makeItemTypeNode("leggings", types -> types.leggings, (config, newVal) -> config.leggings = newVal));
        itemType.then(makeItemTypeNode("boots", types -> types.boots, (config, newVal) -> config.boots = newVal));
        itemType.then(makeItemTypeNode("body", types -> types.body, (config, newVal) -> config.body = newVal));
        itemType.then(makeItemTypeNode("shields", types -> types.shields, (config, newVal) -> config.shields = newVal));
        itemType.then(makeItemTypeNode("otherEquippables", types -> types.otherEquippables, (config, newVal) -> config.otherEquippables = newVal));
        itemType.then(makeItemTypeNode("trinkets", types -> types.trinkets, (config, newVal) -> config.trinkets = newVal));
        itemType.then(makeItemTypeNode("swords", types -> types.swords, (config, newVal) -> config.swords = newVal));
        itemType.then(makeItemTypeNode("tridents", types -> types.tridents, (config, newVal) -> config.tridents = newVal));
        itemType.then(makeItemTypeNode("maces", types -> types.maces, (config, newVal) -> config.maces = newVal));
        itemType.then(makeItemTypeNode("bows", types -> types.bows, (config, newVal) -> config.bows = newVal));
        itemType.then(makeItemTypeNode("crossbows", types -> types.crossbows, (config, newVal) -> config.crossbows = newVal));
        itemType.then(makeItemTypeNode("otherProjectileLaunchers", types -> types.otherProjectileLaunchers, (config, newVal) -> config.otherProjectileLaunchers = newVal));
        itemType.then(makeItemTypeNode("pickaxes", types -> types.pickaxes, (config, newVal) -> config.pickaxes = newVal));
        itemType.then(makeItemTypeNode("shovels", types -> types.shovels, (config, newVal) -> config.shovels = newVal));
        itemType.then(makeItemTypeNode("axes", types -> types.axes, (config, newVal) -> config.axes = newVal));
        itemType.then(makeItemTypeNode("hoes", types -> types.hoes, (config, newVal) -> config.hoes = newVal));
        itemType.then(makeItemTypeNode("otherDiggingItems", types -> types.otherDiggingItems, (config, newVal) -> config.otherDiggingItems = newVal));
        itemType.then(makeItemTypeNode("otherTools", types -> types.otherTools, (config, newVal) -> config.otherTools = newVal));
        itemType.then(makeItemTypeNode("buckets", types -> types.buckets, (config, newVal) -> config.buckets = newVal));
        itemType.then(makeItemTypeNode("food", types -> types.food, (config, newVal) -> config.food = newVal));
        itemType.then(makeItemTypeNode("potions", types -> types.potions, (config, newVal) -> config.potions = newVal));
        itemType.then(makeItemTypeNode("shulkerBoxes", types -> types.shulkerBoxes, (config, newVal) -> config.shulkerBoxes = newVal));

        var randomizer = Commands.literal("randomizer")
            .then(makeBoolean("enabled",
                "preserveItemsOnDeath.randomizer.enabled",
                WikiPage.PRESERVE_ITEMS_ON_DEATH,
                config -> config.preserveItemsOnDeath.randomizer.enabled,
                (config, newVal) -> config.preserveItemsOnDeath.randomizer.enabled = newVal))
            .then(makeBoolean("splitStacks",
                "preserveItemsOnDeath.randomizer.splitStacks",
                WikiPage.PRESERVE_ITEMS_ON_DEATH,
                config -> config.preserveItemsOnDeath.randomizer.splitStacks,
                (config, newVal) -> config.preserveItemsOnDeath.randomizer.splitStacks = newVal))
            .then(makeIntRange("preservedPercentage",
                "preserveItemsOnDeath.randomizer.preservedPercentage",
                WikiPage.PRESERVE_ITEMS_ON_DEATH,
                0,
                100,
                config -> config.preserveItemsOnDeath.randomizer.preservedPercentage,
                (config, newVal) -> config.preserveItemsOnDeath.randomizer.preservedPercentage = newVal))
            .then(makeIntRange("luckAdditiveFactor",
                "preserveItemsOnDeath.randomizer.luckAdditiveFactor",
                WikiPage.PRESERVE_ITEMS_ON_DEATH,
                0,
                200,
                config -> config.preserveItemsOnDeath.randomizer.luckAdditiveFactor,
                (config, newVal) -> config.preserveItemsOnDeath.randomizer.luckAdditiveFactor = newVal))
            .then(makeFloatRange("luckMultiplierFactor",
                "preserveItemsOnDeath.randomizer.luckMultiplierFactor",
                WikiPage.PRESERVE_ITEMS_ON_DEATH,
                0,
                10,
                config -> config.preserveItemsOnDeath.randomizer.luckMultiplierFactor,
                (config, newVal) -> config.preserveItemsOnDeath.randomizer.luckMultiplierFactor = newVal));

        var itemdamage = Commands.literal("itemdamage")
                .then(makeBoolean("enabled",
                        "preserveItemsOnDeath.itemdamage.enabled",
                        WikiPage.PRESERVE_ITEMS_ON_DEATH,
                        config -> config.preserveItemsOnDeath.itemdamage.enabled,
                        (config, newVal) -> config.preserveItemsOnDeath.itemdamage.enabled = newVal))
                .then(makeIntRange("percentage",
                        "preserveItemsOnDeath.itemdamage.percentage",
                        WikiPage.PRESERVE_ITEMS_ON_DEATH,
                        0,
                        100,
                        config -> config.preserveItemsOnDeath.itemdamage.percentage,
                        (config, newVal) -> config.preserveItemsOnDeath.itemdamage.percentage = newVal))
                .then(makeIntRange("baseDamagePercentage",
                        "preserveItemsOnDeath.itemdamage.baseDamagePercentage",
                        WikiPage.PRESERVE_ITEMS_ON_DEATH,
                        0,
                        100,
                        config -> config.preserveItemsOnDeath.itemdamage.baseDamagePercentage,
                        (config, newVal) -> config.preserveItemsOnDeath.itemdamage.baseDamagePercentage = newVal))
                .then(makeIntRange("minimumItemHealth",
                        "preserveItemsOnDeath.itemdamage.minimumItemHealth",
                        WikiPage.PRESERVE_ITEMS_ON_DEATH,
                        0,
                        100,
                        config -> config.preserveItemsOnDeath.itemdamage.minimumItemHealth,
                        (config, newVal) -> config.preserveItemsOnDeath.itemdamage.minimumItemHealth = newVal))
                .then(makeBoolean("percentageRandomness",
                        "preserveItemsOnDeath.randomizer.percentageRandomness",
                        WikiPage.PRESERVE_ITEMS_ON_DEATH,
                        config -> config.preserveItemsOnDeath.itemdamage.percentageRandomness,
                        (config, newVal) -> config.preserveItemsOnDeath.itemdamage.percentageRandomness = newVal))
                .then(makeIntRange("randomizedPercentageMin",
                        "preserveItemsOnDeath.itemdamage.randomizedPercentageMin",
                        WikiPage.PRESERVE_ITEMS_ON_DEATH,
                        0,
                        100,
                        config -> config.preserveItemsOnDeath.itemdamage.randomizedPercentageMin,
                        (config, newVal) -> config.preserveItemsOnDeath.itemdamage.randomizedPercentageMin = newVal))
                .then(makeIntRange("randomizedPercentageMax",
                        "preserveItemsOnDeath.itemdamage.randomizedPercentageMax",
                        WikiPage.PRESERVE_ITEMS_ON_DEATH,
                        0,
                        100,
                        config -> config.preserveItemsOnDeath.itemdamage.randomizedPercentageMax,
                        (config, newVal) -> config.preserveItemsOnDeath.itemdamage.randomizedPercentageMax = newVal));

        root.then(nbt);
        root.then(alwaysDropped);
        root.then(alwaysPreserved);
        root.then(trinkets);
        root.then(itemType);
        root.then(randomizer);
        root.then(itemdamage);

        return root;
    }
}
