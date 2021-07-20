package red.jackf.lenientdeath;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.*;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.tag.ServerTagManagerHolder;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import red.jackf.lenientdeath.utils.UnknownTagException;

import java.util.*;

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

	private static final Set<String> ERRORED_TAGS = new HashSet<>();

	public static LenientDeathConfig CONFIG = AutoConfig.register(LenientDeathConfig.class, GsonConfigSerializer::new).get();

	public static void saveConfig() {
		AutoConfig.getConfigHolder(LenientDeathConfig.class).save();
	}

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(LenientDeathCommand::register);

		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {

			@Override
			public void reload(ResourceManager manager) {
				ERRORED_TAGS.clear();
			}

			@Override
			public Identifier getFabricId() {
				return id("taglistener");
			}
		});
	}

	public static boolean isSafe(Item item) {
		var isInItemList = CONFIG.items.contains(Registry.ITEM.getId(item).toString());
		if (isInItemList) return true;
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
