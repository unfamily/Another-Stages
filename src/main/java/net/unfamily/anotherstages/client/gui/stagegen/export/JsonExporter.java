package net.unfamily.anotherstages.client.gui.stagegen.export;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.unfamily.anotherstages.block.StageBlockRule;
import net.unfamily.anotherstages.client.gui.stagegen.RestrictionDraft;
import net.unfamily.anotherstages.gui.StageGuiRule;
import net.unfamily.anotherstages.item.StageItemHandler;
import net.unfamily.anotherstages.stagegen.StageGenPaths;

import java.util.List;

/** Builds a restriction object (and optional bundled file JSON) for Stage Generator. */
public final class JsonExporter {
    private JsonExporter() {}

    /** Full bundled file body with a single restriction (preview / first write). */
    public static String export(RestrictionDraft d) {
        JsonObject root = new JsonObject();
        root.addProperty("type", StageGenPaths.jsonType(d.category));
        JsonArray restrictions = new JsonArray();
        restrictions.add(restrictionObject(d));
        root.add("restrictions", restrictions);
        return ExportUtil.GSON.toJson(root) + "\n";
    }

    public static JsonObject restrictionObject(RestrictionDraft d) {
        d.ensureId();
        return switch (d.category) {
            case ITEM -> item(d);
            case RECIPE -> recipe(d);
            case BLOCK -> block(d);
            case GUI -> gui(d);
        };
    }

    private static JsonObject item(RestrictionDraft d) {
        JsonObject o = base(d);
        putStrings(o, "items", d.matchers);
        putStrings(o, "exclude", d.exclude);
        List<String> cases = d.caseList();
        if (!cases.isEmpty()) {
            putStrings(o, "cases", cases);
        }
        if (cases.contains(StageItemHandler.CASE_INVENTORY) || d.casesAll) {
            o.addProperty("inventory_ticks", Math.max(1, d.inventoryTicks));
        }
        o.addProperty("can_pickup", d.canPickup);
        if (!d.canPickup) {
            if (d.pickupMessageDefault) {
                o.addProperty("pickup_message", StageItemHandler.DEFAULT_PICKUP_MESSAGE);
            } else if (!d.pickupMessage.isBlank()) {
                o.addProperty("pickup_message", d.pickupMessage);
            }
        }
        o.addProperty("consequence", d.consequence);
        o.addProperty("recipe_viewer", d.recipeViewer);
        o.addProperty("hide_item_name", d.hideItemName);
        putStrings(o, "tooltips", d.tooltips);
        o.addProperty("hide_default_tooltips", d.hideDefaultTooltips);
        return o;
    }

    private static JsonObject recipe(RestrictionDraft d) {
        JsonObject o = base(d);
        putStrings(o, "recipes", d.recipes);
        putStrings(o, "recipe_types", d.recipeTypes);
        putStrings(o, "outputs", d.matchers);
        putStrings(o, "inputs", d.inputs);
        putStrings(o, "exclude_recipes", d.excludeRecipes);
        o.addProperty("recipe_viewer", d.recipeViewer);
        o.addProperty("overlay_corner", d.overlayCorner);
        putStrings(o, "tooltips", d.tooltips);
        o.addProperty("hide_default_tooltips", d.hideDefaultTooltips);
        return o;
    }

    private static JsonObject block(RestrictionDraft d) {
        JsonObject o = base(d);
        putStrings(o, "blocks", d.matchers);
        putStrings(o, "exclude", d.exclude);
        o.addProperty("mimic", d.mimic);
        o.addProperty("camouflage", d.camouflage);
        o.addProperty("unbreakable", d.unbreakable);
        o.addProperty("mimic_mining", d.mimicMining);
        o.addProperty("stop_tick", d.stopTick);
        if (d.breakMessageDefault) {
            o.addProperty("break_message", StageBlockRule.DEFAULT_BREAK_MESSAGE);
        }
        if (d.interactMessageDefault) {
            o.addProperty("interact_message", StageBlockRule.DEFAULT_INTERACT_MESSAGE);
        }
        putStrings(o, "tooltips", d.tooltips);
        o.addProperty("hide_default_tooltips", d.hideDefaultTooltips);
        return o;
    }

    private static JsonObject gui(RestrictionDraft d) {
        JsonObject o = base(d);
        putStrings(o, "menus", d.menus);
        putStrings(o, "menu_types", d.menuTypes);
        o.addProperty("block_open", d.blockOpen);
        if (!d.blockedSlots.isEmpty()) {
            JsonArray slots = new JsonArray();
            for (Integer s : d.blockedSlots) {
                slots.add(s);
            }
            o.add("blocked_slots", slots);
        }
        putStrings(o, "blocked_items", d.blockedItems);
        if (d.maxSlots > 0) {
            o.addProperty("max_slots", d.maxSlots);
        }
        if (d.guiMessageDefault) {
            o.addProperty("message", StageGuiRule.DEFAULT_MESSAGE);
        } else {
            o.addProperty("message", d.guiMessage == null ? "" : d.guiMessage);
        }
        return o;
    }

    private static JsonObject base(RestrictionDraft d) {
        JsonObject o = new JsonObject();
        o.addProperty("id", d.id);
        o.addProperty("stages_logic", RestrictionDraft.normalizeLogic(d.stagesLogic));
        if (d.priority != 0) {
            o.addProperty("priority", d.priority);
        }
        JsonArray stages = new JsonArray();
        for (RestrictionDraft.StageRow row : d.stages) {
            JsonObject s = new JsonObject();
            s.addProperty("stage_type", row.type);
            s.addProperty("stage", row.id);
            s.addProperty("is", row.is);
            stages.add(s);
        }
        o.add("stages", stages);
        return o;
    }

    private static void putStrings(JsonObject o, String key, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        JsonArray arr = new JsonArray();
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                arr.add(v);
            }
        }
        if (!arr.isEmpty()) {
            o.add(key, arr);
        }
    }
}
