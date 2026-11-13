package net.unfamily.anotherstages.mixin.compat.sophisticated;

import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterRecipeContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = BlockConverterRecipeContainer.class, remap = false)
public interface BlockConverterRecipeContainerAccessor {
    @Accessor(value = "recipes", remap = false)
    @SuppressWarnings("rawtypes")
    List anotherStages$getRecipes();

    @Accessor(value = "selectedRecipe", remap = false)
    DataSlot anotherStages$getSelectedRecipeSlot();

    @Accessor(value = "level", remap = false)
    Level anotherStages$getLevel();
}
