package polar.ru.client.ui.clickgui;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.utils.animation.AnimationUtils;
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
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.client.modules.settings.implement.TextSetting;
import polar.ru.client.ui.clickgui.ClickGuiLayout;
import polar.ru.client.ui.clickgui.ClickGuiRenderer;
import polar.ru.client.ui.clickgui.ClickGuiState;
import polar.ru.polar;

public class ClickGuiSettingRenderer {
    private static final float HOVER_SCROLL_OVERFLOW_THRESHOLD = 6.0f;
    private static final float OPTION_H = 11.0f;
    private static final float TAG_GAP = 2.0f;
    private static final float OPTION_RADIUS = 0.5f;

    public void render(DrawContext context, Module module, float panelX, float moduleY, float openProgress, int colorTheme, double mouseX, double mouseY, ClickGuiState state) {
        int alpha = (int)(255.0f * openProgress);
        this.render(context, module, panelX, moduleY, ClickGuiLayout.columnWidth(), colorTheme, alpha, mouseX, mouseY, state, null);
    }

    public void render(DrawContext context, Module module, float panelX, float moduleY, float width, int colorTheme, int alpha, double mouseX, double mouseY, ClickGuiState state, List<ClickGuiRenderer.Region> regions) {
        List<Setting> settings = module.getSettings();
        if (settings == null || settings.isEmpty() || alpha <= 1) {
            return;
        }
        float maxSettingHeight = this.measureSettingsHeight(module, width);
        float settingsClipY = moduleY;
        float settingsClipHeight = maxSettingHeight;
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(panelX, settingsClipY - 3.0f, width, settingsClipHeight);
        float settingYoffset = 0.0f;
        for (Setting setting : settings) {
            if (setting == null || !setting.visible().booleanValue()) continue;
            float settingY = moduleY + settingYoffset + 8.0f;
            float settingPanelX = panelX;
            if (setting instanceof BooleanSetting) {
                BooleanSetting booleanSetting = (BooleanSetting)setting;
                this.renderBooleanSetting(context, settingPanelX, settingY, width, alpha, colorTheme, mouseX, mouseY, booleanSetting, state, regions);
                settingYoffset += 12.0f;
                continue;
            }
            if (setting instanceof TextSetting) {
                TextSetting textSetting = (TextSetting)setting;
                this.renderTextSetting(context, settingPanelX, settingY, width, alpha, colorTheme, mouseX, mouseY, textSetting, state, regions);
                settingYoffset += 22.0f;
                continue;
            }
            if (setting instanceof FloatSetting) {
                FloatSetting floatSetting = (FloatSetting)setting;
                this.renderFloatSetting(context, settingPanelX, settingY, width, alpha, colorTheme, mouseX, mouseY, floatSetting, state, regions);
                settingYoffset += 22.0f;
                continue;
            }
            if (setting instanceof ModeSetting) {
                ModeSetting modeSetting = (ModeSetting)setting;
                this.renderModeSetting(context, settingPanelX, settingY, width, alpha, colorTheme, mouseX, mouseY, modeSetting, state, regions);
                settingYoffset += ClickGuiLayout.calculateModeSettingHeight(modeSetting, width - 16.0f);
                continue;
            }
            if (setting instanceof ListSetting) {
                ListSetting listSetting = (ListSetting)setting;
                this.renderListSetting(context, settingPanelX, settingY, width, alpha, colorTheme, mouseX, mouseY, listSetting, state, regions);
                settingYoffset += ClickGuiLayout.calculateListSettingHeight(listSetting, width - 16.0f);
                continue;
            }
            if (!(setting instanceof BindSetting)) continue;
            BindSetting bindSetting = (BindSetting)setting;
            this.renderBindSetting(context, settingPanelX, settingY, width, alpha, colorTheme, mouseX, mouseY, bindSetting, state, regions);
            settingYoffset += 12.0f;
        }
        ScissorUtils.pop();
    }

    private void renderBooleanSetting(DrawContext context, float panelX, float settingY, float width, int alpha, int colorTheme, double mouseX, double mouseY, BooleanSetting booleanSetting, ClickGuiState state, List<ClickGuiRenderer.Region> regions) {
        AnimationUtils backgroundAnimation = state.getBooleanBackgroundAnimation(booleanSetting);
        AnimationUtils circleAnimation = state.getBooleanCircleAnimation(booleanSetting);
        backgroundAnimation.update(booleanSetting.isState() ? 1.0f : 0.0f);
        circleAnimation.update(booleanSetting.isState() ? 1.0f : 0.0f);
        float backgroundProgress = backgroundAnimation.getValue();
        float circleProgress = circleAnimation.getValue();
        int offColor = ColorUtils.darken(colorTheme, 0.05f);
        int onColor = colorTheme;
        int r2 = (int)((float)(offColor >> 16 & 0xFF) + (float)((onColor >> 16 & 0xFF) - (offColor >> 16 & 0xFF)) * backgroundProgress);
        int g2 = (int)((float)(offColor >> 8 & 0xFF) + (float)((onColor >> 8 & 0xFF) - (offColor >> 8 & 0xFF)) * backgroundProgress);
        int b2 = (int)((float)(offColor & 0xFF) + (float)((onColor & 0xFF) - (offColor & 0xFF)) * backgroundProgress);
        int a2 = (int)((float)(offColor >> 24 & 0xFF) + (float)((onColor >> 24 & 0xFF) - (offColor >> 24 & 0xFF)) * backgroundProgress);
        int interpolatedColor = a2 << 24 | r2 << 16 | g2 << 8 | b2;
        float leftX = panelX + 8.0f;
        float rightX = panelX + width - 8.0f;
        float toggleW = 16.0f;
        float toggleH = 9.0f;
        float toggleX = rightX - toggleW;
        float toggleY = settingY - 5.0f;
        String translatedName = this.translate(booleanSetting.name());
        this.drawStringWithHoverScroll(this.issue(13), context.getMatrices(), translatedName, leftX, settingY - 3.0f, toggleX - 4.0f - leftX, this.getPrimarySettingColor(alpha), mouseX, mouseY, state, this.getSettingTextKey(booleanSetting));
        RenderUtils.drawRoundedRect(context.getMatrices(), toggleX, toggleY, toggleW, toggleH, 3.0f, ColorUtils.rgba(interpolatedColor >> 16 & 0xFF, interpolatedColor >> 8 & 0xFF, interpolatedColor & 0xFF, alpha));
        float circleX = toggleX + 4.5f + circleProgress * 7.0f;
        RenderUtils.drawRoundCircle(context.getMatrices(), circleX, toggleY + 4.5f, 7.0f, ColorUtils.rgba(255, 255, 255, alpha));
        regions.add(ClickGuiRenderer.Region.of(ClickGuiRenderer.Region.Type.TOGGLE, leftX, settingY - 5.0f, width - 16.0f, 12.0f).setting(booleanSetting));
    }

    private void renderFloatSetting(DrawContext context, float panelX, float settingY, float width, int alpha, int colorTheme, double mouseX, double mouseY, FloatSetting floatSetting, ClickGuiState state, List<ClickGuiRenderer.Region> regions) {
        float leftX = panelX + 8.0f;
        float rightX = panelX + width - 8.0f;
        float trackW = width - 16.0f;
        if (floatSetting.isActive()) {
            floatSetting.setValue(state.updateActiveSliderValue(floatSetting, mouseX, trackW));
        }
        AnimationUtils sliderAnimation = state.getSliderAnimation(floatSetting);
        sliderAnimation.update(state.getSliderPos(floatSetting));
        float animatedPos = sliderAnimation.getValue();
        String valueString = this.formatSliderValue(floatSetting);
        float valueX = rightX - this.issue(12).getWidth(valueString);
        float nameMaxWidth = valueX - 4.0f - leftX;
        String translatedName = this.translate(floatSetting.name());
        this.drawStringWithHoverScroll(this.issue(12), context.getMatrices(), translatedName, leftX, settingY - 2.0f, nameMaxWidth, this.getPrimarySettingColor(alpha), mouseX, mouseY, state, this.getSettingTextKey(floatSetting));
        this.issue(12).drawString(context.getMatrices(), valueString, valueX, settingY - 2.0f, ColorUtils.setAlphaColor(colorTheme, alpha));
        int sliderBackgroundColor = ColorUtils.setAlphaColor(ColorUtils.darken(colorTheme, 0.2f), alpha);
        RenderUtils.drawRoundedRect(context.getMatrices(), leftX, settingY + 8.0f, trackW, 4.0f, 1.25f, sliderBackgroundColor);
        int sliderFillColor = ColorUtils.setAlphaColor(colorTheme, alpha);
        RenderUtils.drawRoundedRect(context.getMatrices(), leftX, settingY + 8.0f, animatedPos * trackW, 4.0f, 1.25f, sliderFillColor);
        RenderUtils.drawRoundCircle(context.getMatrices(), leftX + animatedPos * trackW, settingY + 10.0f, 6.0f, ColorUtils.setAlphaColor(-1, alpha));
        regions.add(ClickGuiRenderer.Region.of(ClickGuiRenderer.Region.Type.SLIDER, leftX, settingY + 6.0f, trackW, 8.0f).setting(floatSetting));
    }

    private void renderTextSetting(DrawContext context, float panelX, float settingY, float width, int alpha, int colorTheme, double mouseX, double mouseY, TextSetting textSetting, ClickGuiState state, List<ClickGuiRenderer.Region> regions) {
        float leftX = panelX + 8.0f;
        float rightX = panelX + width - 8.0f;
        String value = textSetting.get();
        boolean editing = state.getEditingTextSetting() == textSetting;
        String preview = value == null || value.isEmpty() ? "..." : value;
        Object boxText = editing ? preview + "_" : preview;
        float boxWidth = Math.min(65.0f, (width - 16.0f) * 0.45f);
        float boxX = rightX - boxWidth;
        String translatedName = this.translate(textSetting.name());
        this.drawStringWithHoverScroll(this.issue(13), context.getMatrices(), translatedName, leftX, settingY - 3.0f, boxX - 4.0f - leftX, this.getPrimarySettingColor(alpha), mouseX, mouseY, state, this.getSettingTextKey(textSetting));
        int textColor = ColorUtils.setAlphaColor(-1, alpha);
        float boxY = settingY - 2.5f;
        RenderUtils.drawRoundedRect(context.getMatrices(), boxX, boxY - 3.0f, boxWidth, 9.0f, 1.5f, ColorUtils.rgba(20, 20, 25, 220 * alpha));
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(boxX + 2.0f, boxY - 3.0f, boxWidth - 4.0f, 9.0);
        float boxTextY = (boxY - 3.0f) + (9.0f - this.issue(12).getHeight()) / 2.0f;
        this.drawStringWithHoverScroll(this.issue(12), context.getMatrices(), (String)boxText, boxX + 3.0f, boxTextY, boxWidth - 6.0f, textColor, mouseX, mouseY, state, this.getSettingTextKey(textSetting) + "_value");
        ScissorUtils.pop();
        regions.add(ClickGuiRenderer.Region.of(ClickGuiRenderer.Region.Type.TEXT_INPUT, boxX, boxY - 3.0f, boxWidth, 9.0f).setting(textSetting));
    }

    private void renderModeSetting(DrawContext context, float panelX, float settingY, float width, int alpha, int colorTheme, double mouseX, double mouseY, ModeSetting modeSetting, ClickGuiState state, List<ClickGuiRenderer.Region> regions) {
        float leftX = panelX + 8.0f;
        float availableWidth = width - 16.0f;
        String translatedName = this.translate(modeSetting.name());
        this.drawStringWithHoverScroll(this.issue(12), context.getMatrices(), translatedName, leftX, settingY - 2.0f, availableWidth, this.getPrimarySettingColor(alpha), mouseX, mouseY, state, this.getSettingTextKey(modeSetting));
        Font chipFont = this.issue(12);
        float chipX = leftX;
        float chipY = settingY + 10.0f - 0.5f;
        float offsetX = 0.0f;
        float offsetY = 0.0f;
        int modsInCurrentRow = 0;
        int maxModsPerRow = 2;
        for (String mode : modeSetting.getMods()) {
            String translatedMode = this.translate(mode);
            float chipWidth = chipFont.getWidth(translatedMode) + 8.0f;
            if (offsetX > 0.0f && offsetX + chipWidth > availableWidth || modsInCurrentRow >= maxModsPerRow) {
                offsetX = 0.0f;
                offsetY += 13.0f;
                modsInCurrentRow = 0;
            }
            boolean selected = modeSetting.getCurrent().equals(mode);
            AnimationUtils animation = state.getModeAnimation(this.getModeKey(modeSetting, mode), selected);
            animation.update(selected ? 1.0f : 0.0f);
            float progress = animation.getValue();
            int bgColor = ColorUtils.setAlphaColor(ColorUtils.interpolateColor(ColorUtils.rgba(20, 20, 25, 255), colorTheme, progress), alpha);
            int textColor = ColorUtils.setAlphaColor(ColorUtils.interpolateColor(ColorUtils.rgba(140, 139, 145, 255), ColorUtils.rgba(255, 255, 255, 255), progress), alpha);
            float currentX = chipX + offsetX;
            float currentY = chipY + offsetY;
            float chipRectY = currentY - 5.0f;
            float chipTextY = chipRectY + (11.0f - chipFont.getHeight()) / 2.0f;
            RenderUtils.drawRoundedRect(context.getMatrices(), currentX, chipRectY, chipWidth, 11.0f, 0.5f, bgColor);
            chipFont.drawString(context.getMatrices(), translatedMode, currentX + (chipWidth - chipFont.getWidth(translatedMode)) / 2.0f, chipTextY, textColor);
            regions.add(ClickGuiRenderer.Region.of(ClickGuiRenderer.Region.Type.CHIP_MODE, currentX, chipRectY, chipWidth, 11.0f).setting(modeSetting).modeValue(mode));
            offsetX += chipWidth + 2.0f;
            ++modsInCurrentRow;
        }
    }

    private void renderListSetting(DrawContext context, float panelX, float settingY, float width, int alpha, int colorTheme, double mouseX, double mouseY, ListSetting listSetting, ClickGuiState state, List<ClickGuiRenderer.Region> regions) {
        float leftX = panelX + 8.0f;
        float rightX = panelX + width - 8.0f;
        float availableWidth = width - 16.0f;
        int enabledCount = 0;
        for (BooleanSetting entry : listSetting.getSettings()) {
            if (!entry.isState()) continue;
            ++enabledCount;
        }
        String counter = enabledCount + "/" + listSetting.getSettings().size();
        float counterX = rightX - this.issue(12).getWidth(counter);
        String translatedName = this.translate(listSetting.name());
        this.drawStringWithHoverScroll(this.issue(12), context.getMatrices(), translatedName, leftX, settingY - 2.0f, counterX - 4.0f - leftX, this.getPrimarySettingColor(alpha), mouseX, mouseY, state, this.getSettingTextKey(listSetting));
        this.issue(12).draw(context.getMatrices(), counter, counterX, settingY - 2.0f, this.getPrimarySettingColor(alpha));
        Font chipFont = this.issue(12);
        float chipX = leftX;
        float chipY = settingY + 10.0f - 0.5f;
        float offsetX = 0.0f;
        float offsetY = 0.0f;
        int itemsInCurrentRow = 0;
        int maxItemsPerRow = 2;
        for (BooleanSetting entry : listSetting.getSettings()) {
            if (!entry.visible().booleanValue()) continue;
            String translatedEntry = this.translate(entry.name());
            float chipWidth = chipFont.getWidth(translatedEntry) + 8.0f;
            if (offsetX > 0.0f && offsetX + chipWidth > availableWidth || itemsInCurrentRow >= maxItemsPerRow) {
                offsetX = 0.0f;
                offsetY += 13.0f;
                itemsInCurrentRow = 0;
            }
            boolean selected = entry.isState();
            AnimationUtils animation = state.getListAnimation(this.getListKey(listSetting, entry), selected);
            animation.update(selected ? 1.0f : 0.0f);
            float progress = animation.getValue();
            int bgColor = ColorUtils.setAlphaColor(ColorUtils.interpolateColor(ColorUtils.rgba(20, 20, 25, 255), colorTheme, progress), alpha);
            int textColor = ColorUtils.setAlphaColor(ColorUtils.interpolateColor(ColorUtils.rgba(140, 139, 145, 255), ColorUtils.rgba(255, 255, 255, 255), progress), alpha);
            float currentX = chipX + offsetX;
            float currentY = chipY + offsetY;
            float chipRectY = currentY - 5.0f;
            float chipTextY = chipRectY + (11.0f - chipFont.getHeight()) / 2.0f;
            RenderUtils.drawRoundedRect(context.getMatrices(), currentX, chipRectY, chipWidth, 11.0f, 0.5f, bgColor);
            chipFont.drawString(context.getMatrices(), translatedEntry, currentX + (chipWidth - chipFont.getWidth(translatedEntry)) / 2.0f, chipTextY, textColor);
            regions.add(ClickGuiRenderer.Region.of(ClickGuiRenderer.Region.Type.CHIP_LIST, currentX, chipRectY, chipWidth, 11.0f).setting(listSetting).listEntry(entry));
            offsetX += chipWidth + 2.0f;
            ++itemsInCurrentRow;
        }
    }

    private void renderBindSetting(DrawContext context, float panelX, float settingY, float width, int alpha, int colorTheme, double mouseX, double mouseY, BindSetting bindSetting, ClickGuiState state, List<ClickGuiRenderer.Region> regions) {
        float leftX = panelX + 8.0f;
        float rightX = panelX + width - 8.0f;
        boolean binding = state.getBindingSetting() == bindSetting;
        AnimationUtils bindAnimation = state.getBindAnimation(this.getBindKey(bindSetting), binding);
        bindAnimation.update(binding ? 1.0f : 0.0f);
        float progress = bindAnimation.getValue();
        String bindString = binding ? "..." : state.toEnglish(KeyBoardUtils.getBindName(bindSetting.getKey()));
        float bindTextWidth = this.issue(12).getWidth(bindString);
        float bindWidth = bindTextWidth + 6.0f;
        float bindX = rightX - bindWidth;
        int bindBackgroundColor = ColorUtils.setAlphaColor(ColorUtils.interpolateColor(ColorUtils.darken(colorTheme, 0.15f), colorTheme, progress), alpha);
        int bindTextColor = ColorUtils.setAlphaColor(ColorUtils.interpolateColor(ColorUtils.rgb(140, 139, 145), -1, progress), alpha);
        float bindRectY = settingY - 5.5f;
        float bindTextY = bindRectY + (9.0f - this.issue(12).getHeight()) / 2.0f;
        RenderUtils.drawRoundedRect(context.getMatrices(), bindX, bindRectY, bindWidth, 9.0f, 1.5f, ColorUtils.rgba(20, 20, 25, 220 * alpha));
        this.issue(12).drawString(context.getMatrices(), bindString, bindX + 3.0f, bindTextY, bindTextColor);
        String translatedName = this.translate(bindSetting.name());
        this.drawStringWithHoverScroll(this.issue(12), context.getMatrices(), translatedName, leftX, settingY - 2.0f, bindX - 4.0f - leftX, this.getPrimarySettingColor(alpha), mouseX, mouseY, state, this.getSettingTextKey(bindSetting));
        regions.add(ClickGuiRenderer.Region.of(ClickGuiRenderer.Region.Type.BIND, bindX, bindRectY, bindWidth, 9.0f).setting(bindSetting));
    }

    private String translate(String key) {
        if (polar.INSTANCE == null || polar.INSTANCE.localizationStorage == null) {
            return key;
        }
        return polar.INSTANCE.localizationStorage.translate(key);
    }

    private String getModeKey(ModeSetting setting, String mode) {
        return System.identityHashCode(setting) + "_mode_" + mode;
    }

    private String getListKey(ListSetting setting, BooleanSetting entry) {
        return setting.hashCode() + "_list_" + entry.name();
    }

    private String getBindKey(BindSetting setting) {
        return setting.hashCode() + "_bind";
    }

    private String formatSliderValue(FloatSetting setting) {
        float value = setting.get();
        float increment = setting.getIncrement();
        if (increment >= 1.0f) {
            return String.valueOf((int)value);
        }
        if (increment >= 0.1f) {
            return String.format("%.1f", Float.valueOf(value));
        }
        return String.format("%.2f", Float.valueOf(value));
    }

    private void drawStringWithHoverScroll(Font font, MatrixStack matrix, String text, float x2, float y2, float maxWidth, int color, double mouseX, double mouseY, ClickGuiState state, String animationKey) {
        if (text == null || text.isEmpty() || maxWidth <= 0.0f) {
            return;
        }
        float totalWidth = font.getWidth(text);
        float overflow = totalWidth - maxWidth;
        if (overflow <= 6.0f) {
            font.draw(matrix, text, x2, y2, color);
            return;
        }
        boolean hovered = this.isTextHovered(x2, y2, maxWidth, font.getHeight(), mouseX, mouseY);
        float scrollPhase = state.advanceTextScrollPhase(animationKey, hovered);
        boolean scrollActive = state.isTextScrollActive(animationKey, hovered);
        AnimationUtils hoverAnimation = state.getTextHoverAnimation(animationKey, scrollActive);
        hoverAnimation.update(scrollActive ? 1.0f : 0.0f);
        float hoverProgress = hoverAnimation.getValue();
        float scrollOffset = this.getHoverScrollOffset(overflow, scrollPhase) * hoverProgress;
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(x2, y2 - 2.0f, maxWidth, font.getHeight() + 4.0f);
        font.draw(matrix, text, x2 - scrollOffset, y2, color);
        ScissorUtils.pop();
    }

    private int getPrimarySettingColor(int alpha) {
        return ColorUtils.rgba(245, 245, 248, alpha);
    }

    private boolean isTextHovered(float x2, float y2, float width, float height, double mouseX, double mouseY) {
        return mouseX >= (double)x2 && mouseX <= (double)(x2 + width) && mouseY >= (double)(y2 - 2.0f) && mouseY <= (double)(y2 + height + 2.0f);
    }

    private float getHoverScrollOffset(float maxOffset, float phase) {
        if (maxOffset <= 0.0f) {
            return 0.0f;
        }
        float pingPong = phase < 0.5f ? phase * 2.0f : 2.0f - phase * 2.0f;
        float eased = pingPong * pingPong * (3.0f - 2.0f * pingPong);
        return maxOffset * eased;
    }

    private String getSettingTextKey(Setting setting) {
        return "setting_text_" + System.identityHashCode(setting);
    }

    private Font issue(int size) {
        return Fonts.getFont("suisse", size);
    }

    public float measureSettingsHeight(Module module) {
        return this.measureSettingsHeight(module, ClickGuiLayout.columnWidth());
    }

    public float measureSettingsHeight(Module module, float width) {
        List<Setting> settings = module.getSettings();
        if (settings == null || settings.isEmpty()) {
            return 0.0f;
        }
        float height = 0.0f;
        float availableWidth = width - 16.0f;
        for (Setting setting : settings) {
            if (setting == null || !setting.visible().booleanValue()) continue;
            if (setting instanceof BooleanSetting) {
                height += 12.0f;
                continue;
            }
            if (setting instanceof FloatSetting) {
                height += 27.0f;
                continue;
            }
            if (setting instanceof ModeSetting) {
                height += ClickGuiLayout.calculateModeSettingHeight((ModeSetting)setting, availableWidth);
                continue;
            }
            if (setting instanceof ListSetting) {
                height += ClickGuiLayout.calculateListSettingHeight((ListSetting)setting, availableWidth);
                continue;
            }
            if (setting instanceof BindSetting) {
                height += 16.0f;
                continue;
            }
            if (!(setting instanceof TextSetting)) continue;
            height += 17.0f;
        }
        return height;
    }
}

