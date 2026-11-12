package net.unfamily.anotherstages.mixin.compat.toms;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tom.storagemod.menu.CraftingTerminalMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CraftingTerminalMenu.class, remap = false)
public abstract class CraftingTerminalMenuMixin {
    @Shadow(remap = false)
    private ResultContainer craftResult;

    @Shadow(remap = false)
    protected Inventory pinv;

    @WrapMethod(method = "onCraftMatrixChanged", remap = false)
    private void anotherStages$gateTomCraft(Operation<Void> original) {
        original.call();
        if (!AsConfig.mixinTomsStorage() || craftResult == null) {
            return;
        }
        RecipeHolder<?> used = craftResult.getRecipeUsed();
        Player player = pinv != null ? pinv.player : null;
        if (used != null
                && player != null
                && RecipeStageGate.isBlockedForPlayer(used.id(), player, player.level())) {
            craftResult.setItem(0, ItemStack.EMPTY);
            craftResult.setRecipeUsed(null);
        }
    }
}
