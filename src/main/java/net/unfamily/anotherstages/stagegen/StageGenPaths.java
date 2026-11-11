package net.unfamily.anotherstages.stagegen;

import net.neoforged.fml.loading.FMLPaths;
import net.unfamily.anotherstages.client.gui.stagegen.RestrictionDraft;
import net.unfamily.iskalib.IskaLibConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Fixed write paths for Stage Generator exports.
 * JSON uses the first entry of Library {@code bootstrap.datapack_paths}.
 */
public final class StageGenPaths {
    public static final String GENERATED_PACK = "another_stages_generated";
    public static final String LOAD_NS = "another_stages";

    private StageGenPaths() {}

    public static String firstDatapackPathConfig() {
        List<? extends String> paths = IskaLibConfig.resolveBootstrapDatapackPaths();
        if (paths == null || paths.isEmpty()) {
            return "";
        }
        String first = paths.getFirst();
        return first == null ? "" : first.trim();
    }

    public static Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    /**
     * Absolute file for a category JSON restriction bundle, or null if no datapack root configured.
     */
    public static Path resolveJsonFile(RestrictionDraft.Category category) {
        String raw = firstDatapackPathConfig();
        if (raw.isBlank()) {
            return null;
        }
        Path root = gameDir().resolve(raw).normalize();
        Path dataRoot = resolveDataRoot(root, raw);
        return dataRoot
                .resolve(LOAD_NS)
                .resolve("load")
                .resolve(jsonFileName(category));
    }

    public static Path resolveKubeFile(RestrictionDraft.Category category) {
        return gameDir()
                .resolve("kubejs")
                .resolve("server_scripts")
                .resolve("another_stages")
                .resolve(kubeFileName(category));
    }

    /** Relative path shown in UI (game-dir relative). */
    public static String displayJsonPath(RestrictionDraft.Category category) {
        Path abs = resolveJsonFile(category);
        if (abs == null) {
            return "(no bootstrap.datapack_paths)";
        }
        Path game = gameDir().normalize();
        try {
            return game.relativize(abs.normalize()).toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            return abs.toString().replace('\\', '/');
        }
    }

    public static String displayKubePath(RestrictionDraft.Category category) {
        Path abs = resolveKubeFile(category);
        Path game = gameDir().normalize();
        try {
            return game.relativize(abs.normalize()).toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            return abs.toString().replace('\\', '/');
        }
    }

    public static String displayFirstDatapackRoot() {
        String raw = firstDatapackPathConfig();
        return raw.isBlank() ? "(empty)" : raw;
    }

    private static Path resolveDataRoot(Path root, String rawConfig) {
        String lower = rawConfig.toLowerCase(Locale.ROOT).replace('\\', '/');
        if (looksLikeDataRoot(lower)) {
            return root;
        }
        // Pack container (datapacks / openloader / global_packs): dedicated generated pack.
        return root.resolve(GENERATED_PACK).resolve("data");
    }

    private static boolean looksLikeDataRoot(String lower) {
        return lower.endsWith("/data") || lower.equals("data") || lower.endsWith("kubejs/data");
    }

    public static boolean isPackContainerRoot(String rawConfig) {
        if (rawConfig == null || rawConfig.isBlank()) {
            return false;
        }
        return !looksLikeDataRoot(rawConfig.toLowerCase(Locale.ROOT).replace('\\', '/'));
    }

    public static Path generatedPackMeta(String rawConfig) {
        Path root = gameDir().resolve(rawConfig.trim()).normalize();
        return root.resolve(GENERATED_PACK).resolve("pack.mcmeta");
    }

    public static String jsonFileName(RestrictionDraft.Category category) {
        return switch (category) {
            case ITEM -> "stage_generator_items.json";
            case RECIPE -> "stage_generator_recipes.json";
            case BLOCK -> "stage_generator_blocks.json";
            case GUI -> "stage_generator_guis.json";
        };
    }

    public static String kubeFileName(RestrictionDraft.Category category) {
        return switch (category) {
            case ITEM -> "stage_generator_items.js";
            case RECIPE -> "stage_generator_recipes.js";
            case BLOCK -> "stage_generator_blocks.js";
            case GUI -> "stage_generator_guis.js";
        };
    }

    public static String jsonType(RestrictionDraft.Category category) {
        return switch (category) {
            case ITEM -> "another_stages:stage_item";
            case RECIPE -> "another_stages:stage_recipe";
            case BLOCK -> "another_stages:stage_block";
            case GUI -> "another_stages:stage_gui";
        };
    }

    /** Library-style known_stages JSON under the Stage Generator datapack root. */
    public static Path resolveKnownStagesJsonFile() {
        String raw = firstDatapackPathConfig();
        if (raw.isBlank()) {
            return null;
        }
        Path root = gameDir().resolve(raw).normalize();
        Path dataRoot = resolveDataRoot(root, raw);
        return dataRoot
                .resolve(LOAD_NS)
                .resolve("load")
                .resolve("stage_generator_known_stages.json");
    }

    public static Path resolveKnownStagesKubeFile() {
        return gameDir()
                .resolve("kubejs")
                .resolve("server_scripts")
                .resolve("another_stages")
                .resolve("stage_generator_known_stages.js");
    }

    public static String displayKnownStagesJsonPath() {
        Path abs = resolveKnownStagesJsonFile();
        if (abs == null) {
            return "(no bootstrap.datapack_paths)";
        }
        return displayRelative(abs);
    }

    public static String displayKnownStagesKubePath() {
        return displayRelative(resolveKnownStagesKubeFile());
    }

    private static String displayRelative(Path abs) {
        Path game = gameDir().normalize();
        try {
            return game.relativize(abs.normalize()).toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            return abs.toString().replace('\\', '/');
        }
    }
}
