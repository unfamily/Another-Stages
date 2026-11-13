package net.unfamily.anotherstages.mixin.compat.ae2;

import appeng.client.gui.AEBaseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AEBaseScreen.class, remap = false)
public interface AeBaseScreenTextInvoker {
    @Invoker(value = "setTextContent", remap = false)
    void anotherStages$invokeSetTextContent(String id, Component content);
}
