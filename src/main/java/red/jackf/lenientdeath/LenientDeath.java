package red.jackf.lenientdeath;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.*;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import red.jackf.lenientdeath.command.LenientDeathCommand;
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
			try {
				var tagId = Identifier.tryParse(tagStr);
				var world = MinecraftClient.getInstance().world;
				if (world == null) continue;
				var tag = world.getTagManager().getTag(Registry.ITEM_KEY, tagId, UnknownTagException::new);
				if (tag.contains(item)) return true;
			} catch (Exception ex) {
				error("Error checking for tag " + tagStr + ", disabling...", ex);
				ERRORED_TAGS.add(tagStr);
			}
		}
		return false;
	}
}
