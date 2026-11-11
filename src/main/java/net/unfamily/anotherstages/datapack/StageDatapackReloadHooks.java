package net.unfamily.anotherstages.datapack;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.block.StageBlockBootstrap;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.gui.StageGuiBootstrap;
import net.unfamily.anotherstages.item.StageItemBootstrap;
import net.unfamily.anotherstages.recipe.StageRecipeBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reloads Another Stages datapack JSON with an explicit {@link ResourceManager}
 * at world start and on {@code /reload}, independent of Library JAR freshness.
 */
@EventBusSubscriber(modid = AnotherStages.MOD_ID)
public final class StageDatapackReloadHooks {
    private static final Logger LOGGER = LoggerFactory.getLogger(StageDatapackReloadHooks.class);

    private StageDatapackReloadHooks() {}

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SimplePreparableReloadListener<Object>() {
            @Override
            protected Object prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Object prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
                reloadAll(resourceManager, "datapack reload");
            }
        });
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        reloadAll(event.getServer().getResourceManager(), "server started");
    }

    private static void reloadAll(ResourceManager rm, String reason) {
        try {
            StageItemBootstrap.reload(rm);
            StageRecipeBootstrap.reload(rm);
            StageBlockBootstrap.reload(rm);
            StageGuiBootstrap.reload(rm);
            if (AsConfig.loggingEnabled()) {
                LOGGER.info("Another Stages datapack reload ({})", reason);
            }
        } catch (Exception e) {
            LOGGER.error("Another Stages datapack reload failed ({}): {}", reason, e.getMessage());
        }
    }
}
