package net.unfamily.anotherstages.item;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.iskalib.stage.StageCatalog;
import net.unfamily.iskalib.stage.StageReloadHooks;

/**
 * Wires item stage ownership into Library catalog/reload without duplicating StageRegistry.
 */
public final class StageItemBootstrap {
    private static boolean installed;

    private StageItemBootstrap() {}

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;

        StageCatalog.addContributor(StageItemHandler::collectReferencedStages);

        StageReloadHooks.Listener previous = StageReloadHooks.getListener();
        StageReloadHooks.setListener(source -> {
            int result = previous != null ? previous.reloadStageBlock(source) : 1;
            reloadItems(source);
            return result;
        });

        StageItemEvents.initialize();
        if (AsConfig.loggingEnabled()) {
            AnotherStages.LOGGER.info("Another Stages item loaders installed");
        }
    }

    private static void reloadItems(CommandSourceStack source) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ResourceManager rm = server != null ? server.getResourceManager() : null;
        StageItemHandler.loadAll(rm);
        if (AsConfig.loggingEnabled()) {
            AnotherStages.LOGGER.info("Reloaded Another Stages item restrictions");
        }
    }
}
