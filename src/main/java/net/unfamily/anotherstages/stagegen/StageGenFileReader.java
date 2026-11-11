package net.unfamily.anotherstages.stagegen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.client.gui.stagegen.KnownStageDraft;
import net.unfamily.anotherstages.client.gui.stagegen.RestrictionDraft;
import net.unfamily.anotherstages.config.AsConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads Stage Generator drafts from fixed JSON or KubeJS Entry-N files (by export preference).
 */
public final class StageGenFileReader {
    private StageGenFileReader() {}

    /** Open-GUI load: Kube when preferred, otherwise JSON. */
    public static List<RestrictionDraft> loadAllForOpen() {
        if (AsConfig.preferKubejs()) {
            return StageGenKubeReader.loadAllKube();
        }
        return loadAllJson();
    }

    /** Known-stages catalog for Stage Generator (JSON or Kube preference). */
    public static List<KnownStageDraft> loadKnownStagesForOpen() {
        if (AsConfig.preferKubejs()) {
            return loadKnownStagesKube();
        }
        return loadKnownStagesJson();
    }

    public static List<KnownStageDraft> loadKnownStagesJson() {
        List<KnownStageDraft> out = new ArrayList<>();
        Path file = StageGenPaths.resolveKnownStagesJsonFile();
        if (file == null || !Files.isRegularFile(file)) {
            return out;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("stages") || !root.get("stages").isJsonArray()) {
                return out;
            }
            for (JsonElement el : root.getAsJsonArray("stages")) {
                if (el.isJsonObject()) {
                    out.add(KnownStageDraft.fromJsonObject(el.getAsJsonObject()));
                }
            }
        } catch (Exception ex) {
            AnotherStages.LOGGER.warn("Failed to load known stages file {}: {}", file, ex.toString());
        }
        return out;
    }

    public static List<KnownStageDraft> loadKnownStagesKube() {
        List<KnownStageDraft> out = new ArrayList<>();
        Path file = StageGenPaths.resolveKnownStagesKubeFile();
        if (!Files.isRegularFile(file)) {
            return out;
        }
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            // AnotherStages.registerStage('player', 'bronze_age')
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "AnotherStages\\.registerStage\\(\\s*['\"]([^'\"]+)['\"]\\s*,\\s*['\"]([^'\"]+)['\"]\\s*\\)");
            java.util.regex.Matcher m = p.matcher(raw);
            while (m.find()) {
                out.add(new KnownStageDraft(m.group(1), m.group(2)));
            }
        } catch (Exception ex) {
            AnotherStages.LOGGER.warn("Failed to load known stages kube file {}: {}", file, ex.toString());
        }
        return out;
    }

    public static List<RestrictionDraft> loadAllJson() {
        List<RestrictionDraft> out = new ArrayList<>();
        for (RestrictionDraft.Category cat : RestrictionDraft.Category.values()) {
            out.addAll(loadCategory(cat));
        }
        return out;
    }

    public static List<RestrictionDraft> loadCategory(RestrictionDraft.Category category) {
        List<RestrictionDraft> out = new ArrayList<>();
        Path file = StageGenPaths.resolveJsonFile(category);
        if (file == null || !Files.isRegularFile(file)) {
            return out;
        }
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            if (!root.has("restrictions") || !root.get("restrictions").isJsonArray()) {
                return out;
            }
            JsonArray arr = root.getAsJsonArray("restrictions");
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) {
                    continue;
                }
                RestrictionDraft draft = fromRestrictionObject(category, el.getAsJsonObject());
                if (draft != null) {
                    out.add(draft);
                }
            }
        } catch (Exception ex) {
            AnotherStages.LOGGER.warn("Failed to load stage generator file {}: {}", file, ex.toString());
        }
        return out;
    }

    /**
     * Best-effort map from on-disk restriction JSON into a mutable draft.
     * Fields the generator does not edit are filled with defaults.
     */
    public static RestrictionDraft fromRestrictionObject(RestrictionDraft.Category category, JsonObject o) {
        RestrictionDraft d = RestrictionDraft.createDefaults(category);
        d.stages.clear();
        d.matchers.clear();
        d.exclude.clear();
        d.tooltips.clear();
        d.recipes.clear();
        d.recipeTypes.clear();
        d.inputs.clear();
        d.excludeRecipes.clear();
        d.menus.clear();
        d.menuTypes.clear();
        d.blockedSlots.clear();
        d.blockedItems.clear();

        if (o.has("id")) {
            d.id = o.get("id").getAsString();
        }
        d.ensureId();
        if (o.has("stages_logic")) {
            d.stagesLogic = o.get("stages_logic").getAsString();
        }
        if (o.has("stages") && o.get("stages").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("stages")) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject s = el.getAsJsonObject();
                String type = s.has("stage_type") ? s.get("stage_type").getAsString() : "player";
                String id = s.has("stage") ? s.get("stage").getAsString() : "";
                boolean is = s.has("is") && s.get("is").getAsBoolean();
                d.stages.add(new RestrictionDraft.StageRow(type, id, is));
            }
        }
        if (d.stages.isEmpty()) {
            d.stages.add(new RestrictionDraft.StageRow());
        }

        readStringList(o, "items", d.matchers);
        readStringList(o, "blocks", d.matchers);
        readStringList(o, "outputs", d.matchers);
        readStringList(o, "exclude", d.exclude);
        readStringList(o, "tooltips", d.tooltips);
        readStringList(o, "recipes", d.recipes);
        readStringList(o, "recipe_types", d.recipeTypes);
        readStringList(o, "inputs", d.inputs);
        readStringList(o, "exclude_recipes", d.excludeRecipes);
        readStringList(o, "menus", d.menus);
        readStringList(o, "menu_types", d.menuTypes);
        readStringList(o, "blocked_items", d.blockedItems);
        if (o.has("blocked_slots") && o.get("blocked_slots").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("blocked_slots")) {
                d.blockedSlots.add(el.getAsInt());
            }
        }
        if (o.has("cases") && o.get("cases").isJsonArray()) {
            d.caseRight = false;
            d.caseLeft = false;
            d.caseMain = false;
            d.caseOff = false;
            d.caseInventory = false;
            d.casesAll = false;
            for (JsonElement el : o.getAsJsonArray("cases")) {
                String c = el.getAsString();
                switch (c) {
                    case "right_click" -> d.caseRight = true;
                    case "left_click" -> d.caseLeft = true;
                    case "main_hand" -> d.caseMain = true;
                    case "off_hand" -> d.caseOff = true;
                    case "inventory" -> d.caseInventory = true;
                    case "all" -> d.casesAll = true;
                    default -> {
                    }
                }
            }
        }
        if (o.has("inventory_ticks")) {
            d.inventoryTicks = o.get("inventory_ticks").getAsInt();
        }
        if (o.has("can_pickup")) {
            d.canPickup = o.get("can_pickup").getAsBoolean();
        }
        if (o.has("consequence")) {
            d.consequence = o.get("consequence").getAsString();
        }
        if (o.has("recipe_viewer")) {
            d.recipeViewer = o.get("recipe_viewer").getAsString();
        }
        if (o.has("hide_item_name")) {
            d.hideItemName = o.get("hide_item_name").getAsBoolean();
        }
        if (o.has("hide_default_tooltips")) {
            d.hideDefaultTooltips = o.get("hide_default_tooltips").getAsBoolean();
        }
        if (o.has("mimic")) {
            d.mimic = o.get("mimic").getAsString();
        }
        if (o.has("camouflage")) {
            d.camouflage = o.get("camouflage").getAsBoolean();
        }
        if (o.has("unbreakable")) {
            d.unbreakable = o.get("unbreakable").getAsBoolean();
        }
        if (o.has("stop_tick")) {
            d.stopTick = o.get("stop_tick").getAsBoolean();
        }
        if (o.has("break_message")) {
            d.breakMessageDefault = true;
        }
        if (o.has("interact_message")) {
            d.interactMessageDefault = true;
        }
        if (o.has("block_open")) {
            d.blockOpen = o.get("block_open").getAsBoolean();
        }
        if (o.has("max_slots")) {
            d.maxSlots = o.get("max_slots").getAsInt();
        }
        if (o.has("message")) {
            d.guiMessageDefault = true;
        }
        if (o.has("overlay_corner")) {
            d.overlayCorner = o.get("overlay_corner").getAsString();
        }
        return d;
    }

    private static void readStringList(JsonObject o, String key, List<String> target) {
        if (!o.has(key) || !o.get(key).isJsonArray()) {
            return;
        }
        for (JsonElement el : o.getAsJsonArray(key)) {
            String s = el.getAsString();
            if (!target.contains(s)) {
                target.add(s);
            }
        }
    }
}
