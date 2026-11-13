package net.unfamily.anotherstages.mixin.compat.mekanism;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.unfamily.anotherstages.compat.mekanism.QioCraftSubjects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Push QIO journal recipe into {@link QioCraftSubjects} (outside mixin package).
 */
@Pseudo
@Mixin(targets = "mekanism.common.content.qio.QIOCraftingWindow$LastRecipeJournal", remap = false)
public abstract class QIOLastRecipeJournalMixin {
    @Inject(method = "updateRecipe", at = @At("TAIL"), remap = false)
    private void anotherStages$captureRecipe(
            RecipeHolder<CraftingRecipe> recipe, TransactionContext context, CallbackInfo ci) {
        if (recipe == null) {
            QioCraftSubjects.clear();
        } else {
            QioCraftSubjects.set(recipe);
        }
    }
}
