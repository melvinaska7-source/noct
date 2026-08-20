package polar.ru.client.ui.clickgui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.math.HoveringUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.api.utils.scissor.ScissorUtils;
import polar.ru.client.ui.clickgui.ClickGuiConfigManager;
import polar.ru.client.ui.clickgui.ClickGuiLayout;
import polar.ru.client.ui.clickgui.ClickGuiWarningDialog;

public class ClickGuiConfigsPanel {
    public static final float BUTTON_WIDTH = 66.0f;
    public static final float BUTTON_HEIGHT = 17.0f;
    private static final float ROW_HEIGHT = 20.0f;
    private static final float ROW_GAP = 10.0f;
    private static final float BOTTOM_HEIGHT = 34.0f;
    private static final float FIELD_HEIGHT = 13.0f;
    private static final float CHIP_HEIGHT = 14.0f;
    private static final int BG_COLOR = ColorUtils.rgba(20, 20, 20, 100);
    private final List<String> configs = new ArrayList<String>();
    private final Map<String, AnimationUtils> toggleAnimation = new HashMap<String, AnimationUtils>();
    private final ClickGuiWarningDialog warningDialog = new ClickGuiWarningDialog();
    private String activeConfig = "";
    private String selectedConfig = "";
    private String status = "Конфиги";
    private String input = "";
    private boolean inputActive;
    private float scroll;
    private long lastRefresh;

    public ClickGuiWarningDialog getWarningDialog() {
        return this.warningDialog;
    }

    public String getActiveConfig() {
        return this.activeConfig;
    }

    public void refresh() {
        long now = System.currentTimeMillis();
        if (now - this.lastRefresh < 400L && !this.configs.isEmpty()) {
            return;
        }
        this.lastRefresh = now;
        this.configs.clear();
        this.configs.addAll(ClickGuiConfigManager.listConfigs());
        if (!this.activeConfig.isEmpty() && !this.configs.contains(this.activeConfig)) {
            this.activeConfig = "";
        }
        if (!this.selectedConfig.isEmpty() && !this.configs.contains(this.selectedConfig)) {
            this.selectedConfig = "";
        }
    }

    public static float getButtonX() {
        return 10.0f;
    }

    public static float getButtonY(Window window) {
        return (float)window.getScaledHeight() - 10.0f - 45.0f - 17.0f - 6.0f;
    }

    public void renderButton(DrawContext context, Window window, int mouseX, int mouseY, int colorTheme, float alphaMul, boolean opened) {
        Font font = Fonts.getFont("moe3", 15);
        if (font == null || window == null) {
            return;
        }
        float x2 = ClickGuiConfigsPanel.getButtonX();
        float y2 = ClickGuiConfigsPanel.getButtonY(window);
        MatrixStack matrices = context.getMatrices();
        boolean hovered = HoveringUtils.isHovered(mouseX, mouseY, x2, y2, 66.0, 17.0);
        RenderUtils.drawShadow(matrices, x2 - 2.0f, y2 - 2.0f, 70.0f, 21.0f, 6.0f, ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 200), alphaMul));
        RenderUtils.drawBlur(matrices, x2, y2, 66.0f, 17.0f, 5.0f, 5.0f, ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 255), alphaMul));
        RenderUtils.drawBlur(matrices, x2, y2, 66.0f, 17.0f, 5.0f, 5.0f, ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 180), alphaMul));
        RenderUtils.drawRoundedRect(matrices, x2, y2, 66.0f, 17.0f, 5.0f, ColorUtils.applyAlpha(opened || hovered ? ColorUtils.setAlphaColor(colorTheme, 110) : BG_COLOR, alphaMul));
        font.drawCenteredString(matrices, "Configs", x2 + 33.0f, y2 + (17.0f - font.getHeight()) / 2.0f, ColorUtils.applyAlpha(-1, alphaMul));
    }

    public void render(DrawContext context, int mouseX, int mouseY, float panelX, float panelY, int colorTheme, float alphaMul) {
        this.refresh();
        Font titleFont = Fonts.getFont("moe3", 15);
        Font nameFont = Fonts.getFont("moe3", 14);
        Font smallFont = Fonts.getFont("suisse", 11);
        if (titleFont == null || nameFont == null || smallFont == null) {
            return;
        }
        MatrixStack matrices = context.getMatrices();
        RenderUtils.drawShadow(matrices, panelX - 3.0f, panelY - 3.0f, 476.0f, 266.0f, 8.0f, ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 210), alphaMul));
        RenderUtils.drawBlur(matrices, panelX, panelY, 470.0f, 260.0f, 5.0f, 6.0f, ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 255), alphaMul));
        RenderUtils.drawBlur(matrices, panelX, panelY, 470.0f, 260.0f, 5.0f, 6.0f, ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 185), alphaMul));
        RenderUtils.drawRoundedRect(matrices, panelX, panelY, 470.0f, 260.0f, 5.0f, ColorUtils.applyAlpha(BG_COLOR, alphaMul));
        titleFont.drawCenteredString(matrices, "Configs", panelX + 52.0f, panelY + 12.0f, ColorUtils.applyAlpha(-1, alphaMul));
        RenderUtils.drawRoundedRect(matrices, panelX + 15.0f, panelY + 20.0f, 153.0f, 0.7f, 0.35f, ColorUtils.applyAlpha(ColorUtils.setAlphaColor(colorTheme, 130), alphaMul));
        float contentY = ClickGuiLayout.getContentY(panelY);
        float contentHeight = ClickGuiLayout.getContentHeight();
        float listY = contentY + 10.0f;
        float listHeight = Math.max(30.0f, contentHeight - 34.0f - 14.0f);
        float bottomY = contentY + contentHeight - 34.0f;
        smallFont.draw(matrices, this.status, panelX + 8.0f, contentY, ColorUtils.applyAlpha(ColorUtils.rgba(175, 175, 185, 225), alphaMul));
        float totalHeight = (float)this.configs.size() * 30.0f;
        float maxScroll = Math.min(0.0f, listHeight - totalHeight);
        this.scroll = Math.max(maxScroll, Math.min(0.0f, this.scroll));
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(panelX, listY, 470.0, listHeight + 1.0f);
        float rowY = listY + this.scroll;
        for (String config : this.configs) {
            if (rowY + 20.0f >= listY && rowY <= listY + listHeight) {
                this.renderRow(context, nameFont, panelX, rowY, config, mouseX, mouseY, colorTheme, alphaMul);
            }
            rowY += 30.0f;
        }
        if (this.configs.isEmpty()) {
            smallFont.drawCenteredString(matrices, "Нет конфигов", panelX + 235.0f, listY + 16.0f, ColorUtils.applyAlpha(ColorUtils.rgba(150, 150, 158, 200), alphaMul));
        }
        ScissorUtils.pop();
        float fieldX = panelX + 15.0f;
        float fieldW = 153.0f;
        RenderUtils.drawShadow(matrices, fieldX, bottomY, fieldW, 13.0f, 5.0f, ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 170), alphaMul));
        RenderUtils.drawBlur(matrices, fieldX, bottomY, fieldW, 13.0f, 2.0f, ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 255), alphaMul));
        RenderUtils.drawRoundedRect(matrices, fieldX, bottomY, fieldW, 13.0f, 2.0f, ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 165), alphaMul));
        if (this.inputActive) {
            RenderUtils.drawRoundedRect(matrices, fieldX, bottomY + 13.0f - 1.0f, fieldW, 0.8f, 0.4f, ColorUtils.applyAlpha(ColorUtils.setAlphaColor(colorTheme, 200), alphaMul));
        }
        String preview = this.input.isEmpty() ? "Название конфига" : this.input;
        int previewColor = this.input.isEmpty() ? ColorUtils.rgba(140, 140, 150, 200) : ColorUtils.rgba(245, 245, 248, 255);
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(fieldX + 2.0f, bottomY, fieldW - 4.0f, 13.0);
        smallFont.draw(matrices, preview, fieldX + 4.0f, bottomY + (13.0f - smallFont.getHeight()) / 2.0f, ColorUtils.applyAlpha(previewColor, alphaMul));
        if (this.inputActive && System.currentTimeMillis() / 500L % 2L == 0L) {
            float cursorX = fieldX + 4.5f + smallFont.getWidth(this.input);
            RenderUtils.drawRoundedRect(matrices, cursorX, bottomY + 2.5f, 0.8f, 8.0f, 0.0f, ColorUtils.applyAlpha(ColorUtils.setAlphaColor(colorTheme, 230), alphaMul));
        }
        ScissorUtils.pop();
        float chipW = (fieldW - 3.0f) / 2.0f;
        float saveX = fieldX;
        float loadX = fieldX + chipW + 3.0f;
        float chipY = bottomY + 13.0f + 4.0f;
        this.renderChip(context, smallFont, "Save", saveX, chipY, chipW, mouseX, mouseY, colorTheme, alphaMul, true);
        this.renderChip(context, smallFont, "Load", loadX, chipY, chipW, mouseX, mouseY, colorTheme, alphaMul, false);
    }

    public void renderDialog(DrawContext context, int mouseX, int mouseY, Window window, int colorTheme) {
        this.warningDialog.render(context, mouseX, mouseY, window, colorTheme);
    }

    private void renderRow(DrawContext context, Font font, float panelX, float rowY, String config, int mouseX, int mouseY, int colorTheme, float alphaMul) {
        MatrixStack matrices = context.getMatrices();
        boolean hovered = HoveringUtils.isHovered(mouseX, mouseY, panelX + 15.0f, rowY, 153.0, 20.0);
        boolean selected = config.equals(this.selectedConfig);
        boolean enabled = config.equals(this.activeConfig);
        int background = selected ? ColorUtils.setAlphaColor(colorTheme, 70) : (hovered ? ColorUtils.rgba(255, 255, 255, 18) : ColorUtils.rgba(0, 0, 0, 60));
        RenderUtils.drawBlur(matrices, panelX + 15.0f, rowY - 0.5f, 153.0f, 21.0f, 3.0f, 6.0f, ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 150), alphaMul));
        RenderUtils.drawRoundedRect(matrices, panelX + 15.0f, rowY - 0.5f, 153.0f, 21.0f, 2.5f, ColorUtils.applyAlpha(background, alphaMul));
        int textColor = enabled ? -1 : ColorUtils.rgba(225, 225, 232, 210);
        float rowTextY = rowY - 0.5f + (21.0f - font.getHeight()) / 2.0f;
        this.drawClipped(context, font, config, panelX + 8.0f, rowTextY, 123.0f, ColorUtils.applyAlpha(textColor, alphaMul));
        AnimationUtils animation = this.toggleAnimation.computeIfAbsent(config, key -> new AnimationUtils(enabled ? 1.0f : 0.0f, 13.0f, Easings.CUBIC_OUT));
        animation.update(enabled ? 1.0f : 0.0f);
        float progress = animation.getValue();
        float toggleX = this.getToggleX(panelX);
        float toggleY = rowY + 5.5f;
        int offColor = ColorUtils.darken(colorTheme, 0.55f);
        int toggleColor = ColorUtils.interpolateColor(offColor, colorTheme, progress);
        RenderUtils.drawRoundedRect(matrices, toggleX, toggleY, 16.0f, 9.0f, 3.0f, ColorUtils.applyAlpha(toggleColor, alphaMul));
        RenderUtils.drawRoundCircle(matrices, toggleX + 4.5f + progress * 6.2f, toggleY + 4.5f, 7.0f, ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 255), alphaMul));
    }

    private void renderChip(DrawContext context, Font font, String label, float x2, float y2, float w2, int mouseX, int mouseY, int colorTheme, float alphaMul, boolean primary) {
        MatrixStack matrices = context.getMatrices();
        boolean hovered = HoveringUtils.isHovered(mouseX, mouseY, x2, y2, w2, 14.0);
        int color = primary ? (hovered ? ColorUtils.setAlphaColor(colorTheme, 220) : ColorUtils.setAlphaColor(colorTheme, 155)) : (hovered ? ColorUtils.rgba(64, 64, 74, 235) : ColorUtils.rgba(40, 40, 48, 220));
        RenderUtils.drawShadow(matrices, x2, y2, w2, 14.0f, 5.0f, ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 160), alphaMul));
        RenderUtils.drawRoundedRect(matrices, x2, y2, w2, 14.0f, 2.5f, ColorUtils.applyAlpha(color, alphaMul));
        font.drawCenteredString(matrices, label, x2 + w2 / 2.0f, y2 + (14.0f - font.getHeight()) / 2.0f, ColorUtils.applyAlpha(-1, alphaMul));
    }

    private void drawClipped(DrawContext context, Font font, String name, float x2, float y2, float maxWidth, int color) {
        Object text = name;
        while (!((String)text).isEmpty() && font.getWidth((String)text) > maxWidth) {
            text = ((String)text).substring(0, ((String)text).length() - 1);
        }
        if (((String)text).length() < name.length() && ((String)text).length() > 1) {
            text = ((String)text).substring(0, ((String)text).length() - 1) + "…";
        }
        font.draw(context.getMatrices(), (String)text, x2, y2, color);
    }

    private float getToggleX(float panelX) {
        return panelX + 15.0f + 153.0f - 21.0f;
    }

    public boolean handleClick(double mouseX, double mouseY, int button, float panelX, float panelY) {
        if (button != 0) {
            return false;
        }
        this.refresh();
        float contentY = ClickGuiLayout.getContentY(panelY);
        float contentHeight = ClickGuiLayout.getContentHeight();
        float listY = contentY + 10.0f;
        float listHeight = Math.max(30.0f, contentHeight - 34.0f - 14.0f);
        float bottomY = contentY + contentHeight - 34.0f;
        float fieldX = panelX + 15.0f;
        float fieldW = 153.0f;
        float chipW = (fieldW - 3.0f) / 2.0f;
        float chipY = bottomY + 13.0f + 4.0f;
        if (HoveringUtils.isHovered(mouseX, mouseY, fieldX, bottomY, fieldW, 13.0)) {
            this.inputActive = true;
            return true;
        }
        if (HoveringUtils.isHovered(mouseX, mouseY, fieldX, chipY, chipW, 14.0)) {
            this.inputActive = false;
            this.handleSave();
            return true;
        }
        if (HoveringUtils.isHovered(mouseX, mouseY, fieldX + chipW + 3.0f, chipY, chipW, 14.0)) {
            this.inputActive = false;
            this.handleLoad();
            return true;
        }
        this.inputActive = false;
        if (!HoveringUtils.isHovered(mouseX, mouseY, panelX, listY, 470.0, listHeight)) {
            return HoveringUtils.isHovered(mouseX, mouseY, panelX, panelY, 470.0, 260.0);
        }
        float rowY = listY + this.scroll;
        for (String config : this.configs) {
            if (rowY + 20.0f >= listY && rowY <= listY + listHeight) {
                float toggleY;
                float toggleX = this.getToggleX(panelX);
                if (HoveringUtils.isHovered(mouseX, mouseY, toggleX, toggleY = rowY + 5.5f, 16.0, 9.0)) {
                    if (config.equals(this.activeConfig)) {
                        this.activeConfig = "";
                        this.status = "Выключен: " + config;
                    } else if (ClickGuiConfigManager.load(config)) {
                        this.activeConfig = config;
                        this.selectedConfig = config;
                        this.input = config;
                        this.status = "Загружен: " + config;
                    } else {
                        this.status = "Ошибка загрузки";
                    }
                    return true;
                }
                if (HoveringUtils.isHovered(mouseX, mouseY, panelX + 15.0f, rowY, 153.0, 20.0)) {
                    this.selectedConfig = config;
                    this.input = config;
                    this.status = "Выбран: " + config;
                    return true;
                }
            }
            rowY += 30.0f;
        }
        return true;
    }

    private void handleSave() {
        String name = ClickGuiConfigManager.sanitize(this.input);
        if (name.isEmpty()) {
            this.status = "Введите название";
            return;
        }
        if (ClickGuiConfigManager.exists(name)) {
            this.warningDialog.open("Конфиг \"" + name + "\" уже существует. Сохранение перезапишет все настройки этого конфига без возможности восстановления.", () -> this.saveInternal(name));
            return;
        }
        this.saveInternal(name);
    }

    private void saveInternal(String name) {
        if (ClickGuiConfigManager.save(name)) {
            this.status = "Сохранён: " + name;
            this.selectedConfig = name;
            this.lastRefresh = 0L;
            this.refresh();
        } else {
            this.status = "Ошибка сохранения";
        }
    }

    private void handleLoad() {
        String name = ClickGuiConfigManager.sanitize(this.input.isEmpty() ? this.selectedConfig : this.input);
        if (name.isEmpty()) {
            this.status = "Выберите конфиг";
            return;
        }
        if (!ClickGuiConfigManager.exists(name)) {
            this.status = "Конфиг не найден";
            return;
        }
        if (ClickGuiConfigManager.load(name)) {
            this.activeConfig = name;
            this.selectedConfig = name;
            this.status = "Загружен: " + name;
        } else {
            this.status = "Ошибка загрузки";
        }
    }

    public boolean handleScroll(double mouseX, double mouseY, float panelX, float panelY, double verticalAmount) {
        float contentHeight;
        float listHeight;
        float contentY = ClickGuiLayout.getContentY(panelY);
        float listY = contentY + 10.0f;
        if (!HoveringUtils.isHovered(mouseX, mouseY, panelX, listY, 470.0, listHeight = Math.max(30.0f, (contentHeight = ClickGuiLayout.getContentHeight()) - 34.0f - 14.0f))) {
            return false;
        }
        float totalHeight = (float)this.configs.size() * 30.0f;
        float maxScroll = Math.min(0.0f, listHeight - totalHeight);
        this.scroll = Math.max(maxScroll, Math.min(0.0f, this.scroll + (float)(verticalAmount * 20.0)));
        return true;
    }

    public boolean isInputActive() {
        return this.inputActive;
    }

    public void setInputActive(boolean inputActive) {
        this.inputActive = inputActive;
    }

    public boolean charTyped(char chr) {
        if (!this.inputActive || Character.isISOControl(chr) || this.input.length() >= 24) {
            return this.inputActive;
        }
        this.input = this.input + chr;
        return true;
    }

    public boolean keyPressed(int keyCode, int modifiers) {
        if (!this.inputActive) {
            return false;
        }
        if ((modifiers & 2) != 0 && keyCode == 86) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.keyboard != null) {
                String clipboard = ClickGuiConfigManager.sanitize(client.keyboard.getClipboard());
                this.input = this.input + clipboard;
                if (this.input.length() > 24) {
                    this.input = this.input.substring(0, 24);
                }
            }
            return true;
        }
        if (keyCode == 259) {
            if (!this.input.isEmpty()) {
                this.input = this.input.substring(0, this.input.length() - 1);
            }
            return true;
        }
        if (keyCode == 261) {
            this.input = "";
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            this.inputActive = false;
            this.handleSave();
            return true;
        }
        if (keyCode == 256) {
            this.inputActive = false;
            return true;
        }
        return true;
    }
}

