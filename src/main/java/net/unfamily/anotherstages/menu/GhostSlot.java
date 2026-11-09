package net.unfamily.anotherstages.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Display / filter slot: holding an item sets a copy without consuming the carried stack
 * (handled by {@link StageGeneratorMenu#clicked}).
 */
public final class GhostSlot extends Slot {
    private final StageGeneratorMenu menu;

    public GhostSlot(StageGeneratorMenu menu, Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.menu = menu;
    }

    @Override
    public boolean isActive() {
        return menu.slotsVisible();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return menu.slotsVisible();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean mayPickup(Player player) {
        return true;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        // Ghost clear is local; do not give the item to the player.
    }
}
