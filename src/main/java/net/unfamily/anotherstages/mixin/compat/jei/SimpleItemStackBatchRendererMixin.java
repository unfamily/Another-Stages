package net.unfamily.anotherstages.mixin.compat.jei;

import mezz.jei.api.ingredients.rendering.BatchRenderElement;
import mezz.jei.library.render.ItemStackRenderer;
import mezz.jei.library.render.batch.SimpleItemStackBatchRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * JEI's batch item renderer skips {@code itemDecorations}, so NeoForge {@code IItemDecorator}
 * (stage lock badge) never runs on the ingredient list. Replicate the single-path behavior.
 */
@Mixin(value = SimpleItemStackBatchRenderer.class, remap = false)
public abstract class SimpleItemStackBatchRendererMixin {
    @Inject(method = "renderBatch", at = @At("HEAD"), cancellable = true, remap = false)
    private void anotherStages$renderWithDecorations(
            GuiGraphicsExtractor guiGraphics,
            ItemStackRenderer itemStackRenderer,
            List<BatchRenderElement<ItemStack>> elements,
            CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        for (BatchRenderElement<ItemStack> element : elements) {
            ItemStack stack = element.ingredient();
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            int x = element.x();
            int y = element.y();
            guiGraphics.fakeItem(stack, x, y);
            Font font = itemStackRenderer.getFontRenderer(minecraft, stack);
            guiGraphics.itemDecorations(font, stack, x, y);
        }
        ci.cancel();
    }
}
