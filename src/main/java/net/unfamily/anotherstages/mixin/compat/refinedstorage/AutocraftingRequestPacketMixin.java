package net.unfamily.anotherstages.mixin.compat.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.refinedmods.refinedstorage.common.support.packet.PacketContext;
import com.refinedmods.refinedstorage.common.support.packet.c2s.AutocraftingRequestPacket;
import net.minecraft.world.entity.player.Player;
import net.unfamily.anotherstages.compat.refinedstorage.RsCraftSubjects;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;

/** Push requesting player UUID around RS preview startTask. */
@Mixin(value = AutocraftingRequestPacket.class, remap = false)
public abstract class AutocraftingRequestPacketMixin {
    @WrapMethod(method = "handle", remap = false)
    private static void anotherStages$withPlayerSubject(
            AutocraftingRequestPacket packet, PacketContext ctx, Operation<Void> original) {
        if (!AsConfig.mixinRefinedStorage()) {
            original.call(packet, ctx);
            return;
        }
        Player player = ctx.getPlayer();
        RsCraftSubjects.pushPlayer(player);
        try {
            original.call(packet, ctx);
        } finally {
            RsCraftSubjects.pop();
        }
    }
}
