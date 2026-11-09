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
    private int selectedIndex = -1;
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
        selectedIndex = -1;
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
