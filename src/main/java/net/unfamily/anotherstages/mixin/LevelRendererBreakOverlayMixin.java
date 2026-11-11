package net.unfamily.anotherstages.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.block.StageBlockHandler;
import net.unfamily.anotherstages.block.StageBlockRule;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * {@code renderBreakingTexture} only draws when {@code getRenderShape() == MODEL}.
 * Camouflaged stage blocks are {@code INVISIBLE}, so swap in the mimic state for the crack overlay.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererBreakOverlayMixin {
    @ModifyArg(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;renderBreakingTexture("
                            + "Lnet/minecraft/world/level/block/state/BlockState;"
                            + "Lnet/minecraft/core/BlockPos;"
                            + "Lnet/minecraft/world/level/BlockAndTintGetter;"
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;"
                            + "Lnet/neoforged/neoforge/client/model/data/ModelData;)V",
                    remap = false),
            index = 0,
            remap = false)
    private BlockState anotherStages$mimicBreakOverlay(BlockState state) {
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
