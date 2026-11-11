package net.unfamily.anotherstages.kubejs;

import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.iskalib.stage.StageHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Posts {@link AnotherStagesEvents#STAGE_CHANGED} from Library {@link StageHooks}.
 * Nested stage changes inside a Kube handler are skipped (no re-entrant post).
 */
public final class KubeStageChangedBridge {
    private static boolean installed;
    /** True while a {@code stageChanged} handler is running on this thread. */
    private static final ThreadLocal<Boolean> POSTING = ThreadLocal.withInitial(() -> false);

    private KubeStageChangedBridge() {}

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        StageHooks.addListener(new StageHooks.Listener() {
            @Override
            public void onPlayerStageChanged(ServerPlayer player, String stage, boolean value) {
                MinecraftServer server = player != null && player.level() != null
                        ? player.level().getServer()
                        : null;
                post("player", stage, value, player, null, server);
            }

            @Override
            public void onWorldStageChanged(MinecraftServer server, String stage, boolean value) {
                post("world", stage, value, null, null, server);
            }

            @Override
            public void onTeamStageChanged(MinecraftServer server, String teamName, String stage, boolean value) {
                post("team", stage, value, null, teamName, server);
            }
        });
    }

    private static void post(
            String stageType,
            @Nullable String stage,
            boolean added,
            @Nullable ServerPlayer player,
            @Nullable String team,
            @Nullable MinecraftServer server
    ) {
        if (!ModList.get().isLoaded("kubejs")) {
            return;
        }
        if (server == null || stage == null || stage.isEmpty()) {
            return;
        }
        if (Boolean.TRUE.equals(POSTING.get())) {
            if (AsConfig.loggingEnabled()) {
                AnotherStages.LOGGER.debug(
                        "Skipping nested AnotherStagesEvents.stageChanged (type={}, stage={}, added={})",
                        stageType, stage, added);
            }
            return;
        }
        if (!AnotherStagesEvents.STAGE_CHANGED.hasListeners()) {
            return;
        }
        POSTING.set(true);
        try {
            AnotherStagesEvents.STAGE_CHANGED.post(
                    ScriptType.SERVER,
                    new StageChangedKubeEvent(stageType, stage, added, player, team, server));
        } catch (Exception ex) {
            AnotherStages.LOGGER.error(
                    "AnotherStagesEvents.stageChanged failed (type={}, stage={}, added={}): {}",
                    stageType, stage, added, ex.toString());
            if (AsConfig.loggingEnabled()) {
                AnotherStages.LOGGER.error("stageChanged stack", ex);
            }
        } finally {
            POSTING.set(false);
        }
    }
}