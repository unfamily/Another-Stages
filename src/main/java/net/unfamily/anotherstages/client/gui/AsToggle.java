package net.unfamily.anotherstages.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * ACM-style On/Off chip (40×16): green when on, grey when off.
 */
public final class AsToggle extends AbstractWidget {
    public static final int WIDTH = 40;
    public static final int HEIGHT = 16;

    private static final int BG_ON = 0xFF3A8F4A;
    private static final int BG_OFF = 0xFF555555;
    private static final int BORDER_ON = 0xFF6FCF7F;
    private static final int BORDER_OFF = 0xFF888888;

    private final BooleanSupplier value;
    private final Consumer<Boolean> onToggle;

    public AsToggle(BooleanSupplier value, Consumer<Boolean> onToggle) {
        super(0, 0, WIDTH, HEIGHT, Component.empty());
        this.value = value;
        this.onToggle = onToggle;
    }

    public boolean isOn() {
        return Boolean.TRUE.equals(value.getAsBoolean());
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        AsUi.playClick();
        onToggle.accept(!isOn());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean on = isOn();
        int bg = on ? BG_ON : BG_OFF;
        int border = on ? BORDER_ON : BORDER_OFF;
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + getWidth();
        int y1 = y0 + getHeight();
        graphics.fill(x0, y0, x1, y1, bg);
        graphics.fill(x0, y0, x1, y0 + 1, border);
        graphics.fill(x0, y1 - 1, x1, y1, border);
        graphics.fill(x0, y0, x0 + 1, y1, border);
        graphics.fill(x1 - 1, y0, x1, y1, border);

        Font font = Minecraft.getInstance().font;
        String label = Component.translatable(on
                ? "screen.another_stages.stage_generator.on"
                : "screen.another_stages.stage_generator.off").getString();
        int tw = font.width(label);
        graphics.text(font, label, x0 + (getWidth() - tw) / 2, y0 + 4, 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
