package polar.ru.client.modules.impl.render.base.implement;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UseCooldownComponent;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
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
import polar.ru.client.modules.impl.misc.ServerHelper;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;
import polar.ru.client.modules.impl.render.base.implement.HudFx;
import polar.ru.mixin.ItemCooldownManagerAccessor;
import polar.ru.mixin.ItemCooldownManagerEntryAccessor;
import polar.ru.polar;

public class Cooldowns
extends InterfaceProcessing {
    private static final float BASE_MIN_WIDTH = 76.0f;
    private static final float EXTRA_WIDTH = 0.0f;
    private static final float ROW_RIGHT_MARGIN = 25.0f;
    private static final float ROW_HEIGHT = 10.0f;
    private static final float HEADER_HEIGHT = 16.0f;
    private static final float HEADER_GAP = 0.2f;
    private static final float CONTENT_PAD_TOP = 6.0f;
    private static final float CONTENT_PAD_BOTTOM = 0.8f;
    private static final long CHAT_HINT_TTL_MS = 120000L;
    private final Map<Identifier, AnimationUtils> animations = new HashMap<Identifier, AnimationUtils>();
    private final Map<Identifier, CooldownSnapshot> snapshots = new HashMap<Identifier, CooldownSnapshot>();
    private final AnimationUtils widthAnimation = new AnimationUtils(70.0f, 10.5f, Easings.QUAD_OUT);
    private final AnimationUtils heightAnimation = new AnimationUtils(16.0f, 10.5f, Easings.QUAD_OUT);
    private final AnimationUtils panelAlphaAnimation = HudFx.newAppearAnimation();
    private final Map<Identifier, Float> maxDurations = new HashMap<Identifier, Float>();
    private static final Map<Identifier, ChatHint> chatHints = new ConcurrentHashMap<Identifier, ChatHint>();

    public Cooldowns(Draggable draggable) {
        super(draggable);
    }

    private Font issue(int size) {
        return Fonts.getFont("suisse", size);
    }

    private Font icon(int size) {
        return Fonts.getFont("icon", size);
    }

    private MCFontRenderer divine_icons(int size) {
        return Fonts.getTtfFont("divine_icons.ttf", size);
    }

    private AnimationUtils getAnimation(Identifier group) {
        return this.animations.computeIfAbsent(group, key -> new AnimationUtils(0.0f, 10.5f, Easings.QUAD_OUT));
    }

    public static void onGameMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        Cooldowns.putChatHint(lower, now, "использовал взрыв штучку", Registries.ITEM.getId(Items.FIRE_CHARGE), "Взрыв штучка");
        Cooldowns.putChatHint(lower, now, "использовал гул", Registries.ITEM.getId(Items.FIREWORK_STAR), "Гул");
        Cooldowns.putChatHint(lower, now, "использовал анти полет", Registries.ITEM.getId(Items.FIREWORK_STAR), "Анти Полет");
        Cooldowns.putChatHint(lower, now, "использовал стан", Registries.ITEM.getId(Items.NETHER_STAR), "Стан");
        Cooldowns.putChatHint(lower, now, "использовал взрыв трап", Registries.ITEM.getId(Items.PRISMARINE_SHARD), "Взрыв трап");
        Cooldowns.putChatHint(lower, now, "использовал снег заморозки", Registries.ITEM.getId(Items.SNOWBALL), "Снег заморозки");
        Cooldowns.putChatHint(lower, now, "использовал снег", Registries.ITEM.getId(Items.SNOWBALL), "Снег");
        Cooldowns.putChatHint(lower, now, "использовал трапку", Registries.ITEM.getId(Items.POPPED_CHORUS_FRUIT), "Трапка");
        Cooldowns.putChatHint(lower, now, "использовал ловушку", Registries.ITEM.getId(Items.HEART_OF_THE_SEA), "Ловушка");
        Cooldowns.putChatHint(lower, now, "использовал уник. трапка", Registries.ITEM.getId(Items.CRYING_OBSIDIAN), "Уник. трапка");
        Cooldowns.putChatHint(lower, now, "использовал деф лива", Registries.ITEM.getId(Items.MAGMA_CREAM), "Деф лива");
        Cooldowns.putChatHint(lower, now, "использовал лива с платформой", Registries.ITEM.getId(Items.CLAY_BALL), "Лива с платформой");
        Cooldowns.putChatHint(lower, now, "использовал дезориентацию", Registries.ITEM.getId(Items.ENDER_EYE), "Дезориентация");
        Cooldowns.putChatHint(lower, now, "использовал пласт", Registries.ITEM.getId(Items.DRIED_KELP), "Пласт");
        Cooldowns.putChatHint(lower, now, "использовал явную пыль", Registries.ITEM.getId(Items.SUGAR), "Явная пыль");
        Cooldowns.putChatHint(lower, now, "использовал божью ауру", Registries.ITEM.getId(Items.PHANTOM_MEMBRANE), "Божья аура");
    }

    private static void putChatHint(String lowerMessage, long now, String needle, Identifier group, String displayName) {
        if (lowerMessage.contains(needle)) {
            chatHints.put(group, new ChatHint(displayName, now));
        }
    }

    private LinkedHashMap<Identifier, CooldownSnapshot> collectCooldowns() {
        LinkedHashMap<Identifier, CooldownSnapshot> result = new LinkedHashMap<Identifier, CooldownSnapshot>();
        if (mc == null || Cooldowns.mc.player == null) {
            return result;
        }
        ItemCooldownManager manager = Cooldowns.mc.player.getItemCooldownManager();
        if (!(manager instanceof ItemCooldownManagerAccessor)) {
            return result;
        }
        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor)manager;
        Map<Identifier, Object> entries = accessor.polar$getEntries();
        if (entries == null || entries.isEmpty()) {
            return result;
        }
        for (Map.Entry<Identifier, Object> cooldownEntry : entries.entrySet()) {
            ItemCooldownManagerEntryAccessor entryAccessor;
            float remainingSeconds;
            Identifier group = cooldownEntry.getKey();
            Object entry = cooldownEntry.getValue();
            if (!(entry instanceof ItemCooldownManagerEntryAccessor) || (remainingSeconds = this.getRemainingSeconds(accessor, entryAccessor = (ItemCooldownManagerEntryAccessor)entry)) <= 0.01f) continue;
            ItemStack stack = this.findStackForGroup(group);
            CooldownSnapshot snapshot = this.snapshots.get(group);
            if (stack != null) {
                snapshot = new CooldownSnapshot(stack.copy(), this.resolveDisplayName(stack.getItem(), stack));
            } else if (snapshot == null && (snapshot = this.createSnapshotForGroup(group)) == null) continue;
            this.snapshots.put(group, snapshot);
            result.put(group, snapshot);
        }
        return result;
    }

    private ItemStack findStackForGroup(Identifier group) {
        if (mc == null || Cooldowns.mc.player == null) {
            return null;
        }
        ArrayList<ItemStack> stacks = new ArrayList<ItemStack>();
        stacks.addAll(Cooldowns.mc.player.getInventory().main);
        stacks.addAll(Cooldowns.mc.player.getInventory().armor);
        stacks.addAll(Cooldowns.mc.player.getInventory().offHand);
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty() || !group.equals((Object)this.getCooldownGroup(stack))) continue;
            return stack;
        }
        return null;
    }

    private CooldownSnapshot createSnapshotForGroup(Identifier group) {
        Item item = (Item)Registries.ITEM.get(group);
        if (item == null) {
            return null;
        }
        ItemStack stack = item.getDefaultStack();
        if (stack.isEmpty()) {
            return null;
        }
        return new CooldownSnapshot(stack, this.resolveDisplayName(item, stack));
    }

    private String resolveDisplayName(Item item, ItemStack stack) {
        String serverHelperName = this.resolveServerHelperName(item);
        if (serverHelperName != null) {
            return serverHelperName;
        }
        String chatHint = this.resolveChatHint(item);
        if (chatHint != null) {
            return chatHint;
        }
        return stack.getName().getString();
    }

    private String resolveServerHelperName(Item item) {
        if (ServerHelper.INSTANCE == null) {
            return null;
        }
        return ServerHelper.INSTANCE.resolveHelperBindName(item);
    }

    private String resolveChatHint(Item item) {
        Identifier group = Registries.ITEM.getId(item);
        ChatHint hint = chatHints.get(group);
        if (hint == null) {
            return null;
        }
        if (System.currentTimeMillis() - hint.timestamp > 120000L) {
            chatHints.remove(group);
            return null;
        }
        return hint.displayName;
    }

    private Identifier getCooldownGroup(ItemStack stack) {
        UseCooldownComponent useCooldown = (UseCooldownComponent)stack.get(DataComponentTypes.USE_COOLDOWN);
        if (useCooldown != null) {
            return useCooldown.cooldownGroup().orElse(Registries.ITEM.getId(stack.getItem()));
        }
        return Registries.ITEM.getId(stack.getItem());
    }

    private float getRemainingSeconds(ItemCooldownManagerAccessor accessor, ItemCooldownManagerEntryAccessor entryAccessor) {
        int currentTick = accessor.polar$getTick();
        int remainingTicks = entryAccessor.polar$getEndTick() - currentTick;
        return Math.max(0.0f, (float)remainingTicks / 20.0f);
    }

    private float getRemainingSeconds(Identifier group) {
        if (mc == null || Cooldowns.mc.player == null) {
            return 0.0f;
        }
        ItemCooldownManager manager = Cooldowns.mc.player.getItemCooldownManager();
        if (!(manager instanceof ItemCooldownManagerAccessor)) {
            return 0.0f;
        }
        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor)manager;
        Map<Identifier, Object> entries = accessor.polar$getEntries();
        if (entries == null) {
            return 0.0f;
        }
        Object entry = entries.get(group);
        if (!(entry instanceof ItemCooldownManagerEntryAccessor)) {
            return 0.0f;
        }
        ItemCooldownManagerEntryAccessor entryAccessor = (ItemCooldownManagerEntryAccessor)entry;
        return this.getRemainingSeconds(accessor, entryAccessor);
    }

    private void drawItemIcon(DrawContext context, ItemStack stack, float x2, float y2, float scale) {
        MatrixStack matrices = context.getMatrices();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        matrices.push();
        matrices.translate(x2, y2, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        context.drawItem(stack, 0, 0);
        matrices.pop();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableDepthTest();
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        this.DefaultStyle(eventRender);
        super.onRender(eventRender);
    }

    public void DefaultStyle(EventRender.Default eventRender) {
        boolean bl = false;
        float x2 = this.draggable.getX();
        float y2 = this.draggable.getY();
        int colorTheme = !polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow") ? polar.INSTANCE.themeStorage.getThemes().getTheme().color[0] : ColorUtils.getThemeColor();
        LinkedHashMap<Identifier, CooldownSnapshot> activeCooldowns = this.collectCooldowns();
        for (Identifier var_2960_2 : activeCooldowns.keySet()) {
            this.getAnimation(var_2960_2).update(1.0f);
        }
        for (Map.Entry entry2 : this.animations.entrySet()) {
            if (activeCooldowns.containsKey(entry2.getKey())) continue;
            ((AnimationUtils)entry2.getValue()).update(0.0f);
        }
        ArrayList<Identifier> renderOrder = new ArrayList<Identifier>(activeCooldowns.keySet());
        for (Identifier var_2960_3 : this.animations.keySet()) {
            if (activeCooldowns.containsKey(var_2960_3)) continue;
            renderOrder.add(var_2960_3);
        }
        boolean bl2 = false;
        for (Identifier group4 : renderOrder) {
            float animValue;
            CooldownSnapshot snapshot = this.snapshots.get(group4);
            if (snapshot == null || !((animValue = this.getAnimation(group4).getValue()) > 0.01f)) continue;
            bl = true;
            break;
        }
        boolean bl3 = mc != null && Cooldowns.mc.currentScreen instanceof ChatScreen;
        boolean shouldShowPanel = bl || bl3;
        this.panelAlphaAnimation.update(shouldShowPanel ? 1.0f : 0.0f);
        float panelProgress = this.panelAlphaAnimation.getValue();
        if (panelProgress <= 0.01f) {
            this.draggable.setWidth(0.0f);
            this.draggable.setHeight(0.0f);
            return;
        }
        int visibleCount = 0;
        float targetWidth = 76.0f;
        float targetHeight = 23.0f;
        for (Identifier group5 : renderOrder) {
            float timeBoxWidth;
            float ringGap;
            float ringSize;
            float animValue;
            CooldownSnapshot snapshot = this.snapshots.get(group5);
            if (snapshot == null || (animValue = this.getAnimation(group5).getValue()) <= 0.01f) continue;
            float remainingSeconds = this.getRemainingSeconds(group5);
            String timeText = snapshot.getTimeText(remainingSeconds);
            float iconSize = 8.0f;
            float nameWidth = iconSize + 3.0f + this.issue(12).getWidth(snapshot.displayName);
            float rowWidth = nameWidth + (ringSize = 6.0f) + (ringGap = 3.0f) + (timeBoxWidth = Math.max(this.issue(10).getStringWidth(timeText) + 4.0f, 9.0f)) + 25.0f + 5.0f;
            if (rowWidth > targetWidth) {
                targetWidth = rowWidth;
            }
            targetHeight += 10.0f * animValue;
            ++visibleCount;
        }
        this.widthAnimation.update(targetWidth);
        this.heightAnimation.update(targetHeight);
        float width = this.widthAnimation.getValue() + 0.0f;
        float height = this.heightAnimation.getValue();
        float rightEdge = x2 + width;
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
            int bgColor = ColorUtils.rgba(20, 20, 20, (int)(100.0f * eased));
            RenderUtils.drawBlur(eventRender.getContext().getMatrices(), x2, y2, width, height, 6.0f, 5.0f, ColorUtils.rgba(255, 255, 255, panelAlphaMul));
            RenderUtils.drawBlur(eventRender.getContext().getMatrices(), x2, y2, width, height, 6.0f, 5.0f, ColorUtils.rgba(0, 0, 0, (int)(180.0f * eased)));
            RenderUtils.drawRoundedRect(eventRender.getContext().getMatrices(), x2, y2, width, height, 6.0f, bgColor);
            float blueLineWidth = width * 0.4f - 5.0f;
            float blueLineX = x2 + (width - blueLineWidth) / 2.0f + 13.0f;
            int themeLineColor = ColorUtils.setAlphaColor(colorTheme, panelAlphaMul);
            RenderUtils.drawRoundedRect(eventRender.getContext().getMatrices(), blueLineX, y2 - 1.5f, blueLineWidth, 3.5f, 1.0f, themeLineColor);
        }
        float headerIconDrawX = rightEdge - 12.0f;
        float headerIconDrawY = y2 + 7.0f;
        float headerTextY = y2 + (22.2f - this.issue(14).getHeight()) / 2.0f;
        float headerIconY = y2 + (22.2f - (float)this.divine_icons(15).getFontHeight()) / 2.0f;
        this.issue(14).draw(eventRender.getContext().getMatrices(), "Cooldowns", x2 + 5.2f, headerTextY, ColorUtils.rgba(255, 255, 255, panelAlphaMul));
        if (this.isFlatStyle()) {
            float headerIconBgSize = 11.0f;
            float headerIconBgX = rightEdge - 14.0f;
            float headerIconBgY = y2 + (22.2f - headerIconBgSize) / 2.0f;
            int headerIconBgColor = ColorUtils.setAlphaColor(colorTheme, 63);
            RenderUtils.drawRoundedRect(eventRender.getContext().getMatrices(), headerIconBgX, headerIconBgY, headerIconBgSize, headerIconBgSize, 2.0f, headerIconBgColor);
        }
        this.divine_icons(15).drawString("e", rightEdge - 12.0f, headerIconY, ColorUtils.setAlphaColor(colorTheme, panelAlphaMul));
        float offsetY = 22.2f;
        for (Identifier group6 : renderOrder) {
            AnimationUtils animation;
            float animValue;
            CooldownSnapshot snapshot = this.snapshots.get(group6);
            if (snapshot == null || (animValue = (animation = this.getAnimation(group6)).getValue()) <= 0.01f) continue;
            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(x2, y2, width, height);
            int alpha = (int)(255.0f * animValue * eased);
            float iconSize = 8.0f;
            float rowBgY = y2 + offsetY - 6.0f;
            float rowBgHeight = 16.8f;
            float iconX = x2 + 5.0f;
            float iconY = rowBgY + (rowBgHeight - iconSize) / 2.0f;
            String displayName = snapshot.displayName;
            float textX = iconX + iconSize + 3.0f;
            float rowTextY = rowBgY + (rowBgHeight - this.issue(12).getHeight()) / 2.0f;
            float nameEndX = textX + this.issue(12).getWidth(displayName);
            float remainingSeconds = this.getRemainingSeconds(group6);
            String timeText = snapshot.getTimeText(remainingSeconds);
            float timeBoxWidth = Math.max(this.issue(10).getStringWidth(timeText) + 4.0f, 9.0f);
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
            this.drawItemIcon(eventRender.getContext(), snapshot.stack, iconX, iconY, 0.5f);
            this.issue(12).draw(eventRender.getContext().getMatrices(), displayName, textX, rowTextY, ColorUtils.rgba(255, 255, 255, alpha));
            float boxH = 9.0f;
            float boxY = rowBgY + (rowBgHeight - boxH) / 2.0f;
            if (!this.isFlatStyle()) {
                RenderUtils.drawBlur(eventRender.getContext().getMatrices(), ringX - 2.0f, boxY, timeBoxX + timeBoxWidth - (ringX - 2.0f), boxH, 1.5f, 5.0f, ColorUtils.rgba(255, 255, 255, 255));
                RenderUtils.drawBlur(eventRender.getContext().getMatrices(), ringX - 2.0f, boxY, timeBoxX + timeBoxWidth - (ringX - 2.0f), boxH, 1.5f, 5.0f, ColorUtils.rgba(0, 0, 0, 180));
            }
            float rowTimeY = rowBgY + (rowBgHeight - this.issue(12).getHeight()) / 2.0f;
            this.issue(12).drawCenteredString(eventRender.getContext().getMatrices(), timeText, timeBoxX + timeBoxWidth / 2.0f, rowTimeY, ColorUtils.rgba(255, 255, 255, alpha));
            float progress = 1.0f;
            Float maxDuration = this.maxDurations.get(group6);
            if (maxDuration != null && maxDuration.floatValue() > 0.0f) {
                progress = MathHelper.clamp((float)(remainingSeconds / maxDuration.floatValue()), (float)0.0f, (float)1.0f);
            }
            if (maxDuration == null || remainingSeconds > maxDuration.floatValue()) {
                this.maxDurations.put(group6, Float.valueOf(remainingSeconds));
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
            ScissorUtils.pop();
            ScissorUtils.unset();
        }
        this.animations.entrySet().removeIf(entry -> !activeCooldowns.containsKey(entry.getKey()) && ((AnimationUtils)entry.getValue()).getValue() <= 0.01f);
        this.snapshots.keySet().removeIf(group -> !this.animations.containsKey(group));
        HudFx.popTransform(matrices0);
        this.draggable.setWidth(width);
        this.draggable.setHeight(height);
    }

    private record ChatHint(String displayName, long timestamp) {
    }

    private static final class CooldownSnapshot {
        private final ItemStack stack;
        private final String displayName;

        private CooldownSnapshot(ItemStack stack, String displayName) {
            this.stack = stack;
            this.displayName = displayName;
        }

        private String getTimeText(float remainingSeconds) {
            float seconds = Math.max(0.0f, remainingSeconds);
            if (seconds >= 10.0f) {
                return String.format(Locale.ROOT, "%.0fs", Float.valueOf(seconds));
            }
            return String.format(Locale.ROOT, "%.1fs", Float.valueOf(seconds));
        }
    }
}

