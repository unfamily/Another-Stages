package net.unfamily.anotherstages.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.unfamily.iskalib.load.LoadJson;
import net.unfamily.iskalib.load.LoadModGate;
import net.unfamily.iskalib.stage.StageRegistry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Loads {@code another_stages:stage_recipe} restrictions and evaluates gates for RecipeBlockBridge.
 */
public final class StageRecipeHandler {
    public static final String TYPE_STAGE_RECIPE = "another_stages:stage_recipe";
    private static final Set<String> ACCEPTED_TYPES = Set.of(TYPE_STAGE_RECIPE);
    private static final Logger LOGGER = LoggerFactory.getLogger(StageRecipeHandler.class);

    private static final List<StageRecipeRule> DATAPACK_RULES = new ArrayList<>();
    private static final Map<String, StageRecipeRule> SCRIPT_RULES = new HashMap<>();
    private static final Map<Identifier, List<StageRecipeRule>> BY_RECIPE_ID = new HashMap<>();
    private static final Map<Identifier, List<StageRecipeRule>> BY_RECIPE_TYPE = new HashMap<>();
    private static final List<StageRecipeRule> IO_RULES = new ArrayList<>();

    private static int RULE_REVISION;

    private StageRecipeHandler() {}

    private static Map<Identifier, JsonElement> collectBootstrap() {
        Map<Identifier, JsonElement> merged = new java.util.LinkedHashMap<>();
        merged.putAll(LoadJson.collectFromModJar("another_stages", "another_stages", "another_stages_recipes"));
        int external = net.unfamily.iskalib.load.LoadFilesystemBootstrap.mergeIntoForTypes(merged, ACCEPTED_TYPES);
        if (external > 0) {
            LOGGER.info("Recipe stage bootstrap: merged {} file(s) from configured datapack paths", external);
        }
        return merged;
    }

    public static void loadAll(@Nullable ResourceManager resourceManagerOrNull) {
        DATAPACK_RULES.clear();
        try {
            Map<Identifier, JsonElement> merged = resourceManagerOrNull != null
                    ? LoadJson.collectMergedJsonForTypes(resourceManagerOrNull, ACCEPTED_TYPES)
                    : collectBootstrap();
            for (var e : LoadJson.orderedEntries(merged)) {
                if (!e.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject json = e.getValue().getAsJsonObject();
                if (!json.has("type") || !ACCEPTED_TYPES.contains(json.get("type").getAsString())) {
                    continue;
                }
                if (!json.has("restrictions") || !json.get("restrictions").isJsonArray()) {
                    continue;
                }
                for (JsonElement el : json.getAsJsonArray("restrictions")) {
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    JsonObject ruleObj = el.getAsJsonObject();
                    if (!LoadModGate.shouldIncludeAtLoad(ruleObj, LOGGER, "stage_recipe_rule")) {
                        continue;
                    }
                    DATAPACK_RULES.add(parseRule(ruleObj));
                }
            }
            rebuildIndexes();
            LOGGER.info("Loaded {} stage recipe rules ({} script)", ruleCount(), SCRIPT_RULES.size());
        } catch (Exception e) {
            LOGGER.error("Error loading stage recipe rules: {}", e.getMessage());
        }
    }

    public static void clearScriptRules() {
        SCRIPT_RULES.clear();
        rebuildIndexes();
    }

    public static void putScriptRule(String id, StageRecipeRule rule) {
        if (id == null || id.isBlank() || rule == null) {
            return;
        }
        SCRIPT_RULES.put(id, rule);
        rebuildIndexes();
    }

    public static boolean hasRuleId(String id) {
        return SCRIPT_RULES.containsKey(id);
    }

    private static int ruleCount() {
        return DATAPACK_RULES.size() + SCRIPT_RULES.size();
    }

    private static Iterable<StageRecipeRule> allRules() {
        if (SCRIPT_RULES.isEmpty()) {
            return DATAPACK_RULES;
        }
        ArrayList<StageRecipeRule> all = new ArrayList<>(ruleCount());
        all.addAll(DATAPACK_RULES);
        all.addAll(SCRIPT_RULES.values());
        return all;
    }

    private static void rebuildIndexes() {
        BY_RECIPE_ID.clear();
        BY_RECIPE_TYPE.clear();
        IO_RULES.clear();
        for (StageRecipeRule rule : allRules()) {
            indexRule(rule);
        }
        RULE_REVISION++;
    }

    /** Bumps when datapack/script recipe indexes rebuild (JEI fingerprint). */
    public static int clientRuleRevision() {
        return RULE_REVISION;
    }

    private static void indexRule(StageRecipeRule rule) {
        boolean indexed = false;
        for (String id : rule.recipes) {
            Identifier recipeId = Identifier.tryParse(id);
            if (recipeId != null) {
                BY_RECIPE_ID.computeIfAbsent(recipeId, k -> new ArrayList<>()).add(rule);
                indexed = true;
            }
        }
        for (String type : rule.recipeTypes) {
            Identifier typeId = Identifier.tryParse(type);
            if (typeId != null) {
                BY_RECIPE_TYPE.computeIfAbsent(typeId, k -> new ArrayList<>()).add(rule);
                indexed = true;
            }
        }
        if (!rule.outputs.isEmpty() || !rule.inputs.isEmpty()) {
            IO_RULES.add(rule);
            indexed = true;
        }
        if (!indexed && !rule.stages.isEmpty()) {
            LOGGER.warn("Ignoring stage_recipe rule with stages but no recipe/recipe_type/output/input matchers");
        }
    }

    private static StageRecipeRule parseRule(JsonObject ruleObj) {
        StageRecipeRule rule = new StageRecipeRule();
        rule.stagesLogic = ruleObj.has("stages_logic") ? ruleObj.get("stages_logic").getAsString() : "AND";
        if (ruleObj.has("stages") && ruleObj.get("stages").isJsonArray()) {
            for (JsonElement stageElement : ruleObj.getAsJsonArray("stages")) {
                if (!stageElement.isJsonObject()) {
                    continue;
                }
                JsonObject stageObj = stageElement.getAsJsonObject();
                StageCondition condition = new StageCondition();
                condition.stageType = stageObj.has("stage_type") ? stageObj.get("stage_type").getAsString() : "player";
                condition.stage = stageObj.has("stage") ? stageObj.get("stage").getAsString() : "";
                condition.is = !stageObj.has("is") || stageObj.get("is").getAsBoolean();
                rule.stages.add(condition);
            }
        }
        readStringList(ruleObj, "recipes", rule.recipes);
        readStringList(ruleObj, "recipe", rule.recipes);
        readStringList(ruleObj, "recipe_types", rule.recipeTypes);
        readStringList(ruleObj, "recipe_type", rule.recipeTypes);
        readStringList(ruleObj, "outputs", rule.outputs);
        readStringList(ruleObj, "output", rule.outputs);
        readStringList(ruleObj, "inputs", rule.inputs);
        readStringList(ruleObj, "input", rule.inputs);
        readStringList(ruleObj, "exclude_recipes", rule.excludeRecipes);
        if (ruleObj.has("recipe_viewer") && ruleObj.get("recipe_viewer").isJsonPrimitive()) {
            rule.recipeViewer = ruleObj.get("recipe_viewer").getAsString().trim().toLowerCase();
        }
        if (ruleObj.has("overlay_corner") && ruleObj.get("overlay_corner").isJsonPrimitive()) {
            rule.overlayCorner = normalizeOverlayCorner(ruleObj.get("overlay_corner").getAsString());
        }
        if (ruleObj.has("tooltips") && ruleObj.get("tooltips").isJsonArray()) {
            for (JsonElement tip : ruleObj.getAsJsonArray("tooltips")) {
                if (tip.isJsonPrimitive()) {
                    rule.tooltips.add(tip.getAsString());
                }
            }
        }
        rule.hideDefaultTooltips = ruleObj.has("hide_default_tooltips")
                && ruleObj.get("hide_default_tooltips").getAsBoolean();
        return rule;
    }

    /** Normalized RV mode: {@code overlay} (default) or {@code hide}. */
    public static String rvMode(StageRecipeRule rule) {
        if (rule == null || rule.recipeViewer == null || rule.recipeViewer.isBlank()) {
            return "overlay";
        }
        return "hide".equalsIgnoreCase(rule.recipeViewer) ? "hide" : "overlay";
    }

    /** Normalized overlay badge corner. */
    public static String overlayCorner(StageRecipeRule rule) {
        if (rule == null) {
            return "top_right";
        }
        return normalizeOverlayCorner(rule.overlayCorner);
    }

    public static String normalizeOverlayCorner(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return "top_right";
        }
        return switch (raw.trim().toLowerCase().replace('-', '_')) {
            case "top_left", "tl" -> "top_left";
            case "bottom_left", "bl" -> "bottom_left";
            case "bottom_right", "br" -> "bottom_right";
            default -> "top_right";
        };
    }

    public static boolean hasLoadedRules() {
        return ruleCount() > 0;
    }

    /**
     * Active rules for a recipe on the client (uses {@link net.unfamily.anotherstages.client.ClientStageCache}).
     */
    public static List<StageRecipeRule> findActiveRulesClient(
            Identifier recipeId,
            @Nullable RecipeHolder<?> holder) {
        List<StageRecipeRule> active = new ArrayList<>();
        if (recipeId == null || ruleCount() == 0) {
            return active;
        }
        List<StageRecipeRule> candidates = collectCandidates(recipeId, holder);
        String recipeIdStr = recipeId.toString();
        for (StageRecipeRule rule : candidates) {
            if (rule.excludeRecipes.contains(recipeIdStr)) {
                continue;
            }
            boolean needsIo = !rule.inputs.isEmpty() || !rule.outputs.isEmpty();
            if (needsIo && (holder == null || !matchesIo(rule, holder, recipeIdStr))) {
                continue;
            }
            if (ruleAppliesClient(rule)) {
                active.add(rule);
            }
        }
        return active;
    }

    private static boolean ruleAppliesClient(StageRecipeRule rule) {
        if (rule.stages.isEmpty()) {
            return true;
        }
        boolean and = !"OR".equalsIgnoreCase(rule.stagesLogic);
        if (and) {
            for (StageCondition condition : rule.stages) {
                boolean has = checkConditionClient(condition);
                if (has != condition.is) {
                    return false;
                }
            }
            return true;
        }
        for (StageCondition condition : rule.stages) {
            boolean has = checkConditionClient(condition);
            if (has == condition.is) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkConditionClient(StageCondition condition) {
        String type = condition.stageType == null ? "player" : condition.stageType;
        return switch (type) {
            case "world" -> net.unfamily.anotherstages.client.ClientStageCache.hasWorldStage(condition.stage);
            case "team" -> net.unfamily.anotherstages.client.ClientStageCache.hasTeamStage(condition.stage);
            default -> net.unfamily.anotherstages.client.ClientStageCache.hasPlayerStage(condition.stage);
        };
    }

    private static void readStringList(JsonObject obj, String key, List<String> out) {
        if (!obj.has(key)) {
            return;
        }
        JsonElement el = obj.get(key);
        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (e.isJsonPrimitive()) {
                    out.add(e.getAsString());
                }
            }
        } else if (el.isJsonPrimitive()) {
            out.add(el.getAsString());
        }
    }

    public static Set<String> collectReferencedStages() {
        Set<String> stages = new HashSet<>();
        for (StageRecipeRule rule : allRules()) {
            for (StageCondition c : rule.stages) {
                if (c.stage != null && !c.stage.isBlank()) {
                    stages.add(c.stage.trim());
                }
            }
        }
        return stages;
    }

    public static boolean isBlocked(
            Identifier recipeId,
            @Nullable UUID ownerUuid,
            @Nullable Level level,
            @Nullable ServerPlayer onlinePlayer) {
        if (recipeId == null || ruleCount() == 0) {
            return false;
        }
        RecipeHolder<?> holder = resolveHolder(recipeId, level);
        List<StageRecipeRule> candidates = collectCandidates(recipeId, holder);
        if (candidates.isEmpty()) {
            return false;
        }
        String recipeIdStr = recipeId.toString();
        for (StageRecipeRule rule : candidates) {
            if (rule.excludeRecipes.contains(recipeIdStr)) {
                continue;
            }
            boolean needsIo = !rule.inputs.isEmpty() || !rule.outputs.isEmpty();
            if (needsIo && (holder == null || !matchesIo(rule, holder, recipeIdStr))) {
                continue;
            }
            if (ruleApplies(rule, ownerUuid, level, onlinePlayer)) {
                return true;
            }
        }
        return false;
    }

    private static List<StageRecipeRule> collectCandidates(Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        List<StageRecipeRule> out = new ArrayList<>();
        out.addAll(BY_RECIPE_ID.getOrDefault(recipeId, Collections.emptyList()));
        if (holder != null) {
            Identifier typeId = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
            if (typeId != null) {
                out.addAll(BY_RECIPE_TYPE.getOrDefault(typeId, Collections.emptyList()));
            }
        }
        for (StageRecipeRule io : IO_RULES) {
            if (!out.contains(io)) {
                out.add(io);
            }
        }
        return out;
    }

    private static boolean matchesIo(StageRecipeRule rule, RecipeHolder<?> holder, String recipeIdStr) {
        if (rule.outputs.isEmpty() && rule.inputs.isEmpty()) {
            return true;
        }
        Recipe<?> recipe = holder.value();
        // Output matching requires assembling with a concrete input; skip strict output match when empty.
        if (!rule.outputs.isEmpty()) {
            // Prefer display/result via special recipes is unreliable without input; match by id list only when outputs set without inputs.
            // Soft-fail: if we cannot resolve an output stack, do not match IO-only on outputs.
            boolean matchedById = rule.recipes.contains(recipeIdStr);
            Identifier typeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            boolean matchedByType = typeId != null && rule.recipeTypes.contains(typeId.toString());
            if (!matchedById && !matchedByType && rule.inputs.isEmpty()) {
                return false;
            }
        }
        if (!rule.inputs.isEmpty()) {
            boolean any = false;
            for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
                boolean hit = ingredient.items().anyMatch(itemHolder -> itemMatchesList(itemHolder.value(), rule.inputs));
                if (hit) {
                    any = true;
                    break;
                }
            }
            if (!any) {
                return false;
            }
        }
        return true;
    }

    private static boolean itemMatchesList(Item item, List<String> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        String itemIdStr = itemId.toString();
        String modId = itemId.getNamespace();
        for (String entry : list) {
            if (entry.startsWith("#")) {
                Identifier tagId = Identifier.tryParse(entry.substring(1));
                if (tagId != null && item.builtInRegistryHolder().is(ItemTags.create(tagId))) {
                    return true;
                }
            } else if (entry.startsWith("@")) {
                if (modId.equals(entry.substring(1).trim())) {
                    return true;
                }
            } else if (itemIdStr.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean ruleApplies(
            StageRecipeRule rule,
            @Nullable UUID ownerUuid,
            @Nullable Level level,
            @Nullable ServerPlayer onlinePlayer) {
        if (rule.stages.isEmpty()) {
            return true;
        }
        if (onlinePlayer == null && hasPlayerScopedCondition(rule)) {
            return true;
        }
        boolean and = !"OR".equalsIgnoreCase(rule.stagesLogic);
        if (and) {
            for (StageCondition condition : rule.stages) {
                boolean has = checkCondition(condition, ownerUuid, level, onlinePlayer);
                if (has != condition.is) {
                    return false;
                }
            }
            return true;
        }
        for (StageCondition condition : rule.stages) {
            boolean has = checkCondition(condition, ownerUuid, level, onlinePlayer);
            if (has == condition.is) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPlayerScopedCondition(StageRecipeRule rule) {
        for (StageCondition condition : rule.stages) {
            String type = condition.stageType == null ? "player" : condition.stageType;
            if ("player".equals(type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkCondition(
            StageCondition condition,
            @Nullable UUID ownerUuid,
            @Nullable Level level,
            @Nullable ServerPlayer onlinePlayer) {
        String type = condition.stageType == null ? "player" : condition.stageType;
        return switch (type) {
            case "world" -> level != null && !level.isClientSide() && StageRegistry.worldHasStage(level, condition.stage);
            case "team" -> {
                if (ownerUuid == null || level == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
                    yield false;
                }
                yield StageRegistry.getInstance(serverLevel.getServer()).hasPlayerTeamStage(ownerUuid, condition.stage);
            }
            default -> onlinePlayer != null && StageRegistry.playerHasStage(onlinePlayer, condition.stage);
        };
    }

    @Nullable
    private static RecipeHolder<?> resolveHolder(Identifier recipeId, @Nullable Level level) {
        if (level == null || level.isClientSide()) {
            return null;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return null;
        }
        RecipeManager manager = server.getRecipeManager();
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, recipeId);
        return manager.byKey(key).orElse(null);
    }
}
