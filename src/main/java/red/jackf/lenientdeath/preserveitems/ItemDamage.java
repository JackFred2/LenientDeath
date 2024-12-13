package red.jackf.lenientdeath.preserveitems;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import red.jackf.lenientdeath.LenientDeath;

import java.util.concurrent.ThreadLocalRandom;

public class ItemDamage {

    public static ItemStack applyItemDamage(ServerPlayer deathPlayer, ItemStack stack) {
        var config = LenientDeath.CONFIG.instance().preserveItemsOnDeath.itemdamage;

        //Item is not Damageable (ignoring)
        if(!stack.isDamageableItem())
            return stack;

        int currentDamage = stack.getDamageValue(); // Placeholder for actual stack method
        int maxDamage = stack.getMaxDamage();      // Placeholder for actual stack method

        // Determine if the item will take damage based on the percentage chance
        boolean willTakeDamage = ThreadLocalRandom.current().nextInt(0, 100) < config.percentage;

        //Return the ItemStack without damaging it (ignoring)
        if (!willTakeDamage) {
            return stack;
        }

        // Calculate damage percentage
        double damagePercentage = config.baseDamagePercentage;

        // Apply randomization if enabled
        if (config.percentageRandomness) {
            double randomFactor = ThreadLocalRandom.current().nextDouble(config.randomizedPercentageMin, config.randomizedPercentageMax);
            damagePercentage = config.baseDamagePercentage * (randomFactor / 100.0);
        }

        // Convert the damage percentage to actual damage
        int newDamage = currentDamage + (int) ((damagePercentage / 100) * maxDamage);

        // Ensure the new damage doesn't exceed the item's maximum or fall below the minimum threshold
        newDamage = Math.min(newDamage, maxDamage - config.minimumItemHealth);

        //If config.minimumItemHealth is <= 0 and the newDamage is or exceeds the maxDamage of an item, remove the ItemStack from the Player Inventory - which destroys the item.
        if(config.minimumItemHealth <= 0 && newDamage >= maxDamage)
        {
            deathPlayer.getInventory().removeItem(stack);
        }

        // Update the stack's damage value
        stack.setDamageValue(newDamage); // Placeholder for actual stack method

        return stack;
    }
}
