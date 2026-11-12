package net.unfamily.anotherstages.compat.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.AESmithingTablePattern;
import appeng.crafting.pattern.AEStonecuttingPattern;
import appeng.crafting.pattern.EncodedCraftingPattern;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

/**
 * Extract vanilla craft/stonecutting/smithing recipe ids from AE2 encoded patterns.
 * Processing patterns have no vanilla recipe id (gated at the external machine).
 */
public final class Ae2PatternRecipes {
    private Ae2PatternRecipes() {}

    @Nullable
    public static Identifier vanillaRecipeId(@Nullable IPatternDetails pattern) {
        ResourceKey<Recipe<?>> key = vanillaRecipeKey(pattern);
        return key == null ? null : key.identifier();
    }

    @Nullable
    public static ResourceKey<Recipe<?>> vanillaRecipeKey(@Nullable IPatternDetails pattern) {
        if (pattern == null) {
            return null;
        }
        if (pattern instanceof AEStonecuttingPattern stonecutting) {
            return stonecutting.getRecipeId();
        }
        if (pattern instanceof AESmithingTablePattern smithing) {
            return smithing.getRecipeId();
        }
        if (pattern instanceof AECraftingPattern) {
            AEItemKey definition = pattern.getDefinition();
            EncodedCraftingPattern encoded = definition.get(AEComponents.ENCODED_CRAFTING_PATTERN);
            return encoded == null ? null : encoded.recipeId();
        }
        return null;
    }
}
