package net.unfamily.anotherstages.mixin;

import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Blocks recipe-book auto-place when the recipe is stage-gated for the player. */
@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin {
    @Inject(method = "placeRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private static <I extends net.minecraft.world.item.crafting.RecipeInput, R extends Recipe<I>> void anotherStages$gatePlace(
            ServerPlaceRecipe.CraftingMenuAccess<R> menu,
            int gridWidth,
            int gridHeight,
            List<Slot> inputGridSlots,
            List<Slot> slotsToClear,
            Inventory inventory,
            RecipeHolder<R> recipe,
            boolean useMaxItems,
            boolean allowDroppingItemsToClear,
            CallbackInfoReturnable<RecipeBookMenu.PostPlaceAction> cir) {
        if (AsConfig.mixinRecipeBookPlace()
                && inventory.player instanceof ServerPlayer player
                && RecipeStageGate.isBlockedForPlayer(recipe.id(), player, player.level())) {
            cir.setReturnValue(RecipeBookMenu.PostPlaceAction.NOTHING);
        }
    }
}
