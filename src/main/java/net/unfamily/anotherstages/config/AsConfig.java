package net.unfamily.anotherstages.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config. Logging toggles live under {@code logging}.
 * Default: logging off. Tell the maintainer if you want a different default or a {@code dev} section split.
 */
public final class AsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOGGING_ENABLED;

    static {
        BUILDER.push("logging");
        LOGGING_ENABLED = BUILDER
                .comment("Enable verbose Another Stages log lines (loaders, reloads, item checks).")
                .define("enabled", false);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private AsConfig() {}

    public static boolean loggingEnabled() {
        if (!SPEC.isLoaded()) {
            return false;
        }
        return LOGGING_ENABLED.getAsBoolean();
    }
}
