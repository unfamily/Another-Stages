package net.unfamily.anotherstages.mixin;

import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {
    @Inject(method = "createResult", at = @At("TAIL"), remap = false)
    private void anotherStages$gateSmithing(CallbackInfo ci) {
        ResultContainer resultSlots = ((ItemCombinerMenuResultAccessor) this).anotherStages$getResultSlots();
        RecipeHolder<?> used = resultSlots.getRecipeUsed();
        if (used == null) {
            return;
        }
        var player = ((ItemCombinerMenuAccessor) this).anotherStages$getPlayer();
        if (RecipeStageGate.isBlockedForPlayer(used.id(), player, player.level())) {
            resultSlots.setItem(0, ItemStack.EMPTY);
            resultSlots.setRecipeUsed(null);
        }
    }
}
