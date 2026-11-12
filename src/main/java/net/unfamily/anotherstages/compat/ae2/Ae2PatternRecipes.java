package net.unfamily.anotherstages.compat.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.AESmithingTablePattern;
import appeng.crafting.pattern.AEStonecuttingPattern;
import appeng.crafting.pattern.EncodedCraftingPattern;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Extract vanilla craft/stonecutting/smithing recipe ids from AE2 encoded patterns.
 * On 1.21.1 AE2 exposes {@link ResourceLocation} recipe ids (not ResourceKey).
 */
public final class Ae2PatternRecipes {
    private Ae2PatternRecipes() {}

    @Nullable
    public static ResourceLocation vanillaRecipeId(@Nullable IPatternDetails pattern) {
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
