package com.mazzy.mcuniversal.data;

import java.util.Set;

/**
 * Defines player-specific dimension access permissions
 * Manages which dimensions a player can access through special items/mechanics
 */
public interface IPlayerDimensionData {

    /**
     * Checks if a player has unlocked access to a specific dimension
     * @param dimensionId ResourceLocation-style ID (e.g., "minecraft:overworld")
     * @return True if dimension is available to the player
     */
    boolean isDimensionUnlocked(String dimensionId);

    /**
     * Grants access to a dimension if not already unlocked
     * @param dimensionId ResourceLocation-style ID to add
     */
    void unlockDimension(String dimensionId);

    /**
     * Gets all dimensions accessible to the player
     * @return Read-only set of dimension IDs - modifications must go through unlockDimension
     */
    Set<String> getUnlockedDimensions();
}