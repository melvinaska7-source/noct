package zov.alphadlc.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.DrawContext;
import org.joml.Vector4f;
import zov.alphadlc.module.settings.impl.Theme;
import zov.alphadlc.module.settings.impl.ThemeManager;
import zov.alphadlc.util.IMinecraft;
import zov.alphadlc.util.cursor.CursorManager;
import zov.alphadlc.util.render.helper.HoverUtil;
import zov.alphadlc.util.render.math.Animation;
import zov.alphadlc.util.render.math.Easing;
import zov.alphadlc.util.render.math.Scissor;
import zov.alphadlc.util.render.msdf.Fonts;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ThemeEditor implements IMinecraft {

    private static final String[] SLOTS = {
            "Тема клиента",
            "Цвет иконок",
            "Неактивный текст",
            "Активный текст",
            "Фон Interface",
            "Фон ClickGui"
    };

    private static final float W = 124f;
    private static final float TAB = 18f;
    private static final float ROW_H = 15f;
    private static final float PAD = 5f;
    private static final float BTN_H = 14f;
    private static final float BTN_GAP = 4f;

    private static final int[] DEFAULTS = {
            0xFF3C6EF5,
            0xFF3C6EF5,
            0xFF7E86A0,
            0xFFE8E8E8,
            0xFF000000,
            0xFF000000
    };

    private static final float SV_SIZE = 58f;
    private static final float HUE_W = 7f;
    private static final float HUE_GAP = 4f;
    private static final float PICKER_PAD = 5f;
    private static final float PICKER_W = SV_SIZE + HUE_GAP + HUE_W;

    private static final File DIR = new File("alphadlc");
    private static final File FILE = new File(DIR, "theme.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Animation expandAnim = new Animation(Easing.QUINTIC_OUT, 380);
    private final Animation pickerAnim = new Animation(Easing.QUINTIC_OUT, 240);
    private final Animation appearAnim = new Animation(Easing.BACK_OUT, 400);
    private final Animation scaleAnim = new Animation(Easing.QUINTIC_OUT, 350);

    public void resetAppear() {
        appearAnim.reset(0f);
        scaleAnim.reset(0.8f);
    }

    private boolean expanded;
    private int editingSlot = -1;
    private final float[] hsv = new float[3];
    private boolean draggingSV, draggingH;
    private float pickerX, pickerY;

    private final int[] colors = new int[SLOTS.length];

    private float x, y, height;

    public ThemeEditor() {
        Theme t = ThemeManager.getInstance().getCurrentTheme();
        colors[0] = t.colorClient;
        colors[1] = t.colorIcons;
        colors[2] = t.colorInactiveText;
        colors[3] = t.colorText;
        colors[4] = t.colorInterfaceBg;
        colors[5] = t.colorClickGui;
        load();
        applyColors();
    }

    private void applyColors() {
        Theme t = ThemeManager.getInstance().getCurrentTheme();
        t.setAccent(colors[0]);
        t.colorIcons = colors[1];
        t.colorInactiveText = colors[2];
        t.colorText = colors[3];
        t.colorInterfaceBg = colors[4];
        t.colorClickGui = colors[5];
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        expandAnim.run(expanded);
        float ep = (float) expandAnim.getValue();

        float contentH = PAD * 2f + SLOTS.length * ROW_H + BTN_GAP + BTN_H;
        height = TAB + contentH * ep;
        x = sw - W - 4f;
        y = sh - height - 4f;

        appearAnim.run(true);
        scaleAnim.run(1f);

        float appear = (float) appearAnim.getValue();
        float scale = (float) scaleAnim.getValue();
        float appearShift = (1f - appear) * (height + 20f);
        y += appearShift;

        if (appear < 0.01f) return;

        float centerX = x + W / 2f;
        float centerY = y + height / 2f;

        if (scale < 0.99f) {
            context.getMatrices().push();
            context.getMatrices().translate(centerX, centerY, 0);
            context.getMatrices().scale(scale, scale, 1f);
            context.getMatrices().translate(-centerX, -centerY, 0);
        }

        Vector4f topRound = new Vector4f(8f, 8f, 0f, 0f);
        DrawUtil.drawRoundBlur(x, y, W, height, topRound, 
            ColorProvider.rgba(200, 200, 200, (int)(255 * appear)), 14f);
        DrawUtil.drawRound(x - 0.5f, y - 0.5f, W + 1f, height + 1f, 
            new Vector4f(8.5f, 8.5f, 0f, 0f), ColorProvider.rgba(48, 66, 122, (int)(70 * appear)));
        DrawUtil.drawRound(x, y, W, height, topRound, 
            ColorProvider.setAlpha(ColorProvider.getColorClickGui(), (int)(130 * appear)));

        DrawUtil.drawRound(x, y, W, TAB, topRound, 
            ColorProvider.setAlpha(ColorProvider.getColorHeaderBg(), (int)(110 * appear)));
        float titleSize = 8.5f;
        float titleW = Fonts.SFREGULAR.get().getWidth("Theme", titleSize);
        float arrowW = 7f;
        float groupGap = 5f;
        float groupTotal = titleW + groupGap + arrowW;
        float groupX = x + (W - groupTotal) / 2f;
        DrawUtil.drawText(Fonts.SFREGULAR.get(), "Theme", groupX, y + (TAB - titleSize) / 2f, 
            ColorProvider.rgba(255, 255, 255, (int)(255 * appear)), titleSize);
        drawArrow(groupX + titleW + groupGap + arrowW / 2f, y + TAB / 2f, !expanded, 
            ColorProvider.setAlpha(ColorProvider.getColorIcons(), (int)(255 * appear)));

        boolean tabHover = HoverUtil.isHovered(mouseX, mouseY, x, y, W, TAB);
        if (tabHover) CursorManager.requestHand();

        if (ep > 0.01f) {
            Scissor.push();
            Scissor.setFromComponentCoordinates(x, y + TAB, W, height - TAB);

            float rowY = y + TAB + PAD;
            for (int i = 0; i < SLOTS.length; i++) {
                renderRow(i, x + 4f, rowY, W - 8f, mouseX, mouseY, ep * appear);
                rowY += ROW_H;
            }

            float btnX = x + 4f;
            float btnY = rowY + BTN_GAP;
            float btnW = W - 8f;
            boolean btnHover = HoverUtil.isHovered(mouseX, mouseY, btnX, btnY, btnW, BTN_H);
            if (btnHover && ep > 0.9f) CursorManager.requestHand();
            int btnBg = btnHover
                    ? ColorProvider.rgba(60, 78, 140, (int) (110 * ep * appear))
                    : ColorProvider.rgba(48, 66, 122, (int) (70 * ep * appear));
            DrawUtil.drawRound(btnX, btnY, btnW, BTN_H, 3f, btnBg);
            String label = "Сбросить";
            float lw = Fonts.SFREGULAR.get().getWidth(label, 7f);
            DrawUtil.drawText(Fonts.SFREGULAR.get(), label, btnX + (btnW - lw) / 2f, btnY + (BTN_H - 7f) / 2f,
                    ColorProvider.rgba(230, 234, 245, (int) (255 * ep * appear)), 7f);

            Scissor.unset();
            Scissor.pop();
        }

        if (scale < 0.99f) {
            context.getMatrices().pop();
        }

        if (draggingSV) {
            hsv[1] = clamp01((mouseX - pickerX) / SV_SIZE);
            hsv[2] = 1f - clamp01((mouseY - pickerY) / SV_SIZE);
            applyHSV();
        } else if (draggingH) {
            hsv[0] = clamp01((float) (mouseY - pickerY) / SV_SIZE);
            applyHSV();
        }

        pickerAnim.run(editingSlot >= 0 && expanded);
        if (pickerAnim.getValue() > 0.01f) {
            renderPicker(mouseX, mouseY, (float) pickerAnim.getValue() * appear);
        }
    }

    private void renderRow(int slot, float rx, float ry, float rw, int mouseX, int mouseY, float ep) {
        boolean hov = HoverUtil.isHovered(mouseX, mouseY, rx, ry, rw, ROW_H);
        boolean active = editingSlot == slot;
        if ((hov || active) && ep > 0.9f) CursorManager.requestHand();

        if (hov || active) {
            DrawUtil.drawRound(rx, ry, rw, ROW_H - 1.5f, 3f, 
                ColorProvider.rgba(60, 78, 140, (int) (55 * ep)));
        }

        int textColor = active ? ColorProvider.getColorText() : ColorProvider.rgba(210, 214, 230, 255);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), SLOTS[slot], rx + 4f, ry + 4f,
                ColorProvider.setAlpha(textColor, (int) (255 * ep)), 7f);

        float sw = 11f;
        float sx = rx + rw - sw - 3f;
        float sy = ry + (ROW_H - sw) / 2f - 0.75f;
        DrawUtil.drawRound(sx - 0.75f, sy - 0.75f, sw + 1.5f, sw + 1.5f, 3f, 
            ColorProvider.rgba(255, 255, 255, (int) (60 * ep)));
        DrawUtil.drawRound(sx, sy, sw, sw, 2.5f, ColorProvider.setAlpha(colors[slot], (int) (255 * ep)));
    }

    private void renderPicker(int mouseX, int mouseY, float anim) {
        int a = (int) (255 * anim);
        float px = pickerX, py = pickerY;

        DrawUtil.drawRoundBlur(px - PICKER_PAD, py - PICKER_PAD, PICKER_W + PICKER_PAD * 2f, SV_SIZE + PICKER_PAD * 2f, 5f,
                ColorProvider.rgba(200, 200, 200, (int) (255 * anim)), 12f);
        DrawUtil.drawRound(px - PICKER_PAD, py - PICKER_PAD, PICKER_W + PICKER_PAD * 2f, SV_SIZE + PICKER_PAD * 2f, 5f,
                ColorProvider.setAlpha(ColorProvider.getColorClickGui(), (int) (140 * anim)));

        int cHue = ColorProvider.setAlpha(Color.HSBtoRGB(hsv[0], 1f, 1f), a);
        int white = ColorProvider.rgba(255, 255, 255, a);
        int clearWhite = ColorProvider.rgba(255, 255, 255, 0);
        int black = ColorProvider.rgba(0, 0, 0, a);
        int clearBlack = ColorProvider.rgba(0, 0, 0, 0);
        DrawUtil.drawRound(px, py, SV_SIZE, SV_SIZE, 2f, cHue);
        DrawUtil.drawRound(px, py, SV_SIZE, SV_SIZE, 2f, white, white, clearWhite, clearWhite);
        DrawUtil.drawRound(px, py, SV_SIZE, SV_SIZE, 2f, clearBlack, black, black, clearBlack);

        float scx = px + hsv[1] * SV_SIZE;
        float scy = py + (1f - hsv[2]) * SV_SIZE;
        DrawUtil.drawRound(scx - 2.5f, scy - 2.5f, 5f, 5f, 2.5f, ColorProvider.rgba(0, 0, 0, (int) (180 * anim)));
        DrawUtil.drawRound(scx - 1.75f, scy - 1.75f, 3.5f, 3.5f, 1.75f, white);

        float hueX = px + SV_SIZE + HUE_GAP;
        for (float i = 0; i <= SV_SIZE; i += 0.5f) {
            DrawUtil.drawRound(hueX, py + i, HUE_W, 1f, 0f, 
                ColorProvider.setAlpha(Color.HSBtoRGB(i / SV_SIZE, 1f, 1f), a));
        }
        float hcy = py + hsv[0] * SV_SIZE;
        DrawUtil.drawRound(hueX - 1.5f, hcy - 2f, HUE_W + 3f, 4f, 2f, 
            ColorProvider.rgba(0, 0, 0, (int) (180 * anim)));
        DrawUtil.drawRound(hueX - 0.5f, hcy - 1f, HUE_W + 1f, 2f, 1f, white);
    }

    private void drawArrow(float cx, float cy, boolean up, int color) {
        float w = 7f, h = 4f;
        int rows = 8;
        float top = cy - h / 2f;
        for (int i = 0; i < rows; i++) {
            float frac = i / (float) (rows - 1);
            float rowW = up ? w * frac : w * (1f - frac);
            float ry = top + frac * h;
            DrawUtil.drawRound(cx - rowW / 2f, ry, rowW, h / rows + 0.5f, 0f, color);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (HoverUtil.isHovered(mouseX, mouseY, x, y, W, TAB) && button == 0) {
            expanded = !expanded;
            if (!expanded) editingSlot = -1;
            return true;
        }

        if (expandAnim.getValue() < 0.5f) {
            return HoverUtil.isHovered(mouseX, mouseY, x, y, W, height);
        }

        if (editingSlot >= 0 && pickerAnim.getValue() > 0.5f) {
            if (HoverUtil.isHovered(mouseX, mouseY, pickerX, pickerY, SV_SIZE, SV_SIZE) && button == 0) {
                draggingSV = true;
                hsv[1] = clamp01((float) (mouseX - pickerX) / SV_SIZE);
                hsv[2] = 1f - clamp01((float) (mouseY - pickerY) / SV_SIZE);
                applyHSV();
                return true;
            }
            if (HoverUtil.isHovered(mouseX, mouseY, pickerX + SV_SIZE + HUE_GAP - 1f, pickerY, HUE_W + 2f, SV_SIZE) && button == 0) {
                draggingH = true;
                hsv[0] = clamp01((float) (mouseY - pickerY) / SV_SIZE);
                applyHSV();
                return true;
            }
            boolean insidePicker = HoverUtil.isHovered(mouseX, mouseY,
                    pickerX - PICKER_PAD, pickerY - PICKER_PAD, PICKER_W + PICKER_PAD * 2f, SV_SIZE + PICKER_PAD * 2f);
            if (insidePicker) return true;
        }

        float btnY = y + TAB + PAD + SLOTS.length * ROW_H + BTN_GAP;
        if (HoverUtil.isHovered(mouseX, mouseY, x + 4f, btnY, W - 8f, BTN_H) && button == 0) {
            resetToDefault();
            return true;
        }

        float rowY = y + TAB + PAD;
        for (int i = 0; i < SLOTS.length; i++) {
            if (HoverUtil.isHovered(mouseX, mouseY, x + 4f, rowY, W - 8f, ROW_H) && button == 0) {
                editingSlot = (editingSlot == i) ? -1 : i;
                if (editingSlot >= 0) {
                    loadHSV(i);
                    pickerX = x - PICKER_W - PICKER_PAD - 4f;
                    pickerY = rowY + ROW_H / 2f - SV_SIZE / 2f;
                    float sh = mc.getWindow().getScaledHeight();
                    pickerY = Math.max(PICKER_PAD + 2f, Math.min(pickerY, sh - SV_SIZE - PICKER_PAD - 2f));
                }
                return true;
            }
            rowY += ROW_H;
        }

        return HoverUtil.isHovered(mouseX, mouseY, x, y, W, height);
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingSV || draggingH) save();
        draggingSV = false;
        draggingH = false;
    }

    private void resetToDefault() {
        System.arraycopy(DEFAULTS, 0, colors, 0, colors.length);
        editingSlot = -1;
        applyColors();
        save();
    }

    public static void applyStartupTheme() {
        int[] c = DEFAULTS.clone();
        if (FILE.exists()) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(FILE.toPath())).getAsJsonObject();
                if (json.has("colors")) {
                    JsonArray arr = json.getAsJsonArray("colors");
                    for (int i = 0; i < Math.min(arr.size(), c.length); i++) {
                        c[i] = arr.get(i).getAsInt();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        Theme t = ThemeManager.getInstance().getCurrentTheme();
        t.setAccent(c[0]);
        t.colorIcons = c[1];
        t.colorInactiveText = c[2];
        t.colorText = c[3];
        t.colorInterfaceBg = c[4];
        t.colorClickGui = c[5];
    }

    private void applyHSV() {
        if (editingSlot < 0) return;
        colors[editingSlot] = Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]) | 0xFF000000;
        applyColors();
    }

    private void loadHSV(int slot) {
        Color c = new Color(colors[slot], true);
        Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsv);
    }

    private static float clamp01(double v) {
        return (float) Math.max(0.0, Math.min(1.0, v));
    }

    public void save() {
        try {
            if (!DIR.exists()) DIR.mkdirs();
            JsonObject json = new JsonObject();
            JsonArray arr = new JsonArray();
            for (int c : colors) arr.add(c);
            json.add("colors", arr);
            Files.writeString(FILE.toPath(), GSON.toJson(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        if (!FILE.exists()) return;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(FILE.toPath())).getAsJsonObject();
            if (json.has("colors")) {
                JsonArray arr = json.getAsJsonArray("colors");
                for (int i = 0; i < Math.min(arr.size(), colors.length); i++) {
                    colors[i] = arr.get(i).getAsInt();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
