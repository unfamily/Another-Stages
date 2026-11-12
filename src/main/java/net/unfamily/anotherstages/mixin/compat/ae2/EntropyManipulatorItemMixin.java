package net.unfamily.anotherstages.mixin.compat.ae2;

import appeng.items.tools.powered.EntropyManipulatorItem;
import appeng.recipes.AERecipeTypes;
import appeng.recipes.entropy.EntropyMode;
import appeng.recipes.entropy.EntropyRecipe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gate Entropy Manipulator in-world effects.
 * Subject is the player holding the tool — player + team stages resolve via that UUID;
 * world stages always apply (global). Not world-only: unlike Transform there is always an actor.
 * Only recipe application is refused when stage-locked.
 */
@Mixin(value = EntropyManipulatorItem.class, remap = false)
public abstract class EntropyManipulatorItemMixin {
    @WrapOperation(
            method = "tryApplyEffect",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/items/tools/powered/EntropyManipulatorItem;findRecipe(Lnet/minecraft/world/level/Level;Lappeng/recipes/entropy/EntropyMode;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)Lappeng/recipes/entropy/EntropyRecipe;",
                    remap = false))
    private EntropyRecipe anotherStages$gateEntropyFind(
            Level level,
            EntropyMode mode,
            BlockState state,
            FluidState fluid,
            Operation<EntropyRecipe> original,
            @Local(argsOnly = true) Player player) {
        EntropyRecipe recipe = original.call(level, mode, state, fluid);
        if (!AsConfig.mixinAe2() || recipe == null || level == null) {
            return recipe;
        }
        for (RecipeHolder<EntropyRecipe> holder : level.getRecipeManager().getAllRecipesFor(AERecipeTypes.ENTROPY)) {
            if (holder.value() == recipe
                    && RecipeStageGate.isBlockedForPlayer(holder.id(), player, level)) {
                return null;
            }
        }
        return recipe;
    }
}
