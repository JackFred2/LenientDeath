package red.jackf.lenientdeath.compatibility;

import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import net.minecraft.item.Item;
import red.jackf.lenientdeath.LenientDeath;

public abstract class TrinketsCompatibility {
    public static void setup() {
        TrinketDropCallback.EVENT.register((rule, stack, ref, entity) -> {
            if (rule != TrinketEnums.DropRule.DEFAULT) return rule;
            if (entity.isPlayer() && LenientDeath.isSafe(stack.getItem())) return TrinketEnums.DropRule.KEEP;
            return rule;
        });
    }

    public static boolean isTrinket(Item item) {
        return item instanceof Trinket;
    }
}
