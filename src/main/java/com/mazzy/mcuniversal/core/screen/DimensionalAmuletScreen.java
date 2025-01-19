package com.mazzy.mcuniversal.core.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.mazzy.mcuniversal.network.DimensionalAmuletActionPacket;
import com.mazzy.mcuniversal.network.DimensionalAmuletActionPacket.Action;
import com.mazzy.mcuniversal.network.NetworkHandler;

public class DimensionalAmuletScreen extends Screen {

    // GUI dimensions
    private final int xSize = 220;
    private final int ySize = 160;

    // Calculated positioning
    private int guiLeft;
    private int guiTop;

    // Track the current section
    private int currentSection = 0; // 0 = My Nation, 1 = Teleports, 2 = Public Waypoints

    public DimensionalAmuletScreen() {
        // Pass an empty component to avoid any inherited titles
        super(Component.empty());
    }

    @Override
    protected void init() {
        super.init();

        // Center the GUI
        this.guiLeft = (this.width - xSize) / 2;
        this.guiTop = (this.height - ySize) / 2;

        // Build our default section
        rebuildSection();
    }

    private void rebuildSection() {
        // Clear all existing widgets first
        this.clearWidgets();

        // Re-add the tab buttons
        this.addRenderableWidget(
                Button.builder(Component.literal("My Nation"), btn -> {
                    currentSection = 0;
                    rebuildSection();
                }).pos(guiLeft + 10, guiTop + 10).size(60, 20).build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Teleports"), btn -> {
                    currentSection = 1;
                    rebuildSection();
                }).pos(guiLeft + 80, guiTop + 10).size(60, 20).build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Waypoints"), btn -> {
                    currentSection = 2;
                    rebuildSection();
                }).pos(guiLeft + 150, guiTop + 10).size(60, 20).build()
        );

        // Add widgets for the chosen section
        switch (currentSection) {
            case 0 -> initMyNationButtons();
            case 1 -> initTeleportButtons();
            case 2 -> initWaypointButtons();
        }
    }

    // ---------- My Nation Section ----------
    private void initMyNationButtons() {
        this.addRenderableWidget(
                Button.builder(Component.literal("Set Home"), btn -> {
                    // Send the "SET_HOME" action to the server
                    NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.SET_HOME));
                }).pos(guiLeft + 15, guiTop + 40).size(70, 20).build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("TP Home"), btn -> {
                    // Send the "TELEPORT_HOME" action
                    NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.TELEPORT_HOME));
                }).pos(guiLeft + 15, guiTop + 65).size(70, 20).build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Set Name"), btn -> {
                    // Example: pass new name as a string to the server
                    NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.SET_NATION_NAME, "MyNation"));
                }).pos(guiLeft + 15, guiTop + 90).size(70, 20).build()
        );
    }

    // ---------- Teleports Section ----------
    private void initTeleportButtons() {
        this.addRenderableWidget(
                Button.builder(Component.literal("Rand Warp"), btn -> {
                    NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.RAND_WARP));
                }).pos(guiLeft + 15, guiTop + 40).size(80, 20).build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("TP Earth"), btn -> {
                    NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.TP_EARTH));
                }).pos(guiLeft + 15, guiTop + 65).size(70, 20).build()
        );
    }

    // ---------- Public Waypoints Section ----------
    private void initWaypointButtons() {
        // In your real usage, these will invoke "GlobalWaypointsPacket"
        // For example:
        //   NetworkHandler.sendToServer(new GlobalWaypointsPacket(
        //       GlobalWaypointsPacket.WaypointAction.CREATE, "MyPublicWP"));
        // This is just a placeholder for demonstration.
        this.addRenderableWidget(
                Button.builder(Component.literal("Create WP"), btn -> {
                    // Example usage with new GlobalWaypointsPacket
                    // NetworkHandler.sendToServer(
                    //     new GlobalWaypointsPacket(WaypointAction.CREATE, "PublicWP")
                    // );
                }).pos(guiLeft + 15, guiTop + 40).size(80, 20).build()
        );

        // Additional logic would iterate through existing waypoints, etc.
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics); // Dark background behind our GUI

        // Draw a slightly larger border for the entire GUI
        guiGraphics.fill(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF333333);

        // Separate the top tab area from the main content
        int lineY = guiTop + 35;
        guiGraphics.fill(guiLeft, lineY, guiLeft + xSize, lineY + 1, 0xFFFFFFFF);

        // Draw a background color for each section
        switch (currentSection) {
            case 0 -> {
                // My Nation: Lighter gray
                guiGraphics.fill(guiLeft + 1, lineY + 1, guiLeft + xSize - 1, guiTop + ySize - 1, 0xFF4B4B4B);
            }
            case 1 -> {
                // Teleports: Slightly greenish tint
                guiGraphics.fill(guiLeft + 1, lineY + 1, guiLeft + xSize - 1, guiTop + ySize - 1, 0xFF4B5F4B);
            }
            case 2 -> {
                // Waypoints: Slightly bluish tint
                guiGraphics.fill(guiLeft + 1, lineY + 1, guiLeft + xSize - 1, guiTop + ySize - 1, 0xFF3E4B5F);
            }
        }

        // Render the buttons (and other UI elements)
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}