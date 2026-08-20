package polar.ru.client.ui.clickgui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.math.HoveringUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;

public class ClickGuiWarningDialog {
    private static final float WIDTH = 196.0f;
    private static final float HEIGHT = 88.0f;
    private static final float RADIUS = 6.0f;
    private static final float BUTTON_H = 16.0f;
    private static final int TITLE_COLOR = ColorUtils.rgba(255, 156, 46, 255);
    private static final int TEXT_COLOR = ColorUtils.rgba(170, 170, 178, 255);
    private static final int BG_COLOR = ColorUtils.rgba(20, 20, 20, 170);
    private final AnimationUtils appearAnimation = new AnimationUtils(0.0f, 11.0f, Easings.CUBIC_OUT);
    private boolean visible;
    private String title = "Предупреждение!";
    private String message = "";
    private Runnable confirmAction;
    private float okX;
    private float okY;
    private float okW;
    private float cancelX;
    private float cancelW;

    public void open(String message, Runnable confirmAction) {
        this.message = message == null ? "" : message;
        this.confirmAction = confirmAction;
        this.visible = true;
    }

    public void close() {
        this.visible = false;
        this.confirmAction = null;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public boolean isActive() {
        return this.visible || this.appearAnimation.getValue() > 0.01f;
    }

    public void render(DrawContext context, int mouseX, int mouseY, Window window, int colorTheme) {
        this.appearAnimation.update(this.visible ? 1.0f : 0.0f);
        float progress = this.appearAnimation.getValue();
        if (progress <= 0.01f || window == null) {
            return;
        }
        Font titleFont = Fonts.getFont("moe3", 16);
        Font textFont = Fonts.getFont("suisse", 12);
        Font buttonFont = Fonts.getFont("suisse", 13);
        if (titleFont == null || textFont == null || buttonFont == null) {
            return;
        }
        float x2 = (float)window.getScaledWidth() / 2.0f - 98.0f;
        float y2 = (float)window.getScaledHeight() / 2.0f - 44.0f;
        float centerX = x2 + 98.0f;
        float centerY = y2 + 44.0f;
        float scale = 0.86f + 0.14f * progress;
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(centerX, centerY, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        matrices.translate(-centerX, -centerY, 0.0f);
        RenderUtils.drawRoundedRect(matrices, 0.0f, 0.0f, window.getScaledWidth(), window.getScaledHeight(), 0.0f, ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 120), progress));
        RenderUtils.drawShadow(matrices, x2 - 3.0f, y2 - 3.0f, 202.0f, 94.0f, 9.0f, ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 220), progress));
        RenderUtils.drawBlur(matrices, x2, y2, 196.0f, 88.0f, 6.0f, 6.0f, ColorUtils.applyAlpha(ColorUtils.rgba(255, 255, 255, 255), progress));
        RenderUtils.drawBlur(matrices, x2, y2, 196.0f, 88.0f, 6.0f, 6.0f, ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 190), progress));
        RenderUtils.drawRoundedRect(matrices, x2, y2, 196.0f, 88.0f, 6.0f, ColorUtils.applyAlpha(BG_COLOR, progress));
        titleFont.drawCenteredString(matrices, this.title, centerX, y2 + 13.0f, ColorUtils.applyAlpha(TITLE_COLOR, progress));
        List<String> lines = this.wrap(textFont, this.message, 174.0f);
        float lineHeight = textFont.getHeight() - 1.0f;
        float textY = y2 + 32.0f;
        for (int i2 = 0; i2 < lines.size(); ++i2) {
            textFont.drawCenteredString(matrices, lines.get(i2), centerX, textY + (float)i2 * lineHeight, ColorUtils.applyAlpha(TEXT_COLOR, progress));
        }
        this.cancelW = this.okW = 85.0f;
        this.okX = x2 + 9.0f;
        this.cancelX = this.okX + this.okW + 8.0f;
        this.okY = y2 + 88.0f - 16.0f - 9.0f;
        boolean okHovered = HoveringUtils.isHovered(mouseX, mouseY, this.okX, this.okY, this.okW, 16.0);
        boolean cancelHovered = HoveringUtils.isHovered(mouseX, mouseY, this.cancelX, this.okY, this.cancelW, 16.0);
        this.drawButton(matrices, buttonFont, "OK", this.okX, this.okY, this.okW, okHovered ? ColorUtils.setAlphaColor(colorTheme, 220) : ColorUtils.setAlphaColor(colorTheme, 150), progress, -1);
        this.drawButton(matrices, buttonFont, "Отмена", this.cancelX, this.okY, this.cancelW, cancelHovered ? ColorUtils.rgba(62, 62, 72, 235) : ColorUtils.rgba(38, 38, 46, 220), progress, ColorUtils.rgba(215, 215, 222, 255));
        matrices.pop();
    }

    private void drawButton(MatrixStack matrices, Font font, String label, float x2, float y2, float w2, int color, float progress, int textColor) {
        RenderUtils.drawShadow(matrices, x2, y2, w2, 16.0f, 5.0f, ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 150), progress));
        RenderUtils.drawRoundedRect(matrices, x2, y2, w2, 16.0f, 3.0f, ColorUtils.applyAlpha(color, progress));
        font.drawCenteredString(matrices, label, x2 + w2 / 2.0f, y2 + (16.0f - font.getHeight()) / 2.0f, ColorUtils.applyAlpha(textColor, progress));
    }

    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (!this.visible) {
            return false;
        }
        if (button != 0) {
            return true;
        }
        if (HoveringUtils.isHovered(mouseX, mouseY, this.okX, this.okY, this.okW, 16.0)) {
            Runnable action = this.confirmAction;
            this.close();
            if (action != null) {
                action.run();
            }
            return true;
        }
        if (HoveringUtils.isHovered(mouseX, mouseY, this.cancelX, this.okY, this.cancelW, 16.0)) {
            this.close();
            return true;
        }
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (!this.visible) {
            return false;
        }
        if (keyCode == 256) {
            this.close();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            Runnable action = this.confirmAction;
            this.close();
            if (action != null) {
                action.run();
            }
            return true;
        }
        return true;
    }

    private List<String> wrap(Font font, String text, float maxWidth) {
        ArrayList<String> lines = new ArrayList<String>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        StringBuilder current = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            String candidate;
            String string = candidate = current.isEmpty() ? word : String.valueOf(current) + " " + word;
            if (font.getWidth(candidate) <= maxWidth || current.isEmpty()) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            lines.add(current.toString());
            current.setLength(0);
            current.append(word);
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }
}

