package net.unfamily.anotherstages.datapack;

import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.iskalib.reload.UtilsReloadHooks;
import net.unfamily.iskalib.stage.StageReloadHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Ensures {@code /iska_lib_dev reload} also refreshes Another Stages stage JSON
 * (and Kube soft-reload via {@link StageReloadHooks}) after Utils content reloads.
 * <p>
 * Utils may register {@link UtilsReloadHooks} after AS construction; common setup and
 * server starting re-wrap so the chain stays AS-aware.
 */
@EventBusSubscriber(modid = AnotherStages.MOD_ID)
public final class AsUtilsReloadBridge {
    private static @Nullable UtilsReloadHooks.Listener asWrapper;

    private AsUtilsReloadBridge() {}

    public static void install() {
        wrapIfNeeded();
    }

    /** Call from the mod event bus ({@link FMLCommonSetupEvent}). */
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(AsUtilsReloadBridge::wrapIfNeeded);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        wrapIfNeeded();
    }

    private static synchronized void wrapIfNeeded() {
        UtilsReloadHooks.Listener current = UtilsReloadHooks.getListener();
        if (current != null && current == asWrapper) {
            return;
        }
        UtilsReloadHooks.Listener previous = current;
        asWrapper = source -> reloadWithStages(previous, source);
        UtilsReloadHooks.setListener(asWrapper);
        if (AsConfig.loggingEnabled()) {
            AnotherStages.LOGGER.info(
                    "UtilsReloadHooks wrapped for Another Stages (iska_lib_dev reload → stage JSON)");
        }
    }

    private static int reloadWithStages(
            @Nullable UtilsReloadHooks.Listener previous, CommandSourceStack source) {
        int result = previous != null ? previous.reloadFromDatapacks(source) : 1;
        StageReloadHooks.Listener stageListener = StageReloadHooks.getListener();
        if (stageListener != null) {
            int stageResult = stageListener.reloadStageBlock(source);
            if (stageResult > 0) {
                result = Math.max(result, stageResult);
            }
        }
        return result;
    }
}
