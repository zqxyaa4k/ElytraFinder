
package de.elytrafinder.client.worldgen;

/*
 * Portions adapted from Structure Finder (Minecraft 26.2).
 * Copyright (c) 2026 StructureFinder contributors.
 * MIT License - see LICENSE-StructureFinder.txt.
 */
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class VanillaWorldgenContext implements AutoCloseable {

    private final RegistryAccess.Frozen registryAccess;
    private final WorldDimensions dimensions;
    private final StructureTemplateManager templateManager;
    private final MultiPackResourceManager resourceManager;
    private final LevelStorageSource.LevelStorageAccess storageAccess;
    private final Path temporaryDirectory;

    private VanillaWorldgenContext(
            RegistryAccess.Frozen registryAccess,
            WorldDimensions dimensions,
            StructureTemplateManager templateManager,
            MultiPackResourceManager resourceManager,
            LevelStorageSource.LevelStorageAccess storageAccess,
            Path temporaryDirectory
    ) {
        this.registryAccess = registryAccess;
        this.dimensions = dimensions;
        this.templateManager = templateManager;
        this.resourceManager = resourceManager;
        this.storageAccess = storageAccess;
        this.temporaryDirectory = temporaryDirectory;
    }

    public static VanillaWorldgenContext load() throws IOException {

        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        MultiPackResourceManager resources = new MultiPackResourceManager(
                PackType.SERVER_DATA,
                List.of(ServerPacksSource.createVanillaPackSource())
        );

        try {

            LayeredRegistryAccess<RegistryLayer> layers =
                    RegistryLayer.createRegistryAccess();

            var staticTags = TagLoader.loadTagsForExistingRegistries(
                    resources,
                    layers.getLayer(RegistryLayer.STATIC)
            );

            var loadingLookups = TagLoader.buildUpdatedLookups(
                    layers.getAccessForLoading(RegistryLayer.WORLDGEN),
                    staticTags
            );

            RegistryAccess.Frozen worldgen = RegistryDataLoader.load(
                    resources,
                    loadingLookups,
                    RegistryDataLoader.WORLDGEN_REGISTRIES,
                    Runnable::run
            ).join();

            RegistryAccess.Frozen access = layers
                    .replaceFrom(RegistryLayer.WORLDGEN, worldgen)
                    .compositeAccess();

            staticTags.forEach(Registry.PendingTags::apply);

            TagLoader.loadTagsForExistingRegistries(
                    resources,
                    access
            ).forEach(Registry.PendingTags::apply);

            Path temporaryDirectory = Files.createTempDirectory(
                    "elytrafinder-"
            );

            LevelStorageSource.LevelStorageAccess storageAccess =
                    LevelStorageSource.createDefault(temporaryDirectory)
                            .createAccess("templates");

            Registry<Block> blocks = access.lookupOrThrow(
                    Registries.BLOCK
            );

            StructureTemplateManager templateManager =
                    new StructureTemplateManager(
                            resources,
                            storageAccess,
                            DataFixers.getDataFixer(),
                            blocks
                    );

           WorldDimensions baseDimensions =
        WorldPresets.createNormalWorldDimensions(access);

Map<ResourceKey<LevelStem>, LevelStem> dimensionMap =
        new HashMap<>(baseDimensions.dimensions());

LevelStem endStem = dimensionMap.get(LevelStem.END);

if (endStem == null ||
        !(endStem.generator() instanceof NoiseBasedChunkGenerator oldEndGenerator)) {
    throw new IllegalStateException(
            "Die Vanilla-End-Dimension konnte nicht geladen werden."
    );
}

NoiseBasedChunkGenerator correctedEndGenerator =
        new NoiseBasedChunkGenerator(
                TheEndBiomeSource.create(
                        access.lookupOrThrow(Registries.BIOME)
                ),
                oldEndGenerator.generatorSettings()
        );

dimensionMap.put(
        LevelStem.END,
        new LevelStem(endStem.type(), correctedEndGenerator)
);

WorldDimensions correctedDimensions =
        new WorldDimensions(dimensionMap);

return new VanillaWorldgenContext(
        access,
        correctedDimensions,
        templateManager,
        resources,
        storageAccess,
        temporaryDirectory
);

        } catch (Throwable throwable) {

            resources.close();
            throw throwable;
        }
    }

    public RegistryAccess.Frozen registryAccess() {
        return registryAccess;
    }

    public WorldDimensions dimensions() {
        return dimensions;
    }

    public StructureTemplateManager templateManager() {
        return templateManager;
    }

    @Override
    public void close() throws IOException {

        storageAccess.close();
        resourceManager.close();

        if (Files.exists(temporaryDirectory)) {

            try (var paths = Files.walk(temporaryDirectory)) {

                for (Path path : paths
                        .sorted(Comparator.reverseOrder())
                        .toList()) {

                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
