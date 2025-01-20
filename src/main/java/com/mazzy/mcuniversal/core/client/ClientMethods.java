package com.mazzy.mcuniversal.core.client;

import net.minecraft.client.Minecraft;
import com.mazzy.mcuniversal.core.screen.DimensionalAmuletScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * Static client-side methods (e.g., opening GUI screens).
 */
public class ClientMethods {

    /**
     * Opens the DimensionalAmuletScreen with no dimension list.
     * (Kept for backward compatibility or if no data is provided.)
     */
    public static void openDimensionalAmuletScreen() {
        openDimensionalAmuletScreen(new ArrayList<>());
    }

    /**
     * Opens the DimensionalAmuletScreen with a given list of unlocked dimensions.
     *
     * @param unlockedDimensionIds A list of dimension IDs the player has unlocked.
     */
    public static void openDimensionalAmuletScreen(List<String> unlockedDimensionIds) {
        Minecraft.getInstance().setScreen(new DimensionalAmuletScreen(unlockedDimensionIds));
    }
}