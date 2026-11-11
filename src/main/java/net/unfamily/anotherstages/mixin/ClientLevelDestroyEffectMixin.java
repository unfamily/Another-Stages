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
 * Break-burst particles: swap camouflaged stage blocks to the mimic state (textures match stone etc.).
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelDestroyEffectMixin {
    @ModifyVariable(method = "addDestroyBlockEffect", at = @At("HEAD"), argsOnly = true, remap = false)
    private BlockState anotherStages$mimicDestroyParticles(BlockState state) {
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
