package net.unfamily.anotherstages.compat.create;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Thread-local subject for Create fan cooking (ItemEntity thrower / owner).
 * Outside mixin package — mixin packages forbid direct class loads.
 */
public final class CreateFanSubject {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private CreateFanSubject() {}

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
