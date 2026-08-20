package polar.ru.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.input.KeyBoardUtils;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.Setting;
import polar.ru.client.modules.settings.implement.*;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiDropdownModule {
    private final Module module;
    private final List<ClickGuiDropdownSetting> settings = new ArrayList<>();
    private float x, y, width;
    private static final float HEADER_HEIGHT = 20f;
    private boolean open;
    private boolean binding;
    private final AnimationUtils openAnimation = new AnimationUtils(0f, 10f, Easings.CUBIC_OUT);

    public ClickGuiDropdownModule(Module module) {
        this.module = module;
        for (Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting s) settings.add(new ClickGuiDropdownBoolean(s));
            else if (setting instanceof FloatSetting s) settings.add(new ClickGuiDropdownSlider(s));
            else if (setting instanceof ModeSetting s) settings.add(new ClickGuiDropdownMode(s));
            else if (setting instanceof ListSetting s) settings.add(new ClickGuiDropdownList(s));
            else if (setting instanceof TextSetting s) settings.add(new ClickGuiDropdownText(s));
            else if (setting instanceof BindSetting s) settings.add(new ClickGuiDropdownBind(s, this));
        }
        open = module.isOpen();
        openAnimation.update(open ? 1f : 0f);
    }

    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public void setWidth(float width) { this.width = width; }

    public float getHeight() {
        float total = HEADER_HEIGHT;
        for (ClickGuiDropdownSetting setting : settings) {
            if (setting.isVisible()) total += setting.getHeight();
        }
        float progress = openAnimation.getValue();
        return HEADER_HEIGHT + (total - HEADER_HEIGHT) * progress;
    }

    public void render(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrices = context.getMatrices();
        openAnimation.update(open ? 1f : 0f);

        int background = ColorUtils.rgba(25, 26, 40, 165);
        RenderUtils.drawRoundedRect(matrices, x + 2, y, width - 5, HEADER_HEIGHT, 5f, background);

        Font font = Fonts.getFont("moe3", 7);
        if (font != null) {
            int textColor = module.isEnable()
                    ? ColorUtils.rgb(255, 255, 255)
                    : ColorUtils.rgb(161, 164, 177);
            font.draw(matrices, module.getDisplayName(), x + 6, y + 6.5f, textColor);
        }

        boolean hasSettings = settings.stream().anyMatch(ClickGuiDropdownSetting::isVisible);
        if (hasSettings) {
            Font icon = Fonts.getFont("icon", 6);
            if (icon != null && !binding) {
                String glyph = open ? "C" : "B";
                icon.draw(matrices, glyph, x + width - 6 - icon.getWidth(glyph),
                        y + 6f + 1f, ColorUtils.rgb(161, 164, 177));
            }
        }

        if (binding) {
            Font bindFont = Fonts.getFont("moe3", 5);
            if (bindFont != null) {
                String text = "...";
                bindFont.draw(matrices, text, x + width - 6 - bindFont.getWidth(text),
                        y + 6.5f, ColorUtils.rgb(161, 164, 177));
            }
        }

        if (openAnimation.getValue() <= 0.001f) return;

        float offset = HEADER_HEIGHT;
        for (ClickGuiDropdownSetting setting : settings) {
            if (!setting.isVisible()) continue;
            setting.setPosition(x, y + offset);
            setting.setWidth(width - 5);
            setting.render(context, mouseX, mouseY);
            offset += setting.getHeight();
        }
    }

    private boolean headerHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= y && mouseY <= y + HEADER_HEIGHT;
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (binding) {
            if (button >= 0) {
                module.setKey(KeyBoardUtils.createMouseBind(button));
                binding = false;
            }
            return;
        }

        if (headerHovered(mouseX, mouseY)) {
            if (button == 0) {
                module.toggle();
            } else if (button == 1) {
                open = !open;
                module.setOpen(open);
            } else if (button == 2) {
                binding = true;
            }
            return;
        }

        if (openAnimation.getValue() > 0.01f) {
            for (ClickGuiDropdownSetting setting : settings) {
                if (setting.isVisible()) setting.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (ClickGuiDropdownSetting setting : settings) {
            if (setting.isVisible()) setting.mouseReleased(mouseX, mouseY, button);
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE ||
                keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                module.setKey(-1);
            } else {
                module.setKey(keyCode);
            }
            binding = false;
            return;
        }
        for (ClickGuiDropdownSetting setting : settings) {
            if (setting.isVisible()) setting.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public void charTyped(char chr, int modifiers) {
        for (ClickGuiDropdownSetting setting : settings) {
            if (setting.isVisible()) setting.charTyped(chr, modifiers);
        }
    }

    public boolean isBindMode() { return binding; }
    public void setBindMode(boolean value) { binding = value; }
}
