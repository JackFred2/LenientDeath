package red.jackf.lenientdeath.preserveitems;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import red.jackf.lenientdeath.LenientDeath;

public class NbtChecker {
    public static final NbtChecker INSTANCE = new NbtChecker();

    private NbtChecker() {}

    public @Nullable Boolean shouldKeep(ItemStack stack) {
        var config = LenientDeath.CONFIG.instance().preserveItemsOnDeath.nbt;
        if (!config.enabled) return null;
        var tag = stack.getTag();
        if (tag == null) return null;
        if (tag.getBoolean(config.nbtKey)) return true;
        return null;
    }
}
