package net.unfamily.anotherstages.compat.mekanism;

import mekanism.common.content.qio.IQIOCraftingWindowHolder;
import mekanism.common.content.qio.QIOCraftingWindow;
import mekanism.common.recipe.MekanismRecipeType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Shared QIO stage gate for {@code canViewRecipe} / {@code mayPickup} / {@code performCraft}.
 * Lives outside the mixin package (mixin packages forbid peer class loads).
 */
public final class QioCraftGate {
    private static final Field JOURNAL_FIELD = optionalField("lastRecipeJournal");
    private static final Field LAST_RECIPE_FIELD = optionalField("lastRecipe");
    private static final Field HOLDER_FIELD = optionalField("holder");

    private QioCraftGate() {}

    public static boolean isBlocked(@Nullable QIOCraftingWindow window, @Nullable Player player) {
        if (!AsConfig.mixinMekanism() || player == null) {
            return false;
        }
        RecipeHolder<CraftingRecipe> recipe = resolveRecipe(window, player);
        if (recipe == null) {
            return true;
        }
        Level level = levelOf(window, player);
        return RecipeStageGate.isBlockedForPlayer(recipe.id(), player, level);
    }

    @Nullable
    private static RecipeHolder<CraftingRecipe> resolveRecipe(
            @Nullable QIOCraftingWindow window, Player player) {
        RecipeHolder<CraftingRecipe> recipe = peekLastRecipe(window);
        if (recipe != null) {
            return recipe;
        }
        recipe = peekJournal(window);
        if (recipe != null) {
            return recipe;
        }
        Level level = levelOf(window, player);
        if (level != null && window != null) {
            try {
                CraftingInput.Positioned positioned = asCraftingInput(window);
                if (positioned != null) {
                    CraftingInput input = positioned.input();
                    if (input != null && !input.isEmpty()) {
                        recipe = MekanismRecipeType
                                .getRecipeFor(RecipeType.CRAFTING, input, level)
                                .orElse(null);
                        if (recipe != null) {
                            return recipe;
                        }
                        if (level instanceof ServerLevel serverLevel) {
                            recipe = serverLevel.getServer()
                                    .getRecipeManager()
                                    .getRecipeFor(RecipeType.CRAFTING, input, serverLevel)
                                    .orElse(null);
                            if (recipe != null) {
                                return recipe;
                            }
                        }
                    }
                }
            } catch (RuntimeException ignored) {
                // fall through
            }
        }
        return QioCraftSubjects.current();
    }

    @Nullable
    private static CraftingInput.Positioned asCraftingInput(QIOCraftingWindow window) {
        try {
            // Public on Mek 26.x; private on some 1.21.1 builds — invoke reflectively.
            var method = QIOCraftingWindow.class.getDeclaredMethod("asCraftingInput");
            method.setAccessible(true);
            return (CraftingInput.Positioned) method.invoke(window);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Nullable
    private static Level levelOf(@Nullable QIOCraftingWindow window, Player player) {
        if (window != null && HOLDER_FIELD != null) {
            try {
                Object holder = HOLDER_FIELD.get(window);
                if (holder instanceof IQIOCraftingWindowHolder h && h.getLevel() != null) {
                    return h.getLevel();
                }
            } catch (ReflectiveOperationException ignored) {
                // fall through
            }
        }
        return player.level();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static RecipeHolder<CraftingRecipe> peekLastRecipe(@Nullable QIOCraftingWindow window) {
        if (window == null || LAST_RECIPE_FIELD == null) {
            return null;
        }
        try {
            return (RecipeHolder<CraftingRecipe>) LAST_RECIPE_FIELD.get(window);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static RecipeHolder<CraftingRecipe> peekJournal(@Nullable QIOCraftingWindow window) {
        if (window == null || JOURNAL_FIELD == null) {
            return null;
        }
        try {
            Object journal = JOURNAL_FIELD.get(window);
            if (journal == null) {
                return null;
            }
            Field recipeField = journal.getClass().getDeclaredField("recipe");
            recipeField.setAccessible(true);
            return (RecipeHolder<CraftingRecipe>) recipeField.get(journal);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Nullable
    private static Field optionalField(String name) {
        try {
            Field field = QIOCraftingWindow.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
