package net.unfamily.anotherstages.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.block.StageBlockHandler;
import net.unfamily.anotherstages.block.StageBlockRule;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side: hide the real mesh while a stage_block rule applies so
 * {@link net.unfamily.anotherstages.client.block.StageBlockClientEvents} can inject the mimic geometry.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockRenderShapeMixin {
    @Inject(
            method = "getRenderShape",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void anotherStages$hideWhenMimicked(CallbackInfoReturnable<RenderShape> cir) {
        if (!AsConfig.mixinBlockAppearance()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !StageBlockHandler.hasAnyRules()) {
            return;
        }
        // Concrete runtime type is always BlockState.
        BlockState state = (BlockState) (Object) this;
        StageBlockRule rule = StageBlockHandler.findActiveRule(state, mc.player, mc.level);
        if (StageBlockHandler.usesCamouflage(rule)) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }
}
