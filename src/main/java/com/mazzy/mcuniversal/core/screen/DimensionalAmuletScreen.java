// File: DimensionalAmuletScreen.java
package com.mazzy.mcuniversal.core.screen;

import com.mazzy.mcuniversal.config.DimensionConfig;
import com.mazzy.mcuniversal.network.DimensionalAmuletActionPacket;
import com.mazzy.mcuniversal.network.DimensionalAmuletActionPacket.Action;
import com.mazzy.mcuniversal.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;

public class DimensionalAmuletScreen extends Screen {

    // GUI dimensions
    private final int xSize = 220;
    private final int ySize = 160;

    // Calculated positioning
    private int guiLeft;
    private int guiTop;

    // Track the current section
    private int currentSection = 0; // 0 = My Nation, 1 = Teleports, ...

    // Matches the dimension ID for "mcuniversal:extra"
    private static final ResourceLocation EXTRA_DIM_ID = new ResourceLocation("mcuniversal", "extra");

    // A text box for the user to input the custom nation name
    private EditBox nationNameField;

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

        // Add widgets for the chosen section
        switch (currentSection) {
            case 0 -> initMyNationButtons();
            case 1 -> initTeleportButtons();
        }
    }

    private void initMyNationButtons() {
        // "Set Home" button
        Button setHomeButton = Button.builder(Component.literal("Set Home"), btn -> {
            // Send the "SET_HOME" action to the server
            NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.SET_HOME));
        }).pos(guiLeft + 15, guiTop + 40).size(70, 20).build();
        // Disable it if we’re not in the correct dimension
        setHomeButton.active = isInExtraDimension();
        this.addRenderableWidget(setHomeButton);

        // "TP Home" button
        Button tpHomeButton = Button.builder(Component.literal("TP Home"), btn -> {
            // Send the "TELEPORT_HOME" action
            NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.TELEPORT_HOME));
        }).pos(guiLeft + 15, guiTop + 65).size(70, 20).build();
        this.addRenderableWidget(tpHomeButton);

        // Create an EditBox for the nation name
        this.nationNameField = new EditBox(
                this.font,
                this.guiLeft + 15,
                this.guiTop + 95,
                100,
                20,
                Component.literal("Nation Name")
        );
        this.nationNameField.setMaxLength(32); // Limit to 32 characters
        this.addRenderableWidget(this.nationNameField);

        // "Set Name" button
        // This sends the text from the nationNameField to the server.
        Button setNameButton = Button.builder(Component.literal("Set Name"), btn -> {
            if (this.nationNameField != null) {
                String typedName = this.nationNameField.getValue().trim();
                // Only send if not empty
                if (!typedName.isEmpty()) {
                    NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.SET_NATION_NAME, typedName));
                }
            }
        }).pos(guiLeft + 120, guiTop + 95).size(70, 20).build();
        this.addRenderableWidget(setNameButton);
    }

    /**
     * In this method, we dynamically create a button for each whitelisted dimension.
     * On click, we send a new action "RANDOM_TP_DIM" with the dimension ID.
     */
    private void initTeleportButtons() {
        // Read dimension whitelist from config
        List<? extends String> dims = DimensionConfig.SERVER.dimensionWhitelist.get();

        int yOffset = 40;
        int buttonHeight = 20;
        int spacing = 5;

        for (String dimId : dims) {
            // Create a button for each dimension
            Button btn = Button.builder(Component.literal("Rand TP " + dimId), b -> {
                // Send a new packet specifying which dimension to random-TP into
                NetworkHandler.sendToServer(
                        new DimensionalAmuletActionPacket(Action.RANDOM_TP_DIM, dimId)
                );
            }).pos(guiLeft + 15, guiTop + yOffset).size(120, buttonHeight).build();

            this.addRenderableWidget(btn);
            yOffset += (buttonHeight + spacing);
        }

        // Optionally: keep an original "Rand Warp" if desired:
        /*
        this.addRenderableWidget(
                Button.builder(Component.literal("Rand Warp (Overworld)"), btn -> {
                    NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.RAND_WARP));
                }).pos(guiLeft + 150, guiTop + 40).size(100, 20).build()
        );
        */
    }

    /**
     * Check if the client player is in the "mcuniversal:extra" dimension.
     */
    private boolean isInExtraDimension() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        Level clientLevel = mc.player.level();
        return clientLevel.dimension().location().equals(EXTRA_DIM_ID);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics); // Dark background behind our GUI

        // Draw a slightly larger border for the entire GUI
        guiGraphics.fill(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF333333);

        // Separate the top tab area from the main content
        int lineY = guiTop + 35;
        guiGraphics.fill(guiLeft, lineY, guiLeft + xSize, lineY + 1, 0xFFFFFFFF);

        // Area background
        switch (currentSection) {
            case 0 -> {
                // My Nation
                guiGraphics.fill(guiLeft + 1, lineY + 1, guiLeft + xSize - 1, guiTop + ySize - 1, 0xFF4B4B4B);
            }
            case 1 -> {
                // Teleports
                guiGraphics.fill(guiLeft + 1, lineY + 1, guiLeft + xSize - 1, guiTop + ySize - 1, 0xFF4B5F4B);
            }
        }

        // Render the buttons and other widgets (including the EditBox)
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Ensure key typing in the EditBox is passed along
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.nationNameField != null && this.nationNameField.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nationNameField != null && this.nationNameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}