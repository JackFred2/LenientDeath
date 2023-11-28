package red.jackf.lenientdeath.preserveitems;

import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import red.jackf.lenientdeath.LenientDeath;

public class TrinketsCompat {
    // equipped trinkets
    protected static void setup() {
        TrinketDropCallback.EVENT.register((dropRule, itemStack, slotReference, livingEntity) -> {
            if (livingEntity instanceof ServerPlayer serverPlayer) {
                if (dropRule == TrinketEnums.DropRule.KEEP) return dropRule;
                if (dropRule == TrinketEnums.DropRule.DESTROY && !LenientDeath.CONFIG.instance().preserveItemsOnDeath.trinkets.overrideDestroyRule) return dropRule;

                var result = PreserveItems.shouldKeepOnDeath(serverPlayer, itemStack);

                if (result) return TrinketEnums.DropRule.KEEP;
                else return dropRule;
            } else {
                return dropRule;
            }
        });
    }

    public static boolean isTrinket(Item item) {
        return item instanceof Trinket;
    }
}
