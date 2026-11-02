
package de.elytrafinder.client.gui;

import de.elytrafinder.client.storage.SeedStorage;
import de.elytrafinder.client.storage.ElytraStatusStorage.Status;

import de.elytrafinder.client.worldgen.EndCityFinder;
import de.elytrafinder.client.worldgen.EndCitySearchService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ElytraFinderScreen extends Screen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 220;
    private static final int MAX_RESULTS = 5;
    private static final int MAX_SEARCH_RESULTS = 50;

    private final EndCitySearchService searchService;

    private final Button[] teleportButtons =
            new Button[MAX_RESULTS];

    private EditBox seedField;
    private EditBox seedNameField;

    private String statusMessage = "Seed eingeben.";

    private List<EndCityFinder.Result> results = List.of();

    private List<Status> elytraStatuses = List.of();

    private long activeSeed;

    private boolean searching = false;

    private boolean autoSearchStarted = false;

    public ElytraFinderScreen(
            EndCitySearchService searchService
    ) {
        super(Component.literal("ElytraFinder"));

        this.searchService = searchService;
    }

    @Override
    protected void init() {

        int panelWidth = Math.min(
                PANEL_WIDTH,
                this.width - 20
        );

        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        int innerX = panelX + 12;
        int innerWidth = panelWidth - 24;

        // Seed-Eingabefeld
        seedField = new EditBox(
                this.font,
                innerX,
                panelY + 53,
                innerWidth,
                16,
                Component.literal("Welt-Seed")
        );

        seedField.setMaxLength(20);
        seedField.setHint(
                Component.literal("Welt-Seed")
        );

        String lastSeed = SeedStorage.getLastSeed();

        if (!lastSeed.isBlank()) {
            seedField.setValue(lastSeed);
        }

        this.addRenderableWidget(seedField);

        // Name des gespeicherten Seeds
        seedNameField = new EditBox(
                this.font,
                innerX,
                panelY + 82,
                innerWidth,
                16,
                Component.literal("Seed-Name")
        );

        seedNameField.setMaxLength(32);
        seedNameField.setHint(
                Component.literal("Name der Welt")
        );

        this.addRenderableWidget(seedNameField);

        // Buttons
        int gap = 4;

        int searchWidth = 54;
        int saveWidth = 64;
        int manageWidth = 64;

        int overlayWidth =
                innerWidth -
                searchWidth -
                saveWidth -
                manageWidth -
                gap * 3;

        int buttonY = panelY + 103;

        // Suchen
        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Suchen"),
                        button -> startSearch()
                ).bounds(
                        innerX,
                        buttonY,
                        searchWidth,
                        18
                ).build()
        );

        // Seed speichern
        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Speichern"),
                        button -> saveCurrentSeed()
                ).bounds(
                        innerX + searchWidth + gap,
                        buttonY,
                        saveWidth,
                        18
                ).build()
        );

        // Meine Seeds
        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Seeds"),
                        button -> Minecraft.getInstance().gui.setScreen(
                                new SavedSeedsScreen(this)
                        )
                ).bounds(
                        innerX + searchWidth + gap + saveWidth + gap,
                        buttonY,
                        manageWidth,
                        18
                ).build()
        );

        // HUD-Overlay umschalten
        this.addRenderableWidget(
                Button.builder(
                        Component.literal(
                                ElytraFinderHud.isEnabled()
                                        ? "HUD: AN"
                                        : "HUD: AUS"
                        ),
                        button -> {

                            ElytraFinderHud.toggleEnabled();

                            button.setMessage(
                                    Component.literal(
                                            ElytraFinderHud.isEnabled()
                                                    ? "HUD: AN"
                                                    : "HUD: AUS"
                                    )
                            );

                            statusMessage =
                                    ElytraFinderHud.isEnabled()
                                            ? "HUD-Overlay aktiviert."
                                            : "HUD-Overlay deaktiviert.";
                        }
                ).bounds(
                        innerX +
                                searchWidth +
                                gap +
                                saveWidth +
                                gap +
                                manageWidth +
                                gap,
                        buttonY,
                        overlayWidth,
                        18
                ).build()
        );

        // Teleport-Buttons
        int teleportWidth = 54;

        for (int i = 0; i < MAX_RESULTS; i++) {

            final int resultIndex = i;

            int teleportX =
                    panelX + panelWidth - 12 - teleportWidth;

            int teleportY =
                    panelY + 152 + (i * 13);

            Button teleportButton = Button.builder(
                    Component.literal("TP"),
                    button -> teleportToResult(resultIndex)
            ).bounds(
                    teleportX,
                    teleportY,
                    teleportWidth,
                    13
            ).build();

            teleportButton.active = false;

            teleportButtons[i] = teleportButton;

            this.addRenderableWidget(teleportButton);
        }

        updateTeleportButtons();

        // Automatische Suche beim Öffnen mit J.
        if (!autoSearchStarted && !lastSeed.isBlank()) {

            autoSearchStarted = true;

            startSearch();
        }
    }

    /**
     * Letzten Seed beim Schließen speichern.
     */
    @Override
    public void removed() {

        super.removed();

        if (seedField == null) {
            return;
        }

        String value = seedField.getValue().trim();

        if (value.isEmpty()) {
            return;
        }

        try {

            Long.parseLong(value);

            SeedStorage.setLastSeed(value);

        } catch (NumberFormatException ignored) {

            // Ungültige Eingaben nicht speichern.

        } catch (IOException exception) {

            exception.printStackTrace();
        }
    }

    /**
     * Seed mit eigenem Namen speichern.
     */
    private void saveCurrentSeed() {

        String input = seedField.getValue().trim();
        String name = seedNameField.getValue().trim();

        if (input.isEmpty()) {
            statusMessage = "Seed eingeben!";
            return;
        }

        if (name.isEmpty()) {
            statusMessage = "Namen eingeben!";
            return;
        }

        try {

            long seed = Long.parseLong(input);

            SeedStorage.saveSeed(name, seed);

            statusMessage = "Seed gespeichert!";

            seedNameField.setValue("");

        } catch (NumberFormatException exception) {

            statusMessage = "Ungültiger Seed.";

        } catch (IOException exception) {

            statusMessage = "Speichern fehlgeschlagen.";

            exception.printStackTrace();
        }
    }

    /**
     * Gespeicherten Seed laden und direkt suchen.
     */
    public void loadSavedSeed(long seed) {

        String value = Long.toString(seed);

        try {

            SeedStorage.setLastSeed(value);

            if (seedField != null) {
                seedField.setValue(value);
            }

            results = List.of();
            elytraStatuses = List.of();

            statusMessage = "Seed geladen.";

            startSearch();

            updateTeleportButtons();

        } catch (IOException exception) {

            statusMessage = "Seed konnte nicht geladen werden.";

            exception.printStackTrace();
        }
    }

    /**
     * Suche anhand der aktuellen Spielerposition starten.
     */
    private void startSearch() {

        if (searching) {
            return;
        }

        String input = seedField.getValue().trim();

        if (input.isEmpty()) {

            statusMessage = "Seed eingeben!";

            results = List.of();
            elytraStatuses = List.of();

            updateTeleportButtons();

            return;
        }

        final long seed;

        try {

            seed = Long.parseLong(input);

        } catch (NumberFormatException exception) {

            statusMessage = "Ungültiger Seed.";

            results = List.of();
            elytraStatuses = List.of();

            updateTeleportButtons();

            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null ||
                minecraft.level == null) {

            statusMessage = "Keine Spielerposition.";

            return;
        }

        // Die Suche wird im End gestartet,
        // damit die X/Z-Koordinaten vergleichbar sind.
        if (!minecraft.level.dimension().equals(Level.END)) {

            statusMessage = "Bitte zuerst ins End wechseln.";

            return;
        }

        try {

            SeedStorage.setLastSeed(input);

        } catch (IOException exception) {

            exception.printStackTrace();
        }

        activeSeed = seed;

        int originX =
                minecraft.player.blockPosition().getX();

        int originZ =
                minecraft.player.blockPosition().getZ();

        searching = true;

        results = List.of();
        elytraStatuses = List.of();

        updateTeleportButtons();

        statusMessage = "Suche läuft ...";

        // Overlay über den Suchstart informieren.
        ElytraFinderHud.beginSearch(seed);

        searchService.search(
                seed,
                originX,
                originZ,
                MAX_SEARCH_RESULTS
        ).whenComplete((foundResults, error) -> {

            minecraft.execute(() -> {

                searching = false;

                if (error != null) {

                    Throwable cause = error;

                    while (cause.getCause() != null) {
                        cause = cause.getCause();
                    }

                    if (cause instanceof
                            EndCitySearchService.SearchAlreadyRunningException) {

                        statusMessage = "Suche läuft bereits.";

                    } else {

                        statusMessage = "Suchfehler! Siehe Log.";

                        cause.printStackTrace();
                    }

                    ElytraFinderHud.setSearchError(
                            statusMessage
                    );

                    updateTeleportButtons();

                    return;
                }

                // Alle gefundenen Kandidaten an das HUD übergeben.
                // Das HUD wählt daraus fortlaufend die fünf nächstgelegenen aus.
                ElytraFinderHud.setResults(
                        activeSeed,
                        foundResults
                );

                results = ElytraFinderHud.getResults();
                elytraStatuses = ElytraFinderHud.getStatuses();

                statusMessage = foundResults.isEmpty()
                        ? "Keine Endschiffe gefunden."
                        : foundResults.size() + " Endschiffe erfasst; die fünf nächsten werden angezeigt.";

                updateTeleportButtons();
            });
        });
    }

    /**
     * Status aus der globalen Überwachung übernehmen.
     */
    @Override
    public void tick() {

        super.tick();

        if (!searching) {
            // Reihenfolge und Live-Blockentfernungen werden vom HUD
            // jeden Tick anhand der aktuellen Spielerposition aktualisiert.
            results = ElytraFinderHud.getResults();
            elytraStatuses = ElytraFinderHud.getStatuses();
            updateTeleportButtons();
        }
    }

    /**
     * Teleport-Buttons nur im Singleplayer aktivieren.
     */
    private void updateTeleportButtons() {

        boolean singleplayer =
                Minecraft.getInstance().getSingleplayerServer() != null;

        for (int i = 0; i < teleportButtons.length; i++) {

            Button button = teleportButtons[i];

            if (button != null) {

                button.active =
                        singleplayer &&
                        !searching &&
                        i < results.size();
            }
        }
    }

    /**
     * Zu einem Endschiff teleportieren.
     */
    private void teleportToResult(int index) {

        if (index < 0 || index >= results.size()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        var server =
                minecraft.getSingleplayerServer();

        if (server == null || minecraft.player == null) {

            statusMessage = "Nur im Singleplayer möglich.";

            updateTeleportButtons();

            return;
        }

        EndCityFinder.Result result =
                results.get(index);

        UUID playerId =
                minecraft.player.getUUID();

        statusMessage = "Teleport läuft ...";

        server.execute(() -> {

            ServerPlayer serverPlayer =
                    server.getPlayerList().getPlayer(playerId);

            ServerLevel endLevel =
                    server.getLevel(Level.END);

            if (serverPlayer == null || endLevel == null) {

                minecraft.execute(() ->
                        statusMessage = "Teleport fehlgeschlagen."
                );

                return;
            }

            boolean successful =
                    serverPlayer.teleportTo(
                            endLevel,
                            result.x() + 0.5,
                            result.y() + 2.0,
                            result.z() + 0.5,
                            Set.<Relative>of(),
                            serverPlayer.getYRot(),
                            serverPlayer.getXRot(),
                            true
                    );

            minecraft.execute(() -> statusMessage =
                    successful
                            ? "Teleport erfolgreich!"
                            : "Teleport fehlgeschlagen."
            );
        });
    }

    /**
     * Statusbezeichnung der Ergebnisliste.
     */
    private String getStatusLabel(int index) {

        if (index >= elytraStatuses.size()) {
            return "?";
        }

        return switch (elytraStatuses.get(index)) {

            case ELYTRA_PRESENT -> "ELYTRA";

            case FRAME_WITHOUT_ELYTRA -> "LEER";

            case FRAME_REMOVED -> "RAHMEN WEG";

            case UNKNOWN -> "?";
        };
    }

    private int getStatusColor(int index) {

        if (index >= elytraStatuses.size()) {
            return 0xFF8899AA;
        }

        return switch (elytraStatuses.get(index)) {

            case ELYTRA_PRESENT -> 0xFF4DDBA0;

            case FRAME_WITHOUT_ELYTRA -> 0xFFFF8A72;

            case FRAME_REMOVED -> 0xFFFFC04D;

            case UNKNOWN -> 0xFF8899AA;
        };
    }

    /**
     * Kompakte Benutzeroberfläche.
     */
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

        int innerX = panelX + 12;

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

        // Akzentleiste
        graphics.fill(
                panelX,
                panelY,
                panelX + panelWidth,
                panelY + 2,
                0xFF00C9B7
        );

        // Titel
        drawCentered(
                graphics,
                "ELYTRAFINDER",
                panelY + 7,
                0xFFFFFFFF
        );

        // Untertitel
        drawCentered(
                graphics,
                "End City Search",
                panelY + 20,
                0xFF00C9B7
        );

        // Beschreibung
        graphics.text(
                this.font,
                "Endschiffe anhand deines Seeds finden.",
                innerX,
                panelY + 33,
                0xFFB7C5D6,
                true
        );

        // Seed-Beschriftung
        graphics.text(
                this.font,
                "WELT-SEED",
                innerX,
                panelY + 44,
                0xFFFFFFFF,
                true
        );

        // Name-Beschriftung
        graphics.text(
                this.font,
                "SEED-NAME",
                innerX,
                panelY + 73,
                0xFFFFFFFF,
                true
        );

        // Statusmeldung
        graphics.text(
                this.font,
                statusMessage,
                innerX,
                panelY + 125,
                0xFF00C9B7,
                true
        );

        // Endschiff-Ergebnisse
        if (!results.isEmpty()) {

            graphics.text(
                    this.font,
                    "ENDSCHIFF / STATUS",
                    innerX,
                    panelY + 140,
                    0xFFFFFFFF,
                    true
            );

            for (int i = 0; i < results.size(); i++) {

                EndCityFinder.Result result =
                        results.get(i);

                String line = String.format(
                        "%d X:%d Z:%d %dm [%s]",
                        i + 1,
                        result.x(),
                        result.z(),
                        result.distanceBlocks(),
                        getStatusLabel(i)
                );

                graphics.text(
                        this.font,
                        line,
                        innerX,
                        panelY + 153 + (i * 13),
                        getStatusColor(i),
                        true
                );
            }
        }
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
