package net.unfamily.anotherstages.mixin.compat.energizedpower;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.jddev0.ep.block.entity.base.SelectableRecipeFluidMachineBlockEntity;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

/**
 * EP SelectableRecipeFluidMachine (Fluid Freezer / Filtration / Stone Solidifier): gate hasWork.
 */
@Mixin(value = SelectableRecipeFluidMachineBlockEntity.class, remap = false)
public abstract class SelectableRecipeFluidMachineBlockEntityMixin {
    @WrapMethod(method = "hasWork", remap = false)
    private boolean anotherStages$gateFluidSelectableHasWork(int index, Operation<Boolean> original) {
        if (!original.call(index)) {
            return false;
        }
        if (!AsConfig.mixinEnergizedPower()) {
            return true;
        }
        SelectableRecipeFluidMachineBlockEntity<?, ?> self =
                (SelectableRecipeFluidMachineBlockEntity<?, ?>) (Object) this;
        RecipeHolder<? extends Recipe<? extends RecipeInput>> recipe = self.getCurrentRecipe();
        if (recipe == null) {
            return true;
        }
        return !RecipeStageGate.isBlockedForOwner(
                recipe.id(), MachineOwners.get(self), self.getLevel());
    }
}
