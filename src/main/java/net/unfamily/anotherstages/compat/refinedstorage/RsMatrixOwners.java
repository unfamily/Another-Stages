package net.unfamily.anotherstages.compat.refinedstorage;

import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Binds RS {@link RecipeMatrix} instances to their block-entity placer UUID supplier
 * so matrix result gating uses placedBy instead of always-null owner.
 */
public final class RsMatrixOwners {
    private static final Map<RecipeMatrix<?, ?>, Supplier<UUID>> OWNERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private RsMatrixOwners() {}

    public static void bind(RecipeMatrix<?, ?> matrix, Supplier<UUID> ownerSupplier) {
        if (matrix != null && ownerSupplier != null) {
            OWNERS.put(matrix, ownerSupplier);
        }
    }

    @Nullable
    public static UUID ownerOf(RecipeMatrix<?, ?> matrix) {
        if (matrix == null) {
            return null;
        }
        Supplier<UUID> supplier = OWNERS.get(matrix);
        return supplier == null ? null : supplier.get();
    }
}
