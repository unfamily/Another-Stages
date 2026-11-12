package net.unfamily.anotherstages.mixin.compat.sophisticated;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CraftingUpgradeContainer.class, remap = false)
public abstract class CraftingUpgradeContainerMixin {
    @Shadow(remap = false)
    private RecipeHolder<CraftingRecipe> lastRecipe;

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
            lastRecipe = null;
            craftResult.setItem(0, ItemStack.EMPTY);
            craftResult.setRecipeUsed(null);
            if (craftResultSlot != null) {
                craftResultSlot.set(ItemStack.EMPTY);
            }
        }
    }
}
