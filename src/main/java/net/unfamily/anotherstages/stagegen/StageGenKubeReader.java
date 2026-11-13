package net.unfamily.anotherstages.stagegen;

import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.client.gui.stagegen.RestrictionDraft;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort parse of Stage Generator KubeJS files ({@code //Entry N} format).
 */
public final class StageGenKubeReader {
    private static final Pattern ENTRY_HEADER = Pattern.compile("(?m)^[ \\t]*//Entry[ \\t]+(\\d+)[ \\t]*\\R");
    private static final Pattern STRING_IN_ARRAY = Pattern.compile("'((?:\\\\'|[^'])*)'|\"((?:\\\\\"|[^\"])*)\"");
    private static final Pattern STAGE_OBJ = Pattern.compile(
            "\\{\\s*stage_type:\\s*'([^']*)'\\s*,\\s*stage:\\s*'([^']*)'\\s*,\\s*is:\\s*(true|false)\\s*\\}");

    private StageGenKubeReader() {}

    public static List<RestrictionDraft> loadAllKube() {
        List<RestrictionDraft> out = new ArrayList<>();
        for (RestrictionDraft.Category cat : RestrictionDraft.Category.values()) {
            out.addAll(loadCategory(cat));
        }
        return out;
    }

    public static List<RestrictionDraft> loadCategory(RestrictionDraft.Category category) {
        List<RestrictionDraft> out = new ArrayList<>();
        Path file = StageGenPaths.resolveKubeFile(category);
        if (file == null || !Files.isRegularFile(file)) {
            return out;
        }
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            out.addAll(parseFile(category, raw));
        } catch (Exception ex) {
            AnotherStages.LOGGER.warn("Failed to load stage generator kube file {}: {}", file, ex.toString());
        }
        return out;
    }

    public static List<RestrictionDraft> parseFile(RestrictionDraft.Category category, String raw) {
        List<RestrictionDraft> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        Matcher headers = ENTRY_HEADER.matcher(raw);
        List<Integer> indices = new ArrayList<>();
        List<Integer> bodyStarts = new ArrayList<>();
        List<Integer> headerStarts = new ArrayList<>();
        while (headers.find()) {
            indices.add(Integer.parseInt(headers.group(1)));
            headerStarts.add(headers.start());
            bodyStarts.add(headers.end());
        }
        if (bodyStarts.isEmpty()) {
            RestrictionDraft lone = parseEntryBody(category, raw, 0);
            if (lone != null && hasUsefulContent(lone)) {
                out.add(lone);
            }
            return out;
        }
        for (int i = 0; i < bodyStarts.size(); i++) {
            int from = bodyStarts.get(i);
            int to = i + 1 < headerStarts.size() ? headerStarts.get(i + 1) : raw.length();
            String body = trimEntryBody(raw.substring(from, to));
            RestrictionDraft draft = parseEntryBody(category, body, indices.get(i));
            if (draft != null) {
                out.add(draft);
            }
        }
        return out;
    }

    private static String trimEntryBody(String body) {
        int warn = body.indexOf("// # Dont'");
        if (warn >= 0) {
            body = body.substring(0, warn);
        }
        // Drop a lone closing `})` of the file wrapper if it leaked into the last entry.
        int close = body.lastIndexOf("\n})");
        if (close >= 0 && !body.substring(close).contains("event.")) {
            body = body.substring(0, close);
        }
        return body;
    }

    private static boolean hasUsefulContent(RestrictionDraft d) {
        return !d.matchers.isEmpty()
                || !d.recipes.isEmpty()
                || !d.recipeTypes.isEmpty()
                || !d.inputs.isEmpty()
                || !d.menus.isEmpty()
                || !d.menuTypes.isEmpty()
                || d.stages.stream().anyMatch(s -> s.id != null && !s.id.isBlank());
    }

    public static RestrictionDraft parseEntryBody(RestrictionDraft.Category category, String body, int entryIndex) {
        RestrictionDraft d = RestrictionDraft.createDefaults(category);
        d.kubeEntryIndex = entryIndex;
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
        d.caseRight = false;
        d.caseLeft = false;
        d.caseMain = false;
        d.caseOff = false;
        d.caseInventory = false;
        d.casesAll = false;

        if (body.contains(".logicOr()")) {
            d.stagesLogic = "OR";
        } else if (body.contains(".logicDefOr()")) {
            d.stagesLogic = "DEF_OR";
        } else if (body.contains(".logicDefAnd()")) {
            d.stagesLogic = "DEF_AND";
        } else {
            d.stagesLogic = "AND";
        }

        Matcher priority = Pattern.compile("\\.priority\\s*\\(\\s*(-?\\d+)\\s*\\)").matcher(body);
        if (priority.find()) {
            d.priority = Integer.parseInt(priority.group(1));
        }

        Matcher stagesBlock = Pattern.compile("\\.stages\\s*\\(\\s*\\[([\\s\\S]*?)]\\s*\\)").matcher(body);
        if (stagesBlock.find()) {
            Matcher stageObj = STAGE_OBJ.matcher(stagesBlock.group(1));
            while (stageObj.find()) {
                d.stages.add(new RestrictionDraft.StageRow(
                        stageObj.group(1),
                        stageObj.group(2),
                        Boolean.parseBoolean(stageObj.group(3))));
            }
        }
        if (d.stages.isEmpty()) {
            d.stages.add(new RestrictionDraft.StageRow());
        }

        readStringArrayMethod(body, "items", d.matchers);
        readStringArrayMethod(body, "blocks", d.matchers);
        readStringArrayMethod(body, "outputs", d.matchers);
        readStringArrayMethod(body, "exclude", d.exclude);
        readStringArrayMethod(body, "recipes", d.recipes);
        readStringArrayMethod(body, "recipeTypes", d.recipeTypes);
        readStringArrayMethod(body, "inputs", d.inputs);
        readStringArrayMethod(body, "excludeRecipes", d.excludeRecipes);
        readStringArrayMethod(body, "menus", d.menus);
        readStringArrayMethod(body, "menuTypes", d.menuTypes);
        readStringArrayMethod(body, "blockedItems", d.blockedItems);

        Matcher slots = Pattern.compile("\\.blockedSlots\\s*\\(\\s*\\[([^]]*)]\\s*\\)").matcher(body);
        if (slots.find()) {
            for (String part : slots.group(1).split(",")) {
                String t = part.trim();
                if (t.isEmpty()) {
                    continue;
                }
                try {
                    d.blockedSlots.add(Integer.parseInt(t));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (body.contains(".caseRightClick()")) {
            d.caseRight = true;
        }
        if (body.contains(".caseLeftClick()")) {
            d.caseLeft = true;
        }
        if (body.contains(".caseMainHand()")) {
            d.caseMain = true;
        }
        if (body.contains(".caseOffHand()")) {
            d.caseOff = true;
        }
        Matcher inv = Pattern.compile("\\.caseInventory\\s*\\(\\s*(\\d+)\\s*\\)").matcher(body);
        if (inv.find()) {
            d.caseInventory = true;
            d.inventoryTicks = Integer.parseInt(inv.group(1));
        } else if (body.contains(".caseInventory()")) {
            d.caseInventory = true;
        }
        Matcher caseAll = Pattern.compile("\\.caseAll\\s*\\(\\s*(\\d+)\\s*\\)").matcher(body);
        if (caseAll.find()) {
            d.casesAll = true;
            d.inventoryTicks = Integer.parseInt(caseAll.group(1));
        } else if (body.contains(".caseAll()")) {
            d.casesAll = true;
        }

        Matcher canPickup = Pattern.compile("\\.canPickup\\s*\\(\\s*(true|false)\\s*\\)").matcher(body);
        if (canPickup.find()) {
            d.canPickup = Boolean.parseBoolean(canPickup.group(1));
        }
        if (body.contains(".pickupMessage(true)")) {
            d.pickupMessageDefault = true;
        }

        if (body.contains(".consequenceBlockDrop()")) {
            d.consequence = "block_drop";
        } else if (body.contains(".consequenceBlockDelete()")) {
            d.consequence = "block_delete";
        } else if (body.contains(".consequenceBlock()")) {
            d.consequence = "block";
        } else if (body.contains(".consequenceDelete()")) {
            d.consequence = "delete";
        } else if (body.contains(".consequenceDrop()")) {
            d.consequence = "drop";
        } else {
            Matcher cons = Pattern.compile("\\.consequences\\s*\\(\\s*'((?:\\\\'|[^'])*)'\\s*\\)").matcher(body);
            if (cons.find()) {
                d.consequence = unescape(cons.group(1)).trim().toLowerCase(java.util.Locale.ROOT);
            }
        }

        if (body.contains(".viewerHardcore()")) {
            d.recipeViewer = "hardcore";
        } else if (body.contains(".viewerHide()")) {
            d.recipeViewer = "hide";
        } else if (body.contains(".viewerOverlay()")) {
            d.recipeViewer = "overlay";
        }

        Matcher hideName = Pattern.compile("\\.hideItemName\\s*\\(\\s*(true|false)\\s*\\)").matcher(body);
        if (hideName.find()) {
            d.hideItemName = Boolean.parseBoolean(hideName.group(1));
        }
        if (body.contains(".hideDefaultTooltips(true)")) {
            d.hideDefaultTooltips = true;
        }

        Matcher tip = Pattern.compile("\\.tooltip\\s*\\(\\s*Text\\.of\\s*\\(\\s*'((?:\\\\'|[^'])*)'\\s*\\)\\s*\\)").matcher(body);
        while (tip.find()) {
            d.tooltips.add(unescape(tip.group(1)));
        }

        Matcher mimic = Pattern.compile("\\.mimic\\s*\\(\\s*'((?:\\\\'|[^'])*)'\\s*\\)").matcher(body);
        if (mimic.find()) {
            d.mimic = unescape(mimic.group(1));
        }
        Matcher camouflage = Pattern.compile("\\.camouflage\\s*\\(\\s*(true|false)\\s*\\)").matcher(body);
        if (camouflage.find()) {
            d.camouflage = Boolean.parseBoolean(camouflage.group(1));
        }
        Matcher unbreakable = Pattern.compile("\\.unbreakable\\s*\\(\\s*(true|false)\\s*\\)").matcher(body);
        if (unbreakable.find()) {
            d.unbreakable = Boolean.parseBoolean(unbreakable.group(1));
        }
        if (Pattern.compile("\\.disableMimicMining\\s*\\(\\s*\\)").matcher(body).find()) {
            d.mimicMining = false;
        }
        Matcher mimicMining = Pattern.compile("\\.mimicMining\\s*\\(\\s*(true|false)\\s*\\)").matcher(body);
        if (mimicMining.find()) {
            d.mimicMining = Boolean.parseBoolean(mimicMining.group(1));
        }
        if (body.contains(".stopTick(true)")) {
            d.stopTick = true;
        }
        if (body.contains(".breakMessage(true)")) {
            d.breakMessageDefault = true;
        }
        if (body.contains(".interactMessage(true)")) {
            d.interactMessageDefault = true;
        }

        Matcher blockOpen = Pattern.compile("\\.blockOpen\\s*\\(\\s*(true|false)\\s*\\)").matcher(body);
        if (blockOpen.find()) {
            d.blockOpen = Boolean.parseBoolean(blockOpen.group(1));
        }
        Matcher maxSlots = Pattern.compile("\\.maxSlots\\s*\\(\\s*(\\d+)\\s*\\)").matcher(body);
        if (maxSlots.find()) {
            d.maxSlots = Integer.parseInt(maxSlots.group(1));
        }
        if (body.contains(".message(true)")) {
            d.guiMessageDefault = true;
        }

        Matcher corner = Pattern.compile("\\.overlayCorner\\s*\\(\\s*'((?:\\\\'|[^'])*)'\\s*\\)").matcher(body);
        if (corner.find()) {
            d.overlayCorner = unescape(corner.group(1));
        }

        return d;
    }

    private static void readStringArrayMethod(String body, String method, List<String> target) {
        Pattern p = Pattern.compile("\\." + method + "\\s*\\(\\s*\\[([\\s\\S]*?)]\\s*\\)");
        Matcher m = p.matcher(body);
        if (!m.find()) {
            return;
        }
        Matcher s = STRING_IN_ARRAY.matcher(m.group(1));
        while (s.find()) {
            String v = s.group(1) != null ? s.group(1) : s.group(2);
            target.add(unescape(v));
        }
    }

    private static String unescape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\'", "'").replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\");
    }
}
