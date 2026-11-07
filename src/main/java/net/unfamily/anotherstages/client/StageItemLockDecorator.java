package net.unfamily.anotherstages.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import net.unfamily.anotherstages.item.StageItemRvHelper;

/**
 * Draws the lock overlay badge on staged items in {@code overlay} mode.
 * Hardcore camouflage is handled by model replacement ({@code ItemModelResolverMixin}).
 */
public final class StageItemLockDecorator implements IItemDecorator {
    public static final StageItemLockDecorator INSTANCE = new StageItemLockDecorator();

    private StageItemLockDecorator() {}

    @Override
    public boolean render(GuiGraphicsExtractor guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        if (stack.isEmpty()) {
            return false;
        }
        StageItemRvHelper.Mode mode = StageItemRvHelper.modeFor(stack.getItem());
        // HARDCORE replaces the baked item model itself (see ItemModelResolverMixin); no GUI overlay.
        if (mode != StageItemRvHelper.Mode.OVERLAY) {
            return false;
        }
        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
        Identifier texture = StageItemRvHelper.textureFor(mode);
        int texSize = StageItemRvHelper.overlayTextureSize();
        // overlay: light tint + lock (24) / fish (32) badge scaled into the 16×16 slot
        guiGraphics.fill(xOffset, yOffset, xOffset + 16, yOffset + 16, 0x33202020);
        guiGraphics.blit(pipeline, texture, xOffset, yOffset, 0f, 0f, 16, 16, texSize, texSize);
        return true;
    }
}


