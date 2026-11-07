package net.unfamily.anotherstages.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * Drops stage-blocked cooking recipes so the furnace cannot progress them.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    @Redirect(
            method = "serverTick",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/RecipeManager$CachedCheck;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/server/level/ServerLevel;)Ljava/util/Optional;",
                    remap = false))
    private static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> anotherStages$gateFurnaceRecipe(
            RecipeManager.CachedCheck<I, T> check,
            I input,
            ServerLevel level,
            ServerLevel levelArg,
            BlockPos pos,
            BlockState state,
            AbstractFurnaceBlockEntity entity) {
        Optional<RecipeHolder<T>> recipe = check.getRecipeFor(input, level);
        if (AsConfig.mixinFurnace()
                && recipe.isPresent()
                && RecipeStageGate.isBlockedForMachine(recipe.get().id(), entity, level)) {
            return Optional.empty();
        }
        return recipe;
    }
}
