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
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.enderio.EnderIoRecipeIds;
import net.unfamily.anotherstages.compat.mekanism.MekRecipeIds;
import net.unfamily.anotherstages.recipe.RecipeInstanceIds;
import net.unfamily.anotherstages.restriction.RestrictionPriority;
import net.unfamily.anotherstages.util.StageIdMatchers;
import net.unfamily.iskalib.load.LoadFilesystemBootstrap;
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
import java.util.LinkedHashSet;
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
    /** Rules with {@code regex:} in recipes/recipe_types (cannot index by exact id). */
    private static final List<StageRecipeRule> PATTERN_RULES = new ArrayList<>();

    private static int RULE_REVISION;

    /**
     * Stage-independent: rules whose id/type/I/O matchers pass for a recipe id.
     * Cleared on index rebuild (datapack / Kube partial or full reload). Holder required to cache.
     */
    private static final Map<Identifier, List<StageRecipeRule>> MATCHER_PASS_CACHE = new HashMap<>();
    private static final List<StageRecipeRule> NO_MATCHING_RULES = List.of();

    private StageRecipeHandler() {}

    private static Map<Identifier, JsonElement> collectBootstrap() {
        Map<Identifier, JsonElement> merged = new java.util.LinkedHashMap<>();
        merged.putAll(LoadJson.collectFromModJar("another_stages", "another_stages", "another_stages_recipes"));
        mergeFilesystem(merged);
        return merged;
    }

    /** ResourceManager first, then jar + live filesystem roots (filesystem wins on same id). */
    private static Map<Identifier, JsonElement> collectMerged(@Nullable ResourceManager resourceManagerOrNull) {
        Map<Identifier, JsonElement> merged = new java.util.LinkedHashMap<>();
        if (resourceManagerOrNull != null) {
            merged.putAll(LoadJson.collectMergedJsonForTypes(resourceManagerOrNull, ACCEPTED_TYPES));
        }
        merged.putAll(LoadJson.collectFromModJar("another_stages", "another_stages", "another_stages_recipes"));
        mergeFilesystem(merged);
        return merged;
    }

    private static void mergeFilesystem(Map<Identifier, JsonElement> merged) {
        int external = LoadFilesystemBootstrap.mergeIntoForTypes(merged, ACCEPTED_TYPES);
        if (external > 0) {
            LOGGER.info("Recipe stage: merged {} file(s) from configured datapack paths", external);
        }
    }

    public static void loadAll(@Nullable ResourceManager resourceManagerOrNull) {
        DATAPACK_RULES.clear();
        try {
            Map<Identifier, JsonElement> merged = collectMerged(resourceManagerOrNull);
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
        PATTERN_RULES.clear();
        for (StageRecipeRule rule : allRules()) {
            indexRule(rule);
        }
        RULE_REVISION++;
        MATCHER_PASS_CACHE.clear();
        RecipeInstanceIds.clear();
    }

    /** Bumps when datapack/script recipe indexes rebuild (JEI fingerprint). */
    public static int clientRuleRevision() {
        return RULE_REVISION;
    }

    private static void indexRule(StageRecipeRule rule) {
        boolean indexed = false;
        for (String id : rule.recipes) {
            if (StageIdMatchers.hasRegexPrefix(id)) {
                continue;
            }
            Identifier recipeId = Identifier.tryParse(id);
            if (recipeId != null) {
                BY_RECIPE_ID.computeIfAbsent(recipeId, k -> new ArrayList<>()).add(rule);
                indexed = true;
            }
        }
        for (String type : rule.recipeTypes) {
            if (StageIdMatchers.hasRegexPrefix(type)) {
                continue;
            }
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
        if (StageIdMatchers.listHasRegex(rule.recipes) || StageIdMatchers.listHasRegex(rule.recipeTypes)) {
            PATTERN_RULES.add(rule);
            indexed = true;
        }
        if (!indexed && !rule.stages.isEmpty()) {
            LOGGER.warn("Ignoring stage_recipe rule with stages but no recipe/recipe_type/output/input matchers");
        }
    }

    private static StageRecipeRule parseRule(JsonObject ruleObj) {
        StageRecipeRule rule = new StageRecipeRule();
        rule.stagesLogic = ruleObj.has("stages_logic") ? ruleObj.get("stages_logic").getAsString() : "AND";
        if (ruleObj.has("priority") && ruleObj.get("priority").isJsonPrimitive()) {
            rule.priority = ruleObj.get("priority").getAsInt();
        }
        if (ruleObj.has("stages") && ruleObj.get("stages").isJsonArray()) {
            for (JsonElement stageElement : ruleObj.getAsJsonArray("stages")) {
                if (!stageElement.isJsonObject()) {
                    continue;
                }
                JsonObject stageObj = stageElement.getAsJsonObject();
                StageCondition condition = new StageCondition();
                condition.stageType = stageObj.has("stage_type")
                        ? stageObj.get("stage_type").getAsString().trim().toLowerCase(java.util.Locale.ROOT)
                        : "player";
                condition.stage = stageObj.has("stage") ? stageObj.get("stage").getAsString().trim() : "";
                condition.is = stageObj.has("is") && stageObj.get("is").getAsBoolean();
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
        return findActiveRulesClient(recipeId, holder, null);
    }

    /**
     * @param recipeView optional JEI recipe object (e.g. Utils wrapper) for I/O / type when holder is null
     */
    public static List<StageRecipeRule> findActiveRulesClient(
            Identifier recipeId,
            @Nullable RecipeHolder<?> holder,
            @Nullable Object recipeView) {
        List<StageRecipeRule> active = new ArrayList<>();
        if (recipeId == null || ruleCount() == 0) {
            return active;
        }
        // Holder path reuses matcher-pass cache (reload-cleared). Wrapper-only JEI views skip cache.
        List<StageRecipeRule> matching = holder != null
                ? rulesPassingMatchers(recipeId, holder)
                : rulesPassingMatchersUncached(recipeId, null, recipeView);
        for (StageRecipeRule rule : matching) {
            if (ruleAppliesClient(rule)) {
                active.add(rule);
            }
        }
        RestrictionPriority.sortByPriorityDesc(active, r -> r.priority);
        return active;
    }

    /** JEI wrapper path: no holder → do not populate {@link #MATCHER_PASS_CACHE}. */
    private static List<StageRecipeRule> rulesPassingMatchersUncached(
            Identifier recipeId,
            @Nullable RecipeHolder<?> holder,
            @Nullable Object recipeView) {
        List<StageRecipeRule> candidates = collectCandidates(recipeId, holder, recipeView);
        if (candidates.isEmpty()) {
            return NO_MATCHING_RULES;
        }
        String recipeIdStr = recipeId.toString();
        List<StageRecipeRule> passing = new ArrayList<>();
        for (StageRecipeRule rule : candidates) {
            if (excludedByAnyId(rule, recipeId, holder)) {
                continue;
            }
            if (!matchersPass(rule, holder, recipeIdStr, recipeView)) {
                continue;
            }
            passing.add(rule);
        }
        return passing.isEmpty() ? NO_MATCHING_RULES : passing;
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
        String type = normalizeStageType(condition.stageType);
        String stage = condition.stage == null ? "" : condition.stage.trim();
        return switch (type) {
            case "world" -> net.unfamily.anotherstages.client.ClientStageCache.hasWorldStage(stage);
            case "team" -> net.unfamily.anotherstages.client.ClientStageCache.hasTeamStage(stage);
            default -> net.unfamily.anotherstages.client.ClientStageCache.hasPlayerStage(stage);
        };
    }

    private static String normalizeStageType(@Nullable String stageType) {
        if (stageType == null || stageType.isBlank()) {
            return "player";
        }
        return stageType.trim().toLowerCase(java.util.Locale.ROOT);
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
        return collectReferencedStages(null);
    }

    /** Stages referenced by loaded rules; if {@code stageType} is set, only that type. */
    public static Set<String> collectReferencedStages(@Nullable String stageType) {
        String want = stageType == null || stageType.isBlank()
                ? null
                : stageType.trim().toLowerCase(java.util.Locale.ROOT);
        if ("team_player".equals(want)) {
            want = "team";
        }
        Set<String> stages = new HashSet<>();
        for (StageRecipeRule rule : allRules()) {
            for (StageCondition c : rule.stages) {
                if (c.stage == null || c.stage.isBlank()) {
                    continue;
                }
                String type = normalizeStageType(c.stageType);
                if ("team_player".equals(type)) {
                    type = "team";
                }
                if (want != null && !want.equals(type)) {
                    continue;
                }
                stages.add(c.stage.trim());
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
        List<StageRecipeRule> matching = rulesPassingMatchers(recipeId, holder);
        if (matching.isEmpty()) {
            return false;
        }
        for (StageRecipeRule rule : matching) {
            if (ruleApplies(rule, ownerUuid, level, onlinePlayer)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rules that pass exclude + id/type/I/O matchers (stage conditions not evaluated).
     * Cached per recipe id when {@code holder} is non-null until the next index rebuild.
     */
    private static List<StageRecipeRule> rulesPassingMatchers(
            Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        if (holder != null) {
            List<StageRecipeRule> cached = MATCHER_PASS_CACHE.get(recipeId);
            if (cached != null) {
                return cached;
            }
        }
        List<StageRecipeRule> candidates = collectCandidates(recipeId, holder, null);
        if (candidates.isEmpty()) {
            if (holder != null) {
                MATCHER_PASS_CACHE.put(recipeId, NO_MATCHING_RULES);
            }
            return NO_MATCHING_RULES;
        }
        String recipeIdStr = recipeId.toString();
        List<StageRecipeRule> passing = new ArrayList<>();
        for (StageRecipeRule rule : candidates) {
            if (excludedByAnyId(rule, recipeId, holder)) {
                continue;
            }
            if (!matchersPass(rule, holder, recipeIdStr, null)) {
                continue;
            }
            passing.add(rule);
        }
        List<StageRecipeRule> frozen = passing.isEmpty() ? NO_MATCHING_RULES : List.copyOf(passing);
        if (holder != null) {
            MATCHER_PASS_CACHE.put(recipeId, frozen);
        }
        return frozen;
    }

    /**
     * Filters candidates by recipe id / type / I/O matchers (AND).
     * Without a holder, {@code recipes} always works; JEI wrappers can supply type/I/O via {@code recipeView}.
     */
    private static boolean matchersPass(
            StageRecipeRule rule,
            @Nullable RecipeHolder<?> holder,
            String recipeIdStr,
            @Nullable Object recipeView) {
        boolean hasMatchers = !rule.recipes.isEmpty()
                || !rule.recipeTypes.isEmpty()
                || !rule.inputs.isEmpty()
                || !rule.outputs.isEmpty();
        if (!hasMatchers) {
            return true;
        }
        if (holder != null) {
            return matchesIo(rule, holder, recipeIdStr);
        }
        return matchesIoWrapper(rule, recipeIdStr, recipeView);
    }

    /** Holder-less JEI wrapper path (Utils Ancient Tablet / Factory, etc.). */
    private static boolean matchesIoWrapper(
            StageRecipeRule rule, String recipeIdStr, @Nullable Object recipeView) {
        Identifier recipeId = Identifier.tryParse(recipeIdStr);
        if (!rule.recipes.isEmpty()
                && (recipeId == null || !recipesMatcherHits(rule, recipeId, null, recipeIdStr))) {
            return false;
        }
        if (!rule.recipeTypes.isEmpty()) {
            Identifier typeId = JeiWrapperIoProbe.inferredRecipeType(recipeView);
            if (typeId == null || !StageIdMatchers.idInList(typeId.toString(), rule.recipeTypes)) {
                return false;
            }
        }
        if (!rule.outputs.isEmpty() && !JeiWrapperIoProbe.outputsMatch(recipeView, rule.outputs)) {
            return false;
        }
        if (!rule.inputs.isEmpty() && !JeiWrapperIoProbe.inputsMatch(recipeView, rule.inputs)) {
            return false;
        }
        return true;
    }

    private static List<StageRecipeRule> collectCandidates(
            Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        return collectCandidates(recipeId, holder, null);
    }

    private static List<StageRecipeRule> collectCandidates(
            Identifier recipeId, @Nullable RecipeHolder<?> holder, @Nullable Object recipeView) {
        LinkedHashSet<StageRecipeRule> out = new LinkedHashSet<>();
        for (Identifier id : dualMatchIds(recipeId, holder)) {
            out.addAll(BY_RECIPE_ID.getOrDefault(id, Collections.emptyList()));
        }
        if (holder != null) {
            Identifier typeId = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
            if (typeId != null) {
                out.addAll(BY_RECIPE_TYPE.getOrDefault(typeId, Collections.emptyList()));
            }
        } else {
            Identifier wrapperType = JeiWrapperIoProbe.inferredRecipeType(recipeView);
            if (wrapperType != null) {
                out.addAll(BY_RECIPE_TYPE.getOrDefault(wrapperType, Collections.emptyList()));
            }
        }
        // EIO remapped/wrapped + Mek synthetic WrappedSmelter live under non-vanilla types —
        // also pull minecraft:smelting (or blasting/smoking) type-index rules.
        Identifier cookingType = EnderIoRecipeIds.vanillaCookingTypeFor(recipeId, holder);
        if (cookingType == null) {
            cookingType = MekRecipeIds.vanillaCookingTypeFor(recipeId, holder);
        }
        if (cookingType != null) {
            out.addAll(BY_RECIPE_TYPE.getOrDefault(cookingType, Collections.emptyList()));
        }
        out.addAll(IO_RULES);
        out.addAll(PATTERN_RULES);
        return new ArrayList<>(out);
    }

    /**
     * AND of every non-empty matcher on the rule ({@code recipes}, {@code recipe_types},
     * {@code outputs}, {@code inputs}). I/O uses {@link RecipeIoProbe} (display/placement first,
     * then ingredient/result probing like Kube when display is empty).
     */
    private static boolean excludedByAnyId(
            StageRecipeRule rule, Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        if (rule.excludeRecipes.isEmpty()) {
            return false;
        }
        for (Identifier id : dualMatchIds(recipeId, holder)) {
            if (StageIdMatchers.idInList(id.toString(), rule.excludeRecipes)) {
                return true;
            }
        }
        return false;
    }

    private static boolean recipesMatcherHits(
            StageRecipeRule rule, Identifier recipeId, @Nullable RecipeHolder<?> holder, String recipeIdStr) {
        if (rule.recipes.isEmpty()) {
            return true;
        }
        if (StageIdMatchers.idInList(recipeIdStr, rule.recipes)) {
            return true;
        }
        for (Identifier id : dualMatchIds(recipeId, holder)) {
            if (StageIdMatchers.idInList(id.toString(), rule.recipes)) {
                return true;
            }
        }
        return false;
    }

    /** EIO remapped cooking ids + Mek mekanism_generated synthetic siblings. */
    private static List<Identifier> dualMatchIds(
            @Nullable Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        LinkedHashSet<Identifier> out = new LinkedHashSet<>(EnderIoRecipeIds.matchIds(recipeId, holder));
        out.addAll(MekRecipeIds.matchIds(recipeId, holder));
        return new ArrayList<>(out);
    }

    private static boolean matchesIo(StageRecipeRule rule, RecipeHolder<?> holder, String recipeIdStr) {
        Recipe<?> recipe = holder.value();
        Identifier recipeId = Identifier.tryParse(recipeIdStr);
        if (recipeId == null) {
            recipeId = holder.id().identifier();
        }
        if (!rule.recipes.isEmpty() && !recipesMatcherHits(rule, recipeId, holder, recipeIdStr)) {
            return false;
        }
        if (!rule.recipeTypes.isEmpty() && !recipeTypeMatcherHits(rule, holder, recipeId)) {
            return false;
        }
        if (!rule.outputs.isEmpty() && !RecipeIoProbe.outputsMatch(recipe, rule.outputs)) {
            return false;
        }
        if (!rule.inputs.isEmpty() && !RecipeIoProbe.inputsMatch(recipe, rule.inputs)) {
            return false;
        }
        return true;
    }

    /**
     * Recipe type match, plus Ender IO remapped/wrapped smelting treated as {@code minecraft:smelting}
     * so JEI and Alloy Smelter agree with vanilla smelting locks.
     */
    private static boolean recipeTypeMatcherHits(
            StageRecipeRule rule, RecipeHolder<?> holder, Identifier recipeId) {
        Identifier typeId = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
        if (typeId != null && StageIdMatchers.idInList(typeId.toString(), rule.recipeTypes)) {
            return true;
        }
        if (EnderIoRecipeIds.isRemappedCookingId(recipeId) || EnderIoRecipeIds.isWrappedAlloyRecipe(holder)) {
            String path = recipeId != null ? recipeId.getPath() : "";
            if (path.startsWith("blasting/")) {
                return StageIdMatchers.idInList("minecraft:blasting", rule.recipeTypes);
            }
            if (path.startsWith("smoking/")) {
                return StageIdMatchers.idInList("minecraft:smoking", rule.recipeTypes);
            }
            // Stock EIO wraps smelting only; wrapped alloy without remap path still counts as smelting.
            return StageIdMatchers.idInList("minecraft:smelting", rule.recipeTypes);
        }
        if (MekRecipeIds.isSyntheticSmeltingId(recipeId) || MekRecipeIds.isWrappedSmelterRecipe(holder)) {
            return StageIdMatchers.idInList("minecraft:smelting", rule.recipeTypes);
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

    private static boolean checkCondition(
            StageCondition condition,
            @Nullable UUID ownerUuid,
            @Nullable Level level,
            @Nullable ServerPlayer onlinePlayer) {
        String type = normalizeStageType(condition.stageType);
        String stage = condition.stage == null ? "" : condition.stage.trim();
        if (stage.isEmpty()) {
            return false;
        }
        return switch (type) {
            case "world" -> level != null && !level.isClientSide() && StageRegistry.worldHasStage(level, stage);
            case "team" -> {
                if (onlinePlayer != null) {
                    yield StageRegistry.playerTeamHasStage(onlinePlayer, stage);
                }
                if (ownerUuid == null || level == null || level.isClientSide()
                        || !(level instanceof ServerLevel serverLevel)) {
                    yield false;
                }
                yield StageRegistry.getInstance(serverLevel.getServer()).hasPlayerTeamStage(ownerUuid, stage);
            }
            default -> {
                if (onlinePlayer != null) {
                    yield StageRegistry.playerHasStage(onlinePlayer, stage);
                }
                if (ownerUuid == null || level == null || level.isClientSide()
                        || !(level instanceof ServerLevel serverLevel) || serverLevel.getServer() == null) {
                    yield false;
                }
                yield StageRegistry.getInstance(serverLevel.getServer()).hasPlayerStage(ownerUuid, stage);
            }
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
