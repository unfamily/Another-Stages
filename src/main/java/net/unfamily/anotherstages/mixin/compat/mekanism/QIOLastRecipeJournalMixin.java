package net.unfamily.anotherstages.mixin.compat.mekanism;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Track the QIO window's current crafting recipe for stage checks (nested journal class).
 */
@Pseudo
@Mixin(targets = "mekanism.common.content.qio.QIOCraftingWindow$LastRecipeJournal", remap = false)
public abstract class QIOLastRecipeJournalMixin {
    @Unique
    private static final ThreadLocal<RecipeHolder<CraftingRecipe>> anotherStages$CURRENT = new ThreadLocal<>();

    @Inject(method = "updateRecipe", at = @At("TAIL"), remap = false)
    private void anotherStages$captureRecipe(
            RecipeHolder<CraftingRecipe> recipe, TransactionContext context, CallbackInfo ci) {
        anotherStages$CURRENT.set(recipe);
    }

    @Unique
    static RecipeHolder<CraftingRecipe> anotherStages$currentRecipe() {
        return anotherStages$CURRENT.get();
    }

    @Unique
    static void anotherStages$clearRecipe() {
        anotherStages$CURRENT.remove();
    }
}
