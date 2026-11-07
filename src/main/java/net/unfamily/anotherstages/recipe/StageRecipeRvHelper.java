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

    public static final Identifier LOCK_ARROW =
            Identifier.fromNamespaceAndPath(AnotherStages.MOD_ID, "textures/stage_locked.png");
    public static final Identifier FISH_ARROW =
            Identifier.fromNamespaceAndPath(AnotherStages.MOD_ID, "textures/stage_fish.png");

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
        return StageAprilFools.isAprilFools() ? FISH_ARROW : LOCK_ARROW;
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
