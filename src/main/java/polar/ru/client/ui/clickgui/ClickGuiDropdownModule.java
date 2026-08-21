package polar.ru.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.Setting;
import polar.ru.client.modules.settings.implement.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClickGuiDropdownModule {

    private final Module module;
    private final List<ClickGuiDropdownSetting> settingComponents = new ArrayList<>();
    private float x, y, width;
    private final float baseHeight = 30.0f;
    private boolean open = false;
    private final AnimationUtils openAnim = new AnimationUtils(0f, 8f, Easings.CUBIC_OUT);
    private final AnimationUtils hoverAnim = new AnimationUtils(0f, 10f, Easings.CUBIC_OUT);
    private final AnimationUtils enableAnim = new AnimationUtils(0f, 7f, Easings.CUBIC_OUT);
    private boolean bindMode = false;
    private BindSetting bindSetting = null;

    public ClickGuiDropdownModule(Module module) {
        this.module = module;
        List<Setting> settings = module.getSettings();
        if (settings != null) {
            for (Setting setting : settings) {
                if (setting instanceof BooleanSetting) {
                    settingComponents.add(new ClickGuiDropdownBoolean((BooleanSetting) setting));
                } else if (setting instanceof FloatSetting) {
                    settingComponents.add(new ClickGuiDropdownSlider((FloatSetting) setting));
                } else if (setting instanceof BindSetting) {
                    bindSetting = (BindSetting) setting;
                    settingComponents.add(new ClickGuiDropdownBind(bindSetting, this));
                } else if (setting instanceof ModeSetting) {
                    settingComponents.add(new ClickGuiDropdownMode((ModeSetting) setting));
                } else if (setting instanceof ListSetting) {
                    settingComponents.add(new ClickGuiDropdownList((ListSetting) setting));
                } else if (setting instanceof TextSetting) {
                    settingComponents.add(new ClickGuiDropdownText((TextSetting) setting));
                }
            }
        }
        open = module.isOpen();
        openAnim.setValue(open ? 1f : 0f);
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        float totalSettingsHeight = 0.0f;
        for (ClickGuiDropdownSetting comp : settingComponents) {
            if (comp.isVisible()) totalSettingsHeight += comp.getHeight();
        }
        return baseHeight + totalSettingsHeight * openAnim.getValue();
    }

    public boolean matchesSearch(String query) {
        if (query == null || query.isEmpty()) return true;
        String q = query.toLowerCase(Locale.ROOT);
        return module.getName().toLowerCase(Locale.ROOT).contains(q)
                || module.getDisplayName().toLowerCase(Locale.ROOT).contains(q)
                || module.getDisplayDescription().toLowerCase(Locale.ROOT).contains(q);
    }

    public void render(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrices = context.getMatrices();
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + baseHeight;

        open = module.isOpen();
        openAnim.update(open ? 1.0f : 0.0f);
        hoverAnim.update(hovered ? 1.0f : 0.0f);
        enableAnim.update(module.isEnable() ? 1.0f : 0.0f);

        float openProgress = openAnim.getValue();
        float hoverProgress = hoverAnim.getValue();
        float enabledProgress = enableAnim.getValue();

        int base = ColorUtils.rgba(22, 22, 25, 205);
        int hover = ColorUtils.rgba(31, 31, 35, 220);
        int enabled = ColorUtils.getThemeColor();
        int bg = ColorUtils.interpolate(base, hover, hoverProgress);
        bg = ColorUtils.interpolate(bg, ColorUtils.multAlpha(enabled, 0.18f), enabledProgress * 0.55f);

        RenderUtils.drawRoundedRect(matrices, x, y, width, baseHeight, 5.0f, bg);
        if (hoverProgress > 0.01f) {
            RenderUtils.drawRoundedRect(matrices, x + 0.5f, y + 0.5f, width - 1.0f, baseHeight - 1.0f,
                    4.5f, ColorUtils.multAlpha(ColorUtils.rgba(130, 130, 135, 90), hoverProgress));
        }

        Font font = Fonts.getFont("suisse", 12);
        if (font != null) {
            String name = module.getDisplayName();
            float maxNameWidth = width - 70.0f;
            if (font.getWidth(name) > maxNameWidth) {
                name = truncate(font, name, maxNameWidth);
            }
            int textColor = ColorUtils.interpolate(ColorUtils.rgb(150, 150, 155), ColorUtils.rgb(245, 245, 247), enabledProgress);
            font.draw(matrices, name, x + 9.0f, y + 9.0f, textColor);

            String stateText = module.isEnable() ? "ON" : "OFF";
            int stateColor = module.isEnable() ? ColorUtils.rgb(232, 232, 235) : ColorUtils.rgb(125, 125, 132);
            float stateX = x + width - 9.0f - font.getWidth(stateText);
            font.draw(matrices, stateText, stateX, y + 9.0f, stateColor);
        }

        // Small divider under the header when settings are expanded.
        if (openProgress > 0.01f && !settingComponents.isEmpty()) {
            RenderUtils.drawRoundedRect(matrices, x + 8.0f, y + baseHeight - 1.0f, width - 16.0f, 1.0f, 0.5f,
                    ColorUtils.rgba(75, 75, 80, (int) (90 * openProgress)));
        }

        if (openProgress > 0.01f) {
            float settingsHeight = 0.0f;
            for (ClickGuiDropdownSetting comp : settingComponents) {
                if (comp.isVisible()) settingsHeight += comp.getHeight();
            }
            if (settingsHeight > 0.0f) {
                RenderUtils.drawRoundedRect(matrices, x + 1.0f, y + baseHeight, width - 2.0f, settingsHeight * openProgress,
                        0.0f, ColorUtils.rgba(14, 14, 17, 110));
                float settingsY = y + baseHeight;
                for (ClickGuiDropdownSetting comp : settingComponents) {
                    if (!comp.isVisible()) continue;
                    comp.setPosition(x + 4.0f, settingsY);
                    comp.setWidth(width - 8.0f);
                    comp.render(context, mouseX, mouseY);
                    settingsY += comp.getHeight();
                }
            }
        }
    }

    private String truncate(Font font, String text, float maxWidth) {
        if (text == null) return "";
        String suffix = "...";
        for (int i = text.length(); i > 0; i--) {
            String candidate = text.substring(0, i) + suffix;
            if (font.getWidth(candidate) <= maxWidth) return candidate;
        }
        return suffix;
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + baseHeight) {
            if (button == 0) {
                module.toggle();
            } else if (button == 1) {
                open = !open;
                module.setOpen(open);
            } else if (button == 2) {
                bindMode = !bindMode;
            }
            return;
        }
        if (openAnim.getValue() > 0.01f) {
            for (ClickGuiDropdownSetting comp : settingComponents) {
                if (comp.isVisible()) comp.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (openAnim.getValue() > 0.01f) {
            for (ClickGuiDropdownSetting comp : settingComponents) {
                if (comp.isVisible()) comp.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindMode) {
            if (keyCode == 256) {
                bindMode = false;
            } else if (keyCode == 261 || keyCode == 259) {
                if (bindSetting != null) bindSetting.setKey(-1);
                bindMode = false;
            } else {
                if (bindSetting != null) bindSetting.setKey(keyCode);
                bindMode = false;
            }
            return;
        }
        if (openAnim.getValue() > 0.01f) {
            for (ClickGuiDropdownSetting comp : settingComponents) {
                if (comp.isVisible()) comp.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    public void charTyped(char chr, int modifiers) {
        if (openAnim.getValue() > 0.01f) {
            for (ClickGuiDropdownSetting comp : settingComponents) {
                if (comp.isVisible()) comp.charTyped(chr, modifiers);
            }
        }
    }

    public boolean isBindMode() {
        return bindMode;
    }

    public void setBindMode(boolean bindMode) {
        this.bindMode = bindMode;
    }
}
