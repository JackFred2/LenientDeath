package red.jackf.lenientdeath.restoreinventory;

import com.mojang.serialization.DataResult;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Inventory;

import java.time.Instant;

public record DeathRecord(Inventory inventory,
                          Instant timeOfDeath,
                          Component deathMessage,
                          GlobalPos location,
                          int experience) {
    private static final String INVENTORY = "Inventory";
    private static final String TIME_OF_DEATH = "TimeOfDeath";
    private static final String DEATH_MESSAGE = "DeathMessage";
    private static final String LOCATION = "Location";
    private static final String EXPERIENCE = "Experience";

    public static DataResult<DeathRecord> fromTag(ServerPlayer player, CompoundTag tag) {
        Inventory inventory;
        Instant timeOfDeath;
        Component deathMessage;
        GlobalPos location;

        if (tag.contains(INVENTORY, CompoundTag.TAG_LIST)) {
            inventory = new Inventory(player);
            inventory.load(tag.getList(INVENTORY, Tag.TAG_COMPOUND));
        } else {
            return DataResult.error(() -> "No inventory");
        }

        if (tag.contains(TIME_OF_DEATH)) {
            DataResult<Instant> timeOfDeathParsed = ExtraCodecs.INSTANT_ISO8601.parse(NbtOps.INSTANCE, tag.get(TIME_OF_DEATH));
            if (timeOfDeathParsed.result().isPresent()) {
                timeOfDeath = timeOfDeathParsed.result().get();
            } else {
                return timeOfDeathParsed.get()
                        .right()
                        .map(partial -> DataResult.<DeathRecord>error(() -> "Could not parse time of death: " + partial.message()))
                        .orElseThrow();
            }
        } else {
            return DataResult.error(() -> "No time of death");
        }

        if (tag.contains(DEATH_MESSAGE, Tag.TAG_STRING)) {
            deathMessage = Component.Serializer.fromJson(tag.getString(DEATH_MESSAGE));
            if (deathMessage == null) return DataResult.error(() -> "Could not parse death message");
        } else {
            return DataResult.error(() -> "No death message");
        }

        if (tag.contains(LOCATION)) {
            DataResult<GlobalPos> locationParsed = GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get(LOCATION));
            if (locationParsed.result().isPresent()) {
                location = locationParsed.result().get();
            } else {
                return locationParsed.get()
                        .right()
                        .map(partial -> DataResult.<DeathRecord>error(() -> "Could not parse location: " + partial.message()))
                        .orElseThrow();
            }
        } else {
            return DataResult.error(() -> "No location");
        }

        int experience = tag.getInt(EXPERIENCE);

        return DataResult.success(new DeathRecord(inventory,
                timeOfDeath,
                deathMessage,
                location,
                experience));
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        Tag inventory = this.inventory.save(new ListTag());
        Tag timeOfDeath = ExtraCodecs.INSTANT_ISO8601.encodeStart(NbtOps.INSTANCE, this.timeOfDeath).result().orElseThrow();
        Tag deathMessage = StringTag.valueOf(Component.Serializer.toJson(this.deathMessage));
        Tag location = GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, this.location).result().orElseThrow();
        Tag experience = IntTag.valueOf(this.experience);

        tag.put(INVENTORY, inventory);
        tag.put(TIME_OF_DEATH, timeOfDeath);
        tag.put(DEATH_MESSAGE, deathMessage);
        tag.put(LOCATION, location);
        tag.put(EXPERIENCE, experience);

        return tag;
    }
}
