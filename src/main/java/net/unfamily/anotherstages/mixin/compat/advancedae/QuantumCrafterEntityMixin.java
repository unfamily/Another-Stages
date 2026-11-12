package net.unfamily.anotherstages.mixin.compat.advancedae;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import net.minecraft.world.item.ItemStack;
import net.pedroksl.advanced_ae.common.entities.QuantumCrafterEntity;
import net.unfamily.anotherstages.compat.ae2.Ae2CraftGate;
import net.unfamily.anotherstages.compat.ae2.Ae2NetworkOwners;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = QuantumCrafterEntity.class, remap = false)
public abstract class QuantumCrafterEntityMixin {
    @Inject(method = "makeCraftingRecipeList", at = @At("RETURN"), remap = false)
    private void anotherStages$invalidateLockedPatterns(CallbackInfo ci) {
        if (!AsConfig.mixinAe2()) {
            return;
        }
        QuantumCrafterEntity self = (QuantumCrafterEntity) (Object) this;
        var patterns = self.getPatternInventory();
        if (patterns == null || self.getLevel() == null) {
            return;
        }
        for (int i = 0; i < patterns.size(); i++) {
            ItemStack stack = patterns.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            IPatternDetails details = PatternDetailsHelper.decodePattern(stack, self.getLevel());
            if (details != null
                    && Ae2CraftGate.isPatternBlocked(
                            details,
                            Ae2NetworkOwners.fromNode(self.getMainNode().getNode()),
                            self.getLevel())) {
                self.setInvalidPattern(i, true);
            }
        }
    }
}
