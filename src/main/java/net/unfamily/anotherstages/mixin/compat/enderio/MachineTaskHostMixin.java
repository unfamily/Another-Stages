package net.unfamily.anotherstages.mixin.compat.enderio;

import com.enderio.core.common.blockentity.EnderBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.compat.enderio.EioStageRearm;
import net.unfamily.anotherstages.compat.enderio.EioTaskHostSubjects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * Capture owning BE for craft-host stage gates; re-arm after a stage-gated empty {@code findRecipe}.
 * Ctor args must match bytecode ({@link EnderBlockEntity}, {@link Supplier}) or the whole mixin fails.
 */
@Pseudo
@Mixin(targets = "com.enderio.enderio.foundation.task.host.MachineTaskHost", remap = false)
public abstract class MachineTaskHostMixin {
    @Shadow(remap = false)
    public abstract void newTaskAvailable();

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void anotherStages$captureBlockEntity(
            EnderBlockEntity blockEntity, Supplier<Boolean> canAcceptNewTask, CallbackInfo ci) {
        if (blockEntity instanceof BlockEntity be) {
            EioTaskHostSubjects.bind(this, be);
        }
    }

    @Inject(method = "findNewTaskIfAble", at = @At("RETURN"), remap = false)
    private void anotherStages$rearmAfterStageGate(CallbackInfo ci) {
        if (EioStageRearm.consume(this)) {
            newTaskAvailable();
        }
    }
}
