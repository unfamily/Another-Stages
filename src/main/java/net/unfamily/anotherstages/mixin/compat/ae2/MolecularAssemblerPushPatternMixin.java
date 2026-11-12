package net.unfamily.anotherstages.mixin.compat.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.MolecularAssemblerBlockEntity;
import net.minecraft.core.Direction;
import net.unfamily.anotherstages.compat.ae2.Ae2CraftGate;
import net.unfamily.anotherstages.compat.ae2.Ae2NetworkOwners;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Safety net: refuse Molecular Assembler pattern push when the vanilla recipe is stage-locked.
 * Insert into adjacent inventories is unaffected; the assembler simply does not start work.
 */
@Mixin(value = MolecularAssemblerBlockEntity.class, remap = false)
public abstract class MolecularAssemblerPushPatternMixin {
    @Inject(method = "pushPattern", at = @At("HEAD"), cancellable = true, remap = false)
    private void anotherStages$gatePushPattern(
            IPatternDetails patternDetails,
            KeyCounter[] table,
            Direction where,
            CallbackInfoReturnable<Boolean> cir) {
        if (!AsConfig.mixinAe2()) {
            return;
        }
        MolecularAssemblerBlockEntity self = (MolecularAssemblerBlockEntity) (Object) this;
        var node = self.getMainNode() != null ? self.getMainNode().getNode() : null;
        if (Ae2CraftGate.isPatternBlocked(
                patternDetails, Ae2NetworkOwners.fromNode(node), self.getLevel())) {
            cir.setReturnValue(false);
        }
    }
}
