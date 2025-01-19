package com.mazzy.mcuniversal.core.client;

import net.minecraft.client.Minecraft;
import com.mazzy.mcuniversal.core.screen.DimensionalAmuletScreen;

/**
 * Static client-side methods (e.g., opening GUI screens).
 */
public class ClientMethods {

    /**
     * Spawn and set the DimensionalAmuletScreen as the active screen.
     */
    public static void openDimensionalAmuletScreen() {
        Minecraft.getInstance().setScreen(new DimensionalAmuletScreen());
    }
}