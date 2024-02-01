package red.jackf.lenientdeath.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.command.subcommand.CommandConfig;
import red.jackf.lenientdeath.command.subcommand.PerPlayer;
import red.jackf.lenientdeath.command.subcommand.RestoreInventory;
import red.jackf.lenientdeath.command.subcommand.Utilities;

import java.util.function.Predicate;

public class LenientDeathCommand {
    public static final Predicate<CommandSourceStack> IS_INTEGRATED_HOST_PREDICATE = stack -> {
        var player = stack.getPlayer();
        if (player == null) return false;
        return stack.getServer().isSingleplayerOwner(player.getGameProfile());
    };

    public LenientDeathCommand(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext context,
            Commands.CommandSelection ignored) {
        var commandNames = LenientDeath.CONFIG.instance().command.commandNames;
        if (commandNames.isEmpty()) return;

        var root = Commands.literal(commandNames.get(0));
        root.then(PerPlayer.createCommandNode());
        root.then(CommandConfig.createCommandNode(context));
        root.then(Utilities.createCommandNode(context));
        root.then(RestoreInventory.createCommandNode(context));
        root.requires(PerPlayer.CHECK_SELF_PREDICATE.or(CommandConfig.CHANGE_CONFIG_PREDICATE).or(Utilities.ANY_UTILITY_PREDICATE).or(RestoreInventory.RESTORE_INVENTORY_PREDICATE));

        var builtRoot = dispatcher.register(root);

        for (int i = 1; i < commandNames.size(); i++) {
            var aliasNode = Commands.literal(commandNames.get(i))
                    .redirect(builtRoot)
                    .requires(PerPlayer.CHECK_SELF_PREDICATE.or(CommandConfig.CHANGE_CONFIG_PREDICATE).or(Utilities.ANY_UTILITY_PREDICATE).or(RestoreInventory.RESTORE_INVENTORY_PREDICATE));

            dispatcher.register(aliasNode);
        }
    }

}
