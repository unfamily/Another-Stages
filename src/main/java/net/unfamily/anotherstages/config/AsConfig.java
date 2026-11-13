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
            .gameRestart()
            .define("crafting", true);

    public static final ModConfigSpec.BooleanValue MIXIN_FURNACE = BUILDER
            .comment("Gate vanilla AbstractFurnaceBlockEntity (skipped when FastFurnace is loaded).")
            .gameRestart()
            .define("furnace", true);

    public static final ModConfigSpec.BooleanValue MIXIN_STONECUTTER = BUILDER
            .comment("Gate StonecutterMenu results.")
            .gameRestart()
            .define("stonecutter", true);

    public static final ModConfigSpec.BooleanValue MIXIN_SMITHING = BUILDER
            .comment("Gate SmithingMenu results.")
            .gameRestart()
            .define("smithing", true);

    public static final ModConfigSpec.BooleanValue MIXIN_CRAFTER = BUILDER
            .comment("Gate CrafterBlock recipe selection.")
            .gameRestart()
            .define("crafter", true);

    public static final ModConfigSpec.BooleanValue MIXIN_CAMPFIRE = BUILDER
            .comment("Gate CampfireBlockEntity cooking.")
            .gameRestart()
            .define("campfire", true);

    public static final ModConfigSpec.BooleanValue MIXIN_RECIPE_BOOK_PLACE = BUILDER
            .comment("Gate ServerPlaceRecipe (recipe book click-to-place).")
            .gameRestart()
            .define("recipe_book_place", true);

    public static final ModConfigSpec.BooleanValue MIXIN_FASTBENCH = BUILDER
            .comment("Gate FastWorkbench FastBenchUtil hooks.")
            .gameRestart()
            .define("fastbench", true);

    public static final ModConfigSpec.BooleanValue MIXIN_FASTFURNACE = BUILDER
            .comment("Gate FastFurnace getRecipe cache.")
            .gameRestart()
            .define("fastfurnace", true);

    public static final ModConfigSpec.BooleanValue MIXIN_POLYMORPH = BUILDER
            .comment("Filter stage-locked recipes from Polymorph / Polymorph+ conflict candidates / selectRecipe.")
            .gameRestart()
            .define("polymorph", true);

    public static final ModConfigSpec.BooleanValue MIXIN_BLOCK_APPEARANCE = BUILDER
            .comment("Client mimic appearance for stage_block.")
            .gameRestart()
            .define("block_appearance", true);

    public static final ModConfigSpec.BooleanValue MIXIN_BLOCK_STOP_TICK = BUILDER
            .comment(
                    "DANGEROUS: honor stage_block stop_tick (skip BE / scheduled / random ticks).",
                    "Disable if a compat crash occurs. Default true; stop_tick itself defaults false per rule.")
            .gameRestart()
            .define("block_stop_tick", true);

    public static final ModConfigSpec.BooleanValue MIXIN_GUI_SLOTS = BUILDER
            .comment("Gate AbstractContainerMenu.clicked for stage_gui blocked slots/items.")
            .gameRestart()
            .define("gui_slots", true);

    public static final ModConfigSpec.BooleanValue MIXIN_IRONFURNACES = BUILDER
            .comment("Gate Iron Furnaces cook start (getRecipeNonCached); insert remains allowed.")
            .gameRestart()
            .define("ironfurnaces", true);

    public static final ModConfigSpec.BooleanValue MIXIN_AE2 = BUILDER
            .comment(
                    "Gate Applied Energistics 2 path coverage: pattern encode, autocraft submit,",
                    "Molecular Assembler push, Crafting Terminal, Inscriber/Charger process, in-world transform.",
                    "ExtendedAE/AdvancedAE share AE2 encode/submit when on the same grid.")
            .gameRestart()
            .define("ae2", true);

    public static final ModConfigSpec.BooleanValue MIXIN_REFINEDSTORAGE = BUILDER
            .comment("Gate Refined Storage 2 autocraft / crafting grid path coverage.")
            .gameRestart()
            .define("refinedstorage", true);

    public static final ModConfigSpec.BooleanValue MIXIN_TOMS_STORAGE = BUILDER
            .comment("Gate Tom's Simple Storage crafting terminal path coverage.")
            .gameRestart()
            .define("toms_storage", true);

    public static final ModConfigSpec.BooleanValue MIXIN_SOPHISTICATED = BUILDER
            .comment("Gate Sophisticated Backpacks/Storage crafting (and related) path coverage.")
            .gameRestart()
            .define("sophisticated", true);

    public static final ModConfigSpec.BooleanValue MIXIN_CREATE = BUILDER
            .comment("Gate Create Mechanical Crafter / processing path coverage (when Create is present).")
            .gameRestart()
            .define("create", true);

    public static final ModConfigSpec.BooleanValue MIXIN_CREATEADDITION = BUILDER
            .comment("Gate Create Crafts & Additions Rolling Mill / Tesla Coil charging recipes.")
            .gameRestart()
            .define("createaddition", true);

    public static final ModConfigSpec.BooleanValue MIXIN_SLICEANDDICE = BUILDER
            .comment("Gate Create Slice & Dice Slicer (basin path also uses Create basin gate).")
            .gameRestart()
            .define("sliceanddice", true);

    public static final ModConfigSpec.BooleanValue MIXIN_CREATE_ENCHANTMENT_INDUSTRY = BUILDER
            .comment("Gate Create Enchantment Industry Printer / Grindstone Drain recipes.")
            .gameRestart()
            .define("create_enchantment_industry", true);

    public static final ModConfigSpec.BooleanValue MIXIN_CREATE_NEW_AGE = BUILDER
            .comment("Gate Create New Age Energiser recipes.")
            .gameRestart()
            .define("create_new_age", true);

    public static final ModConfigSpec.BooleanValue MIXIN_ENDERIO = BUILDER
            .comment("Gate Ender IO Crafter path coverage.")
            .gameRestart()
            .define("enderio", true);

    public static final ModConfigSpec.BooleanValue MIXIN_ENERGIZEDPOWER = BUILDER
            .comment("Gate Energized Power auto crafter / stonecutter / smithing path coverage.")
            .gameRestart()
            .define("energizedpower", true);

    public static final ModConfigSpec.BooleanValue MIXIN_CRAFTINGSTATION = BUILDER
            .comment("Gate Crafting Station menu path coverage.")
            .gameRestart()
            .define("craftingstation", true);

    public static final ModConfigSpec.BooleanValue MIXIN_MEKANISM = BUILDER
            .comment("Gate Mekanism Formulaic Assemblicator / QIO crafting path coverage.")
            .gameRestart()
            .define("mekanism", true);

    public static final ModConfigSpec.BooleanValue MIXIN_INTEGRATEDDYNAMICS = BUILDER
            .comment("Gate Integrated Dynamics / Crafting path coverage.")
            .gameRestart()
            .define("integrateddynamics", true);

    public static final ModConfigSpec.BooleanValue MIXIN_FORESTRY = BUILDER
            .comment("Gate Forestry CE Worktable path coverage.")
            .gameRestart()
            .define("forestry", true);


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

    public static boolean mixinIronFurnaces() {
        return loaded(MIXIN_IRONFURNACES, true);
    }

    public static boolean mixinAe2() {
        return loaded(MIXIN_AE2, true);
    }

    public static boolean mixinRefinedStorage() {
        return loaded(MIXIN_REFINEDSTORAGE, true);
    }

    public static boolean mixinTomsStorage() {
        return loaded(MIXIN_TOMS_STORAGE, true);
    }

    public static boolean mixinSophisticated() {
        return loaded(MIXIN_SOPHISTICATED, true);
    }

    public static boolean mixinCreate() {
        return loaded(MIXIN_CREATE, true);
    }

    public static boolean mixinCreateAddition() {
        return loaded(MIXIN_CREATEADDITION, true);
    }

    public static boolean mixinSliceAndDice() {
        return loaded(MIXIN_SLICEANDDICE, true);
    }

    public static boolean mixinCreateEnchantmentIndustry() {
        return loaded(MIXIN_CREATE_ENCHANTMENT_INDUSTRY, true);
    }

    public static boolean mixinCreateNewAge() {
        return loaded(MIXIN_CREATE_NEW_AGE, true);
    }

    public static boolean mixinEnderio() {
        return loaded(MIXIN_ENDERIO, true);
    }

    public static boolean mixinEnergizedPower() {
        return loaded(MIXIN_ENERGIZEDPOWER, true);
    }

    public static boolean mixinCraftingStation() {
        return loaded(MIXIN_CRAFTINGSTATION, true);
    }

    public static boolean mixinMekanism() {
        return loaded(MIXIN_MEKANISM, true);
    }

    public static boolean mixinIntegratedDynamics() {
        return loaded(MIXIN_INTEGRATEDDYNAMICS, true);
    }

    public static boolean mixinForestry() {
        return loaded(MIXIN_FORESTRY, true);
    }


    private static boolean loaded(ModConfigSpec.BooleanValue value, boolean defaultIfUnloaded) {
        if (!SPEC.isLoaded()) {
            return defaultIfUnloaded;
        }
        return value.getAsBoolean();
    }
}
