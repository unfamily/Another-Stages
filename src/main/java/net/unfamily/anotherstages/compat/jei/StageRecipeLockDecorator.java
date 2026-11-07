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
 * Red wash over the recipe layout plus a lock badge in a configurable corner.
 *
 * @param <T> JEI recipe type (typically {@link RecipeHolder})
 */
public final class StageRecipeLockDecorator<T> implements IRecipeCategoryDecorator<T> {
    public static final StageRecipeLockDecorator<Object> INSTANCE = new StageRecipeLockDecorator<>();

    private static final int BADGE_SIZE = 16;
    private static final int PAD = 1;

    private StageRecipeLockDecorator() {}

    @Override
    public void draw(
            T recipe,
            IRecipeCategory<T> recipeCategory,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphicsExtractor guiGraphics,
            double mouseX,
            double mouseY) {
        Identifier recipeId = StageRecipeRvHelper.idOf(recipe, recipeCategory);
        RecipeHolder<?> holder = StageRecipeRvHelper.asHolder(recipe, recipeId);
        if (StageRecipeRvHelper.modeFor(recipeId, holder) != StageRecipeRvHelper.Mode.OVERLAY) {
            return;
        }
        int width = Math.max(BADGE_SIZE, recipeCategory.getWidth());
        int height = Math.max(BADGE_SIZE, recipeCategory.getHeight());
        guiGraphics.fill(0, 0, width, height, StageRecipeRvHelper.OVERLAY_TINT);

        StageRecipeRvHelper.Corner corner = StageRecipeRvHelper.overlayCorner(recipeId, holder);
        int[] box = StageRecipeRvHelper.lockBox(width, height, BADGE_SIZE, PAD, corner);
        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
        Identifier texture = StageRecipeRvHelper.lockTexture();
        int texSize = StageRecipeRvHelper.lockTextureSize();
        guiGraphics.blit(
                pipeline, texture, box[0], box[1], 0f, 0f,
                BADGE_SIZE, BADGE_SIZE, texSize, texSize, texSize, texSize);
    }

    @Override
    public void decorateTooltips(
            ITooltipBuilder tooltip,
            T recipe,
            IRecipeCategory<T> recipeCategory,
            IRecipeSlotsView recipeSlotsView,
            double mouseX,
            double mouseY) {
        Identifier recipeId = StageRecipeRvHelper.idOf(recipe, recipeCategory);
        RecipeHolder<?> holder = StageRecipeRvHelper.asHolder(recipe, recipeId);
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

        int width = Math.max(BADGE_SIZE, recipeCategory.getWidth());
        int height = Math.max(BADGE_SIZE, recipeCategory.getHeight());
        StageRecipeRvHelper.Corner corner = StageRecipeRvHelper.overlayCorner(recipeId, holder);
        int[] box = StageRecipeRvHelper.lockBox(width, height, BADGE_SIZE, PAD, corner);
        boolean overLock = mouseX >= box[0] && mouseX < box[0] + BADGE_SIZE
                && mouseY >= box[1] && mouseY < box[1] + BADGE_SIZE;

        if (hideDefault) {
            tooltip.clear();
        } else if (!overLock) {
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
}
