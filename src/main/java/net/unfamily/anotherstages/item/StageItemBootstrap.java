package net.unfamily.anotherstages.item;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.iskalib.stage.StageCatalog;
import net.unfamily.iskalib.stage.StageReloadHooks;
import org.jetbrains.annotations.Nullable;

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

        StageCatalog.addContributor(new StageCatalog.Contributor() {
            @Override
            public java.util.Collection<String> knownStages() {
                return StageItemHandler.collectReferencedStages();
            }

            @Override
            public java.util.Collection<String> knownStagesForType(String stageType) {
                return StageItemHandler.collectReferencedStages(stageType);
            }
        });

        StageReloadHooks.Listener previous = StageReloadHooks.getListener();
        StageReloadHooks.setListener(source -> {
            int result = previous != null ? previous.reloadStageBlock(source) : 1;
            reload(null);
            return result;
        });

        StageItemEvents.initialize();
        if (AsConfig.loggingEnabled()) {
            AnotherStages.LOGGER.info("Another Stages item loaders installed");
        }
    }

    public static void reload(@Nullable ResourceManager rm) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ResourceManager pending = StageReloadHooks.currentResourceManagerOrNull();
        ResourceManager manager = rm != null ? rm : (pending != null ? pending : (server != null ? server.getResourceManager() : null));
        StageItemHandler.loadAll(manager);
        if (AsConfig.loggingEnabled()) {
            AnotherStages.LOGGER.info("Reloaded Another Stages item restrictions");
        }
    }
}
