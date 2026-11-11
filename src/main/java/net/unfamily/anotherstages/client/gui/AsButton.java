package net.unfamily.anotherstages.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * Flat modern button matching Another Config Manager styling (no ACM dependency).
 */
public final class AsButton extends AbstractWidget {
    public static final int HEIGHT = 20;
    public static final int ICON_SIZE = 16;

    private static final int BG = 0xFF2C2C32;
    private static final int BG_HOVER = 0xFF3A3A44;
    private static final int BG_DISABLED = 0xFF1E1E22;
    private static final int BORDER = 0xFF5A5A68;
    private static final int BORDER_HOVER = 0xFF8A8A9A;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int TEXT_DISABLED = 0xFF777788;

    private final @Nullable Runnable onPress;
    /** Left = +1, right = -1 for multi-mode cycle buttons. */
    private final @Nullable IntConsumer onCycle;
    private final boolean iconStyle;
    private @Nullable Supplier<Boolean> activeWhen;

    private AsButton(int width, int height, Component message, @Nullable Runnable onPress,
                     @Nullable IntConsumer onCycle, boolean iconStyle) {
        super(0, 0, width, height, message);
        this.onPress = onPress;
        this.onCycle = onCycle;
        this.iconStyle = iconStyle;
    }

    public static AsButton text(int width, Component message, Runnable onPress) {
        return new AsButton(width, HEIGHT, message, onPress, null, false);
    }

    public static AsButton text(int width, int height, Component message, Runnable onPress) {
        return new AsButton(width, height, message, onPress, null, false);
    }

    public static AsButton text(int width, int height, Component message, Runnable onPress, @Nullable Component tooltip) {
        AsButton button = text(width, height, message, onPress);
        if (tooltip != null) {
            button.setTooltip(Tooltip.create(tooltip));
        }
        return button;
    }

    public static AsButton text(int width, Component message, Runnable onPress, @Nullable Component tooltip) {
        AsButton button = text(width, message, onPress);
        if (tooltip != null) {
            button.setTooltip(Tooltip.create(tooltip));
        }
        return button;
    }

    public static AsButton cycle(int width, int height, Component message, IntConsumer delta) {
        return new AsButton(width, height, message, null, delta, false);
    }

    public static AsButton icon(Component glyph, Component tooltip, Runnable onPress) {
        AsButton button = new AsButton(ICON_SIZE, ICON_SIZE, glyph, onPress, null, true);
        button.setTooltip(Tooltip.create(tooltip));
        return button;
    }

    public AsButton activeWhen(Supplier<Boolean> activeWhen) {
        this.activeWhen = activeWhen;
        return this;
    }

    public void refreshActive() {
        if (activeWhen != null) {
            this.active = Boolean.TRUE.equals(activeWhen.get());
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (activeWhen != null) {
            refreshActive();
        }
        if (!this.active) {
            return;
        }
        if (onCycle != null) {
            AsUi.playClick();
            onCycle.accept(1);
            return;
        }
        if (onPress == null) {
            return;
        }
        AsUi.playClick();
        onPress.run();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeWhen != null) {
            refreshActive();
        }
        if (!this.active || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (onCycle != null) {
            if (button == 0) {
                AsUi.playClick();
                onCycle.accept(1);
                return true;
            }
            if (button == 1) {
                AsUi.playClick();
                onCycle.accept(-1);
                return true;
            }
            return false;
        }
        if (button != 0 || onPress == null) {
            return false;
        }
        AsUi.playClick();
        onPress.run();
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (activeWhen != null) {
            refreshActive();
        }
        int bg = !this.active ? BG_DISABLED : (this.isHoveredOrFocused() ? BG_HOVER : BG);
        int border = !this.active ? 0xFF3A3A44 : (this.isHoveredOrFocused() ? BORDER_HOVER : BORDER);
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
        Component message = getMessage();
        int color = this.active ? TEXT : TEXT_DISABLED;
        int tw = font.width(message);
        int tx = x0 + (getWidth() - tw) / 2;
        int ty = y0 + (getHeight() - 8) / 2;
        if (iconStyle) {
            ty = y0 + (getHeight() - 9) / 2;
        }
        graphics.drawString(font, message, tx, ty, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
