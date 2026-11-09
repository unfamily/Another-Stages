package net.unfamily.anotherstages.client.gui.stagegen;

import net.unfamily.anotherstages.client.ClientStageCache;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeSet;

/** Prefix suggestions for Stage Generator EditBoxes (Tab complete). */
public final class StageGenSuggestions {
    private StageGenSuggestions() {}

    public static java.util.List<String> stageIds() {
        TreeSet<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        set.addAll(ClientStageCache.allKnownStages());
        for (RestrictionDraft draft : StageGenSession.get().all()) {
            for (RestrictionDraft.StageRow row : draft.stages) {
                if (row != null && row.id != null && !row.id.isBlank()) {
                    set.add(row.id);
                }
            }
        }
        return new ArrayList<>(set);
    }

    public static java.util.List<String> matching(java.util.List<String> pool, @Nullable String prefix) {
        String p = prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT);
        java.util.List<String> out = new ArrayList<>();
        for (String s : pool) {
            if (s == null || s.isBlank()) {
                continue;
            }
            if (p.isEmpty() || s.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(s);
            }
        }
        return out;
    }

    /**
     * Next completion after {@code current} among matches for {@code typedPrefix}.
     * {@code reverse} cycles backward (Shift+Tab).
     */
    public static @Nullable String nextCompletion(
            java.util.List<String> pool,
            @Nullable String typedPrefix,
            @Nullable String current,
            boolean reverse
    ) {
        java.util.List<String> matches = matching(pool, typedPrefix);
        if (matches.isEmpty()) {
            return null;
        }
        if (current == null || current.isBlank() || !matches.contains(current)) {
            return reverse ? matches.get(matches.size() - 1) : matches.get(0);
        }
        int idx = matches.indexOf(current);
        if (idx < 0) {
            return matches.get(0);
        }
        if (reverse) {
            return matches.get((idx - 1 + matches.size()) % matches.size());
        }
        return matches.get((idx + 1) % matches.size());
    }
}
