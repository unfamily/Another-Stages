package net.unfamily.anotherstages.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.block.StageBlockHandler;
import net.unfamily.anotherstages.block.StageBlockRule;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * NeoForge 26.1 only submits break cracks when {@code getRenderShape() == MODEL}.
 * Mimicked stage blocks are {@code INVISIBLE}, so swap in the mimic state for the overlay.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererBreakOverlayMixin {
    @Inject(method = "extractBlockDestroyAnimation", at = @At("TAIL"), remap = false)
    private void anotherStages$mimicBreakOverlay(Camera camera, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (!AsConfig.mixinBlockAppearance() || !StageBlockHandler.hasAnyRules()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        List<BlockBreakingRenderState> states = levelRenderState.blockBreakingRenderStates;
        for (int i = 0; i < states.size(); i++) {
            BlockBreakingRenderState entry = states.get(i);
            StageBlockRule rule = StageBlockHandler.findActiveRule(entry.blockState(), mc.player, mc.level);
            if (!StageBlockHandler.usesCamouflage(rule)) {
                continue;
            }
            BlockState mimic = StageBlockHandler.mimicState(rule);
            states.set(i, new BlockBreakingRenderState(entry.blockPos(), mimic, entry.progress()));
        }
    }
}
