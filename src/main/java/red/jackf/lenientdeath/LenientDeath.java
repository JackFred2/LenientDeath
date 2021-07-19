package red.jackf.lenientdeath;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class LenientDeath implements ModInitializer {
    public static final String MODID = "lenientdeath";

    public static Identifier id(String path) {
    	return new Identifier(MODID, path);
	}

	public static LenientDeathConfig CONFIG = AutoConfig.register(LenientDeathConfig.class, JanksonConfigSerializer::new).get();

    @Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		System.out.println("Hello Fabric world!");
	}
}
