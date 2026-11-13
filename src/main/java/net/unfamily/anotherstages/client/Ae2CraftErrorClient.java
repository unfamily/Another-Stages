package net.unfamily.anotherstages.client;

import org.jetbrains.annotations.Nullable;

/**
 * Pending flag for AE2 {@code CraftErrorScreen}: apply AS lang once, race-safe with S2C.
 */
public final class Ae2CraftErrorClient {
    private static boolean pending;
    private static boolean applied;
    @Nullable
    private static String recipeId;

    private Ae2CraftErrorClient() {}

    public static void mark(@Nullable String recipeIdOrNull) {
        pending = true;
        applied = false;
        recipeId = recipeIdOrNull;
    }

    public static boolean isPending() {
        return pending && !applied;
    }

    @Nullable
    public static String peekRecipeId() {
        return recipeId;
    }

    /** Mark text as applied; clears pending so later INCOMPLETE_PLAN screens stay AE2-default. */
    public static void markApplied() {
        applied = true;
        pending = false;
        recipeId = null;
    }
}
