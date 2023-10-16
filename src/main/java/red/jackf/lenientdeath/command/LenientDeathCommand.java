package red.jackf.lenientdeath.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import red.jackf.lenientdeath.config.LenientDeathConfig;

public class LenientDeathCommand {
    public LenientDeathCommand(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext buildCtx,
            Commands.CommandSelection selection) {
        var commandNames = LenientDeathConfig.INSTANCE.get().command.commandNames;
        if (commandNames.isEmpty()) return;

        var root = Commands.literal(commandNames.get(0));
        root.then(PerPlayer.createCommandNode(buildCtx));

        var builtRoot = dispatcher.register(root);

        for (int i = 1; i < commandNames.size(); i++) {
            var aliasNode = Commands.literal(commandNames.get(i))
                    .redirect(builtRoot);

            dispatcher.register(aliasNode);
        }
    }

}
