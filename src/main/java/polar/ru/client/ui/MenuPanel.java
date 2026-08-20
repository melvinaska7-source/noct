package polar.ru.client.ui;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import polar.ru.api.QClient;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.client.ClientSoundPlayer;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.Setting;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.client.modules.settings.implement.TextSetting;
import polar.ru.client.ui.clickgui.ClickGuiFiguraPanel;
import polar.ru.client.ui.clickgui.ClickGuiInputHandler;
import polar.ru.client.ui.clickgui.ClickGuiRenderer;
import polar.ru.client.ui.clickgui.ClickGuiSettingRenderer;
import polar.ru.client.ui.clickgui.ClickGuiState;
import polar.ru.client.ui.clickgui.ClickGuiThemeSelector;

public class MenuPanel
extends Screen
implements QClient {
    private static final float SLIDE_SPEED = 8.0f;
    private static final float ALPHA_SPEED = 5.0f;
    private static final float OPEN_SPEED = 7.5f;
    private static final float CLOSE_SPEED = 7.5f;
    private static final float SLIDE_OFFSET = 220.0f;
    private static final float PANEL_OFFSET_Y_MUL = 22.0f;
    private static final ClickGuiState SHARED_STATE = new ClickGuiState();
    private final int categoryCount = Module.ModuleCategory.values().length;
    private final ClickGuiState state = SHARED_STATE;
    private final ClickGuiThemeSelector themeSelector = new ClickGuiThemeSelector();
    private final ClickGuiFiguraPanel figuraPanel = new ClickGuiFiguraPanel();
    private final ClickGuiRenderer renderer = new ClickGuiRenderer(this.state, new ClickGuiSettingRenderer(), this.themeSelector, this.figuraPanel, this);
    private final ClickGuiInputHandler inputHandler = new ClickGuiInputHandler(this.state, this.themeSelector, this.figuraPanel);
    private final AnimationUtils openAnimation = new AnimationUtils(0.0f, 7.5f, Easings.CUBIC_OUT);
    private final Map<Module.ModuleCategory, AnimationUtils> panelSlideX = new EnumMap<Module.ModuleCategory, AnimationUtils>(Module.ModuleCategory.class);
    private final Map<Module.ModuleCategory, AnimationUtils> panelSlideY = new EnumMap<Module.ModuleCategory, AnimationUtils>(Module.ModuleCategory.class);
    private final Map<Module.ModuleCategory, AnimationUtils> panelAlpha = new EnumMap<Module.ModuleCategory, AnimationUtils>(Module.ModuleCategory.class);
    private boolean closing;
    private boolean closeSoundPlayed;

    public MenuPanel() {
        super(Text.of((String)"ClickGui"));
        this.state.refreshModules();
        this.initPanelAnimations();
    }

    private void initPanelAnimations() {
        for (Module.ModuleCategory category : Module.ModuleCategory.values()) {
            this.panelSlideX.put(category, new AnimationUtils(this.getStartOffX(category), 8.0f, Easings.QUINT_OUT));
            this.panelSlideY.put(category, new AnimationUtils(this.getStartOffY(category), 8.0f, Easings.QUINT_OUT));
            this.panelAlpha.put(category, new AnimationUtils(0.0f, 5.0f, Easings.BOUNCE_OUT));
        }
    }

    private void resetPanelSlides() {
        for (Module.ModuleCategory category : Module.ModuleCategory.values()) {
            AnimationUtils sx = this.panelSlideX.get((Object)category);
            AnimationUtils sy = this.panelSlideY.get((Object)category);
            AnimationUtils alpha = this.panelAlpha.get((Object)category);
            if (sx != null) {
                sx.setValue(this.getStartOffX(category));
            }
            if (sy != null) {
                sy.setValue(this.getStartOffY(category));
            }
            if (alpha == null) continue;
            alpha.setValue(0.0f);
        }
    }

    private float getStartOffX(Module.ModuleCategory category) {
        return switch (category) {
            case Module.ModuleCategory.COMBAT -> -220.0f;
            case Module.ModuleCategory.MISC, Module.ModuleCategory.FIGURA -> 220.0f;
            default -> 0.0f;
        };
    }

    private float getStartOffY(Module.ModuleCategory category) {
        return switch (category) {
            case Module.ModuleCategory.MOVEMENT, Module.ModuleCategory.PLAYER -> -220.0f;
            case Module.ModuleCategory.RENDER -> 220.0f;
            default -> 0.0f;
        };
    }

    public float getPanelSlideOffsetX(Module.ModuleCategory category) {
        AnimationUtils sx = this.panelSlideX.get((Object)category);
        if (sx == null) {
            return 0.0f;
        }
        if (!this.closing) {
            sx.update(0.0f);
        }
        return sx.getValue();
    }

    public float getPanelSlideOffsetY(Module.ModuleCategory category) {
        AnimationUtils sy = this.panelSlideY.get((Object)category);
        if (sy == null) {
            return 0.0f;
        }
        if (!this.closing) {
            sy.update(0.0f);
        }
        return sy.getValue();
    }

    public float getPanelAlpha(Module.ModuleCategory category) {
        AnimationUtils alpha = this.panelAlpha.get((Object)category);
        if (alpha == null) {
            return 1.0f;
        }
        if (!this.closing) {
            alpha.update(1.0f);
        }
        return MathHelper.clamp((float)alpha.getValue(), (float)0.0f, (float)1.0f);
    }

    private Window getWindow() {
        return mc == null ? null : mc.getWindow();
    }

    private void syncLayout() {
        Window window = this.getWindow();
        if (window != null) {
            this.state.updatePosition(window, this.categoryCount);
        }
    }

    protected void init() {
        super.init();
        this.resetPanelSlides();
        this.openAnimation.setValue(0.0f);
        this.openAnimation.setEasing(Easings.CUBIC_OUT);
        this.closing = false;
        this.closeSoundPlayed = false;
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Window window = this.getWindow();
        if (window == null) {
            return;
        }
        RenderUtils.beginLiquidBlurFrame();
        this.state.updatePosition(mc.getWindow(), this.categoryCount);
        this.updateAnimation();
        float progress = this.getAnimationProgress();
        if (this.closing && progress <= 0.001f) {
            if (mc != null) {
                mc.setScreen(null);
            }
            return;
        }
        this.state.setRenderOffsetY(this.getPanelOffsetY(progress));
        this.renderer.render(context, mouseX, mouseY, window, progress);
        super.render(context, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.closing) {
            return true;
        }
        this.syncLayout();
        this.state.setRenderOffsetY(this.getPanelOffsetY(this.getAnimationProgress()));
        for (int i2 = this.renderer.getRegions().size() - 1; i2 >= 0; --i2) {
            ClickGuiRenderer.Region r2 = this.renderer.getRegions().get(i2);
            if (!r2.contains(mouseX, mouseY)) continue;
            switch (r2.type) {
                case CATEGORY: {
                    this.state.setSelectedCategory(r2.category);
                    this.state.setScrollTarget(r2.category, 0.0f);
                    return true;
                }
                case SEARCH: {
                    this.state.setSearchActive(true);
                    this.state.setEditingTextSetting(null);
                    return true;
                }
                case MODULE_HEADER: {
                    if (button == 0) {
                        r2.module.setEnabled(!r2.module.isEnable());
                    } else if (button == 1) {
                        r2.module.setOpen(!r2.module.isOpen());
                    }
                    return true;
                }
                case TOGGLE: {
                    Setting setting = r2.setting;
                    if (setting instanceof BooleanSetting) {
                        BooleanSetting s2 = (BooleanSetting)setting;
                        s2.setState(!s2.isState());
                    }
                    return true;
                }
                case CHIP_MODE: {
                    Setting setting = r2.setting;
                    if (setting instanceof ModeSetting) {
                        ModeSetting s3 = (ModeSetting)setting;
                        if (r2.modeValue != null) {
                            s3.set(r2.modeValue);
                        }
                    }
                    return true;
                }
                case CHIP_LIST: {
                    if (r2.listEntry != null) {
                        r2.listEntry.setState(!r2.listEntry.isState());
                    }
                    return true;
                }
                case SLIDER: {
                    Setting setting = r2.setting;
                    if (setting instanceof FloatSetting) {
                        FloatSetting s4 = (FloatSetting)setting;
                        this.state.setActiveSlider(s4);
                        this.state.beginSliderDrag(s4, mouseX);
                        s4.setValue(this.state.getSliderValue(s4, r2.x, mouseX, r2.w));
                    }
                    return true;
                }
                case BIND: {
                    Setting setting = r2.setting;
                    if (setting instanceof BindSetting) {
                        BindSetting s5 = (BindSetting)setting;
                        this.state.setBindingSetting(s5);
                    }
                    return true;
                }
                case TEXT: {
                    Setting setting = r2.setting;
                    if (setting instanceof TextSetting) {
                        TextSetting s6 = (TextSetting)setting;
                        this.state.setEditingTextSetting(s6);
                        this.state.setSearchActive(false);
                    }
                    return true;
                }
                case TEXT_INPUT: {
                    Setting setting = r2.setting;
                    if (setting instanceof TextSetting) {
                        TextSetting s7 = (TextSetting)setting;
                        this.state.setEditingTextSetting(s7);
                        this.state.setSearchActive(false);
                    }
                    return true;
                }
            }
        }
        this.state.setSearchActive(false);
        this.state.setEditingTextSetting(null);
        if (this.state.getBindingSetting() != null) {
            this.state.setBindingSetting(null);
        }
        return this.inputHandler.mouseClicked(mouseX, mouseY, button, this.getWindow()) || super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.closing) {
            return true;
        }
        this.syncLayout();
        return this.inputHandler.mouseReleased(button) || super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.closing) {
            return true;
        }
        this.syncLayout();
        this.state.setRenderOffsetY(this.getPanelOffsetY(this.getAnimationProgress()));
        return this.inputHandler.mouseDragged(mouseX, mouseY, button) || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.closing) {
            return true;
        }
        this.syncLayout();
        this.state.setRenderOffsetY(this.getPanelOffsetY(this.getAnimationProgress()));
        return this.inputHandler.mouseScrolled(mouseX, mouseY, verticalAmount) || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.closing) {
            return true;
        }
        if (this.inputHandler.keyPressed(keyCode, modifiers)) {
            return true;
        }
        if (keyCode == 256) {
            this.startClosing();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        if (this.closing) {
            return true;
        }
        return this.inputHandler.charTyped(chr) || super.charTyped(chr, modifiers);
    }

    public void close() {
        this.startClosing();
    }

    public void removed() {
        if (!this.closeSoundPlayed) {
            this.closeSoundPlayed = true;
            ClientSoundPlayer.playSound("closegui.wav", 0.6, 1.0f);
        }
        super.removed();
    }

    private void startClosing() {
        if (this.closing) {
            return;
        }
        this.closing = true;
        this.openAnimation.setEasing(Easings.CUBIC_IN);
        if (!this.closeSoundPlayed) {
            this.closeSoundPlayed = true;
            ClientSoundPlayer.playSound("closegui.wav", 0.6, 1.0f);
        }
    }

    private void updateAnimation() {
        if (this.closing) {
            this.openAnimation.update(0.0f);
        } else {
            this.openAnimation.setEasing(Easings.CUBIC_OUT);
            this.openAnimation.update(1.0f);
        }
    }

    private float getAnimationProgress() {
        return MathHelper.clamp((float)this.openAnimation.getValue(), (float)0.0f, (float)1.0f);
    }

    private float getPanelOffsetY(float progress) {
        return (1.0f - progress) * 22.0f;
    }
}

