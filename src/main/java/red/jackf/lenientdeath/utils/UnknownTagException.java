package red.jackf.lenientdeath.utils;

import net.minecraft.util.Identifier;

public class UnknownTagException extends Exception {
    public UnknownTagException(Identifier id) {
        super("Unknown tag for id " + id);
    }
}
