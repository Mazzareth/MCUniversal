package com.mazzy.mcuniversal.data;
import java.util.Set;

public interface IPlayerDimensionData {
    boolean isDimensionUnlocked(String dimensionId);
    void unlockDimension(String dimensionId);
    Set<String> getUnlockedDimensions();
}