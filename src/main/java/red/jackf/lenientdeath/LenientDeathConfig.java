package red.jackf.lenientdeath;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.ArrayList;
import java.util.List;

@Config(name = LenientDeath.MODID)
@Config.Gui.Background("minecraft:textures/block/soul_sand.png")
public class LenientDeathConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    public List<String> items = new ArrayList<>(List.of(
        "minecraft:bucket",
        "minecraft:water_bucket",
        "minecraft:lava_bucket"
    ));

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.PrefixText
    public List<String> tags = new ArrayList<>(List.of(
        "lenientdeath:armor",
        "lenientdeath:equipment",
        "lenientdeath:foods"
    ));
}
