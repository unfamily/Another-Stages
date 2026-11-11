package net.unfamily.anotherstages.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.anotherstages.AnotherStages;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: push loaded Stage Generator drafts and known-stage catalog when the editor opens.
 */
public record StageGenSessionSyncS2CPayload(List<String> draftJsons, List<String> knownStageJsons)
        implements CustomPacketPayload {
    public static final Type<StageGenSessionSyncS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AnotherStages.MOD_ID, "stage_gen_session_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StageGenSessionSyncS2CPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
                    StageGenSessionSyncS2CPayload::draftJsons,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
                    StageGenSessionSyncS2CPayload::knownStageJsons,
                    StageGenSessionSyncS2CPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
