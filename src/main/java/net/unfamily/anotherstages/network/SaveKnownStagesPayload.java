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
import net.unfamily.anotherstages.client.gui.stagegen.KnownStageDraft;
import net.unfamily.anotherstages.stagegen.StageGenFileWriter;

import java.util.ArrayList;
import java.util.List;

/** C2S: rewrite Stage Generator known-stages catalog (JSON or Kube). */
public record SaveKnownStagesPayload(List<String> knownJsons) implements CustomPacketPayload {
    public static final Type<SaveKnownStagesPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(AnotherStages.MOD_ID, "save_known_stages"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveKnownStagesPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
                    SaveKnownStagesPayload::knownJsons,
                    SaveKnownStagesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveKnownStagesPayload payload, ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) {
            AnotherStages.LOGGER.warn("Known stages save denied for {}", player.getGameProfile().name());
            return;
        }
        try {
            List<KnownStageDraft> drafts = new ArrayList<>();
            for (String json : payload.knownJsons()) {
                try {
                    drafts.add(KnownStageDraft.fromJson(json));
                } catch (Exception ex) {
                    AnotherStages.LOGGER.warn("Skipping bad known-stage blob: {}", ex.toString());
                }
            }
            StageGenFileWriter.Result result = StageGenFileWriter.saveKnownStages(player, drafts);
            if (!result.ok()) {
                AnotherStages.LOGGER.warn("Known stages save failed: {}", result.message().getString());
            }
        } catch (Exception ex) {
            AnotherStages.LOGGER.error("Known stages save failed", ex);
        }
    }
}
