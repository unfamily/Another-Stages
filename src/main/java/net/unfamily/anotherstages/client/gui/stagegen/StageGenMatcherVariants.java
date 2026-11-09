package net.unfamily.anotherstages.client.gui.stagegen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shop / Deep Drawer–style calibrator variants for stage matchers:
 * item id → {@code #tag}s → {@code @mod} (non-{@code minecraft}).
 */
public final class StageGenMatcherVariants {
    private StageGenMatcherVariants() {}

    /**
     * Variant cycle from a calibrator stack: plain item id, then every item tag that contains it,
     * then {@code @namespace} when the item is not from {@code minecraft}.
     */
    public static List<String> variantsFromStack(@NotNull ItemStack stack) {
        List<String> variants = new ArrayList<>();
        if (stack.isEmpty()) {
            return variants;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return variants;
        }
        variants.add(itemId.toString());

        var itemHolder = BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem());
        BuiltInRegistries.ITEM.getTags()
                .filter(named -> named.contains(itemHolder))
                .map(named -> "#" + named.key().location())
                .sorted()
                .forEach(tag -> {
                    if (!variants.contains(tag)) {
                        variants.add(tag);
                    }
                });

        String namespace = itemId.getNamespace();
        if (!"minecraft".equals(namespace)) {
            String mod = "@" + namespace;
            if (!variants.contains(mod)) {
                variants.add(mod);
            }
        }
        return variants;
    }

    /** Index of {@code value} in {@code variants}, or {@code 0} if missing / empty. */
    public static int indexOfOrZero(@NotNull List<String> variants, @Nullable String value) {
        if (value == null || variants.isEmpty()) {
            return 0;
        }
        String trimmed = value.trim();
        for (int i = 0; i < variants.size(); i++) {
            if (trimmed.equals(variants.get(i))) {
                return i;
            }
        }
        return 0;
    }
}
