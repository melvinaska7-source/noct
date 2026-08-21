package zov.alphadlc.ui.component;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import zov.alphadlc.util.IMinecraft;
import zov.alphadlc.util.cursor.CursorManager;
import zov.alphadlc.util.render.helper.HoverUtil;
import zov.alphadlc.util.render.math.Animation;
import zov.alphadlc.util.render.math.Easing;
import zov.alphadlc.util.render.msdf.Fonts;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;

@Getter
public class SearchField implements IMinecraft {

    private String text = "";
    private String placeholder;
    private boolean focused = false;
    private float x, y, width, height;

    // Анимация появления (выезд снизу)
    private final Animation appearAnim = new Animation(Easing.QUINTIC_OUT, 340);
    // Анимация фокуса
    private final Animation focusAnim = new Animation(Easing.QUINTIC_OUT, 250);
    // Курсор мигания
    private long lastCursorToggle = 0;
    private boolean cursorVisible = true;

    public SearchField(String placeholder) {
        this.placeholder = placeholder;
    }

    public void resetAppear() {
        appearAnim.reset(0f);
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        appearAnim.run(true);
        focusAnim.run(focused);

        float appear = (float) appearAnim.getValue();
        float appearShift = (1f - appear) * 20f;  // Выезд снизу на 20 пикселей
        float renderY = y + appearShift;

        float alpha = appear;
        int intAlpha = (int)(255 * alpha);

        if (alpha < 0.01f) return;

        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, x, renderY, width, height);
        if (hovered) CursorManager.requestIBeam();

        // Фон
        int bgColor = ColorProvider.interpolateColor(
            ColorProvider.rgba(30, 40, 70, (int)(120 * alpha)),
            ColorProvider.rgba(40, 55, 95, (int)(160 * alpha)),
            focusAnim.getValue()
        );

        DrawUtil.drawRoundBlur(x, renderY, width, height, 4f, 
            ColorProvider.rgba(200, 200, 200, intAlpha), 10f);
        DrawUtil.drawRound(x - 0.5f, renderY - 0.5f, width + 1f, height + 1f, 4.5f, 
            ColorProvider.rgba(48, 66, 122, (int)(60 * alpha)));
        DrawUtil.drawRound(x, renderY, width, height, 4f, bgColor);

        // Обводка при фокусе
        if (focusAnim.getValue() > 0.01f) {
            int accent = ColorProvider.getColorVisualModules();
            int borderColor = ColorProvider.setAlpha(accent, (int)(100 * focusAnim.getValue() * alpha));
            DrawUtil.drawRound(x - 0.5f, renderY - 0.5f, width + 1f, height + 1f, 4.5f, borderColor);
        }

        // Текст
        float textSize = 7.5f;
        float textY = renderY + (height - textSize) / 2f + 0.5f;

        if (text.isEmpty() && !focused) {
            // Placeholder
            DrawUtil.drawText(Fonts.SFREGULAR.get(), placeholder, x + 6f, textY, 
                ColorProvider.rgba(120, 130, 160, (int)(180 * alpha)), textSize);
        } else {
            // Введённый текст
            DrawUtil.drawText(Fonts.SFREGULAR.get(), text, x + 6f, textY, 
                ColorProvider.setAlpha(ColorProvider.getColorText(), intAlpha), textSize);

            // Мигающий курсор
            if (focused && alpha > 0.9f) {
                long now = System.currentTimeMillis();
                if (now - lastCursorToggle > 530) {
                    cursorVisible = !cursorVisible;
                    lastCursorToggle = now;
                }

                if (cursorVisible) {
                    float textWidth = Fonts.SFREGULAR.get().getWidth(text, textSize);
                    float cursorX = x + 6f + textWidth;
                    float cursorH = textSize + 1f;
                    DrawUtil.drawRound(cursorX, textY - 0.5f, 0.8f, cursorH, 0.4f, 
                        ColorProvider.setAlpha(ColorProvider.getColorText(), intAlpha));
                }
            }
        }

        // Иконка поиска (справа)
        String searchIcon = "S";  // Или твоя иконка
        float iconSize = 7f;
        float iconWidth = Fonts.SFREGULAR.get().getWidth(searchIcon, iconSize);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), searchIcon, 
            x + width - iconWidth - 6f, textY, 
            ColorProvider.rgba(100, 110, 140, (int)(150 * alpha)), iconSize);
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        boolean wasHovered = HoverUtil.isHovered(mouseX, mouseY, x, y, width, height);
        focused = wasHovered && button == 0;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return;

        if (keyCode == 259) { // Backspace
            if (!text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
            }
        } else if (keyCode == 257 || keyCode == 335) { // Enter
            focused = false;
        }
    }

    public void charTyped(char chr, int modifiers) {
        if (!focused) return;
        if (chr >= 32 && chr < 127) {
            text += chr;
        }
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    public String getText() {
        return text;
    }
}
