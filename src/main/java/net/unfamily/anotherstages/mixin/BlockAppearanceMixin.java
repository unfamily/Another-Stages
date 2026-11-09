package net.unfamily.anotherstages.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.block.StageBlockHandler;
import net.unfamily.anotherstages.block.StageBlockRule;
import net.unfamily.anotherstages.config.AsConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side mimic: locked blocks report the replacement block's appearance.
 */
@Mixin(Block.class)
public abstract class BlockAppearanceMixin {
    @Inject(
            method = "getAppearance",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void anotherStages$mimicAppearance(
            BlockState state,
            BlockAndLightGetter level,
            BlockPos pos,
            Direction side,
            @Nullable BlockState queryState,
            @Nullable BlockPos queryPos,
            CallbackInfoReturnable<BlockState> cir) {
        if (!AsConfig.mixinBlockAppearance()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        StageBlockRule rule = StageBlockHandler.findActiveRule(state, mc.player, mc.level);
        if (StageBlockHandler.usesCamouflage(rule)) {
            cir.setReturnValue(StageBlockHandler.mimicState(rule));
        }
    }
}
