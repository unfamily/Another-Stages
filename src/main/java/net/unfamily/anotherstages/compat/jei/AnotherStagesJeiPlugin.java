package net.unfamily.anotherstages.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.client.ClientStageCache;
import net.unfamily.anotherstages.item.StageItemHandler;
import net.unfamily.anotherstages.item.StageItemRvHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hides JEI ingredients for active {@code rv: hide} stage-item rules.
 * Lock overlay uses NeoForge item decorations (inventory + JEI).
 */
@JeiPlugin
public final class AnotherStagesJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(AnotherStages.MOD_ID, "jei");
    private static IJeiRuntime runtime;
    private static final Set<Item> currentlyHidden = new HashSet<>();
    private static String lastFingerprint = "";

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        lastFingerprint = "";
        refreshHiddenIngredients();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        currentlyHidden.clear();
        lastFingerprint = "";
    }

    /** Called from a soft client tick when JEI is present. */
    public static void onClientTick() {
        if (runtime == null) {
            return;
        }
        refreshHiddenIngredients();
    }

    private static void refreshHiddenIngredients() {
        if (runtime == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null || !StageItemHandler.hasLoadedRestrictions()) {
            restoreAll(runtime.getIngredientManager());
            return;
        }

        String fingerprint = ClientStageCache.fingerprint();
        if (fingerprint.equals(lastFingerprint)) {
            return;
        }
        lastFingerprint = fingerprint;

        IIngredientManager manager = runtime.getIngredientManager();
        Set<Item> shouldHide = new HashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (StageItemRvHelper.modeFor(player, level, item) == StageItemRvHelper.Mode.HIDE) {
                shouldHide.add(item);
            }
        }

        List<ItemStack> toRestore = new ArrayList<>();
        for (Item item : currentlyHidden) {
            if (!shouldHide.contains(item)) {
                toRestore.add(new ItemStack(item));
            }
        }
        if (!toRestore.isEmpty()) {
            manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toRestore);
        }

        List<ItemStack> toHide = new ArrayList<>();
        for (Item item : shouldHide) {
            if (!currentlyHidden.contains(item)) {
                toHide.add(new ItemStack(item));
            }
        }
        if (!toHide.isEmpty()) {
            manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toHide);
        }

        currentlyHidden.clear();
        currentlyHidden.addAll(shouldHide);
    }

    private static void restoreAll(IIngredientManager manager) {
        if (currentlyHidden.isEmpty()) {
            return;
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : currentlyHidden) {
            stacks.add(new ItemStack(item));
        }
        manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacks);
        currentlyHidden.clear();
        lastFingerprint = "";
    }
}
