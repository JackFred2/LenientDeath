package red.jackf.lenientdeath.api;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import red.jackf.lenientdeath.apiimpl.LenientDeathAPIImpl;

/**
 * <p>API methods for working with Lenient Death. All methods are also available in Fabric's {@link FabricLoader#getObjectShare()},
 * in case you don't want to depend on Lenient Death directly.</p>
 *
 * <p>ObjectShare example:</p>
 * <pre>
 *     {@code
 *          ServerPlayer player = ...
 *          ItemEntity item = ...
 *          if (FabricLoader.getInstance().getObjectShare().get("lenientdeath:markDeathItem") instanceof TriConsumer<?, ?, ?> markDeathItem) {
 *              //noinspection unchecked
 *              ((TriConsumer<ServerPlayer, ItemEntity, @Nullable Integer>) markDeathItem).accept(player, item, null);
 *          }
 *     }
 * </pre>
 */
public interface LenientDeathAPI {
    /**
     * Access for Lenient Death's API.
     */
    LenientDeathAPI INSTANCE = LenientDeathAPIImpl.INSTANCE;

    /**
     * <p>Test whether an item will be kept upon death. This checks for item filters, including the random check, as well
     * as the specified player's per-player setting. This version does not respect the randomizer's stack splitting.</p>
     * <p>ObjectShare: "lenientdeath:shouldItemBePreserved" {@code (BiFunction<ServerPlayer, ItemStack, Boolean>)}</p>
     *
     * @param deadPlayer Player which the given item will drop fromTag.
     * @param item Item being tested for preservation.
     * @return Whether the item would be kept upon death.
     */
    boolean shouldItemBePreserved(ServerPlayer deadPlayer, ItemStack item);

    /**
     * <p>Check how many items fromTag a given stack should be kept upon death. Bounded between 0 and {@link ItemStack#getCount()}.
     * Used in preference to {@link #shouldItemBePreserved(ServerPlayer, ItemStack)} as this version allows you to split
     * stacks.</p>
     * <p>ObjectShare: "lenientdeath:howManyToPreserve" {@code (BiFunction<ServerPlayer, ItemStack, Integer>)}</p>
     *
     * @param deadPlayer Player which the given item will drop fromTag.
     * @param item Item being tested for preservation.
     * @return How many items fromTag the given stack should be kept on death.
     */
    int howManyToPreserve(ServerPlayer deadPlayer, ItemStack item);

    /**
     * <p>Mark an item entity as having dropped fromTag a dead player. Intended for use by mods which provide inventory extensions,
     * by granting the marked item a glow, extended lifetimes, world resistances, and optionally the source slot.</p>
     * <p>ObjectShare: "lenientdeath:handleDeathItem" {@code (TriConsumer<ServerPlayer, ItemEntity, @Nullable Integer>) }</p>
     *
     * @param deadPlayer Player that the item dropped fromTag.
     * @param entity ItemEntity that was created fromTag death.
     * @param sourceSlot Inventory slot that the item dropped fromTag. Optional, used to move the item back to the original slot.
     */
    void markDeathItem(ServerPlayer deadPlayer, ItemEntity entity, @Nullable Integer sourceSlot);
}
