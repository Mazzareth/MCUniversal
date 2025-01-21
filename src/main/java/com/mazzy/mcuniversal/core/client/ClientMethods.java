package com.mazzy.mcuniversal.core.client;

import net.minecraft.client.Minecraft;
import com.mazzy.mcuniversal.core.screen.DimensionalAmuletScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT) // <-- Add this class-level annotation
public class ClientMethods {

    // Existing methods are good but add individual method annotations
    @OnlyIn(Dist.CLIENT)
    public static void openDimensionalAmuletScreen() {
        openDimensionalAmuletScreen(new ArrayList<>());
    }

    @OnlyIn(Dist.CLIENT)
    public static void openDimensionalAmuletScreen(List<String> unlockedDimensionIds) {
        if (Minecraft.getInstance().screen == null) { // Add null check for safety
            Minecraft.getInstance().setScreen(new DimensionalAmuletScreen(unlockedDimensionIds));
        }
    }
}