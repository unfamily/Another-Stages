package net.unfamily.anotherstages.mixin.compat.sophisticated;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Sophisticated crafting upgrade: clear locked results and refuse select / setRecipeUsed / transfer.
 */
@Mixin(value = CraftingUpgradeContainer.class, remap = false)
public abstract class CraftingUpgradeContainerMixin {
    @Shadow(remap = false)
    private RecipeHolder<CraftingRecipe> lastRecipe;

    @Shadow(remap = false)
    private ResultSlot craftingResultSlot;

    @WrapMethod(method = "updateCraftingResult", remap = false)
    private void anotherStages$gateSophCraft(
            Level level,
            Player player,
            CraftingContainer craftMatrix,
            ResultContainer craftResult,
            ResultSlot craftResultSlot,
            Operation<Void> original) {
        original.call(level, player, craftMatrix, craftResult, craftResultSlot);
        if (!AsConfig.mixinSophisticated() || lastRecipe == null || player == null) {
            return;
        }
        if (RecipeStageGate.isBlockedForPlayer(lastRecipe.id(), player, level)) {
            clearCraftResult(craftResult, craftResultSlot);
        }
    }

    @WrapMethod(method = "selectCraftingResult", remap = false)
    private void anotherStages$gateSelectCraftingResult(int index, Operation<Void> original) {
        original.call(index);
        if (!AsConfig.mixinSophisticated() || lastRecipe == null) {
            return;
        }
        Player player = ((CraftingUpgradeContainer) (Object) this).getPlayer();
        Level level = player != null ? player.level() : null;
        if (player != null && RecipeStageGate.isBlockedForPlayer(lastRecipe.id(), player, level)) {
            clearCraftResult(null, craftingResultSlot);
            lastRecipe = null;
        }
    }

    @WrapMethod(method = "setRecipeUsed", remap = false)
    private void anotherStages$gateSetRecipeUsed(ResourceKey<Recipe<?>> recipeKey, Operation<Void> original) {
        if (AsConfig.mixinSophisticated() && recipeKey != null) {
            Player player = ((CraftingUpgradeContainer) (Object) this).getPlayer();
            Level level = player != null ? player.level() : null;
            if (player != null && RecipeStageGate.isBlockedForPlayer(recipeKey, player, level)) {
                return;
            }
        }
        original.call(recipeKey);
        if (!AsConfig.mixinSophisticated() || lastRecipe == null) {
            return;
        }
        Player player = ((CraftingUpgradeContainer) (Object) this).getPlayer();
        Level level = player != null ? player.level() : null;
        if (player != null && RecipeStageGate.isBlockedForPlayer(lastRecipe.id(), player, level)) {
            clearCraftResult(null, craftingResultSlot);
            lastRecipe = null;
        }
    }

    @WrapMethod(method = "getSlotStackToTransfer", remap = false)
    private ItemStack anotherStages$gateTransfer(Slot slot, Operation<ItemStack> original) {
        ItemStack stack = original.call(slot);
        if (!AsConfig.mixinSophisticated() || stack.isEmpty() || lastRecipe == null) {
            return stack;
        }
        if (slot == craftingResultSlot || slot instanceof ResultSlot) {
            Player player = ((CraftingUpgradeContainer) (Object) this).getPlayer();
            Level level = player != null ? player.level() : null;
            if (player != null && RecipeStageGate.isBlockedForPlayer(lastRecipe.id(), player, level)) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    private void clearCraftResult(ResultContainer craftResult, ResultSlot craftResultSlot) {
        lastRecipe = null;
        if (craftResult != null) {
            craftResult.setItem(0, ItemStack.EMPTY);
            craftResult.setRecipeUsed(null);
        }
        if (craftResultSlot != null) {
            craftResultSlot.set(ItemStack.EMPTY);
        } else if (craftingResultSlot != null) {
            craftingResultSlot.set(ItemStack.EMPTY);
        }
    }
}
