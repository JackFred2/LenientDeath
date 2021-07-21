package red.jackf.lenientdeath;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.item.Wearable;
import net.minecraft.tag.ServerTagManagerHolder;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import red.jackf.lenientdeath.compatibility.TrinketsCompatibility;
import red.jackf.lenientdeath.utils.UnknownTagException;

import java.util.HashSet;
import java.util.Set;

public class LenientDeath implements ModInitializer {
	public static final String MODID = "lenientdeath";
	public static Identifier id(String path) {
		return new Identifier(MODID, path);
	}

	public static final Logger LOG = LogManager.getLogger(MODID);

	public static void info(String content) {
		LOG.info("[LenientDeath] " + content);
	}
	public static void error(String content, Exception ex) {
		if (ex == null) LOG.error("[LenientDeath] " + content);
		LOG.error("[LenientDeath] " + content, ex);
	}

	public static final Set<String> ERRORED_TAGS = new HashSet<>();

	public static LenientDeathConfig CONFIG = AutoConfig.register(LenientDeathConfig.class, GsonConfigSerializer::new).get();

	public static void saveConfig() {
		AutoConfig.getConfigHolder(LenientDeathConfig.class).save();
	}

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(LenientDeathCommand::register);
		if (FabricLoader.getInstance().isModLoaded("trinkets")) TrinketsCompatibility.setup();
	}

	public static boolean isSafe(Item item) {
		var isInItemList = CONFIG.items.contains(Registry.ITEM.getId(item).toString());
		if (isInItemList) return true;
		if (FabricLoader.getInstance().isModLoaded("trinkets") && CONFIG.trinketsDetection && TrinketsCompatibility.isTrinket(item)) return true;
		for (String tagStr : CONFIG.tags) {
			if (ERRORED_TAGS.contains(tagStr)) continue;
				var tagId = Identifier.tryParse(tagStr);
				if (tagId != null) {
					try {
						var tag = ServerTagManagerHolder.getTagManager().getTag(Registry.ITEM_KEY, tagId, UnknownTagException::new);
						if (tag.contains(item)) return true;
					} catch (Exception ex) {
						error("Error checking for tag " + tagStr + ", disabling...", ex);
						ERRORED_TAGS.add(tagStr);
					}
				} else {
					error("Tag ID " + tagStr + " is not valid, disabling...", null);
					ERRORED_TAGS.add(tagStr);
				}
		}

		// check auto
		if (CONFIG.detectAutomatically) {
			return validSafeEquipment(item) || validSafeArmor(item) || validSafeFoods(item);
		}

		return false;
	}

	public static boolean validSafeEquipment(Item item) {
		var useAction = item.getUseAction(new ItemStack(item));
		return item instanceof ToolItem
			|| item.isDamageable()
			|| useAction == UseAction.BLOCK
			|| useAction == UseAction.BOW
			|| useAction == UseAction.SPEAR
			|| useAction == UseAction.CROSSBOW
			|| useAction == UseAction.SPYGLASS;
	}

	public static boolean validSafeArmor(Item item) {
		return item instanceof Wearable;
	}

	public static boolean validSafeFoods(Item item) {
		var useAction = item.getUseAction(new ItemStack(item));
		return item.isFood() || useAction == UseAction.EAT || useAction == UseAction.DRINK;
	}
}
