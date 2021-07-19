package red.jackf.lenientdeath;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@Config(name = LenientDeath.MODID)
@Config.Gui.CategoryBackground(category = "visual_options", background = "minecraft:textures/block/soul_sand.png")
public class LenientDeathConfig implements ConfigData {
    public List<ListOption> options = new ArrayList<>();

    public static class ListOption {
        public ListOptionType type = ListOptionType.ITEM;
        public String value = "minecraft:diamond_pickaxe";
    }

    public enum ListOptionType {
        ITEM,
        TAG
    }
}
