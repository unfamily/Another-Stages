package net.unfamily.anotherstages.mixin.compat.extendedae;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.glodblock.github.extendedae.common.me.CraftingThread;
import net.minecraft.core.Direction;
import net.unfamily.anotherstages.compat.ae2.Ae2CraftGate;
import net.unfamily.anotherstages.compat.ae2.Ae2NetworkOwners;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Assembler Matrix / Ex crafting threads: refuse acceptJob when pattern recipe is stage-locked.
 */
@Mixin(value = CraftingThread.class, remap = false)
public abstract class CraftingThreadAcceptJobMixin {
    @Shadow(remap = false)
    @Final
    protected AEBaseBlockEntity host;

    @Shadow(remap = false)
    @Final
    protected IGridConnectedBlockEntity girdHost;

    @Inject(method = "acceptJob", at = @At("HEAD"), cancellable = true, remap = false)
    private void anotherStages$gateAcceptJob(
            IPatternDetails patternDetails,
            KeyCounter[] table,
            Direction where,
            CallbackInfoReturnable<Boolean> cir) {
        if (!AsConfig.mixinAe2() || host == null) {
            return;
        }
        var node = girdHost != null && girdHost.getMainNode() != null
                ? girdHost.getMainNode().getNode()
                : null;
        if (Ae2CraftGate.isPatternBlocked(
                patternDetails, Ae2NetworkOwners.fromNode(node), host.getLevel())) {
            cir.setReturnValue(false);
        }
    }
}
