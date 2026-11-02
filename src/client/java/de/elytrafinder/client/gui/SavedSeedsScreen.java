
package de.elytrafinder.client.gui;

import de.elytrafinder.client.storage.SeedStorage;
import de.elytrafinder.client.storage.SeedStorage.SavedSeed;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;

public class SavedSeedsScreen extends Screen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 220;
    private static final int PAGE_SIZE = 5;

    private final ElytraFinderScreen parent;

    private int page;

    private List<SavedSeed> savedSeeds = List.of();

    private String statusMessage = "";

    public SavedSeedsScreen(ElytraFinderScreen parent) {
        this(parent, 0, "");
    }

    public SavedSeedsScreen(
            ElytraFinderScreen parent,
            int page,
            String statusMessage
    ) {
        super(Component.literal("Gespeicherte Seeds"));

        this.parent = parent;
        this.page = page;
        this.statusMessage = statusMessage;
    }

    @Override
    protected void init() {

        savedSeeds = SeedStorage.getSavedSeeds();

        int totalPages = getTotalPages();

        page = Math.max(
                0,
                Math.min(page, totalPages - 1)
        );

        int panelWidth = Math.min(
                PANEL_WIDTH,
                this.width - 20
        );

        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        int firstIndex = page * PAGE_SIZE;

        int lastIndex = Math.min(
                firstIndex + PAGE_SIZE,
                savedSeeds.size()
        );

        // Buttons für die gespeicherten Seeds
        for (int i = firstIndex; i < lastIndex; i++) {

            SavedSeed saved = savedSeeds.get(i);

            int row = i - firstIndex;

            int rowY = panelY + 49 + row * 21;

            // Seed laden
            this.addRenderableWidget(
                    Button.builder(
                            Component.literal("Laden"),
                            button -> loadSeed(saved)
                    ).bounds(
                            panelX + 198,
                            rowY,
                            47,
                            15
                    ).build()
            );

            // Seed löschen
            this.addRenderableWidget(
                    Button.builder(
                            Component.literal("Löschen"),
                            button -> deleteSeed(saved)
                    ).bounds(
                            panelX + 248,
                            rowY,
                            60,
                            15
                    ).build()
            );
        }

        // Vorherige Seite
        Button previousButton = Button.builder(
                Component.literal("<"),
                button -> openPage(page - 1)
        ).bounds(
                panelX + 12,
                panelY + 165,
                75,
                17
        ).build();

        previousButton.active = page > 0;

        this.addRenderableWidget(previousButton);

        // Nächste Seite
        Button nextButton = Button.builder(
                Component.literal(">"),
                button -> openPage(page + 1)
        ).bounds(
                panelX + panelWidth - 87,
                panelY + 165,
                75,
                17
        ).build();

        nextButton.active = page < totalPages - 1;

        this.addRenderableWidget(nextButton);

        // Zurück zum ElytraFinder
        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Zurück zum Finder"),
                        button -> Minecraft.getInstance().gui.setScreen(parent)
                ).bounds(
                        (this.width - 140) / 2,
                        panelY + 190,
                        140,
                        17
                ).build()
        );
    }

    private int getTotalPages() {

        return Math.max(
                1,
                (savedSeeds.size() + PAGE_SIZE - 1) / PAGE_SIZE
        );
    }

    private void openPage(int newPage) {

        Minecraft.getInstance().gui.setScreen(
                new SavedSeedsScreen(
                        parent,
                        newPage,
                        statusMessage
                )
        );
    }

    private void loadSeed(SavedSeed saved) {

        parent.loadSavedSeed(saved.seed());

        Minecraft.getInstance().gui.setScreen(parent);
    }

    private void deleteSeed(SavedSeed saved) {

        try {

            SeedStorage.deleteSeed(saved.name());

            Minecraft.getInstance().gui.setScreen(
                    new SavedSeedsScreen(
                            parent,
                            page,
                            "Seed gelöscht."
                    )
            );

        } catch (IOException exception) {

            exception.printStackTrace();

            Minecraft.getInstance().gui.setScreen(
                    new SavedSeedsScreen(
                            parent,
                            page,
                            "Löschen fehlgeschlagen."
                    )
            );
        }
    }

    @Override
    public void onClose() {

        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {

        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );

        int panelWidth = Math.min(
                PANEL_WIDTH,
                this.width - 20
        );

        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // Hintergrund
        graphics.fill(
                panelX,
                panelY,
                panelX + panelWidth,
                panelY + PANEL_HEIGHT,
                0xE6111927
        );

        // Rahmen
        graphics.outline(
                panelX,
                panelY,
                panelWidth,
                PANEL_HEIGHT,
                0xFF34465B
        );

        // Türkise Akzentleiste
        graphics.fill(
                panelX,
                panelY,
                panelX + panelWidth,
                panelY + 2,
                0xFF00C9B7
        );

        // Überschrift
        drawCentered(
                graphics,
                "MEINE SEEDS",
                panelY + 9,
                0xFFFFFFFF
        );

        // Untertitel
        drawCentered(
                graphics,
                "Gespeicherte Welten",
                panelY + 24,
                0xFFB7C5D6
        );

        // Statusmeldung
        if (!statusMessage.isEmpty()) {

            drawCentered(
                    graphics,
                    statusMessage,
                    panelY + 38,
                    0xFF00C9B7
            );
        }

        // Wenn keine Seeds gespeichert sind
        if (savedSeeds.isEmpty()) {

            drawCentered(
                    graphics,
                    "Noch keine Seeds gespeichert.",
                    panelY + 105,
                    0xFFB7C5D6
            );

        } else {

            int firstIndex = page * PAGE_SIZE;

            int lastIndex = Math.min(
                    firstIndex + PAGE_SIZE,
                    savedSeeds.size()
            );

            // Gespeicherte Seeds anzeigen
            for (int i = firstIndex; i < lastIndex; i++) {

                SavedSeed saved = savedSeeds.get(i);

                int row = i - firstIndex;

                int rowY = panelY + 49 + row * 21;

                String text =
                        saved.name() + " | " + saved.seed();

                // Lange Namen kürzen
                while (
                        this.font.width(text) > 178 &&
                        text.length() > 5
                ) {
                    text = text.substring(
                            0,
                            text.length() - 4
                    ) + "...";
                }

                graphics.text(
                        this.font,
                        text,
                        panelX + 12,
                        rowY + 3,
                        0xFFB7C5D6,
                        true
                );
            }
        }

        // Seitenanzeige
        drawCentered(
                graphics,
                "Seite " + (page + 1) + " von " + getTotalPages(),
                panelY + 168,
                0xFF8899AA
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
