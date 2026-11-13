package net.unfamily.anotherstages.compat.enderio;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ender IO Alloy Smelter remaps vanilla smelting into {@code enderio:smelting/&lt;ns&gt;/&lt;path&gt;}
 * ({@code WrappedAlloySmeltingRecipe}). Dual-id matching lets packdevs lock either the remapped
 * id or the parent vanilla id. Defensive prefixes for blasting/smoking if a future EIO build
 * remaps those too.
 */
public final class EnderIoRecipeIds {
    private static final String EIO = "enderio";
    private static final String[] REMAP_PREFIXES = {"smelting/", "blasting/", "smoking/"};

    private EnderIoRecipeIds() {}

    /**
     * Recipe ids to consult for stage rules: the given id plus parent/remapped siblings.
     */
    public static List<Identifier> matchIds(@Nullable Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        Set<Identifier> out = new LinkedHashSet<>();
        if (recipeId != null) {
            out.add(recipeId);
            Identifier parent = parentOfRemapped(recipeId);
            if (parent != null) {
                out.add(parent);
            }
            Identifier remapped = asRemappedSmelting(recipeId);
            if (remapped != null) {
                out.add(remapped);
            }
        }
        if (holder != null) {
            Identifier holderId = holder.id().identifier();
            if (holderId != null) {
                out.add(holderId);
                Identifier parent = parentOfRemapped(holderId);
                if (parent != null) {
                    out.add(parent);
                }
            }
        }
        return new ArrayList<>(out);
    }

    /** {@code enderio:smelting/ns/path} → {@code ns:path} (also blasting/smoking prefixes). */
    @Nullable
    public static Identifier parentOfRemapped(@Nullable Identifier recipeId) {
        if (recipeId == null || !EIO.equals(recipeId.getNamespace())) {
            return null;
        }
        String path = recipeId.getPath();
        for (String prefix : REMAP_PREFIXES) {
            if (path.startsWith(prefix)) {
                String rest = path.substring(prefix.length());
                int slash = rest.indexOf('/');
                if (slash <= 0 || slash >= rest.length() - 1) {
                    return null;
                }
                String ns = rest.substring(0, slash);
                String parentPath = rest.substring(slash + 1);
                return Identifier.fromNamespaceAndPath(ns, parentPath);
            }
        }
        return null;
    }

    /** {@code ns:path} → {@code enderio:smelting/ns/path} when not already an EIO remap. */
    @Nullable
    public static Identifier asRemappedSmelting(@Nullable Identifier recipeId) {
        if (recipeId == null || EIO.equals(recipeId.getNamespace())) {
            return null;
        }
        String path = "smelting/" + recipeId.getNamespace() + "/" + recipeId.getPath();
        return Identifier.fromNamespaceAndPath(EIO, path);
    }

    public static boolean isRemappedCookingId(@Nullable Identifier recipeId) {
        return parentOfRemapped(recipeId) != null;
    }

    /**
     * True only for EIO's vanilla-smelting wrap ({@code WrappedAlloySmeltingRecipe}).
     * Real alloy recipes must not inherit {@code minecraft:smelting} locks.
     */
    public static boolean isWrappedAlloyRecipe(@Nullable RecipeHolder<?> holder) {
        if (holder == null) {
            return false;
        }
        return holder.value().getClass().getName().contains("WrappedAlloySmeltingRecipe");
    }

    /**
     * When the holder/id is remapped cooking or a wrapped smelting recipe, the vanilla
     * {@code RecipeType} id that stage {@code recipe_types} rules should also see
     * ({@code minecraft:smelting} / blasting / smoking).
     */
    @Nullable
    public static Identifier vanillaCookingTypeFor(
            @Nullable Identifier recipeId, @Nullable RecipeHolder<?> holder) {
        Identifier id = recipeId;
        if (id == null && holder != null) {
            id = holder.id().identifier();
        }
        if (!isRemappedCookingId(id) && !isWrappedAlloyRecipe(holder)) {
            return null;
        }
        String path = id != null ? id.getPath() : "";
        if (path.startsWith("blasting/")) {
            return Identifier.fromNamespaceAndPath("minecraft", "blasting");
        }
        if (path.startsWith("smoking/")) {
            return Identifier.fromNamespaceAndPath("minecraft", "smoking");
        }
        return Identifier.fromNamespaceAndPath("minecraft", "smelting");
    }
}
