package net.unfamily.anotherstages.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.util.StageIdMatchers;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Kube-like I/O probe for stage matchers on NeoForge 1.21.1.
 * Result lookup uses {@link Recipe#getResultItem(HolderLookup.Provider)} (not RegistryAccess).
 */
public final class RecipeIoProbe {
    private RecipeIoProbe() {}

    public static boolean outputsMatch(Recipe<?> recipe, List<String> outputs) {
        return outputsMatch(recipe, outputs, null);
    }

    public static boolean outputsMatch(Recipe<?> recipe, List<String> outputs, @Nullable Level level) {
        if (outputs == null || outputs.isEmpty()) {
            return true;
        }
        for (Item item : collectOutputItems(recipe, lookup(level))) {
            if (StageIdMatchers.matchesItem(item, outputs)) {
                return true;
            }
        }
        return false;
    }

    public static boolean inputsMatch(Recipe<?> recipe, List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return true;
        }
        for (Ingredient ingredient : collectInputIngredients(recipe)) {
            if (ingredient.isEmpty()) {
                continue;
            }
            for (ItemStack stack : ingredient.getItems()) {
                if (!stack.isEmpty() && StageIdMatchers.matchesItem(stack.getItem(), inputs)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static HolderLookup.Provider lookup(@Nullable Level level) {
        if (level != null) {
            return level.registryAccess();
        }
        return net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    private static List<Item> collectOutputItems(Recipe<?> recipe, HolderLookup.Provider registries) {
        List<Item> out = new ArrayList<>();
        try {
            ItemStack result = recipe.getResultItem(registries);
            if (result != null && !result.isEmpty()) {
                out.add(result.getItem());
                return out;
            }
        } catch (RuntimeException ignored) {
            // fall through to reflection
        }
        ItemStack reflected = reflectResult(recipe, registries);
        if (reflected != null && !reflected.isEmpty()) {
            out.add(reflected.getItem());
        }
        return out;
    }

    private static List<Ingredient> collectInputIngredients(Recipe<?> recipe) {
        List<Ingredient> out = new ArrayList<>();
        if (recipe instanceof CraftingRecipe crafting) {
            NonNullList<Ingredient> ingredients = crafting.getIngredients();
            if (ingredients != null) {
                out.addAll(ingredients);
            }
            return out;
        }
        if (recipe instanceof AbstractCookingRecipe cooking) {
            NonNullList<Ingredient> ingredients = cooking.getIngredients();
            if (ingredients != null && !ingredients.isEmpty()) {
                out.add(ingredients.get(0));
            }
            return out;
        }
        if (recipe instanceof StonecutterRecipe || recipe instanceof SingleItemRecipe) {
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            if (ingredients != null) {
                out.addAll(ingredients);
            }
            return out;
        }
        NonNullList<Ingredient> generic = recipe.getIngredients();
        if (generic != null && !generic.isEmpty()) {
            out.addAll(generic);
            return out;
        }
        out.addAll(reflectInputIngredients(recipe));
        return out;
    }

    @Nullable
    private static ItemStack reflectResult(Recipe<?> recipe, HolderLookup.Provider registries) {
        try {
            Method method = recipe.getClass().getMethod("getResultItem", HolderLookup.Provider.class);
            Object value = method.invoke(recipe, registries);
            if (value instanceof ItemStack stack) {
                return stack;
            }
        } catch (ReflectiveOperationException ignored) {
            // try next
        }
        for (String name : new String[]{"getResultItem", "getResult", "resultStack", "output"}) {
            try {
                Method method = recipe.getClass().getMethod(name);
                Object value = method.invoke(recipe);
                if (value instanceof ItemStack stack) {
                    return stack;
                }
            } catch (ReflectiveOperationException ignored) {
                // try next
            }
        }
        return null;
    }

    private static List<Ingredient> reflectInputIngredients(Recipe<?> recipe) {
        List<Ingredient> out = new ArrayList<>();
        for (String name : new String[]{"getIngredients", "ingredients", "input"}) {
            try {
                Method method = recipe.getClass().getMethod(name);
                Object value = method.invoke(recipe);
                if (value instanceof NonNullList<?> list) {
                    for (Object element : list) {
                        if (element instanceof Ingredient ingredient) {
                            out.add(ingredient);
                        }
                    }
                    return out;
                }
                if (value instanceof Ingredient ingredient) {
                    out.add(ingredient);
                    return out;
                }
                if (value instanceof List<?> list) {
                    for (Object element : list) {
                        if (element instanceof Ingredient ingredient) {
                            out.add(ingredient);
                        }
                    }
                    return out;
                }
            } catch (ReflectiveOperationException ignored) {
                // try next
            }
        }
        return out;
    }
}
