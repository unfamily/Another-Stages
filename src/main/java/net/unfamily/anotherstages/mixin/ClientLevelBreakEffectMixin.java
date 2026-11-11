package net.unfamily.anotherstages.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.block.StageBlockHandler;
import net.unfamily.anotherstages.block.StageBlockRule;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Hit/destroy particles: camouflaged stage blocks are {@code INVISIBLE}; use the mimic state.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelBreakEffectMixin {
    @ModifyVariable(
            method = "addBreakingBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/phys/HitResult;)V",
            at = @At("STORE"),
            ordinal = 0,
            remap = false)
    private BlockState anotherStages$mimicHitParticles(BlockState state) {
        return mimicIfCamouflaged(state);
    }

    @ModifyVariable(method = "addDestroyBlockEffect", at = @At("HEAD"), argsOnly = true, remap = false)
    private BlockState anotherStages$mimicDestroyParticles(BlockState state) {
        return mimicIfCamouflaged(state);
    }

    private static BlockState mimicIfCamouflaged(BlockState state) {
        if (!AsConfig.mixinBlockAppearance() || !StageBlockHandler.hasAnyRules()) {
            return state;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return state;
        }
        StageBlockRule rule = StageBlockHandler.findActiveRule(state, mc.player, mc.level);
        return StageBlockHandler.usesCamouflage(rule) ? StageBlockHandler.mimicState(rule) : state;
    }
}
