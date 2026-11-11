package net.unfamily.anotherstages.recipe;

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
import net.unfamily.anotherstages.util.StageIdMatchers;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Kube-like I/O probe for stage matchers on NeoForge 1.21.1 (no Recipe#display / PlacementInfo).
 */
public final class RecipeIoProbe {
    private RecipeIoProbe() {}

    public static boolean outputsMatch(Recipe<?> recipe, List<String> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return true;
        }
        for (Item item : collectOutputItems(recipe)) {
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

    private static List<Item> collectOutputItems(Recipe<?> recipe) {
        List<Item> out = new ArrayList<>();
        ItemStack result = reflectResult(recipe);
        if (result != null && !result.isEmpty()) {
            out.add(result.getItem());
            return out;
        }
        // Last resort: registry id from recipe class name is useless; keep empty.
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
            out.add(cooking.getIngredients().get(0));
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
    private static ItemStack reflectResult(Recipe<?> recipe) {
        // Prefer getResultItem(RegistryAccess) when present.
        try {
            Method method = recipe.getClass().getMethod("getResultItem", net.minecraft.core.RegistryAccess.class);
            Object value = method.invoke(recipe, net.minecraft.core.RegistryAccess.EMPTY);
            if (value instanceof ItemStack stack) {
                return stack;
            }
        } catch (ReflectiveOperationException ignored) {
            // try next
        }
        for (String name : new String[]{"getResultItem", "getResult", "resultStack", "output", "assemble"}) {
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
        // CraftingRecipe / cooking often need RegistryAccess — try BuiltInRegistries wrapper via server later.
        try {
            Method method = Recipe.class.getMethod("getResultItem", net.minecraft.core.RegistryAccess.class);
            Object value = method.invoke(recipe, BuiltInRegistries.REGISTRY);
            if (value instanceof ItemStack stack) {
                return stack;
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // ignore
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
