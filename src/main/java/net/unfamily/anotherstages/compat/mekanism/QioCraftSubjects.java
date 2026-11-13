package net.unfamily.anotherstages.compat.mekanism;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-local QIO crafting recipe for stage checks.
 * Lives outside the mixin package — mixin packages forbid direct class loads.
 */
public final class QioCraftSubjects {
    private static final ThreadLocal<RecipeHolder<CraftingRecipe>> CURRENT = new ThreadLocal<>();

    private QioCraftSubjects() {}

    public static void set(@Nullable RecipeHolder<CraftingRecipe> recipe) {
        if (recipe == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(recipe);
        }
    }

    @Nullable
    public static RecipeHolder<CraftingRecipe> current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
