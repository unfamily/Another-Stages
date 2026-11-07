package net.unfamily.anotherstages.gui;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.iskalib.stage.StageCatalog;
import net.unfamily.iskalib.stage.StageHooks;
import net.unfamily.iskalib.stage.StageReloadHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wires stage_gui loaders into catalog/reload and force-closes menus when stages change.
 */
public final class StageGuiBootstrap {
    private static boolean installed;

    private StageGuiBootstrap() {}

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        StageCatalog.addContributor(StageGuiHandler::collectReferencedStages);
        StageReloadHooks.Listener previous = StageReloadHooks.getListener();
        StageReloadHooks.setListener(source -> {
            int result = previous != null ? previous.reloadStageBlock(source) : 1;
            reload(null);
            return result;
        });
        StageHooks.addListener(new StageHooks.Listener() {
            @Override
            public void onPlayerStageChanged(ServerPlayer player, String stage, boolean value) {
                forceCloseIfBlocked(player);
            }

            @Override
            public void onWorldStageChanged(MinecraftServer server, String stage, boolean value) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    forceCloseIfBlocked(player);
                }
            }

            @Override
            public void onTeamStageChanged(MinecraftServer server, String teamName, String stage, boolean value) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    forceCloseIfBlocked(player);
                }
            }
        });
        reload(null);
        if (AsConfig.loggingEnabled()) {
            AnotherStages.LOGGER.info("Another Stages gui loaders installed");
        }
    }

    public static void reload(@Nullable ResourceManager rm) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ResourceManager pending = StageReloadHooks.currentResourceManagerOrNull();
        ResourceManager manager = rm != null ? rm : (pending != null ? pending : (server != null ? server.getResourceManager() : null));
        StageGuiHandler.loadAll(manager);
    }

    public static void forceCloseIfBlocked(ServerPlayer player) {
        if (player == null || !StageGuiHandler.hasLoadedRules()) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu == player.inventoryMenu) {
            return;
        }
        List<StageGuiRule> active = StageGuiHandler.findActiveRules(player, menu);
        if (!StageGuiHandler.shouldBlockOpen(active)) {
            return;
        }
        StageGuiRule msgRule = StageGuiHandler.firstBlockOpenMessage(active);
        player.closeContainer();
        if (msgRule != null) {
            player.sendSystemMessage(StageGuiHandler.resolveMessage(msgRule), true);
        }
    }
}
