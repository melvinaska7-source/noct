package polar.ru.client.modules.impl.render.base.implement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.api.utils.render.fonts.ttf.MCFontRenderer;
import polar.ru.api.utils.scissor.ScissorUtils;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;
import polar.ru.client.modules.impl.render.base.implement.HudFx;
import polar.ru.polar;

public class Potions
extends InterfaceProcessing {
    private static final float BASE_MIN_WIDTH = 64.0f;
    private static final float EXTRA_WIDTH = 0.0f;
    private static final float ROW_HEIGHT = 10.0f;
    private static final float HEADER_HEIGHT = 16.0f;
    private static final float HEADER_GAP = 0.2f;
    private static final float CONTENT_PAD_TOP = 6.0f;
    private static final float CONTENT_PAD_BOTTOM = 0.8f;
    private static final int EXPIRING_TICKS = 100;
    private static final float PULSE_SPEED_EXPIRING = 3.8f;
    private static final float PULSE_SPEED_BAD = 1.6f;
    private final Map<StatusEffect, AnimationUtils> animations = new LinkedHashMap<StatusEffect, AnimationUtils>();
    private final Map<StatusEffect, PotionSnapshot> snapshots = new HashMap<StatusEffect, PotionSnapshot>();
    private final Map<StatusEffect, Integer> maxDurations = new HashMap<StatusEffect, Integer>();
    private final Set<StatusEffect> renderOrderSeen = new HashSet<StatusEffect>();
    private final AnimationUtils widthAnimation = new AnimationUtils(60.0f, 10.5f, Easings.QUAD_OUT);
    private final AnimationUtils heightAnimation = new AnimationUtils(16.0f, 10.5f, Easings.QUAD_OUT);
    private final AnimationUtils panelAlphaAnimation = HudFx.newAppearAnimation();

    private Font issue(int size) {
        return Fonts.getFont("suisse", size);
    }

    public Potions(Draggable draggable) {
        super(draggable);
    }

    private MCFontRenderer myfont(int size) {
        return Fonts.getTtfFont("myfont.ttf", size);
    }

    private AnimationUtils getAnimation(StatusEffect effect) {
        return this.animations.computeIfAbsent(effect, e2 -> new AnimationUtils(0.0f, 10.5f, Easings.QUAD_OUT));
    }

    private static String getLevelSuffix(int level) {
        int n2 = Math.max(1, level);
        return switch (n2) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> "X".repeat(n2 / 10) + Potions.getLevelSuffix(n2 % 10 == 0 ? 10 : n2 % 10);
        };
    }

    private static String formatDuration(int duration, boolean infinite) {
        if (infinite) {
            return "inf";
        }
        int seconds = Math.max(0, duration / 20);
        int secs = seconds % 60;
        return seconds / 60 + ":" + (String)(secs < 10 ? "0" + secs : String.valueOf(secs));
    }

    private void updateSnapshot(StatusEffectInstance effect) {
        StatusEffect type = (StatusEffect)effect.getEffectType().value();
        PotionSnapshot s2 = this.snapshots.computeIfAbsent(type, e2 -> new PotionSnapshot());
        s2.entry = effect.getEffectType();
        s2.baseName = I18n.translate((String)effect.getTranslationKey(), (Object[])new Object[0]);
        s2.amplifier = effect.getAmplifier() + 1;
        s2.duration = effect.getDuration();
        s2.infinite = effect.isInfinite();
    }

    private List<StatusEffect> buildRenderOrder(Collection<StatusEffectInstance> effects, Set<StatusEffect> active) {
        ArrayList<StatusEffect> order = new ArrayList<StatusEffect>();
        this.renderOrderSeen.clear();
        for (StatusEffectInstance effect : effects) {
            StatusEffect type = (StatusEffect)effect.getEffectType().value();
            if (!this.renderOrderSeen.add(type)) continue;
            order.add(type);
        }
        for (StatusEffect type : this.animations.keySet()) {
            if (active.contains(type)) continue;
            order.add(type);
        }
        return order;
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        this.DefaultStyle(eventRender);
        super.onRender(eventRender);
    }

    public void DefaultStyle(EventRender.Default eventRender) {
        boolean bl = false;
        float baseX = this.draggable.getX();
        float y2 = this.draggable.getY();
        int colorTheme = this.getStableThemeColor();
        float targetWidth = 64.0f;
        int visibleCount = 0;
        Collection<StatusEffectInstance> effects = mc != null && Potions.mc.player != null ? Potions.mc.player.getStatusEffects() : List.of();
        HashSet<StatusEffect> active = new HashSet<StatusEffect>();
        for (StatusEffectInstance var_1293_2 : effects) {
            StatusEffect type = (StatusEffect)var_1293_2.getEffectType().value();
            active.add(type);
            this.getAnimation(type).update(1.0f);
            this.updateSnapshot(var_1293_2);
            int duration = var_1293_2.getDuration();
            Integer prevMax = this.maxDurations.get(type);
            if (prevMax != null && duration <= prevMax) continue;
            this.maxDurations.put(type, duration);
        }
        for (Map.Entry entry : this.animations.entrySet()) {
            if (active.contains(entry.getKey())) continue;
            ((AnimationUtils)entry.getValue()).update(0.0f);
        }
        List<StatusEffect> renderOrder = this.buildRenderOrder(effects, active);
        boolean bl2 = false;
        for (StatusEffect type : renderOrder) {
            float animValue = this.getAnimation(type).getValue();
            PotionSnapshot snapshot = this.snapshots.get(type);
            if (!(animValue > 0.01f) || snapshot == null) continue;
            bl = true;
            break;
        }
        boolean isChatOpen = mc != null && Potions.mc.currentScreen instanceof ChatScreen;
        boolean shouldShowPanel = bl || isChatOpen;
        this.panelAlphaAnimation.update(shouldShowPanel ? 1.0f : 0.0f);
        float panelProgress = this.panelAlphaAnimation.getValue();
        if (panelProgress <= 0.01f) {
            this.draggable.setWidth(0.0f);
            this.draggable.setHeight(0.0f);
            return;
        }
        for (StatusEffect type : renderOrder) {
            float timeBoxWidth;
            float rowWidth;
            float animValue = this.getAnimation(type).getValue();
            PotionSnapshot snapshot = this.snapshots.get(type);
            if (!(animValue > 0.01f) || snapshot == null) continue;
            ++visibleCount;
            String baseName = snapshot.baseName;
            String levelSuffix = Potions.getLevelSuffix(snapshot.amplifier);
            String time = Potions.formatDuration(snapshot.duration, snapshot.infinite);
            float textWidth = this.issue(12).getWidth(baseName);
            if (!levelSuffix.isEmpty()) {
                textWidth += this.issue(12).getWidth(" " + levelSuffix);
            }
            if (!((rowWidth = textWidth + (timeBoxWidth = Math.max(this.issue(10).getStringWidth(time) + 4.0f, 9.0f)) + 32.0f) > targetWidth)) continue;
            targetWidth = rowWidth;
        }
        float targetHeight = 22.2f + (float)visibleCount * 10.0f + 0.8f;
        this.widthAnimation.update(targetWidth);
        this.heightAnimation.update(targetHeight);
        float width = this.widthAnimation.getValue() + 0.0f;
        float height = this.heightAnimation.getValue();
        float rightEdge = baseX + width;
        float x2 = baseX;
        MatrixStack matrices0 = eventRender.getContext().getMatrices();
        float pivotX = x2 + width / 2.0f;
        float pivotY = y2 + height / 2.0f;
        float eased = HudFx.pushTransform(matrices0, panelProgress, pivotX, pivotY);
        int panelAlphaMul = (int)(255.0f * eased);
        if (this.isFlatStyle()) {
            int bgColor = ColorUtils.rgba(20, 20, 20, 255);
            RenderUtils.drawRoundedRect(eventRender.getContext().getMatrices(), x2, y2, width, height, 6.0f, bgColor);
        } else {
            int shadowColor = ColorUtils.rgba(0, 0, 0, (int)(200.0f * eased));
            RenderUtils.drawShadow(eventRender.getContext().getMatrices(), x2 - 2.0f, y2 - 2.0f, width + 4.0f, height + 4.0f, 6.0f, shadowColor);
            float blueLineWidth = width * 0.4f - 5.0f;
            float blueLineX = x2 + (width - blueLineWidth) / 2.0f + 13.0f;
            int themeLineColor = ColorUtils.setAlphaColor(colorTheme, panelAlphaMul);
            RenderUtils.drawRoundedRect(eventRender.getContext().getMatrices(), blueLineX, y2 - 1.5f, blueLineWidth, 3.5f, 1.0f, themeLineColor);
        }
        float headerIconDrawX = rightEdge - 12.0f;
        float headerIconDrawY = y2 + 7.0f;
        float headerTextY = y2 + (22.2f - this.issue(14).getHeight()) / 2.0f;
        float headerIconY = y2 + (22.2f - (float)this.myfont(15).getFontHeight()) / 2.0f;
        this.issue(14).draw(eventRender.getContext().getMatrices(), "Potions", x2 + 5.2f, headerTextY, panelAlphaMul == 255 ? -1 : ColorUtils.rgba(255, 255, 255, panelAlphaMul));
        if (this.isFlatStyle()) {
            float headerIconBgSize = 11.0f;
            float headerIconBgX = rightEdge - 14.0f;
            float headerIconBgY = y2 + (22.2f - headerIconBgSize) / 2.0f;
            int headerIconBgColor = ColorUtils.setAlphaColor(colorTheme, 63);
            RenderUtils.drawRoundedRect(eventRender.getContext().getMatrices(), headerIconBgX, headerIconBgY, headerIconBgSize, headerIconBgSize, 2.0f, headerIconBgColor);
        }
        this.myfont(15).drawString("e", rightEdge - 12.0f, headerIconY, ColorUtils.setAlphaColor(colorTheme, panelAlphaMul));
        float offsetY = 22.2f;
        int effectIndex = 0;
        for (StatusEffect type : renderOrder) {
            float animValue = this.getAnimation(type).getValue();
            PotionSnapshot snapshot = this.snapshots.get(type);
            if (animValue <= 0.01f || snapshot == null) {
                ++effectIndex;
                continue;
            }
            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(x2, y2, width, height);
            int alpha = (int)(255.0f * animValue * eased);
            float iconSize = 8.0f;
            float rowBgY = y2 + offsetY - 6.0f;
            float rowBgHeight = 16.8f;
            float iconX = x2 + 4.0f;
            float iconY = rowBgY + (rowBgHeight - iconSize) / 2.0f;
            String baseName = snapshot.baseName;
            String levelSuffix = Potions.getLevelSuffix(snapshot.amplifier);
            float textX = iconX + iconSize + 3.0f;
            float rowTextY = rowBgY + (rowBgHeight - this.issue(12).getHeight()) / 2.0f;
            float nameEndX = textX + this.issue(12).getWidth(baseName + (String)(levelSuffix.isEmpty() ? "" : " " + levelSuffix));
            String time = Potions.formatDuration(snapshot.duration, snapshot.infinite);
            float timeBoxWidth = Math.max(this.issue(10).getStringWidth(time) + 4.0f, 9.0f);
            float ringSize = 6.0f;
            float ringGap = 3.0f;
            float ringX = nameEndX + 4.0f;
            float timeBoxX = ringX + ringGap + ringSize;
            float rowEndX = timeBoxX + timeBoxWidth + 4.0f;
            float rowContentWidth = rowEndX - x2;
            int contentBgColor = ColorUtils.rgba(20, 20, 20, (int)(100.0f * eased));
            if (!this.isFlatStyle()) {
                RenderUtils.drawBlur(eventRender.getContext().getMatrices(), x2, rowBgY, rowContentWidth, rowBgHeight, 6.0f, 5.0f, ColorUtils.rgba(255, 255, 255, panelAlphaMul));
                RenderUtils.drawBlur(eventRender.getContext().getMatrices(), x2, rowBgY, rowContentWidth, rowBgHeight, 6.0f, 5.0f, ColorUtils.rgba(0, 0, 0, (int)(180.0f * eased)));
            }
            RenderUtils.drawRoundedRect(eventRender.getContext().getMatrices(), x2, rowBgY, rowContentWidth, rowBgHeight, 6.0f, contentBgColor);
            if (snapshot.entry != null) {
                Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(snapshot.entry);
                RenderUtils.drawSprite(eventRender.getContext().getMatrices(), sprite, iconX, iconY, (int)iconSize, ColorUtils.rgba(255, 255, 255, alpha));
            }
            this.issue(12).draw(eventRender.getContext().getMatrices(), baseName, textX, rowTextY, ColorUtils.rgba(255, 255, 255, alpha));
            if (!levelSuffix.isEmpty()) {
                float baseWidth = this.issue(12).getWidth(baseName);
                this.issue(12).draw(eventRender.getContext().getMatrices(), " " + levelSuffix, textX + baseWidth, rowTextY, ColorUtils.rgba(255, 255, 255, alpha));
            }
            float boxH = 9.0f;
            float boxY = rowBgY + (rowBgHeight - boxH) / 2.0f;
            float blurStartX = ringX - 3.0f;
            float blurWidth = timeBoxX + timeBoxWidth - blurStartX + 1.0f;
            if (!this.isFlatStyle()) {
                RenderUtils.drawBlur(eventRender.getContext().getMatrices(), blurStartX, boxY, blurWidth, boxH, 1.5f, 5.0f, ColorUtils.rgba(255, 255, 255, 255));
                RenderUtils.drawBlur(eventRender.getContext().getMatrices(), blurStartX, boxY, blurWidth, boxH, 1.5f, 5.0f, ColorUtils.rgba(0, 0, 0, 180));
            } else {
                int timerBgColor = ColorUtils.rgba(20, 20, 20, (int)(100.0f * eased));
                RenderUtils.drawRoundedRect(eventRender.getContext().getMatrices(), blurStartX, boxY, blurWidth, boxH, 2.0f, timerBgColor);
            }
            float rowTimeY = rowBgY + (rowBgHeight - this.issue(12).getHeight()) / 2.0f;
            this.issue(12).drawCenteredString(eventRender.getContext().getMatrices(), time, timeBoxX + timeBoxWidth / 2.0f, rowTimeY, ColorUtils.rgba(255, 255, 255, alpha));
            float progress = 1.0f;
            if (!snapshot.infinite) {
                int currentDuration = snapshot.duration;
                int maxDuration = this.maxDurations.getOrDefault(type, currentDuration);
                progress = maxDuration > 0 ? MathHelper.clamp((float)((float)currentDuration / (float)maxDuration), (float)0.0f, (float)1.0f) : 0.0f;
            }
            int grayColor = ColorUtils.rgba(55, 55, 55, alpha);
            int ringColor = ColorUtils.setAlphaColor(colorTheme, alpha);
            float thickness = 1.75f;
            float ringY = y2 + offsetY - 0.7f;
            RenderUtils.drawRingArc(eventRender.getContext().getMatrices(), ringX, ringY, ringSize, thickness, -90.0f, 270.0f, grayColor);
            if (progress > 0.0f) {
                float endAngle = -90.0f + 360.0f * progress;
                RenderUtils.drawRingArc(eventRender.getContext().getMatrices(), ringX, ringY, ringSize, thickness, -90.0f, endAngle, ringColor);
            }
            offsetY += 10.0f * animValue;
            ++effectIndex;
            ScissorUtils.pop();
            ScissorUtils.unset();
        }
        HudFx.popTransform(matrices0);
        this.draggable.setWidth(width);
        this.draggable.setHeight(height);
    }

    private int getStableThemeColor() {
        if (!polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor();
    }

    private static final class PotionSnapshot {
        RegistryEntry<StatusEffect> entry;
        String baseName;
        int amplifier;
        int duration;
        boolean infinite;

        private PotionSnapshot() {
        }
    }
}

