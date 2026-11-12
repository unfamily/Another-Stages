package net.unfamily.anotherstages.mixin.compat.ae2;

import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.slot.CraftingTermSlot;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.compat.ae2.Ae2CraftGate;
import net.unfamily.anotherstages.compat.ae2.Ae2NetworkOwners;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Clear AE2 Crafting Terminal result when the matched crafting recipe is stage-locked.
 */
@Mixin(value = CraftingTermMenu.class, remap = false)
public abstract class CraftingTermMenuMixin {
    @Shadow(remap = false)
    private RecipeHolder<CraftingRecipe> currentRecipe;

    @Shadow(remap = false)
    @Final
    private CraftingTermSlot outputSlot;

    @WrapMethod(method = "updateCurrentRecipeAndOutput", remap = false)
    private void anotherStages$gateCraftingTerm(
            ServerLevel level, boolean forceUpdate, Operation<Void> original) {
        original.call(level, forceUpdate);
        if (!AsConfig.mixinAe2() || currentRecipe == null) {
            return;
        }
        CraftingTermMenu self = (CraftingTermMenu) (Object) this;
        Player player = self.getPlayer();
        if (Ae2CraftGate.isRecipeBlocked(
                currentRecipe.id(), Ae2NetworkOwners.fromPlayer(player), level)) {
            currentRecipe = null;
            outputSlot.set(ItemStack.EMPTY);
        }
    }
}
