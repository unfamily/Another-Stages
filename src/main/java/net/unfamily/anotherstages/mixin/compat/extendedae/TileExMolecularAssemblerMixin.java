package net.unfamily.anotherstages.mixin.compat.extendedae;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import com.glodblock.github.extendedae.common.tileentities.TileExMolecularAssembler;
import net.minecraft.core.Direction;
import net.unfamily.anotherstages.compat.ae2.Ae2CraftGate;
import net.unfamily.anotherstages.compat.ae2.Ae2NetworkOwners;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileExMolecularAssembler.class, remap = false)
public abstract class TileExMolecularAssemblerMixin {
    @Inject(method = "pushPattern", at = @At("HEAD"), cancellable = true, remap = false)
    private void anotherStages$gateExPush(
            IPatternDetails patternDetails,
            KeyCounter[] inputHolder,
            Direction ejectionDirection,
            CallbackInfoReturnable<Boolean> cir) {
        if (!AsConfig.mixinAe2()) {
            return;
        }
        TileExMolecularAssembler self = (TileExMolecularAssembler) (Object) this;
        if (Ae2CraftGate.isPatternBlocked(
                patternDetails,
                Ae2NetworkOwners.fromNode(self.getMainNode().getNode()),
                self.getLevel())) {
            cir.setReturnValue(false);
        }
    }
}
