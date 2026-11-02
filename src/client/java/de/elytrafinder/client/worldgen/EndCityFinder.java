
package de.elytrafinder.client.worldgen;

/*
 * Portions adapted from Structure Finder (Minecraft 26.2).
 * Copyright (c) 2026 StructureFinder contributors.
 * MIT License - see LICENSE-StructureFinder.txt.
 */

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class EndCityFinder {

    private static final int MAX_RADIUS_CHUNKS = 1024;

    private static final Identifier END_CITY_ID =
            Identifier.withDefaultNamespace("end_city");

    private static final Identifier END_SHIP_ID =
            Identifier.withDefaultNamespace("end_city/ship");

    private final VanillaWorldgenContext context;

    public EndCityFinder(VanillaWorldgenContext context) {
        this.context = context;
    }

    public List<Result> findNearestWithShip(
            long seed,
            int originX,
            int originZ,
            int maxResults
    ) {

        if (maxResults < 1 || maxResults > 50) {
            throw new IllegalArgumentException(
                    "Die Anzahl der Ergebnisse muss zwischen 1 und 50 liegen."
            );
        }

        int originChunkX = Math.floorDiv(originX, 16);
        int originChunkZ = Math.floorDiv(originZ, 16);

        Registry<Structure> structures =
                context.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        Holder.Reference<Structure> endCity =
                structures.get(END_CITY_ID)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Endstadt-Struktur nicht gefunden."
                                )
                        );

        LevelStem endStem =
                context.dimensions().dimensions().get(LevelStem.END);

        if (endStem == null ||
                !(endStem.generator() instanceof NoiseBasedChunkGenerator generator)) {

            throw new IllegalStateException(
                    "Der End-Chunkgenerator konnte nicht geladen werden."
            );
        }

        ResourceKey<NoiseGeneratorSettings> settingsKey =
                generator.generatorSettings()
                        .unwrapKey()
                        .orElseThrow();

        RandomState randomState =
                RandomState.create(
                        context.registryAccess(),
                        settingsKey,
                        seed
                );

        ChunkGeneratorStructureState structureState =
                generator.createState(
                        context.registryAccess().lookupOrThrow(
                                Registries.STRUCTURE_SET
                        ),
                        randomState,
                        seed
                );

        List<StructurePlacement> placements =
                structureState.getPlacementsForStructure(endCity);

        if (placements.isEmpty()) {
            return List.of();
        }

        List<Candidate> candidates =
                collectCandidates(
                        seed,
                        structureState,
                        placements,
                        originChunkX,
                        originChunkZ
                );

        candidates.sort(
                Comparator.comparingLong(
                        candidate ->
                                candidate.distanceSquared(originX, originZ)
                )
        );

        LevelHeightAccessor height =
                LevelHeightAccessor.create(
                        endStem.type().value().minY(),
                        endStem.type().value().height()
                );

        var shipTemplate =
                context.templateManager().getOrCreate(END_SHIP_ID);

        List<Result> results = new ArrayList<>(maxResults);

        for (Candidate candidate : candidates) {

            Optional<Structure.GenerationStub> generation =
                    findGeneration(
                            endCity,
                            generator,
                            randomState,
                            height,
                            seed,
                            candidate.chunk()
                    );

            if (generation.isEmpty()) {
                continue;
            }

            // Tatsächliches Endschiff innerhalb der Struktur finden.
            TemplateStructurePiece shipPiece =
                    generation.get()
                            .getPiecesBuilder()
                            .build()
                            .pieces()
                            .stream()
                            .filter(TemplateStructurePiece.class::isInstance)
                            .map(TemplateStructurePiece.class::cast)
                            .filter(piece ->
                                    piece.template() == shipTemplate
                            )
                            .findFirst()
                            .orElse(null);

            if (shipPiece == null) {
                continue;
            }

            // Bounding-Box des echten Schiffs speichern.
            var shipBox = shipPiece.getBoundingBox();

            BlockPos locatePosition =
                    candidate.placement().getLocatePos(candidate.chunk());

            int x = locatePosition.getX();
            int y = generation.get().position().getY();
            int z = locatePosition.getZ();

            long dx = (long) x - originX;
            long dz = (long) z - originZ;

            int distance = (int) Math.round(
                    Math.sqrt(dx * dx + dz * dz)
            );

            results.add(
                    new Result(
                            x,
                            y,
                            z,
                            distance,
                            shipBox.minX(),
                            shipBox.minY(),
                            shipBox.minZ(),
                            shipBox.maxX(),
                            shipBox.maxY(),
                            shipBox.maxZ()
                    )
            );

            if (results.size() >= maxResults) {
                break;
            }
        }

        return List.copyOf(results);
    }

    private List<Candidate> collectCandidates(
            long seed,
            ChunkGeneratorStructureState structureState,
            List<StructurePlacement> placements,
            int originChunkX,
            int originChunkZ
    ) {

        List<Candidate> candidates = new ArrayList<>();

        Set<CandidateKey> seen = new HashSet<>();

        for (StructurePlacement placement : placements) {

            if (!(placement instanceof RandomSpreadStructurePlacement randomSpread)) {
                continue;
            }

            int spacing = randomSpread.spacing();

            int regionRadius =
                    Math.floorDiv(MAX_RADIUS_CHUNKS, spacing) + 1;

            int centerRegionX =
                    Math.floorDiv(originChunkX, spacing);

            int centerRegionZ =
                    Math.floorDiv(originChunkZ, spacing);

            for (int regionX = centerRegionX - regionRadius;
                 regionX <= centerRegionX + regionRadius;
                 regionX++) {

                for (int regionZ = centerRegionZ - regionRadius;
                     regionZ <= centerRegionZ + regionRadius;
                     regionZ++) {

                    ChunkPos chunk =
                            randomSpread.getPotentialStructureChunk(
                                    seed,
                                    regionX * spacing,
                                    regionZ * spacing
                            );

                    addCandidate(
                            structureState,
                            placement,
                            chunk,
                            candidates,
                            seen,
                            originChunkX,
                            originChunkZ
                    );
                }
            }
        }

        return candidates;
    }

    private static void addCandidate(
            ChunkGeneratorStructureState structureState,
            StructurePlacement placement,
            ChunkPos chunk,
            List<Candidate> candidates,
            Set<CandidateKey> seen,
            int originChunkX,
            int originChunkZ
    ) {

        if (Math.abs((long) chunk.x() - originChunkX) >
                MAX_RADIUS_CHUNKS ||
                Math.abs((long) chunk.z() - originChunkZ) >
                        MAX_RADIUS_CHUNKS) {

            return;
        }

        if (!placement.isStructureChunk(
                structureState,
                chunk.x(),
                chunk.z()
        )) {
            return;
        }

        CandidateKey key =
                new CandidateKey(
                        placement,
                        chunk.x(),
                        chunk.z()
                );

        if (seen.add(key)) {
            candidates.add(new Candidate(placement, chunk));
        }
    }

    private Optional<Structure.GenerationStub> findGeneration(
            Holder.Reference<Structure> structure,
            ChunkGenerator generator,
            RandomState randomState,
            LevelHeightAccessor height,
            long seed,
            ChunkPos chunk
    ) {

        Structure.GenerationContext generationContext =
                new Structure.GenerationContext(
                        context.registryAccess(),
                        generator,
                        generator.getBiomeSource(),
                        randomState,
                        context.templateManager(),
                        seed,
                        chunk,
                        height,
                        structure.value().biomes()::contains
                );

        return structure.value().findValidGenerationPoint(
                generationContext
        );
    }

    private record Candidate(
            StructurePlacement placement,
            ChunkPos chunk
    ) {

        private long distanceSquared(
                int originX,
                int originZ
        ) {

            long x = placement.getLocatePos(chunk).getX();
            long z = placement.getLocatePos(chunk).getZ();

            long dx = x - originX;
            long dz = z - originZ;

            return dx * dx + dz * dz;
        }
    }

    private record CandidateKey(
            StructurePlacement placement,
            int chunkX,
            int chunkZ
    ) {
    }

    public record Result(
            int x,
            int y,
            int z,
            int distanceBlocks,
            int shipMinX,
            int shipMinY,
            int shipMinZ,
            int shipMaxX,
            int shipMaxY,
            int shipMaxZ
    ) {
    }
}
