package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.math.HoveringUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.render.base.GlassSettings;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;
import polar.ru.client.modules.impl.render.base.implement.Cooldowns;
import polar.ru.client.modules.impl.render.base.implement.Hotbar;
import polar.ru.client.modules.impl.render.base.implement.Information;
import polar.ru.client.modules.impl.render.base.implement.ItemBinds;
import polar.ru.client.modules.impl.render.base.implement.KeyBinds;
import polar.ru.client.modules.impl.render.base.implement.Notifications;
import polar.ru.client.modules.impl.render.base.implement.Potions;
import polar.ru.client.modules.impl.render.base.implement.ScoreboardHud;
import polar.ru.client.modules.impl.render.base.implement.StaffList;
import polar.ru.client.modules.impl.render.base.implement.TargetHud;
import polar.ru.client.modules.impl.render.base.implement.WaterMark;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.polar;

public class Interface
extends Module {
    public static Interface INSTANCE = new Interface();
    private static final ConcurrentHashMap<String, Long> PERF_WARNINGS = new ConcurrentHashMap();
    private static final boolean PERF_DEBUG = Boolean.parseBoolean(System.getProperty("polar.perf.debug", "false"));
    private static final long SLOW_HUD_ELEMENT_NANOS = Long.getLong("polar.perf.hudMs", 5L) * 1000000L;
    private static final long PERF_WARN_COOLDOWN_NANOS = Long.getLong("polar.perf.cooldownMs", 1000L) * 1000000L;
    private final WaterMark waterMark;
    private final KeyBinds keyBinds;
    private final ItemBinds itemBinds;
    private final Potions potions;
    private final Cooldowns cooldowns;
    private final Notifications notifications;
    private final TargetHud targetHud;
    private final Information information;
    private final StaffList staffList;
    private final Hotbar hotbar;
    private final ScoreboardHud scoreboard;
    private boolean targetHudMenuOpen;
    private float targetHudMenuX;
    private float targetHudMenuY;
    private InterfaceProcessing hudContextElement;
    private InterfaceProcessing pendingHudContextElement;
    private float pendingTargetHudMenuX;
    private float pendingTargetHudMenuY;
    private final AnimationUtils targetHudMenuAnimation = new AnimationUtils(0.0f, 12.5f, Easings.CUBIC_OUT);
    private final AnimationUtils targetHudParticlesBgAnimation = new AnimationUtils(1.0f, 15.0f, Easings.CUBIC_OUT);
    private final AnimationUtils targetHudParticlesCircleAnimation = new AnimationUtils(1.0f, 8.2f, Easings.BACK_OUT);
    private final AnimationUtils targetHudBarSwitchAnimation = new AnimationUtils(0.0f, 7.0f, Easings.CUBIC_OUT);
    private final AnimationUtils waterMarkFpsBgAnimation = new AnimationUtils(1.0f, 15.0f, Easings.CUBIC_OUT);
    private final AnimationUtils waterMarkFpsCircleAnimation = new AnimationUtils(1.0f, 8.2f, Easings.BACK_OUT);
    private final AnimationUtils waterMarkMsBgAnimation = new AnimationUtils(1.0f, 15.0f, Easings.CUBIC_OUT);
    private final AnimationUtils waterMarkMsCircleAnimation = new AnimationUtils(1.0f, 8.2f, Easings.BACK_OUT);
    private final AnimationUtils notificationArmorBgAnimation = new AnimationUtils(1.0f, 15.0f, Easings.CUBIC_OUT);
    private final AnimationUtils notificationArmorCircleAnimation = new AnimationUtils(1.0f, 8.2f, Easings.BACK_OUT);
    private final AnimationUtils notificationPotionBgAnimation = new AnimationUtils(1.0f, 15.0f, Easings.CUBIC_OUT);
    private final AnimationUtils notificationPotionCircleAnimation = new AnimationUtils(1.0f, 8.2f, Easings.BACK_OUT);
    private final AnimationUtils notificationPickupBgAnimation = new AnimationUtils(1.0f, 15.0f, Easings.CUBIC_OUT);
    private final AnimationUtils notificationPickupCircleAnimation = new AnimationUtils(1.0f, 8.2f, Easings.BACK_OUT);
    private final AnimationUtils notificationTotemBgAnimation = new AnimationUtils(1.0f, 15.0f, Easings.CUBIC_OUT);
    private final AnimationUtils notificationTotemCircleAnimation = new AnimationUtils(1.0f, 8.2f, Easings.BACK_OUT);
    private static final String HUD_HINT_TEXT = "";
    private final ListSetting hudModules = new ListSetting("Элементы", new BooleanSetting("Ватермарка", true), new BooleanSetting("Горячие клавиши", true), new BooleanSetting("Бинды предметов", true), new BooleanSetting("Зелья", true), new BooleanSetting("Кулдауны", true), new BooleanSetting("Таргет худ", true), new BooleanSetting("Уведомления", true), new BooleanSetting("Стафф", true), new BooleanSetting("Информация", true), new BooleanSetting("Хотбар", true), new BooleanSetting("Скорборд", true));
    private final ListSetting hudStyle = new ListSetting("Стиль худа", new BooleanSetting("Hud 1", true), new BooleanSetting("Hud 2", false));
    private final BooleanSetting glassEnabled = new BooleanSetting("Жидкое стекло", false);
    private final FloatSetting glassAlpha = new FloatSetting("Прозрачность", 0.6f, 0.1f, 1.0f, 0.05f).visible(() -> this.glassEnabled.isState());
    private final FloatSetting glassRadius = new FloatSetting("Радиус", 6.0f, 2.0f, 15.0f, 0.5f).visible(() -> this.glassEnabled.isState());
    private final FloatSetting glassBlur = new FloatSetting("Размытие", 5.0f, 1.0f, 15.0f, 0.5f).visible(() -> this.glassEnabled.isState());
    private final FloatSetting glassTint = new FloatSetting("Оттенок", 200.0f, 0.0f, 360.0f, 5.0f).visible(() -> this.glassEnabled.isState());
    private final BooleanSetting glowEnabled = new BooleanSetting("Свечение", false);
    private final FloatSetting glowSpeed = new FloatSetting("Скорость свечения", 1.0f, 0.5f, 3.0f, 0.1f).visible(() -> this.glowEnabled.isState());
    private final FloatSetting glowWidth = new FloatSetting("Ширина свечения", 30.0f, 10.0f, 100.0f, 5.0f).visible(() -> this.glowEnabled.isState());
    private final FloatSetting glowPause = new FloatSetting("Пауза свечения", 3.0f, 1.0f, 10.0f, 0.5f).visible(() -> this.glowEnabled.isState());
    private final GlassSettings glassSettings = new GlassSettings();

    public Interface() {
        super("HUD", "Интерфейс клиента", Module.ModuleCategory.RENDER);
        this.waterMark = new WaterMark(polar.draggable(this, "WaterMark", 10.0f, 10.0f));
        this.keyBinds = new KeyBinds(polar.draggable(this, "KeyBinds", 30.0f, 30.0f));
        this.itemBinds = new ItemBinds(polar.draggable(this, "ItemBinds", 90.0f, 30.0f));
        this.potions = new Potions(polar.draggable(this, "Potions", 30.0f, 60.0f));
        this.cooldowns = new Cooldowns(polar.draggable(this, "Cooldowns", 30.0f, 90.0f));
        this.staffList = new StaffList(polar.draggable(this, "StaffList", 60.0f, 100.0f));
        this.information = new Information(polar.draggable(this, "Information", 50.0f, 100.0f));
        this.notifications = new Notifications(polar.draggable(this, "Notifications", 0.0f, 0.0f));
        this.targetHud = new TargetHud(polar.draggable(this, "TargetHud", 30.0f, 90.0f));
        this.hotbar = new Hotbar(polar.draggable(this, "Hotbar", 100.0f, 100.0f));
        this.scoreboard = new ScoreboardHud(polar.draggable(this, "Scoreboard", 150.0f, 100.0f));
        this.addSettings(this.hudModules, this.hudStyle, this.glassEnabled, this.glassAlpha, this.glassRadius, this.glassBlur, this.glassTint, this.glowEnabled, this.glowSpeed, this.glowWidth, this.glowPause);
    }

    private Font issue(int size) {
        return Fonts.getFont("suisse", size);
    }

    private int fadeColorSafe(int color, float progress, int minAlpha) {
        int faded = ColorUtils.applyAlpha(color, progress);
        int a2 = ColorUtils.getAlpha(faded);
        if (a2 == 0 && progress > 0.001f) {
            return ColorUtils.setAlphaColor(faded, minAlpha);
        }
        return faded;
    }

    private int fadeTextAlphaSafe(float progress, int maxAlpha, int minAlpha) {
        int alpha = MathHelper.clamp((int)((int)((float)maxAlpha * progress)), (int)0, (int)maxAlpha);
        if (alpha == 0 && progress > 0.001f) {
            return minAlpha;
        }
        return alpha;
    }

    private int getThemeColor() {
        if (!polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor();
    }

    public boolean isFlatStyle() {
        return this.hudStyle.is("Hud 2");
    }

    public boolean isHotbarEnabled() {
        return this.hudModules.is("Хотбар");
    }

    public boolean isScoreboardEnabled() {
        return this.hudModules.is("Скорборд");
    }

    public float getHotbarX() {
        return this.hotbar.draggable.getX();
    }

    public float getHotbarY() {
        return this.hotbar.draggable.getY();
    }

    private InterfaceProcessing getActiveTargetHud() {
        return this.targetHud;
    }

    private boolean isHudElementHovered(InterfaceProcessing element, double mouseX, double mouseY) {
        float width = element.draggable.getWidth();
        float height = element.draggable.getHeight();
        if (width <= 1.0f || height <= 1.0f) {
            return false;
        }
        return HoveringUtils.isHovered(mouseX, mouseY, element.draggable.getX(), element.draggable.getY(), width, height);
    }

    private boolean isHudElementEnabled(InterfaceProcessing element) {
        return element != null && element.draggable.getWidth() > 1.0f && element.draggable.getHeight() > 1.0f;
    }

    private InterfaceProcessing getHoveredHudElement(double mouseX, double mouseY) {
        InterfaceProcessing activeTarget = this.getActiveTargetHud();
        if (this.isHudElementEnabled(activeTarget) && this.isHudElementHovered(activeTarget, mouseX, mouseY)) {
            return activeTarget;
        }
        if (this.isHudElementEnabled(this.waterMark) && this.isHudElementHovered(this.waterMark, mouseX, mouseY)) {
            return this.waterMark;
        }
        if (this.isHudElementEnabled(this.keyBinds) && this.isHudElementHovered(this.keyBinds, mouseX, mouseY)) {
            return this.keyBinds;
        }
        if (this.isHudElementEnabled(this.itemBinds) && this.isHudElementHovered(this.itemBinds, mouseX, mouseY)) {
            return this.itemBinds;
        }
        if (this.isHudElementEnabled(this.potions) && this.isHudElementHovered(this.potions, mouseX, mouseY)) {
            return this.potions;
        }
        if (this.isHudElementEnabled(this.cooldowns) && this.isHudElementHovered(this.cooldowns, mouseX, mouseY)) {
            return this.cooldowns;
        }
        if (this.isHudElementEnabled(this.information) && this.isHudElementHovered(this.information, mouseX, mouseY)) {
            return this.information;
        }
        if (this.isHudElementEnabled(this.staffList) && this.isHudElementHovered(this.staffList, mouseX, mouseY)) {
            return this.staffList;
        }
        if (this.isHudElementEnabled(this.notifications) && this.isHudElementHovered(this.notifications, mouseX, mouseY)) {
            return this.notifications;
        }
        if (this.isHudElementEnabled(this.hotbar) && this.isHudElementHovered(this.hotbar, mouseX, mouseY)) {
            return this.hotbar;
        }
        if (this.isHudElementEnabled(this.scoreboard) && this.isHudElementHovered(this.scoreboard, mouseX, mouseY)) {
            return this.scoreboard;
        }
        return null;
    }

    private float getTargetHudMenuWidth() {
        return 100.0f;
    }

    private boolean hasHudContextSettings(InterfaceProcessing element) {
        return element == this.targetHud || element == this.waterMark || element == this.information || element == this.notifications;
    }

    private float getMenuHeightForElement(InterfaceProcessing element) {
        if (element == this.targetHud) {
            return 43.0f;
        }
        if (element == this.waterMark) {
            return 30.0f;
        }
        if (element == this.information) {
            return 24.0f;
        }
        if (element == this.notifications) {
            return 54.0f;
        }
        return 0.0f;
    }

    private float getTargetHudMenuHeight() {
        return this.getMenuHeightForElement(this.hudContextElement);
    }

    private void clampTargetHudMenuToWindow(float menuWidth, float menuHeight) {
        if (mc == null || mc.getWindow() == null) {
            return;
        }
        float maxX = Math.max(2.0f, (float)mc.getWindow().getScaledWidth() - menuWidth - 2.0f);
        float maxY = Math.max(2.0f, (float)mc.getWindow().getScaledHeight() - menuHeight - 2.0f);
        this.targetHudMenuX = MathHelper.clamp((float)this.targetHudMenuX, (float)2.0f, (float)maxX);
        this.targetHudMenuY = MathHelper.clamp((float)this.targetHudMenuY, (float)2.0f, (float)maxY);
    }

    public boolean handleHudContextClick(double mouseX, double mouseY, int button) {
        InterfaceProcessing hoveredElement = this.getHoveredHudElement(mouseX, mouseY);
        if (button == 1 && hoveredElement != null && this.hasHudContextSettings(hoveredElement)) {
            if (this.targetHudMenuOpen && this.hudContextElement == hoveredElement) {
                this.targetHudMenuOpen = false;
                this.pendingHudContextElement = null;
            } else if (this.targetHudMenuOpen && this.hudContextElement != null && this.hudContextElement != hoveredElement) {
                this.pendingHudContextElement = hoveredElement;
                float menuWidth = this.getTargetHudMenuWidth();
                float menuHeight = this.getMenuHeightForElement(hoveredElement);
                this.pendingTargetHudMenuX = hoveredElement.draggable.getX() + hoveredElement.draggable.getWidth() + 4.0f;
                this.pendingTargetHudMenuY = hoveredElement.draggable.getY() + 1.5f;
                float saveX = this.targetHudMenuX;
                float saveY = this.targetHudMenuY;
                this.targetHudMenuX = this.pendingTargetHudMenuX;
                this.targetHudMenuY = this.pendingTargetHudMenuY;
                this.clampTargetHudMenuToWindow(menuWidth, menuHeight);
                this.pendingTargetHudMenuX = this.targetHudMenuX;
                this.pendingTargetHudMenuY = this.targetHudMenuY;
                this.targetHudMenuX = saveX;
                this.targetHudMenuY = saveY;
                this.targetHudMenuOpen = false;
            } else {
                this.hudContextElement = hoveredElement;
                this.pendingHudContextElement = null;
                this.targetHudMenuOpen = true;
                float menuWidth = this.getTargetHudMenuWidth();
                float menuHeight = this.getTargetHudMenuHeight();
                this.targetHudMenuX = hoveredElement.draggable.getX() + hoveredElement.draggable.getWidth() + 4.0f;
                this.targetHudMenuY = hoveredElement.draggable.getY() + 1.5f;
                this.clampTargetHudMenuToWindow(menuWidth, menuHeight);
            }
            return true;
        }
        if (!this.targetHudMenuOpen || this.hudContextElement == null) {
            return false;
        }
        float menuWidth = this.getTargetHudMenuWidth();
        float menuHeight = this.getTargetHudMenuHeight();
        this.clampTargetHudMenuToWindow(menuWidth, menuHeight);
        float buttonGap = 3.0f;
        float buttonX = this.targetHudMenuX + 5.0f;
        float buttonW = (menuWidth - 10.0f - buttonGap) / 2.0f;
        float buttonH = 10.0f;
        float normalButtonX = buttonX;
        float unusualButtonX = buttonX + buttonW + buttonGap;
        boolean informationContext = this.hudContextElement == this.information;
        boolean targetHudContext = this.hudContextElement == this.targetHud;
        boolean notificationsContext = this.hudContextElement == this.notifications;
        boolean menuHovered = HoveringUtils.isHovered(mouseX, mouseY, this.targetHudMenuX, this.targetHudMenuY, menuWidth, menuHeight);
        if (button == 0 && !menuHovered && hoveredElement == this.hudContextElement) {
            this.targetHudMenuOpen = false;
            this.pendingHudContextElement = null;
            return false;
        }
        if (targetHudContext) {
            TargetHud ctx = (TargetHud)this.hudContextElement;
            float buttonY = this.targetHudMenuY + 25.0f;
            float particlesToggleX = this.targetHudMenuX + menuWidth - 21.0f;
            float particlesToggleY = this.targetHudMenuY + 4.0f;
            boolean normalHovered = HoveringUtils.isHovered(mouseX, mouseY, normalButtonX, buttonY, buttonW, buttonH);
            boolean unusualHovered = HoveringUtils.isHovered(mouseX, mouseY, unusualButtonX, buttonY, buttonW, buttonH);
            boolean particlesHovered = HoveringUtils.isHovered(mouseX, mouseY, particlesToggleX, particlesToggleY, 16.0, 9.0);
            if (button == 0 && normalHovered) {
                ctx.setHealthBarStyleEnabled(false);
                return true;
            }
            if (button == 0 && unusualHovered) {
                ctx.setHealthBarStyleEnabled(true);
                return true;
            }
            if (button == 0 && particlesHovered) {
                ctx.setHeadParticlesEnabled(!ctx.isHeadParticlesEnabled());
                return true;
            }
        } else if (this.hudContextElement == this.waterMark) {
            float baseY = this.targetHudMenuY + 4.5f;
            float toggleX = this.targetHudMenuX + menuWidth - 21.0f;
            boolean fpsHovered = HoveringUtils.isHovered(mouseX, mouseY, toggleX, baseY, 16.0, 9.0);
            boolean msHovered = HoveringUtils.isHovered(mouseX, mouseY, toggleX, baseY + 10.0f, 16.0, 9.0);
            if (button == 0 && fpsHovered) {
                this.waterMark.setShowFps(!this.waterMark.isShowFps());
                return true;
            }
            if (button == 0 && msHovered) {
                this.waterMark.setShowMs(!this.waterMark.isShowMs());
                return true;
            }
        } else if (informationContext) {
            float copyButtonY = this.targetHudMenuY + 4.5f;
            float copyButtonX = this.targetHudMenuX + 5.0f;
            float copyButtonW = menuWidth - 10.0f;
            float copyButtonH = 10.0f;
            boolean copyHovered = HoveringUtils.isHovered(mouseX, mouseY, copyButtonX, copyButtonY, copyButtonW, copyButtonH);
            if (button == 0 && copyHovered) {
                this.information.handleCopyClick();
                this.targetHudMenuOpen = false;
                return true;
            }
        } else if (notificationsContext) {
            Notifications ctx = (Notifications)this.hudContextElement;
            float baseY = this.targetHudMenuY + 4.5f;
            float toggleX = this.targetHudMenuX + menuWidth - 21.0f;
            boolean armorHovered = HoveringUtils.isHovered(mouseX, mouseY, toggleX, baseY, 16.0, 9.0);
            boolean potionHovered = HoveringUtils.isHovered(mouseX, mouseY, toggleX, baseY + 10.0f, 16.0, 9.0);
            boolean pickupHovered = HoveringUtils.isHovered(mouseX, mouseY, toggleX, baseY + 20.0f, 16.0, 9.0);
            boolean totemHovered = HoveringUtils.isHovered(mouseX, mouseY, toggleX, baseY + 30.0f, 16.0, 9.0);
            if (button == 0 && armorHovered) {
                ctx.getLowArmorNotify().setState(!ctx.getLowArmorNotify().isState());
                return true;
            }
            if (button == 0 && potionHovered) {
                ctx.getPotionExpireNotify().setState(!ctx.getPotionExpireNotify().isState());
                return true;
            }
            if (button == 0 && pickupHovered) {
                ctx.getItemPickupNotify().setState(!ctx.getItemPickupNotify().isState());
                return true;
            }
            if (button == 0 && totemHovered) {
                ctx.getTotemPopNotify().setState(!ctx.getTotemPopNotify().isState());
                return true;
            }
        }
        if (button == 0 || button == 1) {
            if (menuHovered) {
                return true;
            }
            if (hoveredElement != this.hudContextElement) {
                this.targetHudMenuOpen = false;
                this.pendingHudContextElement = null;
            }
        }
        return false;
    }

    public void renderHudContextMenu(DrawContext context, int mouseX, int mouseY) {
        if (this.hudContextElement != null && !this.isHudElementEnabled(this.hudContextElement)) {
            this.targetHudMenuOpen = false;
            this.hudContextElement = null;
            this.pendingHudContextElement = null;
        }
        this.targetHudMenuAnimation.update(this.targetHudMenuOpen ? 1.0f : 0.0f);
        float targetMenuProgress = MathHelper.clamp((float)this.targetHudMenuAnimation.getValue(), (float)0.0f, (float)1.0f);
        if (!this.targetHudMenuOpen && targetMenuProgress <= 0.01f) {
            if (this.pendingHudContextElement != null) {
                this.hudContextElement = this.pendingHudContextElement;
                this.pendingHudContextElement = null;
                this.targetHudMenuX = this.pendingTargetHudMenuX;
                this.targetHudMenuY = this.pendingTargetHudMenuY;
                this.targetHudMenuOpen = true;
            } else {
                this.hudContextElement = null;
            }
        }
        if (!this.targetHudMenuOpen && targetMenuProgress <= 0.01f && this.hudContextElement == null) {
            return;
        }
        if (this.hudContextElement == null) {
            return;
        }
        boolean targetHudContext = this.hudContextElement == this.targetHud;
        boolean waterMarkContext = this.hudContextElement == this.waterMark;
        boolean informationContext = this.hudContextElement == this.information;
        boolean notificationsContext = this.hudContextElement == this.notifications;
        float menuWidth = this.getTargetHudMenuWidth();
        float menuHeight = this.getTargetHudMenuHeight();
        this.clampTargetHudMenuToWindow(menuWidth, menuHeight);
        float x2 = this.targetHudMenuX;
        float y2 = this.targetHudMenuY;
        int themeColor = this.getThemeColor();
        float contentProgress = MathHelper.clamp((float)((targetMenuProgress - 0.06f) / 0.94f), (float)0.0f, (float)1.0f);
        int textAlpha = this.fadeTextAlphaSafe(contentProgress, 255, 2);
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        RenderUtils.drawDefaultHudPanel(matrices, x2, y2, menuWidth, menuHeight, 5.0f, 5.0f, ColorUtils.applyAlpha(ColorUtils.rgba(50, 50, 50, 255), targetMenuProgress), ColorUtils.applyAlpha(ColorUtils.darken(themeColor, 0.15f), targetMenuProgress), ColorUtils.applyAlpha(ColorUtils.darken(themeColor, 0.05f), targetMenuProgress));
        if (contentProgress <= 0.02f) {
            matrices.pop();
            return;
        }
        float buttonGap = 3.0f;
        float buttonX = x2 + 5.0f;
        float buttonW = (menuWidth - 10.0f - buttonGap) / 2.0f;
        float buttonH = 10.0f;
        float normalX = buttonX;
        float unusualX = buttonX + buttonW + buttonGap;
        int inactiveColor = ColorUtils.applyAlpha(ColorUtils.rgba(70, 70, 70, 255), contentProgress);
        int activeLeftColor = this.fadeColorSafe(ColorUtils.darken(themeColor, 0.4f), contentProgress, 2);
        int activeRightColor = this.fadeColorSafe(themeColor, contentProgress, 2);
        if (targetHudContext) {
            TargetHud ctx = (TargetHud)this.hudContextElement;
            boolean particlesEnabled = ctx.isHeadParticlesEnabled();
            boolean healthBarStyle = ctx.isHealthBarStyleEnabled();
            this.issue(12).drawStringWithFade(matrices, "Частицы с головы", x2 + 4.7f, y2 + 7.5f, menuWidth - 28.0f, ColorUtils.rgba(255, 255, 255, textAlpha));
            this.targetHudParticlesBgAnimation.update(particlesEnabled ? 1.0f : 0.0f);
            this.targetHudParticlesCircleAnimation.update(particlesEnabled ? 1.0f : 0.0f);
            float bgProgress = this.targetHudParticlesBgAnimation.getValue();
            float circleProgress = this.targetHudParticlesCircleAnimation.getValue();
            int particlesOffColor = ColorUtils.darken(themeColor, 0.05f);
            int particlesColor = ColorUtils.interpolateColor(particlesOffColor, themeColor, bgProgress);
            float particlesToggleX = x2 + menuWidth - 21.0f;
            float particlesToggleY = y2 + 4.5f;
            RenderUtils.drawGradientRect(matrices, particlesToggleX, particlesToggleY, 16.0f, 9.0f, 3.0f, this.fadeColorSafe(particlesColor, contentProgress, 2), this.fadeColorSafe(ColorUtils.darken(particlesColor, 0.65f), contentProgress, 2));
            float particlesCircleX = particlesToggleX + 4.5f + circleProgress * 6.2f;
            RenderUtils.drawRoundCircle(matrices, particlesCircleX + 0.5f, particlesToggleY + 4.5f, 6.85f, ColorUtils.rgba(255, 255, 255, textAlpha));
            this.issue(12).draw(matrices, "Вид полоски", x2 + 4.7f, y2 + 18.0f, ColorUtils.rgba(255, 255, 255, this.fadeTextAlphaSafe(contentProgress, 225, 2)));
            float buttonY = y2 + 25.0f;
            this.targetHudBarSwitchAnimation.update(healthBarStyle ? 1.0f : 0.0f);
            float typeSwitchProgress = MathHelper.clamp((float)this.targetHudBarSwitchAnimation.getValue(), (float)0.0f, (float)1.0f);
            RenderUtils.drawRoundedRect(matrices, normalX, buttonY, buttonW, buttonH, 1.5f, inactiveColor);
            RenderUtils.drawRoundedRect(matrices, unusualX, buttonY, buttonW, buttonH, 1.5f, inactiveColor);
            float activeX = MathHelper.lerp((float)typeSwitchProgress, (float)normalX, (float)unusualX);
            RenderUtils.drawGradientRect(matrices, activeX, buttonY, buttonW, buttonH, 1.5f, activeLeftColor, activeRightColor, true);
            String normalText = "Клиентский";
            String unusualText = "Здоровье";
            float normalTextX = normalX + (buttonW - this.issue(12).getWidth(normalText)) * 0.5f;
            float unusualTextX = unusualX + (buttonW - this.issue(12).getWidth(unusualText)) * 0.55f;
            int normalTextAlpha = MathHelper.clamp((int)((int)((float)textAlpha * (0.65f + 0.35f * (1.0f - typeSwitchProgress)))), (int)0, (int)255);
            int unusualTextAlpha = MathHelper.clamp((int)((int)((float)textAlpha * (0.65f + 0.35f * typeSwitchProgress))), (int)0, (int)255);
            float buttonTextY = buttonY + (buttonH - this.issue(12).getHeight()) / 2.0f;
            this.issue(12).draw(matrices, normalText, normalTextX, buttonTextY, ColorUtils.rgba(255, 255, 255, normalTextAlpha));
            this.issue(12).draw(matrices, unusualText, unusualTextX, buttonTextY, ColorUtils.rgba(255, 255, 255, unusualTextAlpha));
        } else if (waterMarkContext) {
            float wmToggleX = x2 + menuWidth - 21.0f;
            float wmBaseY = y2 + 3.5f;
            float wmLabelX = x2 + 5.0f;
            this.drawWaterMarkToggle(matrices, "Отображать фпс", wmLabelX, wmToggleX, wmBaseY, this.waterMark.isShowFps(), this.waterMarkFpsBgAnimation, this.waterMarkFpsCircleAnimation, themeColor, contentProgress, textAlpha);
            this.drawWaterMarkToggle(matrices, "Отображать пинг", wmLabelX, wmToggleX, wmBaseY + 10.0f, this.waterMark.isShowMs(), this.waterMarkMsBgAnimation, this.waterMarkMsCircleAnimation, themeColor, contentProgress, textAlpha);
        } else if (informationContext) {
            double my;
            float copyButtonX = x2 + 5.0f;
            float copyButtonY = y2 + 4.5f;
            float copyButtonW = menuWidth - 10.0f;
            float copyButtonH = 10.0f;
            double mx = Interface.mc.mouse.getX() / mc.getWindow().getScaleFactor();
            boolean copyHovered = HoveringUtils.isHovered(mx, my = Interface.mc.mouse.getY() / mc.getWindow().getScaleFactor(), copyButtonX, copyButtonY, copyButtonW, copyButtonH);
            int copyBgColor = copyHovered ? this.fadeColorSafe(themeColor, contentProgress, 2) : ColorUtils.applyAlpha(ColorUtils.rgba(70, 70, 70, 255), contentProgress);
            RenderUtils.drawRoundedRect(matrices, copyButtonX, copyButtonY, copyButtonW, copyButtonH, 1.5f, copyBgColor);
            String copyText = "Скопировать координаты.";
            float copyTextX = copyButtonX + (copyButtonW - this.issue(12).getWidth(copyText)) * 0.5f;
            float copyTextY = copyButtonY + (copyButtonH - this.issue(12).getHeight()) / 2.0f;
            this.issue(12).draw(matrices, copyText, copyTextX, copyTextY, ColorUtils.rgba(255, 255, 255, textAlpha));
        } else if (notificationsContext) {
            Notifications ctx = (Notifications)this.hudContextElement;
            float baseY = y2 + 3.5f;
            float labelX = x2 + 5.0f;
            float toggleX = x2 + menuWidth - 21.0f;
            this.drawNotificationToggle(matrices, "Уведомление о броне", labelX, toggleX, baseY, ctx.getLowArmorNotify().isState(), this.notificationArmorBgAnimation, this.notificationArmorCircleAnimation, themeColor, contentProgress, textAlpha);
            this.drawNotificationToggle(matrices, "Окончание зелий", labelX, toggleX, baseY + 10.0f, ctx.getPotionExpireNotify().isState(), this.notificationPotionBgAnimation, this.notificationPotionCircleAnimation, themeColor, contentProgress, textAlpha);
            this.drawNotificationToggle(matrices, "Подбор предметов", labelX, toggleX, baseY + 20.0f, ctx.getItemPickupNotify().isState(), this.notificationPickupBgAnimation, this.notificationPickupCircleAnimation, themeColor, contentProgress, textAlpha);
            this.drawNotificationToggle(matrices, "Поп тотема", labelX, toggleX, baseY + 30.0f, ctx.getTotemPopNotify().isState(), this.notificationTotemBgAnimation, this.notificationTotemCircleAnimation, themeColor, contentProgress, textAlpha);
        }
        matrices.pop();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderHudElement(InterfaceProcessing element, EventRender.Default event) {
        long start = PERF_DEBUG ? System.nanoTime() : 0L;
        element.draggable.beginRenderTilt(event.getContext().getMatrices());
        try {
            element.onRender(event);
        }
        finally {
            long elapsed;
            element.draggable.endRenderTilt(event.getContext().getMatrices());
            if (PERF_DEBUG && (elapsed = System.nanoTime() - start) >= SLOW_HUD_ELEMENT_NANOS) {
                this.logSlowHudElement(element, elapsed);
            }
        }
    }

    private void logSlowHudElement(InterfaceProcessing element, long elapsedNanos) {
        String name = element.getClass().getSimpleName();
        long now = System.nanoTime();
        Long lastWarn = PERF_WARNINGS.get(name);
        if (lastWarn != null && now - lastWarn < PERF_WARN_COOLDOWN_NANOS) {
            return;
        }
        PERF_WARNINGS.put(name, now);
        System.out.println(String.format(Locale.ROOT, "[PerfDebug] Slow HUD element: Interface -> %s took %.2f ms", name, (double)elapsedNanos / 1000000.0));
    }

    private void drawWaterMarkToggle(MatrixStack matrices, String label, float labelX, float toggleX, float toggleY, boolean enabled, AnimationUtils bgAnimation, AnimationUtils circleAnimation, int themeColor, float contentProgress, int textAlpha) {
        float labelY = toggleY + (9.0f - this.issue(12).getHeight()) / 2.0f;
        this.issue(12).draw(matrices, label, labelX, labelY, ColorUtils.rgba(255, 255, 255, textAlpha));
        bgAnimation.update(enabled ? 1.0f : 0.0f);
        circleAnimation.update(enabled ? 1.0f : 0.0f);
        float bgProgress = bgAnimation.getValue();
        float circleProgress = circleAnimation.getValue();
        int offColor = ColorUtils.darken(themeColor, 0.05f);
        int toggleColor = ColorUtils.interpolateColor(offColor, themeColor, bgProgress);
        RenderUtils.drawGradientRect(matrices, toggleX, toggleY, 16.0f, 9.0f, 3.0f, this.fadeColorSafe(toggleColor, contentProgress, 2), this.fadeColorSafe(ColorUtils.darken(toggleColor, 0.65f), contentProgress, 2));
        float circleX = toggleX + 4.5f + circleProgress * 6.2f;
        RenderUtils.drawRoundCircle(matrices, circleX + 0.5f, toggleY + 4.5f, 6.85f, ColorUtils.rgba(255, 255, 255, textAlpha));
    }

    private void drawNotificationToggle(MatrixStack matrices, String label, float labelX, float toggleX, float toggleY, boolean enabled, AnimationUtils bgAnimation, AnimationUtils circleAnimation, int themeColor, float contentProgress, int textAlpha) {
        float labelY = toggleY + (9.0f - this.issue(10).getHeight()) / 2.0f;
        this.issue(10).draw(matrices, label, labelX, labelY, ColorUtils.rgba(255, 255, 255, textAlpha));
        bgAnimation.update(enabled ? 1.0f : 0.0f);
        circleAnimation.update(enabled ? 1.0f : 0.0f);
        float bgProgress = bgAnimation.getValue();
        float circleProgress = circleAnimation.getValue();
        int offColor = ColorUtils.darken(themeColor, 0.05f);
        int toggleColor = ColorUtils.interpolateColor(offColor, themeColor, bgProgress);
        RenderUtils.drawGradientRect(matrices, toggleX, toggleY, 16.0f, 9.0f, 3.0f, this.fadeColorSafe(toggleColor, contentProgress, 2), this.fadeColorSafe(ColorUtils.darken(toggleColor, 0.65f), contentProgress, 2));
        float circleX = toggleX + 4.5f + circleProgress * 6.2f;
        RenderUtils.drawRoundCircle(matrices, circleX + 0.5f, toggleY + 4.5f, 6.85f, ColorUtils.rgba(255, 255, 255, textAlpha));
    }

    public Map<String, InterfaceProcessing> getConfigurableHudElements() {
        LinkedHashMap<String, InterfaceProcessing> elements = new LinkedHashMap<String, InterfaceProcessing>();
        elements.put("waterMark", this.waterMark);
        elements.put("keyBinds", this.keyBinds);
        elements.put("itemBinds", this.itemBinds);
        elements.put("potions", this.potions);
        elements.put("cooldowns", this.cooldowns);
        elements.put("notifications", this.notifications);
        elements.put("targetHud", this.targetHud);
        elements.put("information", this.information);
        elements.put("staffList", this.staffList);
        elements.put("hotbar", this.hotbar);
        elements.put("scoreboard", this.scoreboard);
        return elements;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventLink(priority=-200)
    public void onEvent(EventRender.Default event) {
        boolean showScoreboard;
        this.glassSettings.enabled.setState(this.glassEnabled.isState());
        this.glassSettings.alpha.setValue(this.glassAlpha.getValue().floatValue());
        this.glassSettings.radius.setValue(this.glassRadius.getValue().floatValue());
        this.glassSettings.blurStrength.setValue(this.glassBlur.getValue().floatValue());
        this.glassSettings.tintColor.setValue(this.glassTint.getValue().floatValue());
        this.glassSettings.glowEnabled.setState(this.glowEnabled.isState());
        this.glassSettings.glowSpeed.setValue(this.glowSpeed.getValue().floatValue());
        this.glassSettings.glowWidth.setValue(this.glowWidth.getValue().floatValue());
        this.glassSettings.glowPauseDuration.setValue(this.glowPause.getValue().floatValue());
        boolean isChatOpen = mc != null && mc.getWindow() != null && Interface.mc.currentScreen instanceof ChatScreen;
        boolean showWaterMark = this.hudModules.is("Ватермарка") || isChatOpen;
        boolean showKeyBinds = this.hudModules.is("Горячие клавиши") || isChatOpen;
        boolean showItemBinds = this.hudModules.is("Бинды предметов") || isChatOpen;
        boolean showPotions = this.hudModules.is("Зелья") || isChatOpen;
        boolean showCooldowns = this.hudModules.is("Кулдауны") || isChatOpen;
        boolean showInformation = this.hudModules.is("Информация") || isChatOpen;
        boolean showStaff = this.hudModules.is("Стафф") || isChatOpen;
        boolean showNotifications = this.hudModules.is("Уведомления") || isChatOpen;
        boolean showTargetHud = this.hudModules.is("Таргет худ") || isChatOpen;
        boolean showHotbar = this.hudModules.is("Хотбар") || isChatOpen;
        boolean bl = showScoreboard = this.hudModules.is("Скорборд") || isChatOpen;
        if (isChatOpen) {
            Font hintFont = this.issue(18);
            float x2 = (float)mc.getWindow().getScaledWidth() * 0.5f - hintFont.getWidth(HUD_HINT_TEXT) * 0.5f;
            hintFont.draw(event.getContext().getMatrices(), HUD_HINT_TEXT, x2, 40.0f, -1);
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        try {
            if (showWaterMark) {
                this.waterMark.updateGlassSettings(this.glassSettings);
                this.renderHudElement(this.waterMark, event);
            }
            if (showKeyBinds) {
                this.keyBinds.updateGlassSettings(this.glassSettings);
                this.renderHudElement(this.keyBinds, event);
            }
            if (showItemBinds) {
                this.itemBinds.updateGlassSettings(this.glassSettings);
                this.renderHudElement(this.itemBinds, event);
            }
            if (showPotions) {
                this.potions.updateGlassSettings(this.glassSettings);
                this.renderHudElement(this.potions, event);
            }
            if (showCooldowns) {
                this.cooldowns.updateGlassSettings(this.glassSettings);
                this.renderHudElement(this.cooldowns, event);
            }
            if (showInformation) {
                this.information.updateGlassSettings(this.glassSettings);
                this.renderHudElement(this.information, event);
            }
            if (showStaff) {
                this.staffList.updateGlassSettings(this.glassSettings);
                this.renderHudElement(this.staffList, event);
            }
            if (showNotifications) {
                this.notifications.updateGlassSettings(this.glassSettings);
                this.renderHudElement(this.notifications, event);
            }
            if (showTargetHud) {
                this.targetHud.updateGlassSettings(this.glassSettings);
                this.renderHudElement(this.targetHud, event);
            }
            if (showHotbar) {
                this.hotbar.updateGlassSettings(this.glassSettings);
                this.renderHudElement(this.hotbar, event);
            }
            if (showScoreboard) {
                this.scoreboard.updateGlassSettings(this.glassSettings);
                this.renderHudElement(this.scoreboard, event);
            }
        }
        finally {
            RenderSystem.depthMask((boolean)true);
            RenderSystem.enableDepthTest();
        }
        if (!(Interface.mc.currentScreen instanceof ChatScreen)) {
            this.targetHudMenuOpen = false;
            this.pendingHudContextElement = null;
        }
    }
}

