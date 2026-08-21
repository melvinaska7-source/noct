package polar.ru.client.ui.clickgui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
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
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.ui.MenuPanel;
import polar.ru.client.ui.clickgui.ClickGuiFiguraPanel;
import polar.ru.client.ui.clickgui.ClickGuiLayout;
import polar.ru.client.ui.clickgui.ClickGuiSettingRenderer;
import polar.ru.client.ui.clickgui.ClickGuiState;
import polar.ru.client.ui.clickgui.ClickGuiThemeSelector;
import polar.ru.polar;

public class ClickGuiRenderer {
    private static final Identifier POLAR_LOGO = Identifier.of((String)"polar", (String)"polar.png");
    private final ClickGuiState state;
    private final ClickGuiSettingRenderer settingRenderer;
    private final ClickGuiThemeSelector themeSelector;
    private final ClickGuiFiguraPanel figuraPanel;
    private final MenuPanel menuPanel;
    private final List<Region> regions = new ArrayList<Region>();
    private float contentHeight;

    public ClickGuiRenderer(ClickGuiState state, ClickGuiSettingRenderer settingRenderer, ClickGuiThemeSelector themeSelector, ClickGuiFiguraPanel figuraPanel, MenuPanel menuPanel) {
        this.state = state;
        this.settingRenderer = settingRenderer;
        this.themeSelector = themeSelector;
        this.figuraPanel = figuraPanel;
        this.menuPanel = menuPanel;
    }

    public List<Region> getRegions() {
        return this.regions;
    }

    public float getContentHeight() {
        return this.contentHeight;
    }

    public void render(DrawContext context, int mouseX, int mouseY, Window window, float animationProgress) {
        if (window == null) {
            return;
        }
        this.regions.clear();
        float a2 = MathHelper.clamp((float)animationProgress, (float)0.0f, (float)1.0f);
        int theme = this.getThemeColor();
        float x2 = this.state.getX();
        float y2 = this.state.getY();
        for (Module.ModuleCategory category : Module.ModuleCategory.values()) {
            this.state.getCategorySwitchAnimation(category).update(category == this.state.getSelectedCategory() ? 1.0f : 0.0f);
        }
        context.getMatrices().push();
        float centerX = x2 + 235.0f;
        float centerY = y2 + 130.0f;
        float scale = 0.92f + 0.08f * a2;
        context.getMatrices().translate(centerX, centerY, 0.0f);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.getMatrices().translate(-centerX, -centerY, 0.0f);
        this.renderMain(context, x2, y2, theme, a2, mouseX, mouseY);
        context.getMatrices().pop();
    }

    private void renderMain(DrawContext context, float menuX, float menuY, int theme, float a2, double mouseX, double mouseY) {
        float x2 = menuX;
        float w2 = 470.0f;
        float h2 = 260.0f;
        float y2 = menuY;
        int hud2BgColor = ColorUtils.rgba(14, 14, 18, (int)(250.0f * a2));
        RenderUtils.drawRoundedRect(context.getMatrices(), x2, y2, w2, h2, 9.0f, hud2BgColor);
        RenderUtils.drawTexture(context.getMatrices(), POLAR_LOGO, x2 + 9.0f + 30.0f, y2 + 10.0f + 12.0f, 24.0f, 24.0f, 0.0f, 0.0f, 1.0f, 1.0f, -1);
        float px = x2 + 9.0f;
        float py = y2 + 234.0f;
        float avatarY = py - 1.5f;
        RenderUtils.drawRoundedRect(context.getMatrices(), px, avatarY, 18.0f, 18.0f, 4.0f, ColorUtils.setAlphaColor(theme, (int)(60.0f * a2)));
        String username = MinecraftClient.getInstance().getSession().getUsername();
        String initial = username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase();
        this.font(12).drawCenteredString(context.getMatrices(), initial, px + 9.0f, avatarY + (18.0f - this.font(12).getHeight()) / 2.0f, ColorUtils.setAlphaColor(-1, (int)(255.0f * a2)));
        float nameX = px + 18.0f + 6.0f;
        this.font(12).draw(context.getMatrices(), username, nameX, py + 0.5f, ColorUtils.setAlphaColor(-1, (int)(255.0f * a2)));
        this.font(12).draw(context.getMatrices(), "Admin", nameX, py + 9.5f, ColorUtils.rgba(140, 140, 155, (int)(200.0f * a2)));
        float catX = x2 + 9.0f - 3.0f;
        float catY = y2 + 62.0f;
        float catW = 88.0f;
        int index = 0;
        for (Module.ModuleCategory category : Module.ModuleCategory.values()) {
            if ("LUA".equals(category.name())) continue;
            if (index == 0) {
                this.font(12).draw(context.getMatrices(), "Main", x2 + 9.0f, catY, ColorUtils.rgba(120, 120, 132, (int)(180.0f * a2)));
                catY += 12.0f;
            } else if (index == 2) {
                this.font(12).draw(context.getMatrices(), "Other", x2 + 9.0f, catY += 3.0f, ColorUtils.rgba(120, 120, 132, (int)(180.0f * a2)));
                catY += 14.0f;
            }
            float selectP = this.state.getCategorySwitchAnimation(category).getValue();
            if (selectP > 0.001f) {
                RenderUtils.drawRoundedRect(context.getMatrices(), catX, catY, catW, 19.0f, 6.0f, ColorUtils.setAlphaColor(theme, (int)(40.0f * selectP * a2)));
            }
            int iconColor = ColorUtils.interpolateColor(ColorUtils.rgba(170, 170, 185, (int)(160.0f * a2)), ColorUtils.setAlphaColor(-1, (int)(255.0f * a2)), selectP);
            int textColor = ColorUtils.interpolateColor(ColorUtils.rgba(200, 200, 210, (int)(170.0f * a2)), ColorUtils.setAlphaColor(-1, (int)(255.0f * a2)), selectP);
            float itemCenterY = catY + 9.5f;
            this.icons(10).drawCenteredString(context.getMatrices(), category.getIcons(), catX + 13.0f, itemCenterY - this.icons(10).getHeight() / 2.0f, iconColor);
            String categoryName = category.name();
            String formattedName = categoryName.isEmpty() ? "" : categoryName.substring(0, 1).toUpperCase() + (categoryName.length() > 1 ? categoryName.substring(1).toLowerCase() : "");
            this.font(12).draw(context.getMatrices(), formattedName, catX + 32.0f, itemCenterY - this.font(12).getHeight() / 2.0f, textColor);
            this.regions.add(Region.of(Region.Type.CATEGORY, catX, catY, catW, 19.0f).category(category));
            catY += 22.0f;
            ++index;
        }
        float breadcrumbX = x2 + 100.0f + 6.0f;
        float breadcrumbY = y2 + 10.0f;
        String prefix = "Lavanda > ";
        this.font(12).draw(context.getMatrices(), prefix, breadcrumbX, breadcrumbY, ColorUtils.rgba(140, 140, 155, (int)(180.0f * a2)));
        float catTextX = breadcrumbX + this.font(12).getWidth(prefix);
        for (Module.ModuleCategory cat : Module.ModuleCategory.values()) {
            float catP;
            if ("LUA".equals(cat.name()) || !((catP = this.state.getCategorySwitchAnimation(cat).getValue()) > 0.001f)) continue;
            String cName = cat.name();
            String formattedCName = cName.isEmpty() ? "" : cName.substring(0, 1).toUpperCase() + (cName.length() > 1 ? cName.substring(1).toLowerCase() : "");
            this.font(12).draw(context.getMatrices(), formattedCName, catTextX, breadcrumbY, ColorUtils.setAlphaColor(-1, (int)(220.0f * catP * a2)));
        }
        float searchX = x2 + 100.0f + 8.0f;
        float searchY = y2 + 24.0f;
        float searchW = w2 - 100.0f - 16.0f;
        int searchBg = this.state.isSearchActive() ? ColorUtils.rgba(26, 26, 33, (int)(245.0f * a2)) : ColorUtils.rgba(20, 20, 26, (int)(245.0f * a2));
        RenderUtils.drawRoundedRect(context.getMatrices(), searchX, searchY, searchW, 18.0f, 6.0f, searchBg);
        String text = this.state.getSearchText();
        float textX = searchX + 10.0f;
        float textY = searchY + (18.0f - this.font(12).getHeight()) / 2.0f;
        if (text.isEmpty() && !this.state.isSearchActive()) {
            this.font(12).draw(context.getMatrices(), "Поиск", textX, textY, ColorUtils.rgba(130, 130, 145, (int)(170.0f * a2)));
        } else {
            this.font(12).draw(context.getMatrices(), text, textX, textY, ColorUtils.setAlphaColor(-1, (int)(255.0f * a2)));
            if (this.state.isSearchActive() && System.currentTimeMillis() / 500L % 2L == 0L) {
                float cursorX = textX + this.font(12).getWidth(text.substring(0, this.state.getSearchCursor())) + 1.0f;
                RenderUtils.drawRoundedRect(context.getMatrices(), cursorX, searchY + 4.0f, 1.0f, 10.0f, 0.5f, ColorUtils.setAlphaColor(theme, (int)(255.0f * a2)));
            }
        }
        this.regions.add(Region.of(Region.Type.SEARCH, searchX, searchY, searchW, 18.0f));
        Module.ModuleCategory category = this.state.getSelectedCategory();
        float scroll = this.state.getScroll(category);
        float contentTop = ClickGuiLayout.contentTop(y2);
        float contentBottom = ClickGuiLayout.contentBottom(y2);
        float colW = ClickGuiLayout.columnWidth();
        float col0X = x2 + 100.0f + 8.0f;
        float col1X = col0X + colW + 6.0f;
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(x2, contentTop - 2.0f, w2, contentBottom - contentTop + 4.0f);
        float[] colHeights = new float[]{0.0f, 0.0f};
        for (Module module : this.state.getModules(category)) {
            int col = colHeights[0] <= colHeights[1] ? 0 : 1;
            float cardX = col == 0 ? col0X : col1X;
            float cardY = contentTop + scroll + colHeights[col];
            float openProgress = this.state.getOpenProgress(module);
            float settingsH = this.settingRenderer.measureSettingsHeight(module, colW) * openProgress;
            float cardH = 22.0f + (openProgress > 0.001f ? settingsH : 0.0f);
            boolean cardHovered = mouseX >= (double)cardX && mouseX <= (double)(cardX + colW) && mouseY >= (double)cardY && mouseY <= (double)(cardY + cardH);
            float cardHoverP = this.state.getHoverAnimation("card_scale_" + module.getName(), cardHovered).getValue();
            float cardScale = 1.0f + 0.012f * cardHoverP;
            context.getMatrices().push();
            float cardCenterX = cardX + colW / 2.0f;
            float cardCenterY = cardY + cardH / 2.0f;
            context.getMatrices().translate(cardCenterX, cardCenterY, 0.0f);
            context.getMatrices().scale(cardScale, cardScale, 1.0f);
            context.getMatrices().translate(-cardCenterX, -cardCenterY, 0.0f);
            this.renderCard(context, module, cardX, cardY, colW, cardH, theme, a2, mouseX, mouseY, openProgress);
            context.getMatrices().pop();
            int n2 = col;
            colHeights[n2] = colHeights[n2] + (cardH + 6.0f);
        }
        ScissorUtils.pop();
        this.contentHeight = Math.max(colHeights[0], colHeights[1]) - 6.0f;
        this.state.clampScrollPixels(category, contentBottom - contentTop, this.contentHeight);
    }

    private void renderCard(DrawContext context, Module module, float x2, float y2, float w2, float h2, int theme, float a2, double mouseX, double mouseY, float openProgress) {
        boolean enabled = module.isEnable();
        boolean hovered = mouseX >= (double)x2 && mouseX <= (double)(x2 + w2) && mouseY >= (double)y2 && mouseY <= (double)(y2 + 22.0f);
        float hoverP = this.state.getHoverAnimation("module_" + module.getName(), hovered).getValue();
        RenderUtils.drawRoundedRect(context.getMatrices(), x2, y2, w2, h2, 7.0f, ColorUtils.rgba(20, 20, 26, (int)((235.0f + 20.0f * hoverP) * a2)));
        float textLeft = x2 + 8.0f;
        float headerIconY = y2 + (22.0f - this.icons(10).getHeight()) / 2.0f;
        this.icons(10).drawCenteredString(context.getMatrices(), module.getCategory().getIcons(), textLeft + 4.0f, headerIconY, enabled ? ColorUtils.setAlphaColor(theme, (int)(255.0f * a2)) : ColorUtils.rgba(150, 150, 165, (int)(180.0f * a2)));
        String name = this.translate(module.getName());
        int nameColor = enabled ? ColorUtils.setAlphaColor(-1, (int)(255.0f * a2)) : ColorUtils.rgba(205, 205, 215, (int)(200.0f * a2));
        float cardTextY = y2 + (22.0f - this.font(13).getHeight()) / 2.0f;
        this.font(13).draw(context.getMatrices(), name, textLeft += 12.0f, cardTextY, nameColor);
        String bindLabel = this.findModuleBindLabel(module);
        if (bindLabel != null && !bindLabel.isEmpty()) {
            float bindLabelY = y2 + (22.0f - this.font(12).getHeight()) / 2.0f;
            this.font(12).draw(context.getMatrices(), bindLabel, x2 + w2 - 8.0f - this.font(12).getWidth(bindLabel), bindLabelY, ColorUtils.rgba(140, 140, 152, (int)(190.0f * a2)));
        }
        this.regions.add(Region.of(Region.Type.MODULE_HEADER, x2, y2, w2, 22.0f).module(module));
        if (openProgress > 0.001f && h2 > 22.5f) {
            int alpha = (int)(255.0f * a2 * openProgress);
            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(x2, y2 + 22.0f, w2, h2 - 22.0f);
            this.settingRenderer.render(context, module, x2, y2 + 22.0f, w2, theme, alpha, mouseX, mouseY, this.state, this.regions);
            ScissorUtils.pop();
        }
    }

    private String findModuleBindLabel(Module module) {
        try {
            List<Setting> settings = module.getSettings();
            if (settings == null) {
                return null;
            }
            for (Setting s2 : settings) {
                if (!(s2 instanceof BindSetting)) continue;
                BindSetting bind = (BindSetting)s2;
                String name = KeyBoardUtils.getBindName(bind.getKey());
                return this.state.toEnglish(name);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private int getThemeColor() {
        try {
            if (!polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
                return polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return ColorUtils.getThemeColor();
    }

    private String translate(String key) {
        if (polar.INSTANCE == null || polar.INSTANCE.localizationStorage == null) {
            return key;
        }
        return polar.INSTANCE.localizationStorage.translate(key);
    }

    private Font font(int size) {
        return ClickGuiLayout.font(size);
    }

    private Font fontTitle(int size) {
        Font f2 = Fonts.getFont("moe3", size);
        if (f2 == null) {
            f2 = Fonts.getFont("moe3", 18);
        }
        if (f2 == null) {
            f2 = ClickGuiLayout.font(13);
        }
        return f2;
    }

    private Font icons(int size) {
        Font f2 = Fonts.getFont("icon", size);
        if (f2 == null) {
            f2 = Fonts.getFont("icon", 18);
        }
        return f2;
    }

    public static final class Region {
        public final Type type;
        public final float x;
        public final float y;
        public final float w;
        public final float h;
        public Module module;
        public Setting setting;
        public String modeValue;
        public BooleanSetting listEntry;
        public Module.ModuleCategory category;

        private Region(Type type, float x2, float y2, float w2, float h2) {
            this.type = type;
            this.x = x2;
            this.y = y2;
            this.w = w2;
            this.h = h2;
        }

        public static Region of(Type type, float x2, float y2, float w2, float h2) {
            return new Region(type, x2, y2, w2, h2);
        }

        public Region module(Module m2) {
            this.module = m2;
            return this;
        }

        public Region setting(Setting s2) {
            this.setting = s2;
            return this;
        }

        public Region modeValue(String v2) {
            this.modeValue = v2;
            return this;
        }

        public Region listEntry(BooleanSetting e2) {
            this.listEntry = e2;
            return this;
        }

        public Region category(Module.ModuleCategory c2) {
            this.category = c2;
            return this;
        }

        public boolean contains(double mx, double my) {
            return mx >= (double)this.x && mx <= (double)(this.x + this.w) && my >= (double)this.y && my <= (double)(this.y + this.h);
        }

        public static enum Type {
            CATEGORY,
            MODULE_HEADER,
            TOGGLE,
            CHIP_MODE,
            CHIP_LIST,
            SLIDER,
            BIND,
            TEXT,
            SEARCH,
            TEXT_INPUT;

        }
    }
}

