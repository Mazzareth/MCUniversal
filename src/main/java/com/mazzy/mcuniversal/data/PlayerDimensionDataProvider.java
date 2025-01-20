package com.mazzy.mcuniversal.data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Capability provider that attaches PlayerDimensionData to Player entities.
 * Also handles serialization and deserialization.
 */
public class PlayerDimensionDataProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
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
     * Saves the player's dimension data to an NBT tag
     * so it can be written to the player's data file.
     * (Forge calls this automatically once it's an ICapabilitySerializable.)
     */
    @Override
    public CompoundTag serializeNBT() {
        return backend.saveNBTData();
    }

    /**
     * Loads the player's dimension data from NBT when the world/player data is read.
     * (Forge calls this automatically once it's an ICapabilitySerializable.)
     */
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        backend.loadNBTData(nbt);
    }
}