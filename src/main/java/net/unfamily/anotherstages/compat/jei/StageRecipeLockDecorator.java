package net.unfamily.anotherstages.compat.jei;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.category.extensions.IRecipeCategoryDecorator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.anotherstages.recipe.StageRecipeRvHelper;
import net.unfamily.anotherstages.recipe.StageRecipeRule;

import java.util.List;

/**
 * Draws a lock on the recipe arrow/process area and appends stage lock tooltips.
 *
 * @param <T> JEI recipe type (typically {@link RecipeHolder})
 */
public final class StageRecipeLockDecorator<T> implements IRecipeCategoryDecorator<T> {
    public static final StageRecipeLockDecorator<Object> INSTANCE = new StageRecipeLockDecorator<>();

    private static final int LOCK_SIZE = 16;

    private StageRecipeLockDecorator() {}

    @Override
    public void draw(
            T recipe,
            IRecipeCategory<T> recipeCategory,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphicsExtractor guiGraphics,
            double mouseX,
            double mouseY) {
        Identifier recipeId = StageRecipeRvHelper.idOf(recipe);
        RecipeHolder<?> holder = StageRecipeRvHelper.asHolder(recipe);
        if (StageRecipeRvHelper.modeFor(recipeId, holder) != StageRecipeRvHelper.Mode.OVERLAY) {
            return;
        }
        int[] box = lockBox(recipeCategory);
        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
        Identifier texture = StageRecipeRvHelper.lockTexture();
        guiGraphics.blit(pipeline, texture, box[0], box[1], 0f, 0f, LOCK_SIZE, LOCK_SIZE, LOCK_SIZE, LOCK_SIZE);
    }

    @Override
    public void decorateTooltips(
            ITooltipBuilder tooltip,
            T recipe,
            IRecipeCategory<T> recipeCategory,
            IRecipeSlotsView recipeSlotsView,
            double mouseX,
            double mouseY) {
        Identifier recipeId = StageRecipeRvHelper.idOf(recipe);
        RecipeHolder<?> holder = StageRecipeRvHelper.asHolder(recipe);
        List<StageRecipeRule> overlayRules = StageRecipeRvHelper.activeOverlayRules(recipeId, holder);
        if (overlayRules.isEmpty()) {
            return;
        }

        boolean hideDefault = false;
        for (StageRecipeRule rule : overlayRules) {
            if (rule.hideDefaultTooltips) {
                hideDefault = true;
                break;
            }
        }

        int[] box = lockBox(recipeCategory);
        boolean overLock = mouseX >= box[0] && mouseX < box[0] + LOCK_SIZE
                && mouseY >= box[1] && mouseY < box[1] + LOCK_SIZE;

        if (hideDefault) {
            tooltip.clear();
        } else if (!overLock) {
            // Only force stage tips when hovering the lock (unless clearing defaults)
            boolean anyCustom = false;
            for (StageRecipeRule rule : overlayRules) {
                if (StageRecipeRvHelper.hasCustomTooltips(rule)) {
                    anyCustom = true;
                    break;
                }
            }
            if (!anyCustom) {
                return;
            }
        }

        boolean addedCustom = false;
        for (StageRecipeRule rule : overlayRules) {
            if (rule.tooltips != null) {
                for (String tip : rule.tooltips) {
                    if (tip == null || tip.isBlank()) {
                        continue;
                    }
                    tooltip.add(StageRecipeRvHelper.resolveTooltip(tip.trim()));
                    addedCustom = true;
                }
            }
            if (rule.componentTooltips != null) {
                for (Component tip : rule.componentTooltips) {
                    if (tip != null) {
                        tooltip.add(tip);
                        addedCustom = true;
                    }
                }
            }
        }
        if (!addedCustom && (overLock || hideDefault)) {
            List<String> stages = StageRecipeRvHelper.stageHints(overlayRules);
            if (stages.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.another_stages.recipe_locked"));
            } else {
                tooltip.add(Component.translatable(
                        "tooltip.another_stages.recipe_locked_stage", String.join(", ", stages)));
            }
        }
    }

    /** Lock drawn near the process arrow (center-right of the category layout). */
    private static int[] lockBox(IRecipeCategory<?> category) {
        int width = Math.max(LOCK_SIZE, category.getWidth());
        int height = Math.max(LOCK_SIZE, category.getHeight());
        // Crafting arrow sits near x=61; approximate center-right for other categories
        int x = Math.max(0, Math.min(width - LOCK_SIZE, (width * 2) / 3 - LOCK_SIZE / 2));
        int y = Math.max(0, (height - LOCK_SIZE) / 2);
        return new int[]{x, y};
    }
}
