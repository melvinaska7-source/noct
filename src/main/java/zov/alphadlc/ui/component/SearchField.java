package zov.alphadlc.ui.component;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import zov.alphadlc.util.cursor.CursorManager;
import zov.alphadlc.util.render.helper.HoverUtil;
import zov.alphadlc.util.render.math.Animation;
import zov.alphadlc.util.render.math.Easing;
import zov.alphadlc.util.render.math.Scissor;
import zov.alphadlc.util.render.msdf.Fonts;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;

/**
 * Полностью переписанное поле поиска.
 * Поддерживает каретку, выделение, комбинации клавиш (Ctrl+A/C/V/X, Ctrl+Backspace,
 * навигацию стрелками/Home/End с Shift-выделением) и горизонтальную прокрутку текста.
 */
public class SearchField {
    private float x, y, width, height;
    public String text = "";
    private boolean focused;
    private final String placeholder;

    private int caret;          // позиция каретки (0..text.length())
    private int selectionAnchor; // якорь выделения; == caret если выделения нет
    private float scrollX;       // горизонтальный сдвиг текста, чтобы каретка была видна

    private static final float ICON_BOX_W = 17f;
    private static final float RADIUS = 4f;
    private static final float FONT_SIZE = 7f;
    private static final int MAX_LENGTH = 64;

    private final Animation focusAnim = new Animation(Easing.QUINTIC_OUT, 220);
    private final Animation appearAnim = new Animation(Easing.QUINTIC_OUT, 340);

    public SearchField(String placeholder) {
        this.placeholder = placeholder;
    }

    /** Сбросить анимацию появления (вызывается при открытии ClickGui). */
    public void resetAppear() {
        appearAnim.reset(0f);
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    private float textAreaX() {
        return x + ICON_BOX_W + 3f;
    }

    private float textAreaW() {
        return width - ICON_BOX_W - 6f;
    }

    private float widthOf(String s) {
        return Fonts.SFREGULAR.get().getWidth(s, FONT_SIZE);
    }

    private boolean hasSelection() {
        return caret != selectionAnchor;
    }

    private int selMin() {
        return Math.min(caret, selectionAnchor);
    }

    private int selMax() {
        return Math.max(caret, selectionAnchor);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, x, y, width, height);
        if (hovered) CursorManager.requestIBeam();

        focusAnim.run(focused);
        float fa = (float) focusAnim.getValue();

        // Анимация появления: лёгкий выезд снизу + прозрачность (как у панелей)
        appearAnim.run(true);
        float ap = appearAnim.getValue();
        float ay = y + (1f - ap) * 9f;

        // Фон в стиле панелей ClickGui: матовый блюр + полупрозрачная тёмно-синяя подложка + синяя рамка
        DrawUtil.drawRoundBlur(x, ay, width, height, RADIUS,
                ColorProvider.rgba(200, 200, 200, (int) (255 * ap)), 14f);

        int borderColor = ColorProvider.interpolateColor(
                ColorProvider.rgba(48, 66, 122, (int) (70 * ap)),
                ColorProvider.setAlpha(ColorProvider.getColorClient(), (int) (150 * ap)),
                fa);
        DrawUtil.drawRound(x - 0.5f, ay - 0.5f, width + 1f, height + 1f, RADIUS + 0.5f, borderColor);
        DrawUtil.drawRound(x, ay, width, height, RADIUS,
                ColorProvider.setAlpha(ColorProvider.getColorClickGui(), (int) (130 * ap)));

        // Иконка лупы
        float iconW = Fonts.ICONS_MINCED.get().getWidth("l", 10f);
        float iconX = x + (ICON_BOX_W - iconW) / 2f + 1f;
        float iconY = ay + (height / 2f) - 4.5f;
        int iconColor = ColorProvider.interpolateColor(
                ColorProvider.setAlpha(ColorProvider.getColorIcons(), (int) (160 * ap)),
                ColorProvider.setAlpha(ColorProvider.getColorIcons(), (int) (255 * ap)), fa);
        DrawUtil.drawText(Fonts.ICONS_MINCED.get(), "l", iconX, iconY, iconColor, 10f);

        float taX = textAreaX();
        float taW = textAreaW();
        float textY = ay + (height / 2f) - FONT_SIZE / 2f + 0.2f;

        updateScroll(taW);

        Scissor.push();
        Scissor.setFromComponentCoordinates(taX, ay, taW, height);

        if (text.isEmpty() && !focused) {
            DrawUtil.drawText(Fonts.SFREGULAR.get(), placeholder, taX, textY,
                    ColorProvider.setAlpha(ColorProvider.getColorInactiveText(), (int) (255 * ap)), FONT_SIZE);
        } else {
            // Выделение
            if (hasSelection()) {
                float selX1 = taX - scrollX + widthOf(text.substring(0, selMin()));
                float selX2 = taX - scrollX + widthOf(text.substring(0, selMax()));
                DrawUtil.drawRound(selX1, ay + 3f, selX2 - selX1, height - 6f, 1.5f,
                        ColorProvider.setAlpha(ColorProvider.getColorClient(), (int) (90 * ap)));
            }

            DrawUtil.drawText(Fonts.SFREGULAR.get(), text, taX - scrollX, textY,
                    ColorProvider.setAlpha(ColorProvider.getColorText(), (int) (255 * ap)), FONT_SIZE);

            // Каретка (мигает)
            if (focused && System.currentTimeMillis() % 1000 > 500) {
                float caretX = taX - scrollX + widthOf(text.substring(0, caret));
                DrawUtil.drawRound(caretX, ay + 3f, 0.8f, height - 6f, 0f,
                        ColorProvider.setAlpha(ColorProvider.getColorText(), (int) (230 * ap)));
            }
        }

        Scissor.unset();
        Scissor.pop();
    }

    private void updateScroll(float areaW) {
        float caretPos = widthOf(text.substring(0, caret));
        if (caretPos - scrollX > areaW - 2f) {
            scrollX = caretPos - areaW + 2f;
        }
        if (caretPos - scrollX < 0f) {
            scrollX = caretPos;
        }
        float total = widthOf(text);
        if (total - scrollX < areaW - 2f) {
            scrollX = Math.max(0f, total - areaW + 2f);
        }
        if (total <= areaW) scrollX = 0f;
    }

    public void charTyped(char codePoint, int modifiers) {
        if (!focused) return;
        // Игнорируем управляющие символы и комбинации с Ctrl/Alt/Super (их обрабатывает keyPressed)
        if ((modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER)) != 0) return;
        if (codePoint < 32 || codePoint == 127) return;
        insert(String.valueOf(codePoint));
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return;

        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0 || (modifiers & GLFW.GLFW_MOD_SUPER) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        if (ctrl) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_A -> { selectAll(); return; }
                case GLFW.GLFW_KEY_C -> { copySelection(); return; }
                case GLFW.GLFW_KEY_X -> { cutSelection(); return; }
                case GLFW.GLFW_KEY_V -> { paste(); return; }
            }
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (ctrl) {
                    int start = prevWordBoundary(caret);
                    text = text.substring(0, start) + text.substring(caret);
                    caret = start;
                    selectionAnchor = caret;
                } else if (caret > 0) {
                    text = text.substring(0, caret - 1) + text.substring(caret);
                    caret--;
                    selectionAnchor = caret;
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (caret < text.length()) {
                    text = text.substring(0, caret) + text.substring(caret + 1);
                }
            }
            case GLFW.GLFW_KEY_LEFT -> {
                int target = ctrl ? prevWordBoundary(caret) : Math.max(0, caret - 1);
                moveCaret(target, shift);
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                int target = ctrl ? nextWordBoundary(caret) : Math.min(text.length(), caret + 1);
                moveCaret(target, shift);
            }
            case GLFW.GLFW_KEY_HOME -> moveCaret(0, shift);
            case GLFW.GLFW_KEY_END -> moveCaret(text.length(), shift);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_ESCAPE -> focused = false;
            default -> {}
        }
    }

    private void insert(String s) {
        if (hasSelection()) deleteSelection();
        int free = MAX_LENGTH - text.length();
        if (free <= 0) return;
        if (s.length() > free) s = s.substring(0, free);
        text = text.substring(0, caret) + s + text.substring(caret);
        caret += s.length();
        selectionAnchor = caret;
    }

    private void deleteSelection() {
        int a = selMin(), b = selMax();
        text = text.substring(0, a) + text.substring(b);
        caret = a;
        selectionAnchor = a;
    }

    private void moveCaret(int target, boolean keepSelection) {
        caret = Math.max(0, Math.min(text.length(), target));
        if (!keepSelection) selectionAnchor = caret;
    }

    private void selectAll() {
        selectionAnchor = 0;
        caret = text.length();
    }

    private void copySelection() {
        if (!hasSelection()) return;
        MinecraftClient.getInstance().keyboard.setClipboard(text.substring(selMin(), selMax()));
    }

    private void cutSelection() {
        if (!hasSelection()) return;
        copySelection();
        deleteSelection();
    }

    private void paste() {
        String clip = MinecraftClient.getInstance().keyboard.getClipboard();
        if (clip == null || clip.isEmpty()) return;
        clip = clip.replaceAll("[\\n\\r\\t]", " ");
        insert(clip);
    }

    private int prevWordBoundary(int from) {
        int i = from;
        while (i > 0 && text.charAt(i - 1) == ' ') i--;
        while (i > 0 && text.charAt(i - 1) != ' ') i--;
        return i;
    }

    private int nextWordBoundary(int from) {
        int i = from;
        int len = text.length();
        while (i < len && text.charAt(i) == ' ') i++;
        while (i < len && text.charAt(i) != ' ') i++;
        return i;
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        boolean inside = HoverUtil.isHovered(mouseX, mouseY, x, y, width, height);
        focused = inside;
        if (inside) {
            int index = caretFromMouse((float) mouseX);
            caret = index;
            selectionAnchor = index;
        }
    }

    private int caretFromMouse(float mouseX) {
        float local = mouseX - textAreaX() + scrollX;
        int best = 0;
        float bestDist = Float.MAX_VALUE;
        for (int i = 0; i <= text.length(); i++) {
            float w = widthOf(text.substring(0, i));
            float d = Math.abs(w - local);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    public boolean isFocused() {
        return focused;
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }
}
