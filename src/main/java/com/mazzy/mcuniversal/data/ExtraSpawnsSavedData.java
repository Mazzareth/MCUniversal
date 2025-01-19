package com.mazzy.mcuniversal.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class ExtraSpawnsSavedData extends SavedData {

    private static final String DATA_NAME = "mcuniversal_extra_spawns";

    public static class SpawnEntry {
        public int x;
        public int y;
        public int z;
        public boolean used;

        public SpawnEntry(int x, int y, int z, boolean used) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.used = used;
        }
    }

    private final List<SpawnEntry> spawnList = new ArrayList<>();

    public static ExtraSpawnsSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                ExtraSpawnsSavedData::load,
                ExtraSpawnsSavedData::new,
                DATA_NAME
        );
    }

    public ExtraSpawnsSavedData() {}

    public static ExtraSpawnsSavedData load(CompoundTag tag) {
        ExtraSpawnsSavedData data = new ExtraSpawnsSavedData();
        ListTag spawnEntries = tag.getList("spawns", Tag.TAG_COMPOUND);
        for (Tag t : spawnEntries) {
            if (t instanceof CompoundTag c) {
                int x = c.getInt("x");
                int y = c.getInt("y");
                int z = c.getInt("z");
                boolean used = c.getBoolean("used");
                data.spawnList.add(new SpawnEntry(x, y, z, used));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        ListTag list = new ListTag();
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

    public void addSpawn(BlockPos pos) {
        spawnList.add(new SpawnEntry(pos.getX(), pos.getY(), pos.getZ(), false));
        setDirty();
    }

    /**
     * Claims (consumes) a free spawn by marking it as used.
     */
    public SpawnEntry claimFreeSpawn() {
        for (SpawnEntry entry : spawnList) {
            if (!entry.used) {
                entry.used = true;
                setDirty();
                return entry;
            }
        }
        return null;
    }

    /**
     * Retrieves the first free spawn but does NOT mark it as used.
     * If none is available, returns null.
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