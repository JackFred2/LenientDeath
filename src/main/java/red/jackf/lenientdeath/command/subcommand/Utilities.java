package red.jackf.lenientdeath.command.subcommand;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.PermissionKeys;
import red.jackf.lenientdeath.command.Formatting;
import red.jackf.lenientdeath.command.LenientDeathCommand;
import red.jackf.lenientdeath.preserveitems.PreserveItems;
import red.jackf.lenientdeath.preserveitems.Randomizer;

import java.util.function.Predicate;

import static net.minecraft.network.chat.Component.translatable;
import static red.jackf.lenientdeath.command.Formatting.listItem;
import static red.jackf.lenientdeath.command.Formatting.variable;

public class Utilities {
    private Utilities() {}

    private static final Predicate<CommandSourceStack> SAFE_CHECK_PREDICATE = Permissions.require(
            PermissionKeys.UTILITY_SAFE_CHECK,
            4
    );

    private static final Predicate<CommandSourceStack> LIST_TAG_ITEMS_PREDICATE = Permissions.require(
            PermissionKeys.UTILITY_LIST_TAG_ITEMS,
            4
    );

    public static final Predicate<CommandSourceStack> ANY_UTILITY_PREDICATE = SAFE_CHECK_PREDICATE
            .or(LIST_TAG_ITEMS_PREDICATE)
            .or(LenientDeathCommand.IS_INTEGRATED_HOST_PREDICATE);

    private static SuggestionProvider<CommandSourceStack> createTagSuggestor(CommandBuildContext context) {
        return (ctx, builder) -> SharedSuggestionProvider.suggestResource(context.holderLookup(Registries.ITEM).listTagIds().map(TagKey::location), builder);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createCommandNode(CommandBuildContext context) {
        var root = Commands.literal("utilities");
        root.requires(ANY_UTILITY_PREDICATE);

        var safeCheck = Commands.literal("test")
            .requires(SAFE_CHECK_PREDICATE)
            .then(Commands.argument("item", ItemArgument.item(context))
                .executes(ctx -> Utilities.testItem(ctx, ItemArgument.getItem(ctx, "item").createItemStack(1, false)))
            ).executes(ctx -> Utilities.testItem(ctx, ctx.getSource().getPlayerOrException().getMainHandItem()));

        var listItemsInTag = Commands.literal("listItemsInTag")
            .requires(LIST_TAG_ITEMS_PREDICATE)
            .then(Commands.argument("tag", ResourceLocationArgument.id())
                .suggests(Utilities.createTagSuggestor(context))
                .executes(ctx -> listTagItems(ctx, context))
            );

        root.then(safeCheck);
        root.then(listItemsInTag);

        return root;
    }

    private static int listTagItems(CommandContext<CommandSourceStack> ctx, CommandBuildContext context) {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "tag");
        var tag = context.holderLookup(Registries.ITEM).get(TagKey.create(Registries.ITEM, id));

        // if valid tag
        if (tag.isPresent()) {
            // title
            ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                translatable("lenientdeath.command.utilies.listItemsInTag",
                             Formatting.variable("#" + id))
            ), false);

            var items = tag.get().stream().toList();

            if (items.isEmpty()) { // empty tag
                ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                        translatable("lenientdeath.command.config.list.empty")
                ), false);

                return 0;
            } else { // non-empty tag
                for (Holder<Item> item : items) {
                    item.unwrapKey()
                        .ifPresent(key ->
                                   ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                                               listItem(variable(key.location().toString()))
                                   ), false)
                    );
                }
            }
            return 1;
        } else { // invalid tag
            ctx.getSource().sendFailure(Formatting.errorLine(
                    translatable("lenientdeath.command.config.unknownId",
                                 variable(id.toString()))
            ));
            return 0;
        }
    }

    private static int testItem(CommandContext<CommandSourceStack> ctx, ItemStack stack) {
        var test = PreserveItems.INSTANCE.shouldPreserve(ctx.getSource().getPlayer(), stack, true);
        var random = LenientDeath.CONFIG.instance().preserveItemsOnDeath.randomizer;
        if (test != null && test) {
            ctx.getSource().sendSuccess(() -> Formatting.successLine(
                translatable("lenientdeath.command.utilies.safeCheck.success",
                             variable(stack.getHoverName()))
            ), false);

            return 1;
        } else if (test == null && random.enabled) {
            ctx.getSource().sendSuccess(() -> Formatting.infoLine(
                translatable("lenientdeath.command.utilies.safeCheck.random",
                             variable(stack.getHoverName()),
                             variable(String.valueOf(Randomizer.INSTANCE.getChanceToKeep(ctx.getSource().getPlayer()) * 100)))
            ), false);

            return 1;
        } else {
            ctx.getSource().sendSuccess(() -> Formatting.errorLine(
                    translatable("lenientdeath.command.utilies.safeCheck.failure",
                                 variable(stack.getHoverName()))
            ), false);

            return 0;
        }
    }
}
