package net.unfamily.anotherstages.mixin.compat.sophisticated;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingLogic;
import net.unfamily.anotherstages.compat.refinedstorage.RsCraftSubjects;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import java.util.UUID;

/**
 * Sophisticated cooking upgrades (smelting/blasting/smoking + auto): prevent cook start when locked.
 * Subject comes from CookingUpgradeWrapper / AutoCookingUpgradeWrapper tick (player or entity UUID).
 * Clearing {@code cookingRecipe} lets unlock re-check without yanking items.
 */
@Mixin(value = CookingLogic.class, remap = false)
public abstract class CookingLogicMixin<T extends AbstractCookingRecipe> {
    @Shadow(remap = false)
    private RecipeHolder<T> cookingRecipe;

    @Shadow(remap = false)
    private boolean cookingRecipeInitialized;

    @WrapMethod(method = "getCookingRecipe", remap = false)
    private Optional<T> anotherStages$gateCookingRecipe(Operation<Optional<T>> original) {
        Optional<T> recipe = original.call();
        if (!AsConfig.mixinSophisticated() || recipe.isEmpty() || cookingRecipe == null) {
            return recipe;
        }
        UUID owner = RsCraftSubjects.currentUuid();
        Level level = RsCraftSubjects.currentLevel();
        if (RecipeStageGate.isBlockedForOwner(cookingRecipe.id(), owner, level)) {
            this.cookingRecipe = null;
            this.cookingRecipeInitialized = false;
            return Optional.empty();
        }
        return recipe;
    }
}
