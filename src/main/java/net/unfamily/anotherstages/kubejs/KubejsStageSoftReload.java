package net.unfamily.anotherstages.kubejs;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.iskalib.stage.StageReloadHooks;

/**
 * Soft-reloads KubeJS {@code server_scripts} before stage datapack reload so
 * {@code AnotherStages.items/recipes/blocks/guis} script rules refresh in the same command.
 */
public final class KubejsStageSoftReload {
    private static boolean installed;

    private KubejsStageSoftReload() {}

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        StageReloadHooks.Listener previous = StageReloadHooks.getListener();
        StageReloadHooks.setListener(source -> {
            softReloadServerScripts(source);
            return previous != null ? previous.reloadStageBlock(source) : 1;
        });
    }

    /**
     * @return true if KubeJS server scripts were reloaded
     */
    public static boolean softReloadServerScripts(CommandSourceStack source) {
        if (!ModList.get().isLoaded("kubejs")) {
            return false;
        }
        MinecraftServer server = source.getServer();
        if (server == null) {
            return false;
        }
        try {
            var managers = server.getServerResources().managers();
            var scriptManager = ((dev.latvian.mods.kubejs.core.ReloadableServerResourcesKJS) (Object) managers)
                    .kjs$getServerScriptManager();
            if (scriptManager == null) {
                return false;
            }
            scriptManager.reload();
            source.sendSuccess(
                    () -> Component.translatable("commands.another_stages.kubejs_stages_reloaded"),
                    true);
            if (AsConfig.loggingEnabled()) {
                AnotherStages.LOGGER.info("Soft-reloaded KubeJS server scripts (Another Stages script restrictions)");
            }
            return true;
        } catch (Throwable t) {
            if (AsConfig.loggingEnabled()) {
                AnotherStages.LOGGER.warn("KubeJS soft reload skipped: {}", t.toString());
            }
            return false;
        }
    }
}
