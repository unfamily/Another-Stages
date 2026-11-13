package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Create Deployer: refuse stage-locked deploying / item-application recipes.
 */
@Mixin(value = DeployerBlockEntity.class, remap = false)
public abstract class DeployerBlockEntityMixin {
    @WrapMethod(
            method = "getRecipe(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/crafting/RecipeHolder;",
            remap = false)
    private RecipeHolder<? extends Recipe<? extends RecipeInput>> anotherStages$gateGetRecipe(
            ItemStack stack,
            Operation<RecipeHolder<? extends Recipe<? extends RecipeInput>>> original) {
        RecipeHolder<? extends Recipe<? extends RecipeInput>> holder = original.call(stack);
        if (!AsConfig.mixinCreate() || holder == null) {
            return holder;
        }
        DeployerBlockEntity self = (DeployerBlockEntity) (Object) this;
        Level level = ((BlockEntity) (Object) self).getLevel();
        if (RecipeStageGate.isBlockedForOwner(holder.id(), MachineOwners.get(self), level)) {
            return null;
        }
        return holder;
    }
}
