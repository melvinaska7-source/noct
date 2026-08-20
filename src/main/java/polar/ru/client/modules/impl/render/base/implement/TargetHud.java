package polar.ru.client.modules.impl.render.base.implement;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.misc.NameProtect;
import polar.ru.client.modules.impl.misc.ScoreboardHP;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;
import polar.ru.polar;

public class TargetHud
extends InterfaceProcessing {
    private final AnimationUtils alphaAnimation = new AnimationUtils(0.0f, 9.0f, Easings.QUAD_OUT);
    private final AnimationUtils hpAnimation = new AnimationUtils(1.0f, 9.2f, Easings.QUAD_OUT);
    private final AnimationUtils hpTrailAnimation = new AnimationUtils(1.0f, 7.4f, Easings.QUAD_OUT);
    private final AnimationUtils hpValueAnimation = new AnimationUtils(20.0f, 7.0f, Easings.QUAD_OUT);
    private final AnimationUtils abValueAnimation = new AnimationUtils(0.0f, 7.0f, Easings.QUAD_OUT);
    private final AnimationUtils goldenHpAnimation = new AnimationUtils(0.0f, 9.2f, Easings.QUAD_OUT);
    private final AnimationUtils goldenAlphaAnimation = new AnimationUtils(0.0f, 9.0f, Easings.QUAD_OUT);
    private final List<HeadParticle> headParticles = new ObjectArrayList();
    private LivingEntity lastTarget;
    private float maxAbsorption = 20.0f;
    private boolean headParticlesEnabled = true;
    private boolean healthBarStyleEnabled = false;
    private long lastParticleUpdateNs = System.nanoTime();
    private LivingEntity particleTarget;
    private int lastTargetHurtTime = 0;
    private int cachedBarThemeColor = ColorUtils.rgba(124, 91, 242, 255);
    private final ItemStack[] armorScratch = new ItemStack[4];

    public TargetHud(Draggable draggable) {
        super(draggable);
    }

    private Font issue(int size) {
        return Fonts.getFont("suisse", size);
    }

    public boolean isHeadParticlesEnabled() {
        return this.headParticlesEnabled;
    }

    public void setHeadParticlesEnabled(boolean headParticlesEnabled) {
        this.headParticlesEnabled = headParticlesEnabled;
        if (!headParticlesEnabled) {
            this.headParticles.clear();
        }
    }

    public boolean isHealthBarStyleEnabled() {
        return this.healthBarStyleEnabled;
    }

    public void setHealthBarStyleEnabled(boolean healthBarStyleEnabled) {
        this.healthBarStyleEnabled = healthBarStyleEnabled;
    }

    private void drawBlurPanel(MatrixStack matrices, float x2, float y2, float width, float height, float radius, int themeColor) {
        RenderUtils.drawBlur(matrices, x2, y2, width, height, radius, 5.0f, ColorUtils.rgba(255, 255, 255, 255));
        RenderUtils.drawBlur(matrices, x2, y2, width, height, radius, 5.0f, ColorUtils.rgba(0, 0, 0, 150));
        RenderUtils.drawHudSquarePattern(matrices, x2, y2, width, height, themeColor);
    }

    private int getDurabilityColor(float progress) {
        if (progress > 0.6f) {
            return ColorUtils.rgba(80, 220, 120, 255);
        }
        if (progress > 0.3f) {
            return ColorUtils.rgba(230, 190, 50, 255);
        }
        return ColorUtils.rgba(220, 70, 70, 255);
    }

    private void drawDurabilityBar(MatrixStack matrices, float x2, float y2, float width, float height, float progress, float alpha) {
        int bg = ColorUtils.rgba(30, 32, 42, (int)(160.0f * alpha));
        RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, 1.0f, bg);
        if (progress <= 0.01f) {
            return;
        }
        float fillW = Math.max(2.0f, width * progress);
        int color = this.getDurabilityColor(progress);
        int fillLeft = ColorUtils.applyAlpha(ColorUtils.darken(color, 0.45f), alpha);
        int fillRight = ColorUtils.applyAlpha(color, alpha);
        int glowColor = ColorUtils.applyAlpha(color, alpha * 0.28f);
        RenderUtils.drawRoundedRect(matrices, x2, y2 - 0.35f, fillW, height + 0.7f, 1.0f, glowColor);
        RenderUtils.drawGradientRect(matrices, x2, y2, fillW, height, 1.0f, fillLeft, fillRight, true);
    }

    private float getItemDurabilityProgress(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageable()) {
            return -1.0f;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return -1.0f;
        }
        return MathHelper.clamp((float)((float)(max - stack.getDamage()) / (float)max), (float)0.0f, (float)1.0f);
    }

    private void updateAndRenderHeadParticles(MatrixStack matrices, LivingEntity target, float headX, float headY, float headSize, float alpha, int themeColor) {
        if (target == null || alpha <= 0.02f) {
            this.headParticles.clear();
            this.particleTarget = target;
            this.lastTargetHurtTime = 0;
            return;
        }
        long now = System.nanoTime();
        float deltaTicks = MathHelper.clamp((float)((float)(now - this.lastParticleUpdateNs) / 1.0E9f * 60.0f), (float)0.2f, (float)3.0f);
        this.lastParticleUpdateNs = now;
        if (this.particleTarget != target) {
            this.headParticles.clear();
            this.particleTarget = target;
            this.lastTargetHurtTime = Math.max(0, target.hurtTime);
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float centerX = headX + headSize * 0.5f;
        float centerY = headY + headSize * 0.5f;
        int hurtTime = Math.max(0, target.hurtTime);
        boolean spawnBurst = hurtTime > 0 && (hurtTime > this.lastTargetHurtTime || hurtTime % 3 == 0);
        this.lastTargetHurtTime = hurtTime;
        if (spawnBurst) {
            int burstCount = 1 + random.nextInt(2);
            for (int n2 = 0; n2 < burstCount && this.headParticles.size() < 14; ++n2) {
                float angle = (float)(random.nextDouble() * Math.PI * 2.0);
                float radius = random.nextFloat() * headSize * 0.24f;
                float spreadAngle = (float)(random.nextDouble() * Math.PI * 2.0);
                float speed = 0.58f + random.nextFloat() * 0.9f;
                HeadParticle p2 = new HeadParticle();
                p2.x = centerX + MathHelper.cos((float)angle) * radius;
                p2.y = centerY + MathHelper.sin((float)angle) * radius;
                p2.vx = MathHelper.cos((float)spreadAngle) * speed + (p2.x - centerX) * 0.025f;
                p2.vy = MathHelper.sin((float)spreadAngle) * speed + (p2.y - centerY) * 0.025f;
                p2.size = 3.8f + random.nextFloat() * 1.4f;
                p2.age = 0.0f;
                p2.maxAge = 74.0f + random.nextFloat() * 42.0f;
                this.headParticles.add(p2);
            }
        }
        float velocityDrag = (float)Math.pow(0.975f, deltaTicks);
        for (int i2 = this.headParticles.size() - 1; i2 >= 0; --i2) {
            HeadParticle p3 = this.headParticles.get(i2);
            p3.age += deltaTicks;
            if (p3.age >= p3.maxAge) {
                this.headParticles.remove(i2);
                continue;
            }
            p3.x += p3.vx * deltaTicks;
            p3.y += p3.vy * deltaTicks;
            p3.vx *= velocityDrag;
            p3.vy *= velocityDrag;
            p3.vy += 0.0012f * deltaTicks;
            float life = 1.0f - p3.age / p3.maxAge;
            float smoothLife = life * life * (3.0f - 2.0f * life);
            float particleAlpha = alpha * smoothLife;
            if (particleAlpha <= 0.02f) continue;
            RenderUtils.drawRoundedRect(matrices, p3.x - p3.size * 0.5f, p3.y - p3.size * 0.5f, p3.size, p3.size, p3.size * 0.45f, ColorUtils.applyAlpha(themeColor, particleAlpha * 0.58f));
        }
    }

    private void drawTargetHudItem(EventRender.Default eventRender, MatrixStack matrices, ItemStack stack, float slotX, float slotY, float slotSize, float itemScale) {
        if (stack.isEmpty() || itemScale < 0.05f) {
            return;
        }
        float itemScreenSize = 16.0f * itemScale;
        float offsetX = slotX + (slotSize - itemScreenSize) / 2.0f;
        float offsetY = slotY + (slotSize - itemScreenSize) / 2.0f;
        matrices.push();
        matrices.translate(offsetX, offsetY, 0.0f);
        matrices.scale(itemScale, itemScale, 1.0f);
        eventRender.getContext().drawItem(stack, 0, 0);
        matrices.pop();
    }

    private String getDisplayName(LivingEntity target) {
        String patched;
        NameProtect np;
        String raw = target.getName().getString();
        if (raw == null || raw.isEmpty()) {
            raw = "Unknown";
        }
        if ((np = NameProtect.INSTANCE) != null && np.isEnable() && (patched = np.patch(raw)) != null) {
            return patched;
        }
        return raw;
    }

    private int getThemeColor() {
        if (!polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor();
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        this.DefaultStyle(eventRender);
        super.onRender(eventRender);
    }

    public void DefaultStyle(EventRender.Default eventRender) {
        float fillW;
        boolean hidingHud;
        if (TargetHud.mc.player == null) {
            this.headParticles.clear();
            this.lastTargetHurtTime = 0;
            this.draggable.setWidth(0.0f);
            this.draggable.setHeight(0.0f);
            return;
        }
        Aura aura = ModuleClass.aura;
        boolean chatOpen = TargetHud.mc.currentScreen instanceof ChatScreen;
        LivingEntity auraTarget = aura != null ? aura.getTarget() : null;
        boolean showTargetHud = chatOpen || auraTarget != null;
        this.alphaAnimation.setSpeed(showTargetHud ? 9.0f : 5.0f);
        this.alphaAnimation.update(showTargetHud ? 1.0f : 0.0f);
        float alpha = MathHelper.clamp((float)this.alphaAnimation.getValue(), (float)0.0f, (float)1.0f);
        if (showTargetHud) {
            this.lastTarget = chatOpen ? TargetHud.mc.player : auraTarget;
        }
        LivingEntity target = showTargetHud ? (chatOpen ? TargetHud.mc.player : auraTarget) : this.lastTarget;
        if (target == null || alpha <= 0.01f) {
            this.headParticles.clear();
            this.lastTargetHurtTime = 0;
            this.draggable.setWidth(0.0f);
            this.draggable.setHeight(0.0f);
            this.goldenAlphaAnimation.setValue(0.0f);
            this.abValueAnimation.setValue(0.0f);
            this.goldenHpAnimation.setValue(0.0f);
            return;
        }
        float currentAbsorption = target.getAbsorptionAmount();
        if (currentAbsorption > this.maxAbsorption) {
            this.maxAbsorption = currentAbsorption;
        }
        float maxHealth = Math.max(1.0f, target.getMaxHealth());
        float targetHealthForAnim = showTargetHud ? ScoreboardHP.getHealth(target) : 0.0f;
        this.hpValueAnimation.update(targetHealthForAnim);
        float animatedHealthValue = MathHelper.clamp((float)this.hpValueAnimation.getValue(), (float)0.0f, (float)maxHealth);
        float healthProgress = MathHelper.clamp((float)(targetHealthForAnim / maxHealth), (float)0.0f, (float)1.0f);
        this.hpAnimation.update(healthProgress);
        float hpProgressAnimated = MathHelper.clamp((float)this.hpAnimation.getValue(), (float)0.0f, (float)1.0f);
        if (hpProgressAnimated > this.hpTrailAnimation.getValue()) {
            this.hpTrailAnimation.setValue(MathHelper.lerp((float)0.78f, (float)this.hpTrailAnimation.getValue(), (float)hpProgressAnimated));
        } else {
            this.hpTrailAnimation.update(hpProgressAnimated);
        }
        float hpTrailProgressAnimated = MathHelper.clamp((float)this.hpTrailAnimation.getValue(), (float)0.0f, (float)1.0f);
        boolean bl = hidingHud = !showTargetHud;
        if (hidingHud) {
            hpTrailProgressAnimated = hpProgressAnimated;
        }
        int colorTheme = this.getThemeColor();
        if (showTargetHud) {
            this.cachedBarThemeColor = colorTheme;
        }
        int barColor = showTargetHud ? colorTheme : this.cachedBarThemeColor;
        String name = this.getDisplayName(target);
        String hpText = "HP: " + (int)animatedHealthValue;
        float x2 = this.draggable.getX();
        float y2 = this.draggable.getY();
        int drawAlphaInt = (int)(255.0f * alpha);
        String winLoseText = "";
        int winLoseColor = ColorUtils.rgba(255, 255, 255, drawAlphaInt);
        if (TargetHud.mc.player != null) {
            float targetHealth = targetHealthForAnim;
            float playerHealth = TargetHud.mc.player.getHealth();
            if (targetHealth <= playerHealth) {
                winLoseText = "WIN";
                winLoseColor = ColorUtils.rgba(50, 255, 50, drawAlphaInt);
            } else {
                winLoseText = "LOSE";
                winLoseColor = ColorUtils.rgba(255, 50, 50, drawAlphaInt);
            }
        }
        int armorCount = 0;
        for (ItemStack stack : target.getArmorItems()) {
            if (stack.isEmpty() || armorCount >= this.armorScratch.length) continue;
            this.armorScratch[armorCount++] = stack;
        }
        ItemStack mainHand = target.getMainHandStack();
        ItemStack offHand = target.getOffHandStack();
        ItemStack[] handStacks = new ItemStack[2];
        int handCount = 0;
        if (!offHand.isEmpty()) {
            handStacks[handCount++] = offHand;
        }
        if (!mainHand.isEmpty()) {
            handStacks[handCount++] = mainHand;
        }
        boolean hasArmor = armorCount > 0;
        boolean hasHands = handCount > 0;
        float slotSize = 10.0f;
        float slotGap = 1.5f;
        float slotPad = 1.5f;
        float itemsGap = 2.0f;
        float handSideGap = 3.0f;
        float blurRadius = 4.0f;
        float durBarH = 2.0f;
        float durGap = 1.0f;
        float slotBlockH = slotSize + durGap + durBarH;
        float armorContW = hasArmor ? slotPad * 2.0f + (float)armorCount * slotSize + (float)(armorCount - 1) * slotGap : 0.0f;
        float armorContH = hasArmor ? slotPad * 2.0f + slotBlockH : 0.0f;
        float handsContW = hasHands ? slotPad * 2.0f + slotSize : 0.0f;
        float handsContH = hasHands ? slotPad * 2.0f + (float)handCount * slotSize + (float)Math.max(0, handCount - 1) * slotGap : 0.0f;
        float armorRowH = hasArmor ? armorContH + itemsGap : 0.0f;
        float panelY = y2 + armorRowH;
        float headSize = 20.0f;
        float padding = 4.5f;
        float gap = 7.0f;
        float rightPad = 7.0f;
        float height = headSize + padding * 2.0f;
        float textW = Math.max(this.issue(14).getWidth(name), this.issue(12).getWidth(hpText));
        float barH = 4.0f;
        float width = Math.max(90.0f, padding + headSize + gap + textW + rightPad) + 12.0f;
        float headX = x2 + padding;
        float headY = panelY + padding;
        float textX = headX + headSize + gap - 2.0f;
        float nameY = headY + 1.5f;
        float hpTextY = nameY + 8.0f;
        float barY = headY + headSize - barH - 0.5f;
        float barW = width - (textX - x2) - rightPad;
        MatrixStack matrices = eventRender.getContext().getMatrices();
        matrices.push();
        float armorContX = x2 + (width - armorContW) * 0.5f;
        if (hasArmor) {
            this.drawBlurPanel(matrices, armorContX, y2, armorContW, armorContH, blurRadius, barColor);
        }
        if (this.glassSettings.enabled.isState()) {
            this.glassSettings.drawGlass(matrices, x2, panelY, width, height, barColor);
        } else if (this.isFlatStyle()) {
            int bgColor = ColorUtils.rgba(20, 20, 20, 255);
            RenderUtils.drawRoundedRect(matrices, x2, panelY, width, height, 6.0f, bgColor);
        } else {
            int shadowColor = ColorUtils.rgba(0, 0, 0, 200);
            RenderUtils.drawShadow(matrices, x2 - 2.0f, panelY - 2.0f, width + 4.0f, height + 4.0f, 6.0f, shadowColor);
            int bgColor = ColorUtils.rgba(20, 20, 20, 100);
            RenderUtils.drawBlur(matrices, x2, panelY, width, height, 6.0f, 5.0f, ColorUtils.rgba(255, 255, 255, 255));
            RenderUtils.drawBlur(matrices, x2, panelY, width, height, 6.0f, 5.0f, ColorUtils.rgba(0, 0, 0, 180));
            RenderUtils.drawRoundedRect(matrices, x2, panelY, width, height, 6.0f, bgColor);
            float blueLineWidth = width * 0.4f - 5.0f;
            float blueLineX = x2 + (width - blueLineWidth) / 2.0f + 13.0f;
            int themeLineColor = barColor;
            RenderUtils.drawRoundedRect(matrices, blueLineX, panelY - 1.5f, blueLineWidth, 3.5f, 1.0f, themeLineColor);
        }
        if (!this.isFlatStyle() && this.glassSettings.glowEnabled.isState()) {
            this.glassSettings.drawGlow(matrices, x2, panelY, width, height, barColor);
        }
        if (this.headParticlesEnabled) {
            this.updateAndRenderHeadParticles(matrices, target, headX, headY, headSize, alpha, barColor);
        } else {
            this.headParticles.clear();
        }
        float hurtPercent = 0.0f;
        if (target.hurtTime > 0) {
            hurtPercent = MathHelper.clamp((float)((float)target.hurtTime / 10.0f * 0.55f), (float)0.0f, (float)0.55f);
        }
        if (target instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity)target;
            RenderUtils.drawPlayerHead(matrices, playerEntity.getUuid(), headX, headY, headSize, 5.0f, alpha, hurtPercent);
        } else {
            RenderUtils.drawTargetHudDefaultPlaceholder(matrices, headX, headY, alpha);
        }
        this.issue(14).drawString(matrices, name, textX, nameY, ColorUtils.rgba(255, 255, 255, drawAlphaInt));
        if (!winLoseText.isEmpty()) {
            this.issue(12).drawString(matrices, winLoseText, textX, hpTextY, winLoseColor);
        }
        float hpTextW = this.issue(12).getWidth(hpText);
        float hpCornerX = x2 + width - hpTextW - rightPad;
        float hpCornerY = nameY;
        this.issue(12).drawString(matrices, hpText, hpCornerX, hpCornerY, ColorUtils.rgba(160, 165, 175, drawAlphaInt));
        this.goldenAlphaAnimation.setSpeed(currentAbsorption > 0.0f ? 9.0f : 5.0f);
        this.goldenAlphaAnimation.update(currentAbsorption > 0.0f ? 1.0f : 0.0f);
        float goldenAlpha = MathHelper.clamp((float)this.goldenAlphaAnimation.getValue(), (float)0.0f, (float)1.0f);
        if (goldenAlpha > 0.01f) {
            String abText = "+" + (int)currentAbsorption + "AB";
            float abTW = this.issue(12).getWidth(abText);
            float abX = textX + barW - abTW;
            int goldenAlphaInt = (int)(255.0f * goldenAlpha * alpha);
            this.issue(12).drawGradientStringHorizontal(matrices, abText, abX, hpTextY, ColorUtils.rgba(236, 183, 39, goldenAlphaInt), ColorUtils.rgba(200, 140, 20, goldenAlphaInt));
        }
        int barBg = ColorUtils.rgba(40, 42, 55, (int)(180.0f * alpha));
        RenderUtils.drawRoundedRect(matrices, textX, barY, barW, barH, 1.5f, barBg);
        float goldenReservedW = 0.0f;
        if (goldenAlpha > 0.01f && currentAbsorption > 0.0f) {
            this.abValueAnimation.update(showTargetHud ? currentAbsorption : 0.0f);
            float maxAB = Math.max(1.0f, this.maxAbsorption);
            this.goldenHpAnimation.update(MathHelper.clamp((float)(currentAbsorption / maxAB), (float)0.0f, (float)1.0f));
            float goldenFill = MathHelper.clamp((float)this.goldenHpAnimation.getValue(), (float)0.0f, (float)1.0f);
            goldenReservedW = barW * goldenFill;
            if (goldenReservedW > 1.0f) {
                float goldenX = textX + barW - goldenReservedW;
                int goldenL = ColorUtils.applyAlpha(ColorUtils.rgba(147, 108, 16, 255), goldenAlpha * alpha);
                int goldenR = ColorUtils.applyAlpha(ColorUtils.rgba(236, 183, 39, 255), goldenAlpha * alpha);
                RenderUtils.drawGradientRect(matrices, goldenX, barY, goldenReservedW, barH, 1.5f, goldenL, goldenR, true);
            }
        }
        float hpZoneW = barW - goldenReservedW;
        float trailW = hpZoneW * hpTrailProgressAnimated;
        if (!hidingHud && trailW > 1.0f) {
            int trailBase = this.healthBarStyleEnabled ? ColorUtils.rgba(180, 60, 30, 255) : barColor;
            int trailColorL = ColorUtils.applyAlpha(ColorUtils.darken(trailBase, 0.8f), alpha * 0.5f);
            int trailColorR = ColorUtils.applyAlpha(ColorUtils.darken(trailBase, 0.5f), alpha * 0.5f);
            RenderUtils.drawGradientRect(matrices, textX, barY, trailW, barH, 1.5f, trailColorL, trailColorR, true);
        }
        if ((fillW = hpZoneW * hpProgressAnimated) > 1.0f) {
            int fillColorR;
            int fillColorL;
            if (this.healthBarStyleEnabled) {
                int healthColor;
                float t;
                if (hpProgressAnimated > 0.5f) {
                    t = (hpProgressAnimated - 0.5f) / 0.5f;
                    healthColor = ColorUtils.rgba((int)(255.0f * (1.0f - t)), 200, 50, 255);
                } else {
                    t = hpProgressAnimated / 0.5f;
                    healthColor = ColorUtils.rgba(220, (int)(180.0f * t), 30, 255);
                }
                fillColorL = ColorUtils.applyAlpha(ColorUtils.darken(healthColor, 0.6f), alpha);
                fillColorR = ColorUtils.applyAlpha(healthColor, alpha);
            } else {
                fillColorL = ColorUtils.applyAlpha(ColorUtils.darken(barColor, 0.5f), alpha);
                fillColorR = ColorUtils.applyAlpha(barColor, alpha);
            }
            RenderUtils.drawGradientRect(matrices, textX, barY, fillW, barH, 1.5f, fillColorL, fillColorR, true);
        }
        float itemScale = 0.52f * alpha;
        if (hasHands) {
            float handsContX = x2 + width + handSideGap;
            float handsContY = panelY + (height - handsContH) * 0.5f;
            this.drawBlurPanel(matrices, handsContX, handsContY, handsContW, handsContH, blurRadius, barColor);
            for (int i2 = 0; i2 < handCount; ++i2) {
                float sx = handsContX + slotPad;
                float sy = handsContY + slotPad + (float)i2 * (slotSize + slotGap);
                this.drawTargetHudItem(eventRender, matrices, handStacks[i2], sx, sy, slotSize, itemScale);
            }
        }
        if (hasArmor) {
            for (int i3 = 0; i3 < armorCount; ++i3) {
                float sx = armorContX + slotPad + (float)i3 * (slotSize + slotGap);
                float sy = y2 + slotPad;
                ItemStack stack = this.armorScratch[i3];
                this.drawTargetHudItem(eventRender, matrices, stack, sx, sy, slotSize, itemScale);
                float durProgress = this.getItemDurabilityProgress(stack);
                if (durProgress >= 0.0f) {
                    this.drawDurabilityBar(matrices, sx, sy + slotSize + durGap, slotSize, durBarH, durProgress, alpha);
                }
                this.armorScratch[i3] = ItemStack.EMPTY;
            }
        }
        matrices.pop();
        float totalWidth = Math.max(width, armorContW) + (hasHands ? handSideGap + handsContW : 0.0f);
        this.draggable.setWidth(totalWidth);
        this.draggable.setHeight(armorRowH + height);
    }

    private static final class HeadParticle {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        float age;
        float maxAge;

        private HeadParticle() {
        }
    }
}

