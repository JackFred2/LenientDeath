package red.jackf.lenientdeath.restoreinventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import red.jackf.lenientdeath.compat.TrinketsCompat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record TrinketsRecord(Map<String, Map<String, List<ItemStack>>> items) {
    public static Codec<TrinketsRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, ItemStack.OPTIONAL_CODEC.listOf())).fieldOf("items").forGetter(TrinketsRecord::items)
    ).apply(instance, TrinketsRecord::new));

    public static Optional<TrinketsRecord> from(ServerPlayer player) {
        if (!FabricLoader.getInstance().isModLoaded("trinkets")) return Optional.empty();

        var trinkets = TrinketsCompat.getAllTrinkets(player);
        if (trinkets.isEmpty()) return Optional.empty();
        return Optional.of(new TrinketsRecord(trinkets));
    }
}
