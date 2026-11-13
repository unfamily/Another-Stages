package net.unfamily.anotherstages.restriction;

import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Shared conflict resolution for overlapping stage restrictions.
 * Higher {@code priority} wins; equal priority keeps encounter / load order (stable sort).
 */
public final class RestrictionPriority {
    private RestrictionPriority() {}

    /** Stable sort: higher priority first; ties keep relative order. */
    public static <T> void sortByPriorityDesc(List<T> rules, ToIntFunction<T> priorityOf) {
        if (rules == null || rules.size() < 2) {
            return;
        }
        rules.sort(Comparator.comparingInt(priorityOf).reversed());
    }

    /**
     * First rule among stage-active candidates with the highest priority.
     * {@code null} if {@code rules} is empty.
     */
    @Nullable
    public static <T> T pickHighest(@Nullable List<T> rules, ToIntFunction<T> priorityOf) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        T best = rules.getFirst();
        int bestP = priorityOf.applyAsInt(best);
        for (int i = 1; i < rules.size(); i++) {
            T candidate = rules.get(i);
            int p = priorityOf.applyAsInt(candidate);
            if (p > bestP) {
                best = candidate;
                bestP = p;
            }
        }
        return best;
    }
}
