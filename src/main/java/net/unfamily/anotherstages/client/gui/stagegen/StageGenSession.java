package net.unfamily.anotherstages.client.gui.stagegen;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side Stage Generator workspace: many restrictions (ShopEdit-style browse/edit).
 */
public final class StageGenSession {
    private static final StageGenSession INSTANCE = new StageGenSession();

    private final List<RestrictionDraft> drafts = new ArrayList<>();
    private final List<KnownStageDraft> knownStages = new ArrayList<>();
    private int selectedIndex = -1;
    private int knownSelectedIndex = -1;
    private RestrictionDraft.Category filter = RestrictionDraft.Category.ITEM;

    private StageGenSession() {}

    public static StageGenSession get() {
        return INSTANCE;
    }

    public void replaceAll(List<RestrictionDraft> loaded) {
        drafts.clear();
        if (loaded != null) {
            drafts.addAll(loaded);
        }
        selectedIndex = drafts.isEmpty() ? -1 : 0;
        if (selectedIndex >= 0) {
            filter = drafts.get(selectedIndex).category;
        }
    }

    public void clear() {
        drafts.clear();
        knownStages.clear();
        selectedIndex = -1;
        knownSelectedIndex = -1;
    }

    public void replaceKnownStages(List<KnownStageDraft> loaded) {
        knownStages.clear();
        if (loaded != null) {
            knownStages.addAll(loaded);
        }
        knownSelectedIndex = knownStages.isEmpty() ? -1 : 0;
    }

    public List<KnownStageDraft> knownStages() {
        return knownStages;
    }

    public int knownSelectedIndex() {
        return knownSelectedIndex;
    }

    @Nullable
    public KnownStageDraft knownSelected() {
        if (knownSelectedIndex < 0 || knownSelectedIndex >= knownStages.size()) {
            return null;
        }
        return knownStages.get(knownSelectedIndex);
    }

    public void selectKnownIndex(int index) {
        if (index < 0 || index >= knownStages.size()) {
            knownSelectedIndex = -1;
            return;
        }
        knownSelectedIndex = index;
    }

    public KnownStageDraft createKnown() {
        KnownStageDraft d = new KnownStageDraft();
        knownStages.add(d);
        knownSelectedIndex = knownStages.size() - 1;
        return d;
    }

    public boolean removeKnownSelected() {
        if (knownSelectedIndex < 0 || knownSelectedIndex >= knownStages.size()) {
            return false;
        }
        knownStages.remove(knownSelectedIndex);
        if (knownStages.isEmpty()) {
            knownSelectedIndex = -1;
        } else {
            knownSelectedIndex = Math.min(knownSelectedIndex, knownStages.size() - 1);
        }
        return true;
    }

    public List<RestrictionDraft> all() {
        return drafts;
    }

    public RestrictionDraft.Category filter() {
        return filter;
    }

    public void setFilter(RestrictionDraft.Category filter) {
        this.filter = filter == null ? RestrictionDraft.Category.ITEM : filter;
    }

    public List<RestrictionDraft> filtered() {
        List<RestrictionDraft> out = new ArrayList<>();
        for (RestrictionDraft d : drafts) {
            if (d.category == filter) {
                out.add(d);
            }
        }
        return out;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    @Nullable
    public RestrictionDraft selected() {
        if (selectedIndex < 0 || selectedIndex >= drafts.size()) {
            return null;
        }
        return drafts.get(selectedIndex);
    }

    public void select(RestrictionDraft draft) {
        if (draft == null) {
            selectedIndex = -1;
            return;
        }
        int idx = drafts.indexOf(draft);
        selectedIndex = idx;
        if (idx >= 0) {
            filter = draft.category;
        }
    }

    public void selectIndex(int index) {
        if (index < 0 || index >= drafts.size()) {
            selectedIndex = -1;
            return;
        }
        selectedIndex = index;
        filter = drafts.get(index).category;
    }

    public RestrictionDraft createNew(RestrictionDraft.Category category) {
        RestrictionDraft d = RestrictionDraft.createDefaults(category);
        drafts.add(d);
        selectedIndex = drafts.size() - 1;
        filter = category;
        return d;
    }

    public boolean removeSelected() {
        if (selectedIndex < 0 || selectedIndex >= drafts.size()) {
            return false;
        }
        drafts.remove(selectedIndex);
        if (drafts.isEmpty()) {
            selectedIndex = -1;
        } else {
            selectedIndex = Math.min(selectedIndex, drafts.size() - 1);
            filter = drafts.get(selectedIndex).category;
        }
        return true;
    }
}
