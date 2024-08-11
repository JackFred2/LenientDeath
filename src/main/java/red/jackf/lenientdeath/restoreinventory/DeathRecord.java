package red.jackf.lenientdeath.restoreinventory;

import com.mojang.serialization.DataResult;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Inventory;
import red.jackf.lenientdeath.LenientDeath;

import java.time.Instant;
import java.util.Optional;

public record DeathRecord(Inventory inventory,
                          Optional<TrinketsRecord> trinketsInventory,
                          Instant timeOfDeath,
                          Component deathMessage,
                          GlobalPos location,
                          int experience) {
    private static final String INVENTORY = "Inventory";
    private static final String TRINKETS_INVENTORY = "TrinketsInventory";
    private static final String TIME_OF_DEATH = "TimeOfDeath";
    private static final String DEATH_MESSAGE = "DeathMessage";
    private static final String LOCATION = "Location";
    private static final String EXPERIENCE = "Experience";

    public static DataResult<DeathRecord> fromTag(ServerPlayer player, CompoundTag tag) {
        Inventory inventory;
        Optional<TrinketsRecord> trinkets;
        Instant timeOfDeath;
        Component deathMessage;
        GlobalPos location;

        if (tag.contains(INVENTORY, CompoundTag.TAG_LIST)) {
            inventory = new Inventory(player);
            inventory.load(tag.getList(INVENTORY, Tag.TAG_COMPOUND));
        } else {
            return DataResult.error(() -> "No inventory");
        }

        if (tag.contains(TRINKETS_INVENTORY, Tag.TAG_COMPOUND)) {
            DataResult<TrinketsRecord> trinketsParsed = TrinketsRecord.CODEC.parse(NbtOps.INSTANCE, tag.get(TRINKETS_INVENTORY));
            if (trinketsParsed.result().isPresent()) {
                trinkets = trinketsParsed.result();
            } else {
                return trinketsParsed.error()
                        .map(partial -> DataResult.<DeathRecord>error(() -> "Could not parse trinkets inventory: " + partial.message()))
                        .orElseThrow();
            }
        } else {
            trinkets = Optional.empty();
        }

        if (tag.contains(TIME_OF_DEATH)) {
            DataResult<Instant> timeOfDeathParsed = ExtraCodecs.INSTANT_ISO8601.parse(NbtOps.INSTANCE, tag.get(TIME_OF_DEATH));
            if (timeOfDeathParsed.result().isPresent()) {
                timeOfDeath = timeOfDeathParsed.result().get();
            } else {
                return timeOfDeathParsed.error()
                        .map(partial -> DataResult.<DeathRecord>error(() -> "Could not parse time of death: " + partial.message()))
                        .orElseThrow();
            }
        } else {
            return DataResult.error(() -> "No time of death");
        }

        if (tag.contains(DEATH_MESSAGE, Tag.TAG_STRING)) {
            deathMessage = Component.Serializer.fromJson(tag.getString(DEATH_MESSAGE), player.server.registryAccess());
            if (deathMessage == null) return DataResult.error(() -> "Could not parse death message");
        } else {
            return DataResult.error(() -> "No death message");
        }

        if (tag.contains(LOCATION)) {
            DataResult<GlobalPos> locationParsed = GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get(LOCATION));
            if (locationParsed.result().isPresent()) {
                location = locationParsed.result().get();
            } else {
                return locationParsed.error()
                        .map(partial -> DataResult.<DeathRecord>error(() -> "Could not parse location: " + partial.message()))
                        .orElseThrow();
            }
        } else {
            return DataResult.error(() -> "No location");
        }

        int experience = tag.getInt(EXPERIENCE);

        return DataResult.success(new DeathRecord(inventory,
                trinkets,
                timeOfDeath,
                deathMessage,
                location,
                experience));
    }

    public CompoundTag toTag(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();

        tag.put(INVENTORY, this.inventory.save(new ListTag()));
        this.trinketsInventory.ifPresent(record -> encodeTrinket(tag, record));
        tag.put(TIME_OF_DEATH, ExtraCodecs.INSTANT_ISO8601.encodeStart(NbtOps.INSTANCE, this.timeOfDeath).result().orElseThrow());
        tag.put(DEATH_MESSAGE, StringTag.valueOf(Component.Serializer.toJson(this.deathMessage, player.server.registryAccess())));
        tag.put(LOCATION, GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, this.location).result().orElseThrow());
        tag.put(EXPERIENCE, IntTag.valueOf(this.experience));

        return tag;
    }

    private static void encodeTrinket(CompoundTag tag, TrinketsRecord trinketsRecord) {
        TrinketsRecord.CODEC.encodeStart(NbtOps.INSTANCE, trinketsRecord)
                .ifSuccess(trinkets -> tag.put(TRINKETS_INVENTORY, trinkets))
                .ifError(err -> LenientDeath.LOGGER.error("Error saving trinkets inventory: {}", err.message()));
    }
}
