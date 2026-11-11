package net.unfamily.anotherstages.mixin;

import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blocks recipe-book auto-place when the recipe is stage-gated for the player. */
@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin {
    @Inject(
            method = "recipeClicked(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/crafting/RecipeHolder;Z)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private <I extends RecipeInput, R extends Recipe<I>> void anotherStages$gatePlace(
            ServerPlayer player,
            RecipeHolder<R> recipe,
            boolean useMaxItems,
            CallbackInfo ci) {
        if (recipe == null || player == null) {
            return;
        }
        if (AsConfig.mixinRecipeBookPlace()
                && RecipeStageGate.isBlockedForPlayer(recipe.id(), player, player.level())) {
            ci.cancel();
        }
    }
}
