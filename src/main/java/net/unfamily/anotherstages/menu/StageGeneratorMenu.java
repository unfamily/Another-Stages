package net.unfamily.anotherstages.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Stage Generator container (ShopEdit footprint 300×240): ghost + player inventory.
 */
public final class StageGeneratorMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 300;
    public static final int GUI_HEIGHT = 240;

    public static final int GHOST_SLOT = 0;
    public static final int PLAYER_INV_START = 1;
    public static final int PLAYER_INV_COUNT = 36;

    /** Aligned with Utils ShopEdit inventory column. */
    public static final int PLAYER_INV_X = 20;
    public static final int PLAYER_INV_Y = 154;
    public static final int GHOST_X = PLAYER_INV_X;
    public static final int GHOST_Y = PLAYER_INV_Y - 22;

    /** Side action buttons (ShopEdit SIDE_BTN_*). */
    public static final int SIDE_BTN_X = 188;
    public static final int SIDE_BTN_Y = PLAYER_INV_Y;
    public static final int SIDE_BTN_W = 72;
    public static final int SIDE_BTN_H = 14;
    public static final int SIDE_BTN_GAP = 4;

    public static final int LIST_X = 8;
    public static final int LIST_Y = 28;
    public static final int LIST_RIGHT = SIDE_BTN_X + SIDE_BTN_W;
    public static final int LIST_WIDTH = LIST_RIGHT - LIST_X;

    private final SimpleContainer ghost = new SimpleContainer(1);
    private boolean slotsVisible = true;
    private @Nullable Runnable onGhostChanged;

    public StageGeneratorMenu(int containerId, Inventory playerInv) {
        super(AsMenus.STAGE_GENERATOR.get(), containerId);

        addSlot(new GhostSlot(this, ghost, 0, GHOST_X, GHOST_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new VisibilitySlot(
                        playerInv,
                        col + row * 9 + 9,
                        PLAYER_INV_X + col * 18,
                        PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new VisibilitySlot(playerInv, col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + 58));
        }
    }

    private final class VisibilitySlot extends Slot {
        private VisibilitySlot(Inventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean isActive() {
            return slotsVisible;
        }
    }

    public static StageGeneratorMenu createClient(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new StageGeneratorMenu(id, inv);
    }

    public void setOnGhostChanged(@Nullable Runnable onGhostChanged) {
        this.onGhostChanged = onGhostChanged;
    }

    public ItemStack getGhostStack() {
        return ghost.getItem(0);
    }

    public void setGhostStack(ItemStack stack) {
        ghost.setItem(0, stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        broadcastChanges();
        if (onGhostChanged != null) {
            onGhostChanged.run();
        }
    }

    public void clearGhost() {
        setGhostStack(ItemStack.EMPTY);
    }

    public void setSlotsVisible(boolean visible) {
        this.slotsVisible = visible;
    }

    public boolean slotsVisible() {
        return slotsVisible;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex == GHOST_SLOT && slotsVisible) {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                setGhostStack(ItemStack.EMPTY);
            } else {
                setGhostStack(carried);
            }
            return;
        }
        if (!slotsVisible && slotIndex >= 0) {
            return;
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!slotsVisible) {
            return ItemStack.EMPTY;
        }
        if (index >= PLAYER_INV_START && index < PLAYER_INV_START + PLAYER_INV_COUNT) {
            Slot slot = slots.get(index);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                setGhostStack(stack);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
