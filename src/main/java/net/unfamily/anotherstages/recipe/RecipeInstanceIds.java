package net.unfamily.anotherstages.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Shared identity cache: recipe instance → registry id.
 * <p>
 * Prevents hot paths (machine tick / Mek cache) from scanning recipe lists on every call.
 * Segments are keyed by a stable source (e.g. Mek type provider). Rebuild when
 * {@code holders.size()} changes; {@link #clear()} on datapack/index reload.
 */
public final class RecipeInstanceIds {
    private static final Map<Object, Segment> SEGMENTS = new IdentityHashMap<>();

    private RecipeInstanceIds() {}

    public static void clear() {
        SEGMENTS.clear();
    }

    /**
     * @param recipe     recipe value instance (not the holder)
     * @param sourceKey  identity key for this list (manager / type provider / list owner)
     * @param holders    current holders for that source
     * @param toId       extract registry id from a holder
     */
    @Nullable
    public static Identifier idOf(
            @Nullable Object recipe,
            @Nullable Object sourceKey,
            @Nullable Iterable<? extends RecipeHolder<?>> holders,
            Function<RecipeHolder<?>, Identifier> toId) {
        if (recipe == null || sourceKey == null || holders == null || toId == null) {
            return null;
        }
        Collection<? extends RecipeHolder<?>> list =
                holders instanceof Collection<?> c
                        ? (Collection<? extends RecipeHolder<?>>) c
                        : copyOf(holders);
        int size = list.size();
        Segment seg = SEGMENTS.get(sourceKey);
        if (seg == null || seg.size != size) {
            seg = new Segment(size);
            for (RecipeHolder<?> holder : list) {
                Identifier id = toId.apply(holder);
                if (id != null) {
                    seg.byInstance.put(holder.value(), id);
                }
            }
            SEGMENTS.put(sourceKey, seg);
        }
        Identifier id = seg.byInstance.get(recipe);
        if (id != null) {
            return id;
        }
        if (seg.knownMiss.containsKey(recipe)) {
            return null;
        }
        for (RecipeHolder<?> holder : list) {
            if (holder.value().equals(recipe)) {
                Identifier found = toId.apply(holder);
                if (found != null) {
                    seg.byInstance.put(recipe, found);
                }
                return found;
            }
        }
        seg.knownMiss.put(recipe, Boolean.TRUE);
        return null;
    }

    private static List<RecipeHolder<?>> copyOf(Iterable<? extends RecipeHolder<?>> holders) {
        List<RecipeHolder<?>> out = new ArrayList<>();
        for (RecipeHolder<?> holder : holders) {
            out.add(holder);
        }
        return out;
    }

    private static final class Segment {
        final int size;
        final IdentityHashMap<Object, Identifier> byInstance = new IdentityHashMap<>();
        /** Negative cache: avoid equals full-scan every tick for unknown instances. */
        final IdentityHashMap<Object, Boolean> knownMiss = new IdentityHashMap<>();

        Segment(int size) {
            this.size = size;
        }
    }
}
