package polar.ru.client.ui.clickgui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.MathHelper;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.input.KeyBoardUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.api.utils.scissor.ScissorUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.Setting;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.ui.MenuPanel;
import polar.ru.polar;

public class ClickGuiRenderer {
    private final ClickGuiState state;
    private final ClickGuiSettingRenderer settingRenderer;
    private final ClickGuiThemeSelector themeSelector;
    private final ClickGuiFiguraPanel figuraPanel;
    private final MenuPanel menuPanel;
    private final List<Region> regions = new ArrayList<>();
    private float contentHeight;

    private static final float W = 470.0f;
    private static final float H = 330.0f;
    private static final float PAD = 8.0f;
    private static final float GAP = 6.0f;
    private static final float PANEL_W = 145.0f;
    private static final float PANEL_H = 150.0f;
    private static final float HEADER_H = 24.0f;

    public ClickGuiRenderer(ClickGuiState state, ClickGuiSettingRenderer settingRenderer,
                            ClickGuiThemeSelector themeSelector, ClickGuiFiguraPanel figuraPanel,
                            MenuPanel menuPanel) {
        this.state = state;
        this.settingRenderer = settingRenderer;
        this.themeSelector = themeSelector;
        this.figuraPanel = figuraPanel;
        this.menuPanel = menuPanel;
    }

    public List<Region> getRegions() {
        return regions;
    }

    public float getContentHeight() {
        return contentHeight;
    }

    public void render(DrawContext context, int mouseX, int mouseY, Window window, float animationProgress) {
        if (window == null) return;

        regions.clear();
        float a = MathHelper.clamp(animationProgress, 0.0f, 1.0f);
        state.updatePosition(window, Module.ModuleCategory.values().length);

        float x = state.getX();
        float y = state.getY();
        int theme = getThemeColor();

        context.getMatrices().push();
        float cx = x + W / 2.0f;
        float cy = y + H / 2.0f;
        float scale = 0.94f + 0.06f * a;
        context.getMatrices().translate(cx, cy, 0.0f);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.getMatrices().translate(-cx, -cy, 0.0f);

        int bg = ColorUtils.rgba(10, 10, 14, (int)(242 * a));
        RenderUtils.drawRoundedRect(context.getMatrices(), x, y, W, H, 10.0f, bg);
        RenderUtils.drawRoundedRect(context.getMatrices(), x + 1, y + 1, W - 2, 2.0f, 1.0f,
                ColorUtils.setAlphaColor(theme, (int)(85 * a)));

        Font title = font(16);
        if (title != null) {
            title.draw(context.getMatrices(), "ClickGUI", x + 12, y + 9,
                    ColorUtils.setAlphaColor(-1, (int)(245 * a)));
        }
        Font sub = font(11);
        if (sub != null) {
            sub.draw(context.getMatrices(), "Modules", x + 78, y + 11,
                    ColorUtils.rgba(130, 130, 145, (int)(190 * a)));
        }

        Module.ModuleCategory[] categories = Module.ModuleCategory.values();
        float gridTop = y + 34.0f;
        for (int i = 0; i < categories.length; i++) {
            Module.ModuleCategory category = categories[i];
            int col = i % 3;
            int row = i / 3;
            float panelX = x + PAD + col * (PANEL_W + GAP);
            float panelY = gridTop + row * (PANEL_H + GAP);
            renderCategory(context, category, panelX, panelY, PANEL_W, PANEL_H, theme, a, mouseX, mouseY);
        }

        renderSearch(context, x, y, theme, a);

        context.getMatrices().pop();
    }

    private void renderCategory(DrawContext context, Module.ModuleCategory category,
                                float panelX, float panelY, float width, float height,
                                int theme, float a, int mouseX, int mouseY) {
        int panelBg = ColorUtils.rgba(18, 18, 24, (int)(238 * a));
        RenderUtils.drawRoundedRect(context.getMatrices(), panelX, panelY, width, height, 7.0f, panelBg);
        RenderUtils.drawRoundedRect(context.getMatrices(), panelX, panelY, 3.0f, height, 1.5f,
                ColorUtils.setAlphaColor(theme, (int)(145 * a)));

        Font icon = icons(10);
        Font f = font(12);
        String catName = category.getName();
        if (f != null) {
            f.draw(context.getMatrices(), catName, panelX + 9, panelY + 7,
                    ColorUtils.setAlphaColor(-1, (int)(235 * a)));
        }
        if (icon != null) {
            icon.drawCenteredString(context.getMatrices(), category.getIcons(),
                    panelX + width - 11, panelY + 11 - icon.getHeight() / 2.0f,
                    ColorUtils.setAlphaColor(theme, (int)(220 * a)));
        }

        regions.add(Region.of(Region.Type.CATEGORY, panelX, panelY, width, height).category(category));

        float innerX = panelX + 7;
        float innerY = panelY + HEADER_H;
        float innerW = width - 14;
        float innerH = height - HEADER_H - 5;
        List<Module> modules = state.getModules(category);

        float scroll = state.getScroll(category);
        float cursor = innerY + scroll;
        float total = 0.0f;

        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(panelX, innerY, width, innerH);

        for (Module module : modules) {
            float open = state.getOpenProgress(module);
            float settingsH = module.isOpen() ? settingRenderer.measureSettingsHeight(module, innerW) * open : 0.0f;
            float cardH = 20.0f + settingsH;
            float cardY = cursor;

            if (cardY + cardH >= innerY - 2 && cardY <= innerY + innerH + 2) {
                renderModule(context, module, innerX, cardY, innerW, cardH, theme, a, mouseX, mouseY, open);
            }

            cursor += cardH + 5.0f;
            total += cardH + 5.0f;
        }

        ScissorUtils.pop();
        state.clampScrollPixels(category, innerH, Math.max(0.0f, total - 5.0f));
    }

    private void renderModule(DrawContext context, Module module, float x, float y, float width,
                              float height, int theme, float a, double mouseX, double mouseY,
                              float openProgress) {
        boolean enabled = module.isEnable();
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20.0f;
        float hover = state.getHoverAnimation("ref_module_" + module.getName(), hovered).getValue();

        int cardColor = ColorUtils.rgba(24, 24, 31, (int)((235 + 12 * hover) * a));
        RenderUtils.drawRoundedRect(context.getMatrices(), x, y, width, height, 5.0f, cardColor);

        if (enabled) {
            RenderUtils.drawRoundedRect(context.getMatrices(), x, y, 2.0f, 20.0f, 1.0f,
                    ColorUtils.setAlphaColor(theme, (int)(230 * a)));
        }

        Font f = font(11);
        if (f != null) {
            int textColor = enabled
                    ? ColorUtils.setAlphaColor(-1, (int)(255 * a))
                    : ColorUtils.rgba(185, 185, 195, (int)(210 * a));
            f.draw(context.getMatrices(), translate(module.getName()), x + 7, y + 6, textColor);
        }

        Font bindFont = font(9);
        String bind = findModuleBindLabel(module);
        if (bindFont != null && bind != null && !bind.isEmpty()) {
            float bw = bindFont.getWidth(bind);
            bindFont.draw(context.getMatrices(), bind, x + width - bw - 6, y + 7,
                    ColorUtils.rgba(125, 125, 140, (int)(190 * a)));
        }

        regions.add(Region.of(Region.Type.MODULE_HEADER, x, y, width, 20).module(module));

        if (openProgress > 0.001f && height > 20.5f) {
            int alpha = (int)(255 * a * openProgress);
            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(x, y + 20, width, Math.max(0, height - 20));
            settingRenderer.render(context, module, x, y + 20, width, theme, alpha,
                    mouseX, mouseY, state, regions);
            ScissorUtils.pop();
        }
    }

    private void renderSearch(DrawContext context, float x, float y, int theme, float a) {
        float searchY = y + H - 24.0f;
        float searchX = x + 9.0f;
        float searchW = W - 18.0f;
        int searchBg = ColorUtils.rgba(19, 19, 25, (int)(245 * a));
        RenderUtils.drawRoundedRect(context.getMatrices(), searchX, searchY, searchW, 17.0f, 5.0f, searchBg);

        Font f = font(11);
        String text = state.getSearchText().isEmpty() ? "Search..." : state.getSearchText();
        if (f != null) {
            f.draw(context.getMatrices(), text, searchX + 8, searchY + 5,
                    state.getSearchText().isEmpty()
                            ? ColorUtils.rgba(120, 120, 135, (int)(190 * a))
                            : ColorUtils.setAlphaColor(-1, (int)(245 * a)));
        }
        regions.add(Region.of(Region.Type.SEARCH, searchX, searchY, searchW, 17));
    }

    private String findModuleBindLabel(Module module) {
        try {
            List<Setting> settings = module.getSettings();
            if (settings == null) return null;
            for (Setting s : settings) {
                if (s instanceof BindSetting bind) {
                    return state.toEnglish(KeyBoardUtils.getBindName(bind.getKey()));
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private int getThemeColor() {
        try {
            if (polar.INSTANCE != null && polar.INSTANCE.themeStorage != null
                    && !polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
                return polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
            }
        } catch (Throwable ignored) {}
        return ColorUtils.getThemeColor();
    }

    private String translate(String key) {
        if (polar.INSTANCE == null || polar.INSTANCE.localizationStorage == null) return key;
        return polar.INSTANCE.localizationStorage.translate(key);
    }

    private Font font(int size) {
        Font f = Fonts.getFont("moe3", size);
        return f != null ? f : Fonts.getFont("suisse", size);
    }

    private Font icons(int size) {
        Font f = Fonts.getFont("icon", size);
        return f != null ? f : font(size);
    }

    public static final class Region {
        public final Type type;
        public final float x, y, w, h;
        public Module module;
        public Setting setting;
        public String modeValue;
        public polar.ru.client.modules.settings.implement.BooleanSetting listEntry;
        public Module.ModuleCategory category;

        private Region(Type type, float x, float y, float w, float h) {
            this.type = type; this.x = x; this.y = y; this.w = w; this.h = h;
        }
        public static Region of(Type type, float x, float y, float w, float h) {
            return new Region(type, x, y, w, h);
        }
        public Region module(Module m) { this.module = m; return this; }
        public Region setting(Setting s) { this.setting = s; return this; }
        public Region modeValue(String v) { this.modeValue = v; return this; }
        public Region listEntry(polar.ru.client.modules.settings.implement.BooleanSetting e) { this.listEntry = e; return this; }
        public Region category(Module.ModuleCategory c) { this.category = c; return this; }
        public boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
        public enum Type {
            CATEGORY, MODULE_HEADER, TOGGLE, CHIP_MODE, CHIP_LIST, SLIDER, BIND, TEXT, SEARCH, TEXT_INPUT
        }
    }
}
