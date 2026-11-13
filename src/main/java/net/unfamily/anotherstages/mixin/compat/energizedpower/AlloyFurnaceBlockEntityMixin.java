package net.unfamily.anotherstages.mixin.compat.energizedpower;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.jddev0.ep.block.entity.AlloyFurnaceBlockEntity;
import me.jddev0.ep.recipe.AlloyFurnaceRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

/** EP fuel Alloy Furnace: gate recipe start via getCurrentRecipe. */
@Mixin(value = AlloyFurnaceBlockEntity.class, remap = false)
public abstract class AlloyFurnaceBlockEntityMixin {
    @WrapMethod(method = "getCurrentRecipe", remap = false)
    private Optional<RecipeHolder<AlloyFurnaceRecipe>> anotherStages$gateAlloyRecipe(
            Operation<Optional<RecipeHolder<AlloyFurnaceRecipe>>> original) {
        Optional<RecipeHolder<AlloyFurnaceRecipe>> recipe = original.call();
        if (!AsConfig.mixinEnergizedPower() || recipe.isEmpty()) {
            return recipe;
        }
        AlloyFurnaceBlockEntity self = (AlloyFurnaceBlockEntity) (Object) this;
        RecipeHolder<AlloyFurnaceRecipe> holder = recipe.get();
        if (RecipeStageGate.isBlockedForOwner(holder.id(), MachineOwners.get(self), self.getLevel())) {
            return Optional.empty();
        }
        return recipe;
    }
}
