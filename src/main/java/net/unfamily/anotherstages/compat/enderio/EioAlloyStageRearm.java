package net.unfamily.anotherstages.compat.enderio;

import org.jetbrains.annotations.Nullable;

/** @deprecated Use {@link EioStageRearm}. */
@Deprecated
public final class EioAlloyStageRearm {
    private EioAlloyStageRearm() {}

    public static void mark(@Nullable Object machineTaskHost) {
        EioStageRearm.mark(machineTaskHost);
    }

    public static boolean consume(@Nullable Object machineTaskHost) {
        return EioStageRearm.consume(machineTaskHost);
    }
}
