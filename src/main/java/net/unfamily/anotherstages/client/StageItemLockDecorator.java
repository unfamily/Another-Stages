package net.unfamily.anotherstages.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.IItemDecorator;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.item.StageItemRvHelper;

/**
 * Draws a lock overlay (or barrier for {@code partial}) on staged items when the rule is active.
 */
public final class StageItemLockDecorator implements IItemDecorator {
    public static final StageItemLockDecorator INSTANCE = new StageItemLockDecorator();
    private static final Identifier LOCK = Identifier.fromNamespaceAndPath(AnotherStages.MOD_ID, "textures/gui/lock.png");

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
        if (mode == StageItemRvHelper.Mode.PARTIAL) {
            guiGraphics.fill(xOffset, yOffset, xOffset + 16, yOffset + 16, 0xAA202020);
            guiGraphics.item(new ItemStack(Items.BARRIER), xOffset, yOffset);
            return true;
        }
        // overlay: tint + lock icon in the corner
        guiGraphics.fill(xOffset, yOffset, xOffset + 16, yOffset + 16, 0x44202020);
        guiGraphics.blit(pipeline, LOCK, xOffset, yOffset, 0f, 0f, 16, 16, 16, 16);
        return true;
    }
}
