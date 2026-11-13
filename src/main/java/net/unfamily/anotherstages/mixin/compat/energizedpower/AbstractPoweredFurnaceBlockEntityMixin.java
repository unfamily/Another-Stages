package net.unfamily.anotherstages.mixin.compat.energizedpower;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.jddev0.ep.block.entity.AbstractPoweredFurnaceBlockEntity;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

/**
 * EP powered / advanced / elite furnaces: prevent cook start when the cooking recipe is stage-locked.
 */
@Mixin(value = AbstractPoweredFurnaceBlockEntity.class, remap = false)
public abstract class AbstractPoweredFurnaceBlockEntityMixin {
    @WrapMethod(method = "getCurrentWorkData", remap = false)
    private Optional<RecipeHolder<? extends AbstractCookingRecipe>> anotherStages$gateWorkData(
            int index, Operation<Optional<RecipeHolder<? extends AbstractCookingRecipe>>> original) {
        Optional<RecipeHolder<? extends AbstractCookingRecipe>> recipe = original.call(index);
        if (!AsConfig.mixinEnergizedPower() || recipe.isEmpty()) {
            return recipe;
        }
        AbstractPoweredFurnaceBlockEntity self = (AbstractPoweredFurnaceBlockEntity) (Object) this;
        RecipeHolder<? extends AbstractCookingRecipe> holder = recipe.get();
        if (RecipeStageGate.isBlockedForOwner(holder.id(), MachineOwners.get(self), self.getLevel())) {
            return Optional.empty();
        }
        return recipe;
    }
}
