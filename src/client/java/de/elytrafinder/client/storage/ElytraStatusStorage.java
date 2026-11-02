
package de.elytrafinder.client.storage;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ElytraStatusStorage {

    private static final Path FILE =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("elytrafinder")
                    .resolve("elytra-status.txt");

    private static final Map<Key, Status> STATUSES =
            new HashMap<>();

    static {
        loadFromDisk();
    }

    private ElytraStatusStorage() {
    }

    public enum Status {

        UNKNOWN,

        ELYTRA_PRESENT,

        FRAME_WITHOUT_ELYTRA,

        FRAME_REMOVED
    }

    private record Key(
            long seed,
            int x,
            int z
    ) {
    }

    public static synchronized Status get(
            long seed,
            int x,
            int z
    ) {

        return STATUSES.getOrDefault(
                new Key(seed, x, z),
                Status.UNKNOWN
        );
    }

    public static synchronized void remember(
            long seed,
            int x,
            int z,
            Status status
    ) throws IOException {

        if (status == null || status == Status.UNKNOWN) {
            return;
        }

        Key key = new Key(seed, x, z);

        if (STATUSES.get(key) == status) {
            return;
        }

        STATUSES.put(key, status);

        saveToDisk();
    }

    private static synchronized void loadFromDisk() {

        if (!Files.exists(FILE)) {
            return;
        }

        try {

            List<String> lines =
                    Files.readAllLines(
                            FILE,
                            StandardCharsets.UTF_8
                    );

            for (String line : lines) {

                String[] parts =
                        line.split("\t", -1);

                if (parts.length != 4) {
                    continue;
                }

                try {

                    long seed =
                            Long.parseLong(parts[0]);

                    int x =
                            Integer.parseInt(parts[1]);

                    int z =
                            Integer.parseInt(parts[2]);

                    Status status =
                            Status.valueOf(parts[3]);

                    if (status != Status.UNKNOWN) {

                        STATUSES.put(
                                new Key(seed, x, z),
                                status
                        );
                    }

                } catch (IllegalArgumentException ignored) {

                    // Fehlerhafte Einträge überspringen.
                }
            }

        } catch (IOException exception) {

            System.err.println(
                    "[ElytraFinder] Elytra-Status konnten nicht geladen werden:"
            );

            exception.printStackTrace();
        }
    }

    private static synchronized void saveToDisk()
            throws IOException {

        Files.createDirectories(FILE.getParent());

        List<String> lines = new ArrayList<>();

        for (Map.Entry<Key, Status> entry :
                STATUSES.entrySet()) {

            Key key = entry.getKey();

            lines.add(
                    key.seed() + "\t" +
                    key.x() + "\t" +
                    key.z() + "\t" +
                    entry.getValue().name()
            );
        }

        Files.write(
                FILE,
                lines,
                StandardCharsets.UTF_8
        );
    }
}
