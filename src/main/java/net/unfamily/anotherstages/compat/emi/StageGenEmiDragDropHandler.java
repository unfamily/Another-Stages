package net.unfamily.anotherstages.compat.emi;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.unfamily.anotherstages.client.gui.stagegen.StageGeneratorScreen;

/** EMI drag-and-drop into Stage Generator ghost slot. */
public final class StageGenEmiDragDropHandler implements EmiDragDropHandler<StageGeneratorScreen> {
    @Override
    public boolean dropStack(StageGeneratorScreen screen, EmiIngredient stack, int x, int y) {
        if (!screen.acceptsGhostItems()) {
            return false;
        }
        Rect2i area = screen.getGhostTargetArea();
        if (area == null || x < area.getX() || y < area.getY()
                || x >= area.getX() + area.getWidth()
                || y >= area.getY() + area.getHeight()) {
            return false;
        }
        for (EmiStack emiStack : stack.getEmiStacks()) {
            ItemStack item = emiStack.getItemStack();
            if (item != null && !item.isEmpty()) {
                screen.acceptGhostItem(item);
                return true;
            }
        }
        return false;
    }
}
