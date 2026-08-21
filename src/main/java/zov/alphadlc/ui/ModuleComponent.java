package zov.alphadlc.ui;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.settings.*;
import zov.alphadlc.ui.component.Component;
import zov.alphadlc.ui.component.impl.*;
import zov.alphadlc.util.cursor.CursorManager;
import zov.alphadlc.util.render.helper.HoverUtil;
import zov.alphadlc.util.render.math.Animation;
import zov.alphadlc.util.render.math.Easing;
import zov.alphadlc.util.render.msdf.Fonts;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;

@Getter
public class ModuleComponent extends Component {
    private final Module module;
    private final Panel panel;

    // === АНИМАЦИИ ===
    private final Animation animation = new Animation(Easing.QUINTIC_OUT, 320);
    private final Animation hoverAnim = new Animation(Easing.QUINTIC_OUT, 300);
    private final Animation enabledAnim = new Animation(Easing.QUINTIC_OUT, 400);
    private final Animation heightAnim = new Animation(Easing.QUINTIC_OUT, 350);

    public boolean open;
    private boolean isHovered;
    private boolean binding;

    private final ObjectArrayList<Component> components = new ObjectArrayList<>();

    private static float cachedSettingsIconWidth = -1f;

    public ModuleComponent(Module module, Panel panel) {
        this.module = module;
        this.panel = panel;
        for (Setting setting : module.getSettings()) {
            switch (setting) {
                case BooleanSetting option -> components.add(new BooleanComponent(option));
                case ItemModelSetting option ->
                        components.add(new ItemModelComponent(option, panel::openItemModelGallery));
                case ModeSetting option -> components.add(new ModeComponent(option));
                case ModeListSetting option -> components.add(new ModeListComponent(option));
                case SliderSetting option -> components.add(new SliderComponent(option));
                case BindSetting option -> components.add(new BindComponent(option));
                case ThemeSetting option -> components.add(new ThemeComponent(option));
                case ColorSetting option -> components.add(new ColorPickerComponent(option));
                default -> {}
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        isHovered = HoverUtil.isHovered(mouseX, mouseY, x, y, width, 19);

        hoverAnim.run(isHovered);
        animation.run(open);
        enabledAnim.run(module.isEnabled());

        if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, 19)) CursorManager.requestHand();

        float alpha = Math.max(Math.min(panel.getAnimationAlpha().getValue(), 1), 0);

        int textColor = ColorProvider.interpolateColor(
                ColorProvider.setAlpha(ColorProvider.getColorInactiveText(), (int)(255 * alpha)),
                ColorProvider.setAlpha(ColorProvider.getColorText(), (int)(255 * alpha)),
                enabledAnim.getValue()
        );

        float highlightProgress = Math.max(hoverAnim.getValue(), enabledAnim.getValue());
        int outlineAlpha = (int) ((25 + (40 * highlightProgress)) * alpha);

        int outlineColor = ColorProvider.rgba(255, 255, 255, outlineAlpha);
        int innerColor = ColorProvider.interpolateColor(
                ColorProvider.setAlpha(ColorProvider.getColorMain(), (int)(28 * alpha)),
                ColorProvider.setAlpha(ColorProvider.getColorVisualModules(), (int)(22 * alpha)),
                enabledAnim.getValue()
        );

        // === ПЛАВНЫЙ РАСЧЁТ ВЫСОТЫ ===
        float baseHeight = 19f;
        float targetExtraHeight = 0f;

        if (open || animation.getValue() > 0.01f) {
            targetExtraHeight = 3f;
            for (Component comp : components) {
                if (comp.isVisible()) {
                    targetExtraHeight += comp.getHeight();
                }
            }
            targetExtraHeight += 5f;
        }

        // Плавный переход высоты через анимацию
        heightAnim.run(targetExtraHeight);
        float extraHeight = (float) heightAnim.getValue() * (float) animation.getValue();
        float currentHeight = baseHeight + extraHeight;

        setHeight(currentHeight);

        // Отсечение невидимых карточек
        float visTop = panel.getY() + 25f;
        float visBottom = panel.getY() + panel.getHeight() - 4f;
        if (y + currentHeight < visTop || y > visBottom) {
            isHovered = false;
            return;
        }

        // === РЕНДЕР КАРТОЧКИ ===
        DrawUtil.drawRoundBlur(x, y, width, currentHeight - 0.5f, 3f, 
            ColorProvider.rgba(200, 200, 200, (int)(255 * alpha)), 10f);
        DrawUtil.drawRound(x - 1f, y - 1f, width + 2f, currentHeight + 1f, 3.5f, 
            ColorProvider.rgba(48, 66, 122, (int)(55 * alpha)));
        DrawUtil.drawRound(x, y, width, currentHeight - 0.5f, 3f, innerColor);

        if (binding) {
            DrawUtil.drawText(Fonts.SFREGULAR.get(), "Нажмите клавишу...", 
                x + width / 2f - Fonts.SFREGULAR.get().getWidth("Нажмите клавишу...", 7.5f) / 2f, 
                y + 5.75f, ColorProvider.rgba(255, 255, 255, (int)(255 * alpha)), 7.5f);
        } else {
            float textY = y + 5.75f;

            if (!components.isEmpty()) {
                String icon = "{/}";
                if (cachedSettingsIconWidth < 0f) {
                    cachedSettingsIconWidth = Fonts.SFREGULAR.get().getWidth(icon, 7.5f);
                }
                float iconWidth = cachedSettingsIconWidth;

                int iconAlpha = (int)(180 + 75 * animation.getValue());
                int iconColor = ColorProvider.setAlpha(ColorProvider.getColorInactiveText(), 
                    (int)(iconAlpha * alpha));

                DrawUtil.drawText(Fonts.SFREGULAR.get(), icon, x + 3f, textY, iconColor, 7.5f);
                DrawUtil.drawText(Fonts.SFREGULAR.get(), " " + module.getName(), 
                    x + 3f + iconWidth, textY, textColor, 7.5f);
            } else {
                DrawUtil.drawText(Fonts.SFREGULAR.get(), module.getName(), 
                    x + 3f, textY, textColor, 7.5f);
            }

            // Переключатель
            float toggleW = 20f;
            float toggleH = 10f;
            float toggleX = x + width - toggleW - 4f;
            float toggleY = y + (19f - toggleH) / 2f;

            int toggleBgColor = ColorProvider.interpolateColor(
                ColorProvider.rgba(40, 52, 92, (int)(150 * alpha)),
                ColorProvider.setAlpha(ColorProvider.getColorVisualModules(), (int)(200 * alpha)),
                enabledAnim.getValue()
            );
            DrawUtil.drawRound(toggleX, toggleY, toggleW, toggleH, toggleH / 2f, toggleBgColor);

            float circleSize = toggleH - 2.5f;
            float circleX = toggleX + 1.25f + ((toggleW - circleSize - 2.5f) * enabledAnim.getValue());
            float circleY = toggleY + 1.25f;
            DrawUtil.drawRound(circleX, circleY, circleSize, circleSize, circleSize / 2f, 
                ColorProvider.rgba(255, 255, 255, (int)(255 * alpha)));
        }

        // === РЕНДЕР НАСТРОЕК С STAGGER ===
        if (animation.getValue() > 0.01f) {
            float compY = y + 22f;
            float panelTop = panel.getY() + 25;
            float panelBottom = panel.getY() + panel.getHeight() - 4;

            float boxBottom = y + currentHeight;
            float intersectY = Math.max(y + 19, panelTop);
            float intersectBottom = Math.min(boxBottom, panelBottom);
            float intersectHeight = Math.max(0, intersectBottom - intersectY);

            float darkHeight = currentHeight - 19f;
            if (darkHeight > 0) {
                DrawUtil.drawRound(x + 1f, y + 19, width - 2f, darkHeight, 0f, 
                    ColorProvider.rgba(0, 0, 0, (int)(30 * alpha * animation.getValue())));
            }

            // Stagger-анимация
            float openProgress = (float) animation.getValue();

            for (int i = 0; i < components.size(); i++) {
                Component comp = components.get(i);

                float staggerDelay = i * 0.06f;
                float staggerProgress;

                if (open) {
                    staggerProgress = MathHelper.clamp((openProgress - staggerDelay) / 0.4f, 0f, 1f);
                } else {
                    float reverseDelay = (components.size() - 1 - i) * 0.04f;
                    staggerProgress = MathHelper.clamp((openProgress - reverseDelay) / 0.3f, 0f, 1f);
                }

                // Плавное easing через Math.pow (кубическое)
                staggerProgress = (float) (1 - Math.pow(1 - staggerProgress, 3));

                comp.getAlphaAnim().setValue(Math.min(panel.getAnimationAlpha().getValue(), 1) * staggerProgress);
                comp.getAlphaAnimSetting().run(comp.isVisible());

                float visibleProgress = MathHelper.clamp(comp.getAlphaAnimSetting().getValue(), 0f, 1f);
                if (comp.isVisible() || visibleProgress > 0) {
                    comp.setX(x);
                    comp.setY(compY);
                    comp.setWidth(width - 4);

                    zov.alphadlc.util.render.math.Scissor.push();
                    zov.alphadlc.util.render.math.Scissor.setFromComponentCoordinates(
                        x, intersectY, width, intersectHeight);

                    // Масштабирование настроек при появлении
                    if (staggerProgress < 1f && staggerProgress > 0.01f) {
                        float scale = 0.95f + 0.05f * staggerProgress;
                        context.getMatrices().push();
                        context.getMatrices().translate(comp.getX() + comp.getWidth() / 2f, compY + comp.getHeight() / 2f, 0);
                        context.getMatrices().scale(scale, scale, 1f);
                        context.getMatrices().translate(-(comp.getX() + comp.getWidth() / 2f), -(compY + comp.getHeight() / 2f), 0);
                    }

                    comp.render(context, mouseX, mouseY, partialTicks);

                    if (staggerProgress < 1f && staggerProgress > 0.01f) {
                        context.getMatrices().pop();
                    }

                    zov.alphadlc.util.render.math.Scissor.unset();
                    zov.alphadlc.util.render.math.Scissor.pop();

                    compY += comp.getHeight() * visibleProgress;
                }
            }
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 19)) {
            if (button == 0) module.setEnabled(!module.isEnabled());
            if (button == 1 && !components.isEmpty()) open = !open;
            if (button == 2) binding = !binding;
        }

        if (open) {
            for (Component component : components) {
                if (component.isVisible() && component.getAlphaAnimSetting().getValue() > 0.5f) {
                    component.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (open) {
            for (Component component : components) {
                component.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                module.setKey(-1);
            } else {
                module.setKey(keyCode);
            }
            binding = false;
        }

        if (open) {
            for (Component component : components) {
                component.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    public boolean isBinding() {
        return binding;
    }

    private boolean isHovered(double mouseX, double mouseY, float heightCheck) {
        return HoverUtil.isHovered(mouseX, mouseY, x, y, width, heightCheck);
    }
}
