package polar.ru.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.client.modules.settings.implement.TextSetting;
import polar.ru.client.ui.MenuPanel;
import polar.ru.client.ui.clickgui.ClickGuiFiguraPanel;
import polar.ru.client.ui.clickgui.ClickGuiLayout;
import polar.ru.client.ui.clickgui.ClickGuiRenderer;
import polar.ru.client.ui.clickgui.ClickGuiSettingRenderer;
import polar.ru.client.ui.clickgui.ClickGuiState;
import polar.ru.client.ui.clickgui.ClickGuiThemeSelector;

public class ClickGuiScreen
extends Screen {
    private final ClickGuiState state = new ClickGuiState();
    private final ClickGuiSettingRenderer settingRenderer = new ClickGuiSettingRenderer();
    private final ClickGuiThemeSelector themeSelector = new ClickGuiThemeSelector();
    private final ClickGuiFiguraPanel figuraPanel = new ClickGuiFiguraPanel();
    private final MenuPanel menuPanel = new MenuPanel();
    private final ClickGuiRenderer renderer = new ClickGuiRenderer(this.state, this.settingRenderer, this.themeSelector, this.figuraPanel, this.menuPanel);
    private final AnimationUtils openAnim = new AnimationUtils(0.0f, 8.0f, Easings.CUBIC_OUT);
    private boolean closing = false;

    public ClickGuiScreen() {
        super((Text)Text.literal((String)"ClickGui"));
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.closing) {
            this.openAnim.update(0.0f);
            if (this.openAnim.getValue() <= 0.01f) {
                this.closing = false;
                super.close();
                return;
            }
        } else {
            this.openAnim.update(1.0f);
        }
        this.state.updatePosition(this.client.getWindow(), 0);
        this.renderer.render(context, mouseX, mouseY, this.client.getWindow(), this.openAnim.getValue());
    }

    public void close() {
        if (!this.closing) {
            this.closing = true;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float contentTop = ClickGuiLayout.contentTop(this.state.getY());
        float contentBottom = ClickGuiLayout.contentBottom(this.state.getY());
        for (int i2 = this.renderer.getRegions().size() - 1; i2 >= 0; --i2) {
            ClickGuiRenderer.Region r2 = this.renderer.getRegions().get(i2);
            if (!r2.contains(mouseX, mouseY)) continue;
            if (r2.type != ClickGuiRenderer.Region.Type.CATEGORY && r2.type != ClickGuiRenderer.Region.Type.SEARCH) {
                if (mouseY < (double)contentTop || mouseY > (double)contentBottom) continue;
            }
            switch (r2.type) {
                case CATEGORY: {
                    this.state.setSelectedCategory(r2.category);
                    this.state.setScrollTarget(r2.category, 0.0f);
                    return true;
                }
                case SEARCH: {
                    this.state.setSearchActive(true);
                    this.state.setEditingTextSetting(null);
                    return true;
                }
                case MODULE_HEADER: {
                    if (button == 0) {
                        this.toggleModule(r2.module);
                    } else if (button == 1) {
                        this.setModuleOpen(r2.module, !r2.module.isOpen());
                    }
                    return true;
                }
                case TOGGLE: {
                    this.toggleBoolean((BooleanSetting)r2.setting);
                    return true;
                }
                case CHIP_MODE: {
                    this.setMode((ModeSetting)r2.setting, r2.modeValue);
                    return true;
                }
                case CHIP_LIST: {
                    this.toggleBoolean(r2.listEntry);
                    return true;
                }
                case SLIDER: {
                    FloatSetting s2 = (FloatSetting)r2.setting;
                    this.state.setActiveSlider(s2);
                    this.state.beginSliderDrag(s2, mouseX);
                    s2.setValue(this.state.getSliderValue(s2, r2.x, mouseX, r2.w));
                    return true;
                }
                case BIND: {
                    this.state.setBindingSetting((BindSetting)r2.setting);
                    return true;
                }
                case TEXT: {
                    this.state.setEditingTextSetting((TextSetting)r2.setting);
                    this.state.setSearchActive(false);
                    return true;
                }
            }
        }
        this.state.setSearchActive(false);
        this.state.setEditingTextSetting(null);
        if (this.state.getBindingSetting() != null) {
            this.state.setBindingSetting(null);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.state.getActiveSlider() != null) {
            this.state.endSliderDrag(this.state.getActiveSlider());
            this.state.setActiveSlider(null);
        }
        this.state.stopSearchSelection();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Module.ModuleCategory c2 = this.state.getSelectedCategory();
        float viewH = ClickGuiLayout.contentBottom(this.state.getY()) - ClickGuiLayout.contentTop(this.state.getY());
        this.state.addScrollPixels(c2, (float)verticalAmount * 22.0f, viewH, this.renderer.getContentHeight());
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.state.getBindingSetting() != null) {
            if (keyCode == 256) {
                this.state.setBindingSetting(null);
            } else if (keyCode == 261 || keyCode == 259) {
                this.setBindKey(this.state.getBindingSetting(), 0);
                this.state.setBindingSetting(null);
            } else {
                this.setBindKey(this.state.getBindingSetting(), keyCode);
                this.state.setBindingSetting(null);
            }
            return true;
        }
        if (this.state.getEditingTextSetting() != null) {
            String v2;
            TextSetting s2 = this.state.getEditingTextSetting();
            if (keyCode == 256 || keyCode == 257) {
                this.state.setEditingTextSetting(null);
            } else if (keyCode == 259 && (v2 = s2.get()) != null && !v2.isEmpty()) {
                this.setText(s2, v2.substring(0, v2.length() - 1));
            }
            return true;
        }
        if (this.state.isSearchActive()) {
            if (keyCode == 256) {
                this.state.setSearchActive(false);
                return true;
            }
            if (keyCode == 259) {
                this.state.removeLastSearchChar();
                return true;
            }
            if (keyCode == 257) {
                this.state.setSearchActive(false);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        if (this.state.getEditingTextSetting() != null) {
            TextSetting s2 = this.state.getEditingTextSetting();
            String v2 = s2.get() == null ? "" : s2.get();
            this.setText(s2, v2 + chr);
            return true;
        }
        if (this.state.isSearchActive()) {
            this.state.appendSearchChar(chr);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    public boolean shouldPause() {
        return false;
    }

    private void toggleModule(Module m2) {
        m2.setEnabled(!m2.isEnable());
    }

    private void setModuleOpen(Module m2, boolean open) {
        m2.setOpen(open);
    }

    private void toggleBoolean(BooleanSetting s2) {
        s2.setState(!s2.isState());
    }

    private void setMode(ModeSetting s2, String mode) {
        s2.set(mode);
    }

    private void setBindKey(BindSetting s2, int key) {
        s2.setKey(key);
    }

    private void setText(TextSetting s2, String value) {
        s2.setText(value);
    }
}

