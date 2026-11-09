package net.unfamily.anotherstages.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.client.ClientStageCache;
import net.unfamily.anotherstages.client.gui.stagegen.RestrictionDraft;
import net.unfamily.anotherstages.client.gui.stagegen.StageGenClientPaths;
import net.unfamily.anotherstages.client.gui.stagegen.StageGenSession;
import net.unfamily.anotherstages.menu.StageGeneratorMenu;
import net.unfamily.anotherstages.stagegen.StageGenFileReader;
import net.unfamily.anotherstages.stagegen.StageGenPaths;
import net.unfamily.iskalib.stage.StageEditHooks;
import net.unfamily.iskalib.stage.StageHooks;
import net.unfamily.iskalib.stage.StageRegistry;

import java.util.ArrayList;
import java.util.List;

/** Registers stage-sync payloads and Stage Generator open/save. */
@EventBusSubscriber(modid = AnotherStages.MOD_ID)
public final class AsNetworking {
    private AsNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                StageSyncS2CPayload.TYPE,
                StageSyncS2CPayload.STREAM_CODEC,
                (payload, context) -> ClientStageCache.apply(
                        payload.playerStages(),
                        payload.worldStages(),
                        payload.teamStages()
                )
        );
        registrar.playToClient(
                StageGenPathInfoPayload.TYPE,
                StageGenPathInfoPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> StageGenClientPaths.setFirstDatapackPath(payload.firstDatapackPath()))
        );
        registrar.playToClient(
                StageGenSessionSyncS2CPayload.TYPE,
                StageGenSessionSyncS2CPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> applySessionSync(payload))
        );
        registrar.playToServer(
                SaveStageGeneratorPayload.TYPE,
                SaveStageGeneratorPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        context.enqueueWork(() -> SaveStageGeneratorPayload.handle(payload, serverPlayer));
                    }
                }
        );
        registrar.playToServer(
                DeleteStageGeneratorPayload.TYPE,
                DeleteStageGeneratorPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        context.enqueueWork(() -> DeleteStageGeneratorPayload.handle(payload, serverPlayer));
                    }
                }
        );
    }

    private static void applySessionSync(StageGenSessionSyncS2CPayload payload) {
        List<RestrictionDraft> drafts = new ArrayList<>();
        for (String json : payload.draftJsons()) {
            try {
                drafts.add(RestrictionDraft.fromJson(json));
            } catch (Exception ex) {
                AnotherStages.LOGGER.warn("Skipping bad Stage Generator draft sync blob: {}", ex.toString());
            }
        }
        StageGenSession.get().replaceAll(drafts);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof net.unfamily.anotherstages.client.gui.stagegen.StageGeneratorScreen screen) {
            screen.onSessionSynced();
        }
    }

    public static void installHooks() {
        StageEditHooks.setListener(AsNetworking::openStageGenerator);
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

    /** Opens the Stage Generator menu on the player and syncs loaded drafts. */
    public static void openStageGenerator(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PacketDistributor.sendToPlayer(
                player,
                new StageGenPathInfoPayload(StageGenPaths.displayFirstDatapackRoot()));
        List<String> blobs = new ArrayList<>();
        for (RestrictionDraft draft : StageGenFileReader.loadAllForOpen()) {
            blobs.add(draft.toJson().toString());
        }
        PacketDistributor.sendToPlayer(player, new StageGenSessionSyncS2CPayload(blobs));
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new StageGeneratorMenu(id, inv),
                Component.translatable("screen.another_stages.stage_generator.title")));
    }
}
