package red.jackf.lenientdeath;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public class ItemResilience {
    private static final TagKey<DamageType> ITEMS_IMMUNE_TO = TagKey.create(
            Registries.DAMAGE_TYPE,
            LenientDeath.id("items_immune_to")
    );

    private ItemResilience() {}

    public static boolean areItemsImmuneTo(DamageSource source) {
        return source.is(ITEMS_IMMUNE_TO);
    }

}
