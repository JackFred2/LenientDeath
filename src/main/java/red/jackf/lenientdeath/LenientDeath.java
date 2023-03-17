package red.jackf.lenientdeath;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import red.jackf.lenientdeath.compatibility.TrinketsCompatibility;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class LenientDeath implements ModInitializer {
    public static final String MODID = "lenientdeath";
    public static final Logger LOG = LogManager.getLogger(MODID);
    public static final Set<String> ERRORED_TAGS = new HashSet<>();
    public static LenientDeathConfig CONFIG = AutoConfig.register(LenientDeathConfig.class, GsonConfigSerializer::new).get();

    public static Identifier id(String path) {
        return new Identifier(MODID, path);
    }

    public static void info(String content) {
        LOG.info("[LenientDeath] " + content);
    }

    public static void error(String content, Exception ex) {
        if (ex == null) LOG.error("[LenientDeath] " + content);
        else LOG.error("[LenientDeath] " + content, ex);
    }

    public static void saveConfig() {
        AutoConfig.getConfigHolder(LenientDeathConfig.class).save();
    }

    public static boolean isSafe(Item item) {
        var isInItemList = CONFIG.items.contains(Registries.ITEM.getId(item).toString());
        if (isInItemList) return true;
        if (FabricLoader.getInstance().isModLoaded("trinkets") && CONFIG.trinketsSafe && TrinketsCompatibility.isTrinket(item))
            return true;
        // check auto
        if (CONFIG.detectAutomatically) {
            var autoDetect = validSafeEquipment(item) || validSafeArmor(item) || validSafeFoods(item);
            if (autoDetect) return true;
        }

        // check config
        for (String tagStr : CONFIG.tags) {
            var tagId = Identifier.tryParse(tagStr);
            if (tagId != null) {
                final Optional<TagKey<Item>> registeredTag = Registries.ITEM.streamTags().filter(key -> key.id().equals(tagId)).findFirst();
                if (registeredTag.isPresent()) {
                    if (item.getRegistryEntry().isIn(registeredTag.get())) return true;

                    ERRORED_TAGS.remove(tagStr);
                } else {
                    if (!ERRORED_TAGS.contains(tagStr)) {
                        error("Tag ID " + tagStr + " is not valid.", null);
                        ERRORED_TAGS.add(tagStr);
                    }
                }
            } else {
                if (!ERRORED_TAGS.contains(tagStr)) {
                    error(tagStr + " is not a valid identifier.", null);
                    ERRORED_TAGS.add(tagStr);
                }
            }
        }

        return false;
    }

    public static boolean validSafeEquipment(Item item) {
        var useAction = item.getUseAction(new ItemStack(item));
        return item instanceof ToolItem
            || item instanceof BucketItem
            || item.isDamageable()
            || useAction == UseAction.BLOCK
            || useAction == UseAction.BOW
            || useAction == UseAction.SPEAR
            || useAction == UseAction.CROSSBOW
            || useAction == UseAction.SPYGLASS;
    }

    public static boolean validSafeArmor(Item item) {
        return item instanceof Equipment;
    }

    public static boolean validSafeFoods(Item item) {
        var useAction = item.getUseAction(new ItemStack(item));
        return item.isFood() || useAction == UseAction.EAT || useAction == UseAction.DRINK;
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(LenientDeathCommand::register);
        if (FabricLoader.getInstance().isModLoaded("trinkets")) TrinketsCompatibility.setup();
    }
}
