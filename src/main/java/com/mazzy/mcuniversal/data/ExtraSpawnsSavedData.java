package com.mazzy.mcuniversal.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages persistent storage of custom spawn points in special dimensions
 * Tracks usage status to prevent spawn point reuse
 */
public class ExtraSpawnsSavedData extends SavedData {
    // Persistent storage identifier
    private static final String DATA_NAME = "mcuniversal_extra_spawns";

    /** Represents a single spawn point with usage state */
    public static class SpawnEntry {
        public int x;
        public int y;
        public int z;
        public boolean used; // True if this spawn has been claimed

        public SpawnEntry(int x, int y, int z, boolean used) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.used = used;
        }
    }

    private final List<SpawnEntry> spawnList = new ArrayList<>();

    /**
     * Gets or creates the spawn data for a dimension
     * @param level Target dimension to get data for
     * @return Existing or fresh spawn data instance
     */
    public static ExtraSpawnsSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                ExtraSpawnsSavedData::load,
                ExtraSpawnsSavedData::new,
                DATA_NAME
        );
    }

    /** Required constructor for data loading */
    public ExtraSpawnsSavedData() {}

    /**
     * Loads spawn data from NBT storage
     * @param tag Contains serialized spawn entries
     * @return Populated data instance
     */
    public static ExtraSpawnsSavedData load(CompoundTag tag) {
        ExtraSpawnsSavedData data = new ExtraSpawnsSavedData();
        ListTag spawnEntries = tag.getList("spawns", Tag.TAG_COMPOUND);

        // Reconstruct spawn list from saved NBT data
        for (Tag t : spawnEntries) {
            if (t instanceof CompoundTag c) {
                data.spawnList.add(new SpawnEntry(
                        c.getInt("x"),
                        c.getInt("y"),
                        c.getInt("z"),
                        c.getBoolean("used")
                ));
            }
        }
        return data;
    }

    /**
     * Saves spawn data to NBT format
     * @param compound Tag to write data into
     * @return Modified compound with spawn data
     */
    @Override
    public CompoundTag save(CompoundTag compound) {
        ListTag list = new ListTag();

        // Serialize all spawn entries
        for (SpawnEntry entry : spawnList) {
            CompoundTag c = new CompoundTag();
            c.putInt("x", entry.x);
            c.putInt("y", entry.y);
            c.putInt("z", entry.z);
            c.putBoolean("used", entry.used);
            list.add(c);
        }
        compound.put("spawns", list);
        return compound;
    }

    /**
     * Registers a new spawn point
     * @param pos Location to add as spawn point
     */
    public void addSpawn(BlockPos pos) {
        spawnList.add(new SpawnEntry(pos.getX(), pos.getY(), pos.getZ(), false));
        setDirty(); // Mark data for saving
    }

    /**
     * Claims the first available unused spawn point
     * @return SpawnEntry with marked usage, or null if none available
     */
    public SpawnEntry claimFreeSpawn() {
        for (SpawnEntry entry : spawnList) {
            if (!entry.used) {
                entry.used = true;
                setDirty(); // Mark data for saving
                return entry;
            }
        }
        return null;
    }

    /**
     * Checks for available spawn points without claiming them
     * @return First unused SpawnEntry or null
     */
    public SpawnEntry peekFreeSpawn() {
        for (SpawnEntry entry : spawnList) {
            if (!entry.used) {
                return entry;
            }
        }
        return null;
    }
}