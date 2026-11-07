package net.unfamily.anotherstages.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.recipe.StageCondition;
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

    private StageBlockHandler() {}

    private static Map<Identifier, JsonElement> collectBootstrap() {
        Map<Identifier, JsonElement> merged = new java.util.LinkedHashMap<>();
        merged.putAll(LoadJson.collectFromModJar("another_stages", "another_stages", "another_stages_blocks"));
        int external = net.unfamily.iskalib.load.LoadFilesystemBootstrap.mergeIntoForTypes(merged, ACCEPTED);
        if (external > 0) {
            LOGGER.info("Block stage bootstrap: merged {} file(s) from configured datapack paths", external);
        }
        return merged;
    }

    public static void loadAll(@Nullable ResourceManager rm) {
        DATAPACK_RULES.clear();
        try {
            Map<Identifier, JsonElement> merged = rm != null
                    ? LoadJson.collectMergedJsonForTypes(rm, ACCEPTED)
                    : collectBootstrap();
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
        for (StageBlockRule rule : allRules()) {
            for (String id : rule.blocks) {
                if (id.startsWith("#") || id.startsWith("@")) {
                    continue;
                }
                Identifier blockId = Identifier.tryParse(id);
                if (blockId != null) {
                    BY_BLOCK.computeIfAbsent(blockId, k -> new ArrayList<>()).add(rule);
                }
            }
        }
    }

    private static StageBlockRule parse(JsonObject obj) {
        StageBlockRule rule = new StageBlockRule();
        rule.stagesLogic = obj.has("stages_logic") ? obj.get("stages_logic").getAsString() : "AND";
        if (obj.has("stages") && obj.get("stages").isJsonArray()) {
            for (JsonElement stageElement : obj.getAsJsonArray("stages")) {
                if (!stageElement.isJsonObject()) {
                    continue;
                }
                JsonObject stageObj = stageElement.getAsJsonObject();
                StageCondition c = new StageCondition();
                c.stageType = stageObj.has("stage_type") ? stageObj.get("stage_type").getAsString() : "player";
                c.stage = stageObj.has("stage") ? stageObj.get("stage").getAsString() : "";
                c.is = !stageObj.has("is") || stageObj.get("is").getAsBoolean();
                rule.stages.add(c);
            }
        }
        readList(obj, "blocks", rule.blocks);
        readList(obj, "block", rule.blocks);
        readList(obj, "exclude", rule.exclude);
        rule.mimic = obj.has("mimic") ? obj.get("mimic").getAsString() : "minecraft:stone";
        rule.unbreakable = obj.has("unbreakable") && obj.get("unbreakable").getAsBoolean();
        rule.breakMessage = obj.has("break_message") ? obj.get("break_message").getAsString() : "";
        rule.interactMessage = obj.has("interact_message") ? obj.get("interact_message").getAsString() : "";
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
        Set<String> stages = new HashSet<>();
        for (StageBlockRule rule : allRules()) {
            for (StageCondition c : rule.stages) {
                if (c.stage != null && !c.stage.isBlank()) {
                    stages.add(c.stage.trim());
                }
            }
        }
        return stages;
    }

    @Nullable
    public static StageBlockRule findActiveRule(BlockState state, @Nullable Player player, @Nullable Level level) {
        if (ruleCount() == 0 || state == null) {
            return null;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        List<StageBlockRule> candidates = new ArrayList<>(BY_BLOCK.getOrDefault(id, List.of()));
        for (StageBlockRule rule : allRules()) {
            if (candidates.contains(rule)) {
                continue;
            }
            if (matchesBlock(state.getBlock(), rule.blocks) && !matchesBlock(state.getBlock(), rule.exclude)) {
                candidates.add(rule);
            }
        }
        for (StageBlockRule rule : candidates) {
            if (matchesBlock(state.getBlock(), rule.exclude)) {
                continue;
            }
            if (stagesApply(rule, player, level)) {
                return rule;
            }
        }
        return null;
    }

    private static boolean matchesBlock(Block block, List<String> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        String idStr = id.toString();
        String mod = id.getNamespace();
        for (String entry : list) {
            if (entry.startsWith("#")) {
                Identifier tag = Identifier.tryParse(entry.substring(1));
                if (tag != null && block.builtInRegistryHolder().is(BlockTags.create(tag))) {
                    return true;
                }
            } else if (entry.startsWith("@")) {
                if (mod.equals(entry.substring(1).trim())) {
                    return true;
                }
            } else if (idStr.equals(entry)) {
                return true;
            }
        }
        return false;
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
        String type = c.stageType == null ? "player" : c.stageType;
        return switch (type) {
            case "world" -> level != null && StageRegistry.worldHasStage(level, c.stage);
            case "team" -> player instanceof ServerPlayer sp && StageRegistry.playerTeamHasStage(sp, c.stage);
            default -> player instanceof ServerPlayer sp && StageRegistry.playerHasStage(sp, c.stage);
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
        if (!(player instanceof ServerPlayer sp) || rule.breakMessage == null || rule.breakMessage.isBlank()) {
            return;
        }
        sp.sendSystemMessage(resolveText(rule.breakMessage), true);
    }

    public static Component resolveText(String raw) {
        if (raw.indexOf(' ') < 0 && raw.indexOf('.') >= 0) {
            return Component.translatable(raw);
        }
        return Component.literal(raw);
    }

    public static boolean isLockedAt(BlockGetter level, BlockPos pos, @Nullable Player player) {
        if (!(level instanceof Level lvl)) {
            return false;
        }
        return findActiveRule(lvl.getBlockState(pos), player, lvl) != null;
    }
}
