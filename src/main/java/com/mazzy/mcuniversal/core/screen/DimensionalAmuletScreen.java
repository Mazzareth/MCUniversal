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

/**
 * A GUI (screen) for the "Dimensional Amulet."
 */
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

    // A list of dimension IDs (which the player has unlocked) passed from the server
    private final List<String> unlockedDimensions;

    /**
     * Constructor that allows passing in the list of unlocked dimension IDs.
     *
     * @param unlockedDimensions The dimension IDs that this player has unlocked.
     */
    public DimensionalAmuletScreen(List<String> unlockedDimensions) {
        super(Component.empty());
        this.unlockedDimensions = (unlockedDimensions != null) ? unlockedDimensions : List.of();
    }

    /**
     * Overload constructor if nothing is passed in (for older calls or testing).
     */
    public DimensionalAmuletScreen() {
        super(Component.empty());
        this.unlockedDimensions = List.of();
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

    /**
     * Destroys and re-creates the screen widgets based on the current section.
     */
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

    // -----------------------------
    // Section 0: "My Nation" UI
    // -----------------------------
    private void initMyNationButtons() {
        // "Set Home" button
        Button setHomeButton = Button.builder(Component.literal("Set Home"), btn -> {
            // Send the "SET_HOME" action to the server
            NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.SET_HOME));
        }).pos(guiLeft + 15, guiTop + 40).size(70, 20).build();

        // Disable if not in "mcuniversal:extra"
        setHomeButton.active = isInExtraDimension();
        this.addRenderableWidget(setHomeButton);

        // "TP Home" button
        Button tpHomeButton = Button.builder(Component.literal("TP Home"), btn -> {
            // Send the "TELEPORT_HOME" action
            NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.TELEPORT_HOME));
        }).pos(guiLeft + 15, guiTop + 65).size(70, 20).build();
        this.addRenderableWidget(tpHomeButton);

        // A field for nation name
        this.nationNameField = new EditBox(
                this.font,
                this.guiLeft + 15,
                this.guiTop + 95,
                100,
                20,
                Component.literal("Nation Name")
        );
        nationNameField.setMaxLength(32);
        this.addRenderableWidget(nationNameField);

        // "Set Name" button
        Button setNameButton = Button.builder(Component.literal("Set Name"), btn -> {
            if (this.nationNameField != null) {
                String typedName = this.nationNameField.getValue().trim();
                if (!typedName.isEmpty()) {
                    NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.SET_NATION_NAME, typedName));
                }
            }
        }).pos(guiLeft + 120, guiTop + 95).size(70, 20).build();
        this.addRenderableWidget(setNameButton);
    }

    // -----------------------------
    // Section 1: "Teleports" UI
    // -----------------------------
    private void initTeleportButtons() {
        // Read dimension whitelist from config
        List<? extends String> dims = DimensionConfig.SERVER.dimensionWhitelist.get();

        int yOffset = 40;
        int buttonHeight = 20;
        int spacing = 5;

        // For each dimension in the config, create a random-TP button.
        // If you wanted to show only unlocked ones, you might filter them here.
        for (String dimId : dims) {
            // Example of enabling/disabling based on "unlockedDimensions":
            boolean isUnlocked = this.unlockedDimensions.contains(dimId);

            Button btn = Button.builder(Component.literal("Rand TP " + dimId), b -> {
                // Send a new packet specifying which dimension to random-TP into
                NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.RANDOM_TP_DIM, dimId));
            }).pos(guiLeft + 15, guiTop + yOffset).size(120, buttonHeight).build();

            // Optionally disable the button if locked:
            btn.active = isUnlocked;

            this.addRenderableWidget(btn);
            yOffset += (buttonHeight + spacing);
        }
    }

    /**
     * Helper to check if the client player is in the "mcuniversal:extra" dimension.
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
        this.renderBackground(guiGraphics);

        // Draw a background rectangle for the GUI
        guiGraphics.fill(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF333333);

        // A line separating the top tabs from the content
        int lineY = guiTop + 35;
        guiGraphics.fill(guiLeft, lineY, guiLeft + xSize, lineY + 1, 0xFFFFFFFF);

        // Section background
        switch (currentSection) {
            case 0 -> { // My Nation
                guiGraphics.fill(guiLeft + 1, lineY + 1, guiLeft + xSize - 1, guiTop + ySize - 1, 0xFF4B4B4B);
            }
            case 1 -> { // Teleports
                guiGraphics.fill(guiLeft + 1, lineY + 1, guiLeft + xSize - 1, guiTop + ySize - 1, 0xFF4B5F4B);
            }
        }

        // Render the widgets (buttons, edit boxes, etc.)
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