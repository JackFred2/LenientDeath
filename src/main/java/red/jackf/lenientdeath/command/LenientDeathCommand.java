package red.jackf.lenientdeath.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import red.jackf.lenientdeath.config.LenientDeathConfig;

public class LenientDeathCommand {
    public LenientDeathCommand(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext context,
            Commands.CommandSelection ignored) {
        var commandNames = LenientDeathConfig.INSTANCE.get().command.commandNames;
        if (commandNames.isEmpty()) return;

        var root = Commands.literal(commandNames.get(0));
        root.then(PerPlayer.createCommandNode());
        root.then(CommandConfig.createCommandNode(context));
        root.requires(PerPlayer.CHECK_SELF_PREDICATE.or(CommandConfig.CHANGE_CONFIG_PREDICATE));

        var builtRoot = dispatcher.register(root);

        for (int i = 1; i < commandNames.size(); i++) {
            var aliasNode = Commands.literal(commandNames.get(i))
                    .redirect(builtRoot);

            dispatcher.register(aliasNode);
        }
    }

}
