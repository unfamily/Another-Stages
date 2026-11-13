package net.unfamily.anotherstages.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.client.Ae2CraftErrorClient;

/**
 * Marks the next AE2 {@code CraftErrorScreen} {@code INCOMPLETE_PLAN} as stage-blocked
 * so the client can replace only that display text (consume-once).
 */
public record Ae2CraftBlockedS2CPayload(String recipeId) implements CustomPacketPayload {
    public static final Type<Ae2CraftBlockedS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AnotherStages.MOD_ID, "ae2_craft_blocked"));

    public static final StreamCodec<RegistryFriendlyByteBuf, Ae2CraftBlockedS2CPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    Ae2CraftBlockedS2CPayload::recipeId,
                    Ae2CraftBlockedS2CPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(Ae2CraftBlockedS2CPayload payload) {
        Ae2CraftErrorClient.mark(payload.recipeId() == null || payload.recipeId().isEmpty()
                ? null
                : payload.recipeId());
    }
}
