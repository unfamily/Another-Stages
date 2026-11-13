package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

/**
 * Create Crushing Wheels: refuse starting a stage-locked crushing recipe.
 */
@Mixin(value = CrushingWheelControllerBlockEntity.class, remap = false)
public abstract class CrushingWheelControllerBlockEntityMixin {
    @WrapMethod(method = "findRecipe", remap = false)
    private Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>> anotherStages$gateFind(
            Operation<Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>>> original) {
        Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>> recipe = original.call();
        if (!AsConfig.mixinCreate() || recipe.isEmpty()) {
            return recipe;
        }
        CrushingWheelControllerBlockEntity self = (CrushingWheelControllerBlockEntity) (Object) this;
        Level level = ((BlockEntity) (Object) self).getLevel();
        RecipeHolder<StandardProcessingRecipe<RecipeWrapper>> holder = recipe.get();
        if (RecipeStageGate.isBlockedForOwner(holder.id(), MachineOwners.get(self), level)) {
            return Optional.empty();
        }
        return recipe;
    }
}
