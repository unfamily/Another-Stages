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
 * Slot.x/y are final in 26.x, so slots stay parked off-screen (always {@code isActive})
 * so JEI/EMI/REI keep a normal container. The screen draws/clicks the inv UI only on
 * matcher-entry, using {@link #visualSlotX(int)} / {@link #visualSlotY(int)}.
 * Ghost visual sits in the Shop-style {@code ← [slot] →} calibrator row (not above the inv).
 */
public final class StageGeneratorMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 300;
    public static final int GUI_HEIGHT = 240;

    public static final int GHOST_SLOT = 0;
    public static final int PLAYER_INV_START = 1;
    public static final int PLAYER_INV_COUNT = 36;

    public static final int PLAYER_INV_X = 20;
    public static final int PLAYER_INV_Y = 154;

    public static final int LIST_X = 8;
    public static final int LIST_Y = 28;
    /** Full panel width — actions sit on the bottom bar, not a side column. */
    public static final int LIST_RIGHT = GUI_WIDTH - 8;
    public static final int LIST_WIDTH = LIST_RIGHT - LIST_X;

    /** Matcher calibrator row (matches StageGeneratorScreen ← slot → layout). */
    public static final int MATCHER_ARROW = 12;
    public static final int MATCHER_SLOT = 18;
    public static final int MATCHER_ARROW_GAP = 4;
    public static final int MATCHER_ROW_Y = 34;
    /** FORM_LEFT ({@code LIST_X + 4}) + arrow + gap. */
    public static final int GHOST_X = LIST_X + 4 + MATCHER_ARROW + MATCHER_ARROW_GAP;
    public static final int GHOST_Y = MATCHER_ROW_Y;

    /** Done beside player inventory — matcher-entry only. */
    public static final int SIDE_BTN_X = 188;
    public static final int SIDE_BTN_Y = PLAYER_INV_Y;
    public static final int SIDE_BTN_W = 72;
    public static final int SIDE_BTN_H = 14;

    /** Bottom action bar (New / Done / Save / …) when inventory UI is hidden. */
    public static final int BOTTOM_BTN_H = 14;
    public static final int BOTTOM_BTN_GAP = 4;
    public static final int BOTTOM_PAD = 8;
    public static final int BOTTOM_BAR_Y = GUI_HEIGHT - BOTTOM_PAD - BOTTOM_BTN_H;

    /** Parked coordinates so recipe viewers still see active player slots. */
    private static final int OFFSCREEN = -10000;

    private final SimpleContainer ghost = new SimpleContainer(1);
    /** True when ghost + player inv should be drawn / clickable in the GUI. */
    private boolean slotsUiVisible = true;
    private @Nullable Runnable onGhostChanged;

    public StageGeneratorMenu(int containerId, Inventory playerInv) {
        super(AsMenus.STAGE_GENERATOR.get(), containerId);

        addSlot(new GhostSlot(this, ghost, 0, OFFSCREEN, OFFSCREEN));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, OFFSCREEN, OFFSCREEN));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, OFFSCREEN, OFFSCREEN));
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
        setGhostStack(stack, true);
    }

    /** Sets the calibrator ghost; when {@code notify} is false, skips {@link #onGhostChanged}. */
    public void setGhostStack(ItemStack stack, boolean notify) {
        ghost.setItem(0, stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        broadcastChanges();
        if (notify && onGhostChanged != null) {
            onGhostChanged.run();
        }
    }

    public void clearGhost() {
        setGhostStack(ItemStack.EMPTY, true);
    }

    public void clearGhostQuiet() {
        setGhostStack(ItemStack.EMPTY, false);
    }

    public void setSlotsVisible(boolean visible) {
        this.slotsUiVisible = visible;
    }

    public boolean slotsVisible() {
        return slotsUiVisible;
    }

    /** GUI-local X for drawing/clicking slot {@code index} (ghost=0, then player inv). */
    public static int visualSlotX(int index) {
        if (index == GHOST_SLOT) {
            return GHOST_X;
        }
        int inv = index - PLAYER_INV_START;
        if (inv < 0 || inv >= PLAYER_INV_COUNT) {
            return OFFSCREEN;
        }
        if (inv < 27) {
            return PLAYER_INV_X + (inv % 9) * 18;
        }
        return PLAYER_INV_X + (inv - 27) * 18;
    }

    /** GUI-local Y for drawing/clicking slot {@code index}. */
    public static int visualSlotY(int index) {
        if (index == GHOST_SLOT) {
            return GHOST_Y;
        }
        int inv = index - PLAYER_INV_START;
        if (inv < 0 || inv >= PLAYER_INV_COUNT) {
            return OFFSCREEN;
        }
        if (inv < 27) {
            return PLAYER_INV_Y + (inv / 9) * 18;
        }
        return PLAYER_INV_Y + 58;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (!slotsUiVisible) {
            return;
        }
        if (slotIndex == GHOST_SLOT) {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                setGhostStack(ItemStack.EMPTY);
            } else {
                setGhostStack(carried);
            }
            return;
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!slotsUiVisible) {
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
