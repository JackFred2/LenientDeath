package red.jackf.lenientdeath;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import red.jackf.lenientdeath.mixins.movetooriginalslots.InventoryAccessor;

public interface Util {
    private static boolean isValidSlot(Inventory inventory, int slot) {
        int sum = 0;
        for (var compartment : ((InventoryAccessor) inventory).getCompartments()) sum += compartment.size();
        return slot >= 0 && slot < sum;
    }

    static boolean tryAddToInventory(Inventory target, ItemStack stack, int slot) {
        if (isValidSlot(target, slot) && target.getItem(slot).isEmpty()) {
            if (slot < target.items.size()) {
                return target.add(slot, stack);
            } else {
                target.setItem(slot, stack.copy());
                stack.setCount(0);
                return true;
            }
        }

        return target.add(stack);
    }

    static void dropAsItem(ServerPlayer owner, ItemStack stack) {
        ItemEntity entity = new ItemEntity(owner.getLevel(), owner.getX(), owner.getY(), owner.getZ(), stack, 0, 0, 0);
        entity.setThrower(owner.getUUID());
        entity.setPickUpDelay(40); // 5 seconds
        owner.getLevel().addFreshEntity(entity);
    }
}
