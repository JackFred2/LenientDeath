package red.jackf.lenientdeath.compat;

import dev.emi.trinkets.api.*;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.Util;
import red.jackf.lenientdeath.api.LenientDeathAPI;

import java.util.*;

public class TrinketsCompat {
    // equipped trinkets
    public static void setup() {
        TrinketDropCallback.EVENT.register((dropRule, itemStack, slotReference, livingEntity) -> {
            if (livingEntity instanceof ServerPlayer serverPlayer) {
                if (dropRule == TrinketEnums.DropRule.KEEP) return dropRule;
                if (dropRule == TrinketEnums.DropRule.DESTROY && !LenientDeath.CONFIG.instance().preserveItemsOnDeath.trinkets.overrideDestroyRule) return dropRule;

                var result = LenientDeathAPI.INSTANCE.shouldItemBePreserved(serverPlayer, itemStack);

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

    public static Map<String, Map<String, List<ItemStack>>> getAllTrinkets(ServerPlayer player) {
        var component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) return Collections.emptyMap();

        Map<String, Map<String, List<ItemStack>>> trinkets = new HashMap<>();

        for (Map.Entry<String, Map<String, TrinketInventory>> group : component.get().getInventory().entrySet()) {
            String groupName = group.getKey();
            for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
                String slotName = slot.getKey();
                List<ItemStack> items = new ArrayList<>(slot.getValue().getContainerSize());
                for (int slotIndex = 0; slotIndex < slot.getValue().getContainerSize(); slotIndex++) {
                    ItemStack item = slot.getValue().getItem(slotIndex);
                    if (!item.isEmpty()) {
                        items.add(slot.getValue().getItem(slotIndex));
                    }
                }
                if (!items.isEmpty()) {
                    trinkets.computeIfAbsent(groupName, k -> new HashMap<>()).put(slotName, items);
                }
            }
        }

        return trinkets;
    }

    public static void restoreTrinkets(ServerPlayer player, Map<String, Map<String, List<ItemStack>>> items, boolean replace) {
        List<ItemStack> failed = new ArrayList<>();

        var component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) {
            items.values().stream().flatMap(map -> map.values().stream()).flatMap(Collection::stream).forEach(failed::add);
        } else {
            for (Map.Entry<String, Map<String, List<ItemStack>>> groupEntry : items.entrySet()) {
                var group = component.get().getInventory().get(groupEntry.getKey());
                if (group == null) {
                    groupEntry.getValue().values().stream().flatMap(Collection::stream).forEach(failed::add);
                } else {
                    for (Map.Entry<String, List<ItemStack>> slotEntry : groupEntry.getValue().entrySet()) {
                        var slotInv = group.get(slotEntry.getKey());
                        if (slotInv == null) {
                            failed.addAll(slotEntry.getValue());
                        } else {
                            List<ItemStack> recordedTrinkets = slotEntry.getValue();
                            for (int slot = 0; slot < recordedTrinkets.size(); slot++) {
                                ItemStack trinket = recordedTrinkets.get(slot);
                                if (trinket.isEmpty()) continue;
                                if (slot >= slotInv.getContainerSize()) failed.add(trinket);
                                else if (!slotInv.getItem(slot).isEmpty() && !replace) failed.add(trinket);
                                else slotInv.setItem(slot, trinket);
                            }
                        }
                    }
                }
            }
        }

        for (ItemStack trinket : failed) {
            if (!player.getInventory().add(trinket))
                Util.dropAsItem(player, trinket);
        }
    }
}
