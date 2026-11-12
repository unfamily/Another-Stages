package net.unfamily.anotherstages.mixin.compat.ae2;

import appeng.recipes.transform.TransformLogic;
import appeng.recipes.transform.TransformRecipe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.compat.ae2.Ae2CraftGate;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Skip in-world AE2 transform recipes that are stage-locked (world stages; no network owner).
 * Prevents consume when locked by treating the missing-ingredient list as non-empty.
 * <p>
 * AE2 26 {@code tryTransform} calls {@code List.isEmpty()} twice: ordinal 0 is
 * {@code recipe.ingredients.isEmpty()} (early continue); ordinal 1 is
 * {@code missingIngredients.isEmpty()} (consume gate). Gate must wrap ordinal 1.
 */
@Mixin(value = TransformLogic.class, remap = false)
public abstract class TransformLogicMixin {
    @WrapOperation(
            method = "tryTransform",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;isEmpty()Z",
                    ordinal = 1))
    private static boolean anotherStages$gateTransformConsume(
            List<?> missingIngredients,
            Operation<Boolean> original,
            @Local(argsOnly = true) ItemEntity entity,
            @Local RecipeHolder<TransformRecipe> holder) {
        boolean empty = original.call(missingIngredients);
        if (!empty || !AsConfig.mixinAe2() || holder == null || entity == null) {
            return empty;
        }
        // No machine owner for in-world transform — world stages still apply (ownerUuid null).
        if (Ae2CraftGate.isRecipeBlocked(holder.id(), null, entity.level())) {
            return false;
        }
        return true;
    }
}
