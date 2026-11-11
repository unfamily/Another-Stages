package net.unfamily.anotherstages.client.gui.stagegen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.anotherstages.client.gui.AsButton;
import net.unfamily.anotherstages.client.gui.AsToggle;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.menu.StageGeneratorMenu;
import net.unfamily.anotherstages.network.DeleteStageGeneratorPayload;
import net.unfamily.anotherstages.network.SaveKnownStagesPayload;
import net.unfamily.anotherstages.network.SaveStageGeneratorPayload;
import net.unfamily.anotherstages.stagegen.StageGenCompletion;
import net.unfamily.anotherstages.stagegen.StageGenPaths;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Shop-style Stage Generator: HOME → Browse → ENTRY_EDIT → nested lists/entries.
 * Autosave when Ready (no Save/Copy); close shows reload hint like ShopEdit.
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
        MENU_TYPES,
        STRING_ENTRY,
        KNOWN_STAGES,
        KNOWN_STAGE_ENTRY
    }

    private record AreaButton(String labelKey, RestrictionDraft.Category category) {}

    private static final List<AreaButton> HOME_AREAS = List.of(
            new AreaButton("screen.another_stages.stage_generator.cat.items", RestrictionDraft.Category.ITEM),
            new AreaButton("screen.another_stages.stage_generator.cat.recipes", RestrictionDraft.Category.RECIPE),
            new AreaButton("screen.another_stages.stage_generator.cat.blocks", RestrictionDraft.Category.BLOCK),
            new AreaButton("screen.another_stages.stage_generator.cat.guis", RestrictionDraft.Category.GUI)
    );

    private static final int FORM_LEFT = StageGeneratorMenu.LIST_X + 4;
    /** Form uses full panel width; actions are on the bottom bar (or beside inv). */
    private static final int FORM_RIGHT = StageGeneratorMenu.LIST_RIGHT;
    private static final int FORM_WIDTH = FORM_RIGHT - FORM_LEFT;
    private static final int FORM_GAP = 4;

    private SubView subView = SubView.HOME;
    /** Parent list when on MATCHER_ENTRY (MATCHERS / EXCLUDE / INPUTS / BLOCKED_ITEMS). */
    private SubView matcherListParent = SubView.MATCHERS;
    private int editingMatcherIndex = -1;
    /** Shop-style calibrator cycle: item id / #tags / @mod from ghost stack. */
    private final List<String> matcherVariants = new ArrayList<>();
    private int matcherVariantIndex = 0;
    private int editingStageIndex = -1;
    private int editingKnownIndex = -1;
    /** Parent list when on STRING_ENTRY (tooltips / recipes / menus / slots / …). */
    private SubView stringListParent = SubView.TOOLTIPS;
    private int editingStringIndex = -1;

    private @Nullable StageGenScrollList scrollList;
    private @Nullable EditBox mimicBox;
    private @Nullable EditBox ticksBox;
    private @Nullable EditBox maxSlotsBox;
    private @Nullable EditBox matcherValueBox;
    private @Nullable EditBox stringValueBox;
    private @Nullable SuggestingEditBox stageIdBox;
    private @Nullable EditBox searchBox;
    private final List<EditBox> formBoxes = new ArrayList<>();
    private final List<AsButton> bottomBarButtons = new ArrayList<>();
    private double savedScroll = 0;
    /** Shop-style close dialog: reload hint before leaving the GUI. */
    private boolean closeHint;
    private boolean closingConfirmed;
    /** List filter query; cleared when {@link #subView} changes. */
    private String listSearchQuery = "";
    private @Nullable SubView searchQueryView;
    private boolean refocusSearch;

    public StageGeneratorScreen(StageGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = StageGeneratorMenu.GUI_WIDTH;
        this.imageHeight = StageGeneratorMenu.GUI_HEIGHT;
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
            savedScroll = scrollList.getScrollAmount();
        }
        clearWidgets();
        scrollList = null;
        mimicBox = null;
        ticksBox = null;
        maxSlotsBox = null;
        matcherValueBox = null;
        stringValueBox = null;
        stageIdBox = null;
        searchBox = null;
        formBoxes.clear();
        bottomBarButtons.clear();

        boolean showSlots = showInventorySlots();
        menu.setSlotsVisible(showSlots);
        if (!showSlots) {
            menu.clearGhost();
        }

        if (closeHint) {
            buildCloseHint();
            layoutBottomBar();
            if (scrollList != null) {
                scrollList.setScrollAmount(savedScroll);
            }
            refocusSearch = false;
            return;
        }

        switch (subView) {
            case PREFER -> buildPrefer();
            case HOME -> buildHome();
            case BROWSE -> buildBrowse();
            case ENTRY_EDIT -> buildEntryEdit();
            case CASES -> buildCases();
            case MATCHER_ENTRY -> buildMatcherEntry();
            case STAGE_ENTRY -> buildStageEntry();
            case STRING_ENTRY -> buildStringEntry();
            case MATCHERS, EXCLUDE, BLOCKED_ITEMS, INPUTS -> buildMatcherList();
            case STAGES -> buildStageList();
            case TOOLTIPS, BLOCKED_SLOTS, RECIPES, RECIPE_TYPES, EXCLUDE_RECIPES, MENUS, MENU_TYPES ->
                    buildStringList();
            case KNOWN_STAGES -> buildKnownStagesList();
            case KNOWN_STAGE_ENTRY -> buildKnownStageEntry();
            default -> {
                subView = SubView.ENTRY_EDIT;
                rebuild();
                return;
            }
        }

        layoutBottomBar();
        if (scrollList != null) {
            scrollList.setScrollAmount(savedScroll);
        }
        if (refocusSearch && searchBox != null) {
            setFocused(searchBox);
            searchBox.setFocused(true);
            searchBox.moveCursorToEnd(false);
            refocusSearch = false;
        }
    }

    private boolean showInventorySlots() {
        return subView == SubView.MATCHER_ENTRY;
    }

    private boolean acceptsGhostInCurrentView() {
        return showInventorySlots();
    }

    private int contentBottom() {
        if (showInventorySlots()) {
            // Calibrator ghost sits in the form row; reserve space above the player inv band.
            return topPos + StageGeneratorMenu.PLAYER_INV_Y - 6;
        }
        return topPos + StageGeneratorMenu.BOTTOM_BAR_Y - 4;
    }

    private int matcherSlotX() {
        return StageGeneratorMenu.GHOST_X;
    }

    private int matcherRightArrowX() {
        return matcherSlotX() + StageGeneratorMenu.MATCHER_SLOT + StageGeneratorMenu.MATCHER_ARROW_GAP;
    }

    private int matcherEditBoxX() {
        return matcherRightArrowX() + StageGeneratorMenu.MATCHER_ARROW + StageGeneratorMenu.MATCHER_ARROW_GAP;
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
        formBoxes.add(box);
        return box;
    }

    private void bindListSearch() {
        if (searchQueryView != subView) {
            listSearchQuery = "";
            searchQueryView = subView;
        }
    }

    private boolean matchesSearch(@Nullable String text) {
        if (listSearchQuery == null || listSearchQuery.isBlank()) {
            return true;
        }
        if (text == null) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(listSearchQuery.toLowerCase(Locale.ROOT));
    }

    /**
     * Fixed search bar above a list. Returns the Y for the list top (below the bar).
     */
    private int addListSearchBar(int guiTopY) {
        bindListSearch();
        searchBox = addFormBox(FORM_LEFT, guiTopY, FORM_WIDTH, 14, listSearchQuery, 128, v -> {
            String next = v == null ? "" : v;
            if (next.equals(listSearchQuery)) {
                return;
            }
            listSearchQuery = next;
            refocusSearch = true;
            savedScroll = 0;
            rebuild();
        });
        searchBox.setHint(Component.translatable("screen.another_stages.stage_generator.search_hint"));
        return topPos + guiTopY + 18;
    }

    private AsButton addFormButton(int x, int y, int w, int h, Component label, Runnable action) {
        AsButton btn = AsButton.text(w, h, label, action);
        btn.setPosition(leftPos + x, topPos + y);
        addRenderableWidget(btn);
        return btn;
    }

    /** Left click = forward, right click = back (Shop-style multi-mode selector). */
    private AsButton addFormCycle(int x, int y, int w, int h, Component label, java.util.function.IntConsumer delta) {
        AsButton btn = AsButton.cycle(w, h, label, d -> {
            delta.accept(d);
            autosaveIfReady();
        });
        btn.setPosition(leftPos + x, topPos + y);
        addRenderableWidget(btn);
        return btn;
    }

    private void addFormToggle(int x, int y, int colW, String labelKey, BooleanSupplier get, Consumer<Boolean> set) {
        int labelW = Math.max(24, colW - AsToggle.WIDTH - 4);
        StringWidget sw = new StringWidget(labelW, 12, Component.translatable(labelKey), font);
        sw.setPosition(leftPos + x, topPos + y + 1);
        addRenderableWidget(sw);
        AsToggle toggle = new AsToggle(get, v -> {
            set.accept(v);
            autosaveIfReady();
        });
        toggle.setPosition(leftPos + x + colW - AsToggle.WIDTH, topPos + y);
        addRenderableWidget(toggle);
    }

    private AsButton addBottomButton(Component label, Runnable action) {
        return addBottomButton(label, action, null, () -> true);
    }

    private AsButton addBottomButton(
            Component label,
            Runnable action,
            @Nullable Component tooltip,
            Supplier<Boolean> activeWhen
    ) {
        AsButton btn = AsButton.text(
                60,
                StageGeneratorMenu.BOTTOM_BTN_H,
                label,
                action,
                tooltip);
        btn.activeWhen(activeWhen);
        btn.refreshActive();
        addRenderableWidget(btn);
        bottomBarButtons.add(btn);
        return btn;
    }

    private void layoutBottomBar() {
        int n = bottomBarButtons.size();
        if (n == 0) {
            return;
        }
        int usable = StageGeneratorMenu.LIST_WIDTH;
        int gap = StageGeneratorMenu.BOTTOM_BTN_GAP;
        int w = Math.max(40, (usable - gap * (n - 1)) / n);
        int x = StageGeneratorMenu.LIST_X;
        int y = StageGeneratorMenu.BOTTOM_BAR_Y;
        for (AsButton btn : bottomBarButtons) {
            btn.setWidth(w);
            btn.setPosition(leftPos + x, topPos + y);
            x += w + gap;
        }
    }

    /** Done beside player inventory — matcher-entry item only. */
    private AsButton addInvSideDone(Runnable action) {
        AsButton btn = AsButton.text(
                StageGeneratorMenu.SIDE_BTN_W,
                StageGeneratorMenu.SIDE_BTN_H,
                doneLabel(),
                action);
        btn.setPosition(
                leftPos + StageGeneratorMenu.SIDE_BTN_X,
                topPos + StageGeneratorMenu.SIDE_BTN_Y);
        addRenderableWidget(btn);
        return btn;
    }

    private Component doneLabel() {
        return Component.translatable("screen.another_stages.stage_generator.side.done");
    }

    private void buildPrefer() {
        StageGenScrollList list = createList(topPos + 8, contentBottom());
        list.addLabel("screen.another_stages.stage_generator.prefer_kubejs.title");
        list.addWrappedLabel("screen.another_stages.stage_generator.prefer_kubejs.message");
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
        list.addActionButton(
                Component.translatable("screen.another_stages.stage_generator.home.known_stages"),
                () -> {
                    subView = SubView.KNOWN_STAGES;
                    savedScroll = 0;
                    rebuild();
                });
        addBottomButton(doneLabel(), this::openCloseHint);
    }

    private void buildCloseHint() {
        StageGenScrollList list = createList(topPos + 8, contentBottom());
        list.addLabel("screen.another_stages.stage_generator.close_hint_title");
        list.addWrappedLabel("screen.another_stages.stage_generator.close_reload_hint");
        addBottomButton(
                Component.translatable("screen.another_stages.stage_generator.close"),
                this::confirmClose);
    }

    private void openCloseHint() {
        closeHint = true;
        savedScroll = 0;
        rebuild();
    }

    private void confirmClose() {
        closingConfirmed = true;
        onClose();
    }

    @Override
    public void onClose() {
        if (!closingConfirmed) {
            if (closeHint) {
                confirmClose();
                return;
            }
            if (subView == SubView.HOME || subView == SubView.PREFER) {
                openCloseHint();
                return;
            }
            navigateUp();
            return;
        }
        super.onClose();
    }

    /** One layer up (ESC / Done hierarchy). Autosaves Ready drafts when leaving ENTRY_EDIT. */
    private void navigateUp() {
        switch (subView) {
            case KNOWN_STAGE_ENTRY -> {
                autosaveKnownStages();
                editingKnownIndex = -1;
                subView = SubView.KNOWN_STAGES;
                savedScroll = 0;
                rebuild();
            }
            case KNOWN_STAGES -> {
                autosaveKnownStages();
                subView = SubView.HOME;
                savedScroll = 0;
                rebuild();
            }
            case BROWSE -> {
                subView = SubView.HOME;
                savedScroll = 0;
                rebuild();
            }
            case ENTRY_EDIT -> {
                RestrictionDraft d = draft();
                if (d != null) {
                    flushEntryBoxes(d);
                }
                autosaveIfReady();
                subView = SubView.BROWSE;
                savedScroll = 0;
                rebuild();
            }
            case CASES, STAGES, MATCHERS, EXCLUDE, BLOCKED_ITEMS, INPUTS,
                    TOOLTIPS, BLOCKED_SLOTS, RECIPES, RECIPE_TYPES, EXCLUDE_RECIPES, MENUS, MENU_TYPES -> {
                subView = SubView.ENTRY_EDIT;
                savedScroll = 0;
                rebuild();
            }
            case MATCHER_ENTRY -> {
                RestrictionDraft d = draft();
                if (d != null) {
                    List<String> target = matcherListFor(d, matcherListParent);
                    commitMatcherEntry(target);
                }
                subView = matcherListParent;
                editingMatcherIndex = -1;
                savedScroll = 0;
                rebuild();
            }
            case STAGE_ENTRY -> {
                RestrictionDraft d = draft();
                if (d != null && editingStageIndex >= 0 && editingStageIndex < d.stages.size()
                        && stageIdBox != null) {
                    d.stages.get(editingStageIndex).id = stageIdBox.getValue().trim();
                }
                subView = SubView.STAGES;
                editingStageIndex = -1;
                savedScroll = 0;
                rebuild();
            }
            case STRING_ENTRY -> {
                RestrictionDraft d = draft();
                if (d != null) {
                    commitStringEntry(d);
                }
                subView = stringListParent;
                editingStringIndex = -1;
                savedScroll = 0;
                rebuild();
            }
            default -> openCloseHint();
        }
    }

    /** Silent file write when the selected draft is Ready (Shop-style autosave). */
    private void autosaveIfReady() {
        RestrictionDraft draft = draft();
        if (draft == null) {
            return;
        }
        flushEntryBoxes(draft);
        if (!StageGenCompletion.isReady(draft)) {
            return;
        }
        if (!AsConfig.preferKubejs()) {
            String first = StageGenClientPaths.firstDatapackPath();
            if (first.isBlank() || "(empty)".equals(first)) {
                return;
            }
        } else if (draft.kubeEntryIndex < 0) {
            // Claim next Entry index before send so a second save replaces instead of appending.
            int max = -1;
            for (RestrictionDraft other : session().all()) {
                if (other.category == draft.category && other.kubeEntryIndex > max) {
                    max = other.kubeEntryIndex;
                }
            }
            draft.kubeEntryIndex = max + 1;
        }
        PacketDistributor.sendToServer(new SaveStageGeneratorPayload(draft.toJson().toString()));
    }

    /** Silent rewrite of known-stages catalog (all ready rows). */
    private void autosaveKnownStages() {
        if (editingKnownIndex >= 0 && editingKnownIndex < session().knownStages().size() && stageIdBox != null) {
            KnownStageDraft cur = session().knownStages().get(editingKnownIndex);
            cur.stage = stageIdBox.getValue() == null ? "" : stageIdBox.getValue().trim();
        }
        if (!AsConfig.preferKubejs()) {
            String first = StageGenClientPaths.firstDatapackPath();
            if (first.isBlank() || "(empty)".equals(first)) {
                return;
            }
        }
        List<String> blobs = new ArrayList<>();
        for (KnownStageDraft d : session().knownStages()) {
            if (d != null && d.isReady()) {
                blobs.add(d.toJson().toString());
            }
        }
        PacketDistributor.sendToServer(new SaveKnownStagesPayload(blobs));
    }

    private void buildKnownStagesList() {
        StageGenScrollList list = createList(topPos + 8, contentBottom());
        list.addLabel("screen.another_stages.stage_generator.known.title");
        list.addWrappedLabel("screen.another_stages.stage_generator.known.hint");
        List<KnownStageDraft> rows = session().knownStages();
        if (rows.isEmpty()) {
            list.addLabel("screen.another_stages.stage_generator.known.empty");
        } else {
            for (int i = 0; i < rows.size(); i++) {
                KnownStageDraft d = rows.get(i);
                int index = i;
                boolean ready = d.isReady();
                String reason = d.incompleteReasonKey();
                list.addBrowseRow(
                        ItemStack.EMPTY,
                        "§",
                        Component.literal(d.summary()),
                        reason == null
                                ? Component.translatable("screen.another_stages.stage_generator.complete.ready")
                                : Component.translatable(reason),
                        ready,
                        () -> openKnownStageEntry(index));
            }
        }
        addBottomButton(Component.translatable("screen.another_stages.stage_generator.side.new"), () -> {
            KnownStageDraft created = session().createKnown();
            openKnownStageEntry(session().knownStages().indexOf(created));
        });
        addBottomButton(Component.translatable("screen.another_stages.stage_generator.side.delete"), () -> {
            if (session().knownSelected() == null) {
                return;
            }
            session().removeKnownSelected();
            autosaveKnownStages();
            rebuild();
        }, null, () -> session().knownSelected() != null);
        addBottomButton(doneLabel(), this::navigateUp);
    }

    private void openKnownStageEntry(int index) {
        if (index < 0 || index >= session().knownStages().size()) {
            return;
        }
        session().selectKnownIndex(index);
        editingKnownIndex = index;
        subView = SubView.KNOWN_STAGE_ENTRY;
        savedScroll = 0;
        rebuild();
    }

    private void buildKnownStageEntry() {
        if (editingKnownIndex < 0 || editingKnownIndex >= session().knownStages().size()) {
            subView = SubView.KNOWN_STAGES;
            rebuild();
            return;
        }
        KnownStageDraft row = session().knownStages().get(editingKnownIndex);
        session().selectKnownIndex(editingKnownIndex);

        addFormLabel(FORM_LEFT, 8, FORM_WIDTH, "screen.another_stages.stage_generator.known.entry_title");
        String incomplete = row.incompleteReasonKey();
        if (incomplete != null) {
            addFormLabel(FORM_LEFT, 20, FORM_WIDTH, incomplete);
        }
        addFormLabel(FORM_LEFT, 36, FORM_WIDTH, "screen.another_stages.stage_generator.table.col.type");
        addFormCycle(FORM_LEFT, 48, FORM_WIDTH, 14, Component.literal(row.stageType), delta -> {
            row.stageType = stepStageType(row.stageType, delta);
            rebuild();
        });

        addFormLabel(FORM_LEFT, 70, FORM_WIDTH, "screen.another_stages.stage_generator.stages.id_hint");
        stageIdBox = new SuggestingEditBox(
                font,
                FORM_WIDTH,
                14,
                Component.translatable("screen.another_stages.stage_generator.table.col.id"),
                StageGenSuggestions::stageIds);
        stageIdBox.setPosition(leftPos + FORM_LEFT, topPos + 82);
        stageIdBox.setMaxLength(128);
        stageIdBox.setValue(row.stage == null ? "" : row.stage);
        stageIdBox.setResponder(v -> row.stage = v == null ? "" : v.trim());
        stageIdBox.refreshSuggestions();
        addRenderableWidget(stageIdBox);
        formBoxes.add(stageIdBox);

        addBottomButton(doneLabel(), this::navigateUp);
    }

    /** Browse row label: first matcher / recipe target / menu — never the silent JSON id. */
    private static String browseSummary(RestrictionDraft d) {
        String m = firstMatcherId(d);
        if (m != null && !m.isBlank()) {
            return m;
        }
        if (!d.recipes.isEmpty() && d.recipes.get(0) != null && !d.recipes.get(0).isBlank()) {
            return d.recipes.get(0);
        }
        if (!d.recipeTypes.isEmpty() && d.recipeTypes.get(0) != null && !d.recipeTypes.get(0).isBlank()) {
            return d.recipeTypes.get(0);
        }
        if (!d.inputs.isEmpty() && d.inputs.get(0) != null && !d.inputs.get(0).isBlank()) {
            return d.inputs.get(0);
        }
        if (!d.menus.isEmpty() && d.menus.get(0) != null && !d.menus.get(0).isBlank()) {
            return d.menus.get(0);
        }
        if (!d.menuTypes.isEmpty() && d.menuTypes.get(0) != null && !d.menuTypes.get(0).isBlank()) {
            return d.menuTypes.get(0);
        }
        for (RestrictionDraft.StageRow row : d.stages) {
            if (row != null && row.id != null && !row.id.isBlank()) {
                return row.type + ":" + row.id;
            }
        }
        return "…";
    }

    private void buildBrowse() {
        int listTop = addListSearchBar(6);
        StageGenScrollList list = createList(listTop, contentBottom());
        String titleKey = switch (session().filter()) {
            case ITEM -> "screen.another_stages.stage_generator.browse.items";
            case RECIPE -> "screen.another_stages.stage_generator.browse.recipes";
            case BLOCK -> "screen.another_stages.stage_generator.browse.blocks";
            case GUI -> "screen.another_stages.stage_generator.browse.guis";
        };
        list.addLabel(titleKey);

        List<RestrictionDraft> filtered = session().filtered();
        int shown = 0;
        if (filtered.isEmpty()) {
            list.addLabel("screen.another_stages.stage_generator.browse.empty");
        } else {
            for (RestrictionDraft d : filtered) {
                String label = browseSummary(d);
                if (!matchesSearch(label)) {
                    continue;
                }
                shown++;
                StageGenCompletion.Status st = StageGenCompletion.evaluate(d);
                ItemStack icon = firstMatcherIcon(d);
                String glyph = icon.isEmpty() ? StageGenIcons.glyphForMatcher(firstMatcherId(d)) : null;
                if (icon.isEmpty() && (firstMatcherId(d) == null || firstMatcherId(d).isBlank())) {
                    icon = StageGenIcons.stackForCategory(d.category);
                    glyph = null;
                }
                list.addBrowseRow(icon, glyph, Component.literal(label), st.reason(), st.ready(), () -> {
                    session().select(d);
                    subView = SubView.ENTRY_EDIT;
                    savedScroll = 0;
                    rebuild();
                });
            }
            if (shown == 0) {
                list.addLabel("screen.another_stages.stage_generator.search.empty");
            }
        }

        addBottomButton(Component.translatable("screen.another_stages.stage_generator.side.new"), this::actionNew);
        addBottomButton(Component.translatable("screen.another_stages.stage_generator.side.delete"), this::actionDelete,
                null, () -> session().selected() != null && session().selected().category == session().filter());
        addBottomButton(doneLabel(), this::navigateUp);
    }

    private void actionNew() {
        RestrictionDraft created = session().createNew(session().filter());
        session().select(created);
        subView = SubView.ENTRY_EDIT;
        savedScroll = 0;
        rebuild();
    }

    private void actionDelete() {
        RestrictionDraft d = session().selected();
        if (d == null || d.category != session().filter()) {
            return;
        }
        int deletedKubeIndex = d.kubeEntryIndex;
        PacketDistributor.sendToServer(
                new DeleteStageGeneratorPayload(d.category.name(), d.id == null ? "" : d.id, deletedKubeIndex));
        RestrictionDraft.Category cat = d.category;
        session().removeSelected();
        if (AsConfig.preferKubejs() && deletedKubeIndex >= 0) {
            for (RestrictionDraft other : session().all()) {
                if (other.category == cat && other.kubeEntryIndex > deletedKubeIndex) {
                    other.kubeEntryIndex--;
                }
            }
        }
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

        switch (draft.category) {
            case ITEM -> buildItemEntryFields(draft);
            case RECIPE -> buildRecipeEntryFields(draft);
            case BLOCK -> buildBlockEntryFields(draft);
            case GUI -> buildGuiEntryFields(draft);
        }

        if (!st.ready()) {
            StringWidget incomplete = new StringWidget(FORM_WIDTH, 10, st.reason().copy()
                    .withStyle(s -> s.withColor(0xCC4444)), font);
            incomplete.setPosition(leftPos + FORM_LEFT, topPos + StageGeneratorMenu.BOTTOM_BAR_Y - 12);
            addRenderableWidget(incomplete);
        }

        addBottomButton(doneLabel(), this::navigateUp);
    }

    private void buildItemEntryFields(RestrictionDraft draft) {
        int navY = 22;
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

        int navY2 = 40;
        addFormButton(formColX(0, cols), navY2, formColW(0, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.nav.cases"),
                () -> openSub(SubView.CASES));
        addFormButton(formColX(1, cols), navY2, formColW(1, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.tooltips", draft.tooltips.size()),
                () -> openSub(SubView.TOOLTIPS));
        addFormCycle(formColX(2, cols), navY2, formColW(2, cols), navH,
                Component.literal(draft.consequence),
                delta -> {
                    draft.consequence = stepConsequence(draft.consequence, delta);
                    rebuild();
                });

        int row = 60;
        addFormLabel(formColX(0, 2), row, formColW(0, 2), "screen.another_stages.stage_generator.field.recipe_viewer");
        addFormCycle(formColX(1, 2), row, formColW(1, 2), 12,
                Component.literal(draft.recipeViewer),
                delta -> {
                    draft.recipeViewer = stepViewer(draft.recipeViewer, delta);
                    rebuild();
                });

        row = 76;
        addFormToggle(formColX(0, 2), row, formColW(0, 2), "screen.another_stages.stage_generator.field.can_pickup_label",
                () -> draft.canPickup, v -> draft.canPickup = v);
        addFormToggle(formColX(1, 2), row, formColW(1, 2), "screen.another_stages.stage_generator.field.hide_item_name_label",
                () -> draft.hideItemName, v -> draft.hideItemName = v);

        row = 96;
        addFormLabel(formColX(0, 2), row, formColW(0, 2), "screen.another_stages.stage_generator.field.inventory_ticks");
        ticksBox = addFormBox(formColX(1, 2), row, formColW(1, 2), 12, String.valueOf(draft.inventoryTicks), 8, v -> {
            try {
                draft.inventoryTicks = Integer.parseInt(v.trim());
            } catch (Exception ignored) {
            }
        });
    }

    private void buildRecipeEntryFields(RestrictionDraft draft) {
        int navY = 22;
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

        int navY2 = 40;
        addFormButton(formColX(0, cols), navY2, formColW(0, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.recipe_types", draft.recipeTypes.size()),
                () -> openSub(SubView.RECIPE_TYPES));
        addFormButton(formColX(1, cols), navY2, formColW(1, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.inputs", draft.inputs.size()),
                () -> openSub(SubView.INPUTS));
        addFormButton(formColX(2, cols), navY2, formColW(2, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.exclude_recipes", draft.excludeRecipes.size()),
                () -> openSub(SubView.EXCLUDE_RECIPES));

        int row = 60;
        addFormButton(formColX(0, 2), row, formColW(0, 2), 14,
                Component.translatable("screen.another_stages.stage_generator.table.open.tooltips", draft.tooltips.size()),
                () -> openSub(SubView.TOOLTIPS));
        addFormCycle(formColX(1, 2), row, formColW(1, 2), 14,
                Component.literal("viewer: " + draft.recipeViewer),
                delta -> {
                    draft.recipeViewer = stepViewer(draft.recipeViewer, delta);
                    rebuild();
                });
    }

    private void buildBlockEntryFields(RestrictionDraft draft) {
        int navY = 22;
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

        addFormLabel(FORM_LEFT, 42, FORM_WIDTH, "screen.another_stages.stage_generator.field.mimic");
        mimicBox = addFormBox(FORM_LEFT, 52, FORM_WIDTH, 12, draft.mimic, 128, v -> draft.mimic = v == null ? "" : v.trim());

        addFormToggle(formColX(0, 2), 72, formColW(0, 2), "screen.another_stages.stage_generator.field.camouflage",
                () -> draft.camouflage, v -> draft.camouflage = v);
        addFormToggle(formColX(1, 2), 72, formColW(1, 2), "screen.another_stages.stage_generator.field.unbreakable",
                () -> draft.unbreakable, v -> draft.unbreakable = v);
        addFormToggle(formColX(0, 2), 92, formColW(0, 2), "screen.another_stages.stage_generator.field.stop_tick",
                () -> draft.stopTick, v -> draft.stopTick = v);
        addFormToggle(formColX(1, 2), 92, formColW(1, 2), "screen.another_stages.stage_generator.field.break_msg",
                () -> draft.breakMessageDefault, v -> draft.breakMessageDefault = v);
    }

    private void buildGuiEntryFields(RestrictionDraft draft) {
        int navY = 22;
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

        int navY2 = 40;
        addFormButton(formColX(0, cols), navY2, formColW(0, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.blocked_slots", draft.blockedSlots.size()),
                () -> openSub(SubView.BLOCKED_SLOTS));
        addFormButton(formColX(1, cols), navY2, formColW(1, cols), navH,
                Component.translatable("screen.another_stages.stage_generator.table.open.blocked_items", draft.blockedItems.size()),
                () -> openSub(SubView.BLOCKED_ITEMS));

        addFormToggle(formColX(0, 2), 62, formColW(0, 2), "screen.another_stages.stage_generator.field.block_open",
                () -> draft.blockOpen, v -> draft.blockOpen = v);
        addFormToggle(formColX(1, 2), 62, formColW(1, 2), "screen.another_stages.stage_generator.field.gui_msg_default",
                () -> draft.guiMessageDefault, v -> draft.guiMessageDefault = v);

        addFormLabel(formColX(0, 2), 82, formColW(0, 2), "screen.another_stages.stage_generator.field.max_slots");
        maxSlotsBox = addFormBox(formColX(1, 2), 82, formColW(1, 2), 12, String.valueOf(draft.maxSlots), 8, v -> {
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
        addFormLabel(FORM_LEFT, 8, FORM_WIDTH, "screen.another_stages.stage_generator.nav.cases");
        int row = 24;
        addFormToggle(FORM_LEFT, row, FORM_WIDTH, "screen.another_stages.stage_generator.field.cases_all",
                () -> draft.casesAll, v -> {
                    draft.casesAll = v;
                    rebuild();
                });
        row += 18;
        if (!draft.casesAll) {
            addFormToggle(FORM_LEFT, row, FORM_WIDTH, "screen.another_stages.stage_generator.field.case_right",
                    () -> draft.caseRight, v -> draft.caseRight = v);
            row += 18;
            addFormToggle(FORM_LEFT, row, FORM_WIDTH, "screen.another_stages.stage_generator.field.case_left",
                    () -> draft.caseLeft, v -> draft.caseLeft = v);
            row += 18;
            addFormToggle(FORM_LEFT, row, FORM_WIDTH, "screen.another_stages.stage_generator.field.case_main",
                    () -> draft.caseMain, v -> draft.caseMain = v);
            row += 18;
            addFormToggle(FORM_LEFT, row, FORM_WIDTH, "screen.another_stages.stage_generator.field.case_off",
                    () -> draft.caseOff, v -> draft.caseOff = v);
            row += 18;
            addFormToggle(FORM_LEFT, row, FORM_WIDTH, "screen.another_stages.stage_generator.field.case_inventory",
                    () -> draft.caseInventory, v -> draft.caseInventory = v);
            row += 18;
        }
        addFormToggle(FORM_LEFT, row, FORM_WIDTH, "screen.another_stages.stage_generator.field.pickup_msg_default",
                () -> draft.pickupMessageDefault, v -> draft.pickupMessageDefault = v);
        row += 18;
        addFormToggle(FORM_LEFT, row, FORM_WIDTH, "screen.another_stages.stage_generator.field.hide_default_tooltips",
                () -> draft.hideDefaultTooltips, v -> draft.hideDefaultTooltips = v);

        addBottomButton(doneLabel(), this::navigateUp);
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
        int listTop = addListSearchBar(6);
        StageGenScrollList list = createList(listTop, contentBottom());
        String titleKey = switch (subView) {
            case MATCHERS -> "screen.another_stages.stage_generator.table.title.matchers";
            case EXCLUDE -> "screen.another_stages.stage_generator.table.title.exclude";
            case BLOCKED_ITEMS -> "screen.another_stages.stage_generator.table.title.blocked_items";
            case INPUTS -> "screen.another_stages.stage_generator.table.title.inputs";
            default -> "screen.another_stages.stage_generator.table.title.matchers";
        };
        list.addLabel(titleKey);

        if (target.isEmpty()) {
            list.addLabel("screen.another_stages.stage_generator.matcher.empty");
        } else {
            int shown = 0;
            for (int i = 0; i < target.size(); i++) {
                int idx = i;
                String id = target.get(i);
                if (!matchesSearch(id)) {
                    continue;
                }
                shown++;
                ItemStack icon = StageGenIcons.stackFromMatcher(id);
                String glyph = icon.isEmpty() ? StageGenIcons.glyphForMatcher(id) : null;
                list.addBrowseRow(icon, glyph, Component.literal(id == null || id.isBlank() ? "…" : id),
                        Component.empty(), true, () -> openMatcherEntry(idx));
            }
            if (shown == 0) {
                list.addLabel("screen.another_stages.stage_generator.search.empty");
            }
        }

        addBottomButton(Component.translatable("screen.another_stages.stage_generator.side.new"), () -> {
            target.add("");
            openMatcherEntry(target.size() - 1);
        });
        addBottomButton(Component.translatable("screen.another_stages.stage_generator.side.delete"), () -> {
            if (editingMatcherIndex >= 0 && editingMatcherIndex < target.size()) {
                target.remove(editingMatcherIndex);
            } else if (!target.isEmpty()) {
                target.remove(target.size() - 1);
            }
            editingMatcherIndex = -1;
            savedScroll = 0;
            rebuild();
        }, null, () -> !target.isEmpty());
        addBottomButton(doneLabel(), this::navigateUp);
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
        subView = SubView.MATCHER_ENTRY;
        savedScroll = 0;
        rebuild();
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
        String current = target.get(editingMatcherIndex) == null ? "" : target.get(editingMatcherIndex);

        addFormLabel(FORM_LEFT, 8, FORM_WIDTH, "screen.another_stages.stage_generator.matcher.entry_title");
        addFormLabel(FORM_LEFT, 18, FORM_WIDTH, "screen.another_stages.stage_generator.matcher.calibrate_hint");

        // Shop-style: ← [ghost] → [value] — drop an item to calibrate id / #tag / @mod.
        int rowY = StageGeneratorMenu.MATCHER_ROW_Y;
        int arrowY = rowY + (StageGeneratorMenu.MATCHER_SLOT - StageGeneratorMenu.MATCHER_ARROW) / 2;
        int boxY = rowY + (StageGeneratorMenu.MATCHER_SLOT - 14) / 2;
        syncGhostFromValueQuiet(current);
        setupMatcherVariants(current);

        addFormButton(FORM_LEFT, arrowY, StageGeneratorMenu.MATCHER_ARROW, StageGeneratorMenu.MATCHER_ARROW,
                Component.literal("←"), () -> cycleMatcherVariant(target, -1));
        addFormButton(matcherRightArrowX(), arrowY, StageGeneratorMenu.MATCHER_ARROW, StageGeneratorMenu.MATCHER_ARROW,
                Component.literal("→"), () -> cycleMatcherVariant(target, 1));

        int boxX = matcherEditBoxX();
        int boxW = Math.max(40, FORM_RIGHT - boxX);
        matcherValueBox = addFormBox(boxX, boxY, boxW, 14, current, 256, v -> {
            if (editingMatcherIndex >= 0 && editingMatcherIndex < target.size()) {
                target.set(editingMatcherIndex, v == null ? "" : v.trim());
                matcherVariantIndex = StageGenMatcherVariants.indexOfOrZero(matcherVariants, v);
                autosaveIfReady();
            }
        });

        addInvSideDone(this::navigateUp);
    }

    private void setupMatcherVariants(@Nullable String current) {
        matcherVariants.clear();
        ItemStack stack = menu.getGhostStack();
        if (!stack.isEmpty()) {
            matcherVariants.addAll(StageGenMatcherVariants.variantsFromStack(stack));
        }
        if (current != null && !current.isBlank() && !matcherVariants.contains(current.trim())) {
            matcherVariants.add(0, current.trim());
        }
        if (matcherVariants.isEmpty() && current != null && !current.isBlank()) {
            matcherVariants.add(current.trim());
        }
        matcherVariantIndex = StageGenMatcherVariants.indexOfOrZero(matcherVariants, current);
    }

    private void cycleMatcherVariant(List<String> target, int delta) {
        if (matcherVariants.isEmpty()) {
            return;
        }
        matcherVariantIndex = Math.floorMod(matcherVariantIndex + delta, matcherVariants.size());
        String value = matcherVariants.get(matcherVariantIndex);
        if (matcherValueBox != null) {
            matcherValueBox.setValue(value);
        }
        if (editingMatcherIndex >= 0 && editingMatcherIndex < target.size()) {
            target.set(editingMatcherIndex, value);
        }
        autosaveIfReady();
    }

    private void commitMatcherEntry(List<String> target) {
        if (matcherValueBox == null || editingMatcherIndex < 0 || editingMatcherIndex >= target.size()) {
            return;
        }
        String raw = matcherValueBox.getValue() == null ? "" : matcherValueBox.getValue().trim();
        target.set(editingMatcherIndex, raw);
    }

    private void syncGhostFromValueQuiet(@Nullable String value) {
        if (value == null || value.isBlank() || value.startsWith("#") || value.startsWith("@")) {
            menu.clearGhostQuiet();
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(value.contains(":") ? value : "minecraft:" + value);
        if (id == null) {
            menu.clearGhostQuiet();
            return;
        }
        var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            menu.clearGhostQuiet();
            return;
        }
        menu.setGhostStack(new ItemStack(item), false);
    }

    private void buildStageList() {
        RestrictionDraft draft = draft();
        if (draft == null) {
            subView = SubView.ENTRY_EDIT;
            rebuild();
            return;
        }
        int listTop = addListSearchBar(6);
        StageGenScrollList list = createList(listTop, contentBottom());
        list.addLabel("screen.another_stages.stage_generator.table.title.stages");
        if (draft.stages.isEmpty()) {
            list.addLabel("screen.another_stages.stage_generator.stages.empty");
        } else {
            int shown = 0;
            for (int i = 0; i < draft.stages.size(); i++) {
                int idx = i;
                RestrictionDraft.StageRow row = draft.stages.get(i);
                String label = row.type + " | " + (row.id == null || row.id.isBlank() ? "…" : row.id)
                        + " | is=" + row.is;
                if (!matchesSearch(label)) {
                    continue;
                }
                shown++;
                list.addBrowseRow(ItemStack.EMPTY, "S", Component.literal(label),
                        Component.empty(), !row.id.isBlank(), () -> openStageEntry(idx));
            }
            if (shown == 0) {
                list.addLabel("screen.another_stages.stage_generator.search.empty");
            }
        }
        addBottomButton(Component.translatable("screen.another_stages.stage_generator.side.new"), () -> {
            draft.stages.add(new RestrictionDraft.StageRow());
            openStageEntry(draft.stages.size() - 1);
        });
        addBottomButton(Component.translatable("screen.another_stages.stage_generator.side.delete"), () -> {
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
        addBottomButton(doneLabel(), this::navigateUp);
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
        addFormCycle(FORM_LEFT, 34, FORM_WIDTH, 14, Component.literal(row.type), delta -> {
            row.type = stepStageType(row.type, delta);
            rebuild();
        });

        addFormToggle(FORM_LEFT, 56, FORM_WIDTH, "screen.another_stages.stage_generator.table.col.is",
                () -> row.is, v -> row.is = v);

        addFormLabel(FORM_LEFT, 78, FORM_WIDTH, "screen.another_stages.stage_generator.stages.id_hint");
        stageIdBox = new SuggestingEditBox(
                font,
                FORM_WIDTH,
                14,
                Component.translatable("screen.another_stages.stage_generator.table.col.id"),
                StageGenSuggestions::stageIds);
        stageIdBox.setPosition(leftPos + FORM_LEFT, topPos + 90);
        stageIdBox.setMaxLength(128);
        stageIdBox.setValue(row.id == null ? "" : row.id);
        stageIdBox.setResponder(v -> row.id = v == null ? "" : v.trim());
        stageIdBox.refreshSuggestions();
        addRenderableWidget(stageIdBox);
        formBoxes.add(stageIdBox);

        addBottomButton(doneLabel(), this::navigateUp);
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
        if (view == SubView.TOOLTIPS || view == SubView.BLOCKED_SLOTS || view == SubView.RECIPES
                || view == SubView.RECIPE_TYPES || view == SubView.EXCLUDE_RECIPES
                || view == SubView.MENUS || view == SubView.MENU_TYPES) {
            stringListParent = view;
        }
        subView = view;
        savedScroll = 0;
        rebuild();
    }

    private void flushEntryBoxes(RestrictionDraft draft) {
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

    private void buildStringList() {
        RestrictionDraft draft = draft();
        if (draft == null) {
            subView = SubView.BROWSE;
            rebuild();
            return;
        }
        stringListParent = subView;
        int listTop = addListSearchBar(6);
        StageGenScrollList list = createList(listTop, contentBottom());
        list.addLabel(stringListTitleKey(subView));

        int count = stringListSize(draft, subView);
        if (count == 0) {
            list.addLabel("screen.another_stages.stage_generator.string.empty");
        } else {
            int shown = 0;
            for (int i = 0; i < count; i++) {
                int idx = i;
                String value = stringListValue(draft, subView, i);
                if (!matchesSearch(value)) {
                    continue;
                }
                shown++;
                list.addBrowseRow(
                        ItemStack.EMPTY,
                        null,
                        Component.literal(value == null || value.isBlank() ? "…" : value),
                        Component.empty(),
                        true,
                        () -> openStringEntry(idx));
            }
            if (shown == 0) {
                list.addLabel("screen.another_stages.stage_generator.search.empty");
            }
        }

        addBottomButton(Component.translatable("screen.another_stages.stage_generator.side.new"), () -> {
            addStringListRow(draft, stringListParent);
            openStringEntry(stringListSize(draft, stringListParent) - 1);
        });
        addBottomButton(Component.translatable("screen.another_stages.stage_generator.side.delete"), () -> {
            int size = stringListSize(draft, stringListParent);
            if (size <= 0) {
                return;
            }
            int idx = editingStringIndex >= 0 && editingStringIndex < size
                    ? editingStringIndex
                    : size - 1;
            removeStringListRow(draft, stringListParent, idx);
            editingStringIndex = -1;
            savedScroll = 0;
            rebuild();
        }, null, () -> stringListSize(draft, stringListParent) > 0);
        addBottomButton(doneLabel(), this::navigateUp);
    }

    private void openStringEntry(int index) {
        RestrictionDraft draft = draft();
        if (draft == null) {
            return;
        }
        if (index < 0 || index >= stringListSize(draft, stringListParent)) {
            return;
        }
        editingStringIndex = index;
        subView = SubView.STRING_ENTRY;
        savedScroll = 0;
        rebuild();
    }

    private void buildStringEntry() {
        RestrictionDraft draft = draft();
        if (draft == null || editingStringIndex < 0
                || editingStringIndex >= stringListSize(draft, stringListParent)) {
            subView = stringListParent;
            rebuild();
            return;
        }
        addFormLabel(FORM_LEFT, 8, FORM_WIDTH, "screen.another_stages.stage_generator.string.entry_title");
        addFormLabel(FORM_LEFT, 22, FORM_WIDTH, stringListTitleKey(stringListParent));
        String current = stringListValue(draft, stringListParent, editingStringIndex);
        stringValueBox = addFormBox(FORM_LEFT, 36, FORM_WIDTH, 14, current == null ? "" : current, 256, v -> {});

        addBottomButton(doneLabel(), this::navigateUp);
    }

    private void commitStringEntry(RestrictionDraft draft) {
        if (stringValueBox == null || editingStringIndex < 0) {
            return;
        }
        setStringListValue(
                draft,
                stringListParent,
                editingStringIndex,
                stringValueBox.getValue() == null ? "" : stringValueBox.getValue().trim());
    }

    private static String stringListTitleKey(SubView view) {
        return switch (view) {
            case TOOLTIPS -> "screen.another_stages.stage_generator.table.title.tooltips";
            case BLOCKED_SLOTS -> "screen.another_stages.stage_generator.table.title.blocked_slots";
            case RECIPES -> "screen.another_stages.stage_generator.table.title.recipes";
            case RECIPE_TYPES -> "screen.another_stages.stage_generator.table.title.recipe_types";
            case EXCLUDE_RECIPES -> "screen.another_stages.stage_generator.table.title.exclude_recipes";
            case MENUS -> "screen.another_stages.stage_generator.table.title.menus";
            case MENU_TYPES -> "screen.another_stages.stage_generator.table.title.menu_types";
            default -> "screen.another_stages.stage_generator.string.entry_title";
        };
    }

    private static int stringListSize(RestrictionDraft draft, SubView view) {
        return switch (view) {
            case TOOLTIPS -> draft.tooltips.size();
            case BLOCKED_SLOTS -> draft.blockedSlots.size();
            case RECIPES -> draft.recipes.size();
            case RECIPE_TYPES -> draft.recipeTypes.size();
            case EXCLUDE_RECIPES -> draft.excludeRecipes.size();
            case MENUS -> draft.menus.size();
            case MENU_TYPES -> draft.menuTypes.size();
            default -> 0;
        };
    }

    private static String stringListValue(RestrictionDraft draft, SubView view, int index) {
        return switch (view) {
            case TOOLTIPS -> draft.tooltips.get(index);
            case BLOCKED_SLOTS -> String.valueOf(draft.blockedSlots.get(index));
            case RECIPES -> draft.recipes.get(index);
            case RECIPE_TYPES -> draft.recipeTypes.get(index);
            case EXCLUDE_RECIPES -> draft.excludeRecipes.get(index);
            case MENUS -> draft.menus.get(index);
            case MENU_TYPES -> draft.menuTypes.get(index);
            default -> "";
        };
    }

    private static void setStringListValue(RestrictionDraft draft, SubView view, int index, String value) {
        switch (view) {
            case TOOLTIPS -> draft.tooltips.set(index, value);
            case BLOCKED_SLOTS -> {
                try {
                    draft.blockedSlots.set(index, Integer.parseInt(value));
                } catch (Exception e) {
                    draft.blockedSlots.set(index, 0);
                }
            }
            case RECIPES -> draft.recipes.set(index, value);
            case RECIPE_TYPES -> draft.recipeTypes.set(index, value);
            case EXCLUDE_RECIPES -> draft.excludeRecipes.set(index, value);
            case MENUS -> draft.menus.set(index, value);
            case MENU_TYPES -> draft.menuTypes.set(index, value);
            default -> {
            }
        }
    }

    private static void addStringListRow(RestrictionDraft draft, SubView view) {
        switch (view) {
            case TOOLTIPS -> draft.tooltips.add("");
            case BLOCKED_SLOTS -> draft.blockedSlots.add(0);
            case RECIPES -> draft.recipes.add("");
            case RECIPE_TYPES -> draft.recipeTypes.add("");
            case EXCLUDE_RECIPES -> draft.excludeRecipes.add("");
            case MENUS -> draft.menus.add("");
            case MENU_TYPES -> draft.menuTypes.add("");
            default -> {
            }
        }
    }

    private static void removeStringListRow(RestrictionDraft draft, SubView view, int index) {
        switch (view) {
            case TOOLTIPS -> draft.tooltips.remove(index);
            case BLOCKED_SLOTS -> draft.blockedSlots.remove(index);
            case RECIPES -> draft.recipes.remove(index);
            case RECIPE_TYPES -> draft.recipeTypes.remove(index);
            case EXCLUDE_RECIPES -> draft.excludeRecipes.remove(index);
            case MENUS -> draft.menus.remove(index);
            case MENU_TYPES -> draft.menuTypes.remove(index);
            default -> {
            }
        }
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

    private void onGhostChanged() {
        if (!acceptsGhostInCurrentView() || subView != SubView.MATCHER_ENTRY) {
            return;
        }
        RestrictionDraft draft = draft();
        if (draft == null || editingMatcherIndex < 0) {
            return;
        }
        List<String> target = matcherListFor(draft, matcherListParent);
        if (editingMatcherIndex >= target.size()) {
            return;
        }
        ItemStack stack = menu.getGhostStack();
        if (stack.isEmpty()) {
            setupMatcherVariants("");
            if (matcherValueBox != null) {
                matcherValueBox.setValue("");
            }
            target.set(editingMatcherIndex, "");
            autosaveIfReady();
            return;
        }
        setupMatcherVariants(null);
        if (matcherVariants.isEmpty()) {
            return;
        }
        matcherVariantIndex = 0;
        String value = matcherVariants.get(0);
        if (matcherValueBox != null) {
            matcherValueBox.setValue(value);
        }
        target.set(editingMatcherIndex, value);
        autosaveIfReady();
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

    private static final String[] CONSEQUENCES = {
            "drop", "delete", "block", "block_drop", "block_delete"
    };
    private static final String[] VIEWERS = {"overlay", "hide", "hardcore"};
    private static final String[] STAGE_TYPES = {"player", "world", "team"};

    private static String stepConsequence(String cur, int delta) {
        return stepEnum(CONSEQUENCES, cur, delta);
    }

    private static String stepViewer(String cur, int delta) {
        return stepEnum(VIEWERS, cur, delta);
    }

    private static String stepStageType(String cur, int delta) {
        return stepEnum(STAGE_TYPES, cur, delta);
    }

    private static String stepEnum(String[] values, String cur, int delta) {
        String c = cur == null ? values[0] : cur.toLowerCase(Locale.ROOT);
        int idx = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(c)) {
                idx = i;
                break;
            }
        }
        return values[Math.floorMod(idx + delta, values.length)];
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Always use the container render path so JEI/EMI/REI keep a normal inventory screen.
        // Slots stay active but parked off-screen (Slot.x/y are final); draw them here when UI is on.
        this.renderTransparentBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (menu.slotsVisible()) {
            renderVisualInventory(graphics, mouseX, mouseY);
        }
        if (stageIdBox != null) {
            stageIdBox.renderSuggestions(graphics, font);
        }
    }

    private void renderVisualInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        hoveredSlot = findVisualSlot(mouseX, mouseY);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            int x = leftPos + StageGeneratorMenu.visualSlotX(i);
            int y = topPos + StageGeneratorMenu.visualSlotY(i);
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(font, stack, x, y);
        }
        if (hoveredSlot != null && hoveredSlot.isHighlightable()) {
            int hx = leftPos + StageGeneratorMenu.visualSlotX(hoveredSlot.index) - 1;
            int hy = topPos + StageGeneratorMenu.visualSlotY(hoveredSlot.index) - 1;
            graphics.fill(hx, hy, hx + 18, hy + 18, 0x80FFFFFF);
        }
    }

    private @Nullable Slot findVisualSlot(double mouseX, double mouseY) {
        if (!menu.slotsVisible()) {
            return null;
        }
        for (int i = 0; i < menu.slots.size(); i++) {
            int sx = leftPos + StageGeneratorMenu.visualSlotX(i);
            int sy = topPos + StageGeneratorMenu.visualSlotY(i);
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                return menu.slots.get(i);
            }
        }
        return null;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean scrollbarBusy = scrollList != null && scrollList.isDraggingScrollbar();
        for (EditBox box : formBoxes) {
            if (box == null || !box.isFocused()) {
                continue;
            }
            if (box.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                return true;
            }
            // ESC while typing: unfocus only — do not close / navigateUp.
            if (keyCode == 256) {
                box.setFocused(false);
                setFocused(null);
                return true;
            }
        }
        if (scrollbarBusy && minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        if (scrollbarBusy && keyCode == 256) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Bottom / inv-side action buttons win over the scroll list.
        if (isOverActionButtons(mouseX, mouseY)) {
            List<? extends net.minecraft.client.gui.components.events.GuiEventListener> kids =
                    new ArrayList<>(children());
            for (int i = kids.size() - 1; i >= 0; i--) {
                var child = kids.get(i);
                if (child instanceof AsButton && child.isMouseOver(mouseX, mouseY)
                        && child.mouseClicked(mouseX, mouseY, button)) {
                    setFocused(child);
                    return true;
                }
            }
        }
        //Slots are parked off-screen; handle the drawn inventory UI ourselves.
        if (menu.slotsVisible()) {
            Slot visual = findVisualSlot(mouseX, mouseY);
            if (visual != null) {
                ClickType click = SuggestingEditBox.hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP;
                slotClicked(visual, visual.index, button, click);
                return true;
            }
        }
        if (scrollList != null && scrollList.beginScrollbarDrag(mouseX, mouseY, button)) {
            setFocused(scrollList);
            setDragging(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOverActionButtons(double mouseX, double mouseY) {
        if (!bottomBarButtons.isEmpty()) {
            int y0 = topPos + StageGeneratorMenu.BOTTOM_BAR_Y - 2;
            int y1 = topPos + StageGeneratorMenu.BOTTOM_BAR_Y + StageGeneratorMenu.BOTTOM_BTN_H + 2;
            int x0 = leftPos + StageGeneratorMenu.LIST_X;
            int x1 = leftPos + StageGeneratorMenu.LIST_RIGHT;
            if (mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1) {
                return true;
            }
        }
        if (showInventorySlots()) {
            int x0 = leftPos + StageGeneratorMenu.SIDE_BTN_X;
            int x1 = x0 + StageGeneratorMenu.SIDE_BTN_W;
            int y0 = topPos + StageGeneratorMenu.SIDE_BTN_Y;
            int y1 = y0 + StageGeneratorMenu.SIDE_BTN_H;
            return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (scrollList != null && (scrollList.isDraggingScrollbar() || scrollList.isOverScrollbar(mouseX, mouseY))) {
            if (scrollList.mouseDragged(mouseX, mouseY, button, dx, dy)) {
                return true;
            }
        }
        if (scrollList != null && scrollList.isDraggingScrollbar()) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollList != null && scrollList.isDraggingScrollbar()) {
            scrollList.mouseReleased(mouseX, mouseY, button);
            setDragging(false);
            return true;
        }
        if (scrollList != null) {
            scrollList.endScrollbarDrag();
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
