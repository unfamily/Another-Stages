package net.unfamily.anotherstages.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import net.unfamily.anotherstages.item.StageItemRvHelper;

/**
 * Draws the lock overlay badge on staged items in {@code overlay} mode.
 * Hardcore camouflage is handled by model replacement ({@code ItemRendererMixin}).
 */
public final class StageItemLockDecorator implements IItemDecorator {
    public static final StageItemLockDecorator INSTANCE = new StageItemLockDecorator();

    private StageItemLockDecorator() {}

    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        if (stack.isEmpty()) {
            return false;
        }
        StageItemRvHelper.Mode mode = StageItemRvHelper.modeFor(stack.getItem());
        // HARDCORE replaces the baked item model itself (see ItemRendererMixin); no GUI overlay.
        if (mode != StageItemRvHelper.Mode.OVERLAY) {
            return false;
        }
        ResourceLocation texture = StageItemRvHelper.textureFor(mode);
        int texSize = StageItemRvHelper.overlayTextureSize();
        // Draw above the item (decorator runs with depth test on; item already wrote depth).
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0f, 0f, 200f);
        // Scale the full 24/32 badge into the 16×16 slot (do not crop the top-left 16×16).
        guiGraphics.blit(texture, xOffset, yOffset, 16, 16, 0f, 0f, texSize, texSize, texSize, texSize);
        pose.popPose();
        return true;
    }
}
