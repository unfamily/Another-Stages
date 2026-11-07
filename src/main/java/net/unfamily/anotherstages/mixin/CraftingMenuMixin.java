package net.unfamily.anotherstages.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Blocks crafting result when the matched recipe is stage-gated (3x3 table and inventory 2x2).
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Redirect(
            method = "slotChangedCraftingGrid",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/RecipeCraftingHolder;setRecipeUsed(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/crafting/RecipeHolder;)Z",
                    remap = false))
    private static boolean anotherStages$gateCraftingResult(
            RecipeCraftingHolder result, ServerPlayer player, RecipeHolder<?> holder) {
        if (AsConfig.mixinCrafting()
                && RecipeStageGate.isBlockedForPlayer(holder.id(), player, player.level())) {
            return false;
        }
        return result.setRecipeUsed(player, holder);
    }
}
