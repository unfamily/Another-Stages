package net.unfamily.anotherstages.mixin.compat.ae2;

import appeng.blockentity.misc.InscriberBlockEntity;
import appeng.blockentity.misc.InscriberRecipes;
import appeng.recipes.handlers.InscriberRecipe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.ae2.Ae2CraftGate;
import net.unfamily.anotherstages.compat.ae2.Ae2NetworkOwners;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevent Inscriber processing when the matched AE2 inscriber recipe is stage-locked.
 * Inventory insert remains allowed. Clears {@code cachedTask} when locked so stage unlock/lock
 * re-evaluates without requiring item remove/reinsert.
 */
@Mixin(value = InscriberBlockEntity.class, remap = false)
public abstract class InscriberBlockEntityMixin {
    @Shadow
    private InscriberRecipe cachedTask;

    @WrapOperation(
            method = "getTask",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/blockentity/misc/InscriberRecipes;findRecipe(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Z)Lappeng/recipes/handlers/InscriberRecipe;",
                    remap = false))
    private InscriberRecipe anotherStages$gateInscriberTask(
            Level level,
            ItemStack input,
            ItemStack plateA,
            ItemStack plateB,
            boolean supportNamePress,
            Operation<InscriberRecipe> original) {
        InscriberRecipe recipe = original.call(level, input, plateA, plateB, supportNamePress);
        if (!AsConfig.mixinAe2() || recipe == null || level == null) {
            return recipe;
        }
        return anotherStages$blocked(recipe, level) ? null : recipe;
    }

    @Inject(method = "getTask", at = @At("RETURN"), cancellable = true, remap = false)
    private void anotherStages$clearCachedWhenLocked(CallbackInfoReturnable<InscriberRecipe> cir) {
        InscriberRecipe recipe = cir.getReturnValue();
        if (!AsConfig.mixinAe2() || recipe == null) {
            return;
        }
        InscriberBlockEntity self = (InscriberBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) {
            return;
        }
        if (anotherStages$blocked(recipe, level)) {
            this.cachedTask = null;
            cir.setReturnValue(null);
        }
    }

    private boolean anotherStages$blocked(InscriberRecipe recipe, Level level) {
        for (RecipeHolder<InscriberRecipe> holder : InscriberRecipes.getRecipes(level)) {
            if (holder.value() == recipe
                    && Ae2CraftGate.isRecipeBlocked(
                            holder.id(),
                            Ae2NetworkOwners.fromNode(
                                    ((InscriberBlockEntity) (Object) this).getMainNode().getNode()),
                            level)) {
                return true;
            }
        }
        return false;
    }
}
