package red.jackf.lenientdeath.preserveitems;

import dev.emi.trinkets.api.*;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import red.jackf.lenientdeath.LenientDeath;

import java.util.Map;

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

    public static boolean isTrinket(@Nullable Player player, ItemStack item) {
        return TrinketsApi.getTrinketComponent(player).map(comp -> {
            for (Map<String, TrinketInventory> groups : comp.getInventory().values()) {
                for (TrinketInventory inv : groups.values()) {
                    int size = ((Container) inv).getContainerSize();
                    SlotType slotType = inv.getSlotType();
                    for (int i = 0; i < size; i++) {
                        var ref = new SlotReference(inv, i);
                        if (TrinketsApi.evaluatePredicateSet(slotType.getValidatorPredicates(), item, ref, player)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }).orElse(false);
    }
}
