//--------------------------------------------------------------------------------
// RTPDimensionData.java
// A SavedData-based class for storing globally unlocked dimensions.
// For Minecraft 1.20.1 (and similarly for modern 1.19+ versions).
//--------------------------------------------------------------------------------

package com.mazzy.mcuniversal.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public class RTPDimensionData extends SavedData {
    private static final String DATA_NAME = "rtp_dimension_data";

    // Store the unlocked dimension IDs in a set
    private final Set<String> unlockedDimensions = new HashSet<>();

    // Zero-arg constructor for "brand new" data
    public RTPDimensionData() {
    }

    // ------------------------------------------------------------------------
    // Loading from existing save data
    // Must be a static method named 'load' for SavedData in 1.19+.
    // ------------------------------------------------------------------------
    public static RTPDimensionData load(CompoundTag nbt) {
        RTPDimensionData data = new RTPDimensionData();
        ListTag list = nbt.getList("Dimensions", 8); // 8 = Tag.TAG_STRING
        for (int i = 0; i < list.size(); i++) {
            data.unlockedDimensions.add(list.getString(i));
        }
        return data;
    }

    // ------------------------------------------------------------------------
    // Called automatically when saving to disk
    // ------------------------------------------------------------------------
    @Override
    public CompoundTag save(CompoundTag compound) {
        ListTag list = new ListTag();
        for (String dimensionId : unlockedDimensions) {
            list.add(StringTag.valueOf(dimensionId));
        }
        compound.put("Dimensions", list);
        return compound;
    }

    // ------------------------------------------------------------------------
    // Retrieve or create this data for the Overworld (or any consistent dimension)
    // ------------------------------------------------------------------------
    public static RTPDimensionData get(ServerLevel level) {
        // Typically store global data in the overworld’s dataStorage
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(RTPDimensionData::load, RTPDimensionData::new, DATA_NAME);
    }

    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------
    public Set<String> getUnlockedDimensions() {
        return unlockedDimensions;
    }

    public boolean addDimension(String dimId) {
        boolean changed = unlockedDimensions.add(dimId);
        if (changed) {
            setDirty(); // Mark for saving
        }
        return changed;
    }

    public boolean removeDimension(String dimId) {
        boolean changed = unlockedDimensions.remove(dimId);
        if (changed) {
            setDirty(); // Mark for saving
        }
        return changed;
    }
}