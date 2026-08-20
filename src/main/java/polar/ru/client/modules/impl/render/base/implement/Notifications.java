package polar.ru.client.modules.impl.render.base.implement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.api.utils.notification.NotificationManager;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.polar;

public class Notifications
extends InterfaceProcessing {
    private static final float DEFAULT_PAD_X = 7.0f;
    private static final float DEFAULT_ICON_TEXT_GAP = 1.0f;
    private static final float PREVIEW_ICON_TEXT_GAP = 2.0f;
    private final BooleanSetting lowArmorNotify = new BooleanSetting("Уведомление о броне", true);
    private final BooleanSetting potionExpireNotify = new BooleanSetting("Окончание зелий", true);
    private final BooleanSetting itemPickupNotify = new BooleanSetting("Подбор предметов", true);
    private final BooleanSetting totemPopNotify = new BooleanSetting("Поп тотема", true);
    private final Map<NotificationManager.Entry, AnimationUtils> appearAnimations = new HashMap<NotificationManager.Entry, AnimationUtils>();
    private final Map<NotificationManager.Entry, Float> currentYPositions = new HashMap<NotificationManager.Entry, Float>();
    private final Set<NotificationManager.Entry> activeEntriesScratch = new HashSet<NotificationManager.Entry>();
    private long lastRenderTime = System.currentTimeMillis();
    private float previewAlpha = 0.0f;

    public Notifications(Draggable draggable) {
        super(draggable);
    }

    public BooleanSetting getLowArmorNotify() {
        return this.lowArmorNotify;
    }

    public BooleanSetting getPotionExpireNotify() {
        return this.potionExpireNotify;
    }

    public BooleanSetting getItemPickupNotify() {
        return this.itemPickupNotify;
    }

    public BooleanSetting getTotemPopNotify() {
        return this.totemPopNotify;
    }

    private Font issue(int size) {
        return Fonts.getFont("suisse", size);
    }

    private Font icons(int size) {
        return Fonts.getFont("icon", size);
    }

    private String getEntryText(NotificationManager.Entry entry) {
        if (entry.isCustom()) {
            return entry.customText;
        }
        String state = entry.enabled ? "Включен!" : "Выключен!";
        return entry.moduleName + " " + state;
    }

    private float getDefaultEntryWidth(NotificationManager.Entry entry, float padX) {
        String text = this.getEntryText(entry);
        String iconGlyph = entry.categoryIcon != null && !entry.categoryIcon.isEmpty() ? entry.categoryIcon : "?";
        return this.issue(13).getWidth(text) + this.icons(14).getWidth(iconGlyph) + padX * 2.0f + 1.0f;
    }

    private float getPreviewWidth(String previewText, String previewIconGlyph, float padX) {
        return this.issue(13).getWidth(previewText) + this.icons(16).getWidth(previewIconGlyph) + padX * 2.0f + 2.0f;
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        this.DefaultStyle(eventRender);
        super.onRender(eventRender);
    }

    private void DefaultStyle(EventRender.Default eventRender) {
        float previewWidth;
        if (mc == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        float deltaTime = (float)(currentTime - this.lastRenderTime) / 1000.0f;
        this.lastRenderTime = currentTime;
        List<NotificationManager.Entry> entries = NotificationManager.getActive();
        boolean isChatOpen = Notifications.mc.currentScreen instanceof ChatScreen;
        boolean shouldRender = !entries.isEmpty() || isChatOpen;
        float targetPreviewAlpha = isChatOpen ? 0.7f : 0.0f;
        float alphaSpeed = 8.0f;
        this.previewAlpha += (targetPreviewAlpha - this.previewAlpha) * Math.min(1.0f, alphaSpeed * deltaTime);
        if (!shouldRender && this.previewAlpha < 0.01f) {
            this.appearAnimations.clear();
            this.currentYPositions.clear();
            this.previewAlpha = 0.0f;
            return;
        }
        float baseX = this.draggable.getX();
        float baseY = this.draggable.getY();
        int colorTheme = !polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow") ? polar.INSTANCE.themeStorage.getThemes().getTheme().color[0] : ColorUtils.getThemeColor();
        boolean drawSquares = this.isUnusualRectType();
        long now = System.currentTimeMillis();
        float height = 16.0f;
        float spacing = 3.0f;
        float lerpSpeed = 12.0f;
        float padX = 7.0f;
        String previewText = "Кликни на меня для открытия настроек!";
        String previewIconGlyph = "A";
        float previewIconW = this.icons(16).getWidth(previewIconGlyph);
        float maxWidth = previewWidth = this.getPreviewWidth(previewText, previewIconGlyph, padX);
        for (NotificationManager.Entry entry2 : entries) {
            float width = this.getDefaultEntryWidth(entry2, padX);
            if (!(width > maxWidth)) continue;
            maxWidth = width;
        }
        float targetY = baseY;
        if (this.previewAlpha > 0.01f) {
            float x2 = baseX + (maxWidth - previewWidth) * 0.5f;
            float alpha = this.previewAlpha;
            float scale = 0.9f + 0.100000024f * alpha;
            float slideY = 6.0f * (1.0f - alpha);
            float renderY = targetY + slideY;
            int base = ColorUtils.setAlphaColor(ColorUtils.rgba(50, 50, 50, 255), (int)(255.0f * alpha));
            int top = ColorUtils.setAlphaColor(ColorUtils.darken(colorTheme, 0.15f), (int)(255.0f * alpha));
            int bottom = ColorUtils.setAlphaColor(ColorUtils.darken(colorTheme, 0.05f), (int)(255.0f * alpha));
            float cx = x2 + previewWidth * 0.5f;
            float cy = renderY + height * 0.5f;
            MatrixStack ms = eventRender.getContext().getMatrices();
            ms.push();
            ms.translate(cx, cy, 0.0f);
            ms.scale(scale, scale, 1.0f);
            ms.translate(-cx, -cy, 0.0f);
            if (this.isFlatStyle()) {
                int bgColor = ColorUtils.rgba(20, 20, 20, 255);
                RenderUtils.drawRoundedRect(ms, x2, renderY, previewWidth, height, 6.0f, bgColor);
            } else {
                int shadowColor = ColorUtils.rgba(0, 0, 0, 200);
                RenderUtils.drawShadow(ms, x2 - 2.0f, renderY - 2.0f, previewWidth + 4.0f, height + 4.0f, 6.0f, shadowColor);
                int bgColor = ColorUtils.rgba(20, 20, 20, 100);
                RenderUtils.drawBlur(ms, x2, renderY, previewWidth, height, 6.0f, 5.0f, ColorUtils.rgba(255, 255, 255, 255));
                RenderUtils.drawBlur(ms, x2, renderY, previewWidth, height, 6.0f, 5.0f, ColorUtils.rgba(0, 0, 0, 180));
                RenderUtils.drawRoundedRect(ms, x2, renderY, previewWidth, height, 6.0f, bgColor);
            }
            int textColor = ColorUtils.setAlphaColor(-1, (int)(255.0f * alpha));
            int iconColor = ColorUtils.setAlphaColor(colorTheme, (int)(255.0f * alpha));
            float previewIconY = renderY + (height - this.icons(16).getHeight()) / 2.0f;
            float previewTextY = renderY + (height - this.issue(13).getHeight()) / 2.0f;
            this.icons(16).draw(ms, previewIconGlyph, x2 + padX - 3.5f, previewIconY, iconColor);
            this.issue(13).draw(ms, previewText, x2 + padX + previewIconW + 5.5f, previewTextY, textColor);
            ms.pop();
            targetY += height + spacing;
        }
        for (NotificationManager.Entry entry3 : entries) {
            float diff;
            float appear;
            AnimationUtils anim = this.appearAnimations.computeIfAbsent(entry3, e2 -> new AnimationUtils(0.0f, 12.0f, Easings.QUAD_OUT));
            long age = now - entry3.startTime;
            anim.update(1.0f);
            float alpha = appear = anim.getValue();
            if (age > 2300L) {
                alpha = (1.0f - (float)(age - 2300L) / 200.0f) * appear;
            }
            if (alpha <= 0.0f) {
                targetY += height + spacing;
                continue;
            }
            Float currentY = this.currentYPositions.get(entry3);
            if (currentY == null) {
                currentY = Float.valueOf(targetY);
            }
            currentY = Math.abs(diff = targetY - currentY.floatValue()) > 0.01f ? Float.valueOf(currentY.floatValue() + diff * Math.min(1.0f, lerpSpeed * deltaTime)) : Float.valueOf(targetY);
            this.currentYPositions.put(entry3, currentY);
            String text = this.getEntryText(entry3);
            String iconGlyph = entry3.categoryIcon != null && !entry3.categoryIcon.isEmpty() ? entry3.categoryIcon : "?";
            float iconW = this.icons(14).getWidth(iconGlyph);
            float width = this.getDefaultEntryWidth(entry3, padX);
            float x3 = baseX + (maxWidth - width) * 0.5f;
            float slide = 6.0f * (1.0f - appear);
            float renderY = currentY.floatValue() + slide;
            float scale = 0.9f + 0.100000024f * alpha;
            boolean disabled = !entry3.isCustom() && !entry3.enabled;
            int disabledRed = ColorUtils.rgba(200, 55, 55, 255);
            int base = ColorUtils.setAlphaColor(ColorUtils.rgba(50, 50, 50, 255), (int)(255.0f * alpha));
            int top = ColorUtils.setAlphaColor(ColorUtils.darken(colorTheme, 0.15f), (int)(255.0f * alpha));
            int bottom = ColorUtils.setAlphaColor(ColorUtils.darken(colorTheme, 0.05f), (int)(255.0f * alpha));
            float cx = x3 + width * 0.5f;
            float cy = renderY + height * 0.5f;
            MatrixStack ms = eventRender.getContext().getMatrices();
            ms.push();
            ms.translate(cx, cy, 0.0f);
            ms.scale(scale, scale, 1.0f);
            ms.translate(-cx, -cy, 0.0f);
            if (this.isFlatStyle()) {
                int bgColor = ColorUtils.rgba(20, 20, 20, 255);
                RenderUtils.drawRoundedRect(ms, x3, renderY, width, height, 6.0f, bgColor);
            } else {
                int shadowColor = ColorUtils.rgba(0, 0, 0, 200);
                RenderUtils.drawShadow(ms, x3 - 2.0f, renderY - 2.0f, width + 4.0f, height + 4.0f, 6.0f, shadowColor);
                int bgColor = ColorUtils.rgba(20, 20, 20, 100);
                RenderUtils.drawBlur(ms, x3, renderY, width, height, 6.0f, 5.0f, ColorUtils.rgba(255, 255, 255, 255));
                RenderUtils.drawBlur(ms, x3, renderY, width, height, 6.0f, 5.0f, ColorUtils.rgba(0, 0, 0, 180));
                RenderUtils.drawRoundedRect(ms, x3, renderY, width, height, 6.0f, bgColor);
            }
            int textColor = ColorUtils.setAlphaColor(-1, (int)(255.0f * alpha));
            int iconColor = ColorUtils.setAlphaColor(colorTheme, (int)(255.0f * alpha));
            float notifIconY = renderY + (height - this.icons(14).getHeight()) / 2.0f;
            float notifTextY = renderY + (height - this.issue(13).getHeight()) / 2.0f;
            this.icons(14).draw(ms, iconGlyph, x3 + padX - 1.5f, notifIconY, iconColor);
            float textX = x3 + padX + iconW + 1.0f;
            if (!entry3.isCustom()) {
                String modulePart = entry3.moduleName + " ";
                String statePart = text.length() > modulePart.length() ? text.substring(modulePart.length()) : "";
                int stateColor = disabled ? disabledRed : iconColor;
                this.issue(13).draw(ms, modulePart, textX + 2.0f, notifTextY, textColor);
                this.issue(13).draw(ms, statePart, textX + this.issue(13).getWidth(modulePart) - 0.5f + 2.0f, notifTextY, stateColor);
            } else {
                this.issue(13).draw(ms, text, textX, notifTextY, textColor);
            }
            ms.pop();
            targetY += height + spacing;
        }
        this.activeEntriesScratch.clear();
        this.activeEntriesScratch.addAll(entries);
        this.appearAnimations.keySet().removeIf(entry -> !this.activeEntriesScratch.contains(entry));
        this.currentYPositions.keySet().removeIf(entry -> !this.activeEntriesScratch.contains(entry));
        this.draggable.setWidth(maxWidth);
        this.draggable.setHeight(Math.max(1.0f, targetY - baseY));
    }
}

