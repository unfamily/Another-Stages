package net.unfamily.anotherstages.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.block.StageBlockHandler;
import net.unfamily.anotherstages.block.StageBlockRule;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Hit particles skip {@code RenderShape.INVISIBLE}. Use the mimic state so staged blocks still spark.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelBreakEffectMixin {
    @ModifyVariable(
            method = "addBreakingBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/phys/HitResult;)V",
            at = @At("STORE"),
            ordinal = 0,
            remap = false)
    private BlockState anotherStages$mimicHitParticles(BlockState state) {
        if (!AsConfig.mixinBlockAppearance() || !StageBlockHandler.hasAnyRules()) {
            return state;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return state;
        }
        StageBlockRule rule = StageBlockHandler.findActiveRule(state, mc.player, mc.level);
        return StageBlockHandler.usesCamouflage(rule) ? StageBlockHandler.mimicState(rule) : state;
    }
}
