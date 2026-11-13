package net.unfamily.anotherstages.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.compat.create.CreateRecipeIds;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.owner.MachineOwners;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Create basin operators (Mixer, Press-in-basin): refuse matching / applying stage-locked recipes.
 */
@Mixin(value = BasinOperatingBlockEntity.class, remap = false)
public abstract class BasinOperatingBlockEntityMixin {
    @Shadow(remap = false)
    protected Recipe<?> currentRecipe;

    @WrapMethod(method = "matchBasinRecipe", remap = false)
    private <I extends RecipeInput> boolean anotherStages$gateMatch(
            Recipe<I> recipe, Operation<Boolean> original) {
        boolean match = original.call(recipe);
        if (!match || !AsConfig.mixinCreate() || recipe == null) {
            return match;
        }
        BasinOperatingBlockEntity self = (BasinOperatingBlockEntity) (Object) this;
        Level level = ((BlockEntity) (Object) self).getLevel();
        ResourceLocation id = CreateRecipeIds.idOf(recipe, level);
        if (id == null) {
            return false;
        }
        return !RecipeStageGate.isBlockedForOwner(id, MachineOwners.get(self), level);
    }

    @WrapMethod(method = "applyBasinRecipe", remap = false)
    private void anotherStages$gateApply(Operation<Void> original) {
        if (!AsConfig.mixinCreate()) {
            original.call();
            return;
        }
        BasinOperatingBlockEntity self = (BasinOperatingBlockEntity) (Object) this;
        Recipe<?> recipe = this.currentRecipe;
        if (recipe != null) {
            Level level = ((BlockEntity) (Object) self).getLevel();
            ResourceLocation id = CreateRecipeIds.idOf(recipe, level);
            if (id != null
                    && RecipeStageGate.isBlockedForOwner(id, MachineOwners.get(self), level)) {
                this.currentRecipe = null;
                return;
            }
        }
        original.call();
    }
}
