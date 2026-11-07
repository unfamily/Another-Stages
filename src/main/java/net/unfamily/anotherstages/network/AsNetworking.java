package net.unfamily.anotherstages.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.client.ClientStageCache;
import net.unfamily.iskalib.stage.StageHooks;
import net.unfamily.iskalib.stage.StageRegistry;

import java.util.ArrayList;
import java.util.List;

/** Registers stage-sync payloads and pushes snapshots when stages change. */
@EventBusSubscriber(modid = AnotherStages.MOD_ID)
public final class AsNetworking {
    private AsNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(
                StageSyncS2CPayload.TYPE,
                StageSyncS2CPayload.STREAM_CODEC,
                (payload, context) -> ClientStageCache.apply(
                        payload.playerStages(),
                        payload.worldStages(),
                        payload.teamStages()
                )
        );
    }

    public static void installHooks() {
        StageHooks.addListener(new StageHooks.Listener() {
            @Override
            public void onPlayerStageChanged(ServerPlayer player, String stage, boolean value) {
                syncTo(player);
            }

            @Override
            public void onWorldStageChanged(MinecraftServer server, String stage, boolean value) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    syncTo(player);
                }
            }

            @Override
            public void onTeamStageChanged(MinecraftServer server, String teamName, String stage, boolean value) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    syncTo(player);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncTo(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Client clears on disconnect via ClientStageCache in client events if needed.
    }

    public static void syncTo(ServerPlayer player) {
        if (player == null || !(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        var server = serverLevel.getServer();
        if (server == null) {
            return;
        }
        StageRegistry registry = StageRegistry.getInstance(server);
        List<String> playerStages = new ArrayList<>(registry.getPlayerStages(player));
        List<String> worldStages = new ArrayList<>(registry.getWorldStages());
        List<String> teamStages = new ArrayList<>(registry.getPlayerTeamStages(player));
        PacketDistributor.sendToPlayer(player, new StageSyncS2CPayload(playerStages, worldStages, teamStages));
    }
}
