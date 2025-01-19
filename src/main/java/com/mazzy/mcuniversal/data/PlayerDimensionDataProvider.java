package com.mazzy.mcuniversal.data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Capability provider that attaches PlayerDimensionData to Player entities.
 * Also handles serialization and deserialization.
 */
public class PlayerDimensionDataProvider implements ICapabilityProvider {
    // Register (or retrieve) your capability reference.
    public static final Capability<IPlayerDimensionData> PLAYER_DIMENSION_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    // The data being stored for the player.
    private final PlayerDimensionData backend = new PlayerDimensionData();
    // Expose the data via a LazyOptional so other classes can safely access it.
    private final LazyOptional<IPlayerDimensionData> optionalData = LazyOptional.of(() -> backend);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAYER_DIMENSION_DATA ? optionalData.cast() : LazyOptional.empty();
    }

    /**
     * Saves the player’s dimension data to an NBT tag
     * so it can be written to the player's data file.
     */
    public CompoundTag serializeNBT() {
        return backend.saveNBTData();
    }

    /**
     * Loads the player’s dimension data from NBT when the world (or player) data is read.
     */
    public void deserializeNBT(CompoundTag nbt) {
        backend.loadNBTData(nbt);
    }
}