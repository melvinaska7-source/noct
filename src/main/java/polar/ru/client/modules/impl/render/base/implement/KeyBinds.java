package polar.ru.client.modules.impl.render.base.implement;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.api.utils.input.KeyBoardUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.api.utils.scissor.ScissorUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;
import polar.ru.client.modules.impl.render.base.implement.HudFx;
import polar.ru.polar;

public class KeyBinds
extends InterfaceProcessing {
    private static final float BASE_MIN_WIDTH = 64.0f;
    private static final float EXTRA_WIDTH = 0.0f;
    private static final float ROW_RIGHT_MARGIN = 25.0f;
    private static final float ROW_HEIGHT = 10.0f;
    private static final float HEADER_HEIGHT = 16.0f;
    private static final float HEADER_GAP = 0.2f;
    private static final float CONTENT_PAD_TOP = 6.0f;
    private static final float CONTENT_PAD_BOTTOM = 0.8f;
    private final Map<Module, AnimationUtils> animations = new HashMap<Module, AnimationUtils>();
    private final AnimationUtils widthAnimation = new AnimationUtils(60.0f, 10.5f, Easings.QUAD_OUT);
    private final AnimationUtils heightAnimation = new AnimationUtils(16.0f, 10.5f, Easings.QUAD_OUT);
    private final AnimationUtils panelAlphaAnimation = HudFx.newAppearAnimation();
    private static final Map<Character, Character> RU_TO_EN = new HashMap<Character, Character>();

    private Font issue(int size) {
        return Fonts.getFont("suisse", size);
    }

    private Font icons(int size) {
        return Fonts.getFont("icon", size);
    }

    public KeyBinds(Draggable draggable) {
        super(draggable);
    }

    private AnimationUtils getAnimation(Module module) {
        return this.animations.computeIfAbsent(module, m2 -> new AnimationUtils(0.0f, 10.5f, Easings.QUAD_OUT));
    }

    private String toEnglish(String text) {
        StringBuilder result = new StringBuilder();
        for (char c2 : text.toCharArray()) {
            result.append(RU_TO_EN.getOrDefault(Character.valueOf(c2), Character.valueOf(c2)));
        }
        return result.toString();
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        this.DefaultStyle(eventRender);
        super.onRender(eventRender);
    }

    public void DefaultStyle(EventRender.Default eventRender) {
        float baseX = this.draggable.getX();
        float y2 = this.draggable.getY();
        int colorTheme = this.getStableThemeColor();
        float targetWidth = 64.0f;
        int enabledCount = 0;
        for (Module module : ModuleClass.INSTANCE.getObject()) {
            if (module.getKey() == -1) continue;
            this.getAnimation(module).update(module.isEnable() ? 1.0f : 0.0f);
        }
        boolean hasVisibleModules = false;
        for (Module module : ModuleClass.INSTANCE.getObject()) {
            if (module.getKey() == -1 || !(this.getAnimation(module).getValue() > 0.01f)) continue;
            hasVisibleModules = true;
            break;
        }
        boolean isChatOpen = mc != null && KeyBinds.mc.currentScreen instanceof ChatScreen;
        boolean shouldShowPanel = hasVisibleModules || isChatOpen;
        this.panelAlphaAnimation.update(shouldShowPanel ? 1.0f : 0.0f);
        float panelProgress = this.panelAlphaAnimation.getValue();
        if (panelProgress <= 0.01f) {
            this.draggable.setWidth(0.0f);
            this.draggable.setHeight(0.0f);
            return;
        }
        for (Module module : ModuleClass.INSTANCE.getObject()) {
            if (module.getKey() == -1 || !module.isEnable()) continue;
            ++enabledCount;
            String keyName = this.toEnglish(KeyBoardUtils.getKeyName(module.getKey()));
            Font iconFont = this.icons(11);
            float f2 = iconFont != null ? iconFont.getWidth(module.getCategory().getIcons()) : 0.0f;
            float iconWidth = f2;
            float moduleWidth = iconWidth + 4.0f + this.issue(12).getWidth(module.getDisplayName()) + this.issue(10).getWidth(keyName) + 25.0f;
            if (!(moduleWidth > targetWidth)) continue;
            targetWidth = moduleWidth;
        }
        float targetHeight = 22.2f + (float)enabledCount * 10.2f + 0.8f;
        this.widthAnimation.update(targetWidth);
        this.heightAnimation.update(targetHeight);
        float width = this.widthAnimation.getValue() + 0.0f;
        float height = this.heightAnimation.getValue();
        float rightEdge = baseX + width;
        float x2 = baseX;
        MatrixStack matrices = eventRender.getContext().getMatrices();
        float pivotX = x2 + width / 2.0f;
        float pivotY = y2 + height / 2.0f;
        float eased = HudFx.pushTransform(matrices, panelProgress, pivotX, pivotY);
        int panelAlphaMul = (int)(255.0f * eased);
        if (this.isFlatStyle()) {
            int bgColor = ColorUtils.rgba(20, 20, 20, 255);
            RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, 6.0f, bgColor);
        } else {
            int shadowColor = ColorUtils.rgba(0, 0, 0, (int)(200.0f * eased));
            RenderUtils.drawShadow(matrices, x2 - 2.0f, y2 - 2.0f, width + 4.0f, height + 4.0f, 6.0f, shadowColor);
            int bgColor = ColorUtils.rgba(20, 20, 20, (int)(100.0f * eased));
            RenderUtils.drawBlur(matrices, x2, y2, width, height, 6.0f, 5.0f, ColorUtils.rgba(255, 255, 255, panelAlphaMul));
            RenderUtils.drawBlur(matrices, x2, y2, width, height, 6.0f, 5.0f, ColorUtils.rgba(0, 0, 0, (int)(180.0f * eased)));
            RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, 6.0f, bgColor);
            float blueLineWidth = width * 0.4f - 5.0f;
            float blueLineX = x2 + (width - blueLineWidth) / 2.0f + 13.0f;
            int themeLineColor = ColorUtils.setAlphaColor(colorTheme, panelAlphaMul);
            RenderUtils.drawRoundedRect(matrices, blueLineX, y2 - 1.5f, blueLineWidth, 3.5f, 1.0f, themeLineColor);
        }
        float headerTextY = y2 + (22.2f - this.issue(14).getHeight()) / 2.0f;
        float headerIconY = y2 + (22.2f - this.icons(14).getHeight()) / 2.0f;
        this.issue(14).draw(matrices, "Keybinds", x2 + 5.2f, headerTextY, ColorUtils.rgba(255, 255, 255, panelAlphaMul));
        if (this.isFlatStyle()) {
            float headerIconBgSize = 11.0f;
            float headerIconBgX = rightEdge - 14.0f;
            float headerIconBgY = y2 + (22.2f - headerIconBgSize) / 2.0f;
            int headerIconBgColor = ColorUtils.setAlphaColor(colorTheme, 63);
            RenderUtils.drawRoundedRect(matrices, headerIconBgX, headerIconBgY, headerIconBgSize, headerIconBgSize, 2.0f, headerIconBgColor);
        }
        this.icons(14).draw(matrices, "A", rightEdge - 12.0f, headerIconY, ColorUtils.setAlphaColor(colorTheme, panelAlphaMul));
        float offsetY = 22.2f;
        for (Module module : ModuleClass.INSTANCE.getObject()) {
            AnimationUtils anim;
            float animValue;
            if (module.getKey() == -1 || (animValue = (anim = this.getAnimation(module)).getValue()) <= 0.01f) continue;
            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(x2, y2, width, height);
            String keyName = this.toEnglish(KeyBoardUtils.getBindName(module.getKey()));
            float keyBoxWidth = Math.max(this.issue(10).getStringWidth(keyName) + 4.0f, 9.0f);
            int alpha = (int)(255.0f * animValue * eased);
            int textColor = ColorUtils.rgba(255, 255, 255, alpha);
            Font iconFont = this.icons(13);
            float nameX = x2 + 5.2f;
            float iconWidth = iconFont != null ? iconFont.getWidth(module.getCategory().getIcons()) : 0.0f;
            float iconX = nameX + this.issue(13).getStringWidth(module.getDisplayName()) + 4.0f;
            float keyBoxX = iconX + iconWidth + 4.0f;
            float rowEndX = keyBoxX + keyBoxWidth + 5.0f;
            float rowContentWidth = rowEndX - x2;
            float bindBgY = y2 + offsetY - 6.0f;
            float bindBgHeight = 16.8f;
            int contentBgColor = ColorUtils.rgba(20, 20, 20, (int)(100.0f * eased));
            if (!this.isFlatStyle()) {
                RenderUtils.drawBlur(matrices, x2, bindBgY, rowContentWidth, bindBgHeight, 6.0f, 5.0f, ColorUtils.rgba(255, 255, 255, panelAlphaMul));
                RenderUtils.drawBlur(matrices, x2, bindBgY, rowContentWidth, bindBgHeight, 6.0f, 5.0f, ColorUtils.rgba(0, 0, 0, (int)(180.0f * eased)));
            }
            RenderUtils.drawRoundedRect(matrices, x2, bindBgY, rowContentWidth, bindBgHeight, 6.0f, contentBgColor);
            float rowTextY = bindBgY + (bindBgHeight - this.issue(13).getHeight()) / 2.0f;
            float rowIconY = bindBgY + (bindBgHeight - (iconFont != null ? iconFont.getHeight() : 0.0f)) / 2.0f;
            float rowKeyY = bindBgY + (bindBgHeight - this.issue(12).getHeight()) / 2.0f;
            this.issue(13).draw(matrices, module.getDisplayName(), nameX, rowTextY, textColor);
            if (iconFont != null) {
                String categoryIcon = module.getCategory().getIcons();
                float categoryIconDrawX = iconX;
                float categoryIconDrawY = rowIconY;
                if (!this.isFlatStyle()) {
                    float boxPad = 2.0f;
                    float boxX = categoryIconDrawX - boxPad;
                    float boxY = categoryIconDrawY - boxPad;
                    float boxW = iconWidth + boxPad * 2.0f;
                    float boxH = iconFont.getHeight() + boxPad * 2.0f;
                    RenderUtils.drawBlur(matrices, boxX, boxY, boxW, boxH, 1.5f, 5.0f, ColorUtils.rgba(255, 255, 255, panelAlphaMul));
                    RenderUtils.drawBlur(matrices, boxX, boxY, boxW, boxH, 1.5f, 5.0f, ColorUtils.rgba(0, 0, 0, (int)(180.0f * eased)));
                }
                iconFont.draw(matrices, categoryIcon, categoryIconDrawX, categoryIconDrawY, ColorUtils.setAlphaColor(colorTheme, alpha));
            }
            this.issue(12).drawCenteredString(matrices, keyName, keyBoxX + keyBoxWidth / 2.0f, rowKeyY, ColorUtils.setAlphaColor(colorTheme, alpha));
            offsetY += 10.0f * animValue + 0.2f;
            ScissorUtils.pop();
            ScissorUtils.unset();
        }
        HudFx.popTransform(matrices);
        this.draggable.setWidth(width);
        this.draggable.setHeight(height);
    }

    private int getStableThemeColor() {
        if (!polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor();
    }

    static {
        String ru = "йцукенгшщзхъфывапролджэячсмитьбюЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ";
        String en = "qwertyuiop[]asdfghjkl;'zxcvbnm,.QWERTYUIOP[]ASDFGHJKL;'ZXCVBNM,.";
        for (int i2 = 0; i2 < ru.length(); ++i2) {
            RU_TO_EN.put(Character.valueOf(ru.charAt(i2)), Character.valueOf(en.charAt(i2)));
        }
    }
}

