package net.unfamily.anotherstages.compat.enderio;

import org.jetbrains.annotations.Nullable;

/**
 * When AS gates EIO {@code findRecipe} to empty, {@code MachineTaskHost} clears
 * {@code isNewTaskAvailable}. Mark the host so {@code findNewTaskIfAble} can call
 * {@code newTaskAvailable()} again after a stage unlock without inventory churn.
 */
public final class EioStageRearm {
    private static final ThreadLocal<Object> HOST = new ThreadLocal<>();

    private EioStageRearm() {}

    public static void mark(@Nullable Object machineTaskHost) {
        if (machineTaskHost != null) {
            HOST.set(machineTaskHost);
        }
    }

    public static boolean consume(@Nullable Object machineTaskHost) {
        Object marked = HOST.get();
        if (marked == null) {
            return false;
        }
        HOST.remove();
        return marked == machineTaskHost;
    }
}
