package net.unfamily.anotherstages.block;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.iskalib.stage.StageCatalog;
import net.unfamily.iskalib.stage.StageReloadHooks;
import org.jetbrains.annotations.Nullable;

public final class StageBlockBootstrap {
    private static boolean installed;

    private StageBlockBootstrap() {}

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        StageCatalog.addContributor(StageBlockHandler::collectReferencedStages);
        StageReloadHooks.Listener previous = StageReloadHooks.getListener();
        StageReloadHooks.setListener(source -> {
            int result = previous != null ? previous.reloadStageBlock(source) : 1;
            reload(null);
            return result;
        });
        reload(null);
        if (AsConfig.loggingEnabled()) {
            AnotherStages.LOGGER.info("Another Stages block loaders installed");
        }
    }

    public static void reload(@Nullable ResourceManager rm) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ResourceManager pending = StageReloadHooks.currentResourceManagerOrNull();
        ResourceManager manager = rm != null ? rm : (pending != null ? pending : (server != null ? server.getResourceManager() : null));
        StageBlockHandler.loadAll(manager);
    }
}
