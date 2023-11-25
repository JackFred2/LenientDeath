package red.jackf.lenientdeath;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.Nullable;
import red.jackf.jackfredlib.api.lying.glowing.EntityGlowLie;

/**
 * Adds a glowing outline to dropped items for players to easily find their items again. Only shown to the given player.
 */
public class ItemGlow {
    /**
     * TODO: figure out a way to read if other mods change this. Low prio
     */
    private static final int ITEM_MAX_AGE = 6000;
    private static final int ITEM_FLASH_INTERVAL_TICKS = 10;

    private ItemGlow() {}

    /**
     * Add the outline to a given entity, and show the outline to the player.
     * TODO update JFLib to allow lies to persist over disconnect
     *
     * @param player Player whose items were dropped.
     * @param entity Item that was dropped.
     */
    public static void addItemGlow(ServerPlayer player, ItemEntity entity) {
        var config = LenientDeath.CONFIG.instance().droppedItemGlow;
        if (!config.enabled) return;

        var builder = EntityGlowLie.builder(entity)
                .colour(ChatFormatting.GREEN)
                .onTick(ItemGlow::itemGlowLieTickCallback);

        switch (config.glowVisibility) {
            case everyone -> builder.createAndShow(player.server.getPlayerList().getPlayers());
            case dead_player -> builder.createAndShow(player);
            case dead_player_and_team -> {
                if (!config.noTeamIsValidTeam && player.getTeam() == null) {
                    builder.createAndShow(player);
                } else {
                    builder.createAndShow(player.server.getPlayerList()
                                                       .getPlayers()
                                                       .stream()
                                                       .filter(otherPlayer -> otherPlayer.getTeam() == player.getTeam())
                                                       .toList());
                }
            }
        }
    }

    /**
     * Get the appropriate outline colour for a given item's lifetime.
     *
     * <ul>
     * <li> \inf - 5m  : Aqua </li>
     * <li> 5m   - 3m  : Green </li>
     * <li> 3m   - 2m  : Yellow </li>
     * <li> 2m   - 1m  : Orange </li>
     * <li> 1m   - 30s : Red </li>
     * <li> 30s  - 0s  : Flashing Red, 1 second interval </li>
     * </ul>
     *
     * @param timeLeftTicks How many ticks an item has left to live.
     * @return Colour for the outline to take.
     */
    @Nullable
    private static ChatFormatting getColourForTimeLeft(int timeLeftTicks) {
        int timeLeftSeconds = timeLeftTicks / SharedConstants.TICKS_PER_SECOND;
        if (timeLeftSeconds > 300) return ChatFormatting.AQUA;
        if (timeLeftSeconds > 180) return ChatFormatting.GREEN;
        if (timeLeftSeconds > 120) return ChatFormatting.YELLOW;
        if (timeLeftSeconds > 60) return ChatFormatting.GOLD;
        if (timeLeftSeconds > 30) return ChatFormatting.RED;
        return (timeLeftTicks % ITEM_FLASH_INTERVAL_TICKS) < (ITEM_FLASH_INTERVAL_TICKS / 2) ? ChatFormatting.RED : null;
    }

    private static void itemGlowLieTickCallback(ServerPlayer player, EntityGlowLie<ItemEntity> lie) {
        ItemEntity item = lie.entity();
        if (item.getRemovalReason() != null && item.getRemovalReason().shouldDestroy()) lie.fade(); // expired or pickup check

        if (item.getAge() == -32768) { // infinite lifetime
            lie.setGlowColour(ChatFormatting.LIGHT_PURPLE);
        } else {
            int timeRemaining = ITEM_MAX_AGE - item.getAge();
            lie.setGlowColour(getColourForTimeLeft(timeRemaining));
        }
    }
}
