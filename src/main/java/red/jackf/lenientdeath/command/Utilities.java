package red.jackf.lenientdeath.command;

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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import red.jackf.lenientdeath.preserveitems.PreserveItems;

import java.util.function.Predicate;

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

    protected static final Predicate<CommandSourceStack> ANY_UTILITY_PREDICATE = SAFE_CHECK_PREDICATE.or(LIST_TAG_ITEMS_PREDICATE);

    private static SuggestionProvider<CommandSourceStack> createTagSuggestor(CommandBuildContext context) {
        return (ctx, builder) -> SharedSuggestionProvider.suggestResource(context.holderLookup(Registries.ITEM).listTagIds().map(TagKey::location), builder);
    }

    static LiteralArgumentBuilder<CommandSourceStack> createCommandNode(CommandBuildContext context) {
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
                .executes(ctx -> {
                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "tag");
                    var tag = context.holderLookup(Registries.ITEM).get(TagKey.create(Registries.ITEM, id));

                    // if valid tag
                    if (tag.isPresent()) {
                        // title
                        ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                            CommandFormatting.text(
                                Component.translatable("lenientdeath.command.utilies.listItemsInTag",
                                    CommandFormatting.variable("#" + id).resolve(CommandFormatting.TextType.BLANK)))
                        ), false);

                        var items = tag.get().stream().toList();

                        if (items.isEmpty()) { // empty tag
                            ctx.getSource().sendSuccess(() -> CommandFormatting.info(
                                Component.translatable("lenientdeath.command.config.listEmpty")
                            ), false);

                            return 0;
                        } else { // non-empty tag
                            for (Holder<Item> item : items) {
                                var key = item.unwrapKey();
                                key.ifPresent(itemResourceKey ->
                                    ctx.getSource()
                                    .sendSuccess(() -> CommandFormatting.info(
                                        CommandConfig.LIST_PREFIX,
                                        CommandFormatting.variable(itemResourceKey.location().toString())
                                    ), false)
                                );
                            }
                        }
                        return 1;
                    } else { // invalid tag
                        ctx.getSource().sendFailure(CommandFormatting.error(
                            Component.translatable("lenientdeath.command.config.unknownId",
                                CommandFormatting.variable(String.valueOf(id)).resolve(CommandFormatting.TextType.BLANK))
                        ));
                        return 0;
                    }
                })
            );

        root.then(safeCheck);
        root.then(listItemsInTag);

        return root;
    }

    private static int testItem(CommandContext<CommandSourceStack> ctx, ItemStack stack) {
        if (PreserveItems.INSTANCE.shouldPreserve(null, stack)) {
            ctx.getSource().sendSuccess(() -> CommandFormatting.success(
                Component.translatable("lenientdeath.command.utilies.safeCheck.success", stack.getDisplayName())
            ), false);

            return 1;
        } else {
            ctx.getSource().sendFailure(CommandFormatting.error(
                    Component.translatable("lenientdeath.command.utilies.safeCheck.failure", stack.getDisplayName())
            ));

            return 0;
        }
    }
}
