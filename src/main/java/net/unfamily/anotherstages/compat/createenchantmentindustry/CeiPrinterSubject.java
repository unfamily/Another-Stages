package net.unfamily.anotherstages.compat.createenchantmentindustry;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Current printer BE owner for {@code RecipePrintingBehaviour} stage checks. */
public final class CeiPrinterSubject {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private CeiPrinterSubject() {}

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
