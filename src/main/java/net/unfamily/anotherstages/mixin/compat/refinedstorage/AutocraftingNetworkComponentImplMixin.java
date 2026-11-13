package net.unfamily.anotherstages.mixin.compat.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.refinedmods.refinedstorage.api.autocrafting.calculation.CancellationToken;
import com.refinedmods.refinedstorage.api.autocrafting.calculation.CraftingCalculator;
import com.refinedmods.refinedstorage.api.autocrafting.calculation.CraftingCalculatorImpl;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskId;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskPlan;
import com.refinedmods.refinedstorage.api.network.autocrafting.AutocraftingNetworkComponent;
import com.refinedmods.refinedstorage.api.network.impl.autocrafting.AutocraftingNetworkComponentImpl;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.root.RootStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.unfamily.anotherstages.compat.refinedstorage.RsCraftGate;
import net.unfamily.anotherstages.compat.refinedstorage.RsCraftSubjects;
import net.unfamily.anotherstages.config.AsConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static com.refinedmods.refinedstorage.api.autocrafting.task.TaskPlanCraftingCalculatorListener.calculatePlan;

/**
 * Gate RS autocraft startTask / ensureTask when any INTERNAL pattern recipe is stage-locked.
 */
@Mixin(value = AutocraftingNetworkComponentImpl.class, remap = false)
public abstract class AutocraftingNetworkComponentImplMixin {
    @Shadow(remap = false)
    @Final
    private Supplier<RootStorage> rootStorageProvider;

    @Shadow(remap = false)
    @Final
    private com.refinedmods.refinedstorage.api.autocrafting.PatternRepositoryImpl patternRepository;

    @WrapMethod(method = "startTask", remap = false)
    private Optional<TaskId> anotherStages$gateStartTask(
            ResourceKey resource,
            long amount,
            Actor actor,
            boolean notify,
            CancellationToken cancellationToken,
            Operation<Optional<TaskId>> original) {
        if (!AsConfig.mixinRefinedStorage()) {
            return original.call(resource, amount, actor, notify, cancellationToken);
        }
        if (isPlanStageBlocked(resource, amount, actor, cancellationToken)) {
            return Optional.empty();
        }
        return original.call(resource, amount, actor, notify, cancellationToken);
    }

    @WrapMethod(method = "ensureTask", remap = false)
    private AutocraftingNetworkComponent.EnsureResult anotherStages$gateEnsureTask(
            ResourceKey resource,
            long amount,
            Actor actor,
            CancellationToken cancellationToken,
            Operation<AutocraftingNetworkComponent.EnsureResult> original) {
        if (!AsConfig.mixinRefinedStorage()) {
            return original.call(resource, amount, actor, cancellationToken);
        }
        // Only refuse when a full plan for the requested amount would be stage-locked.
        // Partial ensureTaskForCraftableAmount still goes through start-equivalent addTask via original.
        UUID owner = RsCraftGate.resolveActorUuid(actor, RsCraftSubjects.currentLevel());
        Level level = RsCraftGate.resolveLevel(RsCraftSubjects.currentLevel(), owner);
        try {
            RootStorage rootStorage = rootStorageProvider.get();
            CraftingCalculator calculator = new CraftingCalculatorImpl(patternRepository, rootStorage);
            Optional<TaskPlan> plan = calculatePlan(calculator, resource, amount, cancellationToken);
            if (plan.isPresent()) {
                ResourceLocation blocked = RsCraftGate.findBlockedRecipeInPlan(plan.get(), owner, level);
                if (blocked != null) {
                    notifyIfPlayer(blocked);
                    return AutocraftingNetworkComponent.EnsureResult.MISSING_RESOURCES;
                }
            }
        } catch (Throwable ignored) {
            // Fall through to vanilla ensureTask on calculator errors
        }
        AutocraftingNetworkComponent.EnsureResult result =
                original.call(resource, amount, actor, cancellationToken);
        return result;
    }

    private boolean isPlanStageBlocked(
            ResourceKey resource, long amount, Actor actor, CancellationToken cancellationToken) {
        UUID owner = RsCraftGate.resolveActorUuid(actor, RsCraftSubjects.currentLevel());
        Level level = RsCraftGate.resolveLevel(RsCraftSubjects.currentLevel(), owner);
        try {
            RootStorage rootStorage = rootStorageProvider.get();
            CraftingCalculator calculator = new CraftingCalculatorImpl(patternRepository, rootStorage);
            Optional<TaskPlan> plan = calculatePlan(calculator, resource, amount, cancellationToken);
            if (plan.isEmpty()) {
                return false;
            }
            ResourceLocation blocked = RsCraftGate.findBlockedRecipeInPlan(plan.get(), owner, level);
            if (blocked != null) {
                notifyIfPlayer(blocked);
                return true;
            }
        } catch (Throwable ignored) {
            return false;
        }
        return false;
    }

    private static void notifyIfPlayer(@Nullable ResourceLocation recipeId) {
        Player player = RsCraftSubjects.currentPlayer();
        if (player != null) {
            RsCraftGate.notifyBlocked(player, recipeId);
        }
    }
}
