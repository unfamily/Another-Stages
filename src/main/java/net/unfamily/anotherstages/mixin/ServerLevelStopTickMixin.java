package net.unfamily.anotherstages.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.unfamily.anotherstages.block.StageBlockHandler;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gates scheduled {@code tickBlock}/{@code tickFluid} and random ticks when {@code stop_tick} applies.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelStopTickMixin {
    @Inject(
            method = "tickBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void anotherStages$stopScheduledBlock(BlockPos pos, Block type, CallbackInfo ci) {
        if (!AsConfig.mixinBlockStopTick()) {
            return;
        }
        ServerLevel self = (ServerLevel) (Object) this;
        if (StageBlockHandler.shouldStopTick(self, pos, null)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "tickFluid(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/Fluid;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void anotherStages$stopScheduledFluid(BlockPos pos, Fluid type, CallbackInfo ci) {
        if (!AsConfig.mixinBlockStopTick()) {
            return;
        }
        ServerLevel self = (ServerLevel) (Object) this;
        if (StageBlockHandler.shouldStopTick(self, pos, null)) {
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void anotherStages$stopRandomBlockTick(
            BlockState state,
            Level level,
            BlockPos pos,
            RandomSource random,
            Operation<Void> original) {
        if (AsConfig.mixinBlockStopTick() && StageBlockHandler.shouldStopTick(level, pos, null)) {
            return;
        }
        original.call(state, level, pos, random);
    }

    @WrapOperation(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/FluidState;randomTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void anotherStages$stopRandomFluidTick(
            FluidState state,
            Level level,
            BlockPos pos,
            RandomSource random,
            Operation<Void> original) {
        if (AsConfig.mixinBlockStopTick() && StageBlockHandler.shouldStopTick(level, pos, null)) {
            return;
        }
        original.call(state, level, pos, random);
    }
}
