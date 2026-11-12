package net.unfamily.anotherstages.mixin.compat.advancedae;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.pedroksl.advanced_ae.common.entities.ReactionChamberEntity;
import net.pedroksl.advanced_ae.recipes.ReactionChamberRecipe;
import net.pedroksl.advanced_ae.recipes.ReactionChamberRecipes;
import net.unfamily.anotherstages.compat.ae2.Ae2CraftGate;
import net.unfamily.anotherstages.compat.ae2.Ae2NetworkOwners;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevent Reaction Chamber processing when the matched AdvAE reaction recipe is stage-locked.
 * Insert remains allowed; {@code getTask} drives process start only.
 */
@Mixin(value = ReactionChamberEntity.class, remap = false)
public abstract class ReactionChamberEntityMixin {
    @Shadow
    private ReactionChamberRecipe cachedTask;

    @Inject(method = "getTask", at = @At("RETURN"), cancellable = true, remap = false)
    private void anotherStages$gateReactionTask(CallbackInfoReturnable<ReactionChamberRecipe> cir) {
        ReactionChamberRecipe recipe = cir.getReturnValue();
        if (!AsConfig.mixinAe2() || recipe == null) {
            return;
        }
        ReactionChamberEntity self = (ReactionChamberEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) {
            return;
        }
        for (RecipeHolder<ReactionChamberRecipe> holder : ReactionChamberRecipes.getRecipes(level)) {
            if (holder.value() == recipe
                    && Ae2CraftGate.isRecipeBlocked(
                            holder.id(),
                            Ae2NetworkOwners.fromNode(self.getMainNode().getNode()),
                            level)) {
                this.cachedTask = null;
                cir.setReturnValue(null);
                return;
            }
        }
    }
}
