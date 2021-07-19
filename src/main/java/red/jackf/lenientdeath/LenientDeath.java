package red.jackf.lenientdeath;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.*;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.*;
import net.minecraft.resource.ReloadableResourceManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagManager;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	public static LenientDeathConfig CONFIG = AutoConfig.register(LenientDeathConfig.class, JanksonConfigSerializer::new).get();

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			var rootNode = CommandManager.literal("ld")
				.requires(source -> source.hasPermissionLevel(4))
				.build();

			var generateNode = CommandManager.literal("generate")
				.executes(LenientDeath::generateTags)
				.build();

			dispatcher.getRoot().addChild(rootNode);
			rootNode.addChild(generateNode);
		});

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
				var tag = world.getTagManager().getTag(Registry.ITEM_KEY, tagId, id -> new UnknownTagException("Unknown tag " + id));
				if (tag.contains(item)) return true;
			} catch (Exception ex) {
				error("Error checking for tag " + tagStr + ", disabling...", ex);
				ERRORED_TAGS.add(tagStr);
			}
		}
		return false;
	}

	private static int generateTags(CommandContext<ServerCommandSource> context) {
		info("Generating tags, requested by " + context.getSource().getName());

		try {
			var dir = Files.createDirectories(Path.of("lenientdeath"));
			var foods = new ArrayList<Identifier>();
			var equipment = new ArrayList<Identifier>();
			var armor = new ArrayList<Identifier>();

			var vanillaItems = new ArrayList<Identifier>();
			var modItems = new ArrayList<Identifier>();

			Registry.ITEM.getIds().stream().sorted(Comparator.comparing(Identifier::getNamespace).thenComparing(Identifier::getPath)).forEach(id -> {
				if (id.getNamespace().equals("minecraft")) vanillaItems.add(id);
				else modItems.add(id);
			});

			Stream.concat(vanillaItems.stream(), modItems.stream()).forEach(id -> {
				var item = Registry.ITEM.get(id);
				var testStack = new ItemStack(item);
				var useAction = item.getUseAction(testStack);
				if (item.isFood() || useAction == UseAction.EAT || useAction == UseAction.DRINK) foods.add(id);
				else if (item instanceof Wearable) armor.add(id);
				else if (item instanceof ToolItem || item.isDamageable() || useAction != UseAction.NONE) equipment.add(id);
			});
			writeToFile(dir.resolve("foods.json"), foods);
			writeToFile(dir.resolve("equipment.json"), equipment);
			writeToFile(dir.resolve("armor.json"), armor);
			context.getSource().sendFeedback(new TranslatableText("lenientdeath.command.generate.success", foods.size() + equipment.size() + armor.size()), true);
		} catch (Exception ex) {
			context.getSource().sendError(new TranslatableText("lenientdeath.command.generate.error"));
			error("Error generating tags", ex);
			return 1;
		}

		return 0;
	}

	private static void writeToFile(Path file, List<Identifier> ids) throws IOException {
		var fileContents = new StringBuilder("""
			{
			  "values": [
			""");
		ids.forEach(id -> {
			if (id.getNamespace().equals("minecraft"))
				fileContents.append("    \"")
					.append(id)
					.append("\",\n");
			else
				fileContents.append("    {\n      \"id\": \"") // modded items, mark not required
					.append(id)
					.append("\",\n      \"required\": false\n    },\n");
		});

		//trim last comma and newline
		fileContents.delete(fileContents.length() - 2, fileContents.length());
		fileContents.append("""
			    
			  ]
			}""");
		Files.write(file, fileContents.toString().lines().collect(Collectors.toList()));
	}
}
