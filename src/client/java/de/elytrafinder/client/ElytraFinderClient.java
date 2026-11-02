
package de.elytrafinder.client;

import com.mojang.blaze3d.platform.InputConstants;

import de.elytrafinder.client.gui.ElytraFinderScreen;
import de.elytrafinder.client.gui.ElytraFinderHud;
import de.elytrafinder.client.worldgen.EndCitySearchService;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class ElytraFinderClient implements ClientModInitializer {

    private static final EndCitySearchService SEARCH_SERVICE =
            new EndCitySearchService();

    @Override
    public void onInitializeClient() {

        KeyMapping.Category category =
                KeyMapping.Category.register(
                        Identifier.fromNamespaceAndPath(
                                "elytrafinder",
                                "main"
                        )
                );

        KeyMapping openFinderKey =
                KeyMappingHelper.registerKeyMapping(
                        new KeyMapping(
                                "key.elytrafinder.open",
                                InputConstants.Type.KEYSYM,
                                InputConstants.KEY_J,
                                category
                        )
                );

        // HUD-Overlay registrieren.
        ElytraFinderHud.register();

        // Statusprüfung läuft unabhängig vom geöffneten Menü.
        ClientTickEvents.END_CLIENT_TICK.register(
                ElytraFinderHud::tick
        );

        // Taste J öffnet den ElytraFinder.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (openFinderKey.consumeClick()) {

                if (client.player == null) {
                    continue;
                }

                client.gui.setScreen(
                        new ElytraFinderScreen(SEARCH_SERVICE)
                );
            }
        });

        // Suchdienst beim Beenden von Minecraft schließen.
        ClientLifecycleEvents.CLIENT_STOPPING.register(
                client -> SEARCH_SERVICE.close()
        );

        System.out.println(
                "[ElytraFinder] Client erfolgreich initialisiert!"
        );
    }
}
