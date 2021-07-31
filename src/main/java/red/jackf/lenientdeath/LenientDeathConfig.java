package red.jackf.lenientdeath;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Config(name = LenientDeath.MODID)
@Config.Gui.Background("minecraft:textures/block/soul_sand.png")
public class LenientDeathConfig implements ConfigData {
    @ConfigEntry.Gui.PrefixText
    public boolean detectAutomatically = true;

    @ConfigEntry.Gui.PrefixText
    public boolean trinketsSafe = true;

    @ConfigEntry.Gui.Tooltip
    public List<String> items = new ArrayList<>(Arrays.asList(
        "minecraft:bucket",
        "minecraft:water_bucket",
        "minecraft:lava_bucket"
    ));

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public List<String> tags = new ArrayList<>(Arrays.asList(
        "lenientdeath:safe"
    ));
}
