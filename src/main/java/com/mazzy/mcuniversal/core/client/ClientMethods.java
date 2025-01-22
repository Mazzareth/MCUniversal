package com.mazzy.mcuniversal.core.client;

import net.minecraft.client.Minecraft;
import com.mazzy.mcuniversal.core.screen.DimensionalAmuletScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side GUI handling utilities
 * Contains methods for managing dimensional amulet interface
 */
@OnlyIn(Dist.CLIENT)
public class ClientMethods {

    /**
     * Opens dimensional amulet screen with empty unlocked dimensions list
     */
    @OnlyIn(Dist.CLIENT)
    public static void openDimensionalAmuletScreen() {
        openDimensionalAmuletScreen(new ArrayList<>());
    }

    /**
     * Opens dimensional amulet screen with specified unlocked dimensions
     * @param unlockedDimensionIds List of unlocked dimension IDs to display
     */
    @OnlyIn(Dist.CLIENT)
    public static void openDimensionalAmuletScreen(List<String> unlockedDimensionIds) {
        // Only open if no other screen is active to prevent overlap
        if (Minecraft.getInstance().screen == null) {
            Minecraft.getInstance().setScreen(new DimensionalAmuletScreen(unlockedDimensionIds));
        }
    }
}