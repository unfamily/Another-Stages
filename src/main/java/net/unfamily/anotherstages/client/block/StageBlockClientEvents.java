package net.unfamily.anotherstages.client.block;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.block.StageBlockHandler;
import net.unfamily.anotherstages.block.StageBlockRule;
import net.unfamily.anotherstages.config.AsConfig;

/**
 * Client-side stage_block mimic mesh (NeoForge 1.21.1 path).
 * Locked blocks use {@link net.minecraft.world.level.block.RenderShape#INVISIBLE},
 * then this injects the mimic geometry via {@link AddSectionGeometryEvent}.
 */
@EventBusSubscriber(modid = AnotherStages.MOD_ID, value = Dist.CLIENT)
public final class StageBlockClientEvents {
    private static final ThreadLocal<RandomSource> RANDOM = ThreadLocal.withInitial(RandomSource::create);

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
            BlockRenderDispatcher brd = mc.getBlockRenderer();
            RandomSource random = RANDOM.get();
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                        BlockState actual = ctx.getRegion().getBlockState(cursor);
                        if (actual.isAir()) {
                            continue;
                        }
                        StageBlockRule rule = StageBlockHandler.findActiveRule(actual, mc.player, mc.level);
                        if (!StageBlockHandler.usesCamouflage(rule)) {
                            continue;
                        }
                        BlockState mimicState = StageBlockHandler.mimicState(rule);
                        BlockPos pos = cursor.immutable();

                        random.setSeed(mimicState.getSeed(pos));
                        BakedModel model = brd.getBlockModel(mimicState);
                        ModelData modelData = model.getModelData(ctx.getRegion(), pos, mimicState, ModelData.EMPTY);

                        ctx.getPoseStack().pushPose();
                        ctx.getPoseStack().translate(
                                SectionPos.sectionRelative(pos.getX()),
                                SectionPos.sectionRelative(pos.getY()),
                                SectionPos.sectionRelative(pos.getZ()));

                        for (RenderType renderType : model.getRenderTypes(mimicState, random, modelData)) {
                            brd.renderBatched(
                                    mimicState,
                                    pos,
                                    ctx.getRegion(),
                                    ctx.getPoseStack(),
                                    ctx.getOrCreateChunkBuffer(renderType),
                                    true,
                                    random,
                                    modelData,
                                    renderType);
                        }

                        ctx.getPoseStack().popPose();
                    }
                }
            }
        });
    }
}
