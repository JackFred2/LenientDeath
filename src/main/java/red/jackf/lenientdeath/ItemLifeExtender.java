package red.jackf.lenientdeath;

import net.minecraft.SharedConstants;
import net.minecraft.world.entity.item.ItemEntity;
import red.jackf.lenientdeath.config.LenientDeathConfig;
import red.jackf.lenientdeath.mixins.itemlifeextender.ItemEntityAccessor;

public class ItemLifeExtender {
    private ItemLifeExtender() {}

    public static void extendItemLifetime(ItemEntity item) {
        LenientDeathConfig.ExtendedDeathItemLifetime lifetimeConfig = LenientDeath.CONFIG.instance().extendedDeathItemLifetime;
        if (lifetimeConfig.enabled) {
            if (lifetimeConfig.deathDropItemsNeverDespawn) {
                item.setUnlimitedLifetime();
                item.addTag("LENIENT_DEATH_INFINITE_LIFETIME");
            } else {
                int newAge = 6000 - lifetimeConfig.deathDropItemLifetimeSeconds * SharedConstants.TICKS_PER_SECOND;
                ((ItemEntityAccessor) item).setAge(newAge);
            }
        }
    }
}
