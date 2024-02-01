package red.jackf.lenientdeath.restoreinventory;

import net.minecraft.Util;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import red.jackf.lenientdeath.LenientDeath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RestoreInventory {
    public static final RestoreInventory INSTANCE = new RestoreInventory();

    private static final String DEATHS = "Deaths";
    private static final Logger LOGGER = LenientDeath.getLogger("Inventory Restore");

    private static Path getDeathHistoryDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("lenientdeath_previousdeaths");
    }

    private static Path getPlayerDeathHistoryPath(ServerPlayer player) {
        return getDeathHistoryDir(player.server).resolve(player.getStringUUID() + ".dat");
    }

    public List<DeathRecord> getDeathHistory(ServerPlayer player) {
        Path playerPath = getPlayerDeathHistoryPath(player);

        List<DeathRecord> inventories = new ArrayList<>();
        CompoundTag data = null;

        if (Files.isRegularFile(playerPath)) {
            try {
                data = NbtIo.readCompressed(playerPath, NbtAccounter.unlimitedHeap());
            } catch (IOException e) {
                LOGGER.error("Error decoding player %s death history".formatted(player.getScoreboardName()), e);
            }
        }

        if (data != null && data.contains(DEATHS, CompoundTag.TAG_LIST)) {
            ListTag list = data.getList(DEATHS, Tag.TAG_COMPOUND);

            for (Tag tag : list) {
                if (tag instanceof CompoundTag recordTag) {
                    var parsed = DeathRecord.fromTag(player, recordTag);
                    parsed.resultOrPartial(LOGGER::error).ifPresent(inventories::add);
                }
            }
        }

        return inventories;
    }

    public void saveDeathHistory(ServerPlayer player, List<DeathRecord> deathRecords) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        root.put(DEATHS, list);

        for (DeathRecord record : deathRecords) {
            list.add(record.toTag());
        }

        try {
            Path directory = getDeathHistoryDir(player.server);
            Files.createDirectories(directory);
            Path temp = Files.createTempFile(directory, player.getStringUUID() + "-", ".dat");
            NbtIo.writeCompressed(root, temp);
            Path realPath = directory.resolve(player.getStringUUID() + ".dat");
            Path backupPath = directory.resolve(player.getStringUUID() + ".dat_old");
            Util.safeReplaceFile(realPath, temp, backupPath);
        } catch (IOException e) {
            LOGGER.error("Couldn't save death history", e);
        }
    }

    public void onDeath(ServerPlayer player) {
        DeathRecord record = new DeathRecord(
                player.getInventory(),
                Instant.now(),
                player.getCombatTracker().getDeathMessage(),
                GlobalPos.of(player.level().dimension(), player.blockPosition()),
                player.totalExperience
        );

        List<DeathRecord> history = getDeathHistory(player);

        history.add(0, record);

        while (history.size() > LenientDeath.CONFIG.instance().inventoryRestore.maxInventoriesSaved)
            history.remove(history.size() - 1);

        saveDeathHistory(player, history);
    }
}
