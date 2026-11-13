package net.unfamily.anotherstages.recipe;

import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.client.StageAprilFools;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Client helpers for JEI presentation of staged recipes. */
public final class StageRecipeRvHelper {
    public enum Mode {
        NONE,
        OVERLAY,
        HIDE
    }

    public enum Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    /** Dedicated 16×16 JEI badges under {@code textures/stage_locking/}. */
    public static final Identifier RECIPE_LOCKED =
            Identifier.fromNamespaceAndPath(AnotherStages.MOD_ID, "textures/stage_locking/recipe_locked.png");
    public static final Identifier RECIPE_LOCKED_FISH =
            Identifier.fromNamespaceAndPath(AnotherStages.MOD_ID, "textures/stage_locking/recipe_locked_fish.png");
    public static final int RECIPE_LOCKED_SIZE = 16;
    public static final int RECIPE_LOCKED_FISH_SIZE = 16;

    /** Slight reddish wash over the whole recipe layout. */
    public static final int OVERLAY_TINT = 0x44AA3030;

    private StageRecipeRvHelper() {}

    public static Mode modeFor(@Nullable Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        return modeFor(recipeId, holder, null);
    }

    public static Mode modeFor(
            @Nullable Identifier recipeId, @Nullable RecipeHolder<?> holder, @Nullable Object recipeView) {
        if (recipeId == null || !StageRecipeHandler.hasLoadedRules()) {
            return Mode.NONE;
        }
        List<StageRecipeRule> active = StageRecipeHandler.findActiveRulesClient(recipeId, holder, recipeView);
        if (active.isEmpty()) {
            return Mode.NONE;
        }
        // Highest priority wins (list already sorted desc); exclusive presentation.
        StageRecipeRule primary = active.getFirst();
        return "hide".equals(StageRecipeHandler.rvMode(primary)) ? Mode.HIDE : Mode.OVERLAY;
    }

    public static Identifier lockTexture() {
        return StageAprilFools.isAprilFools() ? RECIPE_LOCKED_FISH : RECIPE_LOCKED;
    }

    /** Native pixel size of {@link #lockTexture()} (16×16 lock / fish badges). */
    public static int lockTextureSize() {
        return StageAprilFools.isAprilFools() ? RECIPE_LOCKED_FISH_SIZE : RECIPE_LOCKED_SIZE;
    }

    public static List<StageRecipeRule> activeOverlayRules(
            @Nullable Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        return activeOverlayRules(recipeId, holder, null);
    }

    public static List<StageRecipeRule> activeOverlayRules(
            @Nullable Identifier recipeId, @Nullable RecipeHolder<?> holder, @Nullable Object recipeView) {
        List<StageRecipeRule> out = new ArrayList<>();
        if (recipeId == null) {
            return out;
        }
        for (StageRecipeRule rule : StageRecipeHandler.findActiveRulesClient(recipeId, holder, recipeView)) {
            if ("overlay".equals(StageRecipeHandler.rvMode(rule))) {
                out.add(rule);
            }
        }
        return out;
    }

    /** Corner from the first active overlay rule (default top_right). */
    public static Corner overlayCorner(@Nullable Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        return overlayCorner(recipeId, holder, null);
    }

    public static Corner overlayCorner(
            @Nullable Identifier recipeId, @Nullable RecipeHolder<?> holder, @Nullable Object recipeView) {
        for (StageRecipeRule rule : activeOverlayRules(recipeId, holder, recipeView)) {
            return toCorner(StageRecipeHandler.overlayCorner(rule));
        }
        return Corner.TOP_RIGHT;
    }

    public static Corner toCorner(String normalized) {
        return switch (StageRecipeHandler.normalizeOverlayCorner(normalized)) {
            case "top_left" -> Corner.TOP_LEFT;
            case "bottom_left" -> Corner.BOTTOM_LEFT;
            case "bottom_right" -> Corner.BOTTOM_RIGHT;
            default -> Corner.TOP_RIGHT;
        };
    }

    /**
     * Badge box [x, y] for a lock icon inside the category layout.
     */
    public static int[] lockBox(int width, int height, int lockSize, int pad, Corner corner) {
        int w = Math.max(lockSize, width);
        int h = Math.max(lockSize, height);
        int x = switch (corner) {
            case TOP_LEFT, BOTTOM_LEFT -> pad;
            case TOP_RIGHT, BOTTOM_RIGHT -> Math.max(0, w - lockSize - pad);
        };
        int y = switch (corner) {
            case TOP_LEFT, TOP_RIGHT -> pad;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> Math.max(0, h - lockSize - pad);
        };
        return new int[]{x, y};
    }

    public static List<String> stageHints(List<StageRecipeRule> rules) {
        Set<String> stages = new LinkedHashSet<>();
        for (StageRecipeRule rule : rules) {
            if (rule.stages == null) {
                continue;
            }
            for (StageCondition c : rule.stages) {
                if (c.stage != null && !c.stage.isBlank()) {
                    stages.add(c.stage.trim());
                }
            }
        }
        return new ArrayList<>(stages);
    }

    public static boolean hasCustomTooltips(StageRecipeRule rule) {
        return rule != null && (
                (rule.tooltips != null && !rule.tooltips.isEmpty())
                        || (rule.componentTooltips != null && !rule.componentTooltips.isEmpty()));
    }

    public static Component resolveTooltip(String raw) {
        if (raw.indexOf(' ') < 0 && raw.indexOf('.') >= 0) {
            return Component.translatable(raw);
        }
        return Component.literal(raw);
    }

    @Nullable
    public static Identifier idOf(Object recipe) {
        return idOf(recipe, null);
    }

    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Identifier idOf(Object recipe, @Nullable IRecipeCategory<?> category) {
        if (recipe instanceof RecipeHolder<?> holder) {
            return holder.id().identifier();
        }
        if (category != null) {
            try {
                // JEI 26+: getIdentifier; older / some mods still override getRegistryName.
                Identifier id = ((IRecipeCategory) category).getIdentifier(recipe);
                if (id != null) {
                    return id;
                }
            } catch (RuntimeException | AbstractMethodError | NoSuchMethodError ignored) {
                // fall through to getRegistryName
            }
            try {
                Identifier id = ((IRecipeCategory) category).getRegistryName(recipe);
                if (id != null) {
                    return id;
                }
            } catch (RuntimeException | AbstractMethodError | NoSuchMethodError ignored) {
                // fall through
            }
        }
        return null;
    }

    @Nullable
    public static RecipeHolder<?> asHolder(Object recipe) {
        return asHolder(recipe, null);
    }

    @Nullable
    public static RecipeHolder<?> asHolder(Object recipe, @Nullable Identifier recipeId) {
        if (recipe instanceof RecipeHolder<?> holder) {
            // FTBIC adapted cooking keeps vanilla smelting ids on MachineRecipe wrappers —
            // resolve the vanilla cooking holder so recipe_types / I/O match JEI locks.
            RecipeHolder<?> vanilla = resolveVanillaCookingIfAdapted(holder);
            if (vanilla != null) {
                return vanilla;
            }
            // Ender IO Alloy: keep remapped enderio:smelting/... holder as-is for RV (dual-id in handler).
            return holder;
        }
        if (recipeId == null) {
            return null;
        }
        // Integrated server (SP/dev): resolve RecipeHolder so recipe_types / RecipeIoProbe work for wrappers.
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSingleplayerServer() != null) {
                ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, recipeId);
                return mc.getSingleplayerServer().getRecipeManager().byKey(key).orElse(null);
            }
        } catch (RuntimeException ignored) {
            // dedicated client / early init
        }
        return null;
    }

    /**
     * When JEI shows an FTBIC {@code MachineRecipe} adapted from vanilla cooking, the holder id
     * is the vanilla smelting/blasting/smoking id — prefer that holder for RV matching.
     */
    @Nullable
    private static RecipeHolder<?> resolveVanillaCookingIfAdapted(RecipeHolder<?> holder) {
        if (holder == null) {
            return null;
        }
        Recipe<?> value = holder.value();
        String className = value.getClass().getName();
        if (!className.contains("ftbic") || !className.contains("MachineRecipe")) {
            return null;
        }
        Identifier id = holder.id().identifier();
        if (id == null || !"minecraft".equals(id.getNamespace())) {
            return null;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSingleplayerServer() != null) {
                ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
                return mc.getSingleplayerServer().getRecipeManager().byKey(key).orElse(null);
            }
        } catch (RuntimeException ignored) {
            // dedicated client / early init — id-only matching still works via BY_RECIPE_ID
        }
        return null;
    }
}
