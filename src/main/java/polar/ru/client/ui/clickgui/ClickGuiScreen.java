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
import polar.ru.client.ui.clickgui.*;

/**
 * Адаптированный ClickGuiScreen — визуально и по логике открытия/закрытия
 * соответствует MenuScreen (затемнение, масштабирование), но использует только
 * существующие зависимости polar.ru. Все оригинальные функции сохранены.
 */
public class ClickGuiScreen extends Screen {

    private final ClickGuiState state = new ClickGuiState();
    private final ClickGuiSettingRenderer settingRenderer = new ClickGuiSettingRenderer();
    private final ClickGuiThemeSelector themeSelector = new ClickGuiThemeSelector();
    private final ClickGuiFiguraPanel figuraPanel = new ClickGuiFiguraPanel();
    private final MenuPanel menuPanel = new MenuPanel();
    private final ClickGuiRenderer renderer = new ClickGuiRenderer(state, settingRenderer, themeSelector, figuraPanel, menuPanel);
    private final AnimationUtils openAnim = new AnimationUtils(0.0f, 8.0f, Easings.CUBIC_OUT);
    private boolean closing = false;

    public ClickGuiScreen() {
        super(Text.literal("ClickGui"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Логика закрытия с анимацией
        if (closing) {
            openAnim.update(0.0f);
            if (openAnim.getValue() <= 0.01f) {
                closing = false;
                super.close();
                return;
            }
        } else {
            openAnim.update(1.0f);
        }

        var window = client.getWindow();
        int screenWidth = window.getScaledWidth();
        int screenHeight = window.getScaledHeight();

        // Затемнение фона (как в MenuScreen)
        int overlayAlpha = (int) (80 * openAnim.getValue());
        context.fill(0, 0, screenWidth, screenHeight, (overlayAlpha << 24) | 0x000000);

        // Обновляем позицию (оригинальный вызов)
        state.updatePosition(window, 0);

        // Масштабирование содержимого относительно центра экрана
        context.getMatrices().push();
        float scale = openAnim.getValue();
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        context.getMatrices().translate(centerX, centerY, 0);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-centerX, -centerY, 0);

        // Отрисовка самого ClickGui (рендерер сам рисует фон и элементы)
        renderer.render(context, mouseX, mouseY, window, openAnim.getValue());

        context.getMatrices().pop();
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
        }
    }

    // Все остальные методы (обработка событий) остаются без изменений
    // --------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float contentTop = ClickGuiLayout.contentTop(state.getY());
        float contentBottom = ClickGuiLayout.contentBottom(state.getY());
        for (int i = renderer.getRegions().size() - 1; i >= 0; --i) {
            ClickGuiRenderer.Region r = renderer.getRegions().get(i);
            if (!r.contains(mouseX, mouseY)) continue;
            if (r.type != ClickGuiRenderer.Region.Type.CATEGORY && r.type != ClickGuiRenderer.Region.Type.SEARCH) {
                if (mouseY < contentTop || mouseY > contentBottom) continue;
            }
            switch (r.type) {
                case CATEGORY:
                    state.setSelectedCategory(r.category);
                    state.setScrollTarget(r.category, 0.0f);
                    return true;
                case SEARCH:
                    state.setSearchActive(true);
                    state.setEditingTextSetting(null);
                    return true;
                case MODULE_HEADER:
                    if (button == 0) toggleModule(r.module);
                    else if (button == 1) setModuleOpen(r.module, !r.module.isOpen());
                    return true;
                case TOGGLE:
                    toggleBoolean((BooleanSetting) r.setting);
                    return true;
                case CHIP_MODE:
                    setMode((ModeSetting) r.setting, r.modeValue);
                    return true;
                case CHIP_LIST:
                    toggleBoolean(r.listEntry);
                    return true;
                case SLIDER:
                    FloatSetting s = (FloatSetting) r.setting;
                    state.setActiveSlider(s);
                    state.beginSliderDrag(s, mouseX);
                    s.setValue(state.getSliderValue(s, r.x, mouseX, r.w));
                    return true;
                case BIND:
                    state.setBindingSetting((BindSetting) r.setting);
                    return true;
                case TEXT:
                    state.setEditingTextSetting((TextSetting) r.setting);
                    state.setSearchActive(false);
                    return true;
            }
        }
        state.setSearchActive(false);
        state.setEditingTextSetting(null);
        if (state.getBindingSetting() != null) state.setBindingSetting(null);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (state.getActiveSlider() != null) {
            state.endSliderDrag(state.getActiveSlider());
            state.setActiveSlider(null);
        }
        state.stopSearchSelection();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Module.ModuleCategory c = state.getSelectedCategory();
        float viewH = ClickGuiLayout.contentBottom(state.getY()) - ClickGuiLayout.contentTop(state.getY());
        state.addScrollPixels(c, (float) verticalAmount * 22.0f, viewH, renderer.getContentHeight());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (state.getBindingSetting() != null) {
            if (keyCode == 256) state.setBindingSetting(null);
            else if (keyCode == 261 || keyCode == 259) {
                setBindKey(state.getBindingSetting(), 0);
                state.setBindingSetting(null);
            } else {
                setBindKey(state.getBindingSetting(), keyCode);
                state.setBindingSetting(null);
            }
            return true;
        }
        if (state.getEditingTextSetting() != null) {
            TextSetting s = state.getEditingTextSetting();
            if (keyCode == 256 || keyCode == 257) {
                state.setEditingTextSetting(null);
            } else if (keyCode == 259) {
                String v = s.get();
                if (v != null && !v.isEmpty()) setText(s, v.substring(0, v.length() - 1));
            }
            return true;
        }
        if (state.isSearchActive()) {
            if (keyCode == 256) {
                state.setSearchActive(false);
                return true;
            }
            if (keyCode == 259) {
                state.removeLastSearchChar();
                return true;
            }
            if (keyCode == 257) {
                state.setSearchActive(false);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (state.getEditingTextSetting() != null) {
            TextSetting s = state.getEditingTextSetting();
            String v = s.get() == null ? "" : s.get();
            setText(s, v + chr);
            return true;
        }
        if (state.isSearchActive()) {
            state.appendSearchChar(chr);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // Вспомогательные методы (оригинальные)
    private void toggleModule(Module m) { m.setEnabled(!m.isEnable()); }
    private void setModuleOpen(Module m, boolean open) { m.setOpen(open); }
    private void toggleBoolean(BooleanSetting s) { s.setState(!s.isState()); }
    private void setMode(ModeSetting s, String mode) { s.set(mode); }
    private void setBindKey(BindSetting s, int key) { s.setKey(key); }
    private void setText(TextSetting s, String value) { s.setText(value); }
}