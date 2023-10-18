package red.jackf.lenientdeath.command;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Fabric Permissions API checkers which allow for a value to be gained at runtime
 */
public interface PermissionsExt {
    static Predicate<CommandSourceStack> requireBool(String permissionKey, Supplier<Boolean> defaultSupplier) {
        return player -> Permissions.check(player, permissionKey, defaultSupplier.get());
    }

    static Predicate<CommandSourceStack> requireInt(String permissionKey, Supplier<Integer> defaultSupplier) {
        return player -> Permissions.check(player, permissionKey, defaultSupplier.get());
    }
}
