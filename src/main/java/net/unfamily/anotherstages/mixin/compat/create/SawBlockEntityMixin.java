package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * Create Mechanical Saw: refuse applying a stage-locked cutting recipe.
 */
@Mixin(value = SawBlockEntity.class, remap = false)
public abstract class SawBlockEntityMixin {
    @Shadow(remap = false)
    private int recipeIndex;

    @Shadow(remap = false)
    private List<RecipeHolder<? extends Recipe<?>>> getRecipes() {
        throw new AssertionError();
    }

    @WrapMethod(method = "applyRecipe", remap = false)
    private void anotherStages$gateApplyRecipe(Operation<Void> original) {
        if (!AsConfig.mixinCreate()) {
            original.call();
            return;
        }
        SawBlockEntity self = (SawBlockEntity) (Object) this;
        Level level = ((BlockEntity) (Object) self).getLevel();
        List<RecipeHolder<? extends Recipe<?>>> recipes = getRecipes();
        if (recipes == null || recipes.isEmpty()) {
            original.call();
            return;
        }
        int index = this.recipeIndex;
        if (index < 0 || index >= recipes.size()) {
            index = 0;
        }
        RecipeHolder<? extends Recipe<?>> holder = recipes.get(index);
        ResourceLocation id = holder.id();
        if (RecipeStageGate.isBlockedForOwner(id, MachineOwners.get(self), level)) {
            return;
        }
        original.call();
    }
}
