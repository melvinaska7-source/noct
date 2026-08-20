package polar.ru.client.modules.impl.render.base.implement;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.api.utils.input.KeyBoardUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.impl.misc.ServerHelper;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;
import polar.ru.client.modules.impl.render.base.implement.HudFx;
import polar.ru.polar;

public class ItemBinds
extends InterfaceProcessing {
    private static final float BASE_MIN_WIDTH = 64.0f;
    private static final float EXTRA_WIDTH = 0.0f;
    private static final float ROW_RIGHT_MARGIN = 25.0f;
    private static final float ROW_HEIGHT = 10.0f;
    private static final float HEADER_HEIGHT = 16.0f;
    private static final float HEADER_GAP = 0.2f;
    private static final float CONTENT_PAD_TOP = 6.0f;
    private static final float CONTENT_PAD_BOTTOM = 0.8f;
    private final Map<ServerHelper.HelperBind, AnimationUtils> animations = new HashMap<ServerHelper.HelperBind, AnimationUtils>();
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

    public ItemBinds(Draggable draggable) {
        super(draggable);
    }

    private AnimationUtils getAnimation(ServerHelper.HelperBind bind) {
        return this.animations.computeIfAbsent(bind, b2 -> new AnimationUtils(0.0f, 10.5f, Easings.QUAD_OUT));
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
        ServerHelper serverHelper = ServerHelper.INSTANCE;
        if (serverHelper != null && serverHelper.isEnable()) {
            for (ServerHelper.HelperBind helperBind : serverHelper.getActiveHelperBinds()) {
                if (helperBind.bind().getKey() != -1) {
                    this.getAnimation(helperBind).update(1.0f);
                    continue;
                }
                this.getAnimation(helperBind).update(0.0f);
            }
        }
        boolean hasVisibleBinds = false;
        if (serverHelper != null && serverHelper.isEnable()) {
            for (ServerHelper.HelperBind bind : serverHelper.getActiveHelperBinds()) {
                if (bind.bind().getKey() == -1 || !(this.getAnimation(bind).getValue() > 0.01f)) continue;
                hasVisibleBinds = true;
                break;
            }
        }
        boolean bl = mc != null && ItemBinds.mc.currentScreen instanceof ChatScreen;
        boolean shouldShowPanel = hasVisibleBinds || bl;
        this.panelAlphaAnimation.update(shouldShowPanel ? 1.0f : 0.0f);
        float panelProgress = this.panelAlphaAnimation.getValue();
        if (panelProgress <= 0.01f) {
            this.draggable.setWidth(0.0f);
            this.draggable.setHeight(0.0f);
            return;
        }
        if (serverHelper != null && serverHelper.isEnable()) {
            for (ServerHelper.HelperBind bind : serverHelper.getActiveHelperBinds()) {
                if (bind.bind().getKey() == -1) continue;
                ++enabledCount;
                String keyName = this.toEnglish(KeyBoardUtils.getKeyName(bind.bind().getKey())).toUpperCase();
                float bindWidth = 10.0f + this.issue(12).getWidth(bind.name()) + 2.0f + this.issue(10).getWidth(keyName) + 25.0f;
                if (!(bindWidth > targetWidth)) continue;
                targetWidth = bindWidth;
            }
        }
        float targetHeight = 22.2f + (float)enabledCount * 10.2f + 0.8f;
        if (bl && enabledCount == 0) {
            targetHeight = 23.0f;
            if (targetWidth < 64.0f) {
                targetWidth = 64.0f;
            }
        }
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
        this.issue(14).draw(matrices, "ItemBinds", x2 + 5.2f, headerTextY, ColorUtils.rgba(255, 255, 255, panelAlphaMul));
        if (this.isFlatStyle()) {
            float headerIconBgSize = 11.0f;
            float headerIconBgX = rightEdge - 14.0f;
            float headerIconBgY = y2 + (22.2f - headerIconBgSize) / 2.0f;
            int headerIconBgColor = ColorUtils.setAlphaColor(colorTheme, 63);
            RenderUtils.drawRoundedRect(matrices, headerIconBgX, headerIconBgY, headerIconBgSize, headerIconBgSize, 2.0f, headerIconBgColor);
        }
        this.icons(14).draw(matrices, "B", rightEdge - 12.0f, headerIconY, ColorUtils.setAlphaColor(colorTheme, panelAlphaMul));
        float offsetY = 22.2f;
        if (serverHelper != null && serverHelper.isEnable()) {
            for (ServerHelper.HelperBind bind : serverHelper.getActiveHelperBinds()) {
                AnimationUtils anim;
                float animValue;
                if (bind.bind().getKey() == -1 || (animValue = (anim = this.getAnimation(bind)).getValue()) <= 0.01f) continue;
                String keyName = this.toEnglish(KeyBoardUtils.getBindName(bind.bind().getKey())).toUpperCase();
                float keyBoxWidth = Math.max(this.issue(10).getStringWidth(keyName) + 4.0f, 9.0f);
                int alpha = (int)(255.0f * animValue * eased);
                int textColor = ColorUtils.rgba(255, 255, 255, alpha);
                int grayColor = ColorUtils.rgba(150, 150, 150, alpha);
                float iconX = x2 + 5.2f;
                float nameX = iconX + 8.0f + 2.0f;
                float keyBoxX = nameX + this.issue(12).getStringWidth(bind.name()) + 2.0f;
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
                ItemStack itemStack = new ItemStack((ItemConvertible)bind.item());
                matrices.push();
                float itemY = bindBgY + (bindBgHeight - 8.0f) / 2.0f;
                matrices.translate(iconX, itemY, 0.0f);
                matrices.scale(0.5f, 0.5f, 1.0f);
                eventRender.getContext().drawItem(itemStack, 0, 0);
                matrices.pop();
                float rowTextY = bindBgY + (bindBgHeight - this.issue(12).getHeight()) / 2.0f;
                float rowKeyY = bindBgY + (bindBgHeight - this.issue(10).getHeight()) / 2.0f;
                this.issue(12).draw(matrices, bind.name(), nameX, rowTextY, textColor);
                this.issue(10).drawCenteredString(matrices, keyName, keyBoxX + keyBoxWidth / 2.0f, rowKeyY, grayColor);
                offsetY += 10.0f * animValue + 0.2f;
            }
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

