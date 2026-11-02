
package de.elytrafinder.client.worldgen;

import de.elytrafinder.client.storage.ElytraStatusStorage.Status;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class ElytraStatusChecker {

    private static final int SEARCH_PADDING = 4;

    private ElytraStatusChecker() {
    }

    public static Status inspect(
            ClientLevel level,
            EndCityFinder.Result result
    ) {

        if (level == null || result == null) {
            return Status.UNKNOWN;
        }

        if (!level.dimension().equals(Level.END)) {
            return Status.UNKNOWN;
        }

        int minX = result.shipMinX() - SEARCH_PADDING;
        int minY = result.shipMinY() - SEARCH_PADDING;
        int minZ = result.shipMinZ() - SEARCH_PADDING;

        int maxX = result.shipMaxX() + SEARCH_PADDING;
        int maxY = result.shipMaxY() + SEARCH_PADDING;
        int maxZ = result.shipMaxZ() + SEARCH_PADDING;

        // Betroffene Chunk-Spalten berechnen.
        int minChunkX = Math.floorDiv(minX, 16);
        int maxChunkX = Math.floorDiv(maxX, 16);

        int minChunkZ = Math.floorDiv(minZ, 16);
        int maxChunkZ = Math.floorDiv(maxZ, 16);

        // Alle benötigten Chunk-Spalten müssen geladen sein.
        for (int chunkX = minChunkX;
             chunkX <= maxChunkX;
             chunkX++) {

            for (int chunkZ = minChunkZ;
                 chunkZ <= maxChunkZ;
                 chunkZ++) {

                BlockPos checkPosition =
                        new BlockPos(
                                chunkX * 16 + 8,
                                result.shipMinY(),
                                chunkZ * 16 + 8
                        );

                if (!level.isLoaded(checkPosition)) {
                    return Status.UNKNOWN;
                }
            }
        }

        // Tatsächlichen Schiffsbereich untersuchen.
        AABB shipArea =
                new AABB(
                        minX,
                        minY,
                        minZ,
                        maxX + 1,
                        maxY + 1,
                        maxZ + 1
                );

        List<ItemFrame> frames =
                level.getEntitiesOfClass(
                        ItemFrame.class,
                        shipArea
                );

        // Hat ein Rahmen noch eine Elytra?
        for (ItemFrame frame : frames) {

            if (frame.getItem().is(Items.ELYTRA)) {
                return Status.ELYTRA_PRESENT;
            }
        }

        // Rahmen vorhanden, aber ohne Elytra.
        if (!frames.isEmpty()) {
            return Status.FRAME_WITHOUT_ELYTRA;
        }

        // Alle Chunks sind geladen, aber kein Rahmen existiert.
        return Status.FRAME_REMOVED;
    }
}
