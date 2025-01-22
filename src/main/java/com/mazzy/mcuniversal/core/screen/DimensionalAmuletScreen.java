package com.mazzy.mcuniversal.core.screen;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * GUI screen for interacting with dimensional amulet functionality
 * Contains two sections: Nation management and dimension teleportation
 */
@OnlyIn(Dist.CLIENT)
public class DimensionalAmuletScreen extends Screen {
    // GUI dimensions and layout constants
    private final int xSize = 220;
    private final int ySize = 160;
    private int guiLeft;  // Calculated left screen position
    private int guiTop;   // Calculated top screen position
    private int currentSection = 0; // 0 = My Nation, 1 = Teleports

    // Special dimension identifier for nation features
    private static final ResourceLocation EXTRA_DIM_ID = new ResourceLocation("mcuniversal", "extra");
    private EditBox nationNameField; // Nation name input field

    // Teleport section state
    private final List<String> unlockedDimensions; // Player's accessible dimensions
    private String sectionTitle = ""; // Current section header
    private static final int MAX_DIM_PER_PAGE = 3; // Teleports per page
    private int dimensionPage = 0; // Current teleport list page

    public DimensionalAmuletScreen(List<String> unlockedDimensions) {
        super(Component.empty());
        // Ensure non-null list for safe iteration
        this.unlockedDimensions = unlockedDimensions != null ? unlockedDimensions : List.of();
    }

    public DimensionalAmuletScreen() {
        this(List.of());
    }

    @Override
    protected void init() {
        super.init();
        // Center the GUI on screen
        this.guiLeft = (this.width - xSize) / 2;
        this.guiTop = (this.height - ySize) / 2;
        rebuildSection(); // Build initial UI components
    }

    /** Reconstructs UI elements when changing sections/pages */
    private void rebuildSection() {
        this.clearWidgets();

        // Section navigation buttons
        this.addRenderableWidget(
                Button.builder(Component.literal("My Nation"), button -> {
                    currentSection = 0;
                    rebuildSection();
                }).pos(guiLeft + 10, guiTop + 10).size(60, 20).build()
        );
        this.addRenderableWidget(
                Button.builder(Component.literal("Teleports"), button -> {
                    currentSection = 1;
                    rebuildSection();
                }).pos(guiLeft + 80, guiTop + 10).size(60, 20).build()
        );

        // Load section-specific components
        switch (currentSection) {
            case 0 -> initMyNationButtons();
            case 1 -> initTeleportButtons();
        }
    }

    /** Initializes nation management section components */
    private void initMyNationButtons() {
        sectionTitle = "My Nation Settings";

        // Home position controls
        Button setHomeButton = Button.builder(
                Component.literal("Set Home"),
                btn -> NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.SET_HOME))
        ).pos(guiLeft + 15, guiTop + 40).size(70, 20).build();
        setHomeButton.active = isInExtraDimension(); // Only active in special dimension
        this.addRenderableWidget(setHomeButton);

        Button tpHomeButton = Button.builder(
                Component.literal("TP Home"),
                btn -> NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.TELEPORT_HOME))
        ).pos(guiLeft + 15, guiTop + 65).size(70, 20).build();
        this.addRenderableWidget(tpHomeButton);

        // Nation name input system
        this.nationNameField = new EditBox(
                this.font,
                this.guiLeft + 15,
                this.guiTop + 95,
                100,
                20,
                Component.literal("Nation Name")
        );
        nationNameField.setMaxLength(32); // Prevent excessively long names
        this.addRenderableWidget(nationNameField);

        // Name submission button
        Button setNameButton = Button.builder(Component.literal("Set Name"), btn -> {
            String typedName = nationNameField.getValue().trim();
            if (!typedName.isEmpty()) {
                NetworkHandler.sendToServer(
                        new DimensionalAmuletActionPacket(Action.SET_NATION_NAME, typedName)
                );
            }
        }).pos(guiLeft + 120, guiTop + 95).size(70, 20).build();
        this.addRenderableWidget(setNameButton);
    }

    /** Initializes dimension teleportation section components */
    private void initTeleportButtons() {
        sectionTitle = "Teleports";

        int yOffset = 40;
        final int buttonHeight = 20;
        final int spacing = 5;
        final int buttonAvailableWidth = xSize - 20;

        // Paginated dimension display
        int startIndex = dimensionPage * MAX_DIM_PER_PAGE;
        int endIndex = Math.min(startIndex + MAX_DIM_PER_PAGE, unlockedDimensions.size());

        // Create button for each dimension on current page
        for (int i = startIndex; i < endIndex; i++) {
            String dimId = unlockedDimensions.get(i);
            Button btn = Button.builder(
                            Component.literal("Rand TP " + dimId),
                            b -> NetworkHandler.sendToServer(
                                    new DimensionalAmuletActionPacket(Action.RANDOM_TP_DIM, dimId)
                            )
                    ).pos(guiLeft + 10, guiTop + yOffset)
                    .size(buttonAvailableWidth, buttonHeight)
                    .build();
            this.addRenderableWidget(btn);
            yOffset += (buttonHeight + spacing);
        }

        // Pagination controls
        final int paginationY = guiTop + (ySize - 25);
        final int buttonWidth = 50;
        final int labelWidth = 70;

        // Previous page button
        Button prevPageButton = Button.builder(Component.literal("Prev Page"), b -> {
            dimensionPage--;
            rebuildSection();
        }).pos(guiLeft + 10, paginationY).size(buttonWidth, 20).build();
        prevPageButton.active = (dimensionPage > 0);
        this.addRenderableWidget(prevPageButton);

        // Next page button
        int totalPages = (unlockedDimensions.size() + MAX_DIM_PER_PAGE - 1) / MAX_DIM_PER_PAGE;
        Button nextPageButton = Button.builder(Component.literal("Next Page"), b -> {
            dimensionPage++;
            rebuildSection();
        }).pos(guiLeft + xSize - 60, paginationY).size(buttonWidth, 20).build();
        nextPageButton.active = (dimensionPage < totalPages - 1);
        this.addRenderableWidget(nextPageButton);

        // Page indicator
        String pageLabelText = "Page " + (dimensionPage + 1) + "/" + totalPages;
        Button pageLabel = Button.builder(
                Component.literal(pageLabelText), b -> {}
        ).pos(guiLeft + (xSize / 2) - 35, paginationY).size(labelWidth, 20).build();
        pageLabel.active = false; // Non-interactive label
        this.addRenderableWidget(pageLabel);
    }

    /** Checks if player is in the special nation dimension */
    private boolean isInExtraDimension() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        Level clientLevel = mc.player.level();
        return clientLevel.dimension().location().equals(EXTRA_DIM_ID);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        // Main panel background
        final int topColor = 0xFF3A3A5C;
        final int bottomColor = 0xFF242436;
        guiGraphics.fillGradient(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, topColor, bottomColor);

        // Border elements
        final int borderColor = 0xFFFFFFFF;
        guiGraphics.fill(guiLeft, guiTop, guiLeft + xSize, guiTop + 1, borderColor); // Top
        guiGraphics.fill(guiLeft, guiTop, guiLeft + 1, guiTop + ySize, borderColor); // Left
        guiGraphics.fill(guiLeft + xSize - 1, guiTop, guiLeft + xSize, guiTop + ySize, borderColor); // Right
        guiGraphics.fill(guiLeft, guiTop + ySize - 1, guiLeft + xSize, guiTop + ySize, borderColor); // Bottom

        // Section divider line
        int lineY = guiTop + 35;
        guiGraphics.fill(guiLeft + 1, lineY, guiLeft + xSize - 1, lineY + 1, 0xFFFFFFFF);

        // Section-specific background tint
        switch (currentSection) {
            case 0 -> guiGraphics.fillGradient( // Nation section
                    guiLeft + 1, lineY + 1, guiLeft + xSize - 1, guiTop + ySize - 1,
                    0x554B4B4B, 0x334B4B4B // Gray tint
            );
            case 1 -> guiGraphics.fillGradient( // Teleport section
                    guiLeft + 1, lineY + 1, guiLeft + xSize - 1, guiTop + ySize - 1,
                    0x554B5F4B, 0x334B5F4B // Green tint
            );
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // Draw section title below main panel
        if (!sectionTitle.isEmpty()) {
            int titleX = guiLeft + (xSize - font.width(sectionTitle)) / 2;
            int titleY = guiTop + ySize + 5;
            int textPadding = 4;
            // Text background
            guiGraphics.fill(titleX - textPadding, titleY - textPadding,
                    titleX + font.width(sectionTitle) + textPadding, titleY + font.lineHeight + textPadding,
                    0xAA000000);
            // Title text
            guiGraphics.drawString(this.font, sectionTitle, titleX, titleY, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause game when screen is open
    }

    // Input handling for nation name field
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (nationNameField != null && nationNameField.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nationNameField != null && nationNameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}