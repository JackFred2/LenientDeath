package red.jackf.lenientdeath;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.item.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LenientDeath implements ModInitializer {
	public static final String MODID = "lenientdeath";
	public static final Logger LOG = LogManager.getLogger(MODID);

	public static void info(String content) {
		LOG.info("[LenientDeath] " + content);
	}

	public static void error(String content, Exception ex) {
		LOG.error("[LenientDeath] " + content, ex);
	}

	public static Identifier id(String path) {
		return new Identifier(MODID, path);
	}

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
	}

	private static int generateTags(CommandContext<ServerCommandSource> context) {
		info("Generating tags, requested by " + context.getSource().getName());

		try {
			var dir = Files.createDirectories(Path.of("lenientdeath"));
			var foods = new ArrayList<Identifier>();
			var equipment = new ArrayList<Identifier>();
			var armor = new ArrayList<Identifier>();
			Registry.ITEM.getIds().stream().sorted((id1, id2) -> {
				var firstVanilla = id1.getNamespace().equals("minecraft");
				var secondVanilla = id2.getNamespace().equals("minecraft");
				if (firstVanilla && !secondVanilla) {
					return -1;
				} else if (!firstVanilla && secondVanilla) {
					return 1;
				} else {
					return Comparator.<Identifier>naturalOrder().compare(id1, id2);
				}
			}).forEach(id -> {
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
