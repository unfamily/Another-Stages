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
 * Prevents consume when locked by treating the ingredient list as non-empty.
 */
@Mixin(value = TransformLogic.class, remap = false)
public abstract class TransformLogicMixin {
    @WrapOperation(
            method = "tryTransform",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;isEmpty()Z",
                    ordinal = 0))
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
