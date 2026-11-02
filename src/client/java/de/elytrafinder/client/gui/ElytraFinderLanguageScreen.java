package de.elytrafinder.client.gui;

import de.elytrafinder.client.ElytraFinderLanguage;
import de.elytrafinder.client.worldgen.EndCitySearchService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ElytraFinderLanguageScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 126;

    private final EndCitySearchService searchService;

    public ElytraFinderLanguageScreen(
            EndCitySearchService searchService
    ) {
        super(Component.literal("ElytraFinder - Language"));
        this.searchService = searchService;
    }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        int buttonWidth = 108;
        int gap = 10;
        int buttonsX = (this.width - (buttonWidth * 2 + gap)) / 2;

        this.addRenderableWidget(
                Button.builder(
                        Component.literal(ElytraFinderLanguage.text("language.german")),
                        button -> selectLanguage("de")
                ).bounds(
                        buttonsX,
                        panelY + 70,
                        buttonWidth,
                        22
                ).build()
        );

        this.addRenderableWidget(
                Button.builder(
                        Component.literal(ElytraFinderLanguage.text("language.english")),
                        button -> selectLanguage("en")
                ).bounds(
                        buttonsX + buttonWidth + gap,
                        panelY + 70,
                        buttonWidth,
                        22
                ).build()
        );
    }

    private void selectLanguage(String language) {
        ElytraFinderLanguage.setLanguage(language);
        Minecraft.getInstance().gui.setScreen(
                new ElytraFinderScreen(searchService)
        );
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        graphics.fill(
                panelX,
                panelY,
                panelX + PANEL_WIDTH,
                panelY + PANEL_HEIGHT,
                0xE6111927
        );

        graphics.outline(
                panelX,
                panelY,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                0xFF34465B
        );

        graphics.fill(
                panelX,
                panelY,
                panelX + PANEL_WIDTH,
                panelY + 2,
                0xFF00C9B7
        );

        drawCentered(
                graphics,
                "ELYTRAFINDER",
                panelY + 15,
                0xFFFFFFFF
        );

        drawCentered(
                graphics,
                ElytraFinderLanguage.text("language.choose_title"),
                panelY + 33,
                0xFF00C9B7
        );

        drawCentered(
                graphics,
                ElytraFinderLanguage.text("language.choose_subtitle"),
                panelY + 49,
                0xFFB7C5D6
        );
    }

    private void drawCentered(
            GuiGraphicsExtractor graphics,
            String text,
            int y,
            int color
    ) {
        graphics.text(
                this.font,
                text,
                (this.width - this.font.width(text)) / 2,
                y,
                color,
                true
        );
    }
}
