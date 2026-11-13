package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

/**
 * Create Mechanical Press (belt / world pressing): refuse stage-locked PRESSING recipes.
 * Basin mode is gated via {@link BasinOperatingBlockEntityMixin}.
 */
@Mixin(value = MechanicalPressBlockEntity.class, remap = false)
public abstract class MechanicalPressBlockEntityMixin {
    @WrapMethod(
            method = "getRecipe(Lnet/minecraft/world/item/ItemStack;)Ljava/util/Optional;",
            remap = false)
    private Optional<RecipeHolder<PressingRecipe>> anotherStages$gateGetRecipe(
            ItemStack stack, Operation<Optional<RecipeHolder<PressingRecipe>>> original) {
        Optional<RecipeHolder<PressingRecipe>> recipe = original.call(stack);
        if (!AsConfig.mixinCreate() || recipe.isEmpty()) {
            return recipe;
        }
        MechanicalPressBlockEntity self = (MechanicalPressBlockEntity) (Object) this;
        Level level = ((BlockEntity) (Object) self).getLevel();
        RecipeHolder<PressingRecipe> holder = recipe.get();
        if (RecipeStageGate.isBlockedForOwner(holder.id(), MachineOwners.get(self), level)) {
            return Optional.empty();
        }
        return recipe;
    }
}
