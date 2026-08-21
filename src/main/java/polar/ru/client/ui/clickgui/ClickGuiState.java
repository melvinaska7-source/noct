package polar.ru.client.ui.clickgui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.util.Window;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.TextSetting;
import polar.ru.client.ui.clickgui.ClickGuiLayout;

public class ClickGuiState {
    private static final Map<Character, Character> RU_TO_EN = new HashMap<Character, Character>();
    private final Map<Module, Float> dotsRotation = new HashMap<Module, Float>();
    private final Map<Module, AnimationUtils> moduleOpenAnimation = new HashMap<Module, AnimationUtils>();
    private final Map<BooleanSetting, AnimationUtils> booleanBackgroundAnimation = new HashMap<BooleanSetting, AnimationUtils>();
    private final Map<BooleanSetting, AnimationUtils> booleanCircleAnimation = new HashMap<BooleanSetting, AnimationUtils>();
    private final Map<FloatSetting, AnimationUtils> sliderAnimation = new HashMap<FloatSetting, AnimationUtils>();
    private final Map<FloatSetting, Double> sliderDragMouseX = new HashMap<FloatSetting, Double>();
    private final Map<FloatSetting, Double> sliderDragRemainder = new HashMap<FloatSetting, Double>();
    private final Map<String, AnimationUtils> modeAnimation = new HashMap<String, AnimationUtils>();
    private final Map<String, AnimationUtils> listAnimation = new HashMap<String, AnimationUtils>();
    private final Map<String, AnimationUtils> bindAnimation = new HashMap<String, AnimationUtils>();
    private final Map<String, AnimationUtils> textHoverAnimation = new HashMap<String, AnimationUtils>();
    private final Map<String, Float> textScrollPhase = new HashMap<String, Float>();
    private final Map<String, Boolean> textScrollFinishing = new HashMap<String, Boolean>();
    private final Map<String, Boolean> textScrollHovered = new HashMap<String, Boolean>();
    private final Map<Module.ModuleCategory, Float> categoryScrollTarget = new EnumMap<Module.ModuleCategory, Float>(Module.ModuleCategory.class);
    private final Map<Module.ModuleCategory, AnimationUtils> categoryScrollAnimation = new EnumMap<Module.ModuleCategory, AnimationUtils>(Module.ModuleCategory.class);
    private final Map<Module.ModuleCategory, AnimationUtils> categorySwitchAnimation = new EnumMap<Module.ModuleCategory, AnimationUtils>(Module.ModuleCategory.class);
    private final Map<Module.ModuleCategory, AnimationUtils> moduleAppearAnimation = new EnumMap<Module.ModuleCategory, AnimationUtils>(Module.ModuleCategory.class);
    private final Map<Module.ModuleCategory, List<Module>> modulesByCategory = new EnumMap<Module.ModuleCategory, List<Module>>(Module.ModuleCategory.class);
    private final List<Module> allModules = new ArrayList<Module>();
    private final Map<String, AnimationUtils> hoverAnimations = new HashMap<String, AnimationUtils>();
    private final AnimationUtils categoryIndicatorYAnim = new AnimationUtils(0.0f, 12.0f, Easings.CUBIC_OUT);
    private Module.ModuleCategory selectedCategory;
    private FloatSetting activeSlider;
    private float x;
    private float y;
    private BindSetting bindingSetting;
    private TextSetting editingTextSetting;
    private Module bindingModule;
    private float renderOffsetY;
    private boolean searchActive;
    private String searchText = "";
    private String undoSearchText = "";
    private int searchCursor = 0;
    private int searchSelectionAnchor = 0;
    private int searchSelectionCursor = 0;
    private boolean searchDragging;

    public ClickGuiState() {
        Module.ModuleCategory[] categories = Module.ModuleCategory.values();
        if (categories.length > 0) {
            this.selectedCategory = categories[0];
        }
        this.refreshModules();
    }

    public void refreshModules() {
        this.allModules.clear();
        this.allModules.addAll(ModuleClass.INSTANCE.getObject().stream().filter(module -> !"AutoForest".equals(module.getName())).toList());
        for (Module.ModuleCategory category : Module.ModuleCategory.values()) {
            this.modulesByCategory.put(category, this.allModules.stream().filter(module -> module.getCategory() == category).toList());
            this.categoryScrollTarget.putIfAbsent(category, Float.valueOf(0.0f));
            this.categoryScrollAnimation.putIfAbsent(category, new AnimationUtils(0.0f, 8.0f, Easings.CUBIC_OUT));
            this.categorySwitchAnimation.putIfAbsent(category, new AnimationUtils(category == this.selectedCategory ? 1.0f : 0.0f, 8.0f, Easings.CUBIC_OUT));
            this.moduleAppearAnimation.putIfAbsent(category, new AnimationUtils(category == this.selectedCategory ? 1.0f : 0.0f, 10.0f, Easings.CUBIC_OUT));
        }
    }

    public void updatePosition(Window window, int categoryCount) {
        this.x = (float)window.getScaledWidth() / 2.0f - 235.0f;
        this.y = (float)window.getScaledHeight() / 2.0f - 165.0f;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getRenderOffsetY() {
        return this.renderOffsetY;
    }

    public void setRenderOffsetY(float renderOffsetY) {
        this.renderOffsetY = renderOffsetY;
    }

    public List<Module> getModules(Module.ModuleCategory category) {
        List<Module> modules = this.modulesByCategory.getOrDefault((Object)category, List.of());
        if (this.searchText.isBlank()) {
            return modules;
        }
        String query = this.searchText.toLowerCase(Locale.ROOT);
        return modules.stream().filter(module -> module.getName().toLowerCase(Locale.ROOT).contains(query) || module.getDisplayName().toLowerCase(Locale.ROOT).contains(query) || module.getDisplayDescription().toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    public List<Module> getAllModules() {
        return this.allModules;
    }

    public String toEnglish(String text) {
        StringBuilder result = new StringBuilder();
        for (char c2 : text.toCharArray()) {
            result.append(RU_TO_EN.getOrDefault(Character.valueOf(c2), Character.valueOf(c2)));
        }
        return result.toString();
    }

    public Module.ModuleCategory getSelectedCategory() {
        if (this.selectedCategory == null) {
            for (Module.ModuleCategory c2 : Module.ModuleCategory.values()) {
                if ("LUA".equals(c2.name())) continue;
                this.selectedCategory = c2;
                break;
            }
        }
        return this.selectedCategory;
    }

    public void setSelectedCategory(Module.ModuleCategory category) {
        this.selectedCategory = category;
        for (Module.ModuleCategory cat : Module.ModuleCategory.values()) {
            AnimationUtils anim = this.categorySwitchAnimation.computeIfAbsent(cat, unused -> new AnimationUtils(cat == category ? 1.0f : 0.0f, 8.0f, Easings.CUBIC_OUT));
            anim.update(cat == category ? 1.0f : 0.0f);
            AnimationUtils appearAnim = this.moduleAppearAnimation.computeIfAbsent(cat, unused -> new AnimationUtils(cat == category ? 1.0f : 0.0f, 10.0f, Easings.CUBIC_OUT));
            appearAnim.update(cat == category ? 1.0f : 0.0f);
        }
    }

    public AnimationUtils getCategorySwitchAnimation(Module.ModuleCategory category) {
        return this.categorySwitchAnimation.computeIfAbsent(category, unused -> new AnimationUtils(category == this.selectedCategory ? 1.0f : 0.0f, 8.0f, Easings.CUBIC_OUT));
    }

    public AnimationUtils getModuleAppearAnimation(Module.ModuleCategory category) {
        return this.moduleAppearAnimation.computeIfAbsent(category, unused -> new AnimationUtils(category == this.selectedCategory ? 1.0f : 0.0f, 10.0f, Easings.CUBIC_OUT));
    }

    public AnimationUtils getHoverAnimation(String key, boolean hovered) {
        AnimationUtils animation = this.hoverAnimations.computeIfAbsent(key, unused -> new AnimationUtils(0.0f, 16.0f, Easings.SINE_OUT));
        animation.update(hovered ? 1.0f : 0.0f);
        return animation;
    }

    public AnimationUtils getCategoryIndicatorYAnim() {
        return this.categoryIndicatorYAnim;
    }

    public FloatSetting getActiveSlider() {
        return this.activeSlider;
    }

    public void setActiveSlider(FloatSetting activeSlider) {
        this.activeSlider = activeSlider;
    }

    public void setScrollTarget(Module.ModuleCategory category, float target) {
        this.categoryScrollTarget.put(category, Float.valueOf(Math.min(0.0f, target)));
    }

    public void addScrollPixels(Module.ModuleCategory category, float deltaPx, float viewHeight, float totalHeight) {
        float maxScroll = Math.min(0.0f, viewHeight - totalHeight);
        float current = this.categoryScrollTarget.getOrDefault((Object)category, Float.valueOf(0.0f)).floatValue();
        float next = current + deltaPx;
        this.categoryScrollTarget.put(category, Float.valueOf(Math.max(maxScroll, Math.min(0.0f, next))));
    }

    public void clampScrollPixels(Module.ModuleCategory category, float viewHeight, float totalHeight) {
        float maxScroll = Math.min(0.0f, viewHeight - totalHeight);
        float current = this.categoryScrollTarget.getOrDefault((Object)category, Float.valueOf(0.0f)).floatValue();
        if (current < maxScroll || current > 0.0f) {
            this.categoryScrollTarget.put(category, Float.valueOf(Math.max(maxScroll, Math.min(0.0f, current))));
        }
    }

    public float getSliderPos(FloatSetting setting) {
        float delta = setting.getMax() - setting.getMin();
        return (setting.get() - setting.getMin()) / delta;
    }

    public float getSliderValue(FloatSetting setting, float posX, double mouseX) {
        return this.getSliderValue(setting, posX, mouseX, 153.0f);
    }

    public float getSliderValue(FloatSetting setting, float posX, double mouseX, float trackWidth) {
        float delta = setting.getMax() - setting.getMin();
        float clickedX = (float)mouseX - posX;
        float value = Math.max(0.0f, Math.min(1.0f, clickedX / trackWidth));
        float outValue = setting.getMin() + delta * value;
        float increment = setting.getIncrement();
        outValue = (float)Math.round(outValue / increment) * increment;
        return Math.max(setting.getMin(), Math.min(setting.getMax(), outValue));
    }

    public void beginSliderDrag(FloatSetting setting, double mouseX) {
        this.sliderDragMouseX.put(setting, mouseX);
        this.sliderDragRemainder.put(setting, 0.0);
    }

    public void endSliderDrag(FloatSetting setting) {
        this.sliderDragMouseX.remove(setting);
        this.sliderDragRemainder.remove(setting);
    }

    public float updateActiveSliderValue(FloatSetting setting, double mouseX) {
        return this.updateActiveSliderValue(setting, mouseX, 153.0f);
    }

    public float updateActiveSliderValue(FloatSetting setting, double mouseX, float trackWidth) {
        double lastMouseX = this.sliderDragMouseX.getOrDefault(setting, mouseX);
        this.sliderDragMouseX.put(setting, mouseX);
        double deltaX = mouseX - lastMouseX;
        if (Math.abs(deltaX) < 1.0E-4) {
            return setting.get();
        }
        float range = setting.getMax() - setting.getMin();
        float increment = setting.getIncrement();
        if (range <= 0.0f || increment <= 0.0f) {
            return setting.get();
        }
        double steps = range / increment;
        if (steps <= 0.0) {
            return setting.get();
        }
        double pixelsPerStep = (double)trackWidth / steps;
        if (pixelsPerStep <= 0.0) {
            return setting.get();
        }
        double accumulated = this.sliderDragRemainder.getOrDefault(setting, 0.0) + deltaX;
        int wholeSteps = (int)(accumulated / pixelsPerStep);
        if (wholeSteps == 0) {
            this.sliderDragRemainder.put(setting, accumulated);
            return setting.get();
        }
        this.sliderDragRemainder.put(setting, accumulated - (double)wholeSteps * pixelsPerStep);
        float value = setting.get() + (float)wholeSteps * increment;
        value = (float)Math.round(value / increment) * increment;
        return Math.max(setting.getMin(), Math.min(setting.getMax(), value));
    }

    public float getScroll(Module.ModuleCategory category) {
        AnimationUtils animation = this.categoryScrollAnimation.computeIfAbsent(category, key -> new AnimationUtils(0.0f, 8.0f, Easings.CUBIC_OUT));
        animation.update(this.categoryScrollTarget.getOrDefault((Object)category, Float.valueOf(0.0f)).floatValue());
        return animation.getValue();
    }

    public void clampScroll(Module.ModuleCategory category, float contentHeight) {
        float totalHeight = this.getTotalModulesHeight(category);
        float maxScroll = Math.min(0.0f, contentHeight - totalHeight);
        float currentTarget = this.categoryScrollTarget.getOrDefault((Object)category, Float.valueOf(0.0f)).floatValue();
        if (currentTarget < maxScroll || currentTarget > 0.0f) {
            this.categoryScrollTarget.put(category, Float.valueOf(Math.max(maxScroll, Math.min(0.0f, currentTarget))));
        }
    }

    public void addScroll(Module.ModuleCategory category, double verticalAmount, float contentHeight) {
        float totalHeight = this.getTotalModulesHeight(category);
        float maxScroll = Math.min(0.0f, contentHeight - totalHeight);
        float currentTarget = this.categoryScrollTarget.getOrDefault((Object)category, Float.valueOf(0.0f)).floatValue();
        float newTarget = currentTarget + (float)(verticalAmount * 20.0);
        this.categoryScrollTarget.put(category, Float.valueOf(Math.max(maxScroll, Math.min(0.0f, newTarget))));
    }

    public float getTotalModulesHeight(Module.ModuleCategory category) {
        float totalHeight = 0.0f;
        for (Module module : this.getModules(category)) {
            totalHeight += 10.0f + ClickGuiLayout.getModuleHeight(module, this.getOpenProgress(module));
        }
        return totalHeight;
    }

    public float getOpenProgress(Module module) {
        AnimationUtils animation = this.moduleOpenAnimation.computeIfAbsent(module, key -> new AnimationUtils(module.isOpen() ? 1.0f : 0.0f, 14.0f, Easings.CUBIC_OUT));
        animation.update(module.isOpen() ? 1.0f : 0.0f);
        return animation.getValue();
    }

    public float updateDotsRotation(Module module, float targetAngle) {
        float currentAngle = this.dotsRotation.getOrDefault(module, Float.valueOf(targetAngle)).floatValue();
        if (Math.abs(targetAngle - (currentAngle += (targetAngle - currentAngle) * 0.06f)) < 0.001f) {
            currentAngle = targetAngle;
        }
        this.dotsRotation.put(module, Float.valueOf(currentAngle));
        return currentAngle;
    }

    public AnimationUtils getBooleanBackgroundAnimation(BooleanSetting setting) {
        return this.booleanBackgroundAnimation.computeIfAbsent(setting, key -> new AnimationUtils(setting.isState() ? 1.0f : 0.0f, 15.0f, Easings.CUBIC_OUT));
    }

    public AnimationUtils getBooleanCircleAnimation(BooleanSetting setting) {
        return this.booleanCircleAnimation.computeIfAbsent(setting, key -> new AnimationUtils(setting.isState() ? 1.0f : 0.0f, 8.2f, Easings.BACK_OUT));
    }

    public AnimationUtils getSliderAnimation(FloatSetting setting) {
        return this.sliderAnimation.computeIfAbsent(setting, key -> new AnimationUtils(this.getSliderPos(setting), 12.0f, Easings.CUBIC_OUT));
    }

    public AnimationUtils getModeAnimation(String key, boolean selected) {
        return this.modeAnimation.computeIfAbsent(key, unused -> new AnimationUtils(selected ? 1.0f : 0.0f, 10.0f, Easings.CUBIC_OUT));
    }

    public AnimationUtils getListAnimation(String key, boolean selected) {
        return this.listAnimation.computeIfAbsent(key, unused -> new AnimationUtils(selected ? 1.0f : 0.0f, 10.0f, Easings.CUBIC_OUT));
    }

    public AnimationUtils getBindAnimation(String key, boolean binding) {
        return this.bindAnimation.computeIfAbsent(key, unused -> new AnimationUtils(binding ? 1.0f : 0.0f, 10.0f, Easings.CUBIC_OUT));
    }

    public AnimationUtils getTextHoverAnimation(String key, boolean hovered) {
        AnimationUtils animation = this.textHoverAnimation.computeIfAbsent(key, unused -> new AnimationUtils(0.0f, 12.0f, Easings.CUBIC_OUT));
        animation.update(hovered ? 1.0f : 0.0f);
        return animation;
    }

    public float advanceTextScrollPhase(String key, boolean hovered) {
        float phase = this.textScrollPhase.getOrDefault(key, Float.valueOf(0.0f)).floatValue();
        boolean wasHovered = this.textScrollHovered.getOrDefault(key, false);
        boolean finishing = this.textScrollFinishing.getOrDefault(key, false);
        if (hovered) {
            if ((phase += 0.004f) > 1.0f) {
                phase -= 1.0f;
            }
            finishing = false;
        } else {
            if (wasHovered && phase > 0.0f) {
                finishing = true;
            }
            if (finishing && (phase += 0.004f) >= 1.0f) {
                phase = 0.0f;
                finishing = false;
            }
        }
        this.textScrollHovered.put(key, hovered);
        this.textScrollFinishing.put(key, finishing);
        this.textScrollPhase.put(key, Float.valueOf(phase));
        return phase;
    }

    public boolean isTextScrollActive(String key, boolean hovered) {
        return hovered || this.textScrollFinishing.getOrDefault(key, false) != false;
    }

    public BindSetting getBindingSetting() {
        return this.bindingSetting;
    }

    public void setBindingSetting(BindSetting bindingSetting) {
        this.bindingSetting = bindingSetting;
    }

    public Module getBindingModule() {
        return this.bindingModule;
    }

    public void setBindingModule(Module bindingModule) {
        this.bindingModule = bindingModule;
    }

    public TextSetting getEditingTextSetting() {
        return this.editingTextSetting;
    }

    public void setEditingTextSetting(TextSetting editingTextSetting) {
        this.editingTextSetting = editingTextSetting;
    }

    public boolean isSearchActive() {
        return this.searchActive;
    }

    public void setSearchActive(boolean searchActive) {
        this.searchActive = searchActive;
    }

    public String getSearchText() {
        return this.searchText;
    }

    public void appendSearchChar(char chr) {
        if (Character.isISOControl(chr) || this.searchText.length() >= 24 && !this.hasSearchSelection()) {
            return;
        }
        this.replaceSearchSelection(String.valueOf(chr));
    }

    public void removeLastSearchChar() {
        if (this.hasSearchSelection()) {
            this.replaceSearchSelection("");
            return;
        }
        if (this.searchCursor > 0) {
            this.rememberSearchUndo();
            this.searchText = this.searchText.substring(0, this.searchCursor - 1) + this.searchText.substring(this.searchCursor);
            --this.searchCursor;
            this.clearSearchSelection();
        }
    }

    public void clearSearchText() {
        this.rememberSearchUndo();
        this.searchText = "";
        this.searchCursor = 0;
        this.clearSearchSelection();
    }

    public void setSearchText(String searchText) {
        this.rememberSearchUndo();
        this.searchText = this.sanitizeSearchText(searchText);
        this.searchCursor = this.searchText.length();
        this.clearSearchSelection();
    }

    public void restoreSearchUndo() {
        String current = this.searchText;
        this.searchText = this.undoSearchText == null ? "" : this.undoSearchText;
        this.undoSearchText = current;
        this.searchCursor = this.searchText.length();
        this.clearSearchSelection();
    }

    public int getSearchCursor() {
        return this.searchCursor;
    }

    public int getSearchSelectionStart() {
        return Math.min(this.searchSelectionAnchor, this.searchSelectionCursor);
    }

    public int getSearchSelectionEnd() {
        return Math.max(this.searchSelectionAnchor, this.searchSelectionCursor);
    }

    public boolean hasSearchSelection() {
        return this.getSearchSelectionStart() != this.getSearchSelectionEnd();
    }

    public String getSelectedSearchText() {
        if (!this.hasSearchSelection()) {
            return "";
        }
        return this.searchText.substring(this.getSearchSelectionStart(), this.getSearchSelectionEnd());
    }

    public void selectAllSearchText() {
        this.searchSelectionAnchor = 0;
        this.searchSelectionCursor = this.searchText.length();
        this.searchCursor = this.searchText.length();
    }

    public void setSearchCursor(int cursor, boolean keepSelection) {
        this.searchCursor = this.clampSearchIndex(cursor);
        if (keepSelection) {
            this.searchSelectionCursor = this.searchCursor;
        } else {
            this.searchSelectionAnchor = this.searchCursor;
            this.searchSelectionCursor = this.searchCursor;
        }
    }

    public void startSearchSelection(int index) {
        this.searchSelectionAnchor = this.searchCursor = this.clampSearchIndex(index);
        this.searchSelectionCursor = this.searchCursor;
        this.searchDragging = true;
    }

    public void updateSearchSelection(int index) {
        if (!this.searchDragging) {
            return;
        }
        this.searchSelectionCursor = this.searchCursor = this.clampSearchIndex(index);
    }

    public void stopSearchSelection() {
        this.searchDragging = false;
    }

    public boolean isSearchDragging() {
        return this.searchDragging;
    }

    public void replaceSearchSelection(String text) {
        this.rememberSearchUndo();
        String insert = this.sanitizeSearchText(text);
        int selectionStart = this.getSearchSelectionStart();
        int selectionEnd = this.getSearchSelectionEnd();
        if (!this.hasSearchSelection()) {
            selectionStart = this.searchCursor;
            selectionEnd = this.searchCursor;
        }
        int available = Math.max(0, 24 - (this.searchText.length() - (selectionEnd - selectionStart)));
        if (insert.length() > available) {
            insert = insert.substring(0, available);
        }
        this.searchText = this.searchText.substring(0, selectionStart) + insert + this.searchText.substring(selectionEnd);
        this.searchCursor = selectionStart + insert.length();
        this.clearSearchSelection();
    }

    private void clearSearchSelection() {
        this.searchSelectionAnchor = this.searchCursor;
        this.searchSelectionCursor = this.searchCursor;
        this.searchDragging = false;
    }

    private int clampSearchIndex(int index) {
        return Math.max(0, Math.min(this.searchText.length(), index));
    }

    private void rememberSearchUndo() {
        this.undoSearchText = this.searchText;
    }

    private String sanitizeSearchText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i2 = 0; i2 < text.length() && builder.length() < 24; ++i2) {
            char chr = text.charAt(i2);
            if (Character.isISOControl(chr)) continue;
            builder.append(chr);
        }
        return builder.toString();
    }

    static {
        String ru = "йцукенгшщзхъфывапролджэячсмитьбюЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ";
        String en = "qwertyuiop[]asdfghjkl;'zxcvbnm,.QWERTYUIOP[]ASDFGHJKL;'ZXCVBNM,.";
        int length = Math.min(ru.length(), en.length());
        for (int i2 = 0; i2 < length; ++i2) {
            RU_TO_EN.put(Character.valueOf(ru.charAt(i2)), Character.valueOf(en.charAt(i2)));
        }
    }
}

