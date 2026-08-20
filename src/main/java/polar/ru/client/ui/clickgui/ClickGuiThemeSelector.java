package polar.ru.client.ui.clickgui;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import polar.ru.api.storages.implement.ThemeStorage;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.math.HoveringUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.polar;

public class ClickGuiThemeSelector {
    public void render(DrawContext context, Window window, float offsetY, float alphaMul, int shadeColor) {
        if (context == null || window == null) {
            return;
        }
        ObjectArrayList<ThemeStorage.Themes> themes = polar.INSTANCE.themeStorage.getThemeList();
        if (themes == null || themes.isEmpty()) {
            return;
        }
        float totalWidth = (float)themes.size() * 8.0f + (float)(themes.size() - 1) * 4.0f;
        float panelWidth = totalWidth + 8.0f;
        float panelX = this.getThemePanelX(window, panelWidth);
        float panelY = 100.0f + offsetY;
        float startX = panelX + 4.0f;
        float startY = panelY + 3.5f;
        RenderUtils.drawBlur(context.getMatrices(), panelX, panelY, panelWidth, 15.0f, 3.5f, ColorUtils.rgba(255, 255, 255, 255));
        RenderUtils.drawRoundedRect(context.getMatrices(), panelX, panelY, panelWidth, 15.0f, 3.5f, ColorUtils.rgba(0, 0, 0, 150));
        ThemeStorage.Themes selected = polar.INSTANCE.themeStorage.getThemes();
        for (int i2 = 0; i2 < themes.size(); ++i2) {
            ThemeStorage.Themes theme = (ThemeStorage.Themes)((Object)themes.get(i2));
            float boxX = startX + (float)i2 * 12.0f;
            float boxY = startY;
            if (theme == selected) {
                RenderUtils.drawRoundedRect(context.getMatrices(), boxX - 0.5f, boxY - 0.5f, 9.0f, 9.0f, 2.5f, ColorUtils.setAlphaColor(-1, Math.max(1, (int)(200.0f * alphaMul))));
            }
            RenderUtils.drawRoundedRect(context.getMatrices(), boxX, boxY, 8.0f, 8.0f, 2.0f, ColorUtils.applyAlpha(this.getThemeDisplayColor(theme), Math.max(0.55f, alphaMul)));
        }
    }

    public boolean handleClick(Window window, double mouseX, double mouseY, int button, float offsetY) {
        if (window == null || button != 0) {
            return false;
        }
        ObjectArrayList<ThemeStorage.Themes> themes = polar.INSTANCE.themeStorage.getThemeList();
        if (themes == null || themes.isEmpty()) {
            return false;
        }
        float totalWidth = (float)themes.size() * 8.0f + (float)(themes.size() - 1) * 4.0f;
        float panelWidth = totalWidth + 8.0f;
        float panelX = this.getThemePanelX(window, panelWidth);
        float panelY = 100.0f + offsetY;
        float startX = panelX + 4.0f;
        float startY = panelY + 3.5f;
        if (!HoveringUtils.isHovered(mouseX, mouseY, panelX, panelY, panelWidth, 15.0)) {
            return false;
        }
        for (int i2 = 0; i2 < themes.size(); ++i2) {
            float boxX = startX + (float)i2 * 12.0f;
            float boxY = startY;
            if (!HoveringUtils.isHovered(mouseX, mouseY, boxX, boxY, 8.0, 8.0)) continue;
            polar.INSTANCE.themeStorage.setThemes((ThemeStorage.Themes)((Object)themes.get(i2)));
            return true;
        }
        return false;
    }

    private int getThemeDisplayColor(ThemeStorage.Themes theme) {
        int color = theme.getTheme().getColor(0);
        if (ColorUtils.alpha(color) == 0) {
            return ColorUtils.rgba(220, 220, 220, 180);
        }
        return color;
    }

    private float getThemePanelX(Window window, float panelWidth) {
        return (float)window.getScaledWidth() / 2.0f - panelWidth / 2.0f;
    }
}

