package net.unfamily.anotherstages.compat.enderio;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Maps EIO {@code MachineTaskHost} instances to their owning block entity (ctor capture).
 */
public final class EioTaskHostSubjects {
    private static final Map<Object, BlockEntity> HOST_TO_BE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private EioTaskHostSubjects() {}

    public static void bind(@Nullable Object host, @Nullable BlockEntity be) {
        if (host == null) {
            return;
        }
        if (be == null) {
            HOST_TO_BE.remove(host);
            return;
        }
        HOST_TO_BE.put(host, be);
    }

    @Nullable
    public static BlockEntity get(@Nullable Object host) {
        return host == null ? null : HOST_TO_BE.get(host);
    }
}
