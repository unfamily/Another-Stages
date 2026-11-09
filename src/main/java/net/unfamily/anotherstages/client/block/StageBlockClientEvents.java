package net.unfamily.anotherstages.client.block;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.block.StageBlockHandler;
import net.unfamily.anotherstages.block.StageBlockRule;
import net.unfamily.anotherstages.config.AsConfig;

/**
 * Client-side stage_block mimic mesh (NeoForge 26.1 path).
 * Pattern aligned with Ethereal Frame / Cable Facades:
 * locked blocks use {@link net.minecraft.world.level.block.RenderShape#INVISIBLE},
 * then this injects the mimic geometry via {@link AddSectionGeometryEvent}.
 */
@EventBusSubscriber(modid = AnotherStages.MOD_ID, value = Dist.CLIENT)
public final class StageBlockClientEvents {
    private StageBlockClientEvents() {}

    /** Rebuild visible chunk meshes after stage sync / cache clear. */
    public static void markAllDirty() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    @SubscribeEvent
    public static void onAddSectionGeometry(AddSectionGeometryEvent event) {
        if (!AsConfig.mixinBlockAppearance() || !StageBlockHandler.hasAnyRules()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        BlockPos origin = event.getSectionOrigin();
        event.addRenderer(ctx -> {
            BlockAndTintGetter region = ctx.getRegion();
            BlockStateModelSet modelSet = mc.getModelManager().getBlockStateModelSet();
            ModelBlockRenderer blockRenderer = ctx.getBlockRenderer();

            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                        BlockState actual = region.getBlockState(cursor);
                        if (actual.isAir()) {
                            continue;
                        }
                        StageBlockRule rule = StageBlockHandler.findActiveRule(actual, mc.player, mc.level);
                        if (!StageBlockHandler.usesCamouflage(rule)) {
                            continue;
                        }
                        renderMimic(ctx, blockRenderer, modelSet, region, cursor.immutable(), StageBlockHandler.mimicState(rule));
                    }
                }
            }
        });
    }

    private static void renderMimic(
            AddSectionGeometryEvent.SectionRenderingContext ctx,
            ModelBlockRenderer blockRenderer,
            BlockStateModelSet modelSet,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState mimicState) {
        BlockStateModel model = modelSet.get(mimicState);
        float baseX = SectionPos.sectionRelative(pos.getX());
        float baseY = SectionPos.sectionRelative(pos.getY());
        float baseZ = SectionPos.sectionRelative(pos.getZ());

        BlockQuadOutput output = (x, y, z, quad, instance) -> {
            ChunkSectionLayer layer = quad.materialInfo().layer();
            ctx.getOrCreateChunkBuffer(layer).putBlockBakedQuad(x, y, z, quad, instance);
        };

        blockRenderer.tesselateBlock(
                output,
                baseX, baseY, baseZ,
                level, pos, mimicState, model,
                mimicState.getSeed(pos)
        );
    }
}
