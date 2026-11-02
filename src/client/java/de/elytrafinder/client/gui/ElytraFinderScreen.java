
package de.elytrafinder.client.gui;

import de.elytrafinder.client.ElytraFinderLanguage;
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

    private String statusMessage = ElytraFinderLanguage.text("status.initial");

    private List<EndCityFinder.Result> results = List.of();

    private List<Status> elytraStatuses = List.of();

    private long activeSeed;

    private boolean searching = false;

    private boolean autoSearchStarted = false;

    private final String initialSeed;
    private final String initialSeedName;
    private final boolean languageReopen;

    public ElytraFinderScreen(
            EndCitySearchService searchService
    ) {
        this(searchService, null, null, false);
    }

    private ElytraFinderScreen(
            EndCitySearchService searchService,
            String initialSeed,
            String initialSeedName,
            boolean languageReopen
    ) {
        super(Component.literal("ElytraFinder"));

        this.searchService = searchService;
        this.initialSeed = initialSeed;
        this.initialSeedName = initialSeedName;
        this.languageReopen = languageReopen;

        if (languageReopen) {
            this.autoSearchStarted = true;
            this.results = ElytraFinderHud.getResults();
            this.elytraStatuses = ElytraFinderHud.getStatuses();
            this.statusMessage = ElytraFinderLanguage.text("status.language_changed");
        }
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
                Component.literal(ElytraFinderLanguage.text("label.world_seed"))
        );

        seedField.setMaxLength(20);
        seedField.setHint(
                Component.literal(ElytraFinderLanguage.text("label.world_seed"))
        );

        String lastSeed = SeedStorage.getLastSeed();

        if (languageReopen && initialSeed != null) {
            seedField.setValue(initialSeed);
        } else if (!lastSeed.isBlank()) {
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
                Component.literal(ElytraFinderLanguage.text("label.seed_name"))
        );

        if (languageReopen && initialSeedName != null) {
            seedNameField.setValue(initialSeedName);
        }

        seedNameField.setMaxLength(32);
        seedNameField.setHint(
                Component.literal(ElytraFinderLanguage.text("hint.world_name"))
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
                        Component.literal(ElytraFinderLanguage.text("button.search")),
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
                        Component.literal(ElytraFinderLanguage.text("button.save")),
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
                        Component.literal(ElytraFinderLanguage.text("button.seeds")),
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
                                        ? ElytraFinderLanguage.text("button.hud_on")
                                        : ElytraFinderLanguage.text("button.hud_off")
                        ),
                        button -> {

                            ElytraFinderHud.toggleEnabled();

                            button.setMessage(
                                    Component.literal(
                                            ElytraFinderHud.isEnabled()
                                                    ? ElytraFinderLanguage.text("button.hud_on")
                                                    : ElytraFinderLanguage.text("button.hud_off")
                                    )
                            );

                            statusMessage =
                                    ElytraFinderHud.isEnabled()
                                            ? ElytraFinderLanguage.text("status.hud_enabled")
                                            : ElytraFinderLanguage.text("status.hud_disabled");
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

        // Language selector: the button shows the language to switch to.
        this.addRenderableWidget(
                Button.builder(
                        Component.literal(ElytraFinderLanguage.switchLabel()),
                        button -> {
                            String currentSeed = seedField.getValue();
                            String currentSeedName = seedNameField.getValue();
                            ElytraFinderLanguage.toggle();
                            ElytraFinderHud.onLanguageChanged();
                            Minecraft.getInstance().gui.setScreen(
                                    new ElytraFinderScreen(
                                            searchService,
                                            currentSeed,
                                            currentSeedName,
                                            true
                                    )
                            );
                        }
                ).bounds(
                        panelX + panelWidth - 48,
                        panelY + 4,
                        36,
                        15
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
            statusMessage = ElytraFinderLanguage.text("status.seed_required");
            return;
        }

        if (name.isEmpty()) {
            statusMessage = ElytraFinderLanguage.text("status.name_required");
            return;
        }

        try {

            long seed = Long.parseLong(input);

            SeedStorage.saveSeed(name, seed);

            statusMessage = ElytraFinderLanguage.text("status.seed_saved");

            seedNameField.setValue("");

        } catch (NumberFormatException exception) {

            statusMessage = ElytraFinderLanguage.text("status.invalid_seed");

        } catch (IOException exception) {

            statusMessage = ElytraFinderLanguage.text("status.save_failed");

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

            statusMessage = ElytraFinderLanguage.text("status.seed_loaded");

            startSearch();

            updateTeleportButtons();

        } catch (IOException exception) {

            statusMessage = ElytraFinderLanguage.text("status.seed_load_failed");

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

            statusMessage = ElytraFinderLanguage.text("status.seed_required");

            results = List.of();
            elytraStatuses = List.of();

            updateTeleportButtons();

            return;
        }

        final long seed;

        try {

            seed = Long.parseLong(input);

        } catch (NumberFormatException exception) {

            statusMessage = ElytraFinderLanguage.text("status.invalid_seed");

            results = List.of();
            elytraStatuses = List.of();

            updateTeleportButtons();

            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null ||
                minecraft.level == null) {

            statusMessage = ElytraFinderLanguage.text("status.no_player");

            return;
        }

        // Die Suche wird im End gestartet,
        // damit die X/Z-Koordinaten vergleichbar sind.
        if (!minecraft.level.dimension().equals(Level.END)) {

            statusMessage = ElytraFinderLanguage.text("status.enter_end");

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

        statusMessage = ElytraFinderLanguage.text("status.searching");

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

                        statusMessage = ElytraFinderLanguage.text("status.search_already");

                    } else {

                        statusMessage = ElytraFinderLanguage.text("status.search_error");

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
                        ? ElytraFinderLanguage.text("status.no_results")
                        : ElytraFinderLanguage.text("status.results", foundResults.size());

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

            statusMessage = ElytraFinderLanguage.text("status.singleplayer_only");

            updateTeleportButtons();

            return;
        }

        EndCityFinder.Result result =
                results.get(index);

        UUID playerId =
                minecraft.player.getUUID();

        statusMessage = ElytraFinderLanguage.text("status.teleporting");

        server.execute(() -> {

            ServerPlayer serverPlayer =
                    server.getPlayerList().getPlayer(playerId);

            ServerLevel endLevel =
                    server.getLevel(Level.END);

            if (serverPlayer == null || endLevel == null) {

                minecraft.execute(() ->
                        statusMessage = ElytraFinderLanguage.text("status.teleport_failed")
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
                            ? ElytraFinderLanguage.text("status.teleport_success")
                            : ElytraFinderLanguage.text("status.teleport_failed")
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

            case FRAME_WITHOUT_ELYTRA -> ElytraFinderLanguage.text("hud.empty");

            case FRAME_REMOVED -> ElytraFinderLanguage.text("hud.frame_removed");

            case UNKNOWN -> "?";
        };
    }

    public ElytraFinderScreen recreateForLanguageChange() {
        String seed = seedField == null ? initialSeed : seedField.getValue();
        String seedName = seedNameField == null ? initialSeedName : seedNameField.getValue();
        return new ElytraFinderScreen(
                searchService,
                seed,
                seedName,
                true
        );
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
                ElytraFinderLanguage.text("screen.subtitle"),
                panelY + 20,
                0xFF00C9B7
        );

        // Beschreibung
        graphics.text(
                this.font,
                ElytraFinderLanguage.text("screen.description"),
                innerX,
                panelY + 33,
                0xFFB7C5D6,
                true
        );

        // Seed-Beschriftung
        graphics.text(
                this.font,
                ElytraFinderLanguage.text("screen.world_seed"),
                innerX,
                panelY + 44,
                0xFFFFFFFF,
                true
        );

        // Name-Beschriftung
        graphics.text(
                this.font,
                ElytraFinderLanguage.text("screen.seed_name"),
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
                    ElytraFinderLanguage.text("screen.results"),
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
