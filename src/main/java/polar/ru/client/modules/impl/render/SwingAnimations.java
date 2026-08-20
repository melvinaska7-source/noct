package polar.ru.client.modules.impl.render;

import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class SwingAnimations
extends Module {
    public static SwingAnimations INSTANCE = new SwingAnimations();
    public boolean swimmingAnimation = true;
    public boolean climbAndCrawl = true;
    public boolean mb3DCompat = false;
    public final BooleanSetting hmiEnable = new BooleanSetting("Мод на красивые руки", false);
    public final ModeSetting hmiAnimationType = new ModeSetting("Вид анимации", "Классик", "Классик", "Шарп").visible(this.hmiEnable::isState);
    public final FloatSetting hmiSmoothness = new FloatSetting("Плавность анимации", 1.0f, 0.35f, 2.5f, 0.05f).visible(this.hmiEnable::isState);
    public final BooleanSetting swingEnabled = new BooleanSetting("Анимация свинга", true).visible(() -> !this.hmiEnable.isState());
    public final ModeSetting swingType = new ModeSetting("Тип свинга", "Smooth", "Smooth", "Static", "Down", "DropDown", "Poke", "SelfBack", "Feast", "ToBack", "Block", "Akrien", "Break", "Pander", "Slant").visible(() -> !this.hmiEnable.isState() && this.swingEnabled.isState());
    public final FloatSetting swingStrength = new FloatSetting("Сила анимации", 1.0f, 0.1f, 3.0f, 0.01f).visible(() -> !this.hmiEnable.isState() && this.swingEnabled.isState() && !this.swingType.is("Pander"));
    public final FloatSetting corner = new FloatSetting("Угол DropDown", 12.0f, 1.0f, 360.0f, 1.0f).visible(() -> !this.hmiEnable.isState() && this.swingEnabled.isState() && this.swingType.is("DropDown"));
    public final FloatSetting slant = new FloatSetting("Наклон DropDown", 12.0f, 1.0f, 360.0f, 1.0f).visible(() -> !this.hmiEnable.isState() && this.swingEnabled.isState() && this.swingType.is("DropDown"));
    public final BooleanSetting smoothEnabled = new BooleanSetting("Плавная анимация", false).visible(() -> !this.hmiEnable.isState());
    public final FloatSetting slowAnimationSpeed = new FloatSetting("Скорость анимации", 12.0f, 1.0f, 50.0f, 1.0f).visible(() -> !this.hmiEnable.isState() && this.smoothEnabled.isState());
    public final BooleanSetting auraTargetOnly = new BooleanSetting("Только при Aura", false).visible(() -> !this.hmiEnable.isState());
    public final BooleanSetting swapHands = new BooleanSetting("Свап рук", false).visible(() -> !this.hmiEnable.isState());
    public final BooleanSetting eatAnim = new BooleanSetting("Анимация еды", false).visible(() -> !this.hmiEnable.isState());

    public SwingAnimations() {
        super("SwingAnimations", "Кастомная анимация аттаки", Module.ModuleCategory.RENDER);
        this.addSettings(this.hmiEnable, this.hmiAnimationType, this.hmiSmoothness, this.swingEnabled, this.swingType, this.swingStrength, this.corner, this.slant, this.smoothEnabled, this.slowAnimationSpeed, this.auraTargetOnly, this.swapHands, this.eatAnim);
    }
}

