package net.unfamily.anotherstages.compat.create;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.recipe.RecipeInstanceIds;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Create recipe ids for stage matching.
 * <p>
 * Craft Deployer resolves {@code create:item_application} (and sandpaper) under the
 * <strong>base</strong> datapack id. Manual right-click is gated separately
 * ({@code ManualApplicationRecipeMixin}) for the interacting player.
 * <p>
 * JEI Deploying synthesizes {@code …_using_deployer} via {@code asDeploying} / sandpaper
 * {@code convert}. Dual-id matching applies the same stage lock both ways.
 * <p>
 * {@link #idOf} uses {@link RecipeInstanceIds} — never full-scan on every basin/millstone tick.
 */
public final class CreateRecipeIds {
    /** Create JEI / convert suffix ({@code ResourceLocation#withSuffix}). */
    public static final String USING_DEPLOYER_SUFFIX = "_using_deployer";

    private CreateRecipeIds() {}

    /** @deprecated use {@link RecipeInstanceIds#clear()} */
    public static void clearIdCache() {
        RecipeInstanceIds.clear();
    }

    /**
     * Resolve a Create {@link Recipe} instance back to its registry id for stage matching.
     */
    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ResourceLocation idOf(@Nullable Recipe<?> recipe, @Nullable Level level) {
        if (recipe == null || level == null || level.isClientSide()) {
            return null;
        }
        RecipeManager manager = level.getRecipeManager();
        RecipeType type = recipe.getType();
        // Prefer typed list — never scan the entire RecipeManager on basin/millstone ticks.
        return RecipeInstanceIds.idOf(recipe, type, manager.getAllRecipesFor(type), RecipeHolder::id);
    }

    public static List<ResourceLocation> matchIds(
            @Nullable ResourceLocation recipeId, @Nullable RecipeHolder<?> holder) {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        addWithDeployerAliases(out, recipeId);
        if (holder != null) {
            addWithDeployerAliases(out, holder.id());
        }
        return new ArrayList<>(out);
    }

    private static void addWithDeployerAliases(
            Set<ResourceLocation> out, @Nullable ResourceLocation recipeId) {
        if (recipeId == null) {
            return;
        }
        out.add(recipeId);
        ResourceLocation parent = parentOfUsingDeployer(recipeId);
        if (parent != null) {
            out.add(parent);
        }
        ResourceLocation synthetic = asUsingDeployer(recipeId);
        if (synthetic != null) {
            out.add(synthetic);
        }
    }

    /**
     * {@code ns:path_using_deployer} → {@code ns:path}; otherwise {@code null}.
     */
    @Nullable
    public static ResourceLocation parentOfUsingDeployer(@Nullable ResourceLocation recipeId) {
        if (recipeId == null) {
            return null;
        }
        String path = recipeId.getPath();
        if (!path.endsWith(USING_DEPLOYER_SUFFIX)) {
            return null;
        }
        String parentPath = path.substring(0, path.length() - USING_DEPLOYER_SUFFIX.length());
        if (parentPath.isEmpty()) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(recipeId.getNamespace(), parentPath);
    }

    /**
     * {@code ns:path} → {@code ns:path_using_deployer}; already-suffixed or null → {@code null}.
     */
    @Nullable
    public static ResourceLocation asUsingDeployer(@Nullable ResourceLocation recipeId) {
        if (recipeId == null || parentOfUsingDeployer(recipeId) != null) {
            return null;
        }
        return recipeId.withSuffix(USING_DEPLOYER_SUFFIX);
    }

    public static boolean isUsingDeployerId(@Nullable ResourceLocation recipeId) {
        return parentOfUsingDeployer(recipeId) != null;
    }
}
