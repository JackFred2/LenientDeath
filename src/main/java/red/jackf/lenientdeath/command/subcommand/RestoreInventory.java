package red.jackf.lenientdeath.command.subcommand;

import com.google.common.collect.Streams;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.PermissionKeys;
import red.jackf.lenientdeath.Util;
import red.jackf.lenientdeath.command.Formatting;
import red.jackf.lenientdeath.compat.TrinketsCompat;
import red.jackf.lenientdeath.restoreinventory.DeathRecord;
import red.jackf.lenientdeath.restoreinventory.TrinketsRecord;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class RestoreInventory {
    public static final Predicate<CommandSourceStack> RESTORE_INVENTORY_PREDICATE = Permissions.require(
            PermissionKeys.RESTORE_INVENTORY,
            4
    );
    private static final DateTimeFormatter HOVER_FORMAT =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(ZoneId.systemDefault());

    public static LiteralArgumentBuilder<CommandSourceStack> createCommandNode(CommandBuildContext context) {
        var root = Commands.literal("deaths");
        root.requires(stack -> LenientDeath.CONFIG.instance().restoreInventory.maxInventoriesSaved > 0 && RESTORE_INVENTORY_PREDICATE.test(stack));

        root.then(makeListNode(context));
        root.then(makeRestoreNode(context));

        return root;
    }

    // /ld deaths restore <player> <death index> [replace]
    private static LiteralArgumentBuilder<CommandSourceStack> makeRestoreNode(CommandBuildContext ignored) {
        var root = Commands.literal("restore");

        var playerArgument = Commands.argument("player", EntityArgument.player());

        var deathIndexArgument = Commands.argument("deathIndex", IntegerArgumentType.integer(0));
        deathIndexArgument.executes(ctx -> restoreInventory(ctx, false));

        var replace = Commands.literal("replace");
        replace.executes(ctx -> restoreInventory(ctx, true));

        deathIndexArgument.then(replace);
        playerArgument.then(deathIndexArgument);
        root.then(playerArgument);

        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> makeListNode(CommandBuildContext ignored) {
        var root = Commands.literal("list");
        root.executes(ctx -> RestoreInventory.listDeaths(ctx, ctx.getSource().getPlayerOrException()));

        var playerArgument = Commands.argument("player", EntityArgument.player());
        playerArgument.executes(ctx -> RestoreInventory.listDeaths(ctx, EntityArgument.getPlayer(ctx, "player")));

        root.then(playerArgument);

        return root;
    }

    private static int listDeaths(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        List<DeathRecord> deaths = red.jackf.lenientdeath.restoreinventory.RestoreInventory.INSTANCE.getDeathHistory(player);
        if (deaths.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                    Component.translatable("lenientdeath.command.restoreInventory.empty", player.getDisplayName())
            ), false);
        } else {
            ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                    Component.translatable("lenientdeath.command.restoreInventory.header", player.getDisplayName())
            ), false);

            for (int i = 0; i < deaths.size(); i++) {
                DeathRecord record = deaths.get(i);
                for (MutableComponent line : formatRecord(i, player, record)) {
                    ctx.getSource().sendSuccess(() -> Formatting.infoLine(line), false);
                }
            }
        }
        return deaths.size();
    }

    private static int restoreInventory(CommandContext<CommandSourceStack> ctx, boolean replace) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int deathIndex = IntegerArgumentType.getInteger(ctx, "deathIndex");

        List<DeathRecord> deaths = red.jackf.lenientdeath.restoreinventory.RestoreInventory.INSTANCE.getDeathHistory(player);
        if (deathIndex >= deaths.size()) {
            ctx.getSource().sendFailure(Formatting.errorLine(Component.translatable("lenientdeath.command.restoreInventory.indexOutOfRange", deathIndex)));
            return 0;
        }

        DeathRecord death = deaths.get(deathIndex);

        if (LenientDeath.CONFIG.instance().restoreInventory.restoreExperience) {
            player.setExperiencePoints(0);
            player.setExperienceLevels(0);
            player.giveExperiencePoints(death.experience());
        }

        if (replace) {
            player.getInventory().replaceWith(death.inventory());
        } else {
            for (int slot = 0; slot < death.inventory().getContainerSize(); slot++) {
                ItemStack item = death.inventory().getItem(slot);
                if (item.isEmpty()) continue;

                if (!Util.tryAddToInventory(player.getInventory(), item, slot)) {
                    Util.dropAsItem(player, item);
                }
            }
        }

        if (FabricLoader.getInstance().isModLoaded("trinkets") && death.trinketsInventory().isPresent()) {
            TrinketsCompat.restoreTrinkets(player, death.trinketsInventory().get().items(), replace);
        }

        MutableComponent line = replace ?
                Component.translatable("lenientdeath.command.restoreInventory.success.replace", player.getDisplayName()) :
                Component.translatable("lenientdeath.command.restoreInventory.success", player.getDisplayName()) ;

        ctx.getSource().sendSuccess(() -> Formatting.successLine(line), true);
        return 1;
    }

    private static Component formatTimeFromNow(Instant time) {
        long seconds = Math.max(0, time.until(Instant.now(), ChronoUnit.SECONDS));

        MutableComponent component;

        if (seconds < 60) component = Component.translatable("lenientdeath.command.restoreInventory.time.secondsAgo", seconds);
        else if (seconds < 3600) component = Component.translatable("lenientdeath.command.restoreInventory.time.minutesAgo", seconds / 60);
        else if (seconds < 86400) component = Component.translatable("lenientdeath.command.restoreInventory.time.hoursAgo", seconds / 3600);
        else component = Component.translatable("lenientdeath.command.restoreInventory.time.daysAgo", seconds / 86400);

        component.setStyle(Formatting.VARIABLE.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(HOVER_FORMAT.format(time)))));

        return component;
    }

    private static long countItems(Inventory inventory) {
        return Streams.concat(inventory.items.stream(), inventory.armor.stream(), inventory.offhand.stream())
                .filter(stack -> !stack.isEmpty())
                .count();
    }

    private static long countTrinkets(TrinketsRecord inventory) {
        long count = 0;

        for (var group : inventory.items().values()) {
            for (var slot : group.values()) {
                for (var item : slot) {
                    if (!item.isEmpty()) count += 1;
                }
            }
        }

        return count;
    }

    private static List<MutableComponent> formatRecord(int index, ServerPlayer player, DeathRecord record) {
        List<MutableComponent> lines = new ArrayList<>();
        String spacer = "   ";

        MutableComponent firstLine = Component.empty()
                .append(Component.literal("" + index).withStyle(ChatFormatting.GREEN))
                .append(": ")
                .append(ComponentUtils.wrapInSquareBrackets(Component.empty().withStyle(ChatFormatting.WHITE).append(record.deathMessage())));
        lines.add(firstLine);

        String positionText = record.location().pos().toShortString();
        String dimensionText = record.location().dimension().location().toString();

        String teleportCommand = "/execute in " + dimensionText + " run tp @s " + positionText.replace(",", "");

        Component position = ComponentUtils.wrapInSquareBrackets(Component.literal(positionText)
                .withStyle(Formatting.VARIABLE
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, teleportCommand)))).withStyle(Formatting.SUCCESS);

        MutableComponent secondLine = Component.literal(spacer)
                .append(Component.translatable("lenientdeath.command.restoreInventory.timeAndPosition",
                        formatTimeFromNow(record.timeOfDeath()),
                        position,
                        Component.literal(dimensionText).withStyle(Formatting.VARIABLE)));
        lines.add(secondLine);

        String playerName = player.getGameProfile().getName();
        String restoreCommand = "/ld deaths restore " + playerName + " " + index;
        String replaceCommand = restoreCommand + " replace";

        Component restoreButton = ComponentUtils.wrapInSquareBrackets(Component.translatable("lenientdeath.command.restoreInventory.restore"))
                .withStyle(Formatting.SUCCESS
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("lenientdeath.command.restoreInventory.restore.hover")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, restoreCommand)));

        Component replaceButton = ComponentUtils.wrapInSquareBrackets(Component.translatable("lenientdeath.command.restoreInventory.replace"))
                .withStyle(Formatting.ERROR
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("lenientdeath.command.restoreInventory.replace.hover")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, replaceCommand)));

        long mainCount = countItems(record.inventory());
        long trinketsCount = record.trinketsInventory().map(RestoreInventory::countTrinkets).orElse(0L);

        MutableComponent thirdLine = Component.literal(spacer)
                .append(Component.translatable("lenientdeath.command.restoreInventory.itemsAndXp",
                        Component.literal("" + (mainCount + trinketsCount)).withStyle(ChatFormatting.GREEN),
                        Component.literal("" + record.experience()).withStyle(ChatFormatting.GREEN)))
                .append(CommonComponents.space())
                .append(restoreButton)
                .append(CommonComponents.space())
                .append(replaceButton);
        lines.add(thirdLine);

        return lines;
    }
}
