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
import polar.ru.client.modules.settings.*;
import polar.ru.client.modules.settings.implement.*;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiDropdownModule {

    private final Module module;
    private final List<ClickGuiDropdownSetting> settingComponents = new ArrayList<>();
    private float x, y;
    private float width;
    private float height = 20f;
    private boolean open = false;
    private final AnimationUtils openAnim = new AnimationUtils(0f, 10f, Easings.CUBIC_OUT);
    private boolean bindMode = false;

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
                    settingComponents.add(new ClickGuiDropdownBind((BindSetting) setting, this));
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
        openAnim.update(open ? 1f : 0f);
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        float totalSettingsHeight = 0;
        for (ClickGuiDropdownSetting comp : settingComponents) {
            if (comp.isVisible()) totalSettingsHeight += comp.getHeight();
        }
        return height + totalSettingsHeight * openAnim.getValue();
    }

    public void render(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrices = context.getMatrices();
        boolean isEnabled = module.isEnable();

        int bgColor = ColorUtils.rgba(25, 26, 40, 165);
        RenderUtils.drawRoundedRect(matrices, x + 2, y, width - 5, height, 5f, bgColor);

        Font font = Fonts.getFont("moe3", 7);
        if (font != null) {
            String name = module.getName();
            int textColor = isEnabled ? ColorUtils.rgb(255,255,255) : ColorUtils.rgb(161, 164, 177);
            font.draw(matrices, name, x + 6, y + 6.5f, textColor);
        }

        boolean hasSettings = !settingComponents.isEmpty();
        if (hasSettings) {
            Font iconFont = Fonts.getFont("icon", 6);
            if (iconFont != null) {
                String icon = open ? "C" : "B";
                float iconX = x + width - 6 - iconFont.getWidth(icon, 6);
                float iconY = y + 6f + 1;
                iconFont.draw(matrices, icon, iconX, iconY, ColorUtils.rgb(161, 164, 177));
            }
            if (bindMode) {
                Font bindFont = Fonts.getFont("moe3", 6);
                if (bindFont != null) {
                    String bindText = module.getKey() == 0 ? "..." : String.valueOf(module.getKey());
                    float bindX = x + width - 6 - bindFont.getWidth(bindText, 6);
                    bindFont.draw(matrices, bindText, bindX, y + 6f + 1, ColorUtils.rgb(161, 164, 177));
                }
            }
        }

        if (openAnim.getValue() > 0.01f) {
            float settingsY = y + height;
            float settingsHeight = 0;
            for (ClickGuiDropdownSetting comp : settingComponents) {
                if (comp.isVisible()) {
                    comp.setPosition(x, settingsY + settingsHeight);
                    comp.setWidth(width - 5);
                    comp.render(context, mouseX, mouseY);
                    settingsHeight += comp.getHeight();
                }
            }
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            if (button == 0) {
                module.toggle();
            } else if (button == 1) {
                open = !open;
                module.setOpen(open);
                openAnim.update(open ? 1f : 0f);
            } else if (button == 2) {
                bindMode = !bindMode;
            }
            return;
        }
        if (openAnim.getValue() > 0.01f) {
            for (ClickGuiDropdownSetting comp : settingComponents) {
                if (comp.isVisible()) {
                    comp.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (openAnim.getValue() > 0.01f) {
            for (ClickGuiDropdownSetting comp : settingComponents) {
                if (comp.isVisible()) {
                    comp.mouseReleased(mouseX, mouseY, button);
                }
            }
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindMode) {
            if (keyCode == 256) {
                bindMode = false;
            } else if (keyCode == 261 || keyCode == 259) {
                module.setKey(0);
                bindMode = false;
            } else {
                module.setKey(keyCode);
                bindMode = false;
            }
            return;
        }
        if (openAnim.getValue() > 0.01f) {
            for (ClickGuiDropdownSetting comp : settingComponents) {
                if (comp.isVisible()) {
                    comp.keyPressed(keyCode, scanCode, modifiers);
                }
            }
        }
    }

    public void charTyped(char chr, int modifiers) {
        if (openAnim.getValue() > 0.01f) {
            for (ClickGuiDropdownSetting comp : settingComponents) {
                if (comp.isVisible()) {
                    comp.charTyped(chr, modifiers);
                }
            }
        }
    }

    public boolean isBindMode() {
        return bindMode;
    }
}