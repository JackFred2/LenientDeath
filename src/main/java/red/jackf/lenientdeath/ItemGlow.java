package red.jackf.lenientdeath;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.Nullable;
import red.jackf.jackfredlib.api.lying.glowing.EntityGlowLie;
import red.jackf.lenientdeath.config.ConfigHandler;

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
        var builder = EntityGlowLie.builder(entity)
                .colour(ChatFormatting.GREEN)
                .onTick(ItemGlow::itemGlowLieTickCallback);

        switch (ConfigHandler.INSTANCE.get().droppedItemGlow.glowVisibility) {
            case EVERYONE -> builder.createAndShow(player.server.getPlayerList().getPlayers());
            case DEAD_PLAYER -> builder.createAndShow(player);
            case DEAD_PLAYER_AND_TEAM -> builder.createAndShow(player.server.getPlayerList()
                                                                       .getPlayers()
                                                                       .stream()
                                                                       .filter(otherPlayer -> otherPlayer.getTeam() == player.getTeam())
                                                                       .toList());
        }
    }

    /**
     * Get the appropriate outline colour for a given item's lifetime.
     *
     * <ul>
     * <li> \inf - 3m  : Green </li>
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
        if (timeLeftSeconds >= 180) return ChatFormatting.GREEN;
        if (timeLeftSeconds >= 120) return ChatFormatting.YELLOW;
        if (timeLeftSeconds >= 60) return ChatFormatting.GOLD;
        if (timeLeftSeconds >= 30) return ChatFormatting.RED;
        return (timeLeftTicks % ITEM_FLASH_INTERVAL_TICKS) < (ITEM_FLASH_INTERVAL_TICKS / 2) ? ChatFormatting.RED : null;
    }

    private static void itemGlowLieTickCallback(ServerPlayer player, EntityGlowLie<ItemEntity> lie) {
        ItemEntity item = lie.entity();
        if (item.getRemovalReason() != null && item.getRemovalReason().shouldDestroy()) lie.fade(); // expired or pickup check

        int timeRemaining = ITEM_MAX_AGE - item.getAge();
        lie.setGlowColour(getColourForTimeLeft(timeRemaining));
    }
}
