package net.unfamily.anotherstages.kubejs;

import net.unfamily.anotherstages.block.StageBlockHandler;
import net.unfamily.anotherstages.gui.StageGuiHandler;
import net.unfamily.anotherstages.item.StageItemHandler;
import net.unfamily.anotherstages.recipe.StageRecipeHandler;

import java.util.ArrayList;
import java.util.List;

/** Collects fluent builders during script eval; commits after scripts load. */
public final class PendingScriptRestrictions {
    private static final List<Runnable> PENDING = new ArrayList<>();

    private PendingScriptRestrictions() {}

    public static void clear() {
        PENDING.clear();
        StageItemHandler.clearScriptRestrictions();
        StageRecipeHandler.clearScriptRules();
        StageBlockHandler.clearScriptRules();
        StageGuiHandler.clearScriptRules();
    }

    public static void enqueue(Runnable commit) {
        PENDING.add(commit);
    }

    public static void commitAll() {
        for (Runnable r : List.copyOf(PENDING)) {
            r.run();
        }
        PENDING.clear();
    }
}
