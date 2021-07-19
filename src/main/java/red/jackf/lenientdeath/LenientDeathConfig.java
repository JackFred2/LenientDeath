package red.jackf.lenientdeath;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.List;

@Config(name = LenientDeath.MODID)
@Config.Gui.CategoryBackground(category = "settings", background = "minecraft:textures/block/soul_sand.png")
public class LenientDeathConfig implements ConfigData {
    @Comment("List of item IDs to be kept upon death")
    public List<String> items = List.of(
        "minecraft:apple"
    );

    @Comment("List of item tags to be kept upon death")
    public List<String> tags = List.of(
        "lenientdeath:items/equipment",
        "lenientdeath:items/armor"
    );
}
