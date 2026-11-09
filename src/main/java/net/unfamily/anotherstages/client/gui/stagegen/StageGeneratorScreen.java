package net.unfamily.anotherstages.client.gui.stagegen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.unfamily.anotherstages.client.gui.AsButton;
import net.unfamily.anotherstages.client.gui.AsToggle;
import net.unfamily.anotherstages.client.gui.stagegen.export.JsonExporter;
import net.unfamily.anotherstages.client.gui.stagegen.export.KubeExporter;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.menu.StageGeneratorMenu;
import net.unfamily.anotherstages.network.DeleteStageGeneratorPayload;
import net.unfamily.anotherstages.network.SaveStageGeneratorPayload;
import net.unfamily.anotherstages.stagegen.StageGenCompletion;
import net.unfamily.anotherstages.stagegen.StageGenPaths;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Shop-style Stage Generator: HOME → Browse → ENTRY_EDIT → nested lists/entries.
 * Player inventory + item selector only on matcher-entry when type is item.
 */
public class StageGeneratorScreen extends AbstractContainerScreen<StageGeneratorMenu>
        implements IStageGenGhostTarget {

    private enum SubView {
        PREFER,
        HOME,
        BROWSE,
        ENTRY_EDIT,
        CASES,
        STAGES,
        STAGE_ENTRY,
        MATCHERS,
        MATCHER_ENTRY,
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

    private enum MatcherKind {
        ITEM,
        TAG,
        MOD
    }

    private record AreaButton(String labelKey, RestrictionDraft.Category category) {}

    private static final List<AreaButton> HOME_AREAS = List.of(
            new AreaButton("screen.another_stages.stage_generator.cat.items", RestrictionDraft.Category.ITEM),
            new AreaButton("screen.another_stages.stage_generator.cat.recipes", RestrictionDraft.Category.RECIPE),
            new AreaButton("screen.another_stages.stage_generator.cat.blocks", RestrictionDraft.Category.BLOCK),
            new AreaButton("screen.another_stages.stage_generator.cat.guis", RestrictionDraft.Category.GUI)
    );

    private static final int FORM_LEFT = StageGeneratorMenu.LIST_X + 4;
    private static final int FORM_RIGHT = StageGeneratorMenu.LIST_RIGHT;
    private static final int FORM_WIDTH = FORM_RIGHT - FORM_LEFT;
    private static final int FORM_GAP = 4;

    private SubView subView = SubView.HOME;
    /** Parent list when on MATCHER_ENTRY (MATCHERS / EXCLUDE / INPUTS / BLOCKED_ITEMS). */
    private SubView matcherListParent = SubView.MATCHERS;
    private int editingMatcherIndex = -1;
    private MatcherKind matcherKind = MatcherKind.ITEM;
    private int editingStageIndex = -1;

    private @Nullable StageGenTablePanel tablePanel;
    private @Nullable StageGenScrollList scrollList;
    private @Nullable EditBox idBox;
    private @Nullable EditBox mimicBox;
    private @Nullable EditBox ticksBox;
    private @Nullable EditBox maxSlotsBox;
    private @Nullable EditBox matcherValueBox;
    private @Nullable SuggestingEditBox stageIdBox;
    private @Nullable StringWidget status;
    private @Nullable AsButton saveButton;
    private double savedScroll = 0;

    public StageGeneratorScreen(StageGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, StageGeneratorMenu.GUI_WIDTH, StageGeneratorMenu.GUI_HEIGHT);
    }

    private StageGenSession session() {
        return StageGenSession.get();
    }

    private @Nullable RestrictionDraft draft() {
        return session().selected();
    }

    @Override
    protected void init() {
        super.init();
        menu.setOnGhostChanged(this::onGhostChanged);
        boolean kubejs = ModList.get().isLoaded("kubejs");
        if (kubejs && AsConfig.kubejsPromptSeed()) {
            subView = SubView.PREFER;
        } else if (subView == SubView.PREFER) {
            subView = SubView.HOME;
        }
        rebuild();
    }

    public void onSessionSynced() {
        if (subView == SubView.BROWSE || subView == SubView.HOME) {
            savedScroll = 0;
            rebuild();
        }
    }

    private void rebuild() {
        if (scrollList != null) {
            savedScroll = scrollList.scrollAmount();
        }
        clearWidgets();
        tablePanel = null;
        scrollList = null;
        idBox = null;
        mimicBox = null;
        ticksBox = null;
        maxSlotsBox = null;
        matcherValueBox = null;
        stageIdBox = null;
        status = null;
        saveButton = null;

        boolean showSlots = showInventorySlots();
        menu.setSlotsVisible(showSlots);
        if (!showSlots) {
            menu.clearGhost();
        }

        switch (subView) {
            case PREFER -> buildPrefer();
            case HOME -> buildHome();
            case BROWSE -> buildBrowse();
            case ENTRY_EDIT -> buildEntryEdit();
            case CASES -> buildCases();
            case MATCHER_ENTRY -> buildMatcherEntry();
            case STAGE_ENTRY -> buildStageEntry();
            case MATCHERS, EXCLUDE, BLOCKED_ITEMS, INPUTS -> buildMatcherList();
            case STAGES -> buildStageList();
            default -> buildTableSubview();
        }

        if (scrollList != null) {
            scrollList.setScrollAmount(savedScroll);
        }
    }

    private boolean showInventorySlots() {
        return subView == SubView.MATCHER_ENTRY && matcherKind == MatcherKind.ITEM;
    }

    private boolean acceptsGhostInCurrentView() {
        return showInventorySlots();
    }

    private int contentBottom() {
        if (showInventorySlots()) {
            return topPos + StageGeneratorMenu.GHOST_Y - 6;
        }
        return topPos + imageHeight - 8;
    }

    private StageGenScrollList createList(int topY, int bottomY) {
        int height = Math.max(40, bottomY - topY);
        StageGenScrollList list = new StageGenScrollList(minecraft, font, StageGeneratorMenu.LIST_WIDTH, height, topY);
        list.setX(leftPos + StageGeneratorMenu.LIST_X);
        addRenderableWidget(list);
        this.scrollList = list;
        return list;
    }

    private int formColX(int col, int colCount) {
        int usable = FORM_WIDTH - FORM_GAP * Math.max(0, colCount - 1);
        int w = usable / Math.max(1, colCount);
        return FORM_LEFT + col * (w + FORM_GAP);
    }

    private int formColW(int col, int colCount) {
        int usable = FORM_WIDTH - FORM_GAP * Math.max(0, colCount - 1);
        int w = usable / Math.max(1, colCount);
        if (col == colCount - 1) {
            return FORM_RIGHT - formColX(col, colCount);
        }
        return w;
    }

    private void addFormLabel(int x, int y, int w, String key) {
        addFormLabel(x, y, w, Component.translatable(key));
    }

    private void addFormLabel(int x, int y, int w, Component text) {
        StringWidget sw = new StringWidget(w, 10, text, font);
        sw.setPosition(leftPos + x, topPos + y);
        addRenderableWidget(sw);
    }

    private EditBox addFormBox(int x, int y, int w, int h, String initial, int maxLen, Consumer<String> onChange) {
        EditBox box = new EditBox(font, leftPos + x, topPos + y, w, h, Component.empty());
        box.setMaxLength(maxLen);
        box.setValue(initial == null ? "" : initial);
        box.setResponder(onChange);
        addRenderableWidget(box);
        return box;
    }

    private AsButton addFormButton(int x, int y, int w, int h, Component label, Runnable action) {
        AsButton btn = AsButton.text(w, h, label, action);
        btn.setPosition(leftPos + x, topPos + y);
        addRenderableWidget(btn);
        return btn;
    }

    private void addFormToggle(int x, int y, int colW, String labelKey, BooleanSupplier get, Consumer<Boolean> set) {
        int labelW = Math.max(24, colW - AsToggle.WIDTH - 4);
        StringWidget sw = new StringWidget(labelW, 12, Component.translatable(labelKey), font);
        sw.setPosition(leftPos + x, topPos + y + 1);
        addRenderableWidget(sw);
        AsToggle toggle = new AsToggle(get, set);
        toggle.setPosition(leftPos + x + colW - AsToggle.WIDTH, topPos + y);
        addRenderableWidget(toggle);
    }

    private AsButton addSideButton(int index, Component label, Runnable action) {
        return addSideButton(index, label, action, null, () -> true);
    }

    private AsButton addSideButton(
            int index,
            Component label,
            Runnable action,
            @Nullable Component tooltip,
            Supplier<Boolean> activeWhen
    ) {
        int x = leftPos + StageGeneratorMenu.SIDE_BTN_X;
        int y = topPos + StageGeneratorMenu.SIDE_BTN_Y
                + index * (StageGeneratorMenu.SIDE_BTN_H + StageGeneratorMenu.SIDE_BTN_GAP);
        AsButton btn = AsButton.text(
                StageGeneratorMenu.SIDE_BTN_W,
                StageGeneratorMenu.SIDE_BTN_H,
                label,
                action,
                tooltip);
        btn.activeWhen(activeWhen);
        btn.refreshActive();
        btn.setPosition(x, y);
        addRenderableWidget(btn);
        return btn;
    }

    private Component doneLabel() {
        return Component.translatable("screen.another_stages.stage_generator.side.done");
    }

    private void buildPrefer() {
        StageGenScrollList list = createList(topPos + 8, contentBottom());
        list.addLabel("screen.another_stages.stage_generator.prefer_kubejs.title");
        list.addLabel("screen.another_stages.stage_generator.prefer_kubejs.message");
        list.addActionButton(
                Component.translatable("screen.another_stages.stage_generator.prefer_kubejs.yes"),
                () -> finishPrefer(true));
        list.addActionButton(
                Component.translatable("screen.another_stages.stage_generator.prefer_kubejs.no"),
                () -> finishPrefer(false));
    }

    private void finishPrefer(boolean preferKube) {
        AsConfig.setPreferKubejs(preferKube);
        AsConfig.clearKubejsPromptSeed();
        AsConfig.saveCommon();
        subView = SubView.HOME;
        savedScroll = 0;
        rebuild();
    }

    private void buildHome() {
        StageGenScrollList list = createList(topPos + 8, contentBottom());
        list.addLabel("screen.another_stages.stage_generator.title");
        list.addLabel("screen.another_stages.stage_generator.home.hint");

        boolean kube = AsConfig.preferKubejs();
        if (!kube) {
            String first = StageGenClientPaths.firstDatapackPath();
            if (first.isBlank()) {
                first = StageGenPaths.displayFirstDatapackRoot();
            }
            list.addLabel(Component.translatable(
                    "screen.another_stages.stage_generator.warn.first_datapack_path",
                    first));
        } else {
            list.addLabel(Component.translatable(
                    "screen.another_stages.stage_generator.warn.kube_path",
                    "kubejs/server_scripts/another_stages/"));
        }

        list.addActionButton(
                Component.translatable(kube
                        ? "screen.another_stages.stage_generator.switch_json"
                        : "screen.another_stages.stage_generator.switch_kubejs"),
                () -> {
                    AsConfig.setPreferKubejs(!AsConfig.preferKubejs());
                    AsConfig.saveCommon();
                    savedScroll = 0;
                    rebuild();
                });
        list.addLabel(kube
                ? "screen.another_stages.stage_generator.export_mode.kubejs"
                : "screen.another_stages.stage_generator.export_mode.json");
        list.addLabel("screen.another_stages.stage_generator.home.areas");
        for (AreaButton area : HOME_AREAS) {
            list.addActionButton(Component.translatable(area.labelKey()), () -> {
                session().setFilter(area.category());
                subView = SubView.BROWSE;
                savedScroll = 0;
                rebuild();
            });
        }
    }

    private void buildBrowse() {
        StageGenScrollList list = createList(topPos + 6, contentBottom());
        String titleKey = switch (session().filter()) {
            case ITEM -> "screen.another_stages.stage_generator.browse.items";
            case RECIPE -> "screen.another_stages.stage_generator.browse.recipes";
            case BLOCK -> "screen.another_stages.stage_generator.browse.blocks";
            case GUI -> "screen.another_stages.stage_generator.browse.guis";
        };
        list.addLabel(titleKey);

        List<RestrictionDraft> filtered = session().filtered();
        if (filtered.isEmpty()) {
            list.addLabel("screen.another_stages.stage_generator.browse.empty");
        } else {
            for (RestrictionDraft d : filtered) {
                StageGenCompletion.Status st = StageGenCompletion.evaluate(d);
                ItemStack icon = firstMatcherIcon(d);
                String glyph = icon.isEmpty() ? StageGenIcons.glyphForMatcher(firstMatcherId(d)) : null;
                if (icon.isEmpty() && (firstMatcherId(d) == null || firstMatcherId(d).isBlank())) {
                    icon = StageGenIcons.stackForCategory(d.category);
                    glyph = null;
                }
                Component title = Component.literal(d.id == null || d.id.isBlank() ? "(no id)" : d.id);
                list.addBrowseRow(icon, glyph, title, st.reason(), st.ready(), () -> {
                    session().select(d);
                    subView = SubView.ENTRY_EDIT;
                    savedScroll = 0;
                    rebuild();
                });
            }
        }

        addSideButton(0, Component.translatable("screen.another_stages.stage_generator.side.new"), this::actionNew);
        addSideButton(1, Component.translatable("screen.another_stages.stage_generator.side.edit"), this::actionEdit,
                null, () -> session().selected() != null && session().selected().category == session().filter());
        addSideButton(2, Component.translatable("screen.another_stages.stage_generator.side.delete"), this::actionDelete,
                null, () -> session().selected() != null && session().selected().category == session().filter());
        addSideButton(3, doneLabel(), () -> {
            subView = SubView.HOME;
            savedScroll = 0;
            rebuild();
        });

        status = new StringWidget(StageGeneratorMenu.LIST_WIDTH, 10, Component.empty(), font);
        status.setPosition(leftPos + StageGeneratorMenu.LIST_X, topPos + imageHeight - 18);
        addRenderableWidget(status);
    }

    private void actionNew() {
        RestrictionDraft created = session().createNew(session().filter());
        session().select(created);
        subView = SubView.ENTRY_EDIT;
        savedScroll = 0;
        rebuild();
    }

    private void actionEdit() {
        RestrictionDraft d = session().selected();
        if (d == null || d.category != session().filter()) {
            List<RestrictionDraft> filtered = session().filtered();
            if (filtered.isEmpty()) {
                return;
            }
            session().select(filtered.get(0));
        }
        subView = SubView.ENTRY_EDIT;
        savedScroll = 0;
        rebuild();
    }

    private void actionDelete() {
        RestrictionDraft d = session().selected();
        if (d == null || d.category != session().filter()) {
            return;
        }
        if (!AsConfig.preferKubejs()) {
            ClientPacketDistributor.sendToServer(
                    new DeleteStageGeneratorPayload(d.category.name(), d.id));
        }
        session().removeSelected();
        savedScroll = 0;
        rebuild();
    }

    private void buildEntryEdit() {
        RestrictionDraft draft = draft();
        if (draft == null) {
            subView = SubView.BROWSE;
            rebuild();
            return;
        }

        StageGenCompletion.Status st = StageGenCompletion.evaluate(draft);
        String titleKey = switch (draft.category) {
            case ITEM -> "screen.another_stages.stage_generator.editor.items";
            case RECIPE -> "screen.another_stages.stage_generator.editor.recipes";
            case BLOCK -> "screen.another_stages.stage_generator.editor.blocks";
            case GUI -> "screen.another_stages.stage_generator.editor.guis";
        };
        addFormLabel(FORM_LEFT, 8, FORM_WIDTH, titleKey);
        addFormLabel(FORM_LEFT, 18, FORM_WIDTH, Component.translatable(
                        st.ready()
                                ? "screen.another_stages.stage_generator.complete.checklist_ready"
                                : "screen.another_stages.stage_generator.complete.checklist_incomplete")
                .append(Component.literal(" — "))
                .append(st.reason()));

        addFormLabel(FORM_LEFT, 30, FORM_WIDTH, "screen.another_stages.stage_generator.field.id");
        idBox = addFormBox(FORM_LEFT, 40, FORM_WIDTH, 12, draft.id, 128, v -> draft.id = v == null ? "" : v.trim());

        switch (draft.category) {
            case ITEM -> buildItemEntryFields(draft);
            case RECIPE -> buildRecipeEntryFields(draft);
            case BLOCK -> buildBlockEntryFields(draft);
            case GUI -> buildGuiEntryFields(draft);
        }

        Component saveTip = st.ready() ? null : st.reason();
        saveButton = addSideButton(
                0,
                Component.translatable("screen.another_stages.stage_generator.save"),
                this::saveSelected,
                saveTip,
                () -> StageGenCompletion.isReady(draft()));
        addSideButton(1, Component.translatable("screen.another_stages.stage_generator.copy"), this::copyPreview);
        addSideButton(2, doneLabel(), () -> {
            flushEntryBoxes(draft);
            subView = SubView.BROWSE;
            savedScroll = 0;
            rebuild();
        });

        status = new StringWidget(StageGeneratorMenu.SIDE_BTN_W, 10, Component.empty(), font);
        status.setPosition(leftPos + StageGeneratorMenu.SIDE_BTN_X,
                topPos + StageGeneratorMenu.SIDE_BTN_Y
                        + 3 * (StageGeneratorMenu.SIDE_BTN_H + StageGeneratorMenu.SIDE_BTN_GAP));
        addRenderableWidget(status);
    }

    private void buildItemEntryFields(RestrictionDraft draft) {
        int navY = 58;
        int navH = 14;
        int cols = 3;
        addFormButton(formColX(0, cols), navY, formColW(0, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.stages", draft.stages.size()),
                () -> openSub(SubView.STAGES));
        addFormButton(formColX(1, cols), navY, formColW(1, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.items", draft.matchers.size()),
                () -> openSub(SubView.MATCHERS));
        addFormButton(formColX(2, cols), navY, formColW(2, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.exclude", draft.exclude.size()),
                () -> openSub(SubView.EXCLUDE));

        int navY2 = 76;
        addFormButton(formColX(0, cols), navY2, formColW(0, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.nav.cases"),
                () -> openSub(SubView.CASES));
        addFormButton(formColX(1, cols), navY2, formColW(1, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.tooltips", draft.tooltips.size()),
                () -> openSub(SubView.TOOLTIPS));
        addFormButton(formColX(2, cols), navY2, formColW(2, cols), navH,
                Component.literal(draft.consequence),
                () -> {
                    draft.consequence = nextConsequence(draft.consequence);
                    rebuild();
                });

        int row = 96;
        addFormLabel(formColX(0, 2), row, formColW(0, 2), "screen.another_stages.stage_generator.field.recipe_viewer");
        addFormButton(formColX(1, 2), row, formColW(1, 2), 12,
                Component.literal(draft.recipeViewer),
                () -> {
                    draft.recipeViewer = nextViewer(draft.recipeViewer);
                    rebuild();
                });

        row = 112;
        addFormToggle(formColX(0, 2), row, formColW(0, 2), "screen.another_stages.stage_generator.field.can_pickup_label",
                () -> draft.canPickup, v -> draft.canPickup = v);
        addFormToggle(formColX(1, 2), row, formColW(1, 2), "screen.another_stages.stage_generator.field.hide_item_name_label",
                () -> draft.hideItemName, v -> draft.hideItemName = v);

        row = 132;
        addFormLabel(formColX(0, 2), row, formColW(0, 2), "screen.another_stages.stage_generator.field.inventory_ticks");
        ticksBox = addFormBox(formColX(1, 2), row, formColW(1, 2), 12, String.valueOf(draft.inventoryTicks), 8, v -> {
            try {
                draft.inventoryTicks = Integer.parseInt(v.trim());
            } catch (Exception ignored) {
            }
        });
    }

    private void buildRecipeEntryFields(RestrictionDraft draft) {
        int navY = 58;
        int navH = 14;
        int cols = 3;
        addFormButton(formColX(0, cols), navY, formColW(0, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.stages", draft.stages.size()),
                () -> openSub(SubView.STAGES));
        addFormButton(formColX(1, cols), navY, formColW(1, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.outputs", draft.matchers.size()),
                () -> openSub(SubView.MATCHERS));
        addFormButton(formColX(2, cols), navY, formColW(2, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.recipes", draft.recipes.size()),
                () -> openSub(SubView.RECIPES));

        int navY2 = 76;
        addFormButton(formColX(0, cols), navY2, formColW(0, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.recipe_types", draft.recipeTypes.size()),
                () -> openSub(SubView.RECIPE_TYPES));
        addFormButton(formColX(1, cols), navY2, formColW(1, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.inputs", draft.inputs.size()),
                () -> openSub(SubView.INPUTS));
        addFormButton(formColX(2, cols), navY2, formColW(2, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.exclude_recipes", draft.excludeRecipes.size()),
                () -> openSub(SubView.EXCLUDE_RECIPES));

        int row = 96;
        addFormButton(formColX(0, 2), row, formColW(0, 2), 14,
                Component.translatable("screen.another_stages.stage_generator.table.open.tooltips", draft.tooltips.size()),
                () -> openSub(SubView.TOOLTIPS));
        addFormButton(formColX(1, 2), row, formColW(1, 2), 14,
                Component.literal("viewer: " + draft.recipeViewer),
                () -> {
                    draft.recipeViewer = "hide".equals(draft.recipeViewer) ? "overlay" : "hide";
                    rebuild();
                });
    }

    private void buildBlockEntryFields(RestrictionDraft draft) {
        int navY = 58;
        int navH = 14;
        int cols = 3;
        addFormButton(formColX(0, cols), navY, formColW(0, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.stages", draft.stages.size()),
                () -> openSub(SubView.STAGES));
        addFormButton(formColX(1, cols), navY, formColW(1, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.blocks", draft.matchers.size()),
                () -> openSub(SubView.MATCHERS));
        addFormButton(formColX(2, cols), navY, formColW(2, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.exclude", draft.exclude.size()),
                () -> openSub(SubView.EXCLUDE));

        addFormLabel(FORM_LEFT, 78, FORM_WIDTH, "screen.another_stages.stage_generator.field.mimic");
        mimicBox = addFormBox(FORM_LEFT, 88, FORM_WIDTH, 12, draft.mimic, 128, v -> draft.mimic = v == null ? "" : v.trim());

        addFormToggle(formColX(0, 2), 108, formColW(0, 2), "screen.another_stages.stage_generator.field.camouflage",
                () -> draft.camouflage, v -> draft.camouflage = v);
        addFormToggle(formColX(1, 2), 108, formColW(1, 2), "screen.another_stages.stage_generator.field.unbreakable",
                () -> draft.unbreakable, v -> draft.unbreakable = v);
        addFormToggle(formColX(0, 2), 128, formColW(0, 2), "screen.another_stages.stage_generator.field.stop_tick",
                () -> draft.stopTick, v -> draft.stopTick = v);
        addFormToggle(formColX(1, 2), 128, formColW(1, 2), "screen.another_stages.stage_generator.field.break_msg",
                () -> draft.breakMessageDefault, v -> draft.breakMessageDefault = v);
    }

    private void buildGuiEntryFields(RestrictionDraft draft) {
        int navY = 58;
        int navH = 14;
        int cols = 3;
        addFormButton(formColX(0, cols), navY, formColW(0, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.stages", draft.stages.size()),
                () -> openSub(SubView.STAGES));
        addFormButton(formColX(1, cols), navY, formColW(1, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.menu_types", draft.menuTypes.size()),
                () -> openSub(SubView.MENU_TYPES));
        addFormButton(formColX(2, cols), navY, formColW(2, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.menus", draft.menus.size()),
                () -> openSub(SubView.MENUS));

        int navY2 = 76;
        addFormButton(formColX(0, cols), navY2, formColW(0, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.blocked_slots", draft.blockedSlots.size()),
                () -> openSub(SubView.BLOCKED_SLOTS));
        addFormButton(formColX(1, cols), navY2, formColW(1, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.blocked_items", draft.blockedItems.size()),
                () -> openSub(SubView.BLOCKED_ITEMS));

        addFormToggle(formColX(0, 2), 98, formColW(0, 2), "screen.another_stages.stage_generator.field.block_open",
                () -> draft.blockOpen, v -> draft.blockOpen = v);
        addFormToggle(formColX(1, 2), 98, formColW(1, 2), "screen.another_stages.stage_generator.field.gui_msg_default",
                () -> draft.guiMessageDefault, v -> draft.guiMessageDefault = v);

        addFormLabel(formColX(0, 2), 118, formColW(0, 2), "screen.another_stages.stage_generator.field.max_slots");
        maxSlotsBox = addFormBox(formColX(1, 2), 118, formColW(1, 2), 12, String.valueOf(draft.maxSlots), 8, v -> {
            try {
                draft.maxSlots = Integer.parseInt(v.trim());
            } catch (Exception ignored) {
            }
        });
    }

    private void buildCases() {
        RestrictionDraft draft = draft();
        if (draft == null) {
            subView = SubView.ENTRY_EDIT;
            rebuild();
            return;
        }
        StageGenScrollList list = createList(topPos + 8, contentBottom());
        list.addLabel("screen.another_stages.stage_generator.nav.cases");
        list.addToggle("screen.another_stages.stage_generator.field.cases_all",
                () -> draft.casesAll, v -> {
                    draft.casesAll = v;
                    rebuild();
                });
        if (!draft.casesAll) {
            list.addToggle("screen.another_stages.stage_generator.field.case_right",
                    () -> draft.caseRight, v -> draft.caseRight = v);
            list.addToggle("screen.another_stages.stage_generator.field.case_left",
                    () -> draft.caseLeft, v -> draft.caseLeft = v);
            list.addToggle("screen.another_stages.stage_generator.field.case_main",
                    () -> draft.caseMain, v -> draft.caseMain = v);
            list.addToggle("screen.another_stages.stage_generator.field.case_off",
                    () -> draft.caseOff, v -> draft.caseOff = v);
            list.addToggle("screen.another_stages.stage_generator.field.case_inventory",
                    () -> draft.caseInventory, v -> draft.caseInventory = v);
        }
        list.addToggle("screen.another_stages.stage_generator.field.pickup_msg_default",
                () -> draft.pickupMessageDefault, v -> draft.pickupMessageDefault = v);
        list.addToggle("screen.another_stages.stage_generator.field.hide_default_tooltips",
                () -> draft.hideDefaultTooltips, v -> draft.hideDefaultTooltips = v);

        addSideButton(0, doneLabel(), () -> {
            subView = SubView.ENTRY_EDIT;
            savedScroll = 0;
            rebuild();
        });
    }

    private void buildMatcherList() {
        RestrictionDraft draft = draft();
        if (draft == null) {
            subView = SubView.BROWSE;
            rebuild();
            return;
        }
        matcherListParent = subView;
        List<String> target = matcherListFor(draft, subView);
        StageGenScrollList list = createList(topPos + 6, contentBottom());
        String titleKey = switch (subView) {
            case MATCHERS -> "screen.another_stages.stage_generator.table.title.matchers";
            case EXCLUDE -> "screen.another_stages.stage_generator.table.title.exclude";
            case BLOCKED_ITEMS -> "screen.another_stages.stage_generator.table.title.blocked_items";
            case INPUTS -> "screen.another_stages.stage_generator.table.title.inputs";
            default -> "screen.another_stages.stage_generator.table.title.matchers";
        };
        list.addLabel(titleKey);
        list.addLabel("screen.another_stages.stage_generator.matcher.list_hint");

        if (target.isEmpty()) {
            list.addLabel("screen.another_stages.stage_generator.matcher.empty");
        } else {
            for (int i = 0; i < target.size(); i++) {
                int idx = i;
                String id = target.get(i);
                ItemStack icon = StageGenIcons.stackFromMatcher(id);
                String glyph = icon.isEmpty() ? StageGenIcons.glyphForMatcher(id) : null;
                list.addBrowseRow(icon, glyph, Component.literal(id == null || id.isBlank() ? "…" : id),
                        Component.empty(), true, () -> openMatcherEntry(idx));
            }
        }

        addSideButton(0, Component.translatable("screen.another_stages.stage_generator.side.new"), () -> {
            target.add("");
            openMatcherEntry(target.size() - 1);
        });
        addSideButton(1, Component.translatable("screen.another_stages.stage_generator.side.delete"), () -> {
            if (editingMatcherIndex >= 0 && editingMatcherIndex < target.size()) {
                target.remove(editingMatcherIndex);
            } else if (!target.isEmpty()) {
                target.remove(target.size() - 1);
            }
            editingMatcherIndex = -1;
            savedScroll = 0;
            rebuild();
        }, null, () -> !target.isEmpty());
        addSideButton(2, doneLabel(), () -> {
            subView = SubView.ENTRY_EDIT;
            savedScroll = 0;
            rebuild();
        });
    }

    private void openMatcherEntry(int index) {
        RestrictionDraft draft = draft();
        if (draft == null) {
            return;
        }
        List<String> target = matcherListFor(draft, matcherListParent);
        if (index < 0 || index >= target.size()) {
            return;
        }
        editingMatcherIndex = index;
        matcherKind = detectMatcherKind(target.get(index));
        subView = SubView.MATCHER_ENTRY;
        savedScroll = 0;
        rebuild();
    }

    private static MatcherKind detectMatcherKind(@Nullable String value) {
        if (value == null) {
            return MatcherKind.ITEM;
        }
        String v = value.trim();
        if (v.startsWith("#")) {
            return MatcherKind.TAG;
        }
        if (v.startsWith("@")) {
            return MatcherKind.MOD;
        }
        return MatcherKind.ITEM;
    }

    private void buildMatcherEntry() {
        RestrictionDraft draft = draft();
        if (draft == null) {
            subView = SubView.ENTRY_EDIT;
            rebuild();
            return;
        }
        List<String> target = matcherListFor(draft, matcherListParent);
        if (editingMatcherIndex < 0 || editingMatcherIndex >= target.size()) {
            subView = matcherListParent;
            rebuild();
            return;
        }
        String current = target.get(editingMatcherIndex);

        addFormLabel(FORM_LEFT, 8, FORM_WIDTH, "screen.another_stages.stage_generator.matcher.entry_title");
        addFormLabel(FORM_LEFT, 20, FORM_WIDTH, "screen.another_stages.stage_generator.matcher.type");

        int cols = 3;
        addFormButton(formColX(0, cols), 32, formColW(0, cols), 14,
                Component.translatable("screen.another_stages.stage_generator.matcher.type.item"),
                () -> {
                    matcherKind = MatcherKind.ITEM;
                    stripPrefixToItem(target);
                    rebuild();
                });
        addFormButton(formColX(1, cols), 32, formColW(1, cols), 14,
                Component.translatable("screen.another_stages.stage_generator.matcher.type.tag"),
                () -> {
                    matcherKind = MatcherKind.TAG;
                    applyKindPrefix(target, "#");
                    rebuild();
                });
        addFormButton(formColX(2, cols), 32, formColW(2, cols), 14,
                Component.translatable("screen.another_stages.stage_generator.matcher.type.mod"),
                () -> {
                    matcherKind = MatcherKind.MOD;
                    applyKindPrefix(target, "@");
                    rebuild();
                });

        addFormLabel(FORM_LEFT, 52, FORM_WIDTH, "screen.another_stages.stage_generator.matcher.value");
        String display = current == null ? "" : current;
        if (matcherKind == MatcherKind.TAG && display.startsWith("#")) {
            display = display.substring(1);
        } else if (matcherKind == MatcherKind.MOD && display.startsWith("@")) {
            display = display.substring(1);
        }
        matcherValueBox = addFormBox(FORM_LEFT, 62, FORM_WIDTH, 14, display, 256, v -> {});

        if (matcherKind == MatcherKind.ITEM) {
            addFormLabel(FORM_LEFT, 84, FORM_WIDTH, "screen.another_stages.stage_generator.ghost_hint");
            syncGhostFromValue(current);
        }

        addSideButton(0, doneLabel(), () -> {
            commitMatcherEntry(target);
            subView = matcherListParent;
            editingMatcherIndex = -1;
            savedScroll = 0;
            rebuild();
        });
    }

    private void stripPrefixToItem(List<String> target) {
        if (editingMatcherIndex < 0 || editingMatcherIndex >= target.size()) {
            return;
        }
        String v = target.get(editingMatcherIndex);
        if (v == null) {
            return;
        }
        if (v.startsWith("#") || v.startsWith("@")) {
            target.set(editingMatcherIndex, v.substring(1));
        }
    }

    private void applyKindPrefix(List<String> target, String prefix) {
        if (editingMatcherIndex < 0 || editingMatcherIndex >= target.size()) {
            return;
        }
        String v = target.get(editingMatcherIndex) == null ? "" : target.get(editingMatcherIndex).trim();
        if (v.startsWith("#") || v.startsWith("@")) {
            v = v.substring(1);
        }
        target.set(editingMatcherIndex, prefix + v);
        menu.clearGhost();
    }

    private void commitMatcherEntry(List<String> target) {
        if (matcherValueBox == null || editingMatcherIndex < 0 || editingMatcherIndex >= target.size()) {
            return;
        }
        String raw = matcherValueBox.getValue() == null ? "" : matcherValueBox.getValue().trim();
        String value = switch (matcherKind) {
            case ITEM -> raw.startsWith("#") || raw.startsWith("@") ? raw.substring(1) : raw;
            case TAG -> raw.startsWith("#") ? raw : "#" + raw;
            case MOD -> raw.startsWith("@") ? raw : "@" + raw;
        };
        target.set(editingMatcherIndex, value);
    }

    private void syncGhostFromValue(@Nullable String value) {
        if (value == null || value.isBlank() || value.startsWith("#") || value.startsWith("@")) {
            menu.clearGhost();
            return;
        }
        Identifier id = Identifier.tryParse(value.contains(":") ? value : "minecraft:" + value);
        if (id == null) {
            menu.clearGhost();
            return;
        }
        var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            menu.clearGhost();
            return;
        }
        menu.setGhostStack(new ItemStack(item));
    }

    private void buildStageList() {
        RestrictionDraft draft = draft();
        if (draft == null) {
            subView = SubView.ENTRY_EDIT;
            rebuild();
            return;
        }
        StageGenScrollList list = createList(topPos + 6, contentBottom());
        list.addLabel("screen.another_stages.stage_generator.table.title.stages");
        if (draft.stages.isEmpty()) {
            list.addLabel("screen.another_stages.stage_generator.stages.empty");
        } else {
            for (int i = 0; i < draft.stages.size(); i++) {
                int idx = i;
                RestrictionDraft.StageRow row = draft.stages.get(i);
                String label = row.type + " | " + (row.id == null || row.id.isBlank() ? "…" : row.id)
                        + " | is=" + row.is;
                list.addBrowseRow(ItemStack.EMPTY, "S", Component.literal(label),
                        Component.empty(), !row.id.isBlank(), () -> openStageEntry(idx));
            }
        }
        addSideButton(0, Component.translatable("screen.another_stages.stage_generator.side.new"), () -> {
            draft.stages.add(new RestrictionDraft.StageRow());
            openStageEntry(draft.stages.size() - 1);
        });
        addSideButton(1, Component.translatable("screen.another_stages.stage_generator.side.delete"), () -> {
            if (!draft.stages.isEmpty()) {
                int idx = editingStageIndex >= 0 && editingStageIndex < draft.stages.size()
                        ? editingStageIndex
                        : draft.stages.size() - 1;
                draft.stages.remove(idx);
                if (draft.stages.isEmpty()) {
                    draft.stages.add(new RestrictionDraft.StageRow());
                }
            }
            editingStageIndex = -1;
            savedScroll = 0;
            rebuild();
        });
        addSideButton(2, doneLabel(), () -> {
            subView = SubView.ENTRY_EDIT;
            savedScroll = 0;
            rebuild();
        });
    }

    private void openStageEntry(int index) {
        RestrictionDraft draft = draft();
        if (draft == null || index < 0 || index >= draft.stages.size()) {
            return;
        }
        editingStageIndex = index;
        subView = SubView.STAGE_ENTRY;
        savedScroll = 0;
        rebuild();
    }

    private void buildStageEntry() {
        RestrictionDraft draft = draft();
        if (draft == null || editingStageIndex < 0 || editingStageIndex >= draft.stages.size()) {
            subView = SubView.STAGES;
            rebuild();
            return;
        }
        RestrictionDraft.StageRow row = draft.stages.get(editingStageIndex);

        addFormLabel(FORM_LEFT, 8, FORM_WIDTH, "screen.another_stages.stage_generator.stages.entry_title");
        addFormLabel(FORM_LEFT, 22, FORM_WIDTH, "screen.another_stages.stage_generator.table.col.type");
        addFormButton(FORM_LEFT, 34, FORM_WIDTH, 14, Component.literal(row.type), () -> {
            row.type = switch (row.type == null ? "player" : row.type.toLowerCase(Locale.ROOT)) {
                case "world" -> "team";
                case "team" -> "player";
                default -> "world";
            };
            rebuild();
        });

        addFormToggle(FORM_LEFT, 56, FORM_WIDTH, "screen.another_stages.stage_generator.table.col.is",
                () -> row.is, v -> row.is = v);

        addFormLabel(FORM_LEFT, 78, FORM_WIDTH, "screen.another_stages.stage_generator.stages.id_hint");
        stageIdBox = new SuggestingEditBox(
                font,
                leftPos + FORM_LEFT,
                topPos + 90,
                FORM_WIDTH,
                14,
                Component.translatable("screen.another_stages.stage_generator.table.col.id"),
                StageGenSuggestions::stageIds);
        stageIdBox.setMaxLength(128);
        stageIdBox.setValue(row.id == null ? "" : row.id);
        stageIdBox.setResponder(v -> row.id = v == null ? "" : v.trim());
        stageIdBox.refreshSuggestions();
        addRenderableWidget(stageIdBox);

        addSideButton(0, doneLabel(), () -> {
            if (stageIdBox != null) {
                row.id = stageIdBox.getValue().trim();
            }
            subView = SubView.STAGES;
            editingStageIndex = -1;
            savedScroll = 0;
            rebuild();
        });
    }

    private void openSub(SubView view) {
        RestrictionDraft draft = draft();
        if (draft != null) {
            flushEntryBoxes(draft);
        }
        if (view == SubView.MATCHERS || view == SubView.EXCLUDE
                || view == SubView.INPUTS || view == SubView.BLOCKED_ITEMS) {
            matcherListParent = view;
        }
        subView = view;
        savedScroll = 0;
        rebuild();
    }

    private void flushEntryBoxes(RestrictionDraft draft) {
        if (idBox != null) {
            draft.id = idBox.getValue().trim();
            draft.ensureId();
        }
        if (mimicBox != null) {
            draft.mimic = mimicBox.getValue().trim();
        }
        if (ticksBox != null) {
            try {
                draft.inventoryTicks = Integer.parseInt(ticksBox.getValue().trim());
            } catch (Exception ignored) {
            }
        }
        if (maxSlotsBox != null) {
            try {
                draft.maxSlots = Integer.parseInt(maxSlotsBox.getValue().trim());
            } catch (Exception ignored) {
            }
        }
    }

    private void buildTableSubview() {
        RestrictionDraft draft = draft();
        if (draft == null) {
            subView = SubView.BROWSE;
            rebuild();
            return;
        }
        RestrictionDraft.TableKind kind = tableKindFor(subView);
        if (kind == null) {
            subView = SubView.ENTRY_EDIT;
            rebuild();
            return;
        }
        StageGenScrollList list = createList(topPos + 6, contentBottom());
        tablePanel = new StageGenTablePanel(
                draft,
                kind,
                font,
                () -> {
                    subView = SubView.ENTRY_EDIT;
                    savedScroll = 0;
                    rebuild();
                },
                this::rebuild);
        tablePanel.populate(list);
        // Table panel already adds Done inside list; also side Done for consistency (remove table's duplicate by only using side)
        addSideButton(0, doneLabel(), () -> {
            subView = SubView.ENTRY_EDIT;
            savedScroll = 0;
            rebuild();
        });
    }

    private List<String> matcherListFor(RestrictionDraft draft, SubView view) {
        SubView effective = view == SubView.MATCHER_ENTRY ? matcherListParent : view;
        return switch (effective) {
            case EXCLUDE -> draft.exclude;
            case BLOCKED_ITEMS -> draft.blockedItems;
            case INPUTS -> draft.inputs;
            default -> draft.matchers;
        };
    }

    private @Nullable RestrictionDraft.TableKind tableKindFor(SubView view) {
        return switch (view) {
            case TOOLTIPS -> RestrictionDraft.TableKind.TOOLTIPS;
            case BLOCKED_SLOTS -> RestrictionDraft.TableKind.BLOCKED_SLOTS;
            case RECIPES -> RestrictionDraft.TableKind.RECIPES;
            case RECIPE_TYPES -> RestrictionDraft.TableKind.RECIPE_TYPES;
            case EXCLUDE_RECIPES -> RestrictionDraft.TableKind.EXCLUDE_RECIPES;
            case MENUS -> RestrictionDraft.TableKind.MENUS;
            case MENU_TYPES -> RestrictionDraft.TableKind.MENU_TYPES;
            default -> null;
        };
    }

    private void onGhostChanged() {
        if (!acceptsGhostInCurrentView()) {
            return;
        }
        ItemStack stack = menu.getGhostStack();
        if (stack.isEmpty() || matcherValueBox == null) {
            return;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return;
        }
        matcherValueBox.setValue(id.toString());
    }

    private void saveSelected() {
        RestrictionDraft draft = draft();
        if (draft == null) {
            return;
        }
        flushEntryBoxes(draft);
        StageGenCompletion.Status st = StageGenCompletion.evaluate(draft);
        if (!st.ready()) {
            setStatus(st.reason());
            return;
        }
        if (!AsConfig.preferKubejs()) {
            String first = StageGenClientPaths.firstDatapackPath();
            if (first.isBlank() || "(empty)".equals(first)) {
                setStatus(Component.translatable("screen.another_stages.stage_generator.save.no_datapack_paths"));
                return;
            }
        }
        ClientPacketDistributor.sendToServer(new SaveStageGeneratorPayload(draft.toJson().toString()));
        setStatus(Component.translatable("screen.another_stages.stage_generator.save.sent"));
    }

    private void copyPreview() {
        RestrictionDraft draft = draft();
        if (draft == null) {
            return;
        }
        flushEntryBoxes(draft);
        String text = AsConfig.preferKubejs() ? KubeExporter.export(draft) : JsonExporter.export(draft);
        if (minecraft != null) {
            minecraft.keyboardHandler.setClipboard(text);
        }
        setStatus(Component.translatable("screen.another_stages.stage_generator.copied"));
    }

    private void setStatus(Component msg) {
        if (status != null) {
            status.setMessage(msg);
        }
    }

    private static @Nullable String firstMatcherId(RestrictionDraft d) {
        if (!d.matchers.isEmpty()) {
            return d.matchers.get(0);
        }
        if (!d.blockedItems.isEmpty()) {
            return d.blockedItems.get(0);
        }
        return null;
    }

    private static ItemStack firstMatcherIcon(RestrictionDraft d) {
        return StageGenIcons.stackFromMatcher(firstMatcherId(d));
    }

    private static String nextConsequence(String cur) {
        return switch (cur == null ? "drop" : cur.toLowerCase(Locale.ROOT)) {
            case "drop" -> "delete";
            case "delete" -> "block";
            case "block" -> "block_drop";
            case "block_drop" -> "block_delete";
            default -> "drop";
        };
    }

    private static String nextViewer(String cur) {
        return switch (cur == null ? "overlay" : cur.toLowerCase(Locale.ROOT)) {
            case "overlay" -> "hide";
            case "hide" -> "hardcore";
            default -> "overlay";
        };
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1A1A1E);
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF5A5A68);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF5A5A68);
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF5A5A68);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF5A5A68);

        if (menu.slotsVisible()) {
            int gx = leftPos + StageGeneratorMenu.GHOST_X - 1;
            int gy = topPos + StageGeneratorMenu.GHOST_Y - 1;
            graphics.fill(gx, gy, gx + 18, gy + 18, 0xFF2C2C32);
            graphics.fill(gx, gy, gx + 18, gy + 1, 0xFF8A8A9A);
            graphics.fill(gx, gy + 17, gx + 18, gy + 18, 0xFF8A8A9A);
            graphics.fill(gx, gy, gx + 1, gy + 18, 0xFF8A8A9A);
            graphics.fill(gx + 17, gy, gx + 18, gy + 18, 0xFF8A8A9A);

            int px = leftPos + StageGeneratorMenu.PLAYER_INV_X - 1;
            int py = topPos + StageGeneratorMenu.PLAYER_INV_Y - 1;
            graphics.fill(px, py, px + 9 * 18 + 2, py + 3 * 18 + 22, 0x662C2C32);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (saveButton != null) {
            saveButton.refreshActive();
        }
        if (!menu.slotsVisible()) {
            hoveredSlot = null;
            for (var renderable : renderables) {
                renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
            if (stageIdBox != null) {
                stageIdBox.extractSuggestions(graphics, font);
            }
            return;
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (stageIdBox != null) {
            stageIdBox.extractSuggestions(graphics, font);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (stageIdBox != null && stageIdBox.isFocused() && stageIdBox.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (scrollList != null && scrollList.beginScrollbarDrag(event)) {
            setFocused(scrollList);
            setDragging(true);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (scrollList != null && (scrollList.isDraggingScrollbar() || scrollList.isOverScrollbar(event.x(), event.y()))) {
            if (scrollList.mouseDragged(event, dx, dy)) {
                return true;
            }
        }
        if (scrollList != null && scrollList.isDraggingScrollbar()) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (scrollList != null && scrollList.isDraggingScrollbar()) {
            scrollList.mouseReleased(event);
            setDragging(false);
            return true;
        }
        if (scrollList != null) {
            scrollList.endScrollbarDrag();
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollList != null && scrollList.isMouseOver(mouseX, mouseY)) {
            return scrollList.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (scrollList != null && !menu.slotsVisible()) {
            return scrollList.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public @Nullable Rect2i getGhostTargetArea() {
        if (!acceptsGhostInCurrentView()) {
            return null;
        }
        return new Rect2i(
                leftPos + StageGeneratorMenu.GHOST_X,
                topPos + StageGeneratorMenu.GHOST_Y,
                16,
                16);
    }

    @Override
    public void acceptGhostItem(ItemStack stack) {
        if (!acceptsGhostInCurrentView() || stack == null || stack.isEmpty()) {
            return;
        }
        menu.setGhostStack(stack);
    }

    @Override
    public boolean acceptsGhostItems() {
        return acceptsGhostInCurrentView();
    }
}
