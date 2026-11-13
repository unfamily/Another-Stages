package net.unfamily.anotherstages.mixin.compat.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.refinedmods.refinedstorage.common.support.packet.PacketContext;
import com.refinedmods.refinedstorage.common.support.packet.c2s.PatternGridCreatePatternPacket;
import net.minecraft.world.entity.player.Player;
import net.unfamily.anotherstages.compat.refinedstorage.RsCraftSubjects;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;

/** Push interacting player as RS encode subject so matrix/encode use player stages. */
@Mixin(value = PatternGridCreatePatternPacket.class, remap = false)
public abstract class PatternGridCreatePatternPacketMixin {
    @WrapMethod(method = "handle", remap = false)
    private static void anotherStages$withPlayerSubject(
            PacketContext ctx, Operation<Void> original) {
        if (!AsConfig.mixinRefinedStorage()) {
            original.call(ctx);
            return;
        }
        Player player = ctx.getPlayer();
        RsCraftSubjects.pushPlayer(player);
        try {
            original.call(ctx);
        } finally {
            RsCraftSubjects.pop();
        }
    }
}
