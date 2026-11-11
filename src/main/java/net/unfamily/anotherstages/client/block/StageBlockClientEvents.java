package net.unfamily.anotherstages.client.block;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.unfamily.anotherstages.AnotherStages;

/**
 * Client-side stage_block helpers for 1.21.1.
 * NeoForge 26 mesh injection ({@code AddSectionGeometryEvent}) is not used here;
 * camouflage relies on appearance / render-shape mixins.
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
}
