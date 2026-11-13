package net.unfamily.anotherstages.mixin.compat.sophisticated;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.Player;
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
 * Prefer {@link RecipeStageGate#isBlockedForPlayer} when a Player is on the subject stack so stage
 * grant unlocks immediately. Clearing {@code cookingRecipe} lets unlock re-check without yanking items.
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
        if (isCookingBlocked(cookingRecipe)) {
            this.cookingRecipe = null;
            this.cookingRecipeInitialized = false;
            return Optional.empty();
        }
        return recipe;
    }

    @WrapMethod(method = "smelt", remap = false)
    private void anotherStages$gateSmelt(T cookingRecipeValue, Operation<Void> original) {
        if (AsConfig.mixinSophisticated() && cookingRecipe != null && isCookingBlocked(cookingRecipe)) {
            this.cookingRecipe = null;
            this.cookingRecipeInitialized = false;
            return;
        }
        original.call(cookingRecipeValue);
    }

    private static boolean isCookingBlocked(RecipeHolder<?> holder) {
        Player player = RsCraftSubjects.currentPlayer();
        Level level = RsCraftSubjects.currentLevel();
        if (player != null) {
            return RecipeStageGate.isBlockedForPlayer(holder.id(), player, level != null ? level : player.level());
        }
        UUID owner = RsCraftSubjects.currentUuid();
        return RecipeStageGate.isBlockedForOwner(holder.id(), owner, level);
    }
}
