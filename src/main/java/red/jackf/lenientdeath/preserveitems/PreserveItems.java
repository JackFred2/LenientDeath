package red.jackf.lenientdeath.preserveitems;

import net.minecraft.world.item.ItemStack;

public class PreserveItems {
    public static final PreserveItems INSTANCE = new PreserveItems();
    private PreserveItems() {}

    public void setup() {
        ManualAllowAndBlocklist.INSTANCE.setup();
    }

    public boolean shouldPreserve(ItemStack stack) {
        var configPreserveTest = ManualAllowAndBlocklist.INSTANCE.shouldKeepBasedOnConfig(stack);
        if (configPreserveTest == ShouldPreserve.NO) return false;
        else if (configPreserveTest == ShouldPreserve.YES) return true;


        return false;
    }
}
