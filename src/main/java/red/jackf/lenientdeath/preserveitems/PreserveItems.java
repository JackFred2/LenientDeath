package red.jackf.lenientdeath.preserveitems;

import net.minecraft.world.item.ItemStack;

public class PreserveItems {
    public static final PreserveItems INSTANCE = new PreserveItems();
    private PreserveItems() {}

    public void setup() {
        ManualAllowAndBlocklist.INSTANCE.setup();
    }

    /**
     * Whether a given ItemStack should be kept within a player's inventory on death.
     * @param stack ItemStack to check
     * @return If the ItemStack should be kept in a player's inventory.
     */
    public boolean shouldPreserve(ItemStack stack) {
        var nbtPreserveTest = NbtChecker.INSTANCE.shouldKeep(stack);
        if (nbtPreserveTest != null) return nbtPreserveTest;

        var configPreserveTest = ManualAllowAndBlocklist.INSTANCE.shouldKeep(stack);
        if (configPreserveTest != null) return configPreserveTest;
        return false;
    }
}
