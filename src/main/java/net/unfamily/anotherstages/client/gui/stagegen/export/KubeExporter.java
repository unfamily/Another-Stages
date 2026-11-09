package net.unfamily.anotherstages.client.gui.stagegen.export;

import net.unfamily.anotherstages.client.gui.stagegen.RestrictionDraft;

import java.util.List;
import java.util.Locale;

/**
 * Builds KubeJS Stage Generator scripts in the canonical Entry-N file format
 * (single {@code AnotherStagesEvents.restrictions} wrapper, no {@code .id()}).
 */
public final class KubeExporter {
    private static final String WARNING_TOP =
            "// # Warning! Don't Remove comments with # or this file can't be open by generator.\n"
                    + "// # Dont' Remove This Line:\n";
    private static final String WARNING_BOTTOM = "// # Dont' Remove This Line:\n";

    private KubeExporter() {}

    /** Full category file: warning + wrapper + {@code //Entry N} bodies. */
    public static String exportFile(RestrictionDraft.Category category, List<RestrictionDraft> drafts) {
        StringBuilder sb = new StringBuilder();
        sb.append(WARNING_TOP);
        sb.append("AnotherStagesEvents.restrictions(event => {\n\n");
        int index = 0;
        if (drafts != null) {
            for (RestrictionDraft d : drafts) {
                if (d == null || d.category != category) {
                    continue;
                }
                sb.append("  //Entry ").append(index).append("\n");
                sb.append(exportEntryBody(d));
                sb.append("\n");
                index++;
            }
        }
        sb.append("\n");
        sb.append(WARNING_BOTTOM);
        sb.append("})\n");
        return sb.toString();
    }

    /**
     * One entry body (indented), starting at {@code event.<mgr>.restrict()} — no wrapper, no {@code .id()}.
     */
    public static String exportEntryBody(RestrictionDraft d) {
        StringBuilder sb = new StringBuilder();
        sb.append("  event.").append(manager(d.category)).append(".restrict()\n");
        sb.append("    .").append(logicMethod(d.stagesLogic)).append("()\n");
        sb.append("    .stages([\n");
        for (RestrictionDraft.StageRow row : d.stages) {
            sb.append("      { stage_type: ").append(ExportUtil.jsString(row.type))
                    .append(", stage: ").append(ExportUtil.jsString(row.id))
                    .append(", is: ").append(row.is).append(" },\n");
        }
        sb.append("    ])\n");
        switch (d.category) {
            case ITEM -> appendItem(sb, d);
            case RECIPE -> appendRecipe(sb, d);
            case BLOCK -> appendBlock(sb, d);
            case GUI -> appendGui(sb, d);
        }
        return sb.toString();
    }

    /** @deprecated use {@link #exportEntryBody} / {@link #exportFile} */
    @Deprecated
    public static String export(RestrictionDraft d) {
        return exportEntryBody(d);
    }

    private static String manager(RestrictionDraft.Category c) {
        return switch (c) {
            case ITEM -> "items";
            case RECIPE -> "recipes";
            case BLOCK -> "blocks";
            case GUI -> "guis";
        };
    }

    private static String logicMethod(String logic) {
        String n = RestrictionDraft.normalizeLogic(logic);
        return switch (n) {
            case "OR" -> "logicOr";
            case "DEF_OR" -> "logicDefOr";
            case "DEF_AND" -> "logicDefAnd";
            default -> "logicAnd";
        };
    }

    private static void appendItem(StringBuilder sb, RestrictionDraft d) {
        if (!d.matchers.isEmpty()) {
            sb.append("    .items(").append(ExportUtil.jsStringArray(d.matchers)).append(")\n");
        }
        if (!d.exclude.isEmpty()) {
            sb.append("    .exclude(").append(ExportUtil.jsStringArray(d.exclude)).append(")\n");
        }
        if (d.casesAll) {
            sb.append("    .casesAll(").append(Math.max(1, d.inventoryTicks)).append(")\n");
        } else {
            if (d.caseRight) {
                sb.append("    .rightClick()\n");
            }
            if (d.caseLeft) {
                sb.append("    .leftClick()\n");
            }
            if (d.caseMain) {
                sb.append("    .mainHand()\n");
            }
            if (d.caseOff) {
                sb.append("    .offHand()\n");
            }
            if (d.caseInventory) {
                sb.append("    .inventory(").append(Math.max(1, d.inventoryTicks)).append(")\n");
            }
        }
        sb.append("    .canPickup(").append(d.canPickup).append(")\n");
        if (!d.canPickup) {
            if (d.pickupMessageDefault) {
                sb.append("    .pickupMessage(true)\n");
            } else if (!d.pickupMessage.isBlank()) {
                sb.append("    .pickupMessage(").append(ExportUtil.jsString(d.pickupMessage)).append(")\n");
            }
        }
        sb.append("    .").append(consequenceMethod(d.consequence)).append("()\n");
        sb.append("    .").append(viewerMethod(d.recipeViewer)).append("()\n");
        sb.append("    .hideItemName(").append(d.hideItemName).append(")\n");
        for (String tip : d.tooltips) {
            sb.append("    .tooltip(Text.of(").append(ExportUtil.jsString(tip)).append("))\n");
        }
        if (d.hideDefaultTooltips) {
            sb.append("    .hideDefaultTooltips(true)\n");
        }
    }

    private static void appendRecipe(StringBuilder sb, RestrictionDraft d) {
        if (!d.recipes.isEmpty()) {
            sb.append("    .recipes(").append(ExportUtil.jsStringArray(d.recipes)).append(")\n");
        }
        if (!d.recipeTypes.isEmpty()) {
            sb.append("    .recipeTypes(").append(ExportUtil.jsStringArray(d.recipeTypes)).append(")\n");
        }
        if (!d.matchers.isEmpty()) {
            sb.append("    .outputs(").append(ExportUtil.jsStringArray(d.matchers)).append(")\n");
        }
        if (!d.inputs.isEmpty()) {
            sb.append("    .inputs(").append(ExportUtil.jsStringArray(d.inputs)).append(")\n");
        }
        if (!d.excludeRecipes.isEmpty()) {
            sb.append("    .excludeRecipes(").append(ExportUtil.jsStringArray(d.excludeRecipes)).append(")\n");
        }
        String viewer = "hide".equalsIgnoreCase(d.recipeViewer) ? "viewerHide" : "viewerOverlay";
        sb.append("    .").append(viewer).append("()\n");
        sb.append("    .overlayCorner(").append(ExportUtil.jsString(d.overlayCorner)).append(")\n");
        for (String tip : d.tooltips) {
            sb.append("    .tooltip(Text.of(").append(ExportUtil.jsString(tip)).append("))\n");
        }
        if (d.hideDefaultTooltips) {
            sb.append("    .hideDefaultTooltips(true)\n");
        }
    }

    private static void appendBlock(StringBuilder sb, RestrictionDraft d) {
        if (!d.matchers.isEmpty()) {
            sb.append("    .blocks(").append(ExportUtil.jsStringArray(d.matchers)).append(")\n");
        }
        if (!d.exclude.isEmpty()) {
            sb.append("    .exclude(").append(ExportUtil.jsStringArray(d.exclude)).append(")\n");
        }
        sb.append("    .mimic(").append(ExportUtil.jsString(d.mimic)).append(")\n");
        sb.append("    .camouflage(").append(d.camouflage).append(")\n");
        sb.append("    .unbreakable(").append(d.unbreakable).append(")\n");
        if (d.stopTick) {
            sb.append("    .stopTick(true)\n");
        }
        if (d.breakMessageDefault) {
            sb.append("    .breakMessage(true)\n");
        }
        if (d.interactMessageDefault) {
            sb.append("    .interactMessage(true)\n");
        }
        for (String tip : d.tooltips) {
            sb.append("    .tooltip(Text.of(").append(ExportUtil.jsString(tip)).append("))\n");
        }
        if (d.hideDefaultTooltips) {
            sb.append("    .hideDefaultTooltips(true)\n");
        }
    }

    private static void appendGui(StringBuilder sb, RestrictionDraft d) {
        if (!d.menus.isEmpty()) {
            sb.append("    .menus(").append(ExportUtil.jsStringArray(d.menus)).append(")\n");
        }
        if (!d.menuTypes.isEmpty()) {
            sb.append("    .menuTypes(").append(ExportUtil.jsStringArray(d.menuTypes)).append(")\n");
        }
        sb.append("    .blockOpen(").append(d.blockOpen).append(")\n");
        if (!d.blockedSlots.isEmpty()) {
            sb.append("    .blockedSlots([");
            for (int i = 0; i < d.blockedSlots.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(d.blockedSlots.get(i));
            }
            sb.append("])\n");
        }
        if (!d.blockedItems.isEmpty()) {
            sb.append("    .blockedItems(").append(ExportUtil.jsStringArray(d.blockedItems)).append(")\n");
        }
        if (d.maxSlots > 0) {
            sb.append("    .maxSlots(").append(d.maxSlots).append(")\n");
        }
        if (d.guiMessageDefault) {
            sb.append("    .message(true)\n");
        } else if (!d.guiMessage.isBlank()) {
            sb.append("    .message(").append(ExportUtil.jsString(d.guiMessage)).append(")\n");
        }
    }

    private static String consequenceMethod(String c) {
        return switch (c == null ? "drop" : c.toLowerCase(Locale.ROOT)) {
            case "delete" -> "delete";
            case "block" -> "block";
            case "block_drop" -> "blockDrop";
            case "block_delete" -> "blockDelete";
            default -> "drop";
        };
    }

    private static String viewerMethod(String v) {
        return switch (v == null ? "overlay" : v.toLowerCase(Locale.ROOT)) {
            case "hide" -> "viewerHide";
            case "hardcore" -> "viewerHardcore";
            default -> "viewerOverlay";
        };
    }
}
