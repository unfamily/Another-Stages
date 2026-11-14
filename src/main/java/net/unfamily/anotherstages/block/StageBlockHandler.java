package net.unfamily.anotherstages.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.client.ClientStageCache;
import net.unfamily.anotherstages.recipe.StageCondition;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads {@code another_stages:stage_block} and evaluates lock state for break/render/loot.
 */
public final class StageBlockHandler {
    public static final String TYPE = "another_stages:stage_block";
    private static final Set<String> ACCEPTED = Set.of(TYPE);
    private static final Logger LOGGER = LoggerFactory.getLogger(StageBlockHandler.class);

    private static final List<StageBlockRule> DATAPACK_RULES = new ArrayList<>();
    private static final Map<String, StageBlockRule> SCRIPT_RULES = new HashMap<>();
    private static final Map<Identifier, List<StageBlockRule>> BY_BLOCK = new HashMap<>();
    /**
     * Rules with {@code #tag}, {@code @mod}, or {@code regex:} matchers — cannot live in {@link #BY_BLOCK}.
     * Scanned only after the exact-id index (not every rule on every lookup).
     */
    private static final List<StageBlockRule> BROAD_RULES = new ArrayList<>();
    /** Exact-id index for {@code stop_tick} rules only (BE tick hot path). */
    private static final Map<Identifier, List<StageBlockRule>> STOP_TICK_BY_BLOCK = new HashMap<>();
    /** Broad matchers among {@code stop_tick} rules only. */
    private static final List<StageBlockRule> STOP_TICK_BROAD = new ArrayList<>();
    /** Fast path: any loaded rule has {@code stop_tick: true}. */
    private static boolean anyStopTickRules = false;

    private StageBlockHandler() {}

    private static Map<Identifier, JsonElement> collectBootstrap() {
        return collectMerged(null);
    }

    /** ResourceManager first, then jar + live filesystem roots (filesystem wins on same id). */
    private static Map<Identifier, JsonElement> collectMerged(@Nullable ResourceManager rm) {
        Map<Identifier, JsonElement> merged = new java.util.LinkedHashMap<>();
        if (rm != null) {
            merged.putAll(LoadJson.collectMergedJsonForTypes(rm, ACCEPTED));
        }
        merged.putAll(LoadJson.collectFromModJar("another_stages", "another_stages", "another_stages_blocks"));
        int external = LoadFilesystemBootstrap.mergeIntoForTypes(merged, ACCEPTED);
        if (external > 0) {
            LOGGER.info("Block stage: merged {} file(s) from configured datapack paths", external);
        }
        return merged;
    }

    public static void loadAll(@Nullable ResourceManager rm) {
        DATAPACK_RULES.clear();
        try {
            Map<Identifier, JsonElement> merged = collectMerged(rm);
            for (var e : LoadJson.orderedEntries(merged)) {
                if (!e.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject json = e.getValue().getAsJsonObject();
                if (!json.has("type") || !ACCEPTED.contains(json.get("type").getAsString())) {
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
                    if (!LoadModGate.shouldIncludeAtLoad(ruleObj, LOGGER, "stage_block_rule")) {
                        continue;
                    }
                    DATAPACK_RULES.add(parse(ruleObj));
                }
            }
            rebuildIndexes();
            LOGGER.info("Loaded {} stage block rules ({} script)", ruleCount(), SCRIPT_RULES.size());
        } catch (Exception ex) {
            LOGGER.error("Error loading stage block rules: {}", ex.getMessage());
        }
    }

    public static void clearScriptRules() {
        SCRIPT_RULES.clear();
        rebuildIndexes();
    }

    public static void putScriptRule(String id, StageBlockRule rule) {
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

    private static Iterable<StageBlockRule> allRules() {
        if (SCRIPT_RULES.isEmpty()) {
            return DATAPACK_RULES;
        }
        ArrayList<StageBlockRule> all = new ArrayList<>(ruleCount());
        all.addAll(DATAPACK_RULES);
        all.addAll(SCRIPT_RULES.values());
        return all;
    }

    private static void rebuildIndexes() {
        BY_BLOCK.clear();
        BROAD_RULES.clear();
        STOP_TICK_BY_BLOCK.clear();
        STOP_TICK_BROAD.clear();
        anyStopTickRules = false;
        for (StageBlockRule rule : allRules()) {
            boolean broad = false;
            boolean exact = false;
            for (String id : rule.blocks) {
                if (id.startsWith("#") || id.startsWith("@") || StageIdMatchers.hasRegexPrefix(id)) {
                    broad = true;
                    continue;
                }
                Identifier blockId = Identifier.tryParse(id);
                if (blockId != null) {
                    BY_BLOCK.computeIfAbsent(blockId, k -> new ArrayList<>()).add(rule);
                    if (rule.stopTick) {
                        STOP_TICK_BY_BLOCK.computeIfAbsent(blockId, k -> new ArrayList<>()).add(rule);
                    }
                    exact = true;
                }
            }
            if (broad || (!exact && !rule.blocks.isEmpty())) {
                BROAD_RULES.add(rule);
                if (rule.stopTick) {
                    STOP_TICK_BROAD.add(rule);
                }
            }
            if (rule.stopTick) {
                anyStopTickRules = true;
            }
        }
    }

    private static StageBlockRule parse(JsonObject obj) {
        StageBlockRule rule = new StageBlockRule();
        rule.stagesLogic = obj.has("stages_logic") ? obj.get("stages_logic").getAsString() : "AND";
        if (obj.has("priority") && obj.get("priority").isJsonPrimitive()) {
            rule.priority = obj.get("priority").getAsInt();
        }
        if (obj.has("stages") && obj.get("stages").isJsonArray()) {
            for (JsonElement stageElement : obj.getAsJsonArray("stages")) {
                if (!stageElement.isJsonObject()) {
                    continue;
                }
                JsonObject stageObj = stageElement.getAsJsonObject();
                StageCondition c = new StageCondition();
                c.stageType = stageObj.has("stage_type")
                        ? stageObj.get("stage_type").getAsString().trim().toLowerCase(java.util.Locale.ROOT)
                        : "player";
                c.stage = stageObj.has("stage") ? stageObj.get("stage").getAsString().trim() : "";
                c.is = stageObj.has("is") && stageObj.get("is").getAsBoolean();
                rule.stages.add(c);
            }
        }
        readList(obj, "blocks", rule.blocks);
        readList(obj, "block", rule.blocks);
        readList(obj, "exclude", rule.exclude);
        rule.mimic = obj.has("mimic") ? obj.get("mimic").getAsString() : "minecraft:stone";
        // Omitted → true (visual camouflage on). Explicit false = loot-only.
        rule.camouflage = !obj.has("camouflage") || obj.get("camouflage").getAsBoolean();
        rule.unbreakable = obj.has("unbreakable") && obj.get("unbreakable").getAsBoolean();
        // Omitted → true (mine as mimic). Explicit false = keep real break speed / harvest.
        rule.mimicMining = !obj.has("mimic_mining") || obj.get("mimic_mining").getAsBoolean();
        // DANGEROUS opt-in: omit / false = no tick freeze
        rule.stopTick = obj.has("stop_tick") && obj.get("stop_tick").getAsBoolean();
        // Omitted / blank → silent; packdevs opt in with lang key or .breakMessage(true).
        if (obj.has("break_message")) {
            rule.breakMessage = obj.get("break_message").getAsString();
        }
        if (obj.has("interact_message")) {
            rule.interactMessage = obj.get("interact_message").getAsString();
        }
        readList(obj, "tooltips", rule.tooltips);
        rule.hideDefaultTooltips = obj.has("hide_default_tooltips") && obj.get("hide_default_tooltips").getAsBoolean();
        return rule;
    }

    private static void readList(JsonObject obj, String key, List<String> out) {
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

    /**
     * Stage ids referenced by {@code stage_block} rules with {@code camouflage == true}
     * (default when omitted). Used to decide when client world mesh must rebuild.
     */
    public static Set<String> collectCamouflageReferencedStages() {
        Set<String> stages = new HashSet<>();
        for (StageBlockRule rule : allRules()) {
            if (!rule.camouflage) {
                continue;
            }
            for (StageCondition c : rule.stages) {
                if (c.stage != null && !c.stage.isBlank()) {
                    stages.add(c.stage.trim());
                }
            }
        }
        return stages;
    }

    public static Set<String> collectReferencedStages(@Nullable String stageType) {
        String want = stageType == null || stageType.isBlank()
                ? null
                : stageType.trim().toLowerCase(java.util.Locale.ROOT);
        if ("team_player".equals(want)) {
            want = "team";
        }
        Set<String> stages = new HashSet<>();
        for (StageBlockRule rule : allRules()) {
            for (StageCondition c : rule.stages) {
                if (c.stage == null || c.stage.isBlank()) {
                    continue;
                }
                String type = c.stageType == null || c.stageType.isBlank()
                        ? "player"
                        : c.stageType.trim().toLowerCase(java.util.Locale.ROOT);
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

    @Nullable
    public static StageBlockRule findActiveRule(BlockState state, @Nullable Player player, @Nullable Level level) {
        if (ruleCount() == 0 || state == null) {
            return null;
        }
        Block block = state.getBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        List<StageBlockRule> candidates = new ArrayList<>(BY_BLOCK.getOrDefault(id, List.of()));
        for (StageBlockRule rule : BROAD_RULES) {
            if (candidates.contains(rule)) {
                continue;
            }
            if (matchesBlock(block, rule.blocks) && !matchesBlock(block, rule.exclude)) {
                candidates.add(rule);
            }
        }
        List<StageBlockRule> active = new ArrayList<>();
        for (StageBlockRule rule : candidates) {
            if (matchesBlock(block, rule.exclude)) {
                continue;
            }
            if (stagesApply(rule, player, level)) {
                active.add(rule);
            }
        }
        return RestrictionPriority.pickHighest(active, r -> r.priority);
    }

    /**
     * True when any {@code stop_tick} rule's block matchers can hit this block (stages ignored).
     * Lets BE ticks skip {@link #findActiveRule} for the vast majority of tiles.
     */
    private static boolean couldMatchStopTick(Block block, Identifier id) {
        if (!STOP_TICK_BY_BLOCK.getOrDefault(id, List.of()).isEmpty()) {
            return true;
        }
        for (StageBlockRule rule : STOP_TICK_BROAD) {
            if (matchesBlock(block, rule.blocks) && !matchesBlock(block, rule.exclude)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesBlock(Block block, List<String> list) {
        return StageIdMatchers.matchesBlock(block, list);
    }

    private static boolean stagesApply(StageBlockRule rule, @Nullable Player player, @Nullable Level level) {
        if (rule.stages.isEmpty()) {
            return true;
        }
        boolean and = !"OR".equalsIgnoreCase(rule.stagesLogic);
        if (and) {
            for (StageCondition c : rule.stages) {
                if (check(c, player, level) != c.is) {
                    return false;
                }
            }
            return true;
        }
        for (StageCondition c : rule.stages) {
            if (check(c, player, level) == c.is) {
                return true;
            }
        }
        return false;
    }

    private static boolean check(StageCondition c, @Nullable Player player, @Nullable Level level) {
        String type = c.stageType == null || c.stageType.isBlank()
                ? "player"
                : c.stageType.trim().toLowerCase(java.util.Locale.ROOT);
        if ("team_player".equals(type)) {
            type = "team";
        }
        String stage = c.stage == null ? "" : c.stage.trim();
        // Library StageRegistry is server-only; client render/tooltips use the synced cache.
        if (level != null && level.isClientSide()) {
            return switch (type) {
                case "world" -> ClientStageCache.hasWorldStage(stage);
                case "team" -> ClientStageCache.hasTeamStage(stage);
                default -> ClientStageCache.hasPlayerStage(stage);
            };
        }
        return switch (type) {
            case "world" -> level != null && StageRegistry.worldHasStage(level, stage);
            case "team" -> player instanceof ServerPlayer sp && StageRegistry.playerTeamHasStage(sp, stage);
            default -> player instanceof ServerPlayer sp && StageRegistry.playerHasStage(sp, stage);
        };
    }

    public static BlockState mimicState(StageBlockRule rule) {
        Identifier id = Identifier.tryParse(rule.mimic);
        if (id == null) {
            id = Identifier.parse("minecraft:stone");
        }
        Block block = BuiltInRegistries.BLOCK.getOptional(id)
                .orElseGet(() -> BuiltInRegistries.BLOCK.getOptional(Identifier.parse("minecraft:stone")).orElseThrow());
        return block.defaultBlockState();
    }

    public static void maybeWarnBreak(Player player, StageBlockRule rule) {
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        Component message = resolveBreakMessage(rule);
        if (message == null) {
            return;
        }
        sp.sendSystemMessage(message, true);
    }

    /**
     * Resolved break feedback, or {@code null} when silent.
     * Component (Kube Text) wins; blank string = off; otherwise lang key / literal.
     */
    @Nullable
    public static Component resolveBreakMessage(StageBlockRule rule) {
        if (rule == null) {
            return null;
        }
        if (rule.breakMessageComponent != null) {
            return rule.breakMessageComponent;
        }
        if (rule.breakMessage == null || rule.breakMessage.isBlank()) {
            return null;
        }
        return resolveText(rule.breakMessage);
    }

    /**
     * Resolved interact feedback, or {@code null} when silent.
     * Component (Kube Text) wins; blank string = off; otherwise lang key / literal.
     */
    @Nullable
    public static Component resolveInteractMessage(StageBlockRule rule) {
        if (rule == null) {
            return null;
        }
        if (rule.interactMessageComponent != null) {
            return rule.interactMessageComponent;
        }
        if (rule.interactMessage == null || rule.interactMessage.isBlank()) {
            return null;
        }
        return resolveText(rule.interactMessage);
    }

    /**
     * String feedback: dotted with no spaces → lang key (translatable); otherwise literal (not translated).
     */
    public static Component resolveText(String raw) {
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.indexOf(' ') < 0 && trimmed.indexOf('.') >= 0) {
            return Component.translatable(trimmed);
        }
        return Component.literal(trimmed);
    }

    /** Whether any block rules are loaded (client geometry early-out). */
    public static boolean hasAnyRules() {
        return ruleCount() > 0;
    }

    /** True when the active rule should hide the real mesh and draw the mimic. */
    public static boolean usesCamouflage(@Nullable StageBlockRule rule) {
        return rule != null && rule.camouflage;
    }

    public static boolean isLockedAt(BlockGetter level, BlockPos pos, @Nullable Player player) {
        if (!(level instanceof Level lvl)) {
            return false;
        }
        return findActiveRule(lvl.getBlockState(pos), player, lvl) != null;
    }

    /**
     * DANGEROUS / best-effort: whether BE / scheduled / random ticks should be skipped at {@code pos}.
     * {@code player} may be null (typical for autonomous machines) — prefer world stages.
     */
    public static boolean shouldStopTick(Level level, BlockPos pos, @Nullable Player player) {
        if (!anyStopTickRules || level == null || pos == null || level.isClientSide()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (!couldMatchStopTick(block, id)) {
            return false;
        }
        StageBlockRule rule = findActiveRule(state, player, level);
        return rule != null && rule.stopTick;
    }
}
