package net.unfamily.anotherstages.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.unfamily.anotherstages.AnotherStages;

import java.util.List;

/**
 * Enforces stage_block break rules, unbreakable flag, mimic mining, warnings, and mimic loot.
 */
@EventBusSubscriber(modid = AnotherStages.MOD_ID)
public final class StageBlockEvents {
    private static final ThreadLocal<Boolean> MIMIC_MINING_REENTRY = ThreadLocal.withInitial(() -> false);

    private StageBlockEvents() {}

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (MIMIC_MINING_REENTRY.get()) {
            return;
        }
        Player player = event.getEntity();
        StageBlockRule rule = StageBlockHandler.findActiveRule(event.getState(), player, player.level());
        if (rule == null) {
            return;
        }
        if (rule.unbreakable) {
            event.setNewSpeed(0.0F);
            return;
        }
        if (!rule.mimicMining) {
            return;
        }
        BlockState mimic = StageBlockHandler.mimicState(rule);
        if (mimic == event.getState()) {
            return;
        }
        BlockPos pos = event.getPosition().orElse(null);
        MIMIC_MINING_REENTRY.set(true);
        try {
            event.setNewSpeed(player.getDestroySpeed(mimic, pos));
        } finally {
            MIMIC_MINING_REENTRY.set(false);
        }
    }

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (MIMIC_MINING_REENTRY.get()) {
            return;
        }
        Player player = event.getEntity();
        StageBlockRule rule = StageBlockHandler.findActiveRule(event.getTargetBlock(), player, player.level());
        if (rule == null) {
            return;
        }
        if (rule.unbreakable) {
            event.setCanHarvest(false);
            return;
        }
        if (!rule.mimicMining) {
            return;
        }
        BlockState mimic = StageBlockHandler.mimicState(rule);
        if (mimic == event.getTargetBlock()) {
            return;
        }
        MIMIC_MINING_REENTRY.set(true);
        try {
            event.setCanHarvest(player.hasCorrectToolForDrops(mimic));
        } finally {
            MIMIC_MINING_REENTRY.set(false);
        }
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        StageBlockRule rule = StageBlockHandler.findActiveRule(event.getState(), player, player.level());
        if (rule == null) {
            return;
        }
        StageBlockHandler.maybeWarnBreak(player, rule);
        if (rule.unbreakable) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }
        Level level = event.getLevel();
        StageBlockRule rule = StageBlockHandler.findActiveRule(event.getState(), player, level);
        if (rule == null) {
            return;
        }
        BlockState mimic = StageBlockHandler.mimicState(rule);
        event.getDrops().clear();
        ServerLevel serverLevel = event.getLevel();
        List<ItemStack> stacks = Block.getDrops(
                mimic, serverLevel, event.getPos(), event.getBlockEntity(), event.getBreaker(), event.getTool());
        double x = event.getPos().getX() + 0.5;
        double y = event.getPos().getY() + 0.5;
        double z = event.getPos().getZ() + 0.5;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                event.getDrops().add(new ItemEntity(serverLevel, x, y, z, stack));
            }
        }
        // Match XP to mimic (e.g. ore → stone = 0), same formula as BlockDropsEvent.
        int mimicXp = mimic.getExpDrop(
                serverLevel, event.getPos(), event.getBlockEntity(), event.getBreaker(), event.getTool());
        event.setDroppedExperience(EnchantmentHelper.processBlockExperience(serverLevel, event.getTool(), mimicXp));
    }
}
