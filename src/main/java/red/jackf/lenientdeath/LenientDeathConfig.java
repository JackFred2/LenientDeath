package red.jackf.lenientdeath;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

@Config(name = LenientDeath.MODID)
@Config.Gui.Background("minecraft:textures/block/soul_sand.png")
public class LenientDeathConfig implements ConfigData {
    @ConfigEntry.Gui.PrefixText
    public boolean detectAutomatically = true;

    @ConfigEntry.Gui.PrefixText
    public boolean trinketsSafe = true;

    @ConfigEntry.Gui.Tooltip
    public List<String> items = new ArrayList<>(List.of(
    ));

    @ConfigEntry.Gui.Tooltip(count = 2)
    public List<String> tags = new ArrayList<>(List.of(
        "lenientdeath:safe"
    ));
}
