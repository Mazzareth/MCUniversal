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

@OnlyIn(Dist.CLIENT)
public class DimensionalAmuletScreen extends Screen {

    private final int xSize = 220;
    private final int ySize = 160;
    private int guiLeft;
    private int guiTop;
    private int currentSection = 0;

    private static final ResourceLocation EXTRA_DIM_ID = new ResourceLocation("mcuniversal", "extra");
    private EditBox nationNameField;

    private final List<String> unlockedDimensions;
    private String sectionTitle = "";

    // Pagination variables
    private static final int MAX_DIM_PER_PAGE = 3;
    private int dimensionPage = 0;

    public DimensionalAmuletScreen(List<String> unlockedDimensions) {
        super(Component.empty());
        this.unlockedDimensions = unlockedDimensions != null ? unlockedDimensions : List.of();
    }

    public DimensionalAmuletScreen() {
        this(List.of());
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - xSize) / 2;
        this.guiTop = (this.height - ySize) / 2;
        rebuildSection();
    }

    private void rebuildSection() {
        this.clearWidgets();

        // Tab buttons
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

        switch (currentSection) {
            case 0 -> initMyNationButtons();
            case 1 -> initTeleportButtons();
        }
    }

    // Section 0: "My Nation"
    private void initMyNationButtons() {
        sectionTitle = "My Nation Settings";

        Button setHomeButton = Button.builder(
                Component.literal("Set Home"),
                btn -> NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.SET_HOME))
        ).pos(guiLeft + 15, guiTop + 40).size(70, 20).build();
        setHomeButton.active = isInExtraDimension();
        this.addRenderableWidget(setHomeButton);

        Button tpHomeButton = Button.builder(
                Component.literal("TP Home"),
                btn -> NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.TELEPORT_HOME))
        ).pos(guiLeft + 15, guiTop + 65).size(70, 20).build();
        this.addRenderableWidget(tpHomeButton);

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

        Button setNameButton = Button.builder(Component.literal("Set Name"), btn -> {
            String typedName = nationNameField.getValue().trim();
            if (!typedName.isEmpty()) {
                NetworkHandler.sendToServer(new DimensionalAmuletActionPacket(Action.SET_NATION_NAME, typedName));
            }
        }).pos(guiLeft + 120, guiTop + 95).size(70, 20).build();
        this.addRenderableWidget(setNameButton);
    }

    // Section 1: "Teleports"
    private void initTeleportButtons() {
        sectionTitle = "Teleports";

        int yOffset = 40;
        int buttonHeight = 20;
        int spacing = 5;
        int buttonAvailableWidth = xSize - 20;

        int startIndex = dimensionPage * MAX_DIM_PER_PAGE;
        int endIndex = Math.min(startIndex + MAX_DIM_PER_PAGE, unlockedDimensions.size());

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
        int paginationY = guiTop + (ySize - 25);
        int buttonWidth = 50;
        int labelWidth = 70;

        Button prevPageButton = Button.builder(Component.literal("Prev Page"), b -> {
            dimensionPage--;
            rebuildSection();
        }).pos(guiLeft + 10, paginationY).size(buttonWidth, 20).build();
        prevPageButton.active = (dimensionPage > 0);
        this.addRenderableWidget(prevPageButton);

        int totalPages = (unlockedDimensions.size() + MAX_DIM_PER_PAGE - 1) / MAX_DIM_PER_PAGE;
        Button nextPageButton = Button.builder(Component.literal("Next Page"), b -> {
            dimensionPage++;
            rebuildSection();
        }).pos(guiLeft + xSize - 60, paginationY).size(buttonWidth, 20).build();
        nextPageButton.active = (dimensionPage < totalPages - 1);
        this.addRenderableWidget(nextPageButton);

        String pageLabelText = "Page " + (dimensionPage + 1) + "/" + totalPages;
        Button pageLabel = Button.builder(
                Component.literal(pageLabelText), b -> {}
        ).pos(guiLeft + (xSize / 2) - 35, paginationY).size(labelWidth, 20).build();
        pageLabel.active = false;
        this.addRenderableWidget(pageLabel);
    }

    private boolean isInExtraDimension() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        Level clientLevel = mc.player.level();
        return clientLevel.dimension().location().equals(EXTRA_DIM_ID);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        int topColor = 0xFF3A3A5C;
        int bottomColor = 0xFF242436;
        guiGraphics.fillGradient(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, topColor, bottomColor);

        final int borderColor = 0xFFFFFFFF;
        guiGraphics.fill(guiLeft, guiTop, guiLeft + xSize, guiTop + 1, borderColor);
        guiGraphics.fill(guiLeft, guiTop, guiLeft + 1, guiTop + ySize, borderColor);
        guiGraphics.fill(guiLeft + xSize - 1, guiTop, guiLeft + xSize, guiTop + ySize, borderColor);
        guiGraphics.fill(guiLeft, guiTop + ySize - 1, guiLeft + xSize, guiTop + ySize, borderColor);

        int lineY = guiTop + 35;
        guiGraphics.fill(guiLeft + 1, lineY, guiLeft + xSize - 1, lineY + 1, 0xFFFFFFFF);

        switch (currentSection) {
            case 0 -> guiGraphics.fillGradient(
                    guiLeft + 1, lineY + 1, guiLeft + xSize - 1, guiTop + ySize - 1,
                    0x554B4B4B, 0x334B4B4B
            );
            case 1 -> guiGraphics.fillGradient(
                    guiLeft + 1, lineY + 1, guiLeft + xSize - 1, guiTop + ySize - 1,
                    0x554B5F4B, 0x334B5F4B
            );
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (!sectionTitle.isEmpty()) {
            int titleX = guiLeft + (xSize - font.width(sectionTitle)) / 2;
            int titleY = guiTop + ySize + 5;
            int textPadding = 4;
            guiGraphics.fill(titleX - textPadding, titleY - textPadding,
                    titleX + font.width(sectionTitle) + textPadding, titleY + font.lineHeight + textPadding,
                    0xAA000000);
            guiGraphics.drawString(this.font, sectionTitle, titleX, titleY, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

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