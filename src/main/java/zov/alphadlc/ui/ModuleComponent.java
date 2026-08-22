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

    private final Animation animation = new Animation(Easing.QUINTIC_OUT, 320);
    private final Animation hoverAnim = new Animation(Easing.QUINTIC_OUT, 300);
    private final Animation enabledAnim = new Animation(Easing.QUINTIC_OUT, 400);

    public boolean open;
    private boolean isHovered;
    private boolean binding;

    private final ObjectArrayList<Component> components = new ObjectArrayList<>();

    // Ширина статичной иконки "{/}" не меняется — измеряем один раз.
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

        float currentHeight = 19f + ((height - 19f) * animation.getValue());

        // Отсечение полностью невидимых карточек
        float visTop = panel.getY() + 25f;
        float visBottom = panel.getY() + panel.getHeight() - 4f;
        if (y + currentHeight < visTop || y > visBottom) {
            isHovered = false;
            return;
        }

        // Матовый блюр под карточкой модуля
        DrawUtil.drawRoundBlur(x, y, width, currentHeight - 0.5f, 3f, ColorProvider.rgba(200, 200, 200, (int)(255 * alpha)), 10f);
        DrawUtil.drawRound(x - 1f, y - 1f, width + 2f, currentHeight + 1f, 3.5f, ColorProvider.rgba(48, 66, 122, (int)(55 * alpha)));
        DrawUtil.drawRound(x, y, width, currentHeight - 0.5f, 3f, innerColor);

        if (binding) {
            DrawUtil.drawText(Fonts.SFREGULAR.get(), "Нажмите клавишу...", x + width / 2f - Fonts.SFREGULAR.get().getWidth("Нажмите клавишу...", 7.5f) / 2f, y + 5.75f, ColorProvider.rgba(255, 255, 255, (int)(255 * alpha)), 7.5f);
        } else {
            float textY = y + 5.75f;

            if (!components.isEmpty()) {
                String icon = "{/}";
                if (cachedSettingsIconWidth < 0f) {
                    cachedSettingsIconWidth = Fonts.SFREGULAR.get().getWidth(icon, 7.5f);
                }
                float iconWidth = cachedSettingsIconWidth;

                DrawUtil.drawText(Fonts.SFREGULAR.get(), icon, x + 3f, textY, 
                    ColorProvider.setAlpha(ColorProvider.getColorInactiveText(), (int)(180 * alpha)), 7.5f);
                DrawUtil.drawText(Fonts.SFREGULAR.get(), " " + module.getName(), x + 3f + iconWidth, textY, textColor, 7.5f);
            } else {
                DrawUtil.drawText(Fonts.SFREGULAR.get(), module.getName(), x + 3f, textY, textColor, 7.5f);
            }

            // Переключатель справа
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
                DrawUtil.drawRound(x + 1f, y + 19, width - 2f, darkHeight, 0f, ColorProvider.rgba(0, 0, 0, (int)(30 * alpha * animation.getValue())));
            }

            // === НОВОЕ: Анимация выпадания настроек с stagger-эффектом ===
            // Каждый компонент появляется с задержкой относительно предыдущего
            float animProgress = (float) animation.getValue();
            int compIndex = 0;
            int visibleCount = 0;
            for (Component c : components) {
                if (c.isVisible() || c.getAlphaAnimSetting().getValue() > 0f) visibleCount++;
            }

            for (Component component : components) {
                component.getAlphaAnim().setValue(Math.min(panel.getAnimationAlpha().getValue(), 1) * animProgress);
                component.getAlphaAnimSetting().run(component.isVisible());

                float visibleProgress = MathHelper.clamp(component.getAlphaAnimSetting().getValue(), 0f, 1f);
                if (component.isVisible() || visibleProgress > 0) {
                    component.setX(x);
                    component.setY(compY);
                    component.setWidth(width - 4);

                    // === STAGGER АНИМАЦИЯ ===
                    // Задержка между компонентами: каждый следующий начинает появляться позже
                    float staggerDelay = compIndex * 0.06f; // 60ms задержка между компонентами
                    float staggerDuration = 0.3f; // Базовая длительность
                    float effectiveProgress;
                    if (animProgress <= staggerDelay) {
                        effectiveProgress = 0f;
                    } else if (animProgress >= staggerDelay + staggerDuration) {
                        effectiveProgress = 1f;
                    } else {
                        effectiveProgress = (animProgress - staggerDelay) / staggerDuration;
                        // Применяем easing к stagger
                        effectiveProgress = (float) Easing.QUINTIC_OUT.ease(effectiveProgress, 0, 1, 1);
                    }

                    // Запускаем slideY анимацию для компонента
                    component.getSlideYAnim().run(open ? 1f : 0f);
                    float slideYProgress = (float) component.getSlideYAnim().getValue();

                    // Сдвиг по Y: компонент "падает" сверху на своё место
                    float slideOffset = (1f - slideYProgress) * -6f * effectiveProgress;
                    // Масштаб по Y: компонент "растягивается" из точки
                    float scaleY = 0.7f + 0.3f * slideYProgress * effectiveProgress;

                    zov.alphadlc.util.render.math.Scissor.push();
                    zov.alphadlc.util.render.math.Scissor.setFromComponentCoordinates(x, intersectY, width, intersectHeight);

                    // Применяем трансформации матрицы для эффекта выпадания
                    context.getMatrices().push();

                    // Центр трансформации — центр компонента
                    float compCenterX = component.getX() + component.getWidth() / 2f;
                    float compCenterY = component.getY() + component.getHeight() / 2f;

                    context.getMatrices().translate(compCenterX, compCenterY + slideOffset, 0);
                    context.getMatrices().scale(1f, scaleY, 1f);
                    context.getMatrices().translate(-compCenterX, -compCenterY, 0);

                    component.render(context, mouseX, mouseY, partialTicks);

                    context.getMatrices().pop();

                    zov.alphadlc.util.render.math.Scissor.unset();
                    zov.alphadlc.util.render.math.Scissor.pop();

                    compY += component.getHeight() * visibleProgress;
                    compIndex++;
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
        for (Component component : components) {
            component.mouseReleased(mouseX, mouseY, button);
        }
    }

    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (Component component : components) {
            component.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                binding = false;
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                module.setKeyBind(GLFW.GLFW_KEY_UNKNOWN);
                binding = false;
                return;
            }
            module.setKeyBind(keyCode);
            binding = false;
            return;
        }
        for (Component component : components) {
            component.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public void charTyped(char chr, int modifiers) {
        for (Component component : components) {
            component.charTyped(chr, modifiers);
        }
    }

    public boolean isHovered(double mouseX, double mouseY, float height) {
        return HoverUtil.isHovered(mouseX, mouseY, x, y, width, height);
    }
}
