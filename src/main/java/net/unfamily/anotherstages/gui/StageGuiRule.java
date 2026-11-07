package net.unfamily.anotherstages.gui;

import net.unfamily.anotherstages.recipe.StageCondition;

import java.util.ArrayList;
import java.util.List;

/** One restriction rule from a stage_gui JSON file. */
public final class StageGuiRule {
    public String stagesLogic = "AND";
    public List<StageCondition> stages = new ArrayList<>();
    /** FQCN of {@link net.minecraft.world.inventory.AbstractContainerMenu}. */
    public List<String> menus = new ArrayList<>();
    /** Registry ids of {@link net.minecraft.world.inventory.MenuType}. */
    public List<String> menuTypes = new ArrayList<>();
    /** Lang key or literal shown when the menu is closed / blocked. */
    public String message = "";
    /** When true (default), close the menu immediately on open if the rule applies. */
    public boolean blockOpen = true;
    /** Absolute menu slot indices that reject clicks. */
    public List<Integer> blockedSlots = new ArrayList<>();
    /** Items (id / #tag / @mod) that cannot be placed in this menu while the rule applies. */
    public List<String> blockedItems = new ArrayList<>();
    /**
     * Soft capacity for non-player container slots; excess stacks are dropped to the player.
     * {@code 0} = disabled.
     */
    public int maxSlots = 0;
}
