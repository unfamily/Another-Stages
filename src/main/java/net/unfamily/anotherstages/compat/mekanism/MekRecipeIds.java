package net.unfamily.anotherstages.compat.mekanism;

import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.recipe.RecipeInstanceIds;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
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
    public static Identifier idOf(
            @Nullable Object recipe,
            @Nullable Level level,
            @Nullable IMekanismRecipeTypeProvider<?, ?, ?> typeProvider) {
        if (recipe == null || level == null || typeProvider == null) {
            return null;
        }
        Collection<? extends RecipeHolder<?>> holders = typeProvider.getRecipes(level);
        Identifier found =
                RecipeInstanceIds.idOf(recipe, typeProvider, holders, h -> h.id().identifier());
        if (found == null) {
            return null;
        }
        Identifier parent = parentOfSynthetic(found);
        return parent != null ? parent : found;
    }

    /** Ids to consult for stage rules: given id plus synthetic/parent siblings. */
    public static List<Identifier> matchIds(@Nullable Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        Set<Identifier> out = new LinkedHashSet<>();
        if (recipeId != null) {
            out.add(recipeId);
            Identifier parent = parentOfSynthetic(recipeId);
            if (parent != null) {
                out.add(parent);
            }
            Identifier synthetic = asSyntheticSmelting(recipeId);
            if (synthetic != null) {
                out.add(synthetic);
            }
        }
        if (holder != null) {
            Identifier holderId = holder.id().identifier();
            if (holderId != null) {
                out.add(holderId);
                Identifier parent = parentOfSynthetic(holderId);
                if (parent != null) {
                    out.add(parent);
                }
            }
        }
        return new ArrayList<>(out);
    }

    /** {@code ns:/mekanism_generated/path} → {@code ns:path}. */
    @Nullable
    public static Identifier parentOfSynthetic(@Nullable Identifier recipeId) {
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
        return Identifier.fromNamespaceAndPath(recipeId.getNamespace(), rest);
    }

    /** {@code ns:path} → {@code ns:/mekanism_generated/path} when not already synthetic. */
    @Nullable
    public static Identifier asSyntheticSmelting(@Nullable Identifier recipeId) {
        if (recipeId == null || isSyntheticSmeltingId(recipeId)) {
            return null;
        }
        return Identifier.fromNamespaceAndPath(
                recipeId.getNamespace(), SYNTHETIC_PREFIX + recipeId.getPath());
    }

    public static boolean isSyntheticSmeltingId(@Nullable Identifier recipeId) {
        return parentOfSynthetic(recipeId) != null;
    }

    /** True for Mek's vanilla-smelting wrap ({@code WrappedSmelterRecipe}). */
    public static boolean isWrappedSmelterRecipe(@Nullable RecipeHolder<?> holder) {
        if (holder == null) {
            return false;
        }
        return holder.value().getClass().getName().contains("WrappedSmelterRecipe");
    }

    /**
     * When id/holder is synthetic or wrapped smelting, the vanilla cooking type
     * ({@code minecraft:smelting}) that {@code recipe_types} locks should also see.
     */
    @Nullable
    public static Identifier vanillaCookingTypeFor(
            @Nullable Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        Identifier id = recipeId;
        if (id == null && holder != null) {
            id = holder.id().identifier();
        }
        if (!isSyntheticSmeltingId(id) && !isWrappedSmelterRecipe(holder)) {
            return null;
        }
        return Identifier.fromNamespaceAndPath("minecraft", "smelting");
    }
}
