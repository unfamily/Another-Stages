package net.unfamily.anotherstages.client.gui.stagegen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.unfamily.anotherstages.client.gui.AsButton;
import net.unfamily.anotherstages.client.gui.AsToggle;

import java.util.List;
import java.util.Locale;

/**
 * Spreadsheet-style editor rows fed into {@link StageGenScrollList}.
 */
public final class StageGenTablePanel {
    public enum ColumnType {
        TEXT,
        STAGE_TYPE,
        BOOL,
        INT
    }

    public record Column(String headerKey, ColumnType type) {}

    private final RestrictionDraft draft;
    private final RestrictionDraft.TableKind kind;
    private final Font font;
    private final Runnable onDone;
    private final Runnable onRebuild;

    private int selectedRow = 0;
    private EditBox cellBox;

    public StageGenTablePanel(
            RestrictionDraft draft,
            RestrictionDraft.TableKind kind,
            Font font,
            Runnable onDone,
            Runnable onRebuild
    ) {
        this.draft = draft;
        this.kind = kind;
        this.font = font;
        this.onDone = onDone;
        this.onRebuild = onRebuild;
    }

    public static List<Column> columnsFor(RestrictionDraft.TableKind kind) {
        return switch (kind) {
            case STAGES -> List.of(
                    new Column("screen.another_stages.stage_generator.table.col.type", ColumnType.STAGE_TYPE),
                    new Column("screen.another_stages.stage_generator.table.col.id", ColumnType.TEXT),
                    new Column("screen.another_stages.stage_generator.table.col.is", ColumnType.BOOL));
            case BLOCKED_SLOTS -> List.of(
                    new Column("screen.another_stages.stage_generator.table.col.slot", ColumnType.INT));
            default -> List.of(
                    new Column("screen.another_stages.stage_generator.table.col.value", ColumnType.TEXT));
        };
    }

    public int rowCount() {
        return switch (kind) {
            case STAGES -> draft.stages.size();
            case MATCHERS -> draft.matchers.size();
            case EXCLUDE -> draft.exclude.size();
            case TOOLTIPS -> draft.tooltips.size();
            case BLOCKED_SLOTS -> draft.blockedSlots.size();
            case BLOCKED_ITEMS -> draft.blockedItems.size();
            case RECIPES -> draft.recipes.size();
            case RECIPE_TYPES -> draft.recipeTypes.size();
            case INPUTS -> draft.inputs.size();
            case EXCLUDE_RECIPES -> draft.excludeRecipes.size();
            case MENUS -> draft.menus.size();
            case MENU_TYPES -> draft.menuTypes.size();
        };
    }

    public void ensureNonEmpty() {
        if (rowCount() == 0) {
            addRow();
        }
        selectedRow = Math.min(selectedRow, Math.max(0, rowCount() - 1));
    }

    public void populate(StageGenScrollList list) {
        ensureNonEmpty();
        List<Column> cols = columnsFor(kind);
        list.addLabel(Component.translatable(
                "screen.another_stages.stage_generator.table.title."
                        + kind.name().toLowerCase(Locale.ROOT)));

        StringBuilder header = new StringBuilder();
        for (int c = 0; c < cols.size(); c++) {
            if (c > 0) {
                header.append(" | ");
            }
            header.append(Component.translatable(cols.get(c).headerKey()).getString());
        }
        list.addLabel(Component.literal(header.toString()));

        for (int row = 0; row < rowCount(); row++) {
            int finalRow = row;
            boolean sel = row == selectedRow;
            AsButton idx = AsButton.text(
                    28,
                    Component.literal(sel ? ">" + (row + 1) : String.valueOf(row + 1)),
                    () -> {
                        selectedRow = finalRow;
                        onRebuild.run();
                    });

            List<net.minecraft.client.gui.components.AbstractWidget> widgets = new java.util.ArrayList<>();
            widgets.add(idx);

            for (int c = 0; c < cols.size(); c++) {
                Column col = cols.get(c);
                switch (col.type()) {
                    case BOOL -> widgets.add(new AsToggle(
                            () -> boolAt(finalRow),
                            v -> {
                                setBoolAt(finalRow, v);
                                onRebuild.run();
                            }));
                    case STAGE_TYPE -> widgets.add(AsButton.text(
                            70,
                            Component.literal(stageTypeAt(finalRow)),
                            () -> {
                                cycleStageType(finalRow);
                                onRebuild.run();
                            }));
                    case INT, TEXT -> {
                        String val = textAt(finalRow, c);
                        widgets.add(AsButton.text(
                                90,
                                Component.literal(val.isBlank() ? "…" : truncate(val, 12)),
                                () -> {
                                    selectedRow = finalRow;
                                    onRebuild.run();
                                }));
                    }
                }
            }
            list.addWidgets(widgets.toArray(net.minecraft.client.gui.components.AbstractWidget[]::new));
        }

        cellBox = new EditBox(
                font,
                0,
                0,
                160,
                18,
                Component.translatable("screen.another_stages.stage_generator.table.cell"));
        cellBox.setMaxLength(256);
        cellBox.setValue(textAt(selectedRow, primaryTextCol(cols)));
        AsButton setCell = AsButton.text(
                72,
                Component.translatable("screen.another_stages.stage_generator.table.set_cell"),
                this::applyCell);
        list.addWidgetsTall(cellBox, setCell);

        AsButton add = AsButton.text(
                70,
                Component.translatable("screen.another_stages.stage_generator.table.add"),
                () -> {
                    addRow();
                    selectedRow = rowCount() - 1;
                    onRebuild.run();
                });
        AsButton del = AsButton.text(
                70,
                Component.translatable("screen.another_stages.stage_generator.table.remove"),
                () -> {
                    removeRow(selectedRow);
                    ensureNonEmpty();
                    onRebuild.run();
                });
        // Exit is the screen side Done button (avoid duplicate Back/Done in the list).
        list.addWidgets(add, del);
    }

    private int primaryTextCol(List<Column> cols) {
        for (int i = 0; i < cols.size(); i++) {
            if (cols.get(i).type() == ColumnType.TEXT || cols.get(i).type() == ColumnType.INT) {
                return i;
            }
        }
        return Math.min(1, cols.size() - 1);
    }

    private void applyCell() {
        if (cellBox == null) {
            return;
        }
        setTextAt(selectedRow, primaryTextCol(columnsFor(kind)), cellBox.getValue());
        onRebuild.run();
    }

    private void addRow() {
        switch (kind) {
            case STAGES -> draft.stages.add(new RestrictionDraft.StageRow());
            case MATCHERS -> draft.matchers.add("");
            case EXCLUDE -> draft.exclude.add("");
            case TOOLTIPS -> draft.tooltips.add("");
            case BLOCKED_SLOTS -> draft.blockedSlots.add(0);
            case BLOCKED_ITEMS -> draft.blockedItems.add("");
            case RECIPES -> draft.recipes.add("");
            case RECIPE_TYPES -> draft.recipeTypes.add("");
            case INPUTS -> draft.inputs.add("");
            case EXCLUDE_RECIPES -> draft.excludeRecipes.add("");
            case MENUS -> draft.menus.add("");
            case MENU_TYPES -> draft.menuTypes.add("");
        }
    }

    private void removeRow(int row) {
        if (row < 0 || row >= rowCount()) {
            return;
        }
        switch (kind) {
            case STAGES -> draft.stages.remove(row);
            case MATCHERS -> draft.matchers.remove(row);
            case EXCLUDE -> draft.exclude.remove(row);
            case TOOLTIPS -> draft.tooltips.remove(row);
            case BLOCKED_SLOTS -> draft.blockedSlots.remove(row);
            case BLOCKED_ITEMS -> draft.blockedItems.remove(row);
            case RECIPES -> draft.recipes.remove(row);
            case RECIPE_TYPES -> draft.recipeTypes.remove(row);
            case INPUTS -> draft.inputs.remove(row);
            case EXCLUDE_RECIPES -> draft.excludeRecipes.remove(row);
            case MENUS -> draft.menus.remove(row);
            case MENU_TYPES -> draft.menuTypes.remove(row);
        }
        selectedRow = Math.max(0, Math.min(selectedRow, rowCount() - 1));
    }

    private boolean boolAt(int row) {
        return kind == RestrictionDraft.TableKind.STAGES
                && row >= 0
                && row < draft.stages.size()
                && draft.stages.get(row).is;
    }

    private void setBoolAt(int row, boolean v) {
        if (kind == RestrictionDraft.TableKind.STAGES && row >= 0 && row < draft.stages.size()) {
            draft.stages.get(row).is = v;
        }
    }

    private String stageTypeAt(int row) {
        if (row >= 0 && row < draft.stages.size()) {
            return draft.stages.get(row).type;
        }
        return "player";
    }

    private void cycleStageType(int row) {
        if (row < 0 || row >= draft.stages.size()) {
            return;
        }
        String cur = draft.stages.get(row).type;
        draft.stages.get(row).type = switch (cur == null ? "player" : cur.toLowerCase(Locale.ROOT)) {
            case "world" -> "team";
            case "team" -> "player";
            default -> "world";
        };
    }

    private String textAt(int row, int col) {
        return switch (kind) {
            case STAGES -> {
                if (row < 0 || row >= draft.stages.size()) {
                    yield "";
                }
                yield col == 1 ? draft.stages.get(row).id : draft.stages.get(row).type;
            }
            case MATCHERS -> getList(draft.matchers, row);
            case EXCLUDE -> getList(draft.exclude, row);
            case TOOLTIPS -> getList(draft.tooltips, row);
            case BLOCKED_ITEMS -> getList(draft.blockedItems, row);
            case RECIPES -> getList(draft.recipes, row);
            case RECIPE_TYPES -> getList(draft.recipeTypes, row);
            case INPUTS -> getList(draft.inputs, row);
            case EXCLUDE_RECIPES -> getList(draft.excludeRecipes, row);
            case MENUS -> getList(draft.menus, row);
            case MENU_TYPES -> getList(draft.menuTypes, row);
            case BLOCKED_SLOTS -> row >= 0 && row < draft.blockedSlots.size()
                    ? String.valueOf(draft.blockedSlots.get(row))
                    : "0";
        };
    }

    private void setTextAt(int row, int col, String value) {
        String v = value == null ? "" : value.trim();
        switch (kind) {
            case STAGES -> {
                if (row >= 0 && row < draft.stages.size()) {
                    if (col == 1) {
                        draft.stages.get(row).id = v;
                    } else {
                        draft.stages.get(row).type = v.isBlank() ? "player" : v;
                    }
                }
            }
            case MATCHERS -> setList(draft.matchers, row, v);
            case EXCLUDE -> setList(draft.exclude, row, v);
            case TOOLTIPS -> setList(draft.tooltips, row, v);
            case BLOCKED_ITEMS -> setList(draft.blockedItems, row, v);
            case RECIPES -> setList(draft.recipes, row, v);
            case RECIPE_TYPES -> setList(draft.recipeTypes, row, v);
            case INPUTS -> setList(draft.inputs, row, v);
            case EXCLUDE_RECIPES -> setList(draft.excludeRecipes, row, v);
            case MENUS -> setList(draft.menus, row, v);
            case MENU_TYPES -> setList(draft.menuTypes, row, v);
            case BLOCKED_SLOTS -> {
                if (row >= 0 && row < draft.blockedSlots.size()) {
                    try {
                        draft.blockedSlots.set(row, Integer.parseInt(v));
                    } catch (NumberFormatException ignored) {
                        draft.blockedSlots.set(row, 0);
                    }
                }
            }
        }
    }

    private static String getList(List<String> list, int row) {
        return row >= 0 && row < list.size() ? list.get(row) : "";
    }

    private static void setList(List<String> list, int row, String v) {
        if (row >= 0 && row < list.size()) {
            list.set(row, v);
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
