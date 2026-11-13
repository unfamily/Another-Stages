package net.unfamily.anotherstages.client.gui.stagegen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Mutable draft for one Stage Generator restriction (v2: multi-row stages / matchers).
 */
public final class RestrictionDraft {
    public enum Category {
        ITEM,
        RECIPE,
        BLOCK,
        GUI
    }

    public enum TableKind {
        STAGES,
        MATCHERS,
        EXCLUDE,
        TOOLTIPS,
        BLOCKED_SLOTS,
        BLOCKED_ITEMS,
        RECIPES,
        RECIPE_TYPES,
        INPUTS,
        EXCLUDE_RECIPES,
        MENUS,
        MENU_TYPES
    }

    public static final class StageRow {
        public String type = "player";
        public String id = "";
        public boolean is = false;

        public StageRow() {}

        public StageRow(String type, String id, boolean is) {
            this.type = type == null || type.isBlank() ? "player" : type;
            this.id = id == null ? "" : id;
            this.is = is;
        }

        public StageRow copy() {
            return new StageRow(type, id, is);
        }
    }

    public Category category = Category.ITEM;

    /** Silent id for JSON merge only — never shown or edited in the GUI. */
    public String id = "";

    /**
     * Index of this draft inside the category Kube {@code //Entry N} file.
     * {@code -1} = new entry not yet written.
     */
    public int kubeEntryIndex = -1;

    public String stagesLogic = "AND";
    /** Higher wins when several rules match; default {@code 0}. */
    public int priority = 0;
    public final List<StageRow> stages = new ArrayList<>();

    public final List<String> matchers = new ArrayList<>();
    public final List<String> exclude = new ArrayList<>();

    // Items
    public boolean casesAll = false;
    public boolean caseRight = true;
    public boolean caseLeft = false;
    public boolean caseMain = false;
    public boolean caseOff = false;
    public boolean caseInventory = false;
    public int inventoryTicks = 30;
    public boolean canPickup = true;
    public boolean pickupMessageDefault = false;
    public String pickupMessage = "";
    public String consequence = "drop";
    public String recipeViewer = "overlay";
    public boolean hideItemName = true;
    public final List<String> tooltips = new ArrayList<>();
    public boolean hideDefaultTooltips = false;

    // Recipes
    public final List<String> recipes = new ArrayList<>();
    public final List<String> recipeTypes = new ArrayList<>();
    public final List<String> inputs = new ArrayList<>();
    public final List<String> excludeRecipes = new ArrayList<>();
    public String overlayCorner = "top_right";

    // Blocks
    public String mimic = "minecraft:stone";
    public boolean camouflage = true;
    public boolean unbreakable = false;
    public boolean mimicMining = true;
    public boolean stopTick = false;
    public boolean breakMessageDefault = false;
    public boolean interactMessageDefault = false;

    // Guis
    public final List<String> menus = new ArrayList<>();
    public final List<String> menuTypes = new ArrayList<>();
    public boolean blockOpen = true;
    public final List<Integer> blockedSlots = new ArrayList<>();
    public final List<String> blockedItems = new ArrayList<>();
    public int maxSlots = 0;
    public boolean guiMessageDefault = true;
    public String guiMessage = "";

    public RestrictionDraft() {
        stages.add(new StageRow("player", "", false));
        ensureId();
    }

    /** New draft with category-specific autonomous defaults (ShopEdit-style New). */
    public static RestrictionDraft createDefaults(Category category) {
        RestrictionDraft d = new RestrictionDraft();
        d.category = category == null ? Category.ITEM : category;
        d.kubeEntryIndex = -1;
        d.ensureId();
        switch (d.category) {
            case ITEM -> {
                d.caseRight = true;
                d.canPickup = true;
                d.consequence = "drop";
                d.recipeViewer = "overlay";
                d.hideItemName = true;
            }
            case RECIPE -> d.recipeViewer = "overlay";
            case BLOCK -> {
                d.camouflage = true;
                d.mimicMining = true;
                d.mimic = "minecraft:stone";
            }
            case GUI -> {
                d.blockOpen = true;
                d.guiMessageDefault = true;
            }
        }
        return d;
    }

    public void ensureId() {
        if (id == null || id.isBlank()) {
            id = "stagegen/" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
    }

    public void addMatcherFromItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        switch (category) {
            case ITEM, BLOCK -> {
                if (!matchers.contains(itemId)) {
                    matchers.add(itemId);
                }
            }
            case RECIPE -> {
                if (!matchers.contains(itemId)) {
                    matchers.add(itemId);
                }
            }
            case GUI -> {
                if (!blockedItems.contains(itemId)) {
                    blockedItems.add(itemId);
                }
            }
        }
    }

    public List<String> caseList() {
        List<String> out = new ArrayList<>();
        if (casesAll) {
            out.add("right_click");
            out.add("left_click");
            out.add("main_hand");
            out.add("off_hand");
            out.add("inventory");
            return out;
        }
        if (caseRight) {
            out.add("right_click");
        }
        if (caseLeft) {
            out.add("left_click");
        }
        if (caseMain) {
            out.add("main_hand");
        }
        if (caseOff) {
            out.add("off_hand");
        }
        if (caseInventory) {
            out.add("inventory");
        }
        return out;
    }

    public JsonObject toJson() {
        ensureId();
        JsonObject root = new JsonObject();
        root.addProperty("category", category.name());
        root.addProperty("id", id);
        root.addProperty("kubeEntryIndex", kubeEntryIndex);
        root.addProperty("stagesLogic", stagesLogic);
        root.addProperty("priority", priority);
        JsonArray stagesArr = new JsonArray();
        for (StageRow row : stages) {
            JsonObject o = new JsonObject();
            o.addProperty("type", row.type);
            o.addProperty("id", row.id);
            o.addProperty("is", row.is);
            stagesArr.add(o);
        }
        root.add("stages", stagesArr);
        root.add("matchers", stringArray(matchers));
        root.add("exclude", stringArray(exclude));
        root.addProperty("casesAll", casesAll);
        root.addProperty("caseRight", caseRight);
        root.addProperty("caseLeft", caseLeft);
        root.addProperty("caseMain", caseMain);
        root.addProperty("caseOff", caseOff);
        root.addProperty("caseInventory", caseInventory);
        root.addProperty("inventoryTicks", inventoryTicks);
        root.addProperty("canPickup", canPickup);
        root.addProperty("pickupMessageDefault", pickupMessageDefault);
        root.addProperty("pickupMessage", pickupMessage);
        root.addProperty("consequence", consequence);
        root.addProperty("recipeViewer", recipeViewer);
        root.addProperty("hideItemName", hideItemName);
        root.add("tooltips", stringArray(tooltips));
        root.addProperty("hideDefaultTooltips", hideDefaultTooltips);
        root.add("recipes", stringArray(recipes));
        root.add("recipeTypes", stringArray(recipeTypes));
        root.add("inputs", stringArray(inputs));
        root.add("excludeRecipes", stringArray(excludeRecipes));
        root.addProperty("overlayCorner", overlayCorner);
        root.addProperty("mimic", mimic);
        root.addProperty("camouflage", camouflage);
        root.addProperty("unbreakable", unbreakable);
        root.addProperty("mimicMining", mimicMining);
        root.addProperty("stopTick", stopTick);
        root.addProperty("breakMessageDefault", breakMessageDefault);
        root.addProperty("interactMessageDefault", interactMessageDefault);
        root.add("menus", stringArray(menus));
        root.add("menuTypes", stringArray(menuTypes));
        root.addProperty("blockOpen", blockOpen);
        JsonArray slots = new JsonArray();
        for (Integer s : blockedSlots) {
            slots.add(s);
        }
        root.add("blockedSlots", slots);
        root.add("blockedItems", stringArray(blockedItems));
        root.addProperty("maxSlots", maxSlots);
        root.addProperty("guiMessageDefault", guiMessageDefault);
        root.addProperty("guiMessage", guiMessage);
        return root;
    }

    public static RestrictionDraft fromJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        RestrictionDraft d = new RestrictionDraft();
        d.stages.clear();
        d.category = Category.valueOf(root.get("category").getAsString());
        d.id = str(root, "id", "");
        d.kubeEntryIndex = root.has("kubeEntryIndex") ? root.get("kubeEntryIndex").getAsInt() : -1;
        d.stagesLogic = str(root, "stagesLogic", "AND");
        d.priority = root.has("priority") ? root.get("priority").getAsInt() : 0;
        if (root.has("stages") && root.get("stages").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("stages")) {
                JsonObject o = el.getAsJsonObject();
                d.stages.add(new StageRow(str(o, "type", "player"), str(o, "id", ""), bool(o, "is", false)));
            }
        }
        if (d.stages.isEmpty()) {
            d.stages.add(new StageRow());
        }
        readStrings(root, "matchers", d.matchers);
        readStrings(root, "exclude", d.exclude);
        d.casesAll = bool(root, "casesAll", false);
        d.caseRight = bool(root, "caseRight", true);
        d.caseLeft = bool(root, "caseLeft", false);
        d.caseMain = bool(root, "caseMain", false);
        d.caseOff = bool(root, "caseOff", false);
        d.caseInventory = bool(root, "caseInventory", false);
        d.inventoryTicks = root.has("inventoryTicks") ? root.get("inventoryTicks").getAsInt() : 30;
        d.canPickup = bool(root, "canPickup", true);
        d.pickupMessageDefault = bool(root, "pickupMessageDefault", false);
        d.pickupMessage = str(root, "pickupMessage", "");
        d.consequence = str(root, "consequence", "drop");
        d.recipeViewer = str(root, "recipeViewer", "overlay");
        d.hideItemName = bool(root, "hideItemName", true);
        readStrings(root, "tooltips", d.tooltips);
        d.hideDefaultTooltips = bool(root, "hideDefaultTooltips", false);
        readStrings(root, "recipes", d.recipes);
        readStrings(root, "recipeTypes", d.recipeTypes);
        readStrings(root, "inputs", d.inputs);
        readStrings(root, "excludeRecipes", d.excludeRecipes);
        d.overlayCorner = str(root, "overlayCorner", "top_right");
        d.mimic = str(root, "mimic", "minecraft:stone");
        d.camouflage = bool(root, "camouflage", true);
        d.unbreakable = bool(root, "unbreakable", false);
        d.mimicMining = bool(root, "mimicMining", true);
        d.stopTick = bool(root, "stopTick", false);
        d.breakMessageDefault = bool(root, "breakMessageDefault", false);
        d.interactMessageDefault = bool(root, "interactMessageDefault", false);
        readStrings(root, "menus", d.menus);
        readStrings(root, "menuTypes", d.menuTypes);
        d.blockOpen = bool(root, "blockOpen", true);
        d.blockedSlots.clear();
        if (root.has("blockedSlots") && root.get("blockedSlots").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("blockedSlots")) {
                d.blockedSlots.add(el.getAsInt());
            }
        }
        readStrings(root, "blockedItems", d.blockedItems);
        d.maxSlots = root.has("maxSlots") ? root.get("maxSlots").getAsInt() : 0;
        d.guiMessageDefault = bool(root, "guiMessageDefault", true);
        d.guiMessage = str(root, "guiMessage", "");
        d.ensureId();
        return d;
    }

    private static JsonArray stringArray(List<String> values) {
        JsonArray arr = new JsonArray();
        for (String v : values) {
            arr.add(v);
        }
        return arr;
    }

    private static void readStrings(JsonObject root, String key, List<String> target) {
        target.clear();
        if (!root.has(key) || !root.get(key).isJsonArray()) {
            return;
        }
        for (JsonElement el : root.getAsJsonArray(key)) {
            target.add(el.getAsString());
        }
    }

    private static String str(JsonObject o, String key, String def) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : def;
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsBoolean() : def;
    }

    public static String normalizeLogic(String logic) {
        if (logic == null || logic.isBlank()) {
            return "AND";
        }
        return logic.trim().toUpperCase(Locale.ROOT);
    }
}
