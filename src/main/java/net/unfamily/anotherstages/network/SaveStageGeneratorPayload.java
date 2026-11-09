package net.unfamily.anotherstages.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.client.gui.stagegen.RestrictionDraft;
import net.unfamily.anotherstages.stagegen.StageGenFileWriter;

/** C2S: persist a Stage Generator draft to fixed JSON/Kube paths (silent — no chat spam). */
public record SaveStageGeneratorPayload(String draftJson) implements CustomPacketPayload {
    public static final Type<SaveStageGeneratorPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(AnotherStages.MOD_ID, "save_stage_generator"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveStageGeneratorPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    SaveStageGeneratorPayload::draftJson,
                    SaveStageGeneratorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveStageGeneratorPayload payload, ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) {
            AnotherStages.LOGGER.warn("Stage Generator save denied for {}", player.getGameProfile().name());
            return;
        }
        try {
            RestrictionDraft draft = RestrictionDraft.fromJson(payload.draftJson());
            StageGenFileWriter.Result result = StageGenFileWriter.save(player, draft);
            if (!result.ok()) {
                AnotherStages.LOGGER.warn("Stage Generator save failed: {}", result.message().getString());
            }
        } catch (Exception ex) {
            AnotherStages.LOGGER.error("Stage Generator save failed", ex);
        }
    }
}
