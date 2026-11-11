package net.unfamily.anotherstages.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.unfamily.anotherstages.util.StageIdMatchers;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Soft I/O probe for JEI custom recipe wrappers (no hard dep on Utils).
 * Covers records with {@code inputs()}/{@code outputs()} {@link ItemStack} lists
 * such as Ancient Tablet / Factory JEI recipes.
 */
public final class JeiWrapperIoProbe {
    private static final Map<String, Identifier> CLASS_TO_RECIPE_TYPE = Map.of(
            "AncientTabletJeiRecipe", Identifier.fromNamespaceAndPath("iska_utils", "ancient_tab"),
            "FactoryJeiRecipe", Identifier.fromNamespaceAndPath("iska_utils", "factory"));

    private JeiWrapperIoProbe() {}

    @Nullable
    public static Identifier inferredRecipeType(@Nullable Object recipeView) {
        if (recipeView == null) {
            return null;
        }
        return CLASS_TO_RECIPE_TYPE.get(recipeView.getClass().getSimpleName());
    }

    public static boolean outputsMatch(@Nullable Object recipeView, List<String> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return true;
        }
        for (ItemStack stack : stacksFrom(recipeView, "outputs")) {
            if (!stack.isEmpty() && StageIdMatchers.matchesItem(stack.getItem(), outputs)) {
                return true;
            }
        }
        return false;
    }

    public static boolean inputsMatch(@Nullable Object recipeView, List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return true;
        }
        for (ItemStack stack : stacksFrom(recipeView, "inputs")) {
            if (!stack.isEmpty() && StageIdMatchers.matchesItem(stack.getItem(), inputs)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<ItemStack> stacksFrom(@Nullable Object recipeView, String accessor) {
        if (recipeView == null) {
            return Collections.emptyList();
        }
        try {
            Method m = recipeView.getClass().getMethod(accessor);
            Object value = m.invoke(recipeView);
            if (value instanceof List<?> list) {
                return (List<ItemStack>) list;
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // not a known wrapper shape
        }
        return Collections.emptyList();
    }
}
