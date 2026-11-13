package net.unfamily.anotherstages.mixin.compat.mekanism;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "mekanism.common.content.qio.QIOCraftingWindow$LastRecipeJournal", remap = false)
public interface QIOLastRecipeAccessor {
    @Accessor(value = "recipe", remap = false)
    RecipeHolder<CraftingRecipe> anotherStages$getRecipe();
}
