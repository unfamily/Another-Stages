package net.unfamily.anotherstages.block;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * Enforces stage_block break rules, unbreakable flag, warnings, and mimic loot.
 */
@EventBusSubscriber(modid = AnotherStages.MOD_ID)
public final class StageBlockEvents {
    private StageBlockEvents() {}

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        StageBlockRule rule = StageBlockHandler.findActiveRule(event.getState(), player, player.level());
        if (rule != null && rule.unbreakable) {
            event.setNewSpeed(0.0F);
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
        List<ItemStack> stacks = Block.getDrops(
                mimic, event.getLevel(), event.getPos(), event.getBlockEntity(), event.getBreaker(), event.getTool());
        double x = event.getPos().getX() + 0.5;
        double y = event.getPos().getY() + 0.5;
        double z = event.getPos().getZ() + 0.5;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                event.getDrops().add(new ItemEntity(event.getLevel(), x, y, z, stack));
            }
        }
    }
}
