package net.unfamily.anotherstages.compat.create;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Thread-local subject for Create {@code RecipeGridHandler.tryToApplyRecipe}
 * (static call from Mechanical Crafter; owner comes from {@code MachineOwners}).
 * Must live outside the mixin package — mixin packages forbid direct class loads.
 */
public final class CreateCrafterSubject {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private CreateCrafterSubject() {}

    public static void push(@Nullable UUID owner) {
        CURRENT.set(owner);
    }

    public static void clear() {
        CURRENT.remove();
    }

    @Nullable
    public static UUID current() {
        return CURRENT.get();
    }
}
