package net.unfamily.anotherstages.item;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.iskalib.load.StagesLogic;
import net.unfamily.iskalib.load.LoadJson;
import net.unfamily.iskalib.load.LoadModCondition;
import net.unfamily.iskalib.load.LoadModGate;
import net.unfamily.iskalib.stage.StageRegistry;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages staged items by checking inventories and applying consequences
 * based on restrictions defined in JSON files.
 */
public class StageItemHandler {
    public static final String STAGE_ITEMS_SUBDIR = "another_stages_items";
    public static final String STAGE_ITEMS_SUBDIR_LEGACY = "iska_utils_stage_items";
    public static final String TYPE_STAGE_ITEM = "another_stages:stage_item";
    public static final String TYPE_STAGE_ITEM_UTILS = "iska_utils:stage_item";
    public static final String TYPE_STAGE_ITEM_LIB = "iska_lib:stage_item";
    private static final java.util.Set<String> ACCEPTED_TYPES = java.util.Set.of(
            TYPE_STAGE_ITEM, TYPE_STAGE_ITEM_UTILS, TYPE_STAGE_ITEM_LIB);

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(StageItemHandler.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Map of item restrictions loaded from JSON files
    private static final Map<String, StageItemRestriction> DATAPACK_ITEM_RESTRICTIONS = new HashMap<>();
    /** Restrictions registered from KubeJS scripts (cleared on script reload only). */
    private static final Map<String, StageItemRestriction> SCRIPT_ITEM_RESTRICTIONS = new HashMap<>();

    /**
     * Loads item restrictions from any {@code data/<namespace>/load/…} file whose
     * {@code type} is {@code another_stages:stage_item}, {@code iska_utils:stage_item}, or {@code iska_lib:stage_item}.
     * Recommended folder: {@code load/another_stages_items/} (legacy {@code load/iska_utils_stage_items/} still works).
     */
    public static void loadAll(ResourceManager resourceManagerOrNull) {
        DATAPACK_ITEM_RESTRICTIONS.clear();
        try {
            Map<Identifier, JsonElement> merged = resourceManagerOrNull != null
                    ? LoadJson.collectMergedJsonForTypes(resourceManagerOrNull, ACCEPTED_TYPES)
                    : collectBootstrap();
            for (var e : LoadJson.orderedEntries(merged)) {
                if (!e.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject json = e.getValue().getAsJsonObject();
                if (json.has("type") && ACCEPTED_TYPES.contains(json.get("type").getAsString())) {
                    StageItemRestriction restriction = parseRestriction(json);
                    String id = LoadJson.definitionIdFromLocation(e.getKey());
                    DATAPACK_ITEM_RESTRICTIONS.put(id, restriction);
                    LOGGER.info("Loaded item restriction: {}", id);
                }
            }
            LOGGER.info("Loaded {} item restrictions", DATAPACK_ITEM_RESTRICTIONS.size());
        } catch (Exception e) {
            LOGGER.error("Error loading item restrictions: {}", e.getMessage());
        }
    }

    private static Map<Identifier, JsonElement> collectBootstrap() {
        Map<Identifier, JsonElement> merged = new java.util.LinkedHashMap<>();
        merged.putAll(LoadJson.collectFromModJar("another_stages", "another_stages", STAGE_ITEMS_SUBDIR));
        merged.putAll(LoadJson.collectFromModJar("another_stages", "another_stages", STAGE_ITEMS_SUBDIR_LEGACY));
        merged.putAll(LoadJson.collectFromModJar("iska_lib", "iska_lib", STAGE_ITEMS_SUBDIR_LEGACY));
        merged.putAll(LoadJson.collectFromModJar("iska_utils", "iska_utils", STAGE_ITEMS_SUBDIR_LEGACY));
        int external = net.unfamily.iskalib.load.LoadFilesystemBootstrap.mergeIntoForTypes(merged, ACCEPTED_TYPES);
        if (external > 0) {
            LOGGER.info("Item stage bootstrap: merged {} file(s) from configured datapack paths", external);
        }
        return merged;
    }

    /**
     * Collects all stage ids referenced by loaded item restrictions (for command suggestions).
     */
    public static java.util.Set<String> collectReferencedStages() {
        java.util.LinkedHashSet<String> stages = new java.util.LinkedHashSet<>();
        for (StageItemRestriction restriction : allRestrictions()) {
            if (restriction == null || restriction.restrictions == null) {
                continue;
            }
            for (StageItemRule rule : restriction.restrictions) {
                if (rule == null || rule.stages == null) {
                    continue;
                }
                for (StageCondition condition : rule.stages) {
                    if (condition != null && condition.stage != null && !condition.stage.isBlank()) {
                        stages.add(condition.stage.trim());
                    }
                }
            }
        }
        return stages;
    }
    
    /**
     * Loads all item restrictions from JSON files
     */
    public static void loadItemRestrictions(Path configPath) {
        DATAPACK_ITEM_RESTRICTIONS.clear();
        
        try {
            Files.list(configPath)
                .filter(path -> path.toString().endsWith(".json") && !path.getFileName().toString().equals("example.json"))
                .forEach(path -> {
                    try {
                        JsonObject json = GSON.fromJson(new FileReader(path.toFile()), JsonObject.class);
                        if (json.has("type") && ACCEPTED_TYPES.contains(json.get("type").getAsString())) {
                            StageItemRestriction restriction = parseRestriction(json);
                            String id = path.getFileName().toString().replace(".json", "");
                            DATAPACK_ITEM_RESTRICTIONS.put(id, restriction);
                            LOGGER.info("Loaded item restriction: {}", id);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error loading item restriction from {}: {}", path, e.getMessage());
                    }
                });
                
            LOGGER.info("Loaded {} item restrictions", DATAPACK_ITEM_RESTRICTIONS.size());
        } catch (IOException e) {
            LOGGER.error("Error loading item restrictions: {}", e.getMessage());
        }
    }

    
    /**
     * Checks a container when opened by a player
     */
    public static void checkContainer(AbstractContainerMenu container, Player player) {
        if (!(player instanceof ServerPlayer) || player.level().isClientSide()) {
            return;
        }
        
        ServerPlayer serverPlayer = (ServerPlayer) player;
        Level level = player.level();
        
        LOGGER.debug("Checking container for player {}: {}", player.getName().getString(), container.getClass().getName());
        
        if (!hasLoadedRestrictions()) {
            LOGGER.debug("No item restrictions loaded, skipping check");
            return;
        }
        
        for (Map.Entry<String, StageItemRestriction> entry : allRestrictionEntries()) {
            String restrictionId = entry.getKey();
            StageItemRestriction restriction = entry.getValue();
            
            for (StageItemRule rule : restriction.restrictions) {
                // Check stage conditions
                boolean stageConditionsMet = checkStageConditions(rule, serverPlayer, level);
                
                if (!stageConditionsMet) {
                    continue;
                }
                
                // Check if container is in whitelist/blacklist
                String containerClassName = container.getClass().getName();
                boolean isContainerAffected = isContainerAffected(rule, containerClassName);
                
                if (isContainerAffected) {
                    // Apply consequence to items
                    LOGGER.info("Applying consequence '{}' to items in container {} for player {}", 
                        rule.consequence, containerClassName, player.getName().getString());
                    applyItemConsequences(rule, serverPlayer, container);
                    
                    // Notify player about the restriction
                    switch (rule.consequence.toLowerCase()) {
                        case "drop":
                            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "message.iska_utils.item_restriction.dropped"), true);
                            break;
                        case "delete":
                            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "message.iska_utils.item_restriction.deleted"), true);
                            break;
                        case "block":
                        case "block_drop":
                        case "block_delete":
                            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "message.iska_utils.item_restriction.blocked"), true);
                            break;
                    }
                }
            }
        }
    }
    
    /**
     * Checks if a container is affected by the rule
     */
    private static boolean isContainerAffected(StageItemRule rule, String containerClassName) {
        if (rule.containersWhitelist) {
            // If whitelist, container must be in the list
            return rule.containersList.contains(containerClassName);
        } else {
            // If blacklist, container must not be in the list
            return rule.containersList.isEmpty() || !rule.containersList.contains(containerClassName);
        }
    }
    
    /**
     * Checks stage conditions
     */
    private static boolean checkStageConditions(StageItemRule rule, ServerPlayer player, Level level) {
        if (rule.stagesLogic.equals("AND")) {
            // All conditions must be true
            for (StageCondition condition : rule.stages) {
                boolean hasStage = checkSingleStageCondition(condition, player, level);
                if (hasStage != condition.is) {
                    return false;
                }
            }
            return true;
        } else if (rule.stagesLogic.equals("OR")) {
            // At least one condition must be true
            for (StageCondition condition : rule.stages) {
                boolean hasStage = checkSingleStageCondition(condition, player, level);
                if (hasStage == condition.is) {
                    return true;
                }
            }
            return false;
        } else if (rule.stagesLogic.equals("DEF_OR")) {
            // Custom DEF_OR logic
            // Check conditions based on specific subconditions in "if"
            if (rule.ifConditions != null && !rule.ifConditions.isEmpty()) {
                for (StageItemIfRule ifRule : rule.ifConditions) {
                    if (checkIfRuleConditions(ifRule, rule, player, level)) {
                        return true;
                    }
                }
            }
            return false;
        } else if (rule.stagesLogic.equals("DEF_AND")) {
            // Custom DEF_AND logic
            // All conditions in "if" must be true
            if (rule.ifConditions != null && !rule.ifConditions.isEmpty()) {
                boolean allValid = true;
                for (StageItemIfRule ifRule : rule.ifConditions) {
                    if (!checkIfRuleConditions(ifRule, rule, player, level)) {
                        allValid = false;
                        break;
                    }
                }
                return allValid;
            }
            return false;
        }
        
        // Default: no conditions means always active
        return rule.stages.isEmpty();
    }
    
    /**
     * Checks a single stage condition
     */
    private static boolean checkSingleStageCondition(StageCondition condition, ServerPlayer player, Level level) {
        if ("player".equals(condition.stageType)) {
            return StageRegistry.playerHasStage(player, condition.stage);
        } else if ("world".equals(condition.stageType)) {
            return StageRegistry.worldHasStage(level, condition.stage);
        } else if ("team".equals(condition.stageType)) {
            return StageRegistry.playerTeamHasStage(player, condition.stage);
        }
        return false;
    }
    
    /**
     * Checks conditions of an if rule
     */
    private static boolean checkIfRuleConditions(StageItemIfRule ifRule, StageItemRule rule, ServerPlayer player, Level level) {
        boolean stageOk = true;
        if (!ifRule.conditions.isEmpty()) {
            List<StageCondition> allStages = rule.stages;
            for (int conditionIndex : ifRule.conditions) {
                if (conditionIndex < 0 || conditionIndex >= allStages.size()) {
                    LOGGER.warn("Invalid condition index: {} (max: {})", conditionIndex, allStages.size() - 1);
                    return false;
                }
                StageCondition condition = allStages.get(conditionIndex);
                boolean hasStage = checkSingleStageCondition(condition, player, level);
                if (hasStage != condition.is) {
                    stageOk = false;
                    break;
                }
            }
        }

        boolean modOk = ifRule.modConditions.isEmpty()
                || LoadModGate.checkModIndices(rule.mods, rule.modsLogic, ifRule.modConditions);
        return stageOk && modOk;
    }
    
    /**
     * Applies consequences to items
     */
    private static void applyItemConsequences(StageItemRule rule, ServerPlayer player, AbstractContainerMenu container) {
        List<Integer> slotsToAffect = new ArrayList<>();
        
        // Find all slots containing target items
        for (int i = 0; i < container.slots.size(); i++) {
            ItemStack stack = container.slots.get(i).getItem();
            if (!stack.isEmpty() && isItemAffected(stack.getItem(), rule.items, rule.exclude)) {
                slotsToAffect.add(i);
            }
        }
        
        // Apply consequence to all affected slots
        for (int slot : slotsToAffect) {
            ItemStack stack = container.slots.get(slot).getItem().copy();
            
            switch (rule.consequence.toLowerCase()) {
                case "drop":
                    // Drop item on the ground
                    player.drop(stack, false);
                    container.slots.get(slot).set(ItemStack.EMPTY);
                    break;
                    
                case "delete":
                    // Delete the item
                    container.slots.get(slot).set(ItemStack.EMPTY);
                    break;
                    
                case "block_drop":
                    // Block usage, drop if not in container context
                    if (!rule.otherCase.isEmpty()) {
                        player.drop(stack, false);
                        container.slots.get(slot).set(ItemStack.EMPTY);
                    }
                    break;
                    
                case "block_delete":
                    // Block usage, delete if not in container context
                    if (!rule.otherCase.isEmpty()) {
                        container.slots.get(slot).set(ItemStack.EMPTY);
                    }
                    break;
                    
                case "block":
                    // Just block usage (no item removal)
                    // This is handled elsewhere for non-container contexts
                    break;
                    
                default:
                    // Default does nothing
                    break;
            }
        }
        
        // Update container if any items were modified
        if (!slotsToAffect.isEmpty()) {
            container.broadcastChanges();
        }
    }
    
    /**
     * Returns true if the item matches any entry in the list (item ID, #tag, or @modid).
     */
    private static boolean itemMatchesList(Item item, List<String> list) {
        if (list == null || list.isEmpty()) return false;
        Identifier itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        String itemIdStr = itemId.toString();
        String modId = itemId.getNamespace();
        for (String entry : list) {
            if (entry.startsWith("#")) {
                Identifier tagId = Identifier.tryParse(entry.substring(1));
                if (tagId != null && item.builtInRegistryHolder().is(ItemTags.create(tagId))) return true;
            } else if (entry.startsWith("@")) {
                String entryModId = entry.substring(1).trim();
                if (!entryModId.isEmpty() && modId.equals(entryModId)) return true;
            } else {
                if (itemIdStr.equals(entry)) return true;
            }
        }
        return false;
    }

    /**
     * Checks if an item is affected by the rule (matches items and is not in exclude).
     * Supports in both items and exclude: item ID, #tag, @modid. Exclude entries exempt the item.
     */
    private static boolean isItemAffected(Item item, List<String> itemsList, List<String> excludeList) {
        if (excludeList != null && !excludeList.isEmpty() && itemMatchesList(item, excludeList)) return false;
        return itemMatchesList(item, itemsList);
    }
    
    /**
     * Parses a restriction from a JSON object
     */
    private static StageItemRestriction parseRestriction(JsonObject json) {
        StageItemRestriction restriction = new StageItemRestriction();
        // overwritable ignored (obsolete)
        
        if (json.has("restrictions") && json.get("restrictions").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("restrictions")) {
                if (element.isJsonObject()) {
                    JsonObject ruleObj = element.getAsJsonObject();
                    if (!LoadModGate.shouldIncludeAtLoad(ruleObj, LOGGER, "stage_item_rule")) {
                        continue;
                    }
                    StageItemRule rule = new StageItemRule();
                    
                    // Parse stage logic
                    rule.stagesLogic = ruleObj.has("stages_logic") ? ruleObj.get("stages_logic").getAsString() : "AND";
                    rule.mods.addAll(LoadModGate.parseMods(ruleObj));
                    rule.modsLogic = LoadModGate.parseModsLogic(ruleObj);
                    
                    // Parse stage conditions
                    if (ruleObj.has("stages") && ruleObj.get("stages").isJsonArray()) {
                        for (JsonElement stageElement : ruleObj.getAsJsonArray("stages")) {
                            if (stageElement.isJsonObject()) {
                                JsonObject stageObj = stageElement.getAsJsonObject();
                                StageCondition condition = new StageCondition();
                                condition.stageType = stageObj.has("stage_type") ? stageObj.get("stage_type").getAsString() : "player";
                                condition.stage = stageObj.has("stage") ? stageObj.get("stage").getAsString() : "";
                                condition.is = !stageObj.has("is") || stageObj.get("is").getAsBoolean();
                                rule.stages.add(condition);
                            }
                        }
                    }
                    
                    // Parse container whitelist
                    String containerWhitelistKey = "containers_whitelist";
                    if (ruleObj.has(containerWhitelistKey)) {
                        rule.containersWhitelist = ruleObj.get(containerWhitelistKey).getAsBoolean();
                    } else {
                        rule.containersWhitelist = true; // Default
                    }
                    
                    // Parse container list
                    if (ruleObj.has("containers_list") && ruleObj.get("containers_list").isJsonArray()) {
                        for (JsonElement containerElement : ruleObj.getAsJsonArray("containers_list")) {
                            if (containerElement.isJsonPrimitive()) {
                                String containerClass = containerElement.getAsString();
                                rule.containersList.add(containerClass);
                            }
                        }
                    }
                    
                    // Parse item list
                    if (ruleObj.has("items") && ruleObj.get("items").isJsonArray()) {
                        for (JsonElement itemElement : ruleObj.getAsJsonArray("items")) {
                            if (itemElement.isJsonPrimitive()) {
                                String itemId = itemElement.getAsString();
                                rule.items.add(itemId);
                            }
                        }
                    }
                    // Parse exclude list (optional, default empty)
                    if (ruleObj.has("exclude") && ruleObj.get("exclude").isJsonArray()) {
                        for (JsonElement exElement : ruleObj.getAsJsonArray("exclude")) {
                            if (exElement.isJsonPrimitive()) {
                                rule.exclude.add(exElement.getAsString());
                            }
                        }
                    }
                    
                    // Parse consequence
                    rule.consequence = ruleObj.has("consequence") ? ruleObj.get("consequence").getAsString() : "drop";
                    
                    // Parse "if" conditions for DEF_OR and DEF_AND logics
                    if (("DEF_OR".equals(rule.stagesLogic) || "DEF_AND".equals(rule.stagesLogic)) 
                            && ruleObj.has("if") && ruleObj.get("if").isJsonArray()) {
                        for (JsonElement ifElement : ruleObj.getAsJsonArray("if")) {
                            if (ifElement.isJsonObject()) {
                                JsonObject ifObj = ifElement.getAsJsonObject();
                                StageItemIfRule ifRule = new StageItemIfRule();
                                
                                // Parse condition indices
                                if (ifObj.has("conditions") && ifObj.get("conditions").isJsonArray()) {
                                    for (JsonElement condElement : ifObj.getAsJsonArray("conditions")) {
                                        if (condElement.isJsonPrimitive()) {
                                            int conditionIndex = condElement.getAsInt();
                                            ifRule.conditions.add(conditionIndex);
                                        }
                                    }
                                }

                                if (ifObj.has("mod_conditions") && ifObj.get("mod_conditions").isJsonArray()) {
                                    for (JsonElement condElement : ifObj.getAsJsonArray("mod_conditions")) {
                                        if (condElement.isJsonPrimitive()) {
                                            ifRule.modConditions.add(condElement.getAsInt());
                                        }
                                    }
                                }
                                
                                // Parse container list
                                if (ifObj.has("containers_list") && ifObj.get("containers_list").isJsonArray()) {
                                    for (JsonElement containerElement : ifObj.getAsJsonArray("containers_list")) {
                                        if (containerElement.isJsonPrimitive()) {
                                            String containerClass = containerElement.getAsString();
                                            ifRule.containersList.add(containerClass);
                                        }
                                    }
                                }
                                
                                // Parse container whitelist
                                String ifContainerWhitelistKey = "containers_whitelist";
                                if (ifObj.has(ifContainerWhitelistKey)) {
                                    ifRule.containersWhitelist = ifObj.get(ifContainerWhitelistKey).getAsBoolean();
                                } else {
                                    ifRule.containersWhitelist = true; // Default
                                }
                                
                                // Parse other case
                                if (ifObj.has("other_case") && ifObj.get("other_case").isJsonArray()) {
                                    for (JsonElement otherCaseElement : ifObj.getAsJsonArray("other_case")) {
                                        if (otherCaseElement.isJsonPrimitive()) {
                                            String otherCase = otherCaseElement.getAsString();
                                            ifRule.otherCase.add(otherCase);
                                        }
                                    }
                                }
                                
                                // Parse item list
                                if (ifObj.has("items") && ifObj.get("items").isJsonArray()) {
                                    for (JsonElement itemElement : ifObj.getAsJsonArray("items")) {
                                        if (itemElement.isJsonPrimitive()) {
                                            String itemId = itemElement.getAsString();
                                            ifRule.items.add(itemId);
                                        }
                                    }
                                }
                                // Parse exclude list (optional)
                                if (ifObj.has("exclude") && ifObj.get("exclude").isJsonArray()) {
                                    for (JsonElement exElement : ifObj.getAsJsonArray("exclude")) {
                                        if (exElement.isJsonPrimitive()) {
                                            ifRule.exclude.add(exElement.getAsString());
                                        }
                                    }
                                }
                                
                                // Parse consequence
                                ifRule.consequence = ifObj.has("consequence") ? ifObj.get("consequence").getAsString() : rule.consequence;
                                expandOtherCaseAll(ifRule.otherCase);
                                
                                rule.ifConditions.add(ifRule);
                            }
                        }
                    }
                    
                    // Parse other case
                    if (ruleObj.has("other_case") && ruleObj.get("other_case").isJsonArray()) {
                        for (JsonElement otherCaseElement : ruleObj.getAsJsonArray("other_case")) {
                            if (otherCaseElement.isJsonPrimitive()) {
                                String otherCase = otherCaseElement.getAsString();
                                rule.otherCase.add(otherCase);
                            }
                        }
                    }

                    // Custom tooltips (optional). Entries are lang keys or literal text.
                    if (ruleObj.has("tooltips") && ruleObj.get("tooltips").isJsonArray()) {
                        for (JsonElement tip : ruleObj.getAsJsonArray("tooltips")) {
                            if (tip.isJsonPrimitive()) {
                                rule.tooltips.add(tip.getAsString());
                            }
                        }
                    }
                    rule.hideDefaultTooltips = ruleObj.has("hide_default_tooltips")
                            && ruleObj.get("hide_default_tooltips").getAsBoolean();
                    // hide_item_name defaults to true (Unfamiliar Item) when omitted
                    rule.hideItemName = !ruleObj.has("hide_item_name")
                            || ruleObj.get("hide_item_name").getAsBoolean();
                    if (ruleObj.has("hidden_item_name") && ruleObj.get("hidden_item_name").isJsonPrimitive()) {
                        rule.hiddenItemName = ruleObj.get("hidden_item_name").getAsString();
                    }
                    // Recipe-viewer presentation: overlay (default) | hardcore | hide
                    rule.recipeViewer = parseRecipeViewer(ruleObj);
                    expandOtherCaseAll(rule.otherCase);

                    restriction.restrictions.add(rule);
                }
            }
        }
        
        return restriction;
    }
    
    /**
     * Checks if an item should be blocked for right-click usage
     * @return true if the item should be blocked, false otherwise
     */
    public static boolean shouldBlockRightClick(Player player, ItemStack itemStack) {
        if (!(player instanceof ServerPlayer) || player.level().isClientSide() || itemStack.isEmpty()) {
            return false;
        }
        
        ServerPlayer serverPlayer = (ServerPlayer) player;
        Level level = player.level();
        
        boolean shouldBlock = checkOtherCaseRestriction(serverPlayer, level, itemStack, "right_click");
        
        // If we should block, provide visual feedback to the player
        if (shouldBlock) {
            // Display a message to the player
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.iska_utils.item_restriction.blocked"), true);
                
            // Invia un pacchetto al client per bloccare l'interazione
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(
                serverPlayer.blockPosition(), level.getBlockState(serverPlayer.blockPosition())));
                
            // Aggiorna lo stato dell'inventario per evitare desync
            serverPlayer.inventoryMenu.broadcastChanges();
        }
        
        return shouldBlock;
    }
    
    /**
     * Checks if an item should be blocked for left-click usage
     * @return true if the item should be blocked, false otherwise
     */
    public static boolean shouldBlockLeftClick(Player player, ItemStack itemStack) {
        if (!(player instanceof ServerPlayer) || player.level().isClientSide() || itemStack.isEmpty()) {
            return false;
        }
        
        ServerPlayer serverPlayer = (ServerPlayer) player;
        Level level = player.level();
        
        boolean shouldBlock = checkOtherCaseRestriction(serverPlayer, level, itemStack, "left_click");
        
        // If we should block, ensure the item attack damage is set to 0
        if (shouldBlock) {
            // Display a message to the player
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.iska_utils.item_restriction.blocked"), true);
                
            // Set attack damage to 0 to prevent damage
            // This is a temporary effect for this specific interaction
            serverPlayer.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                .setBaseValue(0);
            
            // Invia un pacchetto al client per sincronizzare lo stato
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(
                serverPlayer.getId(), serverPlayer.getEntityData().getNonDefaultValues()));
                
            // Aggiorna lo stato dell'inventario per evitare desync
            serverPlayer.inventoryMenu.broadcastChanges();
            
            // Schedule task to reset damage after this tick
            serverPlayer.level().getServer().execute(() -> {
                // Reset attack damage to default (1.0)
                serverPlayer.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                    .setBaseValue(1.0);
                    
                // Risincronizzo con il client dopo il reset
                serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(
                    serverPlayer.getId(), serverPlayer.getEntityData().getNonDefaultValues()));
            });
        }
        
        return shouldBlock;
    }
    
    /**
     * Checks if an item should be restricted in main hand
     * @return the consequence to apply, or null if no restriction
     */
    public static String checkMainHandRestriction(Player player, ItemStack itemStack) {
        if (!(player instanceof ServerPlayer) || player.level().isClientSide() || itemStack.isEmpty()) {
            return null;
        }
        
        ServerPlayer serverPlayer = (ServerPlayer) player;
        Level level = player.level();
        
        return getOtherCaseConsequence(serverPlayer, level, itemStack, "main_hand");
    }
    
    /**
     * Checks if an item should be restricted in off hand
     * @return the consequence to apply, or null if no restriction
     */
    public static String checkOffHandRestriction(Player player, ItemStack itemStack) {
        if (!(player instanceof ServerPlayer) || player.level().isClientSide() || itemStack.isEmpty()) {
            return null;
        }
        
        ServerPlayer serverPlayer = (ServerPlayer) player;
        Level level = player.level();
        
        return getOtherCaseConsequence(serverPlayer, level, itemStack, "off_hand");
    }
    
    /**
     * Checks if a specific other_case restriction applies
     * @return true if restricted, false otherwise
     */
    private static boolean checkOtherCaseRestriction(ServerPlayer player, Level level, ItemStack itemStack, String otherCaseType) {
        String consequence = getOtherCaseConsequence(player, level, itemStack, otherCaseType);
        return consequence != null;
    }
    
    /**
     * Gets the consequence to apply for a specific other_case
     * @return the consequence string or null if no restriction applies
     */
    private static String getOtherCaseConsequence(ServerPlayer player, Level level, ItemStack itemStack, String otherCaseType) {
        Item item = itemStack.getItem();
        
        for (StageItemRestriction restriction : allRestrictions()) {
            for (StageItemRule rule : restriction.restrictions) {
                // Check if this rule has other_case and if it includes the current type
                if (rule.otherCase.contains(otherCaseType)) {
                    // Check stage conditions
                    boolean stageConditionsMet = checkStageConditions(rule, player, level);
                    
                    if (!stageConditionsMet) {
                        continue;
                    }
                    
                    // Check if item is affected
                    if (isItemAffected(item, rule.items, rule.exclude)) {
                        return rule.consequence;
                    }
                }
                
                // If using DEF_OR or DEF_AND, check if conditions
                if (("DEF_OR".equals(rule.stagesLogic) || "DEF_AND".equals(rule.stagesLogic)) 
                        && rule.ifConditions != null && !rule.ifConditions.isEmpty()) {
                    
                    for (StageItemIfRule ifRule : rule.ifConditions) {
                        if (ifRule.otherCase.contains(otherCaseType)) {
                            // Check if conditions are met
                            if (checkIfRuleConditions(ifRule, rule, player, level)) {
                                // Check if item is affected
                                if (isItemAffected(item, ifRule.items, ifRule.exclude)) {
                                    return ifRule.consequence;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Applies a consequence to a player's hand item
     * @param hand The hand holding the item
     * @param consequence The consequence to apply
     * @return true if the item was affected, false otherwise
     */
    public static boolean applyHandConsequence(ServerPlayer player, InteractionHand hand, String consequence) {
        if (consequence == null) {
            return false;
        }
        
        ItemStack stack = player.getItemInHand(hand).copy();
        if (stack.isEmpty()) {
            return false;
        }
        
        switch (consequence.toLowerCase()) {
            case "drop":
            case "block_drop":
                player.drop(stack, false);
                player.setItemInHand(hand, ItemStack.EMPTY);
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.iska_utils.item_restriction.dropped"), true);
                return true;
                
            case "delete":
            case "block_delete":
                player.setItemInHand(hand, ItemStack.EMPTY);
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.iska_utils.item_restriction.deleted"), true);
                return true;
                
            case "block":
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.iska_utils.item_restriction.blocked"), true);
                return true;
                
            default:
                return false;
        }
    }
    

    /**
     * Returns loaded rules that reference the given item (id/tag/mod match), ignoring stage gates.
     * Used for client tooltips / indexing; enforcement still uses stage checks server-side.
     */
    public static List<StageItemRule> findRulesForItem(Item item) {
        List<StageItemRule> matched = new ArrayList<>();
        if (item == null || !hasLoadedRestrictions()) {
            return matched;
        }
        for (StageItemRestriction restriction : allRestrictions()) {
            if (restriction == null || restriction.restrictions == null) {
                continue;
            }
            for (StageItemRule rule : restriction.restrictions) {
                if (rule != null && isItemAffected(item, rule.items, rule.exclude)) {
                    matched.add(rule);
                }
            }
        }
        return matched;
    }

    /**
     * Rules that currently apply for this player/level (stage gates evaluated).
     * Works for {@link ServerPlayer} and client {@link Player} when stages are synced.
     */
    public static List<StageItemRule> findActiveRules(Player player, Level level, Item item) {
        List<StageItemRule> active = new ArrayList<>();
        if (player == null || level == null || item == null) {
            return active;
        }
        for (StageItemRule rule : findRulesForItem(item)) {
            if (ruleAppliesForPlayer(rule, player, level)) {
                active.add(rule);
            }
        }
        return active;
    }

    /** Whether a rule's stage conditions currently match the player. */
    public static boolean ruleAppliesForPlayer(StageItemRule rule, Player player, Level level) {
        if (rule == null || player == null || level == null) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return checkStageConditions(rule, serverPlayer, level);
        }
        // Client path: AND/OR only (DEF_* needs full server if-branches)
        if (rule.stages == null || rule.stages.isEmpty()) {
            return true;
        }
        boolean and = !"OR".equalsIgnoreCase(rule.stagesLogic);
        if (and) {
            for (StageCondition condition : rule.stages) {
                if (checkSingleStageConditionClient(condition, player, level) != condition.is) {
                    return false;
                }
            }
            return true;
        }
        for (StageCondition condition : rule.stages) {
            if (checkSingleStageConditionClient(condition, player, level) == condition.is) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkSingleStageConditionClient(StageCondition condition, Player player, Level level) {
        // Library static checks are server-only; use synced client cache for RV / tooltips.
        if ("world".equals(condition.stageType)) {
            return net.unfamily.anotherstages.client.ClientStageCache.hasWorldStage(condition.stage);
        }
        if ("team".equals(condition.stageType)) {
            return net.unfamily.anotherstages.client.ClientStageCache.hasTeamStage(condition.stage);
        }
        return net.unfamily.anotherstages.client.ClientStageCache.hasPlayerStage(condition.stage);
    }

    private static java.util.Collection<StageItemRestriction> allRestrictions() {
        if (SCRIPT_ITEM_RESTRICTIONS.isEmpty()) {
            return DATAPACK_ITEM_RESTRICTIONS.values();
        }
        if (DATAPACK_ITEM_RESTRICTIONS.isEmpty()) {
            return SCRIPT_ITEM_RESTRICTIONS.values();
        }
        java.util.ArrayList<StageItemRestriction> all =
                new java.util.ArrayList<>(DATAPACK_ITEM_RESTRICTIONS.size() + SCRIPT_ITEM_RESTRICTIONS.size());
        all.addAll(DATAPACK_ITEM_RESTRICTIONS.values());
        all.addAll(SCRIPT_ITEM_RESTRICTIONS.values());
        return all;
    }

    private static java.util.Set<Map.Entry<String, StageItemRestriction>> allRestrictionEntries() {
        java.util.LinkedHashMap<String, StageItemRestriction> merged =
                new java.util.LinkedHashMap<>(DATAPACK_ITEM_RESTRICTIONS);
        merged.putAll(SCRIPT_ITEM_RESTRICTIONS);
        return merged.entrySet();
    }

    public static void clearScriptRestrictions() {
        SCRIPT_ITEM_RESTRICTIONS.clear();
    }

    public static void putScriptRestriction(String id, StageItemRestriction restriction) {
        if (id == null || id.isBlank() || restriction == null) {
            return;
        }
        SCRIPT_ITEM_RESTRICTIONS.put(id, restriction);
    }

    public static boolean hasRestrictionId(String id) {
        return SCRIPT_ITEM_RESTRICTIONS.containsKey(id) || DATAPACK_ITEM_RESTRICTIONS.containsKey(id);
    }

    public static final String[] ALL_OTHER_CASES = {"right_click", "left_click", "main_hand", "off_hand"};

    public static void expandOtherCaseAll(List<String> otherCase) {
        if (otherCase == null || otherCase.isEmpty()) {
            return;
        }
        boolean hasAll = false;
        for (String s : otherCase) {
            if ("all".equalsIgnoreCase(s)) {
                hasAll = true;
                break;
            }
        }
        if (!hasAll) {
            return;
        }
        otherCase.clear();
        for (String s : ALL_OTHER_CASES) {
            otherCase.add(s);
        }
    }

    private static String parseRecipeViewer(JsonObject ruleObj) {
        if (ruleObj.has("recipe_viewer") && ruleObj.get("recipe_viewer").isJsonPrimitive()) {
            return ruleObj.get("recipe_viewer").getAsString().trim().toLowerCase();
        }
        return "overlay";
    }

    public static boolean hasLoadedRestrictions() {
        return !DATAPACK_ITEM_RESTRICTIONS.isEmpty() || !SCRIPT_ITEM_RESTRICTIONS.isEmpty();
    }

    /** Normalized RV mode for a rule ({@code overlay}, {@code hardcore}, {@code hide}). */
    public static String rvMode(StageItemRule rule) {
        if (rule == null || rule.recipeViewer == null || rule.recipeViewer.isBlank()) {
            return "overlay";
        }
        return switch (rule.recipeViewer.toLowerCase()) {
            case "hide" -> "hide";
            case "hardcore" -> "hardcore";
            default -> "overlay";
        };
    }

    // Classes to represent JSON structures
    
    /**
     * Represents a complete item restriction
     */
    public static class StageItemRestriction {
        public List<StageItemRule> restrictions = new ArrayList<>();
    }
    
    /**
     * Represents a restriction rule
     */
    public static class StageItemRule {
        public String stagesLogic = "AND";
        public List<StageCondition> stages = new ArrayList<>();
        public List<LoadModCondition> mods = new ArrayList<>();
        public StagesLogic modsLogic = StagesLogic.DEF_AND;
        public boolean containersWhitelist = true;
        public List<String> containersList = new ArrayList<>();
        public List<String> items = new ArrayList<>();
        /** Exempt items: same format as items (id, #tag, @modid). Default empty. */
        public List<String> exclude = new ArrayList<>();
        public String consequence = "drop";
        public List<StageItemIfRule> ifConditions = new ArrayList<>();
        public List<String> otherCase = new ArrayList<>();
        /** Optional custom tooltip lines (lang key or literal). Empty if omitted. */
        public List<String> tooltips = new ArrayList<>();
        /** When true, default item tooltips are cleared before custom ones are added. */
        public boolean hideDefaultTooltips = false;
        /**
         * When true (default), replace the display name with Unfamiliar Item / Fish (or
         * {@link #hiddenItemName} / {@link #hiddenItemNameComponent}) while the rule is active
         * and {@code recipe_viewer} is overlay or hardcore.
         */
        public boolean hideItemName = true;
        /**
         * Optional custom hidden display name (lang key or literal), same rules as tooltip strings.
         * Used when {@link #hideItemName} is true; empty = default Unfamiliar Item/Fish.
         */
        public String hiddenItemName = "";
        /** KubeJS Component override for the hidden display name (takes priority over the string). */
        public net.minecraft.network.chat.Component hiddenItemNameComponent = null;
        /**
         * Recipe-viewer / client presentation when this rule is active:
         * {@code overlay} (default), {@code hardcore}, {@code hide}.
         */
        public String recipeViewer = "overlay";
        /** KubeJS Component tooltips (in addition to string tooltips). */
        public java.util.List<net.minecraft.network.chat.Component> componentTooltips = new java.util.ArrayList<>();
    }
    
    /**
     * Represents an if condition for DEF_OR logic
     */
    public static class StageItemIfRule {
        public List<Integer> conditions = new ArrayList<>();
        public List<Integer> modConditions = new ArrayList<>();
        public List<String> containersList = new ArrayList<>();
        public List<String> items = new ArrayList<>();
        /** Exempt items: same format as items. Default empty. */
        public List<String> exclude = new ArrayList<>();
        public String consequence = "drop";
        public boolean containersWhitelist = true;
        public List<String> otherCase = new ArrayList<>();
    }
    
    /**
     * Represents a stage condition
     */
    public static class StageCondition {
        public String stageType = "player";
        public String stage = "";
        public boolean is = true;
    }
} 