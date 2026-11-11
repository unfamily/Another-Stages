package net.unfamily.anotherstages.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.anotherstages.AnotherStages;

/**
 * S2C: resolved first {@code bootstrap.datapack_paths} entry for JSON-mode warnings on the client.
 */
public record StageGenPathInfoPayload(String firstDatapackPath) implements CustomPacketPayload {
    public static final Type<StageGenPathInfoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AnotherStages.MOD_ID, "stage_gen_path_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StageGenPathInfoPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    StageGenPathInfoPayload::firstDatapackPath,
                    StageGenPathInfoPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
