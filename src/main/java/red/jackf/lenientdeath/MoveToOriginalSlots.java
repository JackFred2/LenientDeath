package red.jackf.lenientdeath;

import net.minecraft.world.entity.item.ItemEntity;
import red.jackf.lenientdeath.mixinutil.LDRemembersSlot;

public class MoveToOriginalSlots {
    private MoveToOriginalSlots() {}

    public static void saveSlot(ItemEntity item, int slot) {
        ((LDRemembersSlot) item).ld$setSlot(slot);
    }
}
