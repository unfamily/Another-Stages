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
 * Draws lock overlay or hardcore camouflage on staged items when the rule is active.
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
        if (mode == StageItemRvHelper.Mode.NONE || mode == StageItemRvHelper.Mode.HIDE) {
            return false;
        }
        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
        Identifier texture = StageItemRvHelper.textureFor(mode);
        if (mode == StageItemRvHelper.Mode.HARDCORE) {
            // Full-slot camouflage texture
            guiGraphics.blit(pipeline, texture, xOffset, yOffset, 0f, 0f, 16, 16, 16, 16);
            return true;
        }
        // overlay: light tint + lock/fish badge
        guiGraphics.fill(xOffset, yOffset, xOffset + 16, yOffset + 16, 0x33202020);
        guiGraphics.blit(pipeline, texture, xOffset, yOffset, 0f, 0f, 16, 16, 16, 16);
        return true;
    }
}
