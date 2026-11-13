package net.unfamily.anotherstages.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.autocrafting.Pattern;
import com.refinedmods.refinedstorage.api.autocrafting.PatternLayout;
import com.refinedmods.refinedstorage.api.autocrafting.PatternType;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskPlan;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.support.packet.s2c.S2CPackets;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.anotherstages.config.AsConfig;
import net.unfamily.anotherstages.recipe.RecipeIoProbe;
import net.unfamily.anotherstages.recipe.RecipeStageGate;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Shared RS stage checks for matrix / encode / autocraft start.
 */
public final class RsCraftGate {
    private RsCraftGate() {}

    public static boolean isRecipeBlocked(@Nullable Identifier recipeId, @Nullable UUID owner, @Nullable Level level) {
        if (!AsConfig.mixinRefinedStorage() || recipeId == null) {
            return false;
        }
        return RecipeStageGate.isBlockedForOwner(recipeId, owner, level);
    }

    /**
     * @return first stage-blocked recipe id in the plan, or null if none / not applicable
     */
    @Nullable
    public static Identifier findBlockedRecipeInPlan(
            @Nullable TaskPlan plan, @Nullable UUID owner, @Nullable Level level) {
        if (!AsConfig.mixinRefinedStorage() || plan == null || plan.patterns() == null) {
            return null;
        }
        Level resolvedLevel = level != null ? level : fallbackLevel(owner);
        for (Pattern pattern : plan.patterns().keySet()) {
            Identifier blocked = findBlockedRecipe(pattern, owner, resolvedLevel);
            if (blocked != null) {
                return blocked;
            }
        }
        return null;
    }

    @Nullable
    public static Identifier findBlockedRecipe(
            @Nullable Pattern pattern, @Nullable UUID owner, @Nullable Level level) {
        if (pattern == null || pattern.layout() == null) {
            return null;
        }
        PatternLayout layout = pattern.layout();
        if (layout.type() != PatternType.INTERNAL) {
            return null;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        Identifier fromSmithing = matchSmithing(layout, owner, serverLevel);
        if (fromSmithing != null) {
            return fromSmithing;
        }
        Identifier fromStone = matchStonecutter(layout, owner, serverLevel);
        if (fromStone != null) {
            return fromStone;
        }
        return matchCraftingByOutput(layout, owner, serverLevel);
    }

    @Nullable
    private static Identifier matchSmithing(PatternLayout layout, @Nullable UUID owner, ServerLevel level) {
        if (layout.ingredients() == null || layout.ingredients().size() != 3) {
            return null;
        }
        ItemStack template = firstItem(layout.ingredients().get(0).inputs());
        ItemStack base = firstItem(layout.ingredients().get(1).inputs());
        ItemStack addition = firstItem(layout.ingredients().get(2).inputs());
        if (template.isEmpty() || base.isEmpty() || addition.isEmpty()) {
            return null;
        }
        SmithingRecipeInput input = new SmithingRecipeInput(template, base, addition);
        return level.recipeAccess()
                .getRecipeFor(RecipeType.SMITHING, input, level)
                .map(RecipeHolder::id)
                .filter(id -> RecipeStageGate.isBlockedForOwner(id, owner, level))
                .map(net.minecraft.resources.ResourceKey::identifier)
                .orElse(null);
    }

    @Nullable
    private static Identifier matchStonecutter(PatternLayout layout, @Nullable UUID owner, ServerLevel level) {
        if (layout.ingredients() == null || layout.ingredients().size() != 1) {
            return null;
        }
        ItemStack inputStack = firstItem(layout.ingredients().getFirst().inputs());
        ItemStack expected = primaryOutputStack(layout.outputs());
        if (inputStack.isEmpty() || expected.isEmpty()) {
            return null;
        }
        SingleRecipeInput input = new SingleRecipeInput(inputStack);
        for (RecipeHolder<StonecutterRecipe> holder :
                level.recipeAccess().recipeMap().byType(RecipeType.STONECUTTING)) {
            if (!holder.value().matches(input, level)) {
                continue;
            }
            ItemStack out = holder.value().assemble(input);
            if (!ItemStack.isSameItemSameComponents(out, expected)) {
                continue;
            }
            if (RecipeStageGate.isBlockedForOwner(holder.id(), owner, level)) {
                return holder.id().identifier();
            }
            return null;
        }
        return null;
    }

    /**
     * INTERNAL crafting patterns drop empty-slot positions, so resolve by primary output item
     * against crafting recipes (same approach as Autocrafter canStep hardening).
     */
    @Nullable
    private static Identifier matchCraftingByOutput(PatternLayout layout, @Nullable UUID owner, ServerLevel level) {
        Item outputItem = primaryOutputItem(layout.outputs());
        if (outputItem == null) {
            return null;
        }
        String outputId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(outputItem).toString();
        Identifier blockedId = null;
        boolean sawMatch = false;
        boolean sawAllowed = false;
        for (RecipeHolder<CraftingRecipe> holder :
                level.recipeAccess().recipeMap().byType(RecipeType.CRAFTING)) {
            if (!RecipeIoProbe.outputsMatch(holder.value(), List.of(outputId))) {
                continue;
            }
            sawMatch = true;
            if (RecipeStageGate.isBlockedForOwner(holder.id(), owner, level)) {
                if (blockedId == null) {
                    blockedId = holder.id().identifier();
                }
            } else {
                sawAllowed = true;
            }
        }
        if (!sawMatch || sawAllowed) {
            return null;
        }
        return blockedId;
    }

    @Nullable
    public static UUID resolveActorUuid(@Nullable Actor actor, @Nullable Level level) {
        UUID fromSubject = RsCraftSubjects.currentUuid();
        if (fromSubject != null) {
            return fromSubject;
        }
        if (!(actor instanceof PlayerActor(String name)) || name == null || name.isEmpty()) {
            return null;
        }
        MinecraftServer server = serverOf(level);
        if (server == null) {
            return null;
        }
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        return online == null ? null : online.getUUID();
    }

    @Nullable
    public static Level resolveLevel(@Nullable Level hint, @Nullable UUID owner) {
        if (hint != null) {
            return hint;
        }
        Level fromSubject = RsCraftSubjects.currentLevel();
        if (fromSubject != null) {
            return fromSubject;
        }
        return fallbackLevel(owner);
    }

    @Nullable
    private static Level fallbackLevel(@Nullable UUID owner) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        if (owner != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(owner);
            if (player != null) {
                return player.level();
            }
        }
        return server.overworld();
    }

    @Nullable
    private static MinecraftServer serverOf(@Nullable Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getServer();
        }
        return ServerLifecycleHooks.getCurrentServer();
    }

    public static void notifyBlocked(@Nullable Player player, @Nullable Identifier recipeId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Component title = Component.translatable("another_stages.rs.craft_blocked.title");
        Component body = recipeId == null
                ? Component.translatable("another_stages.rs.craft_blocked")
                : Component.translatable("another_stages.rs.craft_blocked_recipe", recipeId.toString());
        try {
            S2CPackets.sendMessage(serverPlayer, title, body);
        } catch (Throwable ignored) {
            // RS packet path unavailable — fall through to action-bar
        }
        serverPlayer.sendSystemMessage(body, true);
    }

    private static ItemStack firstItem(List<? extends com.refinedmods.refinedstorage.api.resource.ResourceKey> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (inputs.getFirst() instanceof ItemResource itemResource) {
            return itemResource.toItemStack();
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    private static Item primaryOutputItem(List<ResourceAmount> outputs) {
        ItemStack stack = primaryOutputStack(outputs);
        return stack.isEmpty() ? null : stack.getItem();
    }

    private static ItemStack primaryOutputStack(List<ResourceAmount> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ResourceAmount amount = outputs.getFirst();
        if (amount == null || !(amount.resource() instanceof ItemResource itemResource)) {
            return ItemStack.EMPTY;
        }
        return itemResource.toItemStack(amount.amount() > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) amount.amount());
    }
}
