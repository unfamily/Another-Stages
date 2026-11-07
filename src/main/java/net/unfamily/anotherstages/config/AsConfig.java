package net.unfamily.anotherstages.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config (Library-style: comment + push/define, no Spec translation keys).
 * Logging under {@code logging}; mixin gates under {@code mixins}.
 */
public final class AsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Verbose Another Stages logging.").push("logging");
    }

    public static final ModConfigSpec.BooleanValue LOGGING_ENABLED = BUILDER
            .comment("Enable verbose Another Stages log lines (loaders, reloads, item checks).")
            .define("enabled", false);

    static {
        BUILDER.pop();
        BUILDER.comment(
                        "Toggle recipe/block gating mixins. Off = vanilla/compat hooks skipped (early-return).",
                        "Defaults are on. FastFurnace already skips the vanilla furnace mixin when loaded.")
                .push("mixins");
    }

    public static final ModConfigSpec.BooleanValue MIXIN_CRAFTING = BUILDER
            .comment("Gate vanilla CraftingMenu recipe selection.")
            .define("crafting", true);

    public static final ModConfigSpec.BooleanValue MIXIN_FURNACE = BUILDER
            .comment("Gate vanilla AbstractFurnaceBlockEntity (skipped when FastFurnace is loaded).")
            .define("furnace", true);

    public static final ModConfigSpec.BooleanValue MIXIN_STONECUTTER = BUILDER
            .comment("Gate StonecutterMenu results.")
            .define("stonecutter", true);

    public static final ModConfigSpec.BooleanValue MIXIN_SMITHING = BUILDER
            .comment("Gate SmithingMenu results.")
            .define("smithing", true);

    public static final ModConfigSpec.BooleanValue MIXIN_CRAFTER = BUILDER
            .comment("Gate CrafterBlock recipe selection.")
            .define("crafter", true);

    public static final ModConfigSpec.BooleanValue MIXIN_CAMPFIRE = BUILDER
            .comment("Gate CampfireBlockEntity cooking.")
            .define("campfire", true);

    public static final ModConfigSpec.BooleanValue MIXIN_RECIPE_BOOK_PLACE = BUILDER
            .comment("Gate ServerPlaceRecipe (recipe book click-to-place).")
            .define("recipe_book_place", true);

    public static final ModConfigSpec.BooleanValue MIXIN_FASTBENCH = BUILDER
            .comment("Gate FastWorkbench FastBenchUtil hooks.")
            .define("fastbench", true);

    public static final ModConfigSpec.BooleanValue MIXIN_FASTFURNACE = BUILDER
            .comment("Gate FastFurnace getRecipe cache.")
            .define("fastfurnace", true);

    public static final ModConfigSpec.BooleanValue MIXIN_BLOCK_APPEARANCE = BUILDER
            .comment("Client mimic appearance for stage_block.")
            .define("block_appearance", true);

    public static final ModConfigSpec.BooleanValue MIXIN_GUI_SLOTS = BUILDER
            .comment("Gate AbstractContainerMenu.clicked for stage_gui blocked slots/items.")
            .define("gui_slots", true);

    static {
        BUILDER.pop();
        BUILDER.comment(
                        "Optional player feedback. Recipe locks are silent by default (GUI/JEI already show the lock).",
                        "Enable action-bar tips mainly for machines without a useful GUI cue.")
                .push("feedback");
    }

    public static final ModConfigSpec.BooleanValue RECIPE_BLOCKED_ACTIONBAR = BUILDER
            .comment(
                    "When true, show an action-bar message if a stage-blocked recipe runs while the owner has no menu open.",
                    "Skipped when a container GUI is open. Default false.")
            .define("recipe_blocked_actionbar", false);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private AsConfig() {}

    public static boolean loggingEnabled() {
        return loaded(LOGGING_ENABLED, false);
    }

    public static boolean recipeBlockedActionbar() {
        return loaded(RECIPE_BLOCKED_ACTIONBAR, false);
    }

    public static boolean mixinCrafting() {
        return loaded(MIXIN_CRAFTING, true);
    }

    public static boolean mixinFurnace() {
        return loaded(MIXIN_FURNACE, true);
    }

    public static boolean mixinStonecutter() {
        return loaded(MIXIN_STONECUTTER, true);
    }

    public static boolean mixinSmithing() {
        return loaded(MIXIN_SMITHING, true);
    }

    public static boolean mixinCrafter() {
        return loaded(MIXIN_CRAFTER, true);
    }

    public static boolean mixinCampfire() {
        return loaded(MIXIN_CAMPFIRE, true);
    }

    public static boolean mixinRecipeBookPlace() {
        return loaded(MIXIN_RECIPE_BOOK_PLACE, true);
    }

    public static boolean mixinFastbench() {
        return loaded(MIXIN_FASTBENCH, true);
    }

    public static boolean mixinFastfurnace() {
        return loaded(MIXIN_FASTFURNACE, true);
    }

    public static boolean mixinBlockAppearance() {
        return loaded(MIXIN_BLOCK_APPEARANCE, true);
    }

    public static boolean mixinGuiSlots() {
        return loaded(MIXIN_GUI_SLOTS, true);
    }

    private static boolean loaded(ModConfigSpec.BooleanValue value, boolean defaultIfUnloaded) {
        if (!SPEC.isLoaded()) {
            return defaultIfUnloaded;
        }
        return value.getAsBoolean();
    }
}
