package net.unfamily.anotherstages.compat.enderio;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ender IO Alloy Smelter remaps vanilla smelting into {@code enderio:smelting/&lt;ns&gt;/&lt;path&gt;}
 * ({@code WrappedAlloySmeltingRecipe}). Dual-id matching lets packdevs lock either the remapped
 * id or the parent vanilla id.
 */
public final class EnderIoRecipeIds {
    private static final String EIO = "enderio";
    private static final String[] REMAP_PREFIXES = {"smelting/", "blasting/", "smoking/"};

    private EnderIoRecipeIds() {}

    public static List<ResourceLocation> matchIds(
            @Nullable ResourceLocation recipeId, @Nullable RecipeHolder<?> holder) {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        if (recipeId != null) {
            out.add(recipeId);
            ResourceLocation parent = parentOfRemapped(recipeId);
            if (parent != null) {
                out.add(parent);
            }
            ResourceLocation remapped = asRemappedSmelting(recipeId);
            if (remapped != null) {
                out.add(remapped);
            }
        }
        if (holder != null) {
            ResourceLocation holderId = holder.id();
            if (holderId != null) {
                out.add(holderId);
                ResourceLocation parent = parentOfRemapped(holderId);
                if (parent != null) {
                    out.add(parent);
                }
            }
        }
        return new ArrayList<>(out);
    }

    @Nullable
    public static ResourceLocation parentOfRemapped(@Nullable ResourceLocation recipeId) {
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
                return ResourceLocation.fromNamespaceAndPath(ns, parentPath);
            }
        }
        return null;
    }

    @Nullable
    public static ResourceLocation asRemappedSmelting(@Nullable ResourceLocation recipeId) {
        if (recipeId == null || EIO.equals(recipeId.getNamespace())) {
            return null;
        }
        String path = "smelting/" + recipeId.getNamespace() + "/" + recipeId.getPath();
        return ResourceLocation.fromNamespaceAndPath(EIO, path);
    }

    public static boolean isRemappedCookingId(@Nullable ResourceLocation recipeId) {
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
    public static ResourceLocation vanillaCookingTypeFor(
            @Nullable ResourceLocation recipeId, @Nullable RecipeHolder<?> holder) {
        ResourceLocation id = recipeId;
        if (id == null && holder != null) {
            id = holder.id();
        }
        if (!isRemappedCookingId(id) && !isWrappedAlloyRecipe(holder)) {
            return null;
        }
        String path = id != null ? id.getPath() : "";
        if (path.startsWith("blasting/")) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "blasting");
        }
        if (path.startsWith("smoking/")) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "smoking");
        }
        return ResourceLocation.fromNamespaceAndPath("minecraft", "smelting");
    }
}
