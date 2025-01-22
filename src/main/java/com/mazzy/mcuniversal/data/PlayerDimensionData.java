package com.mazzy.mcuniversal.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

/**
 * NBT-based implementation of player dimension access permissions.
 * Stores unlocked dimensions in a HashSet and provides serialization
 * to/from Minecraft's NBT format for persistent storage.
 */
public class PlayerDimensionData implements IPlayerDimensionData {

    private final Set<String> unlockedDimensions = new HashSet<>();

    /**
     * Checks if a dimension is unlocked by verifying its presence in the internal set
     * @param dimensionId ResourceLocation-style ID to check (e.g., "minecraft:the_nether")
     * @return true if the dimension exists in the unlocked set, false otherwise
     */
    @Override
    public boolean isDimensionUnlocked(String dimensionId) {
        return unlockedDimensions.contains(dimensionId);
    }

    /**
     * Adds a dimension to the unlocked set using HashSet's built-in deduplication
     * @param dimensionId ResourceLocation-style ID to add to the unlocked set
     */
    @Override
    public void unlockDimension(String dimensionId) {
        unlockedDimensions.add(dimensionId);
    }

    /**
     * Returns direct reference to the unlocked dimensions set. Note: While this provides
     * access to the underlying collection, modifications should be made through unlockDimension
     * to ensure future compatibility with persistence systems
     * @return Mutable Set containing all currently unlocked dimension IDs
     */
    @Override
    public Set<String> getUnlockedDimensions() {
        return unlockedDimensions;
    }

    /**
     * Serializes unlocked dimensions to NBT format for world saving
     * @return CompoundTag containing a list of unlocked dimension IDs as strings
     */
    public CompoundTag saveNBTData() {
        CompoundTag tag = new CompoundTag();

        ListTag dimList = new ListTag();
        for (String dim : unlockedDimensions) {
            dimList.add(StringTag.valueOf(dim));
        }
        tag.put("UnlockedDimensions", dimList);

        return tag;
    }

    /**
     * Loads unlocked dimensions from saved NBT data, replacing current entries
     * @param nbt CompoundTag containing saved dimension data in NBT format
     */
    public void loadNBTData(CompoundTag nbt) {
        unlockedDimensions.clear();

        if (nbt.contains("UnlockedDimensions", Tag.TAG_LIST)) {
            ListTag dims = nbt.getList("UnlockedDimensions", Tag.TAG_STRING);
            for (int i = 0; i < dims.size(); i++) {
                unlockedDimensions.add(dims.getString(i));
            }
        }
    }
}