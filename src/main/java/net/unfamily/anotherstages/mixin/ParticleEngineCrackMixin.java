package net.unfamily.anotherstages.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.block.StageBlockHandler;
import net.unfamily.anotherstages.block.StageBlockRule;
import net.unfamily.anotherstages.config.AsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Hit/crack particles skip {@code RenderShape.INVISIBLE}. Use the mimic state so staged blocks still spark.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineCrackMixin {
    @ModifyVariable(method = "crack", at = @At("STORE"), ordinal = 0, remap = false)
    private BlockState anotherStages$mimicCrack(BlockState state) {
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
