package polar.ru.client.ui.clickgui;

import net.minecraft.client.util.Window;
import polar.ru.api.QClient;
import polar.ru.api.utils.input.KeyBoardUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.Setting;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.client.modules.settings.implement.TextSetting;

public class ClickGuiInputHandler implements QClient {
    private final ClickGuiState state;
    private final ClickGuiThemeSelector themeSelector;
    private final ClickGuiFiguraPanel figuraPanel;
    private final ClickGuiRenderer renderer;

    public ClickGuiInputHandler(ClickGuiState state, ClickGuiThemeSelector themeSelector,
                                ClickGuiFiguraPanel figuraPanel, ClickGuiRenderer renderer) {
        this.state = state;
        this.themeSelector = themeSelector;
        this.figuraPanel = figuraPanel;
        this.renderer = renderer;
    }

    public ClickGuiInputHandler(ClickGuiState state, ClickGuiThemeSelector themeSelector,
                                ClickGuiFiguraPanel figuraPanel) {
        this(state, themeSelector, figuraPanel,
                new ClickGuiRenderer(state, new ClickGuiSettingRenderer(), themeSelector, figuraPanel, null));
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, Window window) {
        if (state.getBindingModule() != null && button >= 2) {
            state.getBindingModule().setKey(KeyBoardUtils.createMouseBind(button));
            state.setBindingModule(null);
            return true;
        }
        if (state.getBindingSetting() != null && button >= 2) {
            state.getBindingSetting().setKey(KeyBoardUtils.createMouseBind(button));
            state.setBindingSetting(null);
            return true;
        }

        if (button == 0) {
            for (ClickGuiRenderer.Region r : renderer.getRegions()) {
                if (r.type == ClickGuiRenderer.Region.Type.SEARCH && r.contains(mouseX, mouseY)) {
                    state.setEditingTextSetting(null);
                    state.setSearchActive(true);
                    state.startSearchSelection(state.getSearchCursor());
                    return true;
                }
            }
        }

        state.setEditingTextSetting(null);

        ListSetting listSetting;
        for (int i = renderer.getRegions().size() - 1; i >= 0; --i) {
            ClickGuiRenderer.Region r = renderer.getRegions().get(i);
            if (!r.contains(mouseX, mouseY)) continue;

            switch (r.type) {
                case MODULE_HEADER -> {
                    if (r.module == null) return true;
                    if (button == 0) {
                        r.module.toggle();
                    } else if (button == 1) {
                        r.module.setOpen(!r.module.isOpen());
                    } else if (button == 2) {
                        state.setBindingModule(r.module);
                    }
                    return true;
                }
                case TOGGLE -> {
                    if (button == 0 && r.setting instanceof BooleanSetting b) {
                        b.setState(!b.isState());
                    }
                    return true;
                }
                case CHIP_MODE -> {
                    if (button == 0 && r.setting instanceof ModeSetting m && r.modeValue != null) {
                        m.set(r.modeValue);
                    }
                    return true;
                }
                case CHIP_LIST -> {
                    if (button == 0 && r.listEntry != null) {
                        r.listEntry.setState(!r.listEntry.isState());
                    }
                    return true;
                }
                case SLIDER -> {
                    if (button == 0 && r.setting instanceof FloatSetting f) {
                        f.setActive(true);
                        f.setValue(state.getSliderValue(f, r.x, mouseX, r.w));
                        state.beginSliderDrag(f, mouseX);
                    }
                    return true;
                }
                case BIND -> {
                    if (button == 0 && r.setting instanceof BindSetting b) {
                        state.setBindingSetting(b);
                    }
                    return true;
                }
                case TEXT_INPUT -> {
                    if (button == 0 && r.setting instanceof TextSetting t) {
                        state.setEditingTextSetting(t);
                    }
                    return true;
                }
                case CATEGORY -> {
                    if (button == 0 && r.category == Module.ModuleCategory.FIGURA) {
                        return figuraPanel.handleClick(mouseX, mouseY, button, r.x, r.y + 24, r.h - 28);
                    }
                    return true;
                }
                default -> {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mouseReleased(int button) {
        state.stopSearchSelection();
        if (button == 0) {
            for (Module module : state.getAllModules()) {
                if (module.getSettings() == null) continue;
                for (Setting setting : module.getSettings()) {
                    if (setting instanceof FloatSetting f) {
                        f.setActive(false);
                        state.endSliderDrag(f);
                    }
                }
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        for (ClickGuiRenderer.Region r : renderer.getRegions()) {
            if (r.type == ClickGuiRenderer.Region.Type.CATEGORY && r.contains(mouseX, mouseY)) {
                state.addScroll(r.category, verticalAmount, r.h - 29.0f);
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int modifiers) {
        if (state.getEditingTextSetting() != null) {
            TextSetting text = state.getEditingTextSetting();
            if (keyCode == 256 || keyCode == 257 || keyCode == 335) {
                state.setEditingTextSetting(null);
                return true;
            }
            if (keyCode == 259) {
                String current = text.get();
                if (current != null && !current.isEmpty()) text.setText(current.substring(0, current.length() - 1));
                return true;
            }
            return true;
        }

        if (state.isSearchActive()) {
            if (keyCode == 256 || keyCode == 257 || keyCode == 335) {
                state.setSearchActive(false);
                return true;
            }
            if (keyCode == 259) {
                state.removeLastSearchChar();
                return true;
            }
            if (keyCode == 261) {
                state.clearSearchText();
                return true;
            }
            if (keyCode == 263) {
                state.setSearchCursor(state.getSearchCursor() - 1, (modifiers & 1) != 0);
                return true;
            }
            if (keyCode == 262) {
                state.setSearchCursor(state.getSearchCursor() + 1, (modifiers & 1) != 0);
                return true;
            }
        }

        if (state.getBindingModule() != null) {
            if (keyCode == 256) {
                state.setBindingModule(null);
            } else if (keyCode == 261 || keyCode == 259) {
                state.getBindingModule().setKey(-1);
                state.setBindingModule(null);
            } else {
                state.getBindingModule().setKey(keyCode);
                state.setBindingModule(null);
            }
            return true;
        }

        if (state.getBindingSetting() != null) {
            if (keyCode == 256) {
                state.setBindingSetting(null);
            } else if (keyCode == 261 || keyCode == 259) {
                state.getBindingSetting().setKey(-1);
                state.setBindingSetting(null);
            } else {
                state.getBindingSetting().setKey(keyCode);
                state.setBindingSetting(null);
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (state.getEditingTextSetting() != null) {
            if (!Character.isISOControl(chr)) {
                TextSetting text = state.getEditingTextSetting();
                text.setText(text.get() + chr);
            }
            return true;
        }
        if (state.isSearchActive()) {
            state.appendSearchChar(chr);
            return true;
        }
        return false;
    }
}
