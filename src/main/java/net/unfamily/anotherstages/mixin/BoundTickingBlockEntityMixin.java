package net.unfamily.anotherstages.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.block.StageBlockHandler;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gates {@code LevelChunk.BoundTickingBlockEntity#tick} when {@code stop_tick} is active.
 */
@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity", remap = false)
public abstract class BoundTickingBlockEntityMixin {
    @Shadow
    @Final
    private BlockEntity blockEntity;

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void anotherStages$stopTick(CallbackInfo ci) {
        if (!AsConfig.mixinBlockStopTick()) {
            return;
        }
        BlockEntity be = this.blockEntity;
        if (be == null || be.isRemoved()) {
            return;
        }
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (StageBlockHandler.shouldStopTick(level, be.getBlockPos(), null)) {
            ci.cancel();
        }
    }
}
