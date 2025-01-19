package com.mazzy.mcuniversal.data;

import java.util.Set;

/**
 * Capability interface for storing per-player dimension data.
 * Add any getters/setters you need to manage dimension unlocks,
 * or other relevant dimension-tracking info.
 */
public interface IPlayerDimensionData {

    /**
     * Returns true if the player has unlocked the specified dimension.
     */
    boolean isDimensionUnlocked(String dimensionId);

    /**
     * Unlocks the specified dimension for the player.
     */
    void unlockDimension(String dimensionId);

    /**
     * Returns a Set of all currently unlocked dimensions.
     */
    Set<String> getUnlockedDimensions();
}