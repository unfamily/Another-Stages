package net.unfamily.anotherstages.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
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

/**
 * C2S: delete a restriction from the fixed export file.
 * JSON uses silent {@code id}; Kube uses {@code kubeEntryIndex}.
 */
public record DeleteStageGeneratorPayload(String category, String id, int kubeEntryIndex)
        implements CustomPacketPayload {
    public static final Type<DeleteStageGeneratorPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(AnotherStages.MOD_ID, "delete_stage_generator"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteStageGeneratorPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    DeleteStageGeneratorPayload::category,
                    ByteBufCodecs.STRING_UTF8,
                    DeleteStageGeneratorPayload::id,
                    ByteBufCodecs.VAR_INT,
                    DeleteStageGeneratorPayload::kubeEntryIndex,
                    DeleteStageGeneratorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteStageGeneratorPayload payload, ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) {
            player.sendSystemMessage(Component.translatable(
                    "screen.another_stages.stage_generator.save.denied"));
            return;
        }
        try {
            RestrictionDraft.Category cat = RestrictionDraft.Category.valueOf(payload.category());
            StageGenFileWriter.Result result =
                    StageGenFileWriter.delete(player, cat, payload.id(), payload.kubeEntryIndex());
            // Silent like save — only report failures.
            if (!result.ok()) {
                player.sendSystemMessage(result.message());
            }
        } catch (Exception ex) {
            player.sendSystemMessage(Component.translatable(
                    "screen.another_stages.stage_generator.save.failed",
                    ex.getMessage() == null ? "delete" : ex.getMessage()));
        }
    }
}
