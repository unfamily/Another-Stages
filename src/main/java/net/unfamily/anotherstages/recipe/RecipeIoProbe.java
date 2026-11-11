package net.unfamily.anotherstages.recipe;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;
import net.unfamily.anotherstages.util.StageIdMatchers;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Kube-like I/O probe for stage matchers: dig into recipe ingredients/results when
 * {@link Recipe#display()} / {@link PlacementInfo} are empty or not placeable.
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
            if (ingredient.items().anyMatch(h -> StageIdMatchers.matchesItem(h.value(), inputs))) {
                return true;
            }
        }
        for (RecipeDisplay display : safeDisplay(recipe)) {
            if (display instanceof StonecutterRecipeDisplay stone && slotMentions(stone.input(), inputs)) {
                return true;
            }
            if (display instanceof FurnaceRecipeDisplay furnace && slotMentions(furnace.ingredient(), inputs)) {
                return true;
            }
        }
        return false;
    }

    private static List<Item> collectOutputItems(Recipe<?> recipe) {
        List<Item> out = new ArrayList<>();
        for (RecipeDisplay display : safeDisplay(recipe)) {
            collectItemsFromSlot(display.result(), out);
        }
        if (!out.isEmpty()) {
            return out;
        }
        if (recipe instanceof SingleItemRecipe single) {
            ItemStack stack = single.assemble(new SingleRecipeInput(ItemStack.EMPTY));
            if (!stack.isEmpty()) {
                out.add(stack.getItem());
            }
        }
        ItemStack reflected = reflectNoArgStack(recipe, "getResultItem", "getResult", "resultStack", "output");
        if (reflected != null && !reflected.isEmpty()) {
            out.add(reflected.getItem());
        }
        return out;
    }

    private static List<Ingredient> collectInputIngredients(Recipe<?> recipe) {
        List<Ingredient> out = new ArrayList<>();
        PlacementInfo info = recipe.placementInfo();
        if (info != null && !info.isImpossibleToPlace()) {
            out.addAll(info.ingredients());
        }
        if (!out.isEmpty()) {
            return out;
        }
        if (recipe instanceof SingleItemRecipe single) {
            Ingredient input = single.input();
            if (input != null && !input.isEmpty()) {
                out.add(input);
            }
        }
        List<Ingredient> reflected = reflectInputIngredients(recipe);
        out.addAll(reflected);
        return out;
    }

    private static List<RecipeDisplay> safeDisplay(Recipe<?> recipe) {
        try {
            List<RecipeDisplay> displays = recipe.display();
            return displays != null ? displays : List.of();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static void collectItemsFromSlot(SlotDisplay slot, List<Item> out) {
        if (slot instanceof SlotDisplay.ItemStackSlotDisplay stackDisplay) {
            out.add(stackDisplay.stack().item().value());
            return;
        }
        if (slot instanceof SlotDisplay.ItemSlotDisplay itemDisplay) {
            out.add(itemDisplay.item().value());
            return;
        }
        if (slot instanceof SlotDisplay.TagSlotDisplay tagDisplay) {
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagDisplay.tag())) {
                out.add(holder.value());
            }
            return;
        }
        if (slot instanceof SlotDisplay.Composite composite) {
            for (SlotDisplay child : composite.contents()) {
                collectItemsFromSlot(child, out);
            }
        }
    }

    private static boolean slotMentions(SlotDisplay slot, List<String> matchers) {
        if (slot instanceof SlotDisplay.ItemStackSlotDisplay stackDisplay) {
            return StageIdMatchers.matchesItem(stackDisplay.stack().item().value(), matchers);
        }
        if (slot instanceof SlotDisplay.ItemSlotDisplay itemDisplay) {
            return StageIdMatchers.matchesItem(itemDisplay.item().value(), matchers);
        }
        if (slot instanceof SlotDisplay.TagSlotDisplay tagDisplay) {
            TagKey<Item> tag = tagDisplay.tag();
            String tagId = tag.location().toString();
            if (StageIdMatchers.idInList("#" + tagId, matchers) || StageIdMatchers.idInList(tagId, matchers)) {
                return true;
            }
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                if (StageIdMatchers.matchesItem(holder.value(), matchers)) {
                    return true;
                }
            }
            return false;
        }
        if (slot instanceof SlotDisplay.Composite composite) {
            for (SlotDisplay child : composite.contents()) {
                if (slotMentions(child, matchers)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private static ItemStack reflectNoArgStack(Recipe<?> recipe, String... methodNames) {
        for (String name : methodNames) {
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
        for (String name : List.of("getInputs", "getIngredients", "inputs", "ingredients")) {
            try {
                Method method = recipe.getClass().getMethod(name);
                Object value = method.invoke(recipe);
                List<Ingredient> parsed = coerceIngredients(value);
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            } catch (ReflectiveOperationException ignored) {
                // try next
            }
        }
        return List.of();
    }

    private static List<Ingredient> coerceIngredients(@Nullable Object value) {
        List<Ingredient> out = new ArrayList<>();
        if (value instanceof Ingredient ingredient) {
            out.add(ingredient);
            return out;
        }
        if (value instanceof Ingredient[] array) {
            for (Ingredient ingredient : array) {
                if (ingredient != null && !ingredient.isEmpty()) {
                    out.add(ingredient);
                }
            }
            return out;
        }
        if (value instanceof Collection<?> collection) {
            for (Object element : collection) {
                if (element instanceof Ingredient ingredient) {
                    if (!ingredient.isEmpty()) {
                        out.add(ingredient);
                    }
                    continue;
                }
                if (element instanceof ItemStack stack && !stack.isEmpty()) {
                    out.add(Ingredient.of(stack.getItem()));
                    continue;
                }
                if (element instanceof Item item) {
                    out.add(Ingredient.of(item));
                    continue;
                }
                // CountedIngredient-style: ingredient() / getIngredient() / stack()
                Ingredient nested = reflectNestedIngredient(element);
                if (nested != null && !nested.isEmpty()) {
                    out.add(nested);
                }
            }
        }
        return out;
    }

    @Nullable
    private static Ingredient reflectNestedIngredient(@Nullable Object element) {
        if (element == null) {
            return null;
        }
        for (String name : List.of("ingredient", "getIngredient", "asIngredient")) {
            try {
                Method method = element.getClass().getMethod(name);
                Object value = method.invoke(element);
                if (value instanceof Ingredient ingredient) {
                    return ingredient;
                }
            } catch (ReflectiveOperationException ignored) {
                // try next
            }
        }
        ItemStack stack = reflectNoArgStackOn(element, "stack", "getStack", "itemStack");
        if (stack != null && !stack.isEmpty()) {
            return Ingredient.of(stack.getItem());
        }
        try {
            Method itemMethod = element.getClass().getMethod("item");
            Object item = itemMethod.invoke(element);
            if (item instanceof Item i) {
                return Ingredient.of(i);
            }
            if (item instanceof Holder<?> holder && holder.value() instanceof Item i) {
                return Ingredient.of(i);
            }
        } catch (ReflectiveOperationException ignored) {
            // no-op
        }
        return null;
    }

    @Nullable
    private static ItemStack reflectNoArgStackOn(Object target, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method method = target.getClass().getMethod(name);
                Object value = method.invoke(target);
                if (value instanceof ItemStack stack) {
                    return stack;
                }
            } catch (ReflectiveOperationException ignored) {
                // try next
            }
        }
        return null;
    }
}
