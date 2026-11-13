package net.unfamily.anotherstages.mixin.compat.energizedpower;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.jddev0.ep.block.entity.base.SimpleRecipeFluidMachineBlockEntity;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

/**
 * EP SimpleRecipeFluidMachine (Plant Growth / Transposer / Stone Liquefier / …): gate work start.
 */
@Mixin(value = SimpleRecipeFluidMachineBlockEntity.class, remap = false)
public abstract class SimpleRecipeFluidMachineBlockEntityMixin<C extends RecipeInput, R extends Recipe<C>> {
    @WrapMethod(method = "getCurrentWorkData", remap = false)
    private Optional<RecipeHolder<R>> anotherStages$gateFluidWorkData(
            int index, Operation<Optional<RecipeHolder<R>>> original) {
        Optional<RecipeHolder<R>> recipe = original.call(index);
        if (!AsConfig.mixinEnergizedPower() || recipe.isEmpty()) {
            return recipe;
        }
        SimpleRecipeFluidMachineBlockEntity<?, ?> self =
                (SimpleRecipeFluidMachineBlockEntity<?, ?>) (Object) this;
        RecipeHolder<R> holder = recipe.get();
        if (RecipeStageGate.isBlockedForOwner(holder.id(), MachineOwners.get(self), self.getLevel())) {
            return Optional.empty();
        }
        return recipe;
    }
}
