package net.unfamily.anotherstages.recipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

    /** Same art as item overlay: 24×24 lock / 32×32 fish. */
    public static final Identifier RECIPE_LOCKED =
            Identifier.fromNamespaceAndPath(AnotherStages.MOD_ID, "textures/stage_locked.png");
    public static final Identifier RECIPE_LOCKED_FISH =
            Identifier.fromNamespaceAndPath(AnotherStages.MOD_ID, "textures/stage_fish.png");
    public static final int RECIPE_LOCKED_SIZE = 24;
    public static final int RECIPE_LOCKED_FISH_SIZE = 32;

    /** Slight reddish wash over the whole recipe layout. */
    public static final int OVERLAY_TINT = 0x44AA3030;

    private StageRecipeRvHelper() {}

    public static Mode modeFor(@Nullable Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        if (recipeId == null || !StageRecipeHandler.hasLoadedRules()) {
            return Mode.NONE;
        }
        List<StageRecipeRule> active = StageRecipeHandler.findActiveRulesClient(recipeId, holder);
        Mode strongest = Mode.NONE;
        for (StageRecipeRule rule : active) {
            Mode mode = "hide".equals(StageRecipeHandler.rvMode(rule)) ? Mode.HIDE : Mode.OVERLAY;
            if (rank(mode) > rank(strongest)) {
                strongest = mode;
            }
        }
        return strongest;
    }

    private static int rank(Mode mode) {
        return switch (mode) {
            case HIDE -> 2;
            case OVERLAY -> 1;
            case NONE -> 0;
        };
    }

    public static Identifier lockTexture() {
        return StageAprilFools.isAprilFools() ? RECIPE_LOCKED_FISH : RECIPE_LOCKED;
    }

    /** Native pixel size of {@link #lockTexture()} (24 lock / 32 fish). */
    public static int lockTextureSize() {
        return StageAprilFools.isAprilFools() ? RECIPE_LOCKED_FISH_SIZE : RECIPE_LOCKED_SIZE;
    }

    public static List<StageRecipeRule> activeOverlayRules(
            @Nullable Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        List<StageRecipeRule> out = new ArrayList<>();
        if (recipeId == null) {
            return out;
        }
        for (StageRecipeRule rule : StageRecipeHandler.findActiveRulesClient(recipeId, holder)) {
            if ("overlay".equals(StageRecipeHandler.rvMode(rule))) {
                out.add(rule);
            }
        }
        return out;
    }

    /** Corner from the first active overlay rule (default top_right). */
    public static Corner overlayCorner(@Nullable Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        for (StageRecipeRule rule : activeOverlayRules(recipeId, holder)) {
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
     * Badge box [x, y] for a 16×16 icon inside the category layout.
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
        if (recipe instanceof RecipeHolder<?> holder) {
            return holder.id().identifier();
        }
        return null;
    }

    @Nullable
    public static RecipeHolder<?> asHolder(Object recipe) {
        if (recipe instanceof RecipeHolder<?> holder) {
            return holder;
        }
        return null;
    }
}
