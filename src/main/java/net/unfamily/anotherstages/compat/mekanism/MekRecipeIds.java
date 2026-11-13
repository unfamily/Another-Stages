package net.unfamily.anotherstages.compat.mekanism;

import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Mekanism recipe ids for stage matching.
 * <p>
 * Energized Smelter wraps vanilla {@code RecipeType.SMELTING} as {@code WrappedSmelterRecipe}
 * under a synthetic id ({@code &lt;ns&gt;:/mekanism_generated/&lt;path&gt;}) with runtime type
 * {@code mekanism:smelting}. Dual-id + type parity lets {@code recipe_types: [minecraft:smelting]}
 * and parent vanilla ids gate the machine the same way as EIO remapped cooking.
 */
public final class MekRecipeIds {
    /**
     * Mek {@code RegistryUtils.synthetic} uses {@code withPrefix("/" + name + "/")} —
     * path becomes {@code /mekanism_generated/&lt;originalPath&gt;}.
     */
    private static final String SYNTHETIC_PREFIX = "/mekanism_generated/";
    /** Defensive: older docs/builds without the leading slash. */
    private static final String SYNTHETIC_PREFIX_ALT = "mekanism_generated/";

    private MekRecipeIds() {}

    /**
     * Resolve Mek recipe registry id from the machine's recipe list.
     * Prefer the vanilla parent when the holder uses a {@code mekanism_generated/} synthetic id
     * so {@code RecipeManager.byKey} and smoke locks resolve.
     */
    @Nullable
    public static ResourceLocation idOf(
            @Nullable Object recipe,
            @Nullable Level level,
            @Nullable IMekanismRecipeTypeProvider<?, ?, ?> typeProvider) {
        if (recipe == null || level == null || typeProvider == null) {
            return null;
        }
        ResourceLocation found = null;
        for (RecipeHolder<?> holder : typeProvider.getRecipes(level)) {
            if (holder.value() == recipe) {
                found = holder.id();
                break;
            }
        }
        if (found == null) {
            for (RecipeHolder<?> holder : typeProvider.getRecipes(level)) {
                if (holder.value().equals(recipe)) {
                    found = holder.id();
                    break;
                }
            }
        }
        if (found == null) {
            return null;
        }
        ResourceLocation parent = parentOfSynthetic(found);
        return parent != null ? parent : found;
    }

    public static List<ResourceLocation> matchIds(
            @Nullable ResourceLocation recipeId, @Nullable RecipeHolder<?> holder) {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        if (recipeId != null) {
            out.add(recipeId);
            ResourceLocation parent = parentOfSynthetic(recipeId);
            if (parent != null) {
                out.add(parent);
            }
            ResourceLocation synthetic = asSyntheticSmelting(recipeId);
            if (synthetic != null) {
                out.add(synthetic);
            }
        }
        if (holder != null) {
            ResourceLocation holderId = holder.id();
            if (holderId != null) {
                out.add(holderId);
                ResourceLocation parent = parentOfSynthetic(holderId);
                if (parent != null) {
                    out.add(parent);
                }
            }
        }
        return new ArrayList<>(out);
    }

    @Nullable
    public static ResourceLocation parentOfSynthetic(@Nullable ResourceLocation recipeId) {
        if (recipeId == null) {
            return null;
        }
        String path = recipeId.getPath();
        String rest = null;
        if (path.startsWith(SYNTHETIC_PREFIX)) {
            rest = path.substring(SYNTHETIC_PREFIX.length());
        } else if (path.startsWith(SYNTHETIC_PREFIX_ALT)) {
            rest = path.substring(SYNTHETIC_PREFIX_ALT.length());
        }
        if (rest == null || rest.isEmpty()) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(recipeId.getNamespace(), rest);
    }

    @Nullable
    public static ResourceLocation asSyntheticSmelting(@Nullable ResourceLocation recipeId) {
        if (recipeId == null || isSyntheticSmeltingId(recipeId)) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(
                recipeId.getNamespace(), SYNTHETIC_PREFIX + recipeId.getPath());
    }

    public static boolean isSyntheticSmeltingId(@Nullable ResourceLocation recipeId) {
        return parentOfSynthetic(recipeId) != null;
    }

    public static boolean isWrappedSmelterRecipe(@Nullable RecipeHolder<?> holder) {
        if (holder == null) {
            return false;
        }
        return holder.value().getClass().getName().contains("WrappedSmelterRecipe");
    }

    @Nullable
    public static ResourceLocation vanillaCookingTypeFor(
            @Nullable ResourceLocation recipeId, @Nullable RecipeHolder<?> holder) {
        ResourceLocation id = recipeId;
        if (id == null && holder != null) {
            id = holder.id();
        }
        if (!isSyntheticSmeltingId(id) && !isWrappedSmelterRecipe(holder)) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath("minecraft", "smelting");
    }
}
