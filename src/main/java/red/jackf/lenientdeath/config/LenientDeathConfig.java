package red.jackf.lenientdeath.config;

import blue.endless.jankson.Comment;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class LenientDeathConfig {
    public static ConfigHandler INSTANCE = new ConfigHandler();

    @Comment("""
            Options relating to handling this config file. Note that disabling any of these options might requires you
            to restart the server to enable changes.""")
    public Config config = new Config();

    public static class Config {
        @Comment("""
                Should Lenient Death watch this config file for changes, and auto reload them? Useful for servers.
                Options: true, false
                Default: true""")
        public boolean enableFileWatcher = true;
    }

    @Comment("""
            When players die, any dropped items from their inventory will have a glowing outline shown through walls,
            in order to help them find and recover their items. This outline only shows to the player who died and their
            team, unless changed in the settings.""")
    public DroppedItemGlow droppedItemGlow = new DroppedItemGlow();
    public static class DroppedItemGlow {
        @Comment("""
                Should this feature be enabled?
                Options: true, false
                Default: true""")
        public boolean enabled = true;
        @Comment("""
                Who should we show a dead player's items' outlines to?
                Options: DEAD_PLAYER, DEAD_PLAYER_AND_TEAM, EVERYONE
                Default: DEAD_PLAYER_AND_TEAM""")
        public Visibility glowVisibility = Visibility.DEAD_PLAYER_AND_TEAM;

        public enum Visibility {
            DEAD_PLAYER,
            DEAD_PLAYER_AND_TEAM,
            EVERYONE
        }
    }

    @Comment("""
            When a dead player's inventory is dropped, certain items will be kept based on their type, NBT, or an
            allow-list. This behavior can be configured here, and can be set to work on a per-player basis.""")
    public PreserveItemsOnDeath preserveItemsOnDeath = new PreserveItemsOnDeath();

    public static class PreserveItemsOnDeath {
        @Comment("""
                Configures which items should always be kept on death, ignoring the other settings based on NBT
                or type settings. Has a lower priority than the Always Dropped filter.""")
        public AlwaysPreserved alwaysPreserved = new AlwaysPreserved();

        @Comment("""
                Configures which items should always be dropped on death, ignoring the other settings based on NBT
                or type settings. Takes precedence over the Always Preserved Container""")
        public AlwaysDropped alwaysDropped = new AlwaysDropped();

        public static class AlwaysPreserved {
            @Comment("""
                    Which items should always be kept on a player's death?
                    Options: 0 or more Item IDs, in the form "minecraft:golden_pickaxe". If an item by a given ID doesn't exist,
                             it is silently ignored when loading.
                    Default: Empty list""")
            public List<ResourceLocation> items = new ArrayList<>(List.of(new ResourceLocation("water_bucket")));

            @Comment("""
                    Which item tags should always be kept on a player's death?
                    Options: 0 or more Item IDs, in the form "minecraft:swords" without a '#'. If a tag by a given ID doesn't
                             exist it is silently ignored when loading.
                    Default: Empty list""")
            public List<ResourceLocation> tags = new ArrayList<>();
        }

        public static class AlwaysDropped {
            @Comment("""
                    Which items should always be dropped on a player's death?
                    Options: 0 or more Item IDs, in the form "minecraft:golden_pickaxe". If an item by a given ID doesn't exist,
                             it is silently ignored when loading.
                    Default: Empty list""")
            public List<ResourceLocation> items = new ArrayList<>();

            @Comment("""
                    Which item tags should always be dropped on a player's death?
                    Options: 0 or more Item IDs, in the form "minecraft:swords" without a '#'. If a tag by a given ID doesn't
                             exist it is silently ignored when loading.
                    Default: Empty list""")
            public List<ResourceLocation> tags = new ArrayList<>();
        }
    }

    public void onLoad() {
        if (config.enableFileWatcher) ConfigChangeListener.INSTANCE.start();
        else ConfigChangeListener.INSTANCE.stop();
    }
}
