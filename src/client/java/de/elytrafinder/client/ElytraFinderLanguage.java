package de.elytrafinder.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public final class ElytraFinderLanguage {

    private static final Path LANGUAGE_FILE =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("elytrafinder")
                    .resolve("language.txt");

    private static final Map<String, String> DE = Map.ofEntries(
            Map.entry("title.saved", "Gespeicherte Seeds"),
            Map.entry("language.choose_title", "Sprache auswählen"),
            Map.entry("language.choose_subtitle", "Wähle deine Sprache für ElytraFinder."),
            Map.entry("language.german", "Deutsch"),
            Map.entry("language.english", "English"),
            Map.entry("button.search", "Suchen"),
            Map.entry("button.save", "Speichern"),
            Map.entry("button.seeds", "Seeds"),
            Map.entry("button.load", "Laden"),
            Map.entry("button.delete", "Löschen"),
            Map.entry("button.back", "Zurück zum Finder"),
            Map.entry("button.hud_on", "HUD: AN"),
            Map.entry("button.hud_off", "HUD: AUS"),
            Map.entry("status.initial", "Seed eingeben."),
            Map.entry("label.world_seed", "Welt-Seed"),
            Map.entry("label.seed_name", "Seed-Name"),
            Map.entry("hint.world_seed", "Welt-Seed"),
            Map.entry("hint.world_name", "Name der Welt"),
            Map.entry("status.seed_required", "Seed eingeben!"),
            Map.entry("status.name_required", "Namen eingeben!"),
            Map.entry("status.seed_saved", "Seed gespeichert!"),
            Map.entry("status.invalid_seed", "Ungültiger Seed."),
            Map.entry("status.save_failed", "Speichern fehlgeschlagen."),
            Map.entry("status.seed_loaded", "Seed geladen."),
            Map.entry("status.seed_load_failed", "Seed konnte nicht geladen werden."),
            Map.entry("status.no_player", "Keine Spielerposition."),
            Map.entry("status.enter_end", "Bitte zuerst ins End wechseln."),
            Map.entry("status.searching", "Suche läuft ..."),
            Map.entry("status.search_already", "Suche läuft bereits."),
            Map.entry("status.search_error", "Suchfehler! Siehe Log."),
            Map.entry("status.no_results", "Keine Endschiffe gefunden."),
            Map.entry("status.results", "%d Endschiffe erfasst; die fünf nächsten werden angezeigt."),
            Map.entry("status.singleplayer_only", "Nur im Singleplayer möglich."),
            Map.entry("status.teleporting", "Teleport läuft ..."),
            Map.entry("status.teleport_failed", "Teleport fehlgeschlagen."),
            Map.entry("status.teleport_success", "Teleport erfolgreich!"),
            Map.entry("status.hud_enabled", "HUD-Overlay aktiviert."),
            Map.entry("status.hud_disabled", "HUD-Overlay deaktiviert."),
            Map.entry("status.language_changed", "Sprache geändert: Deutsch."),
            Map.entry("hud.no_search", "Noch keine Suche."),
            Map.entry("hud.searching", "Suche läuft ..."),
            Map.entry("hud.no_results", "Keine Endschiffe gefunden."),
            Map.entry("hud.next", "NÄCHSTE %d ENDSCHIFFE"),
            Map.entry("hud.blocks", "Blöcke"),
            Map.entry("hud.empty", "LEER"),
            Map.entry("hud.frame_removed", "WEG"),
            Map.entry("screen.subtitle", "Endstadtsuche"),
            Map.entry("screen.description", "Endschiffe anhand deines Seeds finden."),
            Map.entry("screen.world_seed", "WELT-SEED"),
            Map.entry("screen.seed_name", "SEED-NAME"),
            Map.entry("screen.results", "ENDSCHIFF / STATUS"),
            Map.entry("screen.my_seeds", "MEINE SEEDS"),
            Map.entry("screen.saved_worlds", "Gespeicherte Welten"),
            Map.entry("screen.no_saved_seeds", "Noch keine Seeds gespeichert."),
            Map.entry("screen.page", "Seite %d von %d"),
            Map.entry("status.seed_deleted", "Seed gelöscht."),
            Map.entry("status.delete_failed", "Löschen fehlgeschlagen.")
    );

    private static final Map<String, String> EN = Map.ofEntries(
            Map.entry("title.saved", "Saved Seeds"),
            Map.entry("language.choose_title", "Choose your language"),
            Map.entry("language.choose_subtitle", "Select your language for ElytraFinder."),
            Map.entry("language.german", "Deutsch"),
            Map.entry("language.english", "English"),
            Map.entry("button.search", "Search"),
            Map.entry("button.save", "Save"),
            Map.entry("button.seeds", "Seeds"),
            Map.entry("button.load", "Load"),
            Map.entry("button.delete", "Delete"),
            Map.entry("button.back", "Back to Finder"),
            Map.entry("button.hud_on", "HUD: ON"),
            Map.entry("button.hud_off", "HUD: OFF"),
            Map.entry("status.initial", "Enter a seed."),
            Map.entry("label.world_seed", "World Seed"),
            Map.entry("label.seed_name", "Seed Name"),
            Map.entry("hint.world_seed", "World Seed"),
            Map.entry("hint.world_name", "World name"),
            Map.entry("status.seed_required", "Enter a seed!"),
            Map.entry("status.name_required", "Enter a name!"),
            Map.entry("status.seed_saved", "Seed saved!"),
            Map.entry("status.invalid_seed", "Invalid seed."),
            Map.entry("status.save_failed", "Could not save seed."),
            Map.entry("status.seed_loaded", "Seed loaded."),
            Map.entry("status.seed_load_failed", "Could not load seed."),
            Map.entry("status.no_player", "Player position unavailable."),
            Map.entry("status.enter_end", "Please enter the End first."),
            Map.entry("status.searching", "Searching ..."),
            Map.entry("status.search_already", "A search is already running."),
            Map.entry("status.search_error", "Search failed! Check the log."),
            Map.entry("status.no_results", "No End Ships found."),
            Map.entry("status.results", "%d End Ships found; showing the five closest."),
            Map.entry("status.singleplayer_only", "Available in singleplayer only."),
            Map.entry("status.teleporting", "Teleporting ..."),
            Map.entry("status.teleport_failed", "Teleport failed."),
            Map.entry("status.teleport_success", "Teleport successful!"),
            Map.entry("status.hud_enabled", "HUD overlay enabled."),
            Map.entry("status.hud_disabled", "HUD overlay disabled."),
            Map.entry("status.language_changed", "Language changed: English."),
            Map.entry("hud.no_search", "No search yet."),
            Map.entry("hud.searching", "Searching ..."),
            Map.entry("hud.no_results", "No End Ships found."),
            Map.entry("hud.next", "NEXT %d END SHIPS"),
            Map.entry("hud.blocks", "blocks"),
            Map.entry("hud.empty", "EMPTY"),
            Map.entry("hud.frame_removed", "GONE"),
            Map.entry("screen.subtitle", "End City Search"),
            Map.entry("screen.description", "Find End Ships using your world seed."),
            Map.entry("screen.world_seed", "WORLD SEED"),
            Map.entry("screen.seed_name", "SEED NAME"),
            Map.entry("screen.results", "END SHIP / STATUS"),
            Map.entry("screen.my_seeds", "MY SAVED SEEDS"),
            Map.entry("screen.saved_worlds", "Saved worlds"),
            Map.entry("screen.no_saved_seeds", "No seeds saved yet."),
            Map.entry("screen.page", "Page %d of %d"),
            Map.entry("status.seed_deleted", "Seed deleted."),
            Map.entry("status.delete_failed", "Could not delete seed.")
    );

    private static String language = loadLanguage();

    private ElytraFinderLanguage() {
    }

    public static boolean hasSavedLanguage() {
        try {
            if (!Files.exists(LANGUAGE_FILE)) {
                return false;
            }

            String saved = Files.readString(
                    LANGUAGE_FILE,
                    StandardCharsets.UTF_8
            ).trim().toLowerCase(Locale.ROOT);

            return "de".equals(saved) || "en".equals(saved);
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static void setLanguage(String selectedLanguage) {
        if (!"de".equals(selectedLanguage) && !"en".equals(selectedLanguage)) {
            throw new IllegalArgumentException("Language must be 'de' or 'en'.");
        }

        language = selectedLanguage;
        saveLanguage();
    }

    public static boolean isGerman() {
        return "de".equals(language);
    }

    /**
     * The label shows the language that will be selected when clicked.
     */
    public static String switchLabel() {
        return isGerman() ? "EN" : "DE";
    }

    public static void toggle() {
        language = isGerman() ? "en" : "de";
        saveLanguage();
    }

    public static String text(String key) {
        return text(key, new Object[0]);
    }

    public static String text(String key, Object... arguments) {
        String value = (isGerman() ? DE : EN).getOrDefault(key, key);
        if (arguments == null || arguments.length == 0) {
            return value;
        }
        return String.format(Locale.ROOT, value, arguments);
    }

    private static String loadLanguage() {
        try {
            if (Files.exists(LANGUAGE_FILE)) {
                String saved = Files.readString(
                        LANGUAGE_FILE,
                        StandardCharsets.UTF_8
                ).trim().toLowerCase(Locale.ROOT);

                if ("de".equals(saved) || "en".equals(saved)) {
                    return saved;
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return Locale.getDefault().getLanguage().equals("de")
                ? "de"
                : "en";
    }

    private static void saveLanguage() {
        try {
            Files.createDirectories(LANGUAGE_FILE.getParent());
            Files.writeString(
                    LANGUAGE_FILE,
                    language,
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            System.err.println("[ElytraFinder] Could not save language preference:");
            exception.printStackTrace();
        }
    }
}
