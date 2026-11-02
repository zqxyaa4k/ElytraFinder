package de.elytrafinder.client.gui;

import de.elytrafinder.client.storage.ElytraStatusStorage;
import de.elytrafinder.client.storage.ElytraStatusStorage.Status;

import de.elytrafinder.client.worldgen.EndCityFinder;
import de.elytrafinder.client.worldgen.ElytraStatusChecker;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ElytraFinderHud {

    private static final Path SETTINGS_FILE =
            net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("elytrafinder")
                    .resolve("hud-overlay.txt");

    private static final int MAX_VISIBLE_RESULTS = 5;
    private static final float HUD_SCALE = 0.85f;

    private static final NumberFormat BLOCK_FORMAT =
            NumberFormat.getIntegerInstance(Locale.GERMANY);

    // Reihenfolge im Uhrzeigersinn, beginnend mit der Blickrichtung.
    private static final String[] DIRECTION_ARROWS = {
            "↑", "↗", "→", "↘", "↓", "↙", "←", "↖"
    };

    private static boolean enabled = loadEnabled();

    private static long activeSeed;

    private static boolean searching;

    private static String searchMessage = "Noch keine Suche.";

    /*
     * allResults contains the larger search result pool.
     * results contains only the five closest results to the player's
     * current position and is recalculated every client tick.
     */
    private static List<EndCityFinder.Result> allResults = List.of();
    private static List<EndCityFinder.Result> results = List.of();

    private static List<Status> statuses = List.of();

    private static final Map<String, Status> statusCache = new HashMap<>();

    private static final Map<String, Integer> missingFrameScans =
            new HashMap<>();

    private static int scanTicks;

    private ElytraFinderHud() {
    }

    /**
     * HUD-Overlay bei Fabric registrieren.
     */
    public static void register() {

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(
                        "elytrafinder",
                        "hud_overlay"
                ),
                (graphics, deltaTracker) -> render(graphics)
        );
    }

    /**
     * Prüfen, ob das Overlay aktiviert ist.
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Overlay-Einstellung ändern und speichern.
     */
    public static void setEnabled(boolean value) {

        enabled = value;

        saveEnabled();
    }

    public static void toggleEnabled() {
        setEnabled(!enabled);
    }

    /**
     * Neue Suche beginnt.
     */
    public static void beginSearch(long seed) {

        activeSeed = seed;
        searching = true;
        searchMessage = "Suche läuft ...";

        allResults = List.of();
        results = List.of();
        statuses = List.of();

        statusCache.clear();
        missingFrameScans.clear();

        scanTicks = 0;
    }

    /**
     * Suchergebnisse übernehmen.
     *
     * Die Suche liefert bis zu 50 Kandidaten. Daraus werden anhand der
     * aktuellen Spielerposition fortlaufend die fünf nächsten ausgewählt.
     */
    public static void setResults(
            long seed,
            List<EndCityFinder.Result> foundResults
    ) {

        activeSeed = seed;
        searching = false;

        allResults = List.copyOf(foundResults);

        statusCache.clear();
        missingFrameScans.clear();

        for (EndCityFinder.Result result : allResults) {
            statusCache.put(
                    getResultKey(result),
                    ElytraStatusStorage.get(
                            activeSeed,
                            result.x(),
                            result.z()
                    )
            );
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != null) {
            refreshVisibleResults(
                    minecraft.player.getX(),
                    minecraft.player.getZ()
            );
        } else {
            results = allResults.stream()
                    .limit(MAX_VISIBLE_RESULTS)
                    .toList();
            refreshVisibleStatuses();
        }

        searchMessage = allResults.isEmpty()
                ? "Keine Endschiffe gefunden."
                : allResults.size() + " Endschiffe gefunden.";
    }

    /**
     * Suchfehler übernehmen.
     */
    public static void setSearchError(String message) {

        searching = false;
        searchMessage = message;
    }

    /**
     * Aktuelle fünf Ergebnisse für das normale Menü bereitstellen.
     */
    public static List<EndCityFinder.Result> getResults() {
        return results;
    }

    /**
     * Statusliste passend zur aktuellen Ergebnisreihenfolge bereitstellen.
     */
    public static List<Status> getStatuses() {
        return statuses;
    }

    /**
     * Läuft global weiter, auch wenn das Hauptmenü geschlossen ist.
     *
     * Die Blockentfernungen und die Rangfolge werden jeden Client-Tick
     * aktualisiert. Die aufwändigere Elytra-Statusprüfung erfolgt weiterhin
     * einmal pro Sekunde.
     */
    public static void tick(Minecraft minecraft) {

        if (allResults.isEmpty() || searching) {
            return;
        }

        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (!minecraft.level.dimension().equals(Level.END)) {
            return;
        }

        refreshVisibleResults(
                minecraft.player.getX(),
                minecraft.player.getZ()
        );

        scanTicks++;

        // Status einmal pro Sekunde prüfen.
        if (scanTicks < 20) {
            return;
        }

        scanTicks = 0;

        for (EndCityFinder.Result result : results) {

            Status detectedStatus =
                    ElytraStatusChecker.inspect(
                            minecraft.level,
                            result
                    );

            String key = getResultKey(result);

            Status previousStatus =
                    statusCache.getOrDefault(
                            key,
                            ElytraStatusStorage.get(
                                    activeSeed,
                                    result.x(),
                                    result.z()
                            )
                    );

            // Fehlenden Rahmen erst nach drei Prüfungen bestätigen.
            if (detectedStatus == Status.FRAME_REMOVED) {

                int confirmations =
                        missingFrameScans.merge(
                                key,
                                1,
                                Integer::sum
                        );

                if (confirmations < 3) {
                    continue;
                }

            } else {
                missingFrameScans.remove(key);
            }

            if (detectedStatus != Status.UNKNOWN) {

                try {

                    ElytraStatusStorage.remember(
                            activeSeed,
                            result.x(),
                            result.z(),
                            detectedStatus
                    );

                    statusCache.put(key, detectedStatus);

                } catch (IOException exception) {

                    System.err.println(
                            "[ElytraFinder] HUD-Status konnte nicht gespeichert werden:"
                    );

                    exception.printStackTrace();
                }
            }
        }

        refreshVisibleStatuses();
    }

    /**
     * Berechnet Abstände von der aktuellen Spielerposition und sortiert
     * die gesamte bekannte Trefferliste neu. Das geschieht ohne erneute
     * Weltgenerierung und damit auch während des Laufens.
     */
    private static void refreshVisibleResults(
            double playerX,
            double playerZ
    ) {

        results = allResults.stream()
                .sorted(Comparator.comparingDouble(result ->
                        distanceSquared(result, playerX, playerZ)
                ))
                .limit(MAX_VISIBLE_RESULTS)
                .map(result -> withLiveDistance(
                        result,
                        playerX,
                        playerZ
                ))
                .toList();

        refreshVisibleStatuses();
    }

    private static double distanceSquared(
            EndCityFinder.Result result,
            double playerX,
            double playerZ
    ) {

        double dx = result.x() - playerX;
        double dz = result.z() - playerZ;

        return dx * dx + dz * dz;
    }

    private static EndCityFinder.Result withLiveDistance(
            EndCityFinder.Result result,
            double playerX,
            double playerZ
    ) {

        int liveDistance = (int) Math.round(
                Math.sqrt(distanceSquared(result, playerX, playerZ))
        );

        return new EndCityFinder.Result(
                result.x(),
                result.y(),
                result.z(),
                liveDistance,
                result.shipMinX(),
                result.shipMinY(),
                result.shipMinZ(),
                result.shipMaxX(),
                result.shipMaxY(),
                result.shipMaxZ()
        );
    }

    private static void refreshVisibleStatuses() {

        List<Status> currentStatuses = new ArrayList<>();

        for (EndCityFinder.Result result : results) {
            currentStatuses.add(
                    statusCache.getOrDefault(
                            getResultKey(result),
                            ElytraStatusStorage.get(
                                    activeSeed,
                                    result.x(),
                                    result.z()
                            )
                    )
            );
        }

        statuses = List.copyOf(currentStatuses);
    }

    private static String getResultKey(EndCityFinder.Result result) {
        return activeSeed + ":" + result.x() + ":" + result.z();
    }

    /**
     * HUD-Overlay zeichnen.
     */
    private static void render(
            GuiGraphicsExtractor graphics
    ) {

        if (!enabled) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null ||
                !minecraft.level.dimension().equals(Level.END)) {
            return;
        }

        // Im normalen Menü nicht zusätzlich anzeigen.
        if (minecraft.gui.screen() != null) {
            return;
        }

        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        lines.add("ELYTRAFINDER");
        colors.add(0xFF00C9B7);

        if (searching) {

            lines.add("Suche läuft ...");
            colors.add(0xFFFFFFFF);

        } else if (results.isEmpty()) {

            lines.add(searchMessage);
            colors.add(0xFFB7C5D6);

        } else {

            lines.add("NÄCHSTE " + results.size() + " ENDSCHIFFE");
            colors.add(0xFFB7C5D6);

            for (int i = 0; i < results.size(); i++) {

                EndCityFinder.Result result = results.get(i);

                Status status = i < statuses.size()
                        ? statuses.get(i)
                        : Status.UNKNOWN;

                String line =
                        String.format(
                                Locale.ROOT,
                                "%02d  %s Blöcke  %s",
                                i + 1,
                                BLOCK_FORMAT.format(result.distanceBlocks()),
                                getShortStatusText(status)
                        );

                // Ein einzelner Live-Pfeil zeigt zum nächstgelegenen Ziel.
                // Er wird relativ zur aktuellen Blickrichtung berechnet und
                // erscheint nur in der ersten (nächsten) Ergebniszeile.
                if (i == 0) {
                    line = getDirectionArrow(minecraft, result) + "  " + line;
                }

                lines.add(line);
                colors.add(getStatusColor(status));
            }
        }

        int panelWidth = 0;

        for (String line : lines) {
            panelWidth = Math.max(
                    panelWidth,
                    minecraft.font.width(line)
            );
        }

        panelWidth += 16;

        int panelHeight = lines.size() == 2
                ? 43
                : 34 + Math.max(1, results.size()) * 10;

        int screenWidth =
                minecraft.getWindow().getGuiScaledWidth();

        int scaledScreenWidth =
                (int) Math.ceil(screenWidth / HUD_SCALE);

        int panelX = scaledScreenWidth - panelWidth - 8;
        int panelY = 8;

        // Gesamtes HUD inklusive Schrift kompakter skalieren.
        graphics.pose().pushMatrix();
        graphics.pose().scale(HUD_SCALE, HUD_SCALE);

        graphics.fill(
                panelX,
                panelY,
                panelX + panelWidth,
                panelY + panelHeight,
                0xD9111927
        );

        graphics.outline(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                0xFF34465B
        );

        // Dezente Akzentleiste am oberen Rand.
        graphics.fill(
                panelX + 1,
                panelY + 1,
                panelX + panelWidth - 1,
                panelY + 2,
                0xFF00C9B7
        );

        for (int i = 0; i < lines.size(); i++) {

            int textY = i == 0
                    ? panelY + 5
                    : (i == 1
                            ? panelY + 18
                            : panelY + 29 + ((i - 2) * 10));

            graphics.text(
                    minecraft.font,
                    lines.get(i),
                    panelX + 8,
                    textY,
                    colors.get(i),
                    true
            );
        }

        graphics.pose().popMatrix();
    }

    /**
     * Gibt einen von acht Richtungspfeilen zum nächstgelegenen Endschiff zurück.
     *
     * Der Pfeil ist relativ zur Blickrichtung des Spielers:
     * ↑ vorwärts, ↗ rechts-vorwärts, → rechts, usw.
     * Die Berechnung erfolgt direkt beim Rendern, damit sich der Pfeil auch
     * beim bloßen Umschauen sofort aktualisiert.
     */
    private static String getDirectionArrow(
            Minecraft minecraft,
            EndCityFinder.Result target
    ) {
        if (minecraft.player == null) {
            return "↑";
        }

        double dx = target.x() - minecraft.player.getX();
        double dz = target.z() - minecraft.player.getZ();

        if (Math.abs(dx) < 0.001 && Math.abs(dz) < 0.001) {
            return "↑";
        }

        // Minecraft-Yaw: 0° = Süden (+Z), -90° = Osten (+X).
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double relativeYaw = targetYaw - minecraft.player.getYRot();

        // Auf [-180°, 180°) normalisieren.
        relativeYaw = ((relativeYaw + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;

        // Acht Sektoren zu je 45°. Positive Minecraft-Yaw-Abweichung
        // liegt rechts von der aktuellen Blickrichtung.
        int sector = (int) Math.floor((relativeYaw + 22.5) / 45.0);
        sector = Math.floorMod(sector, 8);

        return DIRECTION_ARROWS[sector];
    }

    private static String getShortStatusText(Status status) {

        return switch (status) {

            case ELYTRA_PRESENT -> "ELYTRA";
            case FRAME_WITHOUT_ELYTRA -> "LEER";
            case FRAME_REMOVED -> "WEG";
            case UNKNOWN -> "?";
        };
    }

    private static int getStatusColor(Status status) {

        return switch (status) {

            case ELYTRA_PRESENT -> 0xFF4DDBA0;
            case FRAME_WITHOUT_ELYTRA -> 0xFFFF8A72;
            case FRAME_REMOVED -> 0xFFFFC04D;
            case UNKNOWN -> 0xFFB0B0B0;
        };
    }

    private static boolean loadEnabled() {

        try {

            if (!Files.exists(SETTINGS_FILE)) {
                return false;
            }

            return Boolean.parseBoolean(
                    Files.readString(
                            SETTINGS_FILE,
                            StandardCharsets.UTF_8
                    ).trim()
            );

        } catch (IOException exception) {

            exception.printStackTrace();

            return false;
        }
    }

    private static void saveEnabled() {

        try {

            Files.createDirectories(
                    SETTINGS_FILE.getParent()
            );

            Files.writeString(
                    SETTINGS_FILE,
                    Boolean.toString(enabled),
                    StandardCharsets.UTF_8
            );

        } catch (IOException exception) {

            System.err.println(
                    "[ElytraFinder] HUD-Einstellung konnte nicht gespeichert werden:"
            );

            exception.printStackTrace();
        }
    }
}
