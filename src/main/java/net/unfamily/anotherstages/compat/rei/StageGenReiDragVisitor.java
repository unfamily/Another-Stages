package net.unfamily.anotherstages.compat.rei;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.unfamily.anotherstages.client.gui.stagegen.StageGeneratorScreen;

import java.util.stream.Stream;

/** REI drag visitor for Stage Generator ghost slot. */
public final class StageGenReiDragVisitor implements DraggableStackVisitor<StageGeneratorScreen> {
    @Override
    public <R extends Screen> boolean isHandingScreen(R screen) {
        return screen instanceof StageGeneratorScreen;
    }

    @Override
    public DraggedAcceptorResult acceptDraggedStack(
            DraggingContext<StageGeneratorScreen> context,
            DraggableStack stack
    ) {
        StageGeneratorScreen screen = context.getScreen();
        if (!screen.acceptsGhostItems()) {
            return DraggedAcceptorResult.PASS;
        }
        Rect2i area = screen.getGhostTargetArea();
        if (area == null) {
            return DraggedAcceptorResult.PASS;
        }
        var mouse = context.getCurrentPosition();
        if (mouse == null
                || mouse.x < area.getX()
                || mouse.y < area.getY()
                || mouse.x >= area.getX() + area.getWidth()
                || mouse.y >= area.getY() + area.getHeight()) {
            return DraggedAcceptorResult.PASS;
        }
        if (stack.getStack().getType() != VanillaEntryTypes.ITEM) {
            return DraggedAcceptorResult.PASS;
        }
        ItemStack item = stack.getStack().castValue();
        if (item == null || item.isEmpty()) {
            return DraggedAcceptorResult.PASS;
        }
        screen.acceptGhostItem(item);
        return DraggedAcceptorResult.CONSUMED;
    }

    @Override
    public Stream<BoundsProvider> getDraggableAcceptingBounds(
            DraggingContext<StageGeneratorScreen> context,
            DraggableStack stack
    ) {
        StageGeneratorScreen screen = context.getScreen();
        Rect2i area = screen.getGhostTargetArea();
        if (area == null || !screen.acceptsGhostItems()) {
            return Stream.empty();
        }
        return Stream.of(BoundsProvider.ofRectangle(
                new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight())));
    }
}
