package net.unfamily.anotherstages.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import net.unfamily.anotherstages.item.StageItemRvHelper;

/**
 * Draws the lock overlay badge on staged items in {@code overlay} mode.
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
        if (mode != StageItemRvHelper.Mode.OVERLAY) {
            return false;
        }
        ResourceLocation texture = StageItemRvHelper.textureFor(mode);
        int texSize = StageItemRvHelper.overlayTextureSize();
        guiGraphics.blit(texture, xOffset, yOffset, 0, 0, 16, 16, texSize, texSize);
        return true;
    }
}
