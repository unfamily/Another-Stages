package net.unfamily.anotherstages.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * Vanilla furnace family (no FastFurnace): prevent stage-locked cooking from starting.
 * When FastFurnace is present this mixin is skipped; see FastFurnaceRecipeMixin.
 * <p>
 * Passes the furnace BE so {@link net.unfamily.anotherstages.owner.MachineOwners} applies when
 * claimed. No owner is still normal — world-stage rules apply; player-scoped rules stay fail-closed.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    @WrapOperation(
            method = {"serverTick", "getTotalCookTime"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/RecipeManager$CachedCheck;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/server/level/ServerLevel;)Ljava/util/Optional;"))
    private static <I extends RecipeInput, T extends net.minecraft.world.item.crafting.Recipe<I>>
            Optional<RecipeHolder<T>> anotherStages$gateFurnaceRecipe(
                    RecipeManager.CachedCheck<I, T> check,
                    I input,
                    ServerLevel level,
                    Operation<Optional<RecipeHolder<T>>> original,
                    @Local(argsOnly = true) AbstractFurnaceBlockEntity furnace) {
        Optional<RecipeHolder<T>> recipe = original.call(check, input, level);
        if (!AsConfig.mixinFurnace() || recipe.isEmpty()) {
            return recipe;
        }
        RecipeHolder<T> holder = recipe.get();
        boolean blocked = RecipeStageGate.isBlockedForMachine(holder.id(), furnace, level);
        if (AsConfig.loggingEnabled()) {
            AnotherStages.LOGGER.info(
                    "Furnace gate recipe={} blocked={}", holder.id().identifier(), blocked);
        }
        return blocked ? Optional.empty() : recipe;
    }
}
