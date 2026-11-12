package net.unfamily.anotherstages.mixin.compat;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Applies Fast* mixins only when the corresponding mod is present. */
public final class AsCompatMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String lower = mixinClassName == null ? "" : mixinClassName.toLowerCase();
        if (lower.contains("fastbench")) {
            return isLoaded("fastbench");
        }
        if (lower.contains("polymorph")) {
            // Polymorph+ on 26.1.2 uses mod id polymorph_plus (not upstream polymorph).
            return isLoaded("polymorph_plus");
        }
        if (lower.contains(".jei.") || lower.contains("simpleitemstackbatchrenderer")) {
            return isLoaded("jei");
        }
        if (lower.contains("fastfurnace") && lower.contains("compat")) {
            // Gate FastFurnace's merged getRecipe(ServerLevel) on AbstractFurnaceBlockEntity
            return isLoaded("fastfurnace");
        }
        // Vanilla CachedCheck redirect fights FastFurnace's redirect on the same call — skip when FF present
        if (lower.contains("abstractfurnaceblockentitymixin")) {
            return !isLoaded("fastfurnace");
        }
        if (lower.contains("ironfurnaces") || lower.contains("iron_furnaces")) {
            return isLoaded("ironfurnaces");
        }
        if (lower.contains(".ae2.") || lower.contains("compat.ae2")) {
            return isLoaded("ae2");
        }
        if (lower.contains(".refinedstorage") || lower.contains("compat.rs2") || lower.contains("compat.refinedstorage")) {
            return isLoaded("refinedstorage");
        }
        if (lower.contains("toms_storage") || lower.contains("tomsstorage") || lower.contains("compat.toms")) {
            return isLoaded("toms_storage");
        }
        if (lower.contains("sophisticated")) {
            return isLoaded("sophisticatedbackpacks") || isLoaded("sophisticatedstorage");
        }
        if (lower.contains(".create.") || lower.contains("compat.create")) {
            return isLoaded("create");
        }
        if (lower.contains("enderio")) {
            return isLoaded("enderio");
        }
        if (lower.contains("energizedpower") || lower.contains("energized_power")) {
            return isLoaded("energizedpower");
        }
        if (lower.contains("craftingstation")) {
            return isLoaded("craftingstation");
        }
        if (lower.contains("mekanism")) {
            return isLoaded("mekanism");
        }
        if (lower.contains("integrateddynamics") || lower.contains("integratedcrafting")) {
            return isLoaded("integrateddynamics") || isLoaded("integratedcrafting");
        }
        if (lower.contains("forestry")) {
            return isLoaded("forestry");
        }
        if (lower.contains("extendedae") || lower.contains("expatternprovider")) {
            return isLoaded("extendedae") || isLoaded("expatternprovider");
        }
        if (lower.contains("advancedae")) {
            return isLoaded("advanced_ae") || isLoaded("advancedae");
        }
        return true;
    }

    private static boolean isLoaded(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
