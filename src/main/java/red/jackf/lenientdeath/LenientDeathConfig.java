package red.jackf.lenientdeath;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.List;

@Config(name = LenientDeath.MODID)
@Config.Gui.Background("minecraft:textures/block/soul_sand.png")
public class LenientDeathConfig implements ConfigData {
    @Comment("List of item IDs to be kept upon death")
    @ConfigEntry.Gui.Tooltip
    public List<String> items = List.of(
        "minecraft:bucket",
        "minecraft:water_bucket",
        "minecraft:lava_bucket"
    );

    @Comment("List of item tags to be kept upon death")
    @ConfigEntry.Gui.Tooltip
    public List<String> tags = List.of(
        "lenientdeath:items/armor",
        "lenientdeath:items/equipment",
        "lenientdeath:items/foods"
    );
}
