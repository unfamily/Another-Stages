package net.unfamily.anotherstages.client.gui.stagegen;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Screen target for JEI/EMI/REI ghost ingredient drops into the Stage Generator. */
public interface IStageGenGhostTarget {
    @Nullable
    Rect2i getGhostTargetArea();

    /** Accept an item id (namespace:path) into the active matcher list / ghost slot. */
    void acceptGhostItem(ItemStack stack);

    default boolean acceptsGhostItems() {
        return true;
    }
}
