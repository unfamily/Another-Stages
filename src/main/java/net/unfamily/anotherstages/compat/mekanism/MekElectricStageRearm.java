package net.unfamily.anotherstages.compat.mekanism;

/**
 * When AS gates electric-machine {@code getRecipe} to null, Mek's
 * {@code RecipeCacheLookupMonitor} calls {@code setHasNoRecipe} and will not look up again
 * until inventory changes. Skip that sticky latch so stage unlock re-queries next tick.
 */
public final class MekElectricStageRearm {
    private static final ThreadLocal<Boolean> SKIP_NO_RECIPE = new ThreadLocal<>();

    private MekElectricStageRearm() {}

    public static void markSkipNoRecipe() {
        SKIP_NO_RECIPE.set(Boolean.TRUE);
    }

    public static boolean consumeSkipNoRecipe() {
        Boolean marked = SKIP_NO_RECIPE.get();
        if (marked == null) {
            return false;
        }
        SKIP_NO_RECIPE.remove();
        return marked;
    }
}
