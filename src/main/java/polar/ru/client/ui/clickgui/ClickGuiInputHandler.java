package polar.ru.client.ui.clickgui;

import java.util.List;
import net.minecraft.client.util.Window;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.LocalizationStorage;
import polar.ru.api.utils.input.KeyBoardUtils;
import polar.ru.api.utils.math.HoveringUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.Setting;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.client.modules.settings.implement.TextSetting;
import polar.ru.client.ui.clickgui.ClickGuiFiguraPanel;
import polar.ru.client.ui.clickgui.ClickGuiLayout;
import polar.ru.client.ui.clickgui.ClickGuiState;
import polar.ru.client.ui.clickgui.ClickGuiThemeSelector;
import polar.ru.polar;

public class ClickGuiInputHandler
implements QClient {
    private final ClickGuiState state;
    private final ClickGuiThemeSelector themeSelector;
    private final ClickGuiFiguraPanel figuraPanel;

    public ClickGuiInputHandler(ClickGuiState state, ClickGuiThemeSelector themeSelector, ClickGuiFiguraPanel figuraPanel) {
        this.state = state;
        this.themeSelector = themeSelector;
        this.figuraPanel = figuraPanel;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, Window window) {
        if (window != null && button == 0) {
            int categoryCount = Module.ModuleCategory.values().length;
            float searchW = this.getSearchWidth();
            float searchX = ClickGuiLayout.getSearchX(this.state.getX(), categoryCount, searchW) - 10.0f;
            float searchY = ClickGuiLayout.getSearchY(this.state.getY() + this.state.getRenderOffsetY());
            String langLabel = switch (polar.INSTANCE.localizationStorage.getLanguage()) {
                default -> throw new MatchException(null, null);
                case LocalizationStorage.Language.RUSSIAN -> "RU";
                case LocalizationStorage.Language.ENGLISH -> "EN";
                case LocalizationStorage.Language.UKRAINIAN -> "UA";
            };
            float btnW = this.issue(14).getWidth(langLabel) + 10.0f;
            float btnH = 18.0f;
            float btnX = searchX + searchW + 4.0f;
            if (HoveringUtils.isHovered(mouseX, mouseY, btnX, searchY, btnW, btnH)) {
                polar.INSTANCE.localizationStorage.cycleLanguage();
                return true;
            }
            boolean searchHovered = HoveringUtils.isHovered(mouseX, mouseY, searchX, searchY, searchW, 18.0);
            this.state.setSearchActive(searchHovered);
            if (searchHovered) {
                this.state.setEditingTextSetting(null);
                this.state.startSearchSelection(this.getSearchIndexAt(mouseX, searchX));
                return true;
            }
        }
        if (this.state.getBindingModule() != null && button >= 2) {
            this.state.getBindingModule().setKey(KeyBoardUtils.createMouseBind(button));
            this.state.setBindingModule(null);
            return true;
        }
        if (this.state.getBindingSetting() != null && button >= 2) {
            this.state.getBindingSetting().setKey(KeyBoardUtils.createMouseBind(button));
            this.state.setBindingSetting(null);
            return true;
        }
        this.state.setEditingTextSetting(null);
        if (this.themeSelector.handleClick(window, mouseX, mouseY, button, this.state.getRenderOffsetY())) {
            return true;
        }
        Module.ModuleCategory[] categories = Module.ModuleCategory.values();
        for (int i2 = 0; i2 < categories.length; ++i2) {
            float contentHeight;
            float contentY;
            Module.ModuleCategory category = categories[i2];
            float panelX = ClickGuiLayout.getCategoryPanelX(this.state.getX(), i2);
            if (!HoveringUtils.isHovered(mouseX, mouseY, panelX, contentY = ClickGuiLayout.getContentY(this.state.getY() + this.state.getRenderOffsetY()), 470.0, contentHeight = ClickGuiLayout.getContentHeight())) continue;
            if (category == Module.ModuleCategory.FIGURA) {
                if (!this.figuraPanel.handleClick(mouseX, mouseY, button, panelX, contentY, contentHeight)) continue;
                return true;
            }
            float moduleY = contentY + this.state.getScroll(category);
            for (Module module : this.state.getModules(category)) {
                List<Setting> settings;
                float openProgress = this.state.getOpenProgress(module);
                float moduleHeight = ClickGuiLayout.getModuleHeight(module, openProgress);
                if (HoveringUtils.isHovered(mouseX, mouseY, panelX + 15.0f, moduleY, 153.0, 20.0)) {
                    if (button == 0) {
                        module.toggle();
                        return true;
                    }
                    if (button == 1) {
                        module.setOpen(!module.isOpen());
                        this.state.clampScroll(category, contentHeight);
                        return true;
                    }
                    if (button == 2) {
                        this.state.setBindingModule(module);
                        return true;
                    }
                    return true;
                }
                if (module.isOpen() && openProgress > 0.1f && (settings = module.getSettings()) != null && this.handleSettingClick(mouseX, mouseY, button, panelX, moduleY, settings)) {
                    return true;
                }
                moduleY += 10.0f + moduleHeight;
            }
        }
        return false;
    }

    public boolean mouseReleased(int button) {
        this.state.stopSearchSelection();
        if (button == 0) {
            for (Module module : this.state.getAllModules()) {
                List<Setting> settings = module.getSettings();
                if (settings == null) continue;
                for (Setting setting : settings) {
                    if (!(setting instanceof FloatSetting)) continue;
                    FloatSetting floatSetting = (FloatSetting)setting;
                    floatSetting.setActive(false);
                    this.state.endSliderDrag(floatSetting);
                }
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || !this.state.isSearchActive() || !this.state.isSearchDragging()) {
            return false;
        }
        int categoryCount = Module.ModuleCategory.values().length;
        float searchX = ClickGuiLayout.getSearchX(this.state.getX(), categoryCount, this.getSearchWidth()) - 10.0f;
        this.state.updateSearchSelection(this.getSearchIndexAt(mouseX, searchX));
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        Module.ModuleCategory[] categories = Module.ModuleCategory.values();
        for (int i2 = 0; i2 < categories.length; ++i2) {
            float contentHeight;
            float contentY;
            Module.ModuleCategory category = categories[i2];
            float panelX = ClickGuiLayout.getCategoryPanelX(this.state.getX(), i2);
            if (!HoveringUtils.isHovered(mouseX, mouseY, panelX, contentY = ClickGuiLayout.getContentY(this.state.getY() + this.state.getRenderOffsetY()), 470.0, contentHeight = ClickGuiLayout.getContentHeight())) continue;
            if (category == Module.ModuleCategory.FIGURA) {
                if (!this.figuraPanel.handleScroll(mouseX, mouseY, panelX, contentY, contentHeight, verticalAmount)) continue;
                return true;
            }
            this.state.addScroll(category, verticalAmount, contentHeight);
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int modifiers) {
        if (this.state.getEditingTextSetting() != null) {
            TextSetting textSetting = this.state.getEditingTextSetting();
            if (keyCode == 256 || keyCode == 257 || keyCode == 335) {
                this.state.setEditingTextSetting(null);
                return true;
            }
            if (keyCode == 259) {
                String current = textSetting.get();
                if (current != null && !current.isEmpty()) {
                    textSetting.setText(current.substring(0, current.length() - 1));
                }
                return true;
            }
            return true;
        }
        if (this.state.isSearchActive()) {
            if ((modifiers & 2) != 0) {
                if (keyCode == 65) {
                    this.state.selectAllSearchText();
                    return true;
                }
                if (keyCode == 67) {
                    if (this.state.hasSearchSelection() && mc != null && ClickGuiInputHandler.mc.keyboard != null) {
                        ClickGuiInputHandler.mc.keyboard.setClipboard(this.state.getSelectedSearchText());
                    }
                    return true;
                }
                if (keyCode == 86) {
                    if (mc != null && ClickGuiInputHandler.mc.keyboard != null) {
                        this.state.replaceSearchSelection(ClickGuiInputHandler.mc.keyboard.getClipboard());
                    }
                    return true;
                }
                if (keyCode == 90) {
                    this.state.restoreSearchUndo();
                    return true;
                }
            }
            if (keyCode == 256 || keyCode == 257 || keyCode == 335) {
                this.state.setSearchActive(false);
                return true;
            }
            if (keyCode == 259) {
                this.state.removeLastSearchChar();
                return true;
            }
            if (keyCode == 261) {
                this.state.clearSearchText();
                return true;
            }
            if (keyCode == 263) {
                this.state.setSearchCursor(this.state.getSearchCursor() - 1, (modifiers & 1) != 0);
                return true;
            }
            if (keyCode == 262) {
                this.state.setSearchCursor(this.state.getSearchCursor() + 1, (modifiers & 1) != 0);
                return true;
            }
        }
        if (this.state.getBindingModule() != null) {
            if (keyCode == 256) {
                this.state.setBindingModule(null);
            } else if (keyCode == 261 || keyCode == 259) {
                this.state.getBindingModule().setKey(-1);
                this.state.setBindingModule(null);
            } else {
                this.state.getBindingModule().setKey(keyCode);
                this.state.setBindingModule(null);
            }
            return true;
        }
        if (this.state.getBindingSetting() != null) {
            if (keyCode == 256) {
                this.state.setBindingSetting(null);
            } else if (keyCode == 261 || keyCode == 259) {
                this.state.getBindingSetting().setKey(-1);
                this.state.setBindingSetting(null);
            } else {
                this.state.getBindingSetting().setKey(keyCode);
                this.state.setBindingSetting(null);
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (this.state.getEditingTextSetting() != null) {
            if (!Character.isISOControl(chr)) {
                TextSetting textSetting = this.state.getEditingTextSetting();
                textSetting.setText(textSetting.get() + chr);
            }
            return true;
        }
        if (!this.state.isSearchActive()) {
            return false;
        }
        this.state.appendSearchChar(chr);
        return true;
    }

    private int getSearchIndexAt(double mouseX, float searchX) {
        String text = this.state.getSearchText();
        float textX = searchX + 19.0f;
        float localX = (float)mouseX - textX;
        if (localX <= 0.0f || text.isEmpty()) {
            return 0;
        }
        for (int i2 = 1; i2 <= text.length(); ++i2) {
            float currentWidth;
            float previousWidth = this.issue(14).getWidth(text.substring(0, i2 - 1));
            float midpoint = previousWidth + ((currentWidth = this.issue(14).getWidth(text.substring(0, i2))) - previousWidth) * 0.5f;
            if (!(localX < midpoint)) continue;
            return i2 - 1;
        }
        return text.length();
    }

    private float getSearchWidth() {
        String query = this.state.getSearchText();
        String text = query.isEmpty() ? "Search..." : query;
        float contentWidth = 19.0f + this.issue(14).getWidth(text) + 8.0f;
        return Math.max(75.0f, contentWidth);
    }

    private boolean handleSettingClick(double mouseX, double mouseY, int button, float panelX, float moduleY, List<Setting> settings) {
        float settingYoffset = 20.0f;
        Font chipFont = Fonts.getFont("suisse", 12);
        for (Setting setting : settings) {
            float chipWidth;
            float offsetY;
            float offsetX;
            float availableWidth;
            float chipHeight;
            float chipY;
            if (setting == null || !setting.visible().booleanValue()) continue;
            float settingY = moduleY + settingYoffset + 8.0f;
            if (setting instanceof BooleanSetting) {
                BooleanSetting booleanSetting = (BooleanSetting)setting;
                float toggleX = panelX + 75.0f;
                float toggleY = settingY - 2.0f - 3.0f;
                if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, toggleX, toggleY, 16.0, 9.0)) {
                    booleanSetting.setState(!booleanSetting.isState());
                    return true;
                }
                settingYoffset += 12.0f;
                continue;
            }
            if (setting instanceof TextSetting) {
                TextSetting textSetting = (TextSetting)setting;
                float boxX = panelX + 49.0f;
                float boxY = settingY - 2.5f - 3.0f;
                if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, boxX, boxY, 60.0, 9.0)) {
                    this.state.setSearchActive(false);
                    this.state.stopSearchSelection();
                    this.state.setEditingTextSetting(textSetting);
                    return true;
                }
                settingYoffset += 22.0f;
                continue;
            }
            if (setting instanceof FloatSetting) {
                FloatSetting floatSetting = (FloatSetting)setting;
                float sliderX = panelX + 8.0f;
                float sliderY = settingY - 3.0f + 9.0f;
                if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, sliderX, sliderY, 153.0, 4.5)) {
                    floatSetting.setActive(true);
                    floatSetting.setValue(this.state.getSliderValue(floatSetting, sliderX, mouseX));
                    this.state.beginSliderDrag(floatSetting, mouseX);
                    return true;
                }
                settingYoffset += 22.0f;
                continue;
            }
            if (setting instanceof ModeSetting) {
                ModeSetting modeSetting = (ModeSetting)setting;
                float chipX = panelX + 8.0f - 1.5f;
                chipY = settingY + 10.0f - 0.5f;
                chipHeight = ClickGuiLayout.getTagChipHeight();
                availableWidth = ClickGuiLayout.getTagAvailableWidth();
                offsetX = 0.0f;
                offsetY = 0.0f;
                int modsInCurrentRow = 0;
                int maxModsPerRow = 2;
                for (String mode : modeSetting.getMods()) {
                    String translatedMode = this.translate(mode);
                    chipWidth = (chipFont != null ? chipFont.getWidth(translatedMode) : (float)translatedMode.length() * 6.0f) + 8.0f;
                    if (offsetX > 0.0f && offsetX + chipWidth > availableWidth || modsInCurrentRow >= maxModsPerRow) {
                        offsetX = 0.0f;
                        offsetY += chipHeight + 2.0f;
                        modsInCurrentRow = 0;
                    }
                    if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, chipX + offsetX, chipY + offsetY - 5.0f, chipWidth, chipHeight)) {
                        modeSetting.set(mode);
                        return true;
                    }
                    offsetX += chipWidth + 3.0f;
                    ++modsInCurrentRow;
                }
                settingYoffset += ClickGuiLayout.calculateModeSettingHeight(modeSetting);
                continue;
            }
            if (setting instanceof ListSetting) {
                ListSetting listSetting = (ListSetting)setting;
                float chipX = panelX + 8.0f - 1.5f;
                chipY = settingY + 10.0f - 0.5f;
                chipHeight = ClickGuiLayout.getTagChipHeight();
                availableWidth = ClickGuiLayout.getTagAvailableWidth();
                offsetX = 0.0f;
                offsetY = 0.0f;
                int itemsInCurrentRow = 0;
                int maxItemsPerRow = 2;
                for (BooleanSetting entry : listSetting.getSettings()) {
                    if (!entry.visible().booleanValue()) continue;
                    String translatedEntry = this.translate(entry.name());
                    chipWidth = (chipFont != null ? chipFont.getWidth(translatedEntry) : (float)translatedEntry.length() * 6.0f) + 8.0f;
                    if (offsetX > 0.0f && offsetX + chipWidth > availableWidth || itemsInCurrentRow >= maxItemsPerRow) {
                        offsetX = 0.0f;
                        offsetY += chipHeight + 2.0f;
                        itemsInCurrentRow = 0;
                    }
                    if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, chipX + offsetX, chipY + offsetY - 5.0f, chipWidth, chipHeight)) {
                        entry.setState(!entry.isState());
                        return true;
                    }
                    offsetX += chipWidth + 3.0f;
                    ++itemsInCurrentRow;
                }
                settingYoffset += ClickGuiLayout.calculateListSettingHeight(listSetting);
                continue;
            }
            if (!(setting instanceof BindSetting)) continue;
            BindSetting bindSetting = (BindSetting)setting;
            String bindString = this.state.getBindingSetting() == bindSetting ? "..." : this.state.toEnglish(KeyBoardUtils.getBindName(bindSetting.getKey()));
            float bindWidth = this.issue(12).getWidth(bindString) + 6.0f;
            float bindX = panelX + 161.0f - bindWidth;
            float bindY = settingY - 2.5f - 3.0f;
            if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, bindX, bindY, bindWidth, 9.0)) {
                this.state.setBindingSetting(bindSetting);
                return true;
            }
            settingYoffset += 12.0f;
        }
        return false;
    }

    private String translate(String key) {
        if (polar.INSTANCE == null || polar.INSTANCE.localizationStorage == null) {
            return key;
        }
        return polar.INSTANCE.localizationStorage.translate(key);
    }

    private Font issue(int size) {
        return Fonts.getFont("suisse", size);
    }
}

