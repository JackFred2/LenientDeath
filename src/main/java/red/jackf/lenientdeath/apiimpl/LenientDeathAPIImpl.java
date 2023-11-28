package red.jackf.lenientdeath.apiimpl;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;
import red.jackf.lenientdeath.ItemGlow;
import red.jackf.lenientdeath.ItemLifeExtender;
import red.jackf.lenientdeath.MoveToOriginalSlots;
import red.jackf.lenientdeath.api.LenientDeathAPI;
import red.jackf.lenientdeath.mixinutil.LDDeathDropMarkable;
import red.jackf.lenientdeath.preserveitems.PreserveItems;

import java.util.function.BiFunction;

public class LenientDeathAPIImpl implements LenientDeathAPI {
    public static final LenientDeathAPIImpl INSTANCE = new LenientDeathAPIImpl();

    @Override
    public boolean shouldItemBePreserved(ServerPlayer deadPlayer, ItemStack item) {
        return PreserveItems.shouldKeepOnDeath(deadPlayer, item);
    }

    @Override
    public void markDeathItem(ServerPlayer deadPlayer, ItemEntity item, @Nullable Integer sourceSlot) {
        ItemGlow.addItemGlow(deadPlayer, item);
        ItemLifeExtender.extendItemLifetime(item);
        ((LDDeathDropMarkable) item).lenientdeath$markDeathDropItem();
        if (sourceSlot != null) MoveToOriginalSlots.saveSlot(item, sourceSlot);
    }

    public static void setup() {
        FabricLoader.getInstance().getObjectShare().put(
                "lenientdeath:markDeathItem",
                (TriConsumer<ServerPlayer, ItemEntity, @Nullable Integer>) LenientDeathAPIImpl.INSTANCE::markDeathItem);
        FabricLoader.getInstance().getObjectShare().put(
                "lenientdeath:shouldItemBePreserved",
                (BiFunction<ServerPlayer, ItemStack, Boolean>) INSTANCE::shouldItemBePreserved);
    }
}
