package net.unfamily.anotherstages.recipe;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.iskalib.stage.StageCatalog;
import net.unfamily.iskalib.stage.StageReloadHooks;
import org.jetbrains.annotations.Nullable;

public final class StageRecipeBootstrap {
    private static boolean installed;

    private StageRecipeBootstrap() {}

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        RecipeStageProvider.register();
        StageCatalog.addContributor(StageRecipeHandler::collectReferencedStages);
        StageReloadHooks.Listener previous = StageReloadHooks.getListener();
        StageReloadHooks.setListener(source -> {
            int result = previous != null ? previous.reloadStageBlock(source) : 1;
            reload(null);
            return result;
        });
        reload(null);
        if (AsConfig.loggingEnabled()) {
            AnotherStages.LOGGER.info("Another Stages recipe provider installed");
        }
    }

    public static void reload(@Nullable ResourceManager rm) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ResourceManager pending = StageReloadHooks.currentResourceManagerOrNull();
        ResourceManager manager = rm != null ? rm : (pending != null ? pending : (server != null ? server.getResourceManager() : null));
        StageRecipeHandler.loadAll(manager);
    }
}
