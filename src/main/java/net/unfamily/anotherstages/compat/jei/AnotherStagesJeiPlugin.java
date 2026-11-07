package net.unfamily.anotherstages.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.client.ClientStageCache;
import net.unfamily.anotherstages.item.StageItemHandler;
import net.unfamily.anotherstages.item.StageItemRvHelper;
import net.unfamily.anotherstages.recipe.StageRecipeHandler;
import net.unfamily.anotherstages.recipe.StageRecipeRvHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JEI: hide staged ingredients / recipes, lock overlays via NeoForge item decorations
 * and {@link StageRecipeLockDecorator}.
 */
@JeiPlugin
public final class AnotherStagesJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(AnotherStages.MOD_ID, "jei");
    private static IJeiRuntime runtime;
    private static final Set<Item> currentlyHiddenItems = new HashSet<>();
    private static final Map<IRecipeType<?>, Set<Object>> currentlyHiddenRecipes = new HashMap<>();
    private static String lastFingerprint = "";

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        Set<Identifier> seen = new HashSet<>();
        registration.getJeiHelpers().getAllRecipeTypes().forEach(type -> {
            if (seen.add(type.getUid())) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                IRecipeType rawType = type;
                registration.addRecipeCategoryDecorator(rawType, StageRecipeLockDecorator.INSTANCE);
            }
        });
        // Fallback for vanilla holder types not yet exposed as JEI categories
        for (RecipeType<?> vanillaType : BuiltInRegistries.RECIPE_TYPE) {
            IRecipeHolderType<?> holderType = IRecipeHolderType.create(vanillaType);
            if (seen.add(holderType.getUid())) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                IRecipeType rawType = holderType;
                registration.addRecipeCategoryDecorator(rawType, StageRecipeLockDecorator.INSTANCE);
            }
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        lastFingerprint = "";
        refreshAll();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        currentlyHiddenItems.clear();
        currentlyHiddenRecipes.clear();
        lastFingerprint = "";
    }

    /** Called from a soft client tick when JEI is present. */
    public static void onClientTick() {
        if (runtime == null) {
            return;
        }
        refreshAll();
    }

    private static void refreshAll() {
        if (runtime == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) {
            restoreAllItems(runtime.getIngredientManager());
            restoreAllRecipes(runtime.getRecipeManager());
            return;
        }

        String fingerprint = ClientStageCache.fingerprint()
                + "|items=" + (StageItemHandler.hasLoadedRestrictions() ? "1" : "0")
                + "|recipes=" + StageRecipeHandler.clientRuleRevision();
        if (fingerprint.equals(lastFingerprint)) {
            return;
        }
        lastFingerprint = fingerprint;

        refreshHiddenIngredients(player, level);
        refreshHiddenRecipes();
    }

    private static void refreshHiddenIngredients(Player player, Level level) {
        IIngredientManager manager = runtime.getIngredientManager();
        if (!StageItemHandler.hasLoadedRestrictions()) {
            restoreAllItems(manager);
            return;
        }

        Set<Item> shouldHide = new HashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (StageItemRvHelper.modeFor(player, level, item) == StageItemRvHelper.Mode.HIDE) {
                shouldHide.add(item);
            }
        }

        List<ItemStack> toRestore = new ArrayList<>();
        for (Item item : currentlyHiddenItems) {
            if (!shouldHide.contains(item)) {
                toRestore.add(new ItemStack(item));
            }
        }
        if (!toRestore.isEmpty()) {
            manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toRestore);
        }

        List<ItemStack> toHide = new ArrayList<>();
        for (Item item : shouldHide) {
            if (!currentlyHiddenItems.contains(item)) {
                toHide.add(new ItemStack(item));
            }
        }
        if (!toHide.isEmpty()) {
            manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toHide);
        }

        currentlyHiddenItems.clear();
        currentlyHiddenItems.addAll(shouldHide);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void refreshHiddenRecipes() {
        IRecipeManager recipeManager = runtime.getRecipeManager();
        if (!StageRecipeHandler.hasLoadedRules()) {
            restoreAllRecipes(recipeManager);
            return;
        }

        Map<IRecipeType<?>, Set<Object>> shouldHide = new HashMap<>();
        List<IRecipeCategory<?>> categories = recipeManager.createRecipeCategoryLookup()
                .includeHidden()
                .get()
                .toList();

        for (IRecipeCategory<?> category : categories) {
            IRecipeType<?> type = category.getRecipeType();
            recipeManager.createRecipeLookup(type)
                    .includeHidden()
                    .get()
                    .forEach(recipe -> {
                        Identifier id = StageRecipeRvHelper.idOf(recipe, category);
                        RecipeHolder<?> holder = StageRecipeRvHelper.asHolder(recipe, id);
                        if (StageRecipeRvHelper.modeFor(id, holder) == StageRecipeRvHelper.Mode.HIDE) {
                            shouldHide.computeIfAbsent(type, t -> new HashSet<>()).add(recipe);
                        }
                    });
        }

        for (Map.Entry<IRecipeType<?>, Set<Object>> entry : currentlyHiddenRecipes.entrySet()) {
            IRecipeType type = entry.getKey();
            Set<Object> wasHidden = entry.getValue();
            Set<Object> nowHidden = shouldHide.getOrDefault(type, Set.of());
            List toRestore = new ArrayList();
            for (Object recipe : wasHidden) {
                if (!nowHidden.contains(recipe)) {
                    toRestore.add(recipe);
                }
            }
            if (!toRestore.isEmpty()) {
                recipeManager.unhideRecipes(type, toRestore);
            }
        }

        for (Map.Entry<IRecipeType<?>, Set<Object>> entry : shouldHide.entrySet()) {
            IRecipeType type = entry.getKey();
            Set<Object> nowHidden = entry.getValue();
            Set<Object> wasHidden = currentlyHiddenRecipes.getOrDefault(type, Set.of());
            List toHide = new ArrayList();
            for (Object recipe : nowHidden) {
                if (!wasHidden.contains(recipe)) {
                    toHide.add(recipe);
                }
            }
            if (!toHide.isEmpty()) {
                recipeManager.hideRecipes(type, toHide);
            }
        }

        currentlyHiddenRecipes.clear();
        currentlyHiddenRecipes.putAll(shouldHide);
    }

    private static void restoreAllItems(IIngredientManager manager) {
        if (currentlyHiddenItems.isEmpty()) {
            return;
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : currentlyHiddenItems) {
            stacks.add(new ItemStack(item));
        }
        manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacks);
        currentlyHiddenItems.clear();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void restoreAllRecipes(IRecipeManager recipeManager) {
        if (currentlyHiddenRecipes.isEmpty()) {
            return;
        }
        for (Map.Entry<IRecipeType<?>, Set<Object>> entry : currentlyHiddenRecipes.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                recipeManager.unhideRecipes((IRecipeType) entry.getKey(), (Collection) entry.getValue());
            }
        }
        currentlyHiddenRecipes.clear();
    }
}
