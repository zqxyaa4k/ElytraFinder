
package de.elytrafinder.client.worldgen;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EndCitySearchService implements AutoCloseable {

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(task -> {

                Thread thread =
                        new Thread(task, "ElytraFinder Search");

                thread.setDaemon(true);

                return thread;
            });

    private final AtomicBoolean searching =
            new AtomicBoolean(false);

    private VanillaWorldgenContext context;

    public CompletableFuture<List<EndCityFinder.Result>> search(
            long seed,
            int originX,
            int originZ,
            int maxResults
    ) {

        if (!searching.compareAndSet(false, true)) {

            return CompletableFuture.failedFuture(
                    new SearchAlreadyRunningException()
            );
        }

        return CompletableFuture.supplyAsync(() -> {

            try {

                if (context == null) {
                    context = VanillaWorldgenContext.load();
                }

                EndCityFinder finder =
                        new EndCityFinder(context);

                return finder.findNearestWithShip(
                        seed,
                        originX,
                        originZ,
                        maxResults
                );

            } catch (Exception exception) {

                throw new CompletionException(exception);
            }

        }, executor).whenComplete((result, error) -> {
            searching.set(false);
        });
    }

    @Override
    public void close() {

        if (executor.isShutdown()) {
            return;
        }

        executor.execute(() -> {

            if (context != null) {

                try {
                    context.close();

                } catch (IOException exception) {

                    exception.printStackTrace();
                }
            }
        });

        executor.shutdown();
    }

    public static final class SearchAlreadyRunningException
            extends IllegalStateException {
    }
}
