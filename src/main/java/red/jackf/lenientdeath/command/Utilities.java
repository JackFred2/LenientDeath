package red.jackf.lenientdeath.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import red.jackf.lenientdeath.preserveitems.PreserveItems;

import java.util.function.Predicate;

public class Utilities {
    private Utilities() {}

    private static final Predicate<CommandSourceStack> SAFE_CHECK_PREDICATE = Permissions.require(
            PermissionKeys.UTILITY_SAFE_CHECK,
            4
    );

    protected static final Predicate<CommandSourceStack> ANY_UTILITY_PREDICATE = SAFE_CHECK_PREDICATE;

    static LiteralArgumentBuilder<CommandSourceStack> createCommandNode(CommandBuildContext context) {
        var root = Commands.literal("utilities");
        root.requires(ANY_UTILITY_PREDICATE);

        var safeCheck = Commands.literal("test")
                .requires(SAFE_CHECK_PREDICATE)
                .then(Commands.argument("item", ItemArgument.item(context))
                    .executes(ctx -> Utilities.testItem(ctx, ItemArgument.getItem(ctx, "item").createItemStack(1, false)))
                ).executes(ctx -> Utilities.testItem(ctx, ctx.getSource().getPlayerOrException().getMainHandItem()));

        root.then(safeCheck);

        return root;
    }

    private static int testItem(CommandContext<CommandSourceStack> ctx, ItemStack stack) {
        if (PreserveItems.INSTANCE.shouldPreserve(stack, false)) {
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
