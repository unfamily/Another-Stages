package net.unfamily.anotherstages.compat.jei;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.unfamily.anotherstages.client.gui.stagegen.IStageGenGhostTarget;
import net.unfamily.anotherstages.client.gui.stagegen.StageGeneratorScreen;

import java.util.ArrayList;
import java.util.List;

/** JEI drag-and-drop into Stage Generator ghost slot. */
public final class StageGenGhostIngredientHandler implements IGhostIngredientHandler<StageGeneratorScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(
            StageGeneratorScreen gui,
            ITypedIngredient<I> ingredient,
            boolean doStart
    ) {
        List<Target<I>> targets = new ArrayList<>();
        if (!(gui instanceof IStageGenGhostTarget ghostTarget) || !ghostTarget.acceptsGhostItems()) {
            return targets;
        }
        Object raw = ingredient.getIngredient();
        if (!(raw instanceof ItemStack stack) || stack.isEmpty()) {
            return targets;
        }
        Rect2i area = ghostTarget.getGhostTargetArea();
        if (area == null) {
            return targets;
        }
        targets.add(new Target<>() {
            @Override
            public Rect2i getArea() {
                Rect2i live = ghostTarget.getGhostTargetArea();
                return live != null ? live : area;
            }

            @Override
            public void accept(I ingredient) {
                if (ingredient instanceof ItemStack item && !item.isEmpty()) {
                    ghostTarget.acceptGhostItem(item);
                }
            }
        });
        return targets;
    }

    @Override
    public void onComplete() {
        // no-op
    }
}
