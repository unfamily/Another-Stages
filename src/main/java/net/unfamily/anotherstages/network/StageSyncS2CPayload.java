package net.unfamily.anotherstages.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.unfamily.anotherstages.AnotherStages;

import java.util.List;

/** Full snapshot of stages relevant to recipe-viewer / client overlays. */
public record StageSyncS2CPayload(
        List<String> playerStages,
        List<String> worldStages,
        List<String> teamStages
) implements CustomPacketPayload {
    public static final Type<StageSyncS2CPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(AnotherStages.MOD_ID, "stage_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StageSyncS2CPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    StageSyncS2CPayload::playerStages,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    StageSyncS2CPayload::worldStages,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    StageSyncS2CPayload::teamStages,
                    StageSyncS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
