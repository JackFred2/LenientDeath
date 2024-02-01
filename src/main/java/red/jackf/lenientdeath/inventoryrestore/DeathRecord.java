package red.jackf.lenientdeath.inventoryrestore;

import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public record DeathRecord(Inventory inventory,
                          Instant timeOfDeath,
                          Component deathMessage,
                          BlockPos location,
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
        BlockPos location;

        if (tag.contains(INVENTORY, CompoundTag.TAG_LIST)) {
            inventory = new Inventory(player);
            inventory.load(tag.getList(INVENTORY, Tag.TAG_COMPOUND));
        } else {
            return DataResult.error(() -> "No inventory");
        }

        if (tag.contains(TIME_OF_DEATH, Tag.TAG_STRING)) {
            try {
                timeOfDeath = Instant.parse(tag.getString(TIME_OF_DEATH));
            } catch (DateTimeParseException ignored) {
                return DataResult.error(() -> "Could not parse time of death");
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

        if (tag.contains(LOCATION, Tag.TAG_INT_ARRAY)) {
            DataResult<BlockPos> locationParsed = BlockPos.CODEC.parse(NbtOps.INSTANCE, tag.get(LOCATION));
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

        ListTag inventory = this.inventory.save(new ListTag());
        StringTag timeOfDeath = StringTag.valueOf(this.timeOfDeath.toString());
        StringTag deathMessage = StringTag.valueOf(Component.Serializer.toJson(this.deathMessage));
        Tag location = BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, this.location).result().orElseThrow();
        Tag experience = IntTag.valueOf(this.experience);

        tag.put(INVENTORY, inventory);
        tag.put(TIME_OF_DEATH, timeOfDeath);
        tag.put(DEATH_MESSAGE, deathMessage);
        tag.put(LOCATION, location);
        tag.put(EXPERIENCE, experience);

        return tag;
    }
}
