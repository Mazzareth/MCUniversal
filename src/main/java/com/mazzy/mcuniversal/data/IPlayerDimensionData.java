package com.mazzy.mcuniversal.data;

import java.util.Set;

/**
 * Capability interface for storing per-player dimension data.
 * This includes methods to check/unlock dimension access and retrieve
 * the current set of unlocked dimensions for that player.
 */
public interface IPlayerDimensionData {

    /**
     * Returns true if the player has unlocked the specified dimension.
     *
     * @param dimensionId e.g., "minecraft:the_nether"
     * @return true if unlocked, false otherwise
     */
    boolean isDimensionUnlocked(String dimensionId);

    /**
     * Unlocks the specified dimension for the player if not already unlocked.
     *
     * @param dimensionId e.g., "minecraft:the_nether"
     */
    void unlockDimension(String dimensionId);

    /**
     * Returns a Set of all currently unlocked dimensions for this player.
     *
     * @return read-only or modifiable Set of dimension IDs
     */
    Set<String> getUnlockedDimensions();
}