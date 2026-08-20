package polar.ru.client.ui.clickgui;

import java.util.ArrayList;
import java.util.List;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.Setting;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;

public final class ClickGuiLayout {
    public static final float WIDTH = 470.0f;
    public static final float HEIGHT = 260.0f;
    public static final float SIDEBAR_W = 100.0f;
    public static final float PANEL_GAP = 0.0f;
    public static final float PANEL_RADIUS = 9.0f;
    public static final float SIDE_PAD = 9.0f;
    public static final float LOGO_Y = 10.0f;
    public static final float PROFILE_Y = 234.0f;
    public static final float AVATAR_SIZE = 18.0f;
    public static final float CATS_Y = 62.0f;
    public static final float CAT_ITEM_H = 19.0f;
    public static final float CAT_ITEM_GAP = 3.0f;
    public static final float CAT_LABEL_H = 12.0f;
    public static final float MAIN_PAD = 8.0f;
    public static final float SEARCH_H = 18.0f;
    public static final float SEARCH_RADIUS = 6.0f;
    public static final float COL_GAP = 6.0f;
    public static final float CARD_GAP = 6.0f;
    public static final float CARD_RADIUS = 7.0f;
    public static final float CARD_HEADER_H = 22.0f;
    public static final float CARD_PAD = 8.0f;
    public static final float SET_GAP = 5.0f;
    public static final float ROW_BOOL_H = 16.0f;
    public static final float ROW_BIND_H = 16.0f;
    public static final float ROW_TEXT_H = 17.0f;
    public static final float ROW_SLIDER_H = 27.0f;
    public static final float CHIP_H = 15.0f;
    public static final float CHIP_GAP = 4.0f;
    public static final float CHIP_PAD_X = 7.0f;
    public static final float CHIP_RADIUS = 5.0f;
    public static final float HEADER_HEIGHT = 40.0f;
    public static final float MODULE_SIZE = 80.0f;
    public static final float MODULE_GAP = 10.0f;
    public static final float MODULE_PADDING = 15.0f;
    public static final float MODULE_HEADER_HEIGHT = 20.0f;
    public static final float SETTING_PADDING = 8.0f;
    public static final float SETTING_HEIGHT = 20.0f;
    public static final float CATEGORY_ICON_SIZE = 24.0f;
    public static final float CATEGORY_GAP = 30.0f;
    public static final float CATEGORY_PANEL_STEP = 108.0f;
    public static final float THEME_PANEL_Y = 100.0f;
    public static final float THEME_PANEL_H = 15.0f;
    public static final float THEME_BOX_SIZE = 8.0f;
    public static final float THEME_BOX_GAP = 4.0f;
    public static final float THEME_BOX_RADIUS = 2.0f;
    public static final float THEME_SIDE_PADDING = 4.0f;
    public static final float MODULE_INNER_WIDTH = 153.0f;
    public static final float SETTING_START_Y = 20.0f;
    public static final float SETTING_BOTTOM_PADDING = 3.0f;
    public static final float SETTING_LEFT = 8.0f;
    public static final float SETTING_RIGHT = 161.0f;
    public static final float SLIDER_WIDTH = 153.0f;
    public static final float TEXT_SETTING_WIDTH = 60.0f;
    public static final float CLICKABLE_WIDTH = 153.0f;
    public static final float TAG_START_Y = 10.0f;
    public static final float TAG_GAP = 3.0f;
    public static final float TAG_ROW_GAP = 2.0f;
    public static final int SEARCH_MAX_CHARS = 24;
    public static final float SEARCH_WIDTH = 75.0f;
    public static final float SEARCH_HEIGHT = 18.0f;
    public static final float SEARCH_GAP = 8.0f;
    public static final float SEARCH_ICON_X = 3.5f;
    public static final float SEARCH_TEXT_X = 19.0f;
    public static final float SEARCH_RIGHT_PADDING = 8.0f;
    public static final float TOGGLE_W = 24.0f;
    public static final float TOGGLE_H = 14.0f;
    public static final float SLIDER_H = 20.0f;
    public static final float SLIDER_KNOB = 8.0f;

    private ClickGuiLayout() {
    }

    public static float mainX(float menuX) {
        return menuX;
    }

    public static float mainW() {
        return 470.0f;
    }

    public static float contentTop(float menuY) {
        return menuY + 48.0f;
    }

    public static float contentBottom(float menuY) {
        return menuY + 260.0f - 8.0f;
    }

    public static float columnWidth() {
        return (ClickGuiLayout.mainW() - 100.0f - 16.0f - 6.0f) / 2.0f;
    }

    public static Font font(int size) {
        return Fonts.getFont("moe3", size);
    }

    public static float getContentY(float y2) {
        return y2 + 40.0f;
    }

    public static float getContentHeight() {
        return 220.0f;
    }

    public static float getModuleGridX(float baseX, int colIndex) {
        return baseX + 15.0f + (float)colIndex * 90.0f;
    }

    public static float getModuleGridY(float baseY, int rowIndex) {
        return baseY + 15.0f + (float)rowIndex * 90.0f;
    }

    public static int getModulesPerRow() {
        return 4;
    }

    public static float calculateModuleHeight(Module module, float openProgress) {
        float baseHeight = 80.0f;
        if (module.isOpen()) {
            return baseHeight + ClickGuiLayout.calculateSettingsHeight(module) * openProgress;
        }
        return baseHeight;
    }

    public static float calculateSettingsHeight(Module module) {
        float height = 0.0f;
        List<Setting> settings = module.getSettings();
        if (settings == null || settings.isEmpty()) {
            return 0.0f;
        }
        for (Setting setting : settings) {
            if (setting == null || !setting.visible().booleanValue()) continue;
            height += 28.0f;
        }
        return height;
    }

    public static float getModuleHeight(Module module, float openProgress) {
        return ClickGuiLayout.calculateModuleHeight(module, openProgress);
    }

    public static float getTotalCategoriesWidth(int categoryCount) {
        return 470.0f * (float)categoryCount + 8.0f * (float)(categoryCount - 1);
    }

    public static float getCategoryPanelX(float x2, int index) {
        return x2 + (float)index * 108.0f;
    }

    public static float getSearchX(float x2, int categoryCount) {
        return x2 + ClickGuiLayout.getTotalCategoriesWidth(categoryCount) / 2.0f - 37.5f;
    }

    public static float getSearchX(float x2, int categoryCount, float searchWidth) {
        return x2 + ClickGuiLayout.getTotalCategoriesWidth(categoryCount) / 2.0f - searchWidth / 2.0f;
    }

    public static float getSearchY(float y2) {
        return y2 + 260.0f + 8.0f;
    }

    public static boolean hasVisibleSettings(List<Setting> settings) {
        for (Setting setting : settings) {
            if (setting == null || !setting.visible().booleanValue()) continue;
            return true;
        }
        return false;
    }

    public static float calculateModeSettingHeight(ModeSetting modeSetting) {
        return ClickGuiLayout.calculateModeSettingHeight(modeSetting, ClickGuiLayout.getTagAvailableWidth());
    }

    public static float calculateModeSettingHeight(ModeSetting modeSetting, float availableWidth) {
        return ClickGuiLayout.calculateWrappedChipHeight(modeSetting.getMods(), availableWidth) + 12.0f;
    }

    public static float calculateListSettingHeight(ListSetting listSetting) {
        return ClickGuiLayout.calculateListSettingHeight(listSetting, ClickGuiLayout.getTagAvailableWidth());
    }

    public static float calculateListSettingHeight(ListSetting listSetting, float availableWidth) {
        ArrayList<String> labels = new ArrayList<String>();
        for (BooleanSetting entry : listSetting.getSettings()) {
            if (!entry.visible().booleanValue()) continue;
            labels.add(entry.name());
        }
        return ClickGuiLayout.calculateWrappedChipHeight(labels, availableWidth) + 12.0f;
    }

    public static float getTagAvailableWidth() {
        return ClickGuiLayout.columnWidth() - 16.0f;
    }

    public static float getTagHeight() {
        Font font = Fonts.getFont("suisse", 12);
        return (font != null ? font.getHeight() : 6.0f) + 6.0f;
    }

    public static float getTagChipHeight() {
        return ClickGuiLayout.getTagHeight() - 7.0f;
    }

    public static float calculateTagWidth(String text) {
        Font font = Fonts.getFont("suisse", 12);
        float textWidth = font != null ? font.getWidth(text) : (text != null ? (float)text.length() * 6.0f : 0.0f);
        return textWidth + 5.0f;
    }

    public static float calculateTagChipWidth(String text) {
        return Math.max(24.0f, ClickGuiLayout.calculateTagWidth(text) + 2.0f);
    }

    private static float calculateWrappedChipHeight(List<String> entries) {
        return ClickGuiLayout.calculateWrappedChipHeight(entries, ClickGuiLayout.getTagAvailableWidth());
    }

    private static float calculateWrappedChipHeight(List<String> entries, float availableWidth) {
        if (entries == null || entries.isEmpty()) {
            return ClickGuiLayout.getTagChipHeight();
        }
        Font font = Fonts.getFont("suisse", 12);
        float chipHeight = ClickGuiLayout.getTagChipHeight();
        float offsetX = 0.0f;
        float totalHeight = chipHeight;
        int modsInCurrentRow = 0;
        int maxModsPerRow = 2;
        for (String entry : entries) {
            float chipWidth = (font != null ? font.getWidth(entry) : (float)entry.length() * 6.0f) + 8.0f;
            if (offsetX > 0.0f && offsetX + chipWidth > availableWidth || modsInCurrentRow >= maxModsPerRow) {
                offsetX = 0.0f;
                totalHeight += chipHeight + 2.0f;
                modsInCurrentRow = 0;
            }
            offsetX += chipWidth + 3.0f;
            ++modsInCurrentRow;
        }
        return totalHeight;
    }
}

