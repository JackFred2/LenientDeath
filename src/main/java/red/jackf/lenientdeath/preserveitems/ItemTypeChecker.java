package red.jackf.lenientdeath.preserveitems;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jetbrains.annotations.Nullable;
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.config.LenientDeathConfig.PreserveItemsOnDeath.ByItemType.TypeBehavior;

import java.util.Set;

public class ItemTypeChecker {
    public static ItemTypeChecker INSTANCE = new ItemTypeChecker();
    private static final Set<UseAnim> OTHER_TOOLS_ANIMS = Set.of(
            UseAnim.BRUSH,
            UseAnim.TOOT_HORN,
            UseAnim.SPYGLASS
    );
    private ItemTypeChecker() {}

    public @Nullable Boolean shouldKeep(@Nullable Player player, ItemStack stack) {
        var config = LenientDeath.CONFIG.instance().preserveItemsOnDeath.byItemType;
        if (!config.enabled) return null;

        Item item = stack.getItem();
        TypeBehavior result = TypeBehavior.ignore;

        if (item instanceof Equipable)
            if (item instanceof ArmorItem armor)
                result = result.and(switch (armor.getType()) {
                    case HELMET -> config.helmets;
                    case CHESTPLATE -> config.chestplates;
                    case LEGGINGS -> config.leggings;
                    case BOOTS -> config.boots;
                });
            else if (item instanceof ElytraItem) result = result.and(config.elytras);
            else if (item instanceof ShieldItem) result = result.and(config.shields);
            else result = result.and(config.otherEquippables);

        if (FabricLoader.getInstance().isModLoaded("trinkets") && TrinketsCompat.isTrinket(player, stack)) result = result.and(config.trinkets);

        if (item instanceof SwordItem) result = result.and(config.swords);
        if (item instanceof TridentItem) result = result.and(config.tridents);

        if (item instanceof ProjectileWeaponItem)
            if (item instanceof BowItem) result = result.and(config.bows);
            else if (item instanceof CrossbowItem) result = result.and(config.crossbows);
            else result = result.and(config.otherProjectileLaunchers);

        if (item instanceof DiggerItem)
            if (item instanceof PickaxeItem) result = result.and(config.pickaxes);
            else if (item instanceof ShovelItem) result = result.and(config.shovels);
            else if (item instanceof AxeItem) result = result.and(config.axes);
            else if (item instanceof HoeItem) result = result.and(config.hoes);
            else result = result.and(config.otherDiggingItems);
        else
            if (OTHER_TOOLS_ANIMS.contains(stack.getUseAnimation())) result = result.and(config.otherTools);

        if (item instanceof BucketItem) result = result.and(config.buckets);

        if (item.isEdible()) result = result.and(config.food);

        if (item instanceof PotionItem) result = result.and(config.potions);

        if (item instanceof BlockItem blockItem)
            if (blockItem.getBlock() instanceof ShulkerBoxBlock) result = result.and(config.shulkerBoxes);

        return switch (result) {
            case drop -> false;
            case preserve -> true;
            case ignore -> null;
        };
    }
}
