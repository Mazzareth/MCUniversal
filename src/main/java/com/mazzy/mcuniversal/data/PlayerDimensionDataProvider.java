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

public class PlayerDimensionDataProvider implements ICapabilityProvider {
    public static final Capability<IPlayerDimensionData> PLAYER_DIMENSION_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerDimensionData backend = new PlayerDimensionData();
    private final LazyOptional<IPlayerDimensionData> optionalData = LazyOptional.of(() -> backend);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAYER_DIMENSION_DATA ? optionalData.cast() : LazyOptional.empty();
    }

    // For saving to NBT
    public CompoundTag serializeNBT() {
        return backend.saveNBTData();
    }

    // For loading from NBT
    public void deserializeNBT(CompoundTag nbt) {
        backend.loadNBTData(nbt);
    }
}