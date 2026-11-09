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

    public static final ModConfigSpec.BooleanValue MIXIN_POLYMORPH = BUILDER
            .comment("Filter stage-locked recipes from Polymorph+ conflict candidates / selectRecipe.")
            .define("polymorph", true);

    public static final ModConfigSpec.BooleanValue MIXIN_BLOCK_APPEARANCE = BUILDER
            .comment("Client mimic appearance for stage_block.")
            .define("block_appearance", true);

    public static final ModConfigSpec.BooleanValue MIXIN_BLOCK_STOP_TICK = BUILDER
            .comment(
                    "DANGEROUS: honor stage_block stop_tick (skip BE / scheduled / random ticks).",
                    "Disable if a compat crash occurs. Default true; stop_tick itself defaults false per rule.")
            .define("block_stop_tick", true);

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
        BUILDER.comment(
                        "Stage Generator GUI (/another_stages edit). Internal preference flags;",
                        "kubejs_prompt_seed is a one-shot like Another Dynamics cableFacadesConfigSeed.")
                .push("stage_generator");
    }

    public static final ModConfigSpec.BooleanValue PREFER_KUBEJS = BUILDER
            .comment(
                    "When true, Stage Generator writes fixed KubeJS server_scripts files;",
                    "when false, writes JSON under the first iska_lib bootstrap.datapack_paths entry.",
                    "Set by the first-open KubeJS preference dialog (or manually).")
            .define("prefer_kubejs", false);

    public static final ModConfigSpec.BooleanValue KUBEJS_PROMPT_SEED = BUILDER
            .comment(
                    "One-shot: when KubeJS is present and the generator opens, ask whether to manage",
                    "stages with KubeJS, then set prefer_kubejs and clear this flag.",
                    "Left true while KubeJS is absent so the prompt runs after KubeJS is added later.")
            .define("kubejs_prompt_seed", true);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private AsConfig() {}

    /** Persist common config after programmatic {@link ModConfigSpec.ConfigValue#set} updates. */
    public static void saveCommon() {
        SPEC.save();
    }

    public static boolean preferKubejs() {
        return loaded(PREFER_KUBEJS, false);
    }

    public static boolean kubejsPromptSeed() {
        return loaded(KUBEJS_PROMPT_SEED, true);
    }

    public static void setPreferKubejs(boolean prefer) {
        PREFER_KUBEJS.set(prefer);
    }

    public static void clearKubejsPromptSeed() {
        KUBEJS_PROMPT_SEED.set(false);
    }

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

    public static boolean mixinPolymorph() {
        return loaded(MIXIN_POLYMORPH, true);
    }

    public static boolean mixinBlockAppearance() {
        return loaded(MIXIN_BLOCK_APPEARANCE, true);
    }

    public static boolean mixinBlockStopTick() {
        return loaded(MIXIN_BLOCK_STOP_TICK, true);
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
