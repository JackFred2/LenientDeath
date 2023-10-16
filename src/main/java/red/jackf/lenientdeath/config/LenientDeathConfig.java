package red.jackf.lenientdeath.config;

import blue.endless.jankson.Comment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.preserveitems.ManualAllowAndBlocklist;

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
            Options relating to Lenient Death's '/ld' command. Used to configure the mod in-game, as well as to manage
            per-player mode if enabled.""")
    public Command command = new Command();

    public static class Command {
        @Comment("""
                List of names to use for the command, in case one conflicts. To disable, remove all names and have an
                empty list []. Requires a world reload / server restart for changes to take effect.
                Options: Any string without whitespace.
                Default: ["lenientdeath", "ld"]""")
        public List<String> commandNames = new ArrayList<>(List.of(
                "lenientdeath",
                "ld"
        ));
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
            By default, when a player dies their drops will despawn after 5 minutes. With this option, you can extend this
            so players are under less pressure to get their items back.""")
    public ExtendedDeathItemLifetime extendedDeathItemLifetime = new ExtendedDeathItemLifetime();

    public static class ExtendedDeathItemLifetime {
        @Comment("""
                Should this feature be enabled?
                Options: true, false
                Default: false""")
        public boolean enabled = false;

        @Comment("""
                How long should a dead player's dropped items last before despawning, in seconds? Ignored if items are set
                to never despawn.
                Options: Any number in the range [0, 1800] (0 minutes to 30 minutes). Vanilla despawn time is 300 seconds,
                         or 5 minutes. Setting a value below 300 will actually make items despawn sooner than vanilla.
                Default: 900 (15 minutes).""")
        public int deathDropItemLifetimeSeconds = 15 * 60;

        @Comment("""
                Should items dropped on a player's death never despawn? Please be aware that this may cause performance
                issues over a long period of time if players never try to recover their items as they will accumulate.
                To mitigate this, any item entities will be given the 'LENIENT_DEATH_INFINITE_LIFETIME' tag, so you can
                use the command '/kill @e[type=item,tag=LENIENT_DEATH_INFINITE_LIFETIME]' to remove them.
                Options: true, false
                Default: false""")
        public boolean deathDropItemsNeverDespawn = false;
    }

    @Comment("""
            When a dead player's inventory is dropped, certain items can be kept based on their type, NBT, or an
            allow or block-list.
            
            These rules are evaluated in the following order: NBT -> Always Dropped List -> Always Preserved List -> Item Type""")
    public PreserveItemsOnDeath preserveItemsOnDeath = new PreserveItemsOnDeath();

    public static class PreserveItemsOnDeath {
        @Comment("""
                Should this feature be enabled?
                Options: true, false
                Default: false""")
        public boolean enabled = true;

        @Comment("""
                Allows you to preserve items based on the presence of an NBT tag. Note that Lenient Death doesn't add this
                NBT tag to items for you, they will need to be added via another method.""")
        public Nbt nbt = new Nbt();

        @Comment("""
                Configures which items should always be dropped on death, ignoring the other settings based on NBT
                or type settings. Takes precedence over the Always Preserved config.""")
        public AlwaysDropped alwaysDropped = new AlwaysDropped();

        @Comment("""
                Configures which items should always be kept on death, ignoring the other settings based on NBT
                or type settings. Has a lower priority than the Always Dropped filter.""")
        public AlwaysPreserved alwaysPreserved = new AlwaysPreserved();

        @Comment("""
                Allows you to preserve or drop items based on their type (armour, weapon, food, etc). Has better compatibility
                with mods which don't add their items to various tags. Items part of multiple types will use the first result
                from the following order: DROP > PRESERVE > IGNORE""")
        public ByItemType byItemType = new ByItemType();

        @Comment("""
                Allows you to use Lenient Death on a per-player basis. This can be used if only some of your players want
                to use the mod. Does not affect other Lenient Death features.""")
        public PerPlayer perPlayer = new PerPlayer();

        // TODO implement
        public static class PerPlayer {
            @Comment("""
                    Whether Lenient Death item preservation should be on a per player basis.
                    Options: true, false
                    Default: false""")
            public boolean enabled = false;

            @Comment("""
                    The default enabled state for players when they join. If true, Lenient Death is default enabled for
                    the player.
                    Options: true, false
                    Default: true""")
            public boolean defaultEnabledForPlayer = true;

            @Comment("""
                    Whether the player should be able to change their own per-player setting, using the Lenient Death
                    command. Admins can always change other players' settings.
                    Options: true, false
                    Default: true""")
            public boolean playersCanChangeTheirOwnSetting = true;
        }

        public static class Nbt {
            @Comment("""
                    Whether preserving based off item NBT should be enabled.
                    Options: true, false
                    Default: false""")
            public boolean enabled = false;

            @Comment("""
                    The name of the NBT tag to look for. This is expected to be a Boolean, i.e. in the form {Soulbound: 1b}.
                    Options: A String of characters, with a length of at least 1. Must not be wholly whitespace characters.
                    Default: 'Soulbound'""")
            public String nbtKey = "Soulbound";
        }

        public static class AlwaysPreserved {
            @Comment("""
                    Which items should always be kept on a player's death?
                    Options: 0 or more Item IDs, in the form "minecraft:golden_pickaxe". If an item by a given ID doesn't exist,
                             a warning will be logged to the console.
                    Default: Empty list""")
            public List<ResourceLocation> items = new ArrayList<>();

            @Comment("""
                    Which item tags should always be kept on a player's death?
                    Options: 0 or more Item IDs, in the form "minecraft:swords" without a '#'. If a tag by a given ID doesn't
                             exist, a warning will be logged to the console.
                    Default: 'lenientdeath:safe'""")
            public List<ResourceLocation> tags = new ArrayList<>(List.of(new ResourceLocation(LenientDeath.MODID, "safe")));
        }

        public static class AlwaysDropped {
            @Comment("""
                    Which items should always be dropped on a player's death?
                    Options: 0 or more Item IDs, in the form "minecraft:golden_pickaxe". If an item by a given ID doesn't exist,
                             a warning will be logged to the console.
                    Default: Empty list""")
            public List<ResourceLocation> items = new ArrayList<>();

            @Comment("""
                    Which item tags should always be dropped on a player's death?
                    Options: 0 or more Item IDs, in the form "minecraft:swords" without a '#'. If a tag by a given ID doesn't
                             exist, a warning will be logged to the console.
                    Default: Empty list""")
            public List<ResourceLocation> tags = new ArrayList<>();
        }

        public static class ByItemType {
            @Comment("""
                    Whether preserving based off item type should be enabled.
                    Options: true, false
                    Default: true""")
            public boolean enabled = true;

            @Comment("""
                    Should helmet-type items always drop, be preserved, or fall to further processing?
                    Examples: Iron Helmet, Turtle Helmet
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior helmets = TypeBehavior.PRESERVE;

            @Comment("""
                    Should chestplate-type items always drop, be preserved, or fall to further processing?
                    Example: Golden Chestplate
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior chestplates = TypeBehavior.PRESERVE;

            @Comment("""
                    Should elytra-type items always drop, be preserved, or fall to further processing?
                    Example: Elytra
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior elytras = TypeBehavior.PRESERVE;

            @Comment("""
                    Should leggings-type items always drop, be preserved, or fall to further processing?
                    Example: Diamond leggings
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior leggings = TypeBehavior.PRESERVE;

            @Comment("""
                    Should boots-type items always drop, be preserved, or fall to further processing?
                    Example: Chainmail boots
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior boots = TypeBehavior.PRESERVE;

            @Comment("""
                    Should shield-type items always drop, be preserved, or fall to further processing?
                    Example: Shield
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior shields = TypeBehavior.PRESERVE;

            @Comment("""
                    Should other equippable items always drop, be preserved, or fall to further processing?
                    Example: Skulls
                    Options: DROP, PRESERVE, IGNORE
                    Default: IGNORE""")
            public TypeBehavior otherEquippables = TypeBehavior.IGNORE;

            @Comment("""
                    Should sword-type items always drop, be preserved, or fall to further processing?
                    Example: Wooden Sword
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior swords = TypeBehavior.PRESERVE;

            @Comment("""
                    Should trident-type items always drop, be preserved, or fall to further processing?
                    Example: Wooden Sword
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior tridents = TypeBehavior.PRESERVE;

            @Comment("""
                    Should bow-type items always drop, be preserved, or fall to further processing?
                    Example: Bow
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior bows = TypeBehavior.PRESERVE;

            @Comment("""
                    Should crossbow-type items always drop, be preserved, or fall to further processing?
                    Example: Crossbow
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior crossbows = TypeBehavior.PRESERVE;

            @Comment("""
                    Should other projectile-launching items always drop, be preserved, or fall to further processing?
                    Example: <None in Vanilla>
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior otherProjectileLaunchers = TypeBehavior.PRESERVE;

            @Comment("""
                    Should pickaxe-type items always drop, be preserved, or fall to further processing?
                    Example: Netherite Pickaxe
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior pickaxes = TypeBehavior.PRESERVE;

            @Comment("""
                    Should shovel-type items always drop, be preserved, or fall to further processing?
                    Example: Iron Shovel
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior shovels = TypeBehavior.PRESERVE;

            @Comment("""
                    Should axe-type items always drop, be preserved, or fall to further processing?
                    Example: Diamond Axe
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior axes = TypeBehavior.PRESERVE;

            @Comment("""
                    Should hoe-type items always drop, be preserved, or fall to further processing?
                    Example: Golden Hoe
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior hoes = TypeBehavior.PRESERVE;

            @Comment("""
                    Should other digging items not in the above categories always drop, be preserved, or fall to further
                    processing?
                    Example: <None in Vanilla>
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior otherDiggingItems = TypeBehavior.PRESERVE;

            @Comment("""
                    Should tools not in the above categories always drop, be preserved, or fall to further processing?
                    Example: Brush, Spyglass, Goat Horn
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior otherTools = TypeBehavior.PRESERVE;

            @Comment("""
                    Should buckets always drop, be preserved, or fall to further processing?
                    Example: Bucket, Bucket of Water, Bucket of Lava
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior buckets = TypeBehavior.PRESERVE;

            @Comment("""
                    Should food items always drop, be preserved, or fall to further processing?
                    Example: Cooked Steak, Mushroom Stew
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior food = TypeBehavior.PRESERVE;

            @Comment("""
                    Should potion items always drop, be preserved, or fall to further processing?
                    Example: Water Bottle, Potion of Instant Health II
                    Options: DROP, PRESERVE, IGNORE
                    Default: PRESERVE""")
            public TypeBehavior potions = TypeBehavior.PRESERVE;

            @Comment("""
                    Should shulker boxes always drop, be preserved, or fall to further processing?
                    Note: Items contained within won't be checked, so this may be used to cheese other settings.
                    Example: Shulker Box
                    Options: DROP, PRESERVE, IGNORE
                    Default: IGNORE""")
            public TypeBehavior shulkerBoxes = TypeBehavior.IGNORE;

            public enum TypeBehavior {
                DROP,
                PRESERVE,
                IGNORE;

                public TypeBehavior and(TypeBehavior other) {
                    if (this.ordinal() > other.ordinal()) return other;
                    return this;
                }
            }
        }
    }

    public void verify() {
        var defaultInstance = new LenientDeathConfig();

        this.extendedDeathItemLifetime.deathDropItemLifetimeSeconds
                = Mth.clamp(this.extendedDeathItemLifetime.deathDropItemLifetimeSeconds, 0, 1800);

        if (this.preserveItemsOnDeath.nbt.nbtKey.isBlank())
            this.preserveItemsOnDeath.nbt.nbtKey = defaultInstance.preserveItemsOnDeath.nbt.nbtKey;
    }

    public void onLoad() {
        if (config.enableFileWatcher) ConfigChangeListener.INSTANCE.start();
        else ConfigChangeListener.INSTANCE.stop();

        ManualAllowAndBlocklist.INSTANCE.refreshItems();
    }
}
