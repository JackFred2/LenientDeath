package red.jackf.lenientdeath.config;

import blue.endless.jankson.Comment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import red.jackf.lenientdeath.LenientDeath;
import red.jackf.lenientdeath.mixinutil.LDPerPlayer;
import red.jackf.lenientdeath.preserveitems.ManualAllowAndBlocklist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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

        @Comment("""
                Whether to remove these comments from the config file. Not recommended.
                Options: true, false
                Default: false""")
        public boolean stripComments = false;
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
            Lenient Death can print a player's death coordinates in chat when they die, allowing players without minimaps
            to find their way back.""")
    public DeathCoordinates deathCoordinates = new DeathCoordinates();

    public static class DeathCoordinates {
        @Comment("""
                Whether players should be sent the coordinates they died at.
                Options: true, false
                Default: true""")
        public boolean sendToDeadPlayer = true;

        @Comment("""
                Whether a player's death coordinates should be sent to the server log / console.
                Options: true, false
                Default: true""")
        public boolean sendToServerLog = true;

        @Comment("""
                Whether a player's death coordinates should be sent to other online admins.
                Options: true, false
                Default: false""")
        public boolean sendToOtherAdmins = false;
    }

    @Comment("""
                Allows you to use Lenient Death on a per-player basis. This can be used if only some of your players want
                to use the mod. Affects item and XP preservation on death.
                Per-player mode can also be used with a permission plugin / mod, which will overwrite these settings.
                See the wiki for more details.""")
    public PerPlayer perPlayer = new PerPlayer();

    public static class PerPlayer {
        @Comment("""
                    The default enabled state for players when they join. If true, Lenient Death is enabled by default for
                    new players.
                    Options: true, false
                    Default: true""")
        public boolean defaultEnabledForPlayer = true;

        @Comment("""
                    Whether the player should be able to change their own per-player setting, using the Lenient Death
                    command. Admins can always change their own and other players' settings.
                    Options: true, false
                    Default: true""")
        public boolean playersCanChangeTheirOwnSetting = true;
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
                Options: dead_player, dead_player_and_team, everyone
                Default: dead_player_and_team""")
        public Visibility glowVisibility = Visibility.dead_player_and_team;

        @Comment("""
                Only applies if glowVisibility is set to 'dead_player_and_team'. If a player who is not on a team dies,
                should we show the item glow to other players not on a team?
                Options: true, false
                Default: false""")
        public boolean noTeamIsValidTeam = false;

        public enum Visibility {
            dead_player,
            dead_player_and_team,
            everyone
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
                To mitigate this, any item entities made to never despawn will be given the 'LENIENT_DEATH_INFINITE_LIFETIME'
                tag, so you can use the command '/kill @e[type=item,tag=LENIENT_DEATH_INFINITE_LIFETIME,nbt={Age: -32768s}]'
                to remove them.
                Options: true, false
                Default: false""")
        public boolean deathDropItemsNeverDespawn = false;
    }

    @Comment("""
            This feature allows you to give additional protection to item entities that were dropped on death, including
            fire, cactus, void recovery, blanket damage protection and more.""")
    public ItemResilience itemResilience = new ItemResilience();

    public static class ItemResilience {
        @Comment("""
                Whether all items dropped from a player's death should be fire and lava-proof.
                Options: true, false
                Default: false""")
        public boolean allDeathItemsAreFireProof = false;

        @Comment("""
                Whether all items dropped from a player's death should be immune to cacti.
                Options: true, false
                Default: false""")
        public boolean allDeathItemsAreCactusProof = false;

        @Comment("""
                Whether all items dropped from a player's death should be immune to explosions.
                Options: true, false
                Default: false""")
        public boolean allDeathItemsAreExplosionProof = false;

        @Comment("""
                Features related to handling item drops when a player dies to the void; for example if they fall off the
                end island, or are playing SkyBlock.""")
        public VoidRecovery voidRecovery = new VoidRecovery();

        public static class VoidRecovery {

            @Comment("""
                How death drop items that fall into the void should be handled?
                Options:
                  - disabled (despawn items as vanilla)
                  - last_grounded_position (teleport to last position either the player or the item was on a solid block)
                  - preserve (keep items in the inventory even if they wouldn't normally; applies to everyone)
                Default: last_grounded_position""")
            public Mode mode = Mode.last_grounded_position;

            @Comment("""
                    When a player dies to the void, should Lenient Death notify them where their items were moved to? Only
                    applies if mode = last_grounded_position.
                    This option exists because players who may not be aware of this feature probably would not look for
                    their items otherwise.
                    Options: true, false
                    Default: true""")
            public boolean announce = true;

            public enum Mode {
                disabled,
                last_grounded_position,
                preserve
            }
        }
    }

    @Comment("""
            In Vanilla, when a player dies they lose all of their experience, a small part of which is dropped as XP orbs.
            This feature lets you keep all or a portion of your experience when you die. Can be used on as part of the
            per-player feature.""")
    public PreserveExperienceOnDeath preserveExperienceOnDeath = new PreserveExperienceOnDeath();

    public static class PreserveExperienceOnDeath {
        @Comment("""
                Should this feature be enabled?
                Per-player mode can be overwritten by the appropriate permission, see the wiki for more details.
                Options: yes, per_player, no
                Default: no""")
        public PerPlayerEnabled enabled = PerPlayerEnabled.no;

        @Comment("""
                What percentage of a player's experience should be dropped on death?
                Options: [0, 100]
                Default: 60""")
        public int preservedPercentage = 60;
    }

    @Comment("""
            When a dead player's inventory is dropped, certain items can be kept based on their type, NBT, or an
            allow or block-list. Can be used on as part of the per-player feature.
            
            These rules are evaluated in the following order:
            NBT -> Always Dropped List -> Always Preserved List -> Item Type -> Randomizer""")
    public PreserveItemsOnDeath preserveItemsOnDeath = new PreserveItemsOnDeath();

    public static class PreserveItemsOnDeath {
        @Comment("""
                Should this feature be enabled?
                Per-player mode can be overwritten by the appropriate permission, see the wiki for more details.
                Options: yes, per_player, no
                Default: yes""")
        public PerPlayerEnabled enabled = PerPlayerEnabled.yes;

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
                from the following order: drop > preserve > ignore.""")
        public ByItemType byItemType = new ByItemType();

        @Comment("""
                Allows you to preserve items based on a random chance percentage. This is only applied to categories
                that haven't been decided by other modules.""")
        public Randomizer randomizer = new Randomizer();

        public static class Nbt {
            @Comment("""
                    Whether preserving based off a certain item NBT tag should be enabled.
                    Options: true, false
                    Default: false""")
            public boolean enabled = false;

            @Comment("""
                    The name of the NBT tag to look for. This is expected to be a Boolean, i.e. in the form {Soulbound: 1b}.
                    Options: A single word.
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
            public List<ResourceLocation> tags = new ArrayList<>(List.of(LenientDeath.id("safe")));
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
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior helmets = TypeBehavior.preserve;

            @Comment("""
                    Should chestplate-type items always drop, be preserved, or fall to further processing?
                    Example: Golden Chestplate
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior chestplates = TypeBehavior.preserve;

            @Comment("""
                    Should elytra-type items always drop, be preserved, or fall to further processing?
                    Example: Elytra
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior elytras = TypeBehavior.preserve;

            @Comment("""
                    Should leggings-type items always drop, be preserved, or fall to further processing?
                    Example: Diamond leggings
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior leggings = TypeBehavior.preserve;

            @Comment("""
                    Should boots-type items always drop, be preserved, or fall to further processing?
                    Example: Chainmail boots
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior boots = TypeBehavior.preserve;

            @Comment("""
                    Should shield-type items always drop, be preserved, or fall to further processing?
                    Example: Shield
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior shields = TypeBehavior.preserve;

            @Comment("""
                    Should other equippable items always drop, be preserved, or fall to further processing?
                    Example: Skulls
                    Options: drop, preserve, ignore
                    Default: ignore""")
            public TypeBehavior otherEquippables = TypeBehavior.ignore;

            @Comment("""
                    Should sword-type items always drop, be preserved, or fall to further processing?
                    Example: Wooden Sword
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior swords = TypeBehavior.preserve;

            @Comment("""
                    Should trident-type items always drop, be preserved, or fall to further processing?
                    Example: Wooden Sword
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior tridents = TypeBehavior.preserve;

            @Comment("""
                    Should bow-type items always drop, be preserved, or fall to further processing?
                    Example: Bow
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior bows = TypeBehavior.preserve;

            @Comment("""
                    Should crossbow-type items always drop, be preserved, or fall to further processing?
                    Example: Crossbow
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior crossbows = TypeBehavior.preserve;

            @Comment("""
                    Should other projectile-launching items always drop, be preserved, or fall to further processing?
                    Example: <None in Vanilla>
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior otherProjectileLaunchers = TypeBehavior.preserve;

            @Comment("""
                    Should pickaxe-type items always drop, be preserved, or fall to further processing?
                    Example: Netherite Pickaxe
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior pickaxes = TypeBehavior.preserve;

            @Comment("""
                    Should shovel-type items always drop, be preserved, or fall to further processing?
                    Example: Iron Shovel
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior shovels = TypeBehavior.preserve;

            @Comment("""
                    Should axe-type items always drop, be preserved, or fall to further processing?
                    Example: Diamond Axe
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior axes = TypeBehavior.preserve;

            @Comment("""
                    Should hoe-type items always drop, be preserved, or fall to further processing?
                    Example: Golden Hoe
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior hoes = TypeBehavior.preserve;

            @Comment("""
                    Should other digging items not in the above categories always drop, be preserved, or fall to further
                    processing?
                    Example: <None in Vanilla>
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior otherDiggingItems = TypeBehavior.preserve;

            @Comment("""
                    Should tools not in the above categories always drop, be preserved, or fall to further processing?
                    Example: Brush, Spyglass, Goat Horn
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior otherTools = TypeBehavior.preserve;

            @Comment("""
                    Should buckets always drop, be preserved, or fall to further processing?
                    Example: Bucket, Bucket of Water, Bucket of Lava
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior buckets = TypeBehavior.preserve;

            @Comment("""
                    Should food items always drop, be preserved, or fall to further processing?
                    Example: Cooked Steak, Mushroom Stew
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior food = TypeBehavior.preserve;

            @Comment("""
                    Should potion items always drop, be preserved, or fall to further processing?
                    Example: Water Bottle, Potion of Instant Health II
                    Options: drop, preserve, ignore
                    Default: preserve""")
            public TypeBehavior potions = TypeBehavior.preserve;

            @Comment("""
                    Should shulker boxes always drop, be preserved, or fall to further processing?
                    Note: Items contained within won't be checked, so this may be used to cheese other settings.
                    Example: Shulker Box
                    Options: drop, preserve, ignore
                    Default: ignore""")
            public TypeBehavior shulkerBoxes = TypeBehavior.ignore;

            public enum TypeBehavior {
                drop,
                preserve,
                ignore;

                public TypeBehavior and(TypeBehavior other) {
                    if (this.ordinal() > other.ordinal()) return other;
                    return this;
                }
            }
        }

        public static class Randomizer {
            @Comment("""
                    Whether preserving undecided items using RNG should be enabled.
                    Options: true, false
                    Default: false""")
            public boolean enabled = false;

            @Comment("""
                     What percentage of a player's undecided items be preserved on death? This is an average, and does
                     not guarantee a set amount of items.
                     Options: [0, 100]
                     Default: 25""")
            public int preservedPercentage = 25;

            @Comment("""
                    How much a player's luck attribute should add to the item preservation chance. Also applies to
                    negative luck. For more information, see https://minecraft.wiki/w/Attribute#Attributes_for_players
                    
                    Set to 0 to have no effect. Recommended to only have one of loadAdditiveFactor and
                    luckMultiplierFactor be above 0 at one time.
                    
                    The final preservation chance is calculated as follows:
                    chance = (preservedPercentage * (1 + (luckMultiplierFactor * playerLuck)) + (luckAdditiveFactor * playerLuck).
                    Options: [0, 200]
                    Default: 20""")
            public int luckAdditiveFactor = 20;

            @Comment("""
                    How much a player's luck attribute should multiply the item preservation chance. Also applies to
                    negative luck. For more information, see https://minecraft.wiki/w/Attribute#Attributes_for_players
                    
                    Set to 0 to have no effect. Recommended to only have one of loadAdditiveFactor and
                    luckMultiplierFactor be above 0 at one time.
                    
                    The final preservation chance is calculated as follows:
                    chance = (preservedPercentage * (1 + (luckMultiplierFactor * playerLuck)) + (luckAdditiveFactor * playerLuck).
                    Options: [0, 10]
                    Default: 0""")
            public float luckMultiplierFactor = 0f;
        }
    }

    public enum PerPlayerEnabled {
        yes(p -> true),
        per_player(LDPerPlayer::isEnabledFor),
        no(p -> false);

        private final Predicate<Player> test;

        PerPlayerEnabled(Predicate<Player> test) {
            this.test = test;
        }

        public boolean test(Player player) {
            return test.test(player);
        }
    }

    public void verify() {
        var defaultInstance = new LenientDeathConfig();

        this.extendedDeathItemLifetime.deathDropItemLifetimeSeconds
                = Mth.clamp(this.extendedDeathItemLifetime.deathDropItemLifetimeSeconds, 0, 1800);

        if (this.preserveItemsOnDeath.nbt.nbtKey.isBlank())
            this.preserveItemsOnDeath.nbt.nbtKey = defaultInstance.preserveItemsOnDeath.nbt.nbtKey;

        this.preserveExperienceOnDeath.preservedPercentage
                = Mth.clamp(this.preserveExperienceOnDeath.preservedPercentage, 0, 100);

        this.preserveItemsOnDeath.randomizer.preservedPercentage
                = Mth.clamp(this.preserveItemsOnDeath.randomizer.preservedPercentage, 0, 100);

        this.preserveItemsOnDeath.randomizer.luckAdditiveFactor
                = Mth.clamp(this.preserveItemsOnDeath.randomizer.luckAdditiveFactor, 0, 200);

        this.preserveItemsOnDeath.randomizer.luckMultiplierFactor
                = Mth.clamp(this.preserveItemsOnDeath.randomizer.luckMultiplierFactor, 0, 10);
    }

    public void onLoad(@Nullable LenientDeathConfig old) {
        if (config.enableFileWatcher) ConfigChangeListener.INSTANCE.start();
        else ConfigChangeListener.INSTANCE.stop();

        ManualAllowAndBlocklist.INSTANCE.refreshItems();

        // update available in case of per player changes
        if (LenientDeath.getCurrentServer() != null) {
            for (ServerPlayer player : LenientDeath.getCurrentServer().getPlayerList().getPlayers()) {
                LenientDeath.getCurrentServer().getCommands().sendCommands(player);
            }
        }

        // re-save for comment changes
        if (old != null && old.config.stripComments != this.config.stripComments)
            LenientDeathConfig.INSTANCE.save();
    }
}
