package net.unfamily.anotherstages.mixin.compat.polymorph;

import com.illusivesoulworks.polymorph.common.capability.AbstractRecipeData;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Filters stage-locked recipes out of Polymorph+ candidate resolution so locked
 * alternatives are not auto-selected or shown in the conflict UI. Also refuses
 * {@code selectRecipe} when the chosen holder is stage-locked.
 */
@Mixin(value = AbstractRecipeData.class, remap = false)
public abstract class AbstractRecipeDataMixin {
    @Shadow
    public abstract Object getOwner();

    /**
     * After Polymorph resolves {@code recipes} from the explicit list or its cache,
     * drop stage-locked holders before selection / UI pair building.
     */
    @ModifyVariable(method = "getRecipe", at = @At(value = "STORE", ordinal = 0), ordinal = 1)
    private <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> anotherStages$filterCandidates(
            List<RecipeHolder<T>> recipes,
            RecipeType<T> type,
            I recipeInput,
            Level level,
            List<RecipeHolder<T>> recipesListIn) {
        if (!AsConfig.mixinPolymorph() || recipes == null || recipes.isEmpty()) {
            return recipes;
        }
        List<RecipeHolder<T>> filtered = new ArrayList<>(recipes.size());
        for (RecipeHolder<T> holder : recipes) {
            if (holder != null && !isBlocked(holder, level)) {
                filtered.add(holder);
            }
        }
        return filtered;
    }

    @WrapMethod(method = "selectRecipe")
    private void anotherStages$gateSelectRecipe(RecipeHolder<?> recipe, Operation<Void> original) {
        if (!AsConfig.mixinPolymorph() || recipe == null) {
            original.call(recipe);
            return;
        }
        Level level = ownerLevel();
        if (isBlocked(recipe, level)) {
            return;
        }
        original.call(recipe);
    }

    private boolean isBlocked(RecipeHolder<?> holder, Level level) {
        Object owner = this.getOwner();
        if (owner instanceof Player player) {
            Level useLevel = level != null ? level : player.level();
            return RecipeStageGate.isBlockedForPlayer(holder.id(), player, useLevel);
        }
        if (owner instanceof BlockEntity be) {
            Level useLevel = level != null ? level : be.getLevel();
            return RecipeStageGate.isBlockedForMachine(holder.id(), be, useLevel);
        }
        return false;
    }

    private Level ownerLevel() {
        Object owner = this.getOwner();
        if (owner instanceof Player player) {
            return player.level();
        }
        if (owner instanceof BlockEntity be) {
            return be.getLevel();
        }
        return null;
    }
}
