package net.unfamily.anotherstages.compat.jade;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.block.StageBlockHandler;
import net.unfamily.anotherstages.block.StageBlockRule;
import net.unfamily.anotherstages.item.AsItems;
import net.unfamily.anotherstages.item.StageItemHandler;
import net.unfamily.anotherstages.item.StageItemRvHelper;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

/**
 * Jade: camouflaged stage blocks show as mimic via ray-trace; non-camouflage locks stay clear.
 * Staged item entities can be renamed / hardcore-swapped.
 */
@WailaPlugin
public final class AnotherStagesJadePlugin implements IWailaPlugin {
    private static IWailaClientRegistration client;

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        client = registration;
        registration.addRayTraceCallback(AnotherStagesJadePlugin::onRayTrace);
        registration.registerEntityComponent(StageItemEntityHideProvider.INSTANCE, ItemEntity.class);
        registration.registerEntityIcon(StageItemEntityHideProvider.INSTANCE, ItemEntity.class);
    }

    private static snownee.jade.api.Accessor<?> onRayTrace(
            net.minecraft.world.phys.HitResult hit,
            snownee.jade.api.Accessor<?> accessor,
            snownee.jade.api.Accessor<?> original) {
        if (client == null || !(accessor instanceof snownee.jade.api.BlockAccessor blockAccessor)) {
            return accessor;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return accessor;
        }
        StageBlockRule rule = StageBlockHandler.findActiveRule(blockAccessor.getBlockState(), mc.player, mc.level);
        if (!StageBlockHandler.usesCamouflage(rule)) {
            return accessor;
        }
        BlockState mimic = StageBlockHandler.mimicState(rule);
        return client.blockAccessor()
                .from(blockAccessor)
                .blockState(mimic)
                .blockEntity(() -> null)
                .fakeBlock(new ItemStack(mimic.getBlock()))
                .build();
    }

    private enum StageItemEntityHideProvider implements IEntityComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(AnotherStages.MOD_ID, "jade_item_hide");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public int getDefaultPriority() {
            return 10_000;
        }

        @Override
        public IElement getIcon(EntityAccessor accessor, IPluginConfig config, IElement currentIcon) {
            if (!(accessor.getEntity() instanceof ItemEntity itemEntity)) {
                return currentIcon;
            }
            ItemStack stack = itemEntity.getItem();
            StageItemRvHelper.Mode mode = StageItemRvHelper.modeFor(stack.getItem());
            if (mode == StageItemRvHelper.Mode.HARDCORE) {
                return IElementHelper.get().item(new ItemStack(AsItems.STAGE_LOCKED_HARDCORE.get()));
            }
            return currentIcon;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof ItemEntity itemEntity)) {
                return;
            }
            ItemStack stack = itemEntity.getItem();
            if (!StageItemHandler.hasLoadedRestrictions()) {
                return;
            }
            StageItemRvHelper.Mode mode = StageItemRvHelper.modeFor(stack.getItem());
            if (mode == StageItemRvHelper.Mode.NONE || mode == StageItemRvHelper.Mode.HIDE) {
                return;
            }
            if (mode == StageItemRvHelper.Mode.HARDCORE
                    || StageItemRvHelper.shouldHideItemName(stack.getItem())) {
                tooltip.replace(JadeIds.CORE_OBJECT_NAME, StageItemRvHelper.hiddenDisplayName(stack.getItem()));
                tooltip.remove(JadeIds.CORE_MOD_NAME);
                tooltip.remove(JadeIds.DEBUG_REGISTRY_NAME);
            }
        }
    }
}
