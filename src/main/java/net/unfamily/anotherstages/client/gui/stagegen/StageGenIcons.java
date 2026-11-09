package net.unfamily.anotherstages.client.gui.stagegen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a display stack for Stage Generator matcher / browse rows.
 */
public final class StageGenIcons {
    private StageGenIcons() {}

    public static ItemStack stackFromMatcher(@Nullable String matcher) {
        if (matcher == null || matcher.isBlank()) {
            return ItemStack.EMPTY;
        }
        String m = matcher.trim();
        if (m.startsWith("#") || m.startsWith("@")) {
            return ItemStack.EMPTY;
        }
        Identifier id = Identifier.tryParse(m.contains(":") ? m : "minecraft:" + m);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
        if (item != Items.AIR) {
            return new ItemStack(item);
        }
        Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (block != null) {
            Item asItem = block.asItem();
            if (asItem != null && asItem != Items.AIR) {
                return new ItemStack(asItem);
            }
        }
        return ItemStack.EMPTY;
    }

    public static String glyphForMatcher(@Nullable String matcher) {
        if (matcher == null || matcher.isBlank()) {
            return "?";
        }
        String m = matcher.trim();
        if (m.startsWith("#")) {
            return "#";
        }
        if (m.startsWith("@")) {
            return "@";
        }
        return "·";
    }

    public static ItemStack stackForCategory(RestrictionDraft.Category category) {
        return switch (category) {
            case ITEM -> new ItemStack(Items.IRON_INGOT);
            case RECIPE -> new ItemStack(Items.CRAFTING_TABLE);
            case BLOCK -> new ItemStack(Items.STONE);
            case GUI -> new ItemStack(Items.CHEST);
        };
    }
}
