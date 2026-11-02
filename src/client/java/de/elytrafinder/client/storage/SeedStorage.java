
package de.elytrafinder.client.storage;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SeedStorage {

    private static final Path FILE =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("elytrafinder")
                    .resolve("seeds.txt");

    private static final Map<String, Long> SAVED_SEEDS =
            new LinkedHashMap<>();

    private static String lastSeed = "";

    static {
        loadFromDisk();
    }

    private SeedStorage() {
    }

    public static synchronized String getLastSeed() {
        return lastSeed;
    }

    public static synchronized List<SavedSeed> getSavedSeeds() {

        List<SavedSeed> result = new ArrayList<>();

        SAVED_SEEDS.forEach((name, seed) ->
                result.add(new SavedSeed(name, seed))
        );

        return List.copyOf(result);
    }

    public static synchronized void setLastSeed(
            String seed
    ) throws IOException {

        lastSeed = seed;

        saveToDisk();
    }

    public static synchronized void saveSeed(
            String name,
            long seed
    ) throws IOException {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Der Seed benötigt einen Namen."
            );
        }

        String cleanName = name.trim();

        SAVED_SEEDS.put(cleanName, seed);

        // Gleichzeitig als letzten Seed speichern.
        lastSeed = Long.toString(seed);

        saveToDisk();
    }

    public static synchronized void deleteSeed(
            String name
    ) throws IOException {

        SAVED_SEEDS.remove(name);

        saveToDisk();
    }

    private static synchronized void loadFromDisk() {

        if (!Files.exists(FILE)) {
            return;
        }

        try {

            List<String> lines = Files.readAllLines(
                    FILE,
                    StandardCharsets.UTF_8
            );

            for (String line : lines) {

                String[] parts = line.split("\t", 3);

                try {

                    if (parts.length == 2 &&
                            parts[0].equals("last")) {

                        Long.parseLong(parts[1]);
                        lastSeed = parts[1];

                    } else if (parts.length == 3 &&
                            parts[0].equals("saved")) {

                        String name = new String(
                                Base64.getUrlDecoder().decode(parts[1]),
                                StandardCharsets.UTF_8
                        );

                        long seed = Long.parseLong(parts[2]);

                        if (!name.isBlank()) {
                            SAVED_SEEDS.put(name, seed);
                        }
                    }

                } catch (IllegalArgumentException ignored) {
                    // Ungültige einzelne Einträge überspringen.
                }
            }

        } catch (IOException exception) {

            System.err.println(
                    "[ElytraFinder] Seeds konnten nicht geladen werden:"
            );

            exception.printStackTrace();
        }
    }

    private static synchronized void saveToDisk()
            throws IOException {

        Files.createDirectories(FILE.getParent());

        List<String> lines = new ArrayList<>();

        lines.add("last\t" + lastSeed);

        for (Map.Entry<String, Long> entry :
                SAVED_SEEDS.entrySet()) {

            String encodedName = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(
                            entry.getKey().getBytes(StandardCharsets.UTF_8)
                    );

            lines.add(
                    "saved\t" +
                    encodedName +
                    "\t" +
                    entry.getValue()
            );
        }

        Files.write(
                FILE,
                lines,
                StandardCharsets.UTF_8
        );
    }

    public record SavedSeed(
            String name,
            long seed
    ) {
    }
}
