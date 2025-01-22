package com.mazzy.mcuniversal.data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Forge capability provider for player dimension access data.
 * Handles registration, storage, and serialization of dimension permissions
 * using Minecraft's capability system and NBT persistence.
 */
public class PlayerDimensionDataProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {

    /**
     * Capability instance that identifies and provides access to dimension data.
     * Registered once during mod setup using CapabilityManager.
     */
    public static final Capability<IPlayerDimensionData> PLAYER_DIMENSION_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    // Backing implementation of the capability
    private final PlayerDimensionData backend = new PlayerDimensionData();

    // Lazy wrapper for capability instance (thread-safe initialization)
    private final LazyOptional<IPlayerDimensionData> optionalData = LazyOptional.of(() -> backend);

    /**
     * Provides access to the capability instance if requested
     * @param cap Capability being queried
     * @param side Logical side (client/server) - not used in this implementation
     * @return LazyOptional containing capability if it matches our PLAYER_DIMENSION_DATA
     */
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAYER_DIMENSION_DATA ? optionalData.cast() : LazyOptional.empty();
    }

    /**
     * Serializes capability data to NBT for world saving
     * @return CompoundTag containing all dimension access data
     */
    @Override
    public CompoundTag serializeNBT() {
        return backend.saveNBTData();
    }

    /**
     * Restores capability data from saved NBT
     * @param nbt CompoundTag containing previously saved dimension data
     */
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        backend.loadNBTData(nbt);
    }
}