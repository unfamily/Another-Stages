package net.unfamily.anotherstages.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
 * Loads {@code another_stages:stage_gui} rules and evaluates menu open / slot gates.
 */
public final class StageGuiHandler {
    public static final String TYPE_STAGE_GUI = "another_stages:stage_gui";
    private static final Set<String> ACCEPTED_TYPES = Set.of(TYPE_STAGE_GUI);
    private static final Logger LOGGER = LoggerFactory.getLogger(StageGuiHandler.class);

    private static final List<StageGuiRule> DATAPACK_RULES = new ArrayList<>();
    private static final Map<String, StageGuiRule> SCRIPT_RULES = new HashMap<>();

    private StageGuiHandler() {}

    public static void loadAll(@Nullable ResourceManager resourceManagerOrNull) {
        DATAPACK_RULES.clear();
        try {
            Map<Identifier, JsonElement> merged = resourceManagerOrNull != null
                    ? LoadJson.collectMergedJsonForTypes(resourceManagerOrNull, ACCEPTED_TYPES)
                    : LoadJson.collectFromModJar("another_stages", "another_stages", "another_stages_guis");
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
                    if (!LoadModGate.shouldIncludeAtLoad(ruleObj, LOGGER, "stage_gui_rule")) {
                        continue;
                    }
                    DATAPACK_RULES.add(parseRule(ruleObj));
                }
            }
            LOGGER.info("Loaded {} stage gui rules ({} script)", ruleCount(), SCRIPT_RULES.size());
        } catch (Exception e) {
            LOGGER.error("Error loading stage gui rules: {}", e.getMessage());
        }
    }

    public static void clearScriptRules() {
        SCRIPT_RULES.clear();
    }

    public static void putScriptRule(String id, StageGuiRule rule) {
        if (id == null || id.isBlank() || rule == null) {
            return;
        }
        SCRIPT_RULES.put(id, rule);
    }

    public static boolean hasRuleId(String id) {
        return SCRIPT_RULES.containsKey(id);
    }

    public static boolean hasLoadedRules() {
        return ruleCount() > 0;
    }

    private static int ruleCount() {
        return DATAPACK_RULES.size() + SCRIPT_RULES.size();
    }

    private static Iterable<StageGuiRule> allRules() {
        if (SCRIPT_RULES.isEmpty()) {
            return DATAPACK_RULES;
        }
        ArrayList<StageGuiRule> all = new ArrayList<>(ruleCount());
        all.addAll(DATAPACK_RULES);
        all.addAll(SCRIPT_RULES.values());
        return all;
    }

    public static Set<String> collectReferencedStages() {
        Set<String> stages = new HashSet<>();
        for (StageGuiRule rule : allRules()) {
            for (StageCondition c : rule.stages) {
                if (c.stage != null && !c.stage.isBlank()) {
                    stages.add(c.stage.trim());
                }
            }
        }
        return stages;
    }

    private static StageGuiRule parseRule(JsonObject ruleObj) {
        StageGuiRule rule = new StageGuiRule();
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
        readStringList(ruleObj, "menus", rule.menus);
        readStringList(ruleObj, "menu", rule.menus);
        readStringList(ruleObj, "menu_types", rule.menuTypes);
        readStringList(ruleObj, "menu_type", rule.menuTypes);
        if (ruleObj.has("message") && ruleObj.get("message").isJsonPrimitive()) {
            rule.message = ruleObj.get("message").getAsString();
        }
        rule.blockOpen = !ruleObj.has("block_open") || ruleObj.get("block_open").getAsBoolean();
        if (ruleObj.has("blocked_slots") && ruleObj.get("blocked_slots").isJsonArray()) {
            for (JsonElement el : ruleObj.getAsJsonArray("blocked_slots")) {
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                    rule.blockedSlots.add(el.getAsInt());
                }
            }
        }
        readStringList(ruleObj, "blocked_items", rule.blockedItems);
        if (ruleObj.has("max_slots") && ruleObj.get("max_slots").isJsonPrimitive()) {
            rule.maxSlots = Math.max(0, ruleObj.get("max_slots").getAsInt());
        }
        return rule;
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

    public static List<StageGuiRule> findActiveRules(ServerPlayer player, AbstractContainerMenu menu) {
        List<StageGuiRule> active = new ArrayList<>();
        if (player == null || menu == null || ruleCount() == 0) {
            return active;
        }
        String className = menu.getClass().getName();
        String typeId = menuTypeId(menu);
        for (StageGuiRule rule : allRules()) {
            if (!menuMatches(rule, className, typeId)) {
                continue;
            }
            if (ruleApplies(rule, player, player.level())) {
                active.add(rule);
            }
        }
        return active;
    }

    private static String menuTypeId(AbstractContainerMenu menu) {
        try {
            MenuType<?> type = menu.getType();
            Identifier key = BuiltInRegistries.MENU.getKey(type);
            return key != null ? key.toString() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean menuMatches(StageGuiRule rule, String className, String typeId) {
        boolean hasMenu = !rule.menus.isEmpty();
        boolean hasType = !rule.menuTypes.isEmpty();
        if (!hasMenu && !hasType) {
            return false;
        }
        boolean menuOk = !hasMenu || rule.menus.contains(className);
        boolean typeOk = !hasType || (!typeId.isEmpty() && rule.menuTypes.contains(typeId));
        if (hasMenu && hasType) {
            return menuOk || typeOk;
        }
        return hasMenu ? menuOk : typeOk;
    }

    private static boolean ruleApplies(StageGuiRule rule, ServerPlayer player, Level level) {
        if (rule.stages.isEmpty()) {
            return true;
        }
        boolean and = !"OR".equalsIgnoreCase(rule.stagesLogic);
        if (and) {
            for (StageCondition condition : rule.stages) {
                if (checkCondition(condition, player, level) != condition.is) {
                    return false;
                }
            }
            return true;
        }
        for (StageCondition condition : rule.stages) {
            if (checkCondition(condition, player, level) == condition.is) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkCondition(StageCondition condition, ServerPlayer player, Level level) {
        String type = condition.stageType == null ? "player" : condition.stageType;
        return switch (type) {
            case "world" -> level != null && StageRegistry.worldHasStage(level, condition.stage);
            case "team" -> StageRegistry.getInstance(player.level().getServer())
                    .hasPlayerTeamStage(player, condition.stage);
            default -> StageRegistry.playerHasStage(player, condition.stage);
        };
    }

    public static Component resolveMessage(StageGuiRule rule) {
        if (rule == null || rule.message == null || rule.message.isBlank()) {
            return Component.translatable("message.another_stages.gui_blocked");
        }
        String raw = rule.message.trim();
        if (raw.indexOf(' ') < 0 && raw.indexOf('.') >= 0) {
            return Component.translatable(raw);
        }
        return Component.literal(raw);
    }

    public static boolean shouldBlockOpen(List<StageGuiRule> active) {
        for (StageGuiRule rule : active) {
            if (rule.blockOpen) {
                return true;
            }
        }
        return false;
    }

    public static StageGuiRule firstBlockOpenMessage(List<StageGuiRule> active) {
        for (StageGuiRule rule : active) {
            if (rule.blockOpen) {
                return rule;
            }
        }
        return active.isEmpty() ? null : active.getFirst();
    }

    public static boolean isSlotBlocked(List<StageGuiRule> active, int slotIndex) {
        for (StageGuiRule rule : active) {
            if (rule.blockedSlots.contains(slotIndex)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isItemBlockedInMenu(List<StageGuiRule> active, Item item) {
        for (StageGuiRule rule : active) {
            if (itemMatchesList(item, rule.blockedItems)) {
                return true;
            }
        }
        return false;
    }

    public static void enforceMaxSlots(ServerPlayer player, AbstractContainerMenu menu, List<StageGuiRule> active) {
        int max = 0;
        for (StageGuiRule rule : active) {
            if (rule.maxSlots > 0) {
                max = max == 0 ? rule.maxSlots : Math.min(max, rule.maxSlots);
            }
        }
        if (max <= 0) {
            return;
        }
        List<Integer> contentSlots = new ArrayList<>();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (isPlayerInventorySlot(slot, player)) {
                continue;
            }
            if (!slot.getItem().isEmpty()) {
                contentSlots.add(i);
            }
        }
        if (contentSlots.size() <= max) {
            return;
        }
        for (int i = contentSlots.size() - 1; i >= max; i--) {
            int slotIndex = contentSlots.get(i);
            ItemStack stack = menu.slots.get(slotIndex).getItem().copy();
            menu.slots.get(slotIndex).set(ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
        menu.broadcastChanges();
    }

    public static void ejectBlockedItems(ServerPlayer player, AbstractContainerMenu menu, List<StageGuiRule> active) {
        boolean changed = false;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (isPlayerInventorySlot(slot, player)) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            if (isItemBlockedInMenu(active, stack.getItem()) || isSlotBlocked(active, i)) {
                ItemStack copy = stack.copy();
                slot.set(ItemStack.EMPTY);
                player.drop(copy, false);
                changed = true;
            }
        }
        if (changed) {
            menu.broadcastChanges();
        }
    }

    private static boolean isPlayerInventorySlot(Slot slot, Player player) {
        return slot.container == player.getInventory();
    }

    private static boolean itemMatchesList(Item item, List<String> list) {
        if (list == null || list.isEmpty() || item == null) {
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
}
