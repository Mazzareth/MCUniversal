package com.mazzy.mcuniversal.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

public class PlayerDimensionData implements IPlayerDimensionData {
    // Store dimension IDs in a simple set
    private final Set<String> unlockedDimensions = new HashSet<>();

    @Override
    public boolean isDimensionUnlocked(String dimensionId) {
        return unlockedDimensions.contains(dimensionId);
    }

    @Override
    public void unlockDimension(String dimensionId) {
        unlockedDimensions.add(dimensionId);
    }

    @Override
    public Set<String> getUnlockedDimensions() {
        return unlockedDimensions;
    }

    /**
     * Write the unlocked dimensions to NBT for saving to the player’s data file.
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
     * Read the unlocked dimensions from NBT.
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