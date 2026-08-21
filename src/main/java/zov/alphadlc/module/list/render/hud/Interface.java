package zov.alphadlc.module.list.render.hud;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import org.joml.Matrix4f;
import zov.alphadlc.AlphaDLC;
import zov.alphadlc.event.list.EventHUD;
import zov.alphadlc.event.list.EventPacket;
import zov.alphadlc.event.list.EventPopTotem;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.list.combat.KillAura;
import zov.alphadlc.module.list.misc.NameProtect;
import zov.alphadlc.module.list.player.ServerHelper;
import zov.alphadlc.module.list.render.NameTags;
import zov.alphadlc.module.settings.*;
import zov.alphadlc.util.base.Instance;
import zov.alphadlc.util.draggable.DragManager;
import zov.alphadlc.util.draggable.Draggable;
import zov.alphadlc.util.keyboard.KeyStorage;
import zov.alphadlc.util.math.Counter;
import zov.alphadlc.util.render.builders.Builder;
import zov.alphadlc.util.render.builders.states.QuadColorState;
import zov.alphadlc.util.render.builders.states.QuadRadiusState;
import zov.alphadlc.util.render.builders.states.SizeState;
import zov.alphadlc.util.render.math.Animation;
import zov.alphadlc.util.render.math.Easing;
import zov.alphadlc.util.render.helper.HoverUtil;
import zov.alphadlc.util.render.math.Scissor;
import zov.alphadlc.util.render.msdf.Fonts;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;
import zov.alphadlc.util.replace.ReplaceUtil;
import zov.alphadlc.util.server.Server;
import zov.alphadlc.util.staff.StaffManager;

import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ModuleInformation(moduleName = "Interface", moduleDesc = "Настройка элементов HUD на экране", moduleCategory = ModuleCategory.RENDER)
public class Interface extends Module {

    private static final Identifier TARGET_HUD_GLOW_TEXTURE = Identifier.of("mre", "images/glow.png");
    private final ModeListSetting elements = new ModeListSetting("Элементы",
            new BooleanSetting("Ватермарка", true),
            new BooleanSetting("Инфо", true),
            new BooleanSetting("Координаты", false),
            new BooleanSetting("Активный таргет", true),
//            new BooleanSetting("Таргет худ от темы", false),
            new BooleanSetting("Привязанные модули", true),
            new BooleanSetting("Активные модераторы", true),
            new BooleanSetting("Бафы", true),
            new BooleanSetting("КулДауны", true),
            new BooleanSetting("ServerHelper", true),
            new BooleanSetting("Нотификации", true),
            new BooleanSetting("Кастомный хотбар", false),
            new BooleanSetting("Броня", false),
            new BooleanSetting("Полоса тотемов", false),
            new BooleanSetting("Блюр фона", true)
    );

    private final SliderSetting backgroundIntensity =
            new SliderSetting("Интенсивность фона", 0.5f, 0.05f, 1.0f, 0.01f);
    private final SliderSetting headerIntensity =
            new SliderSetting("Интенсивность заголовков", 0.5f, 0.05f, 1.0f, 0.01f);
    private final SliderSetting itemIntensity =
            new SliderSetting("Интенсивность элементов", 0.2f, 0.05f, 1.0f, 0.01f);
    private final SliderSetting lowHpAlertThreshold =
            new SliderSetting("Порог ХП оповещения", 8f, 1f, 20f, 0.5f);

    private final Draggable watermarkDrag = DragManager.installDrag(this, "Watermark", 4, 4);
    private final Draggable infoDrag = DragManager.installDrag(this, "Info", 4, 24);
    private final Draggable keyBindsDrag = DragManager.installDrag(this, "HotKeys", 100, 50);
    private final Draggable staffListDrag = DragManager.installDrag(this, "StaffList", 200, 50);
    private final Draggable potionsDrag = DragManager.installDrag(this, "Potions", 300, 50);
    private final Draggable cooldownsDrag = DragManager.installDrag(this, "CoolDowns", 300, 130);
    private final Draggable serverHelperDrag = DragManager.installDrag(this, "ServerHelper", 300, 200);
    private final Draggable targetHUDDrag = DragManager.installDrag(this, "TargetHUD", 130, 130);
    private final Draggable hotbarDrag = DragManager.installDrag(this, "CustomHotbar", 150, 220);
    private final Draggable armourDrag = DragManager.installDrag(this, "ArmourBar", 150, 245);
    private final Draggable totemBarDrag = DragManager.installDrag(this, "TotemBar", 150, 270);

    public final NotificationsElement notifications = new NotificationsElement();

    public float getBackgroundIntensity() {
        return backgroundIntensity.getFloatValue();
    }

    public void drawHeaderBackground(float x, float y, float w, float h, float radius, int alpha) {
        float intensity = headerIntensity.getFloatValue();
        if (elements.isEnabled("Блюр фона")) {
            int color = ColorProvider.setAlpha(ColorProvider.getColorInterfaceBg(), (int) (alpha * intensity));
            DrawUtil.drawRoundBlur(x, y, w, h, radius, ColorProvider.rgba(200, 200, 200, alpha), 12);
            DrawUtil.drawRound(x, y, w, h, radius, color);
        } else {
            DrawUtil.drawRound(x, y, w, h, radius, ColorProvider.setAlpha(ColorProvider.getColorInterfaceBg(), (int) (alpha * intensity)));
        }
    }

    private void drawItemBackground(float x, float y, float w, float h, float radius, int alpha) {
        float intensity = itemIntensity.getFloatValue();
        if (elements.isEnabled("Блюр фона")) {
            int color = ColorProvider.setAlpha(ColorProvider.getColorInterfaceBg(), (int) (alpha * intensity));
            DrawUtil.drawRoundBlur(x, y, w, h, radius, ColorProvider.rgba(200, 200, 200, alpha), 12);
            DrawUtil.drawRound(x, y, w, h, radius, color);
        } else {
            DrawUtil.drawRound(x, y, w, h, radius, ColorProvider.setAlpha(ColorProvider.getColorInterfaceBg(), (int) (alpha * intensity)));
        }
    }

    public void drawBackground(float x, float y, float w, float h, float radius, int alpha) {
        if (elements.isEnabled("Блюр фона")) {
            int color = ColorProvider.setAlpha(ColorProvider.getColorInterfaceBg(), (int) (alpha * backgroundIntensity.getFloatValue()));

            DrawUtil.drawRoundBlur(x, y, w, h, radius, ColorProvider.rgba(200, 200, 200, alpha), 12);
            DrawUtil.drawRound(x, y, w, h, radius, color);

        } else {
            int color = ColorProvider.setAlpha(ColorProvider.getColorInterfaceBg(), (int) (alpha * backgroundIntensity.getFloatValue()));
            DrawUtil.drawRound(x, y, w, h, radius, color);
        }

    }

    public void drawBackground(float x, float y, float w, float h, float tl, float tr, float bl, float br, int alpha) {
        org.joml.Vector4f radii = new org.joml.Vector4f(tl, tr, bl, br);
        if (elements.isEnabled("Блюр фона")) {
            int color = ColorProvider.setAlpha(ColorProvider.getColorInterfaceBg(), (int) (alpha * backgroundIntensity.getFloatValue()));
            DrawUtil.drawRoundBlur(x, y, w, h, radii, ColorProvider.rgba(200, 200, 200, alpha), 12);
            DrawUtil.drawRound(x, y, w, h, radii, color);
        } else {
            int color = ColorProvider.setAlpha(ColorProvider.getColorInterfaceBg(), (int) (alpha * backgroundIntensity.getFloatValue()));
            DrawUtil.drawRound(x, y, w, h, radii, color);
        }

    }

    @Subscribe
    public void onEventHUD(EventHUD e) {
        if (mc.player == null || mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;

        if (elements.isEnabled("Нотификации")) {
            notifications.render(e.getDrawContext());
            renderNotificationsExample(e.getDrawContext());
        }

        if (elements.isEnabled("Ватермарка")) {
            renderWatermark(e.getDrawContext());
        }
        if (elements.isEnabled("Инфо")) {
            renderInfo(e.getDrawContext());
        }
        if (elements.isEnabled("Координаты")) {
            renderCoordsInfo(e.getDrawContext());
        }
        if (elements.isEnabled("Активный таргет")) {
            renderTargetHUD(e.getDrawContext());
        }
        if (elements.isEnabled("Привязанные модули")) {
            renderKeyBinds(e.getDrawContext());
        }
        if (elements.isEnabled("Активные модераторы")) {
            renderStaffList(e.getDrawContext());
        }
        if (elements.isEnabled("Бафы")) {
            renderPotions(e.getDrawContext());
        }
        if (elements.isEnabled("КулДауны")) {
            renderCoolDowns(e.getDrawContext());
        }
        if (elements.isEnabled("ServerHelper")) {
            renderServerHelper(e.getDrawContext());
        }
        if (elements.isEnabled("Кастомный хотбар")) {
            renderCustomHotbar(e.getDrawContext());
        }
        if (elements.isEnabled("Броня")) {
            renderArmourBar(e.getDrawContext());
        }
        if (elements.isEnabled("Полоса тотемов")) {
            renderTotemBar(e.getDrawContext());
        }
    }

    @Subscribe
    private void onUpdate(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        if (elements.isEnabled("Активные модераторы")) {
            update();
        }
        if (elements.isEnabled("Бафы")) {
            updatePotions();
        }
        if (elements.isEnabled("КулДауны")) {
            updateCooldowns();
        }
    }

    @Subscribe
    private void onPopTotem(EventPopTotem e) {
        if (!isTotemNotifEnabled()) return;
        PlayerEntity player = e.getPlayer();
        String name = player.getName().getString();
        boolean enchanted = !player.getOffHandStack().getEnchantments().isEmpty();

        Text tagText = NameTags.processName(player);
        notifications.postTotem(tagText, enchanted);
    }

    @Subscribe
    private void onCooldownPacket(EventPacket e) {
        if (mc.player == null || e.getType() != EventPacket.Type.RECEIVE) return;
        if (e.getPacket() instanceof CooldownUpdateS2CPacket c) {
            Item item = Registries.ITEM.get(c.cooldownGroup());
            if (item == null || item == Items.AIR) return;
            for (CooldownItem ci : cooldownItems) {
                if (ci.item == item) ci.active = false;
            }
            if (c.cooldown() != 0) {
                long durMs = c.cooldown() * 50L;
                cooldownItems.add(new CooldownItem(item, System.currentTimeMillis() + durMs, durMs));
            }
        } else if (e.getPacket() instanceof PlayerRespawnS2CPacket) {
            cooldownItems.clear();
        }
    }

    private final Animation animation = new Animation(Easing.EXPO_OUT, 300);
    private final Animation armorAnim = new Animation(Easing.EXPO_OUT, 300);
    private final Animation hpAnimation = new Animation(Easing.EXPO_OUT, 600);
    private final Animation outdatedHpAnimation = new Animation(Easing.EXPO_OUT, 1200);
    private final Animation absorptionAnimation = new Animation(Easing.EXPO_OUT, 300);
    private final Animation absorptionTrailAnimation = new Animation(Easing.EXPO_OUT, 1200);

    private float lastHealthVal = 0;
    private long lastTime = System.currentTimeMillis();

    private Entity lastTarget;
    private float lastHpPercent = -1f;
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);

    private final Animation lowHpAlertAnimation = new Animation(Easing.EXPO_OUT, 300);


    private void renderKeyBinds(DrawContext context) {
        if (mc.player == null) return;

        if (!(mc.currentScreen instanceof ChatScreen)) {
            keybindsPopup.open = false;
            keybindsPopup.draggingSlider = null;
        }

        beginScale(keybindsPopup, context);
        renderKeyBindsNew(context);
        endScale(keybindsPopup, context);

        runPopup(keybindsPopup, context);
    }

    private record BindEntry(String label, String bind, double animValue, ModuleCategory category) {}

    // Переиспользуемый буфер, чтобы не аллоцировать новый ArrayList каждый кадр.
    private final List<BindEntry> keybindEntries = new ArrayList<>();

    private void renderKeyBindsNew(DrawContext context) {
        if (mc.player == null) return;

        float posX = keyBindsDrag.getX();
        float posY = keyBindsDrag.getY();

        float headerHeight = 14f;
        float itemHeight = 9.5f;
        float minWidth = 52f;
        float padX = 5f;
        float padY = 2f;

        List<BindEntry> entries = keybindEntries;
        entries.clear();
        for (Module module : AlphaDLC.getInstance().getModuleStorage().getModules()) {
            if (module.getKey() != -1 && module.getAnimation().getValue() > 0.001) {
                entries.add(new BindEntry(module.getName(), KeyStorage.getKey(module.getKey()), module.getAnimation().getValue(), module.getCategory()));
            }
            for (Setting setting : module.getSettings()) {
                if (setting instanceof BooleanSetting bs && bs.getKey() != -1 && bs.getValue()) {
                    entries.add(new BindEntry(bs.getName(), KeyStorage.getKey(bs.getKey()), 1.0, module.getCategory()));
                }
            }
        }

        boolean isFound = !entries.isEmpty();
        if (!isFound && !(mc.currentScreen instanceof ChatScreen)) alpha.run(0);
        else alpha.run(1);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int headerAlpha = (int) Math.min(255, Math.max(0, 255 * globalAlpha));

        boolean showExample = (mc.currentScreen instanceof ChatScreen) && !isFound;

        // Расчёт ширины левой (иконка + название) и правой (бинд) колонок
        float maxLabelBoxW = minWidth;
        float maxBindBoxW = 0f;
        for (BindEntry entry : entries) {
            float animVal = (float) Math.min(1.0, Math.max(0.0, entry.animValue()));
            if (animVal <= 0.001f) continue;
            float lw = 9f + Fonts.SFMEDIUM.get().getWidth(entry.label(), 7f) + 7f;
            if (lw > maxLabelBoxW) maxLabelBoxW = lw;
            float bw = Fonts.SFMEDIUM.get().getWidth(entry.bind(), 6.75f) + 6f;
            if (bw > maxBindBoxW) maxBindBoxW = bw;
        }
        if (showExample) {
            float lw = 9f + Fonts.SFMEDIUM.get().getWidth("Example", 7f) + 7f;
            if (lw > maxLabelBoxW) maxLabelBoxW = lw;
            float bw = Fonts.SFMEDIUM.get().getWidth("K", 6.75f) + 6f;
            if (bw > maxBindBoxW) maxBindBoxW = bw;
        }

        // Высота панели считается по сумме анимированных высот строк — плавное появление/исчезновение
        float contentHeight = 0f;
        if (showExample) {
            contentHeight = itemHeight;
        } else {
            for (BindEntry entry : entries) {
                float av = (float) Math.min(1.0, Math.max(0.0, entry.animValue()));
                if (av <= 0.001f) continue;
                contentHeight += itemHeight * av;
            }
        }

        float rawWidth = maxLabelBoxW + maxBindBoxW + padX * 2;
        keybindsPopup.panelWidth.run(rawWidth);
        float totalRowWidth = (float) keybindsPopup.panelWidth.getValue();
        if (totalRowWidth < 20f) totalRowWidth = rawWidth;
        float totalHeight = headerHeight + contentHeight + padY * 2;

        drawElementBackground(keybindsPopup, posX, posY, totalRowWidth, totalHeight, 3f, globalAlpha);
        drawElementShine(keybindsPopup, context, posX, posY, totalRowWidth, totalHeight, 3f);

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Keybinds", posX + padX + 4f, posY + padY + 2.5f, ColorProvider.rgba(255, 255, 255, headerAlpha), 8f);
        DrawUtil.drawText(Fonts.ALPHADLC.get(), "g", posX + totalRowWidth - padX - 11f, posY + padY + 3f, ColorProvider.setAlpha(ColorProvider.getColorIcons(), headerAlpha), 9);

        float curY = posY + headerHeight + padY;

        for (BindEntry entry : entries) {
            float animVal = (float) Math.min(1.0, Math.max(0.0, entry.animValue()));
            if (animVal <= 0.001f) continue;

            int itemAlpha = (int) Math.min(255, Math.max(0, 255 * animVal * globalAlpha));
            if (itemAlpha < 5) continue;

            float rowHeight = itemHeight * animVal;
            context.getMatrices().push();
            context.getMatrices().translate(posX + totalRowWidth / 2f, curY + rowHeight / 2f, 0);
            context.getMatrices().scale(animVal, animVal, animVal);
            context.getMatrices().translate(-(posX + totalRowWidth / 2f), -(curY + rowHeight / 2f), 0);

            String catIcon = switch (entry.category()) {
                case COMBAT -> "a";
                case MOVEMENT -> "b";
                case RENDER -> "c";
                case PLAYER -> "d";
                case MISC -> "e";
            };
            DrawUtil.drawText(Fonts.ICONS_MINCED.get(), catIcon, posX + padX + 1f, curY + 1.5f, ColorProvider.setAlpha(ColorProvider.getColorIcons(), itemAlpha), 7f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), entry.label(), posX + padX + 9f, curY + 1.35f, ColorProvider.rgba(255, 255, 255, itemAlpha), 7f);

            drawKeybindCap(entry.bind(), posX + totalRowWidth - padX - maxBindBoxW, curY, maxBindBoxW, itemHeight, itemAlpha);

            context.getMatrices().pop();
            curY += rowHeight;
        }

        if (showExample) {
            int exampleAlpha = headerAlpha;
            DrawUtil.drawText(Fonts.ICONS_MINCED.get(), "a", posX + padX + 1f, curY + 1.5f, ColorProvider.setAlpha(ColorProvider.getColorIcons(), exampleAlpha), 7f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Example", posX + padX + 9f, curY + 1.35f, ColorProvider.rgba(255, 255, 255, exampleAlpha), 7f);
            drawKeybindCap("K", posX + totalRowWidth - padX - maxBindBoxW, curY, maxBindBoxW, itemHeight, exampleAlpha);
        }

        keyBindsDrag.setWidth(totalRowWidth);
        keyBindsDrag.setHeight(totalHeight);
    }

    // Клавиша бинда без фона-квадратика (только текст, центрированный в колонке)
    private void drawKeybindCap(String bind, float colX, float rowY, float colW, float itemHeight, int itemAlpha) {
        float bindW = Fonts.SFMEDIUM.get().getWidth(bind, 6.75f);
        // +0.75 компенсирует горизонтальный сдвиг пера в рендере текста
        float textX = colX + (colW - bindW) / 2f + 0.75f;
        float textY = rowY + itemHeight / 2f - 4f;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), bind, textX, textY, ColorProvider.rgba(255, 255, 255, itemAlpha), 6.75f);
    }

    private final List<Staff> staffPlayers = new ArrayList<>();
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    private final Pattern prefixMatches = Pattern.compile(".*(ꔷ|ꔳ|ꔩ|ꔥ|ꔡ|ꔗ|ꔓ|\\bmod\\b|\\badm\\b|\\bhelp\\b|\\bwne\\b|модер|хелп|помощ|админ|владел|отриц|\\btaf\\b|\\bcurat\\b|куратор|\\bdev\\b|разраб|\\bsupp\\b|саппорт|\\byt\\b|\\[yt\\]|ютуб|стажер|сотрудник).*");
    // Геометрия кастомного хотбара (как ванильный: основной блок по центру, офф-хенд отдельно слева)
    private static final float HB_PAD = 3f;
    private static final float HB_CELL = 20f;
    private static final float HB_ICON = 16f;
    private static final float HB_OFFHAND_GAP = 4f;

    private float hbMainWidth() { return HB_PAD * 2f + HB_CELL * 9f; }
    private float hbOffhandBoxW() { return HB_PAD * 2f + HB_CELL; }
    private float hbHeight() { return HB_PAD * 2f + HB_CELL; }
    private float hbBottomY() { return mc.getWindow().getScaledHeight() - 4f; }

    // Правый видимый край основного блока хотбара (с учётом его масштаба)
    private float hotbarMainVisualRight() {
        float size = hotbarPopup.size.getFloatValue();
        return mc.getWindow().getScaledWidth() / 2f + hbMainWidth() * size / 2f;
    }

    // Цвет полосы прочности: зелёный -> жёлтый -> красный
    private int durabilityColor(float ratio) {
        ratio = MathHelper.clamp(ratio, 0f, 1f);
        int rgb = java.awt.Color.HSBtoRGB(ratio * 0.33f, 0.85f, 0.95f);
        return ColorProvider.rgba((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255);
    }

    // Обрезка текста по ширине с многоточием
    private String trimTextToWidth(String text, float size, float maxWidth) {
        var font = Fonts.SFMEDIUM.get();
        if (text == null || font.getWidth(text, size) <= maxWidth) return text;
        String ellipsis = "..";
        String result = text;
        while (result.length() > 0 && font.getWidth(result + ellipsis, size) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + ellipsis;
    }

    // Текст с тёмной обводкой (для количества предметов)
    private void drawOutlinedCount(String text, float x, float y, float size) {
        int outline = ColorProvider.rgba(0, 0, 0, 200);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), text, x - 0.6f, y, outline, size);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), text, x + 0.6f, y, outline, size);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), text, x, y - 0.6f, outline, size);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), text, x, y + 0.6f, outline, size);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), text, x, y, -1, size);
    }

    // ===== CustomHotbar (заменяет ванильный; основной блок по центру, офф-хенд отдельно слева) =====
    private void renderCustomHotbar(DrawContext context) {
        if (mc.player == null) return;

        float size = hotbarPopup.size.getFloatValue();
        float offW = hbOffhandBoxW();
        float mainW = hbMainWidth();
        float height = hbHeight();
        float totalWidth = offW + HB_OFFHAND_GAP + mainW;
        float screenW = mc.getWindow().getScaledWidth();

        // Origin (левый-верх) так, чтобы ОСНОВНОЙ блок был по центру экрана, а низ — на hbBottomY(),
        // независимо от масштаба (beginScale масштабирует вокруг origin).
        float ox = screenW / 2f - (offW + HB_OFFHAND_GAP + mainW / 2f) * size;
        float oy = hbBottomY() - height * size;

        hotbarDrag.setX(ox);
        hotbarDrag.setY(oy);
        hotbarDrag.setWidth(totalWidth);
        hotbarDrag.setHeight(height);

        if (!(mc.currentScreen instanceof ChatScreen)) {
            hotbarPopup.open = false;
            hotbarPopup.draggingSlider = null;
        }
        beginScale(hotbarPopup, context);
        renderCustomHotbarContent(context, ox, oy);
        endScale(hotbarPopup, context);
        runPopup(hotbarPopup, context);
    }

    private void renderCustomHotbarContent(DrawContext context, float ox, float oy) {
        float pad = HB_PAD;
        float cell = HB_CELL;
        float iconSize = HB_ICON;
        float offW = hbOffhandBoxW();
        float mainW = hbMainWidth();
        float height = hbHeight();

        int selected = mc.player.getInventory().selectedSlot;
        int divColor = ColorProvider.rgba(255, 255, 255, 35);

        // ---- Офф-хенд (левая рука) — отдельный блок слева, как в ванилле ----
        float offX = ox;
        float offY = oy;
        drawElementBackground(hotbarPopup, offX, offY, offW, height, 3f, 1f);
        drawElementShine(hotbarPopup, context, offX, offY, offW, height, 3f);
        drawHotbarCell(context, mc.player.getOffHandStack(), offX + pad, offY + pad, cell, iconSize, false);

        // ---- Основной блок: 9 слотов ----
        float mainX = ox + offW + HB_OFFHAND_GAP;
        float mainY = oy;
        drawElementBackground(hotbarPopup, mainX, mainY, mainW, height, 3f, 1f);
        drawElementShine(hotbarPopup, context, mainX, mainY, mainW, height, 3f);

        // Счётчик уровня опыта (зелёный) сверху по центру основного блока, без полоски
        String lvl = String.valueOf(mc.player.experienceLevel);
        float lvlW = Fonts.SFMEDIUM.get().getWidth(lvl, 8f);
        int xpColor = ColorProvider.rgba(80, 220, 80, 255);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), lvl, mainX + mainW / 2f - lvlW / 2f, mainY - 9f, xpColor, 8f);

        float cellY = mainY + pad;
        for (int i = 0; i < 9; i++) {
            float cellX = mainX + pad + i * cell;
            if (i > 0) {
                DrawUtil.drawRound(cellX - 0.25f, cellY + 3f, 0.5f, cell - 6f, 0.25f, divColor);
            }
            drawHotbarCell(context, mc.player.getInventory().getStack(i), cellX, cellY, cell, iconSize, i == selected);
        }
    }

    private void drawHotbarCell(DrawContext context, ItemStack stack, float cellX, float cellY, float cell, float iconSize, boolean selected) {
        if (selected) {
            DrawUtil.drawRound(cellX + 0.5f, cellY, cell - 1f, cell, 2.5f,
                    ColorProvider.setAlpha(ColorProvider.getColorClient(), 90));
        }
        if (stack.isEmpty()) return;

        float iconX = cellX + (cell - iconSize) / 2f;
        float iconY = cellY + (cell - iconSize) / 2f;
        drawCooldownIcon(context, stack, iconX, iconY, iconSize, 255);

        if (hotbarCounts.getValue() && stack.getCount() > 1) {
            String cnt = String.valueOf(stack.getCount());
            float cw = Fonts.SFMEDIUM.get().getWidth(cnt, 7.5f);
            drawOutlinedCount(cnt, cellX + cell - cw - 1.5f, cellY + cell - 8.5f, 7.5f);
        }
    }

    // ===== ArmourBar (зафиксирован справа от хотбара) =====
    private void renderArmourBar(DrawContext context) {
        if (mc.player == null) return;

        float pad = 3f;
        float cell = 20f;
        float iconSize = 16f;
        int count = 4;
        float width = pad * 2f + cell * count;
        float height = pad * 2f + cell;

        float size = armourPopup.size.getFloatValue();
        float screenW = mc.getWindow().getScaledWidth();

        // Левый край примыкает к правому краю хотбара, низ выровнен с хотбаром — не зависит от масштаба
        float ox = hotbarMainVisualRight() + 4f;
        float oy = hbBottomY() - height * size;
        if (ox + width * size > screenW - 2f) ox = screenW - 2f - width * size;

        armourDrag.setX(ox);
        armourDrag.setY(oy);
        armourDrag.setWidth(width);
        armourDrag.setHeight(height);

        if (!(mc.currentScreen instanceof ChatScreen)) {
            armourPopup.open = false;
            armourPopup.draggingSlider = null;
        }
        beginScale(armourPopup, context);
        renderArmourBarContent(context, ox, oy, width, height, pad, cell, iconSize);
        endScale(armourPopup, context);
        runPopup(armourPopup, context);
    }

    private void renderArmourBarContent(DrawContext context, float posX, float posY, float width, float height,
                                        float pad, float cell, float iconSize) {
        boolean durab = armourDurability.getValue();

        net.minecraft.entity.EquipmentSlot[] eqSlots = {
                net.minecraft.entity.EquipmentSlot.HEAD,
                net.minecraft.entity.EquipmentSlot.CHEST,
                net.minecraft.entity.EquipmentSlot.LEGS,
                net.minecraft.entity.EquipmentSlot.FEET
        };

        drawElementBackground(armourPopup, posX, posY, width, height, 3f, 1f);
        drawElementShine(armourPopup, context, posX, posY, width, height, 3f);

        for (int i = 0; i < eqSlots.length; i++) {
            float cellX = posX + pad + i * cell;
            float cellY = posY + pad;
            ItemStack stack = mc.player.getEquippedStack(eqSlots[i]);
            if (stack.isEmpty()) continue;

            float iconX = cellX + (cell - iconSize) / 2f;
            float iconY = cellY + (cell - iconSize) / 2f;
            drawCooldownIcon(context, stack, iconX, iconY, iconSize, 255);

            if (durab && stack.isDamageable() && stack.getMaxDamage() > 0) {
                float ratio = 1f - (float) stack.getDamage() / (float) stack.getMaxDamage();
                ratio = MathHelper.clamp(ratio, 0f, 1f);
                float barH = 2.5f;
                float barW = iconSize;
                float barX = cellX + (cell - barW) / 2f;
                float barY = cellY + cell - barH - 0.5f;
                DrawUtil.drawRound(barX, barY, barW, barH, barH / 2f, ColorProvider.rgba(20, 20, 20, 200));
                DrawUtil.drawRound(barX, barY, barW * ratio, barH, barH / 2f, durabilityColor(ratio));
            }
        }
    }

    // ===== TotemBar (цифра под тотемом) =====
    private void renderTotemBar(DrawContext context) {
        if (mc.player == null) return;
        if (!(mc.currentScreen instanceof ChatScreen)) {
            totemBarPopup.open = false;
            totemBarPopup.draggingSlider = null;
        }
        beginScale(totemBarPopup, context);
        renderTotemBarContent(context);
        endScale(totemBarPopup, context);
        runPopup(totemBarPopup, context);
    }

    private void renderTotemBarContent(DrawContext context) {
        float posX = totemBarDrag.getX();
        float posY = totemBarDrag.getY();

        int totemCount = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.getItem() == net.minecraft.item.Items.TOTEM_OF_UNDYING) totemCount += s.getCount();
        }

        String text = totemCount + "x";
        float pad = 4f;
        float iconSize = 16f;
        float textSize = 8f;
        float gap = 1.5f;
        float textW = Fonts.SFMEDIUM.get().getWidth(text, textSize);

        float contentW = Math.max(iconSize, textW);
        float width = pad * 2f + contentW;
        float height = pad * 2f + iconSize + gap + textSize;

        drawElementBackground(totemBarPopup, posX, posY, width, height, 3f, 1f);
        drawElementShine(totemBarPopup, context, posX, posY, width, height, 3f);

        float iconX = posX + (width - iconSize) / 2f;
        float iconY = posY + pad;
        drawCooldownIcon(context, new ItemStack(net.minecraft.item.Items.TOTEM_OF_UNDYING), iconX, iconY, iconSize, 255);

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), text, posX + (width - textW) / 2f, iconY + iconSize + gap, -1, textSize);

        totemBarDrag.setWidth(width);
        totemBarDrag.setHeight(height);
    }

    private final Animation alpha2 = new Animation(Easing.EXPO_OUT, 200);

    private void renderStaffList(DrawContext context) {
        if (mc.player == null) return;

        if (!(mc.currentScreen instanceof ChatScreen)) {
            stafflistPopup.open = false;
            stafflistPopup.draggingSlider = null;
        }

        beginScale(stafflistPopup, context);
        renderStaffListNew(context);
        endScale(stafflistPopup, context);

        runPopup(stafflistPopup, context);
    }

    private void renderStaffListNew(DrawContext context) {
        float posX = staffListDrag.getX();
        float posY = staffListDrag.getY();

        float headerHeight = 14f;
        float itemHeight = 11.5f;
        float minWidth = 52f;
        float padX = 5f;
        float padY = 2f;
        float statusBoxW = 12f;

        for (Staff staff : staffPlayers) staff.animation.run(staff.isOnServer ? 1 : 0);

        boolean isFound = false;
        for (Staff staff : staffPlayers) {
            if (staff.animation.getValue() > 0.001f) isFound = true;
        }

        if (!isFound && !(mc.currentScreen instanceof ChatScreen)) alpha2.run(0);
        else alpha2.run(1);

        float globalAlpha = (float) alpha2.getValue();
        if (globalAlpha <= 0.05f) return;

        int headerAlpha = (int) Math.min(255, Math.max(0, 255 * globalAlpha));

        boolean showExample = (mc.currentScreen instanceof ChatScreen) && !isFound;

        float maxNameBoxW = minWidth;
        for (Staff staff : staffPlayers) {
            if (staff.animation.getValue() > 0.001f) {
                float nw = 11f + Fonts.SFMEDIUM.get().getWidth(staff.prefix, 7f) + 7f;
                if (nw > maxNameBoxW) maxNameBoxW = nw;
            }
        }
        if (showExample) {
            float nw = 11f + Fonts.SFMEDIUM.get().getWidth("Example", 7f) + 7f;
            if (nw > maxNameBoxW) maxNameBoxW = nw;
        }

        // Плавная высота по сумме анимированных строк и плавная ширина
        float contentHeight = 0f;
        if (showExample) {
            contentHeight = itemHeight;
        } else {
            for (Staff staff : staffPlayers) {
                float av = (float) staff.animation.getValue();
                if (av <= 0.001f) continue;
                contentHeight += itemHeight * av;
            }
        }

        float rawWidth = maxNameBoxW + statusBoxW + padX * 2;
        stafflistPopup.panelWidth.run(rawWidth);
        float totalRowWidth = (float) stafflistPopup.panelWidth.getValue();
        if (totalRowWidth < 20f) totalRowWidth = rawWidth;
        float totalHeight = headerHeight + contentHeight + padY * 2;

        drawElementBackground(stafflistPopup, posX, posY, totalRowWidth, totalHeight, 3f, globalAlpha);
        drawElementShine(stafflistPopup, context, posX, posY, totalRowWidth, totalHeight, 3f);

        // Иконка шапки — ровно над кружком статуса (+0.75 компенсирует сдвиг пера в рендере текста)
        float statusCX = posX + totalRowWidth - padX - statusBoxW / 2f;
        float headerIconW = Fonts.ALPHADLC.get().getWidth("i", 9);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "StaffList", posX + padX + 4f, posY + padY + 2.5f, ColorProvider.rgba(255, 255, 255, headerAlpha), 8f);
        DrawUtil.drawText(Fonts.ALPHADLC.get(), "i", statusCX - headerIconW / 2f + 0.75f, posY + padY + 3f, ColorProvider.setAlpha(ColorProvider.getColorIcons(), headerAlpha), 9);

        float curY = posY + headerHeight + padY;

        for (Staff staff : staffPlayers) {
            float animVal = (float) staff.animation.getValue();
            if (animVal <= 0.001f) continue;

            int itemAlpha = (int) Math.min(255, Math.max(0, 255 * animVal * globalAlpha));
            if (itemAlpha < 5) continue;

            float rowHeight = itemHeight * animVal;
            context.getMatrices().push();
            context.getMatrices().translate(posX + totalRowWidth / 2f, curY + rowHeight / 2f, 0);
            context.getMatrices().scale(animVal, animVal, animVal);
            context.getMatrices().translate(-(posX + totalRowWidth / 2f), -(curY + rowHeight / 2f), 0);

            net.minecraft.util.Identifier skinTexture;
            PlayerListEntry playerEntry = mc.getNetworkHandler().getPlayerListEntry(staff.name);
            if (playerEntry != null) {
                skinTexture = playerEntry.getSkinTextures().texture();
            } else {
                skinTexture = DefaultSkinHelper.getTexture();
            }
            int textureId = mc.getTextureManager().getTexture(skinTexture).getGlId();
            float headSize = 8f;
            Builder.texture()
                    .size(new SizeState(headSize, headSize))
                    .radius(new QuadRadiusState(2))
                    .color(new QuadColorState(ColorProvider.setAlpha(-1, itemAlpha)))
                    .texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, textureId)
                    .smoothness(1f)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), posX + padX + 1f, curY + (itemHeight - headSize) / 2f);

            DrawUtil.drawText(Fonts.SFMEDIUM.get(), staff.prefix, posX + padX + 10f, curY + (itemHeight - 7f) / 2f - 0.35f, 7f, itemAlpha);

            int dotColor = staff.status == Status.NONE ? ColorProvider.rgba(32, 255, 32, itemAlpha) : ColorProvider.rgba(255, 32, 32, itemAlpha);
            float dotCX = posX + totalRowWidth - padX - statusBoxW / 2f;
            DrawUtil.drawCircle(dotCX, curY + itemHeight / 2f, 3.5f, dotColor);

            context.getMatrices().pop();
            curY += rowHeight;
        }

        if (showExample) {
            int exampleAlpha = headerAlpha;
            int textureId = mc.getTextureManager().getTexture(DefaultSkinHelper.getTexture()).getGlId();
            float headSize = 8f;
            Builder.texture()
                    .size(new SizeState(headSize, headSize))
                    .radius(new QuadRadiusState(2))
                    .color(new QuadColorState(ColorProvider.setAlpha(-1, exampleAlpha)))
                    .texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, textureId)
                    .smoothness(1f)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), posX + padX + 1f, curY + (itemHeight - headSize) / 2f);

            DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Example", posX + padX + 10f, curY + (itemHeight - 7f) / 2f - 0.35f, ColorProvider.rgba(255, 255, 255, exampleAlpha), 7f);
            float dotCX = posX + totalRowWidth - padX - statusBoxW / 2f;
            DrawUtil.drawCircle(dotCX, curY + itemHeight / 2f, 3.5f, ColorProvider.rgba(32, 255, 32, exampleAlpha));
        }

        staffListDrag.setWidth(totalRowWidth);
        staffListDrag.setHeight(totalHeight);
    }

    private final Animation alpha3 = new Animation(Easing.EXPO_OUT, 200);

    // Кольцо-таймер эффекта — только для Potions
    private static final float POTION_RING_DIAMETER = 8f;
    private static final float POTION_RING_GAP = 3f;
    private static final float POTION_RING_THICKNESS = 1.4f;

    // === Общие настройки HUD-элементов (правый клик по элементу) ===
    private enum PopupKind { TOGGLE, SLIDER }

    private static final class PopupRow {
        final PopupKind kind;
        final String label;
        final BooleanSetting bool;
        final SliderSetting slider;
        float x, y, w, h;

        PopupRow(PopupKind kind, String label, BooleanSetting bool, SliderSetting slider) {
            this.kind = kind;
            this.label = label;
            this.bool = bool;
            this.slider = slider;
        }
    }

    // Набор настроек и состояние окна для одного HUD-элемента
    private static final class HudPopup {
        final String title;
        final Draggable drag;
        final BooleanSetting blur, shine, corners;
        final SliderSetting size, alpha, shineAlpha, shineThickness;
        final BooleanSetting ring; // nullable — только для Potions

        boolean open = false;
        boolean transformed = false;
        final Animation anim = new Animation(Easing.EXPO_OUT, 250);
        // Плавное изменение ширины панели при появлении/исчезновении строк
        final Animation panelWidth = new Animation(Easing.EXPO_OUT, 220);
        final java.util.List<PopupRow> rendered = new java.util.ArrayList<>();
        // Дополнительные строки-переключатели (специфичные для элемента)
        final java.util.List<PopupRow> extraRows = new java.util.ArrayList<>();
        float px, py, pw, ph;
        SliderSetting draggingSlider = null;
        float trackX, trackW;

        HudPopup(String title, Draggable drag,
                 BooleanSetting blur, SliderSetting size, SliderSetting alpha, BooleanSetting ring,
                 BooleanSetting shine, SliderSetting shineAlpha, SliderSetting shineThickness,
                 BooleanSetting corners) {
            this.title = title;
            this.drag = drag;
            this.blur = blur;
            this.size = size;
            this.alpha = alpha;
            this.ring = ring;
            this.shine = shine;
            this.shineAlpha = shineAlpha;
            this.shineThickness = shineThickness;
            this.corners = corners;
        }
    }

    // Potions
    private final BooleanSetting potionsBlur = new BooleanSetting("Блюр бафов", true).setVisible(() -> false);
    private final SliderSetting potionsSize = new SliderSetting("Размер бафов", 1.0, 0.5, 2.0, 0.05).setVisible(() -> false);
    private final SliderSetting potionsAlpha = new SliderSetting("Прозрачность бафов", 180, 0, 255, 1).setVisible(() -> false);
    private final BooleanSetting potionsRing = new BooleanSetting("Кольцо таймер", true).setVisible(() -> false);
    private final BooleanSetting potionsShine = new BooleanSetting("Блик бафов", true).setVisible(() -> false);
    private final SliderSetting potionsShineAlpha = new SliderSetting("Прозрачность блика", 30, 0, 100, 1).setVisible(() -> false);
    private final SliderSetting potionsShineThickness = new SliderSetting("Толщина блика", 0.5, 0.2, 2.0, 0.05).setVisible(() -> false);
    private final BooleanSetting potionsCorners = new BooleanSetting("Уголки бафов", true).setVisible(() -> false);

    // Keybinds
    private final BooleanSetting keybindsBlur = new BooleanSetting("Блюр кейбиндов", true).setVisible(() -> false);
    private final SliderSetting keybindsSize = new SliderSetting("Размер кейбиндов", 1.0, 0.5, 2.0, 0.05).setVisible(() -> false);
    private final SliderSetting keybindsAlpha = new SliderSetting("Прозрачность кейбиндов", 180, 0, 255, 1).setVisible(() -> false);
    private final BooleanSetting keybindsShine = new BooleanSetting("Блик кейбиндов", true).setVisible(() -> false);
    private final SliderSetting keybindsShineAlpha = new SliderSetting("Прозрачность блика кб", 30, 0, 100, 1).setVisible(() -> false);
    private final SliderSetting keybindsShineThickness = new SliderSetting("Толщина блика кб", 0.5, 0.2, 2.0, 0.05).setVisible(() -> false);
    private final BooleanSetting keybindsCorners = new BooleanSetting("Уголки кейбиндов", true).setVisible(() -> false);

    // StaffList
    private final BooleanSetting stafflistBlur = new BooleanSetting("Блюр стафф", true).setVisible(() -> false);
    private final SliderSetting stafflistSize = new SliderSetting("Размер стафф", 1.0, 0.5, 2.0, 0.05).setVisible(() -> false);
    private final SliderSetting stafflistAlpha = new SliderSetting("Прозрачность стафф", 180, 0, 255, 1).setVisible(() -> false);
    private final BooleanSetting stafflistShine = new BooleanSetting("Блик стафф", true).setVisible(() -> false);
    private final SliderSetting stafflistShineAlpha = new SliderSetting("Прозрачность блика стафф", 30, 0, 100, 1).setVisible(() -> false);
    private final SliderSetting stafflistShineThickness = new SliderSetting("Толщина блика стафф", 0.5, 0.2, 2.0, 0.05).setVisible(() -> false);
    private final BooleanSetting stafflistCorners = new BooleanSetting("Уголки списка модераторов", true).setVisible(() -> false);

    // CoolDowns
    private final BooleanSetting cooldownsBlur = new BooleanSetting("Блюр кд", true).setVisible(() -> false);
    private final SliderSetting cooldownsSize = new SliderSetting("Размер кд", 1.0, 0.5, 2.0, 0.05).setVisible(() -> false);
    private final SliderSetting cooldownsAlpha = new SliderSetting("Прозрачность кд", 180, 0, 255, 1).setVisible(() -> false);
    private final BooleanSetting cooldownsRing = new BooleanSetting("Кольцо таймер кд", true).setVisible(() -> false);
    private final BooleanSetting cooldownsShine = new BooleanSetting("Блик кд", true).setVisible(() -> false);
    private final SliderSetting cooldownsShineAlpha = new SliderSetting("Прозрачность блика кд", 30, 0, 100, 1).setVisible(() -> false);
    private final SliderSetting cooldownsShineThickness = new SliderSetting("Толщина блика кд", 0.5, 0.2, 2.0, 0.05).setVisible(() -> false);
    private final BooleanSetting cooldownsCorners = new BooleanSetting("Уголки кулдаунов", true).setVisible(() -> false);

    private final HudPopup potionsPopup = new HudPopup("Potions", potionsDrag,
            potionsBlur, potionsSize, potionsAlpha, potionsRing,
            potionsShine, potionsShineAlpha, potionsShineThickness, potionsCorners);
    private final HudPopup cooldownsPopup = new HudPopup("CoolDowns", cooldownsDrag,
            cooldownsBlur, cooldownsSize, cooldownsAlpha, cooldownsRing,
            cooldownsShine, cooldownsShineAlpha, cooldownsShineThickness, cooldownsCorners);

    // ServerHelper
    private final BooleanSetting serverHelperBlur = new BooleanSetting("Блюр ServerHelper", true).setVisible(() -> false);
    private final SliderSetting serverHelperSize = new SliderSetting("Размер ServerHelper", 1.0, 0.5, 2.0, 0.05).setVisible(() -> false);
    private final SliderSetting serverHelperAlpha = new SliderSetting("Прозрачность ServerHelper", 180, 0, 255, 1).setVisible(() -> false);
    private final BooleanSetting serverHelperShine = new BooleanSetting("Блик ServerHelper", true).setVisible(() -> false);
    private final SliderSetting serverHelperShineAlpha = new SliderSetting("Прозрачность блика ServerHelper", 30, 0, 100, 1).setVisible(() -> false);
    private final SliderSetting serverHelperShineThickness = new SliderSetting("Толщина блика ServerHelper", 0.5, 0.2, 2.0, 0.05).setVisible(() -> false);
    private final BooleanSetting serverHelperCorners = new BooleanSetting("Уголки ServerHelper", true).setVisible(() -> false);
    private final HudPopup serverHelperPopup = new HudPopup("ServerHelper", serverHelperDrag,
            serverHelperBlur, serverHelperSize, serverHelperAlpha, null,
            serverHelperShine, serverHelperShineAlpha, serverHelperShineThickness, serverHelperCorners);

    // Watermark
    private final BooleanSetting wmBlur = new BooleanSetting("Блюр вм", true).setVisible(() -> false);
    private final SliderSetting wmSize = new SliderSetting("Размер вм", 1.05, 0.5, 2.0, 0.05).setVisible(() -> false);
    private final SliderSetting wmAlpha = new SliderSetting("Прозрачность вм", 180, 0, 255, 1).setVisible(() -> false);
    private final BooleanSetting wmShine = new BooleanSetting("Блик вм", true).setVisible(() -> false);
    private final SliderSetting wmShineAlpha = new SliderSetting("Прозрачность блика вм", 30, 0, 100, 1).setVisible(() -> false);
    private final SliderSetting wmShineThickness = new SliderSetting("Толщина блика вм", 0.5, 0.2, 2.0, 0.05).setVisible(() -> false);
    private final BooleanSetting wmCorners = new BooleanSetting("Уголки ватермарки", true).setVisible(() -> false);
    private final BooleanSetting wmBps = new BooleanSetting("Добавить BPS", false).setVisible(() -> false);
    private final BooleanSetting wmTps = new BooleanSetting("Добавить TPS", false).setVisible(() -> false);
    private final BooleanSetting wmCoords = new BooleanSetting("Добавить координаты", false).setVisible(() -> false);
    private final HudPopup watermarkPopup = new HudPopup("Watermark", watermarkDrag,
            wmBlur, wmSize, wmAlpha, null,
            wmShine, wmShineAlpha, wmShineThickness, wmCorners);

    // Info (нижняя строка) — отдельный перетаскиваемый элемент
    private final BooleanSetting infoBlur = new BooleanSetting("Блюр инфо", true).setVisible(() -> false);
    private final SliderSetting infoSize = new SliderSetting("Размер инфо", 1.05, 0.5, 2.0, 0.05).setVisible(() -> false);
    private final SliderSetting infoAlpha = new SliderSetting("Прозрачность инфо", 180, 0, 255, 1).setVisible(() -> false);
    private final BooleanSetting infoShine = new BooleanSetting("Блик инфо", true).setVisible(() -> false);
    private final SliderSetting infoShineAlpha = new SliderSetting("Прозрачность блика инфо", 30, 0, 100, 1).setVisible(() -> false);
    private final SliderSetting infoShineThickness = new SliderSetting("Толщина блика инфо", 0.5, 0.2, 2.0, 0.05).setVisible(() -> false);
    private final BooleanSetting infoCorners = new BooleanSetting("Уголки инфо", true).setVisible(() -> false);
    private final HudPopup infoPopup = new HudPopup("Info", infoDrag,
            infoBlur, infoSize, infoAlpha, null,
            infoShine, infoShineAlpha, infoShineThickness, infoCorners);
    {
        infoPopup.extraRows.add(new PopupRow(PopupKind.TOGGLE, "Добавить BPS", wmBps, null));
        infoPopup.extraRows.add(new PopupRow(PopupKind.TOGGLE, "Добавить TPS", wmTps, null));
        infoPopup.extraRows.add(new PopupRow(PopupKind.TOGGLE, "Добавить координаты", wmCoords, null));
    }

    // CustomHotbar
    private final BooleanSetting hotbarBlur = new BooleanSetting("Блюр хотбар", true).setVisible(() -> false);
    private final SliderSetting hotbarSize = new SliderSetting("Размер хотбар", 1.0, 0.5, 2.0, 0.05).setVisible(() -> false);
    private final SliderSetting hotbarAlpha = new SliderSetting("Прозрачность хотбар", 180, 0, 255, 1).setVisible(() -> false);
    private final BooleanSetting hotbarShine = new BooleanSetting("Блик хотбар", true).setVisible(() -> false);
    private final SliderSetting hotbarShineAlpha = new SliderSetting("Прозрачность блика хотбар", 30, 0, 100, 1).setVisible(() -> false);
    private final SliderSetting hotbarShineThickness = new SliderSetting("Толщина блика хотбар", 0.5, 0.2, 2.0, 0.05).setVisible(() -> false);
    private final BooleanSetting hotbarCorners = new BooleanSetting("Уголки хотбара", true).setVisible(() -> false);
    private final BooleanSetting hotbarCounts = new BooleanSetting("Показывать количество", true).setVisible(() -> false);
    private final HudPopup hotbarPopup = new HudPopup("CustomHotbar", hotbarDrag,
            hotbarBlur, hotbarSize, hotbarAlpha, null,
            hotbarShine, hotbarShineAlpha, hotbarShineThickness, hotbarCorners);
    {
        hotbarPopup.extraRows.add(new PopupRow(PopupKind.TOGGLE, "Показывать количество", hotbarCounts, null));
    }

    // ArmourBar
    private final BooleanSetting armourBlur = new BooleanSetting("Блюр броня", true).setVisible(() -> false);
    private final SliderSetting armourSize = new SliderSetting("Размер броня", 1.0, 0.5, 2.0, 0.05).setVisible(() -> false);
    private final SliderSetting armourAlpha = new SliderSetting("Прозрачность броня", 180, 0, 255, 1).setVisible(() -> false);
    private final BooleanSetting armourShine = new BooleanSetting("Блик броня", true).setVisible(() -> false);
    private final SliderSetting armourShineAlpha = new SliderSetting("Прозрачность блика броня", 30, 0, 100, 1).setVisible(() -> false);
    private final SliderSetting armourShineThickness = new SliderSetting("Толщина блика броня", 0.5, 0.2, 2.0, 0.05).setVisible(() -> false);
    private final BooleanSetting armourCorners = new BooleanSetting("Уголки брони", true).setVisible(() -> false);
    private final BooleanSetting armourDurability = new BooleanSetting("Полоса прочности", true).setVisible(() -> false);
    private final HudPopup armourPopup = new HudPopup("ArmourBar", armourDrag,
            armourBlur, armourSize, armourAlpha, null,
            armourShine, armourShineAlpha, armourShineThickness, armourCorners);
    {
        armourPopup.extraRows.add(new PopupRow(PopupKind.TOGGLE, "Полоса прочности", armourDurability, null));
    }

    // TotemBar
    private final BooleanSetting totemBarBlur = new BooleanSetting("Блюр тотем-бар", true).setVisible(() -> false);
    private final SliderSetting totemBarSize = new SliderSetting("Размер тотем-бар", 1.0, 0.5, 2.0, 0.05).setVisible(() -> false);
    private final SliderSetting totemBarAlpha = new SliderSetting("Прозрачность тотем-бар", 180, 0, 255, 1).setVisible(() -> false);
    private final BooleanSetting totemBarShine = new BooleanSetting("Блик тотем-бар", true).setVisible(() -> false);
    private final SliderSetting totemBarShineAlpha = new SliderSetting("Прозрачность блика тотем-бар", 30, 0, 100, 1).setVisible(() -> false);
    private final SliderSetting totemBarShineThickness = new SliderSetting("Толщина блика тотем-бар", 0.5, 0.2, 2.0, 0.05).setVisible(() -> false);
    private final BooleanSetting totemBarCorners = new BooleanSetting("Уголки полосы тотемов", true).setVisible(() -> false);
    private final HudPopup totemBarPopup = new HudPopup("TotemBar", totemBarDrag,
            totemBarBlur, totemBarSize, totemBarAlpha, null,
            totemBarShine, totemBarShineAlpha, totemBarShineThickness, totemBarCorners);

    // TargetHUD
    private final BooleanSetting thBlur = new BooleanSetting("Блюр тх", true).setVisible(() -> false);
    private final SliderSetting thSize = new SliderSetting("Размер тх", 1.0, 0.5, 2.0, 0.05).setVisible(() -> false);
    private final SliderSetting thAlpha = new SliderSetting("Прозрачность тх", 180, 0, 255, 1).setVisible(() -> false);
    private final BooleanSetting thShine = new BooleanSetting("Блик тх", true).setVisible(() -> false);
    private final SliderSetting thShineAlpha = new SliderSetting("Прозрачность блика тх", 30, 0, 100, 1).setVisible(() -> false);
    private final SliderSetting thShineThickness = new SliderSetting("Толщина блика тх", 0.5, 0.2, 2.0, 0.05).setVisible(() -> false);
    private final BooleanSetting thCorners = new BooleanSetting("Уголки таргет худа", true).setVisible(() -> false);
    private final BooleanSetting thShowOnHover = new BooleanSetting("Показывать при наведении", true).setVisible(() -> false);
    private final BooleanSetting thShowItems = new BooleanSetting("Показывать предметы", true).setVisible(() -> false);
    private final HudPopup targetHudPopup = new HudPopup("TargetHUD", targetHUDDrag,
            thBlur, thSize, thAlpha, null,
            thShine, thShineAlpha, thShineThickness, thCorners);
    {
        targetHudPopup.extraRows.add(new PopupRow(PopupKind.TOGGLE, "Показывать при наведении", thShowOnHover, null));
        targetHudPopup.extraRows.add(new PopupRow(PopupKind.TOGGLE, "Показывать предметы", thShowItems, null));
    }

    // Notifications
    private final Draggable notificationsDrag = DragManager.installDrag(this, "Notifications", 250, 250);
    private final BooleanSetting ntBlur = new BooleanSetting("Блюр notif", true).setVisible(() -> false);
    private final SliderSetting ntSize = new SliderSetting("Размер notif", 1.0, 0.5, 2.0, 0.05).setVisible(() -> false);
    private final SliderSetting ntAlpha = new SliderSetting("Прозрачность notif", 180, 0, 255, 1).setVisible(() -> false);
    private final BooleanSetting ntShine = new BooleanSetting("Блик notif", true).setVisible(() -> false);
    private final SliderSetting ntShineAlpha = new SliderSetting("Прозрачность блика notif", 30, 0, 100, 1).setVisible(() -> false);
    private final SliderSetting ntShineThickness = new SliderSetting("Толщина блика notif", 0.5, 0.2, 2.0, 0.05).setVisible(() -> false);
    private final BooleanSetting ntCorners = new BooleanSetting("Уголки уведомлений", true).setVisible(() -> false);
    private final BooleanSetting notifModuleStates = new BooleanSetting("Состояния модулей", true).setVisible(() -> false);
    private final BooleanSetting notifTotem = new BooleanSetting("Снос тотема", true).setVisible(() -> false);
    private final HudPopup notificationsPopup = new HudPopup("Notifications", notificationsDrag,
            ntBlur, ntSize, ntAlpha, null,
            ntShine, ntShineAlpha, ntShineThickness, ntCorners);
    {
        notificationsPopup.extraRows.add(new PopupRow(PopupKind.TOGGLE, "Состояния модулей", notifModuleStates, null));
        notificationsPopup.extraRows.add(new PopupRow(PopupKind.TOGGLE, "Снос тотема", notifTotem, null));
    }

    public boolean isModuleStateNotifEnabled() {
        return elements.isEnabled("Нотификации") && notifModuleStates.getValue();
    }

    public boolean isTotemNotifEnabled() {
        return elements.isEnabled("Нотификации") && notifTotem.getValue();
    }
    private final HudPopup keybindsPopup = new HudPopup("Keybinds", keyBindsDrag,
            keybindsBlur, keybindsSize, keybindsAlpha, null,
            keybindsShine, keybindsShineAlpha, keybindsShineThickness, keybindsCorners);
    private final HudPopup stafflistPopup = new HudPopup("StaffList", staffListDrag,
            stafflistBlur, stafflistSize, stafflistAlpha, null,
            stafflistShine, stafflistShineAlpha, stafflistShineThickness, stafflistCorners);

    private java.util.List<PopupRow> buildRows(HudPopup p) {
        java.util.List<PopupRow> rows = new java.util.ArrayList<>();
        rows.add(new PopupRow(PopupKind.SLIDER, "Размер", null, p.size));
        rows.add(new PopupRow(PopupKind.TOGGLE, "Блюр", p.blur, null));
        rows.add(new PopupRow(PopupKind.SLIDER, "Прозрачность", null, p.alpha));
        if (p.ring != null) rows.add(new PopupRow(PopupKind.TOGGLE, "Кольцо таймер", p.ring, null));
        rows.add(new PopupRow(PopupKind.TOGGLE, "Блик", p.shine, null));
        if (p.shine.getValue()) {
            rows.add(new PopupRow(PopupKind.SLIDER, "Прозрачность блика", null, p.shineAlpha));
            rows.add(new PopupRow(PopupKind.SLIDER, "Толщина блика", null, p.shineThickness));
        }
        rows.add(new PopupRow(PopupKind.TOGGLE, "Уголки", p.corners, null));
        rows.addAll(p.extraRows);
        return rows;
    }

    private double potionsMouseX() {
        return mc.mouse.getX() / mc.getWindow().getScaleFactor();
    }

    private double potionsMouseY() {
        return mc.mouse.getY() / mc.getWindow().getScaleFactor();
    }

    private void renderPotions(DrawContext context) {
        if (mc.player == null) return;

        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        if (!chatOpen) {
            potionsPopup.open = false;
            potionsPopup.draggingSlider = null;
        }

        beginScale(potionsPopup, context);
        renderPotionsNew(context);
        endScale(potionsPopup, context);

        runPopup(potionsPopup, context);
    }

    // Масштабирование и покачивание всего элемента (масштаб — вокруг левого верхнего угла,
    // покачивание «качели» — вокруг центра элемента).
    private void beginScale(HudPopup p, DrawContext context) {
        float size = p.size.getFloatValue();
        float angle = p.drag.getWobbleAngle();
        boolean needScale = Math.abs(size - 1f) > 0.001f;
        boolean needRot = Math.abs(angle) > 0.01f;
        p.transformed = needScale || needRot;
        if (!p.transformed) return;

        float ox = p.drag.getX();
        float oy = p.drag.getY();
        float cx = ox + p.drag.getWidth() / 2f;
        float cy = oy + p.drag.getHeight() / 2f;

        if (needScale) {
            zov.alphadlc.util.render.renderers.IRenderer.DEFAULT_MATRIX
                    .identity().translate(ox, oy, 0f).scale(size, size, 1f).translate(-ox, -oy, 0f);
        }

        context.getMatrices().push();
        if (needScale) {
            context.getMatrices().translate(ox, oy, 0f);
            context.getMatrices().scale(size, size, 1f);
            context.getMatrices().translate(-ox, -oy, 0f);
        }
        if (needRot) {
            context.getMatrices().translate(cx, cy, 0f);
            context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(angle));
            context.getMatrices().translate(-cx, -cy, 0f);
        }
    }

    private void endScale(HudPopup p, DrawContext context) {
        if (!p.transformed) return;
        context.getMatrices().pop();
        zov.alphadlc.util.render.renderers.IRenderer.DEFAULT_MATRIX.identity();
        float size = p.size.getFloatValue();
        if (Math.abs(size - 1f) > 0.001f) {
            p.drag.setWidth(p.drag.getWidth() * size);
            p.drag.setHeight(p.drag.getHeight() * size);
        }
    }

    private void runPopup(HudPopup p, DrawContext context) {
        p.anim.run(p.open ? 1 : 0);
        if (p.anim.getValue() > 0.01f) {
            renderSettingsPopup(p, context);
        }
    }

    // Единый фон элемента: блюр на полной непрозрачности + тёмная подложка (управляется прозрачностью)
    private void drawElementBackground(HudPopup p, float x, float y, float w, float h, float radius, float alphaFactor) {
        float clampFactor = MathHelper.clamp(alphaFactor, 0f, 1f);
        int alpha = (int) (p.alpha.getIntValue() * clampFactor);
        alpha = Math.min(255, Math.max(0, alpha));
        if (p.blur.getValue()) {
            int blurAlpha = Math.min(255, Math.max(0, (int) (255 * clampFactor)));
            DrawUtil.drawRoundBlur(x, y, w, h, radius, ColorProvider.rgba(200, 200, 200, blurAlpha), 12);
            DrawUtil.drawRound(x, y, w, h, radius, ColorProvider.setAlpha(ColorProvider.getColorInterfaceBg(), alpha));
        } else {
            DrawUtil.drawRound(x, y, w, h, radius, ColorProvider.setAlpha(ColorProvider.getColorInterfaceBg(), alpha));
        }
        drawElementCorners(p, x, y, w, h, radius, clampFactor);
    }

    // Уголки: каждый угол — четверть кольца, средняя линия которого совпадает с контуром
    // угла элемента. За счёт этого кольцо ложится ПОВЕРХ границы — половина толщины внутри
    // элемента, половина снаружи (обхватывает край).
    // Прозрачность настройки элемента на уголки НЕ влияет — они всегда непрозрачные
    // (учитывается только фактор появления/исчезновения, чтобы не оставаться при скрытии).
    private void drawElementCorners(HudPopup p, float x, float y, float w, float h, float radius, float fadeFactor) {
        if (!p.corners.getValue()) return;
        int cornerAlpha = Math.min(255, Math.max(0, (int) (255 * fadeFactor)));
        int c = ColorProvider.setAlpha(ColorProvider.getColorClient(), cornerAlpha);

        float th = 1.6f;                       // толщина дуги
        float rc = Math.max(radius, 2f);       // радиус = скругление угла элемента
        float outer = rc + th / 2f;            // внешний радиус: середина кольца на контуре
        // Центр дуги — центр скругления угла элемента; кольцо занимает [rc - th/2, rc + th/2],
        // поэтому оно лежит ровно на границе, наполовину внутри и наполовину снаружи.
        // Угол: 0°=вправо, 90°=вниз, 180°=влево, 270°=вверх.
        DrawUtil.drawRingArc(x + rc,     y + rc,     outer, th, 180f, 270f, c); // верх-лево
        DrawUtil.drawRingArc(x + w - rc, y + rc,     outer, th, 270f, 360f, c); // верх-право
        DrawUtil.drawRingArc(x + w - rc, y + h - rc, outer, th, 0f,   90f,  c); // низ-право
        DrawUtil.drawRingArc(x + rc,     y + h - rc, outer, th, 90f,  180f, c); // низ-лево
    }

    private void drawElementShine(HudPopup p, DrawContext context, float x, float y, float w, float h, float radius) {
        if (!p.shine.getValue()) return;
        zov.alphadlc.util.render.renderers.impl.HudShine.render(
                context.getMatrices(), x, y, w, h, radius,
                p.shineThickness.getFloatValue(), 1f, p.shineAlpha.getFloatValue() / 100f,
                0f);
    }
    private void renderPotionsNew(DrawContext context) {
        if (mc.player == null) return;

        float posX = potionsDrag.getX();
        float posY = potionsDrag.getY();

        float headerHeight = 14f;
        float itemHeight = 12f;
        float minWidth = 52f;
        float padX = 5f;
        float padY = 2f;

        // Сортировка перенесена в updatePotions() (по тику, только при изменении набора),
        // чтобы не пересортировывать CopyOnWriteArrayList каждый кадр. Порядок идентичен.

        boolean isFound = false;
        for (PotionItem item : potionItems) {
            item.animation.run(item.active ? 1 : 0);
            item.rowAnim.run(item.active ? 1 : 0);
            if (item.animation.getValue() > 0.001f) isFound = true;
        }

        if (!isFound && !(mc.currentScreen instanceof ChatScreen)) alpha3.run(0);
        else alpha3.run(1);

        float globalAlpha = (float) alpha3.getValue();
        if (globalAlpha <= 0.05f) return;

        int headerAlpha = (int) Math.min(255, Math.max(0, 255 * globalAlpha));

        // Update random effect for example display when chat is open
        boolean showExample = (mc.currentScreen instanceof ChatScreen) && !isFound;
        if (showExample) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastEffectChange > 2000) {
                if (cachedAllEffects == null) {
                    cachedAllEffects = new java.util.ArrayList<>();
                    net.minecraft.registry.Registries.STATUS_EFFECT.streamEntries().forEach(cachedAllEffects::add);
                }
                if (!cachedAllEffects.isEmpty()) {
                    currentRandomEffect = cachedAllEffects.get(RANDOM.nextInt(cachedAllEffects.size()));
                    lastEffectChange = currentTime;
                }
            }
        }

        float maxNameBoxW = minWidth;
        float maxTimeBoxW = 0f;
        float contentHeight = 0f;
        for (PotionItem item : potionItems) {
            float animVal = (float) item.animation.getValue();
            if (animVal > 0.001f) {
                contentHeight += itemHeight * (float) item.rowAnim.getValue();
                String lvlStr = item.amplifier >= 1 ? "LVL " + (item.amplifier + 1) : "";
                float nameW = Fonts.SFMEDIUM.get().getWidth(item.name, 7f);
                float lvlW = lvlStr.isEmpty() ? 0f : Fonts.SFMEDIUM.get().getWidth(lvlStr, 6f);
                float nw = nameW + lvlW + 8f + 10f + 10f;
                if (nw > maxNameBoxW) maxNameBoxW = nw;

                int seconds = item.durationTicks / 20;
                int minutes = seconds / 60;
                int sec = seconds % 60;
                String timeStr = String.format("%d:%02d", minutes, sec);
                float tw = Fonts.SFMEDIUM.get().getWidth(timeStr, 6.75f) + 10f
                        + (potionsRing.getValue() ? POTION_RING_DIAMETER + POTION_RING_GAP : 0f);
                if (tw > maxTimeBoxW) maxTimeBoxW = tw;
            }
        }

        // Account for Example row when chat is open and no potions
        if (showExample) {
            float exNameW = Fonts.SFMEDIUM.get().getWidth("Example", 7f);
            if (exNameW + 8f + 10f + 10f > maxNameBoxW) maxNameBoxW = exNameW + 8f + 10f + 10f;
            float exTimeW = Fonts.SFMEDIUM.get().getWidth("**:**", 6.75f) + 10f
                    + (potionsRing.getValue() ? POTION_RING_DIAMETER + POTION_RING_GAP : 0f);
            if (exTimeW > maxTimeBoxW) maxTimeBoxW = exTimeW;
            contentHeight = itemHeight;
        }

        float rawWidth = maxNameBoxW + maxTimeBoxW + padX * 2;
        potionsPopup.panelWidth.run(rawWidth);
        float totalRowWidth = (float) potionsPopup.panelWidth.getValue();
        if (totalRowWidth < 20f) totalRowWidth = rawWidth;
        float totalHeight = headerHeight + contentHeight + padY * 2;

        // Фон с учётом настроек элемента (блюр + прозрачность из правого клика)
        drawElementBackground(potionsPopup, posX, posY, totalRowWidth, totalHeight, 3f, globalAlpha);

        // Блик (можно отключить/настроить в настройках элемента)
        drawElementShine(potionsPopup, context, posX, posY, totalRowWidth, totalHeight, 3f);

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Potions", posX + padX + 4f, posY + padY + 2.5f, ColorProvider.rgba(255, 255, 255, headerAlpha), 8f);
        DrawUtil.drawText(Fonts.ALPHADLC.get(), "f", posX + totalRowWidth - padX - 11f, posY + padY + 3f, ColorProvider.setAlpha(ColorProvider.getColorIcons(), headerAlpha), 9);

        float curY = posY + headerHeight + padY;

        for (PotionItem item : potionItems) {
            float animVal = (float) item.animation.getValue();
            if (animVal <= 0.001f) continue;

            float rowAnimVal = (float) item.rowAnim.getValue();
            if (rowAnimVal <= 0.001f) continue;

            int seconds = item.durationTicks / 20;
            int minutes = seconds / 60;
            int sec = seconds % 60;
            String timeStr = String.format("%d:%02d", minutes, sec);
            String nameStr = item.name;

            boolean isHarmful = !item.effect.value().isBeneficial();
            String effectId = net.minecraft.registry.Registries.STATUS_EFFECT.getId(item.effect.value()).getPath();
            boolean isNightVision = effectId.equals("night_vision");

            // Blinking animation for expiring potions (except night vision)
            int textAlpha = 255;
            if (item.durationTicks <= 200 && item.durationTicks > 0 && !isNightVision) {
                double output = 0.5 + 0.5 * Math.cos(2 * Math.PI * (System.currentTimeMillis() % 700) / 700.0);
                textAlpha = (int) (100 + (155 * output));
            } else if (item.durationTicks == 0) {
                textAlpha = 0;
            }

            int itemAlpha = (int) Math.min(255, Math.max(0, textAlpha * animVal * globalAlpha));
            if (itemAlpha < 5) continue;

            float nameW = Fonts.SFMEDIUM.get().getWidth(nameStr, 7f);
            String lvlStr = item.amplifier >= 1 ? "LVL " + (item.amplifier + 1) : "";
            float lvlW = lvlStr.isEmpty() ? 0f : Fonts.SFMEDIUM.get().getWidth(lvlStr, 6f);
            float timeW = Fonts.SFMEDIUM.get().getWidth(timeStr, 6.75f);
            float timeBoxW = timeW + 10f;

            // Apply row scale animation
            float rowHeight = itemHeight * rowAnimVal;
            context.getMatrices().push();
            context.getMatrices().translate(posX + totalRowWidth / 2f, curY + rowHeight / 2f, 0);
            context.getMatrices().scale(rowAnimVal, rowAnimVal, rowAnimVal);
            context.getMatrices().translate(-(posX + totalRowWidth / 2f), -(curY + rowHeight / 2f), 0);

            float potionMid = textMidY(curY + 2.5f, 7f);
            net.minecraft.client.texture.Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(item.effect);
            if (sprite != null) {
                float iconSize = 8f;
                float iconX = posX + padX + 2f;
                float iconY = potionMid - iconSize / 2f;
                int color = (itemAlpha << 24) | 0xFFFFFF;
                RenderSystem.setShaderColor(1f, 1f, 1f, itemAlpha / 255f);
                context.drawSpriteStretched(net.minecraft.client.render.RenderLayer::getGuiTextured, sprite, (int) iconX, (int) iconY, (int) iconSize, (int) iconSize, color);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }

            float dotX = posX + padX + 11f;
            float dotY = potionMid - 1.5f;
            DrawUtil.drawRound(dotX, dotY, 3f, 3f, 1.5f, ColorProvider.rgba(120, 120, 120, itemAlpha));

            int nameColor = isHarmful ? ColorProvider.rgba(255, 80, 80, itemAlpha) : ColorProvider.rgba(255, 255, 255, itemAlpha);
            float textX = posX + padX + 18f;
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), nameStr, textX, curY + 2.5f, nameColor, 7f);

            if (!lvlStr.isEmpty()) {
                float lvlX = textX + nameW + 6f;
                DrawUtil.drawText(Fonts.SFMEDIUM.get(), lvlStr, lvlX, curY + 3.5f, ColorProvider.rgba(160, 160, 160, itemAlpha), 6f);
            }

            // Timer with per-digit animation
            float timerX = posX + totalRowWidth - padX - timeBoxW;

            // Кольцо-таймер слева от времени: показывает остаток длительности эффекта
            if (potionsRing.getValue()) {
                float ringRadius = POTION_RING_DIAMETER / 2f;
                float ringCX = timerX - POTION_RING_GAP - ringRadius;
                float ringCY = curY + itemHeight / 2f;
                float progress = 1f;
                if (item.maxDurationTicks > 0 && item.durationTicks >= 0) {
                    progress = MathHelper.clamp((float) item.durationTicks / (float) item.maxDurationTicks, 0f, 1f);
                }
                DrawUtil.drawRingArc(ringCX, ringCY, ringRadius, POTION_RING_THICKNESS, -90f, 270f, ColorProvider.rgba(80, 80, 80, itemAlpha));
                if (progress > 0f) {
                    DrawUtil.drawRingArc(ringCX, ringCY, ringRadius, POTION_RING_THICKNESS, -90f, -90f + 360f * progress,
                            ColorProvider.setAlpha(ColorProvider.getColorClient(), itemAlpha));
                }
            }

            String timerKey = "potions_duration_" + effectId + "_" + item.amplifier;
            zov.alphadlc.util.render.timer.TimerTextAnimator.draw(
                Fonts.SFMEDIUM.get(),
                timerKey,
                timeStr,
                timerX + (timeBoxW - timeW) / 2f + 1f,
                curY + 2.5f,
                ColorProvider.rgba(255, 255, 255, itemAlpha),
                6.75f
            );

            context.getMatrices().pop();

            curY += rowHeight;
        }

        // Draw Example row when chat is open and no potions
        if (showExample && currentRandomEffect != null) {
            int exampleAlpha = headerAlpha;
            String nameStr = "Example";
            String timeStr = "**:**";
            
            float nameW = Fonts.SFMEDIUM.get().getWidth(nameStr, 7f);
            float timeW = Fonts.SFMEDIUM.get().getWidth(timeStr, 6.75f);
            float timeBoxW = timeW + 10f;

            // Draw icon
            float exMid = textMidY(curY + 2.5f, 7f);
            net.minecraft.client.texture.Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(currentRandomEffect);
            if (sprite != null) {
                float iconSize = 8f;
                float iconX = posX + padX + 2f;
                float iconY = exMid - iconSize / 2f;
                int color = (exampleAlpha << 24) | 0xFFFFFF;
                RenderSystem.setShaderColor(1f, 1f, 1f, exampleAlpha / 255f);
                context.drawSpriteStretched(net.minecraft.client.render.RenderLayer::getGuiTextured, sprite, (int) iconX, (int) iconY, (int) iconSize, (int) iconSize, color);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }

            // Draw dot
            float dotX = posX + padX + 11f;
            float dotY = exMid - 1.5f;
            DrawUtil.drawRound(dotX, dotY, 3f, 3f, 1.5f, ColorProvider.rgba(120, 120, 120, exampleAlpha));

            // Draw name
            float textX = posX + padX + 18f;
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), nameStr, textX, curY + 2.5f, ColorProvider.rgba(255, 255, 255, exampleAlpha), 7f);

            // Draw timer
            float timerX = posX + totalRowWidth - padX - timeBoxW;

            if (potionsRing.getValue()) {
                float ringRadius = POTION_RING_DIAMETER / 2f;
                float ringCX = timerX - POTION_RING_GAP - ringRadius;
                float ringCY = curY + itemHeight / 2f;
                DrawUtil.drawRingArc(ringCX, ringCY, ringRadius, POTION_RING_THICKNESS, -90f, 270f, ColorProvider.rgba(80, 80, 80, exampleAlpha));
                DrawUtil.drawRingArc(ringCX, ringCY, ringRadius, POTION_RING_THICKNESS, -90f, 180f,
                        ColorProvider.setAlpha(ColorProvider.getColorClient(), exampleAlpha));
            }

            DrawUtil.drawText(Fonts.SFMEDIUM.get(), timeStr, timerX + (timeBoxW - timeW) / 2f + 1f, curY + 2.5f, ColorProvider.rgba(255, 255, 255, exampleAlpha), 6.75f);
        }

        potionsDrag.setWidth(totalRowWidth);
        potionsDrag.setHeight(totalHeight);
    }

    private static final float POPUP_HEADER_H = 15f;
    private static final float POPUP_TOGGLE_H = 14f;
    private static final float POPUP_SLIDER_H = 19f;
    private static final float POPUP_PAD = 4f;
    private static final float POPUP_TRACK_INSET = 6f;

    private void renderSettingsPopup(HudPopup p, DrawContext context) {
        java.util.List<PopupRow> rows = buildRows(p);

        // Ширина окна под самую длинную строку
        float w = 96f;
        for (PopupRow r : rows) {
            float lw = Fonts.SFMEDIUM.get().getWidth(r.label, 6.75f);
            float need = lw + 12f + (r.kind == PopupKind.TOGGLE ? 22f : 34f);
            if (need > w) w = need;
        }

        float totalH = POPUP_HEADER_H + POPUP_PAD;
        for (PopupRow r : rows) {
            totalH += (r.kind == PopupKind.TOGGLE ? POPUP_TOGGLE_H : POPUP_SLIDER_H);
        }

        float ex = p.drag.getX();
        float ey = p.drag.getY();
        float ew = p.drag.getWidth();

        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();

        // Обычно окно открывается справа от элемента, но для кастомного хотбара — слева
        float x = p.title.equals("CustomHotbar") ? ex - w - 4f : ex + ew + 4f;
        float y = ey;

        x = MathHelper.clamp(x, 2f, Math.max(2f, screenW - w - 2f));
        y = MathHelper.clamp(y, 2f, Math.max(2f, screenH - totalH - 2f));

        p.px = x;
        p.py = y;
        p.pw = w;
        p.ph = totalH;

        float anim = (float) p.anim.getValue();
        int a = (int) Math.min(255, Math.max(0, 255 * anim));

        drawBackground(x, y, w, totalH, 4f, a);

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Настройки", x + 6f, y + 4.5f, ColorProvider.rgba(255, 255, 255, a), 7.5f);
        DrawUtil.drawRound(x + 5f, y + POPUP_HEADER_H - 1.5f, w - 10f, 0.5f, 0f, ColorProvider.rgba(120, 120, 120, a));

        float cy = y + POPUP_HEADER_H;
        for (PopupRow r : rows) {
            r.x = x;
            r.y = cy;
            r.w = w;
            r.h = (r.kind == PopupKind.TOGGLE ? POPUP_TOGGLE_H : POPUP_SLIDER_H);

            if (r.kind == PopupKind.TOGGLE) {
                drawPotionsToggleRow(r.label, r.bool, r.x, r.y, r.w, r.h, a);
            } else {
                drawPotionsSliderRow(r.label, r.slider, r.x, r.y, r.w, r.h, a);
            }
            cy += r.h;
        }

        p.rendered.clear();
        p.rendered.addAll(rows);

        if (p.draggingSlider != null) {
            double val = (potionsMouseX() - p.trackX) / p.trackW
                    * (p.draggingSlider.getMax() - p.draggingSlider.getMin()) + p.draggingSlider.getMin();
            double stepped = Math.round(val / p.draggingSlider.getStep()) * p.draggingSlider.getStep();
            p.draggingSlider.setValue(stepped);
        }
    }

    private void drawPotionsToggleRow(String label, BooleanSetting setting, float x, float y, float w, float h, int a) {
        // Двигаем анимацию тумблера здесь: некоторые настройки (напр. «Уголки») не являются
        // прямыми полями модуля и не обновляются в ModuleStorage.
        setting.getAnimation().run(setting.getValue());

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), label, x + 6f, y + (h / 2f) - 3.25f, ColorProvider.rgba(255, 255, 255, a), 6.75f);

        float toggleW = 15f;
        float toggleH = 8f;
        float toggleX = x + w - toggleW - 6f;
        float toggleY = y + (h - toggleH) / 2f;

        float tAnim = (float) setting.getAnimation().getValue();
        int inactive = ColorProvider.setAlpha(ColorProvider.getColorInactiveIndicator(), a);
        int active = ColorProvider.setAlpha(ColorProvider.getColorIndicator(), a);
        int bg = ColorProvider.interpolateColor(inactive, active, tAnim);
        DrawUtil.drawRound(toggleX, toggleY, toggleW, toggleH, toggleH / 2f, bg);

        float knob = toggleH - 1f;
        float knobMinX = toggleX + 0.5f;
        float knobMaxX = toggleX + toggleW - knob - 0.5f;
        float knobX = knobMinX + (knobMaxX - knobMinX) * tAnim;
        DrawUtil.drawCircle(knobX + knob / 2f, toggleY + 0.5f + knob / 2f, knob / 2f, ColorProvider.setAlpha(ColorProvider.getColorSliderCircle(), a));
    }

    private String formatSliderValue(SliderSetting s) {
        if (s.getStep() < 1) {
            return java.lang.String.format(java.util.Locale.US, "%.2f", s.getValue());
        }
        return String.valueOf(s.getIntValue());
    }

    private void drawPotionsSliderRow(String label, SliderSetting setting, float x, float y, float w, float h, int a) {
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), label, x + 6f, y + 2.5f, ColorProvider.rgba(255, 255, 255, a), 6.5f);

        String valStr = formatSliderValue(setting);
        float valW = Fonts.SFMEDIUM.get().getWidth(valStr, 6.5f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), valStr, x + w - 6f - valW, y + 2.5f, ColorProvider.setAlpha(ColorProvider.getColorInactiveText(), a), 6.5f);

        float trackX = x + POPUP_TRACK_INSET;
        float trackW = w - POPUP_TRACK_INSET * 2f;
        float trackY = y + h - 5.5f;

        DrawUtil.drawRound(trackX, trackY, trackW, 3f, 1f, ColorProvider.setAlpha(ColorProvider.getColorSliderWindow(), a));

        float fill = (float) (trackW * (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin()));
        fill = MathHelper.clamp(fill, 0f, trackW);
        DrawUtil.drawRound(trackX, trackY, fill, 3f, 1f, ColorProvider.setAlpha(ColorProvider.getColorSlider(), a));

        float circleX = trackX + fill;
        DrawUtil.drawRound(circleX - 2.5f, trackY - 1f, 5f, 5f, 1.75f, ColorProvider.setAlpha(ColorProvider.getColorSliderCircle(), a));
    }

    public boolean isPotionsActive() {
        return isEnabled() && elements.isEnabled("Бафы");
    }

    public boolean isCustomHotbarActive() {
        return isEnabled() && elements.isEnabled("Кастомный хотбар");
    }

    // Роутинг кликов чата на настройки HUD-элементов
    public boolean handlePotionsClick(double mouseX, double mouseY, int button) {
        if (!isEnabled() || !(mc.currentScreen instanceof ChatScreen)) return false;
        if (elements.isEnabled("Бафы") && handleElementClick(potionsPopup, mouseX, mouseY, button)) return true;
        if (elements.isEnabled("КулДауны") && handleElementClick(cooldownsPopup, mouseX, mouseY, button)) return true;
        if (elements.isEnabled("ServerHelper") && handleElementClick(serverHelperPopup, mouseX, mouseY, button)) return true;
        if (elements.isEnabled("Привязанные модули") && handleElementClick(keybindsPopup, mouseX, mouseY, button)) return true;
        if (elements.isEnabled("Активные модераторы") && handleElementClick(stafflistPopup, mouseX, mouseY, button)) return true;
        if (elements.isEnabled("Ватермарка") && handleElementClick(watermarkPopup, mouseX, mouseY, button)) return true;
        if (elements.isEnabled("Инфо") && handleElementClick(infoPopup, mouseX, mouseY, button)) return true;
        if (elements.isEnabled("Активный таргет") && handleElementClick(targetHudPopup, mouseX, mouseY, button)) return true;
        if (elements.isEnabled("Нотификации") && handleElementClick(notificationsPopup, mouseX, mouseY, button)) return true;
        if (elements.isEnabled("Кастомный хотбар") && handleElementClick(hotbarPopup, mouseX, mouseY, button)) return true;
        if (elements.isEnabled("Броня") && handleElementClick(armourPopup, mouseX, mouseY, button)) return true;
        if (elements.isEnabled("Полоса тотемов") && handleElementClick(totemBarPopup, mouseX, mouseY, button)) return true;
        return false;
    }

    private boolean handleElementClick(HudPopup p, double mouseX, double mouseY, int button) {
        if (p.open && p.anim.getValue() > 0.5f) {
            if (button == 0) {
                for (PopupRow r : p.rendered) {
                    if (!HoverUtil.isHovered(mouseX, mouseY, r.x, r.y, r.w, r.h)) continue;
                    if (r.kind == PopupKind.TOGGLE) {
                        r.bool.toggle();
                    } else {
                        p.draggingSlider = r.slider;
                        p.trackX = r.x + POPUP_TRACK_INSET;
                        p.trackW = r.w - POPUP_TRACK_INSET * 2f;
                        double val = (mouseX - p.trackX) / p.trackW
                                * (r.slider.getMax() - r.slider.getMin()) + r.slider.getMin();
                        double stepped = Math.round(val / r.slider.getStep()) * r.slider.getStep();
                        r.slider.setValue(stepped);
                    }
                    return true;
                }
            }
            if (HoverUtil.isHovered(mouseX, mouseY, p.px, p.py, p.pw, p.ph)) {
                return true;
            }
        }

        if (button == 1 && p.drag.isHovering()) {
            p.open = !p.open;
            return true;
        }

        // Клик мимо элемента и окна закрывает настройки (клик по самому элементу оставляет их открытыми для перетаскивания)
        if (p.open && !p.drag.isHovering()) {
            p.open = false;
        }
        return false;
    }

    public void handlePotionsRelease(int button) {
        if (button == 0) {
            potionsPopup.draggingSlider = null;
            cooldownsPopup.draggingSlider = null;
            serverHelperPopup.draggingSlider = null;
            keybindsPopup.draggingSlider = null;
            stafflistPopup.draggingSlider = null;
            watermarkPopup.draggingSlider = null;
            infoPopup.draggingSlider = null;
            targetHudPopup.draggingSlider = null;
            notificationsPopup.draggingSlider = null;
            hotbarPopup.draggingSlider = null;
            armourPopup.draggingSlider = null;
            totemBarPopup.draggingSlider = null;
        }
    }

    public void update() {
        for (Staff staff : staffPlayers) {
            staff.isOnServer = false;
        }

        for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getPlayerList()) {
            String name = playerListEntry.getProfile().getName().replaceAll("[\\[\\]]", "");
            PlayerListEntry info = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(name);
            boolean vanish = info == null;
            boolean isGM3 = info != null && info.getGameMode() == GameMode.SPECTATOR;

            boolean matchesPrefix = prefixMatches.matcher(playerListEntry.getDisplayName() != null ? playerListEntry.getDisplayName().getString().toLowerCase(Locale.ROOT) : "").matches();
            boolean isValidName = namePattern.matcher(name).matches();
            boolean notSelf = !name.equals(MinecraftClient.getInstance().player.getName().getString());

            if ((isValidName && notSelf && matchesPrefix) || (isValidName && notSelf && vanish) || StaffManager.isStaff(name)) {
                if (StaffManager.isStaff(name)) {
                    String[] names = new String[]{"auction", "exp_smith", "shop_balls", "shop_grief", "free", "shop_kits", "siege", "rwplus", "bossfight", "guide", "shop_smith", "shop_spawners", "colliseum", "battlepass", "buyer", "huckster", "buff_brewer", "killer", "shop_mage"};
                    boolean contains = false;
                    if (MinecraftClient.getInstance().getCurrentServerEntry() != null && MinecraftClient.getInstance().getCurrentServerEntry().address != null && (MinecraftClient.getInstance().getCurrentServerEntry().address.contains("mc.rwdonat.pw") || MinecraftClient.getInstance().getCurrentServerEntry().address.contains("mc.cakeworld.pw"))) {
                        for (int i = 0; i < Arrays.stream(names).count(); i++) {
                            if (name.contains(names[i])) {
                                contains = true;
                                break;
                            }
                        }
                    }
                    if (contains) continue;
                }
                Optional<Staff> existingStaff = staffPlayers.stream().filter(s -> s.name.equals(name)).findFirst();

                Status status = vanish ? Status.VANISHED : (isGM3 ? Status.VANISHED : Status.NONE);

                if (existingStaff.isPresent()) {
                    Staff s = existingStaff.get();
                    s.isOnServer = true;
                    s.status = status;
                } else {
                    String[] names = new String[]{"auction", "exp_smith", "shop_balls", "shop_grief", "free", "shop_kits", "siege", "rwplus", "bossfight", "guide", "shop_smith", "shop_spawners", "colliseum", "battlepass", "buyer", "huckster", "buff_brewer", "killer", "shop_mage"};
                    boolean contains = false;
                    if (MinecraftClient.getInstance().getCurrentServerEntry() != null && MinecraftClient.getInstance().getCurrentServerEntry().address != null && (MinecraftClient.getInstance().getCurrentServerEntry().address.contains("mc.rwdonat.pw") || MinecraftClient.getInstance().getCurrentServerEntry().address.contains("mc.cakeworld.pw"))) {
                        for (int i = 0; i < Arrays.stream(names).count(); i++) {
                            if (name.contains(names[i])) {
                                contains = true;
                            }
                        }
                    }
                    if (!contains) {
                        Text originalPrefix = playerListEntry.getDisplayName();
                        Text prefix = originalPrefix;
                        if (prefix != null) {
                            prefix = ReplaceUtil.replaceSymbols(prefix);
                            String fullString = prefix.getString();
                            int nickIndex = fullString.indexOf(name);
                            if (nickIndex != -1) {
                                int endIndex = nickIndex + name.length();
                                if (endIndex < fullString.length()) {
                                    net.minecraft.text.MutableText newText = Text.empty();
                                    int currentLength = 0;
                                    net.minecraft.text.MutableText baseCopy = prefix.copy();
                                    baseCopy.getSiblings().clear();
                                    String mainContent = baseCopy.getString();

                                    if (!mainContent.isEmpty() && currentLength < endIndex) {
                                        int takeLength = Math.min(mainContent.length(), endIndex - currentLength);
                                        newText.append(Text.literal(mainContent.substring(0, takeLength)).setStyle(prefix.getStyle()));
                                        currentLength += takeLength;
                                    }

                                    for (Text sibling : prefix.getSiblings()) {
                                        if (currentLength >= endIndex) break;
                                        net.minecraft.text.MutableText siblingCopy = sibling.copy();
                                        siblingCopy.getSiblings().clear();
                                        String siblingContent = siblingCopy.getString();

                                        int takeLength = Math.min(siblingContent.length(), endIndex - currentLength);
                                        if (takeLength > 0) {
                                            newText.append(Text.literal(siblingContent.substring(0, takeLength)).setStyle(sibling.getStyle()));
                                            currentLength += takeLength;
                                        }
                                    }

                                    prefix = newText;
                                }
                            }
                        }
                        Staff staff = new Staff(prefix == null ? Text.of(playerListEntry.getProfile().getName()) : prefix, name, vanish || isGM3, status);
                        staff.isOnServer = true;
                        staffPlayers.add(staff);
                    }
                }
            }
        }

        staffPlayers.removeIf(staff -> !staff.isOnServer && staff.animation.getValue() == 0);
    }

    public enum Status {
        NONE("", -1),
        VANISHED("SPEC", ColorProvider.rgba(229, 0, 63, 255));

        public final String string;
        public final int color;

        Status(String string, int color) {
            this.string = string;
            this.color = color;
        }
    }

    public static class Staff {
        Text prefix;
        public String name;
        boolean isSpec;
        Status status;
        boolean isOnServer;
        Animation animation;
        long mills;

        public Staff(Text prefix, String name, boolean isSpec, Status status) {
            this.prefix = prefix;
            this.name = name;
            this.isSpec = isSpec;
            this.status = status;
            animation = new Animation(Easing.EXPO_OUT, 233);
            mills = System.currentTimeMillis();
        }
    }

    public int getPing(PlayerEntity entity) {
        PlayerListEntry list = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        return list != null ? list.getLatency() : 0;
    }

    private void renderWatermark(DrawContext context) {
        if (mc.player == null) return;

        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        if (!chatOpen) {
            watermarkPopup.open = false;
            watermarkPopup.draggingSlider = null;
        }

        beginScale(watermarkPopup, context);
        renderWatermarkNew(context);
        endScale(watermarkPopup, context);
        runPopup(watermarkPopup, context);
    }

    // Info — отдельный HUD-элемент (нижняя строка)
    private void renderInfo(DrawContext context) {
        if (mc.player == null) return;

        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        if (!chatOpen) {
            infoPopup.open = false;
            infoPopup.draggingSlider = null;
        }

        beginScale(infoPopup, context);
        renderInfoNew(context);
        endScale(infoPopup, context);
        runPopup(infoPopup, context);
    }

    private void renderWatermarkNew(DrawContext context) {
        Counter.updateFPS();

        String userText = "User";
        String fpsValue = Counter.getCurrentFPS() + " Fps";
        String pingValue = Server.getPing(mc.player) + " Ping";

        float x = watermarkDrag.getX();
        float y = watermarkDrag.getY();
        float height = 16.5f;

        int iconColor = ColorProvider.getColorIcons();
        int whiteColor = -1;
        int dotColor = ColorProvider.rgba(255, 255, 255, 60);

        String pingIcon = "\u0051";
        String logoIcon = "\u0034";  // иконка из icons2.png (U+34)

        // ---- Единый бокс верхней строки: Logo · AlphaDLC · User · FPS · Ping ----
        String title = "LEAK BY NEDO t.me/LegitDLC";
        float logoIconW = Fonts.ICONS2.get().getWidth(logoIcon, 7f);
        float titleW = Fonts.SFMEDIUM.get().getWidth(title, 7f);

        float userIconW = Fonts.ALPHADLC.get().getWidth("b", 7f);
        float fpsIconW = Fonts.ALPHADLC.get().getWidth("n", 7f);
        float pingIconW = Fonts.ICONS_NURIK.get().getWidth(pingIcon, 7f);
        float userW = Fonts.SFMEDIUM.get().getWidth(userText, 7f);
        float fpsW = Fonts.SFMEDIUM.get().getWidth(fpsValue, 7f);
        float pingW = Fonts.SFMEDIUM.get().getWidth(pingValue, 7f);

        float iconGap = 1.5f;   // иконка ближе к тексту
        float sepGap = 3.5f;    // отступ вокруг точки-разделителя
        float dotSize = 3f;     // точка-разделитель
        float sep = sepGap + dotSize + sepGap;

        float boxWidth = 5f
                + logoIconW + iconGap + titleW
                + sep + userIconW + iconGap + userW
                + sep + fpsIconW + iconGap + fpsW
                + sep + pingIconW + iconGap + pingW
                + 5f;

        drawElementBackground(watermarkPopup, x, y, boxWidth, height, 3f, 1f);
        drawElementShine(watermarkPopup, context, x, y, boxWidth, height, 3f);

        float dotY = y + (height - dotSize) / 2f;
        float cx = x + 5f;
        DrawUtil.drawText(Fonts.ICONS2.get(), logoIcon, cx - 0.5f, y + 3.5f, iconColor, 7f);
        cx += logoIconW + iconGap;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), title, cx, y + 4f, whiteColor, 7f);
        cx += titleW + sepGap;
        DrawUtil.drawRound(cx, dotY, dotSize, dotSize, dotSize / 2f, dotColor);
        cx += dotSize + sepGap;

        DrawUtil.drawText(Fonts.ALPHADLC.get(), "b", cx, y + 4.75f, iconColor, 7f);
        cx += userIconW + iconGap;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), userText, cx, y + 4f, whiteColor, 7f);
        cx += userW + sepGap;
        DrawUtil.drawRound(cx, dotY, dotSize, dotSize, dotSize / 2f, dotColor);
        cx += dotSize + sepGap;

        DrawUtil.drawText(Fonts.ALPHADLC.get(), "n", cx, y + 4.75f, iconColor, 7f);
        cx += fpsIconW + iconGap;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), fpsValue, cx, y + 4f, whiteColor, 7f);
        cx += fpsW + sepGap;
        DrawUtil.drawRound(cx, dotY, dotSize, dotSize, dotSize / 2f, dotColor);
        cx += dotSize + sepGap;

        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), pingIcon, cx, y + 4.75f, iconColor, 7f);
        cx += pingIconW + iconGap;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), pingValue, cx, y + 4f, whiteColor, 7f);

        watermarkDrag.setWidth(boxWidth);
        watermarkDrag.setHeight(height);
    }

    // Нижняя строка ("Info") — отдельный HUD-элемент: координаты · TPS · BPS (с точками-разделителями)
    private void renderInfoNew(DrawContext context) {
        boolean showCoords = wmCoords.getValue();
        boolean showTps = wmTps.getValue();
        boolean showBps = wmBps.getValue();

        float x = infoDrag.getX();
        float y = infoDrag.getY();
        float height = 16.5f;

        int iconColor = ColorProvider.getColorIcons();
        int whiteColor = -1;
        int dotColor = ColorProvider.rgba(255, 255, 255, 60);

        var msdf = Fonts.SFMEDIUM.get();
        var icons = Fonts.ICONS_NURIK.get();

        float iconGap = 1.5f;
        float sepGap = 3.5f;
        float dotSize = 3f;
        float sep = sepGap + dotSize + sepGap;
        float leftPad = 5f, rightPad = 5f;

        // Пусто: если чат открыт — рисуем плейсхолдер (чтобы можно было открыть настройки), иначе скрываем
        if (!showCoords && !showTps && !showBps) {
            if (mc.currentScreen instanceof ChatScreen) {
                String ph = "Info";
                float w = leftPad + msdf.getWidth(ph, 7f) + rightPad;
                drawElementBackground(infoPopup, x, y, w, height, 3f, 1f);
                drawElementShine(infoPopup, context, x, y, w, height, 3f);
                DrawUtil.drawText(msdf, ph, x + leftPad, y + 4f, ColorProvider.rgba(255, 255, 255, 120), 7f);
                infoDrag.setWidth(w);
                infoDrag.setHeight(height);
            } else {
                infoDrag.setWidth(0f);
                infoDrag.setHeight(0f);
            }
            return;
        }

        String coordsIcon = "\u0046";
        String tpsIcon = "\u0024";
        String bpsIcon = "\u0040";

        String xPart = "x" + (int) mc.player.getX();
        String yPart = "y" + (int) mc.player.getY();
        String zPart = "z" + (int) mc.player.getZ();

        double dX = mc.player.getX() - mc.player.prevX;
        double dZ = mc.player.getZ() - mc.player.prevZ;
        String bpsValue = String.format(java.util.Locale.US, "%.1f Bps", Math.hypot(dX, dZ) * 20);
        String tpsValue = String.format(java.util.Locale.US, "%.1f Tps", AlphaDLC.getInstance().getTpsGetter().getTPS());

        int groups = (showCoords ? 1 : 0) + (showTps ? 1 : 0) + (showBps ? 1 : 0);

        // ---- ширина ----
        float boxWidth = leftPad + rightPad;
        if (showCoords) {
            boxWidth += icons.getWidth(coordsIcon, 7f) + iconGap
                    + msdf.getWidth(xPart, 7f) + sep + msdf.getWidth(yPart, 7f) + sep + msdf.getWidth(zPart, 7f);
        }
        if (showTps) boxWidth += icons.getWidth(tpsIcon, 7f) + iconGap + msdf.getWidth(tpsValue, 7f);
        if (showBps) boxWidth += icons.getWidth(bpsIcon, 7f) + iconGap + msdf.getWidth(bpsValue, 7f);
        boxWidth += sep * (groups - 1);

        drawElementBackground(infoPopup, x, y, boxWidth, height, 3f, 1f);
        drawElementShine(infoPopup, context, x, y, boxWidth, height, 3f);

        float dotY = y + (height - dotSize) / 2f;
        float cx = x + leftPad;
        boolean first = true;

        if (showCoords) {
            DrawUtil.drawText(icons, coordsIcon, cx, y + 4.75f, iconColor, 7f);
            cx += icons.getWidth(coordsIcon, 7f) + iconGap;
            DrawUtil.drawText(msdf, xPart, cx, y + 4f, whiteColor, 7f);
            cx += msdf.getWidth(xPart, 7f) + sepGap;
            DrawUtil.drawRound(cx, dotY, dotSize, dotSize, dotSize / 2f, dotColor);
            cx += dotSize + sepGap;
            DrawUtil.drawText(msdf, yPart, cx, y + 4f, whiteColor, 7f);
            cx += msdf.getWidth(yPart, 7f) + sepGap;
            DrawUtil.drawRound(cx, dotY, dotSize, dotSize, dotSize / 2f, dotColor);
            cx += dotSize + sepGap;
            DrawUtil.drawText(msdf, zPart, cx, y + 4f, whiteColor, 7f);
            cx += msdf.getWidth(zPart, 7f);
            first = false;
        }
        if (showTps) {
            if (!first) {
                cx += sepGap;
                DrawUtil.drawRound(cx, dotY, dotSize, dotSize, dotSize / 2f, dotColor);
                cx += dotSize + sepGap;
            }
            DrawUtil.drawText(icons, tpsIcon, cx, y + 4.75f, iconColor, 7f);
            cx += icons.getWidth(tpsIcon, 7f) + iconGap;
            DrawUtil.drawText(msdf, tpsValue, cx, y + 4f, whiteColor, 7f);
            cx += msdf.getWidth(tpsValue, 7f);
            first = false;
        }
        if (showBps) {
            if (!first) {
                cx += sepGap;
                DrawUtil.drawRound(cx, dotY, dotSize, dotSize, dotSize / 2f, dotColor);
                cx += dotSize + sepGap;
            }
            DrawUtil.drawText(icons, bpsIcon, cx, y + 4.75f, iconColor, 7f);
            cx += icons.getWidth(bpsIcon, 7f) + iconGap;
            DrawUtil.drawText(msdf, bpsValue, cx, y + 4f, whiteColor, 7f);
            cx += msdf.getWidth(bpsValue, 7f);
        }

        infoDrag.setWidth(boxWidth);
        infoDrag.setHeight(height);
    }

    private int colorLerp(int start, int end, float speed, float offset) {
        long t = System.currentTimeMillis();
        double ph = t * (speed / 1000.0) + offset;
        float p = (float) (Math.sin(ph) * 0.5 + 0.5);

        int sr = (start >> 16) & 0xFF;
        int sg = (start >> 8) & 0xFF;
        int sb = start & 0xFF;
        int er = (end >> 16) & 0xFF;
        int eg = (end >> 8) & 0xFF;
        int eb = end & 0xFF;

        int r = (int) (sr * (1f - p) + er * p);
        int g = (int) (sg * (1f - p) + eg * p);
        int b = (int) (sb * (1f - p) + eb * p);

        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private void renderCoordsInfo(DrawContext context) {}

    private void renderTargetHUD(DrawContext context) {
        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        if (!chatOpen) {
            targetHudPopup.open = false;
            targetHudPopup.draggingSlider = null;
        }
        beginScale(targetHudPopup, context);
        renderTargetHUDContent(context);
        endScale(targetHudPopup, context);
        runPopup(targetHudPopup, context);
    }

    private void renderTargetHUDContent(DrawContext context) {
        KillAura killAura = Instance.get(KillAura.class);
        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        LivingEntity target = null;
        if (killAura.isEnabled() && killAura.getTarget() != null && killAura.getTarget().isAlive()) {
            target = killAura.getTarget();
        }
        else if (thShowOnHover.getValue() && mc.targetedEntity instanceof LivingEntity living && living.isAlive()) {
            target = living;
        }
        else if (chatOpen) {
            target = mc.player;
        }
        if (target != null) {
            lastTarget = target;
            animation.run(1);
            armorAnim.run(1);
        } else {
            animation.run(0);
            armorAnim.run(0);
        }

        if (animation.getValue() <= 0.05f || lastTarget == null || !(lastTarget instanceof LivingEntity)) return;

        LivingEntity livingEntity = (LivingEntity) lastTarget;
        AbstractClientPlayerEntity playerEntity = lastTarget instanceof AbstractClientPlayerEntity ? (AbstractClientPlayerEntity) lastTarget : null;

        float anim = (float) animation.getValue();
        int alphaInt = (int) (255 * anim);

        float width = 108;
        float height = 34;
        float x = targetHUDDrag.getX();
        float y = targetHUDDrag.getY();

        drawElementBackground(targetHudPopup, x, y, width, height, 4, anim);
        drawElementShine(targetHudPopup, context, x, y, width, height, 4);

        // Голова/энтити — по центру по высоте
        float headSize = 24f;
        float headX = x + 4;
        float headY = y + (height - headSize) / 2f;

        float hurtPercent = livingEntity.hurtTime / 10f;
        int headColor = ColorProvider.rgba(255, (int)(255 * (1 - hurtPercent)), (int)(255 * (1 - hurtPercent)), alphaInt);

        // Голова как текстура (игрок — скин, моб — текстура его рендерера), кроп лица 8x8
        try {
            net.minecraft.util.Identifier faceTex = getEntityFaceTexture(livingEntity, playerEntity);
            if (faceTex != null) {
                net.minecraft.client.texture.AbstractTexture tex = mc.getTextureManager().getTexture(faceTex);
                // фикс "мыла": без билинейного фильтра/мипмапов
                tex.setFilter(false, false);
                int texId = tex.getGlId();
                if (texId > 0) {
                    zov.alphadlc.util.render.renderers.impl.BuiltTexture headTexture = Builder.texture()
                            .size(new SizeState(headSize, headSize))
                            .radius(new QuadRadiusState(6))
                            .color(new QuadColorState(headColor))
                            .texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                            .smoothness(1f)
                            .build();

                    headTexture.render(context.getMatrices().peek().getPositionMatrix(), headX, headY);
                }
            }
        } catch (Exception ignored) {}

        float textX = headX + headSize + 5;
        float rightEdge = x + width - 5;

        zov.alphadlc.module.list.misc.NameProtect nameProtect = Instance.get(zov.alphadlc.module.list.misc.NameProtect.class);

        String name = nameProtect.isEnabled() ? nameProtect.getCustomName(livingEntity.getName().getString()) : livingEntity.getName().getString();

        // Обрезаем имя по доступной ширине (Scissor не учитывает масштаб beginScale и прятал бы ник)
        String shownName = trimTextToWidth(name, 8.5f, rightEdge - textX);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), shownName, textX, y + 3f, ColorProvider.rgba(255, 255, 255, alphaInt), 8.5f);

        float currentHp = zov.alphadlc. util.server.Server.getHealth(livingEntity, false);
        if (Float.isNaN(currentHp) || currentHp < 0) currentHp = 0;

        float absorption = livingEntity.getAbsorptionAmount();
        if (Float.isNaN(absorption) || absorption < 0) absorption = 0;

        float barX = textX;
        float barHeight = 4f;
        float barY = y + height - 8f;
        float barWidth = rightEdge - textX;

        float maxHealth = livingEntity.getMaxHealth();
        // Полоска вмещает и обычное хп, и золотое (AB) — AB справа
        float total = maxHealth + absorption;
        if (total <= 0f) total = 1f;

        float healthFrac = MathHelper.clamp(currentHp / total, 0, 1);
        float absFrac = MathHelper.clamp(absorption / total, 0, 1);

        hpAnimation.run(healthFrac);
        absorptionAnimation.run(absFrac);

        float healthW = barWidth * (float) hpAnimation.getValue();
        float absW = barWidth * (float) absorptionAnimation.getValue();

        int c1 = ColorProvider.getColorClient();
        int hpColor = ColorProvider.setAlpha(c1, alphaInt);
        int goldColor = ColorProvider.rgba(255, 215, 0, alphaInt);
        int backColor = ColorProvider.rgba(20, 20, 20, (int)(160 * anim));

        DrawUtil.drawRound(barX, barY, barWidth, barHeight, barHeight / 2f, backColor);

        // Обычное хп (без градиента, сплошной цвет)
        if (healthW > 0.5f) {
            DrawUtil.drawRound(barX, barY, healthW, barHeight, barHeight / 2f, hpColor);
        }
        // Золотое хп (AB) — справа от обычного
        if (absW > 0.5f) {
            DrawUtil.drawRound(barX + healthW, barY, absW, barHeight, barHeight / 2f, goldColor);
        }

        // Стрелка-указатель (arrow drop down) едет за заполнением; над ней — число ХП (с учётом AB)
        float fillW = Math.min(barWidth, healthW + absW);
        float arrowX = MathHelper.clamp(barX + fillW, barX + 3f, barX + barWidth - 3f);
        float arrowTipY = barY - 1f;
        float arrowTopY = barY - 5f;
        boolean hasAbs = absorption > 0.05f;
        int arrowColor = hasAbs ? goldColor : ColorProvider.rgba(255, 255, 255, alphaInt);
        drawArrowDropDown(arrowX, arrowTopY, arrowTipY, 6f, arrowColor);

        float shownHp = currentHp + absorption;
        String hpNum = String.format(java.util.Locale.US, "%.1f", shownHp);
        float hpNumW = Fonts.SFMEDIUM.get().getWidth(hpNum, 6.5f);
        float hpNumX = MathHelper.clamp(arrowX - hpNumW / 2f, x + 3f, x + width - hpNumW - 3f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), hpNum, hpNumX, arrowTopY - 8f, ColorProvider.rgba(255, 255, 255, alphaInt), 6.5f);

        float armorAlpha = (float) armorAnim.getValue();
        if (thShowItems.getValue() && armorAlpha > 0.05f) {
            java.util.List<ItemStack> armorList = new ArrayList<>();
            for (ItemStack stack : livingEntity.getArmorItems()) armorList.add(stack);
            Collections.reverse(armorList); // шлем -> ботинки
            java.util.List<ItemStack> handsList = new ArrayList<>();
            handsList.add(livingEntity.getMainHandStack());
            handsList.add(livingEntity.getOffHandStack());

            float cell = 13f;
            float boxPad = 2f;
            float boxH = cell + boxPad * 2f;
            float armorBoxW = 4 * cell + boxPad * 2f;
            float handsBoxW = 2 * cell + boxPad * 2f;
            float boxesY = y + height + 3f;

            // Броня — слева, руки — справа
            float armorBoxX = x;
            float handsBoxX = x + width - handsBoxW;

            // Фон-окошечки: броня отдельно, руки отдельно
            drawElementBackground(targetHudPopup, armorBoxX, boxesY, armorBoxW, boxH, 3f, armorAlpha);
            drawElementBackground(targetHudPopup, handsBoxX, boxesY, handsBoxW, boxH, 3f, armorAlpha);

            float itemScale = 11f / 16f;
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 100);
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            drawItemSlots(context, textRenderer, armorList, armorBoxX + boxPad, boxesY + boxPad, cell, itemScale);
            drawItemSlots(context, textRenderer, handsList, handsBoxX + boxPad, boxesY + boxPad, cell, itemScale);
            context.getMatrices().pop();
        }

        targetHUDDrag.setWidth(width);
        targetHUDDrag.setHeight(height);
    }

    private float trailHealthPercent = 1f;
    private float lastHealthPercent = 1f;
    private float lastHpRaw = -1f;




    private final List<HeadParticle> headParticles = new ArrayList<>();

    private static class HeadParticle {
        float x, y, vx, vy, size;
        long spawnTime;
        int color;

        HeadParticle(float startX, float startY, int color) {
            this.x = startX;
            this.y = startY;
            double angle = Math.random() * Math.PI * 2;
            double speed = Math.random() * 0.4 + 0.1;
            this.vx = (float) (Math.cos(angle) * speed);
            this.vy = (float) (Math.sin(angle) * speed);
            this.size = (float) (Math.random() * 8 + 2);
            this.spawnTime = System.currentTimeMillis();
            this.color = color;
        }

        void update() {
            x += vx;
            y += vy;
        }

        float getAlpha() {
            long elapsed = System.currentTimeMillis() - spawnTime;
            if (elapsed >= 2000) return 0;
            return 1f - ((float) elapsed / 2000f);
        }
    }
    // Текстура "лица" энтити: у игрока — скин, у моба — текстура его рендерера (кроп лица 8x8 как у скина)
    @SuppressWarnings({"rawtypes", "unchecked"})
    private net.minecraft.util.Identifier getEntityFaceTexture(LivingEntity entity, AbstractClientPlayerEntity player) {
        try {
            if (player != null) {
                return player.getSkinTextures().texture();
            }
            net.minecraft.client.render.entity.EntityRenderer baseRenderer =
                    mc.getEntityRenderDispatcher().getRenderer(entity);
            if (baseRenderer instanceof net.minecraft.client.render.entity.LivingEntityRenderer) {
                net.minecraft.client.render.entity.LivingEntityRenderer renderer =
                        (net.minecraft.client.render.entity.LivingEntityRenderer) baseRenderer;
                float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
                net.minecraft.client.render.entity.state.LivingEntityRenderState state =
                        (net.minecraft.client.render.entity.state.LivingEntityRenderState)
                                renderer.getAndUpdateRenderState(entity, tickDelta);
                if (state != null) {
                    return renderer.getTexture(state);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public void drawEntity(float x, float y, float scale, float yawAngle, float pitchAngle, net.minecraft.entity.LivingEntity entity) {
        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.translate(x, y, 50.0);
        matrices.scale(-scale, scale, scale);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yawAngle));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pitchAngle));

        float bodyYaw = entity.bodyYaw;
        float prevBodyYaw = entity.prevBodyYaw;
        float headYaw = entity.headYaw;
        float prevHeadYaw = entity.prevHeadYaw;
        float yaw = entity.getYaw();
        float prevYaw = entity.prevYaw;
        float pitch = entity.getPitch();
        float prevPitch = entity.prevPitch;

        entity.bodyYaw = 0;
        entity.prevBodyYaw = 0;
        entity.headYaw = 0;
        entity.prevHeadYaw = 0;
        entity.setYaw(0);
        entity.prevYaw = 0;
        entity.setPitch(0);
        entity.prevPitch = 0;

        net.minecraft.client.render.DiffuseLighting.disableGuiDepthLighting();
        net.minecraft.client.render.VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        mc.getEntityRenderDispatcher().render(entity, 0.0, 0.0, 0.0, tickDelta, matrices, immediate, 0x00F000F0);

        immediate.draw();
        net.minecraft.client.render.DiffuseLighting.enableGuiDepthLighting();

        entity.bodyYaw = bodyYaw;
        entity.prevBodyYaw = prevBodyYaw;
        entity.headYaw = headYaw;
        entity.prevHeadYaw = prevHeadYaw;
        entity.setYaw(yaw);
        entity.prevYaw = prevYaw;
        entity.setPitch(pitch);
        entity.prevPitch = prevPitch;

        matrices.pop();
    }



    private java.awt.Color lerpColor(java.awt.Color a, java.awt.Color b, float t) {
        return new java.awt.Color(
                (int) (a.getRed() + t * (b.getRed() - a.getRed())),
                (int) (a.getGreen() + t * (b.getGreen() - a.getGreen())),
                (int) (a.getBlue() + t * (b.getBlue() - a.getBlue()))
        );
    }

    private static class PotionItem {
        String name;
        int amplifier;
        int durationTicks;
        int maxDurationTicks;
        boolean active;
        net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect;
        Animation animation = new Animation(Easing.EXPO_OUT, 233);
        Animation rowAnim = new Animation(Easing.DECELERATE, 150);

        PotionItem(String name, int amplifier, int durationTicks, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect) {
            this.name = name;
            this.amplifier = amplifier;
            this.durationTicks = durationTicks;
            this.maxDurationTicks = durationTicks;
            this.effect = effect;
            this.active = true;
        }
    }

    private final java.util.List<PotionItem> potionItems = new CopyOnWriteArrayList<>();

    // Random effect for example display when chat is open
    private net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> currentRandomEffect;
    private long lastEffectChange = 0;
    private static java.util.List<net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect>> cachedAllEffects;
    private static final java.util.Random RANDOM = new java.util.Random();

    private void updatePotions() {
        java.util.Map<String, StatusEffectInstance> currentEffects = mc.player.getStatusEffects().stream()
                .collect(Collectors.toMap(
                        e -> net.minecraft.text.Text.translatable(e.getTranslationKey()).getString() + ":" + e.getAmplifier(),
                        e -> e,
                        (e1, e2) -> e1
                ));

        potionItems.forEach(item -> {
            String key = item.name + ":" + item.amplifier;
            StatusEffectInstance effect = currentEffects.get(key);

            if (effect != null) {
                item.durationTicks = effect.getDuration();
                if (item.durationTicks > item.maxDurationTicks) {
                    item.maxDurationTicks = item.durationTicks;
                }
                if (!item.active) {
                    item.animation.setValue(1.0f);
                }
                item.active = true;
                currentEffects.remove(key);
            } else {
                item.active = false;
            }
        });

        boolean added = !currentEffects.isEmpty();
        currentEffects.forEach((key, effect) -> {
            potionItems.add(new PotionItem(
                    net.minecraft.text.Text.translatable(effect.getTranslationKey()).getString(),
                    effect.getAmplifier(),
                    effect.getDuration(),
                    effect.getEffectType()
            ));
        });

        boolean removed = potionItems.removeIf(item -> !item.active && item.animation.getValue() == 0);

        // Сортируем только когда набор изменился (добавили/удалили), а не каждый кадр в рендере.
        if (added || removed) {
            potionItems.sort(java.util.Comparator.comparing(pi -> pi.name));
        }
    }

    // ===================== CoolDowns =====================
    private static class CooldownItem {
        final Item item;
        long endTimeMs;
        long maxDurationMs;
        boolean active = true;
        final Animation animation = new Animation(Easing.EXPO_OUT, 233);
        final Animation rowAnim = new Animation(Easing.DECELERATE, 150);

        CooldownItem(Item item, long endTimeMs, long maxDurationMs) {
            this.item = item;
            this.endTimeMs = endTimeMs;
            this.maxDurationMs = maxDurationMs;
        }

        int remainingSeconds() {
            long rem = endTimeMs - System.currentTimeMillis();
            return (int) Math.max(0, Math.ceil(rem / 1000.0));
        }
    }

    private final java.util.List<CooldownItem> cooldownItems = new CopyOnWriteArrayList<>();

    private static final Item[] COOLDOWN_EXAMPLE_ITEMS = {
            Items.ENDER_PEARL, Items.ENDER_EYE, Items.CHORUS_FRUIT, Items.MACE,
            Items.ENCHANTED_GOLDEN_APPLE, Items.TRIDENT, Items.SHIELD, Items.GOAT_HORN
    };
    private int cooldownExampleIndex = 0;
    private long cooldownExampleChange = 0;

    private void updateCooldowns() {
        if (mc.player == null) return;
        for (CooldownItem ci : cooldownItems) {
            boolean cooling = mc.player.getItemCooldownManager().isCoolingDown(ci.item.getDefaultStack());
            if (!cooling || System.currentTimeMillis() >= ci.endTimeMs) {
                ci.active = false;
            }
        }
        cooldownItems.removeIf(ci -> !ci.active && ci.animation.getValue() == 0);

        if (cooldownItems.isEmpty() && mc.currentScreen instanceof ChatScreen) {
            long now = System.currentTimeMillis();
            if (now - cooldownExampleChange >= 1500) {
                cooldownExampleIndex = (cooldownExampleIndex + 1) % COOLDOWN_EXAMPLE_ITEMS.length;
                cooldownExampleChange = now;
            }
        }
    }

    private void renderCoolDowns(DrawContext context) {
        if (mc.player == null) return;

        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        if (!chatOpen) {
            cooldownsPopup.open = false;
            cooldownsPopup.draggingSlider = null;
        }

        beginScale(cooldownsPopup, context);
        renderCoolDownsNew(context);
        endScale(cooldownsPopup, context);

        runPopup(cooldownsPopup, context);
    }

    private final Animation cdAlpha = new Animation(Easing.EXPO_OUT, 200);

    private void drawCooldownIcon(DrawContext context, ItemStack stack, float x, float y, float size, int alpha) {
        float scale = size / 16f;
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1f);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha / 255f);
        context.drawItem(stack, 0, 0);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        context.getMatrices().pop();
    }

    private void renderCoolDownsNew(DrawContext context) {
        if (mc.player == null) return;

        float posX = cooldownsDrag.getX();
        float posY = cooldownsDrag.getY();

        float headerHeight = 14f;
        float itemHeight = 12f;
        float minWidth = 56f;
        float padX = 5f;
        float padY = 2f;

        boolean isFound = false;
        for (CooldownItem item : cooldownItems) {
            item.animation.run(item.active ? 1 : 0);
            item.rowAnim.run(item.active ? 1 : 0);
            if (item.animation.getValue() > 0.001f) isFound = true;
        }

        boolean showExample = (mc.currentScreen instanceof ChatScreen) && !isFound;

        if (!isFound && !(mc.currentScreen instanceof ChatScreen)) cdAlpha.run(0);
        else cdAlpha.run(1);

        float globalAlpha = (float) cdAlpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int headerAlpha = (int) Math.min(255, Math.max(0, 255 * globalAlpha));

        float maxNameBoxW = minWidth;
        float maxTimeBoxW = 0f;
        float contentHeight = 0f;
        for (CooldownItem item : cooldownItems) {
            float animVal = (float) item.animation.getValue();
            if (animVal > 0.001f) {
                contentHeight += itemHeight * (float) item.rowAnim.getValue();
                String name = item.item.getName().getString();
                float nameW = Fonts.SFMEDIUM.get().getWidth(name, 7f);
                float nw = nameW + 8f + 12f + 10f;
                if (nw > maxNameBoxW) maxNameBoxW = nw;

                int sec = item.remainingSeconds();
                String timeStr = String.format("%d:%02d", sec / 60, sec % 60);
                float tw = Fonts.SFMEDIUM.get().getWidth(timeStr, 6.75f) + 10f
                        + (cooldownsRing.getValue() ? POTION_RING_DIAMETER + POTION_RING_GAP : 0f);
                if (tw > maxTimeBoxW) maxTimeBoxW = tw;
            }
        }

        if (showExample) {
            float exNameW = Fonts.SFMEDIUM.get().getWidth("Example", 7f);
            if (exNameW + 8f + 12f + 10f > maxNameBoxW) maxNameBoxW = exNameW + 8f + 12f + 10f;
            float exTimeW = Fonts.SFMEDIUM.get().getWidth("**:**", 6.75f) + 10f
                    + (cooldownsRing.getValue() ? POTION_RING_DIAMETER + POTION_RING_GAP : 0f);
            if (exTimeW > maxTimeBoxW) maxTimeBoxW = exTimeW;
            contentHeight = itemHeight;
        }

        float rawWidth = maxNameBoxW + maxTimeBoxW + padX * 2;
        cooldownsPopup.panelWidth.run(rawWidth);
        float totalRowWidth = (float) cooldownsPopup.panelWidth.getValue();
        if (totalRowWidth < 20f) totalRowWidth = rawWidth;
        float totalHeight = headerHeight + contentHeight + padY * 2;

        drawElementBackground(cooldownsPopup, posX, posY, totalRowWidth, totalHeight, 3f, globalAlpha);
        drawElementShine(cooldownsPopup, context, posX, posY, totalRowWidth, totalHeight, 3f);

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Cooldowns", posX + padX + 4f, posY + padY + 2.5f, ColorProvider.rgba(255, 255, 255, headerAlpha), 8f);
        DrawUtil.drawText(Fonts.ALPHADLC.get(), "h", posX + totalRowWidth - padX - 11f, posY + padY + 3f, ColorProvider.setAlpha(ColorProvider.getColorIcons(), headerAlpha), 9);

        float curY = posY + headerHeight + padY;

        for (CooldownItem item : cooldownItems) {
            float animVal = (float) item.animation.getValue();
            if (animVal <= 0.001f) continue;
            float rowAnimVal = (float) item.rowAnim.getValue();
            if (rowAnimVal <= 0.001f) continue;

            int sec = item.remainingSeconds();
            String timeStr = String.format("%d:%02d", sec / 60, sec % 60);
            String nameStr = item.item.getName().getString();

            int itemAlpha = (int) Math.min(255, Math.max(0, 255 * animVal * globalAlpha));
            if (itemAlpha < 5) continue;

            float timeW = Fonts.SFMEDIUM.get().getWidth(timeStr, 6.75f);
            float timeBoxW = timeW + 10f;

            float rowHeight = itemHeight * rowAnimVal;
            context.getMatrices().push();
            context.getMatrices().translate(posX + totalRowWidth / 2f, curY + rowHeight / 2f, 0);
            context.getMatrices().scale(rowAnimVal, rowAnimVal, rowAnimVal);
            context.getMatrices().translate(-(posX + totalRowWidth / 2f), -(curY + rowHeight / 2f), 0);

            float cdMid = textMidY(curY + 2.75f, 7f);
            drawCooldownIcon(context, item.item.getDefaultStack(), posX + padX + 1f, cdMid - 5f, 10f, itemAlpha);

            float dotX = posX + padX + 13f;
            float dotY = cdMid - 1.5f;
            DrawUtil.drawRound(dotX, dotY, 3f, 3f, 1.5f, ColorProvider.rgba(120, 120, 120, itemAlpha));

            float textX = posX + padX + 20f;
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), nameStr, textX, curY + 2.75f, ColorProvider.rgba(255, 255, 255, itemAlpha), 7f);

            float timerX = posX + totalRowWidth - padX - timeBoxW;

            if (cooldownsRing.getValue()) {
                float ringRadius = POTION_RING_DIAMETER / 2f;
                float ringCX = timerX - POTION_RING_GAP - ringRadius;
                float ringCY = cdMid;
                float progress = 1f;
                if (item.maxDurationMs > 0) {
                    long rem = item.endTimeMs - System.currentTimeMillis();
                    progress = MathHelper.clamp((float) rem / (float) item.maxDurationMs, 0f, 1f);
                }
                DrawUtil.drawRingArc(ringCX, ringCY, ringRadius, POTION_RING_THICKNESS, -90f, 270f, ColorProvider.rgba(80, 80, 80, itemAlpha));
                if (progress > 0f) {
                    DrawUtil.drawRingArc(ringCX, ringCY, ringRadius, POTION_RING_THICKNESS, -90f, -90f + 360f * progress,
                            ColorProvider.setAlpha(ColorProvider.getColorClient(), itemAlpha));
                }
            }

            String timerKey = "cooldowns_duration_" + item.item.getTranslationKey();
            zov.alphadlc.util.render.timer.TimerTextAnimator.draw(
                    Fonts.SFMEDIUM.get(), timerKey, timeStr,
                    timerX + (timeBoxW - timeW) / 2f + 1f, curY + 3.5f,
                    ColorProvider.rgba(255, 255, 255, itemAlpha), 6.75f);

            context.getMatrices().pop();
            curY += rowHeight;
        }

        if (showExample) {
            int exampleAlpha = headerAlpha;
            String nameStr = "Example";
            String timeStr = "**:**";
            ItemStack stack = COOLDOWN_EXAMPLE_ITEMS[cooldownExampleIndex].getDefaultStack();

            float timeW = Fonts.SFMEDIUM.get().getWidth(timeStr, 6.75f);
            float timeBoxW = timeW + 10f;

            float cdExMid = textMidY(curY + 3.5f, 7f);
            drawCooldownIcon(context, stack, posX + padX + 1f, cdExMid - 5f, 10f, exampleAlpha);

            float dotX = posX + padX + 13f;
            float dotY = cdExMid - 1.5f;
            DrawUtil.drawRound(dotX, dotY, 3f, 3f, 1.5f, ColorProvider.rgba(120, 120, 120, exampleAlpha));

            float textX = posX + padX + 20f;
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), nameStr, textX, curY + 3.5f, ColorProvider.rgba(255, 255, 255, exampleAlpha), 7f);

            float timerX = posX + totalRowWidth - padX - timeBoxW;

            if (cooldownsRing.getValue()) {
                float ringRadius = POTION_RING_DIAMETER / 2f;
                float ringCX = timerX - POTION_RING_GAP - ringRadius;
                float ringCY = cdExMid;
                DrawUtil.drawRingArc(ringCX, ringCY, ringRadius, POTION_RING_THICKNESS, -90f, 270f, ColorProvider.rgba(80, 80, 80, exampleAlpha));
                DrawUtil.drawRingArc(ringCX, ringCY, ringRadius, POTION_RING_THICKNESS, -90f, 180f,
                        ColorProvider.setAlpha(ColorProvider.getColorClient(), exampleAlpha));
            }

            DrawUtil.drawText(Fonts.SFMEDIUM.get(), timeStr, timerX + (timeBoxW - timeW) / 2f + 1f, curY + 3.5f, ColorProvider.rgba(255, 255, 255, exampleAlpha), 6.75f);
        }

        cooldownsDrag.setWidth(totalRowWidth);
        cooldownsDrag.setHeight(totalHeight);
    }

    // Визуальный вертикальный центр текста (эмпирически: базовая линия рендера пера)
    private static float textMidY(float drawY, float size) {
        return drawY + size * 0.59f;
    }

    // Стрелка "arrow drop down" — треугольник остриём вниз
    private void drawArrowDropDown(float cx, float topY, float tipY, float w, int color) {
        float h = tipY - topY;
        if (h <= 0f) h = 4f;
        int steps = Math.max(4, (int) (h * 2));
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float rowW = w * (1f - t);
            float ry = topY + h * t;
            DrawUtil.drawRound(cx - rowW / 2f, ry, rowW, h / steps + 0.6f, 0f, color);
        }
    }

    // Рисует предметы по ячейкам с центрированием внутри окошка
    private void drawItemSlots(DrawContext context, TextRenderer textRenderer, java.util.List<ItemStack> items,
                               float startX, float startY, float cell, float itemScale) {
        float drawn = 16f * itemScale;
        float inset = (cell - drawn) / 2f;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack == null || stack.isEmpty()) continue;
            float cx = startX + i * cell + inset;
            float cy = startY + inset;
            context.getMatrices().push();
            context.getMatrices().translate(cx, cy, 0);
            context.getMatrices().scale(itemScale, itemScale, 1f);
            context.drawItem(stack, 0, 0);
            context.drawStackOverlay(textRenderer, stack, 0, 0);
            context.getMatrices().pop();
        }
    }

    public float getNotificationsX() {
        return notificationsDrag.getX();
    }

    public float getNotificationsY() {
        return notificationsDrag.getY();
    }

    // Ширина примера уведомления (та же формула, что и в renderNotificationsExample)
    private float notifExampleWidth() {
        float toggleW = 15f, pad = 6f, gap = 6f;
        float textW = Fonts.SFMEDIUM.get().getWidth("Пример уведомления", 7f);
        return pad + textW + gap + toggleW + pad;
    }

    // Центр по X, вокруг которого выравниваются уведомления — совпадает с центром примера
    public float getNotificationsCenterX() {
        return notificationsDrag.getX() + notifExampleWidth() / 2f;
    }

    // Масштаб уведомлений (тот же слайдер "Размер", что и у примера)
    public float getNotificationsScale() {
        return notificationsPopup.size.getFloatValue();
    }

    // ===================== Notifications (Potions style) =====================
    // Публичные обёртки для NotificationsElement — единый фон/блик как у Potions
    public void drawNotifBackground(DrawContext context, float x, float y, float w, float h, float radius, float alphaFactor) {
        drawElementBackground(notificationsPopup, x, y, w, h, radius, alphaFactor);
        drawElementShine(notificationsPopup, context, x, y, w, h, radius);
    }

    public void drawNewToggle(float x, float y, float toggleW, float toggleH, boolean on, float anim, int alpha) {
        int inactive = ColorProvider.setAlpha(ColorProvider.getColorInactiveIndicator(), alpha);
        int active = ColorProvider.setAlpha(ColorProvider.getColorIndicator(), alpha);
        int bg = ColorProvider.interpolateColor(inactive, active, anim);
        DrawUtil.drawRound(x, y, toggleW, toggleH, toggleH / 2f, bg);
        float knob = toggleH - 1f;
        float knobMinX = x + 0.5f;
        float knobMaxX = x + toggleW - knob - 0.5f;
        float knobX = knobMinX + (knobMaxX - knobMinX) * anim;
        DrawUtil.drawCircle(knobX + knob / 2f, y + 0.5f + knob / 2f, knob / 2f, ColorProvider.setAlpha(ColorProvider.getColorSliderCircle(), alpha));
    }

    private final Animation exampleNotifToggleAnim = new Animation(Easing.EXPO_OUT, 200);

    private void renderNotificationsExample(DrawContext context) {
        if (mc.player == null) return;
        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        if (!chatOpen) {
            notificationsPopup.open = false;
            notificationsPopup.draggingSlider = null;
            return;
        }

        beginScale(notificationsPopup, context);

        float posX = notificationsDrag.getX();
        float posY = notificationsDrag.getY();

        String text = "Пример уведомления";
        float toggleW = 15f;
        float toggleH = 8f;
        float pad = 6f;
        float gap = 6f;
        float textW = Fonts.SFMEDIUM.get().getWidth(text, 7f);
        float width = pad + textW + gap + toggleW + pad;
        float height = 15f;

        drawElementBackground(notificationsPopup, posX, posY, width, height, 3f, 1f);
        drawElementShine(notificationsPopup, context, posX, posY, width, height, 3f);

        // Автопереключение примера каждые 3 секунды (вкл/выкл)
        boolean exampleOn = (System.currentTimeMillis() / 3000L) % 2 == 0;
        exampleNotifToggleAnim.run(exampleOn ? 1 : 0);

        // Текст слева, переключатель после него — группа отцентрирована симметричными отступами
        float textX = posX + pad;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), text, textX, posY + (height / 2f) - 7f * 0.59f, ColorProvider.rgba(255, 255, 255, 255), 7f);

        float tX = textX + textW + gap;
        float tY = posY + (height - toggleH) / 2f;
        drawNewToggle(tX, tY, toggleW, toggleH, exampleOn, (float) exampleNotifToggleAnim.getValue(), 255);

        notificationsDrag.setWidth(width);
        notificationsDrag.setHeight(height);

        endScale(notificationsPopup, context);

        runPopup(notificationsPopup, context);
    }

    // ===================== ServerHelper =====================
    private static class ServerHelperItem {
        final Item item;
        final String keyBind;
        final String name;

        ServerHelperItem(Item item, String keyBind, String name) {
            this.item = item;
            this.keyBind = keyBind;
            this.name = name;
        }
    }

    private void renderServerHelper(DrawContext context) {
        if (mc.player == null) return;

        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        if (!chatOpen) {
            serverHelperPopup.open = false;
            serverHelperPopup.draggingSlider = null;
        }

        beginScale(serverHelperPopup, context);
        renderServerHelperNew(context);
        endScale(serverHelperPopup, context);

        runPopup(serverHelperPopup, context);
    }

    private void renderServerHelperNew(DrawContext context) {
        if (mc.player == null) return;

        ServerHelper serverHelper = AlphaDLC.getInstance().getModuleStorage().get(ServerHelper.class);
        if (serverHelper == null) return;

        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        boolean showExample = chatOpen && !serverHelper.isEnabled();

        float posX = serverHelperDrag.getX();
        float posY = serverHelperDrag.getY();

        float itemSize = 32f;
        float gap = 4f;
        float padding = 2f;

        // Build list of items from ServerHelper bindings
        java.util.List<ServerHelperItem> items = new java.util.ArrayList<>();
        
        if (showExample) {
            // Show example items when chat is open
            items.add(new ServerHelperItem(Items.FIREWORK_STAR, "G", "АнтиПолет"));
            items.add(new ServerHelperItem(Items.SPLASH_POTION, "H", "Гринч"));
            items.add(new ServerHelperItem(Items.PLAYER_HEAD, "J", "Shift"));
            items.add(new ServerHelperItem(Items.HEART_OF_THE_SEA, "K", "Трапка"));
        } else if (serverHelper.isEnabled()) {
            // Анти Полет
            int antiFlyKey = serverHelper.getAntiFlyKey();
            if (antiFlyKey != -1 && hasItemInInventory(Items.FIREWORK_STAR)) {
                items.add(new ServerHelperItem(Items.FIREWORK_STAR, getKeyNameEnglish(antiFlyKey), "АнтиПолет"));
            }

            // Зелье Гринча
            int grinchKey = serverHelper.getGrinchPotionKey();
            if (grinchKey != -1 && hasItemInInventory(Items.SPLASH_POTION)) {
                items.add(new ServerHelperItem(Items.SPLASH_POTION, getKeyNameEnglish(grinchKey), "Гринч"));
            }

            // AutoShift
            int autoShiftKey = serverHelper.getAutoShiftKey();
            if (autoShiftKey != -1 && hasItemInInventory(Items.PLAYER_HEAD)) {
                items.add(new ServerHelperItem(Items.PLAYER_HEAD, getKeyNameEnglish(autoShiftKey), "Shift"));
            }

            // Новогодний ужас
            int horrorKey = serverHelper.getNewYearHorrorKey();
            if (horrorKey != -1 && hasItemInInventory(Items.SPLASH_POTION)) {
                items.add(new ServerHelperItem(Items.SPLASH_POTION, getKeyNameEnglish(horrorKey), "Ужас"));
            }

            // Эссенция кромешника
            int essenceKey = serverHelper.getDarkEssenceKey();
            if (essenceKey != -1 && hasItemInInventory(Items.SPLASH_POTION)) {
                items.add(new ServerHelperItem(Items.SPLASH_POTION, getKeyNameEnglish(essenceKey), "Эссенция"));
            }

            // Снежок
            int snowballKey = serverHelper.getSnowballKey();
            if (snowballKey != -1 && hasItemInInventory(Items.SPLASH_POTION)) {
                items.add(new ServerHelperItem(Items.SPLASH_POTION, getKeyNameEnglish(snowballKey), "Снежок"));
            }

            // Ловушки (Трапка)
            int trapKey = serverHelper.getTrapKey();
            if (trapKey != -1 && hasItemInInventory(Items.HEART_OF_THE_SEA)) {
                items.add(new ServerHelperItem(Items.HEART_OF_THE_SEA, getKeyNameEnglish(trapKey), "Трапка"));
            }
        }

        if (items.isEmpty()) return;

        int itemsPerRow = items.size();
        float boxSize = itemSize + padding * 2;
        float totalWidth = boxSize * itemsPerRow + gap * (itemsPerRow - 1);
        float totalHeight = boxSize;

        // Save position and size for popup
        serverHelperPopup.px = posX;
        serverHelperPopup.py = posY;
        serverHelperPopup.pw = totalWidth;
        serverHelperPopup.ph = totalHeight;

        // Get settings values
        int alpha = serverHelperPopup.alpha.getIntValue();
        float radius = 4f;

        // Draw items
        float curX = posX;
        for (ServerHelperItem item : items) {
            // Draw item box background with blur and alpha settings (like other HUD elements)
            if (serverHelperPopup.blur.getValue()) {
                // Blur with full opacity
                DrawUtil.drawRoundBlur(curX, posY, boxSize, boxSize, radius, ColorProvider.rgba(200, 200, 200, 255), 12);
                // Background with InterfaceBg color and alpha setting
                DrawUtil.drawRound(curX, posY, boxSize, boxSize, radius, ColorProvider.setAlpha(ColorProvider.getColorInterfaceBg(), alpha));
            } else {
                // Background without blur
                DrawUtil.drawRound(curX, posY, boxSize, boxSize, radius, ColorProvider.setAlpha(ColorProvider.getColorInterfaceBg(), alpha));
            }
            
            // Draw corners if enabled
            if (serverHelperPopup.corners.getValue()) {
                float th = 1.6f;
                float rc = Math.max(radius, 2f);
                float outer = rc + th / 2f;
                int cornerColor = ColorProvider.setAlpha(ColorProvider.getColorClient(), 255);
                
                DrawUtil.drawRingArc(curX + rc, posY + rc, outer, th, 180f, 270f, cornerColor);
                DrawUtil.drawRingArc(curX + boxSize - rc, posY + rc, outer, th, 270f, 360f, cornerColor);
                DrawUtil.drawRingArc(curX + boxSize - rc, posY + boxSize - rc, outer, th, 0f, 90f, cornerColor);
                DrawUtil.drawRingArc(curX + rc, posY + boxSize - rc, outer, th, 90f, 180f, cornerColor);
            }
            
            // Draw shine if enabled
            if (serverHelperPopup.shine.getValue()) {
                zov.alphadlc.util.render.renderers.impl.HudShine.render(
                        context.getMatrices(), curX, posY, boxSize, boxSize, radius,
                        serverHelperPopup.shineThickness.getFloatValue(), 1f, 
                        serverHelperPopup.shineAlpha.getFloatValue() / 100f,
                        0f);
            }
            
            // Draw item icon (positioned higher)
            ItemStack stack = item.item.getDefaultStack();
            float iconSize = 18f;
            float iconX = curX + (boxSize - iconSize) / 2f;
            float iconY = posY + padding + 2f;
            
            context.getMatrices().push();
            context.getMatrices().translate(iconX, iconY, 0);
            float iconScale = iconSize / 16f;
            context.getMatrices().scale(iconScale, iconScale, 1f);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            context.drawItem(stack, 0, 0);
            context.getMatrices().pop();
            
            // Draw keybind text below item (inside box) - increased size from 7f to 8.5f
            float keyW = Fonts.SFMEDIUM.get().getWidth(item.keyBind, 8.5f);
            float keyX = curX + (boxSize - keyW) / 2f;
            float keyY = posY + boxSize - padding - 10f;
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), item.keyBind, keyX, keyY, ColorProvider.rgba(200, 200, 200, 255), 8.5f);
            
            curX += boxSize + gap;
        }

        serverHelperDrag.setWidth(totalWidth);
        serverHelperDrag.setHeight(totalHeight);
    }

    private String getKeyNameEnglish(int keyCode) {
        if (keyCode == -1) return "None";
        if (keyCode == -100) return "LMB";
        if (keyCode == -99) return "RMB";
        if (keyCode == -98) return "MMB";
        
        // Get key name from GLFW
        String keyName = org.lwjgl.glfw.GLFW.glfwGetKeyName(keyCode, 0);
        if (keyName != null) {
            // Convert Russian to English layout
            return convertRussianToEnglish(keyName.toUpperCase());
        }
        
        // Fallback to KeyStorage and convert
        String key = KeyStorage.getKey(keyCode);
        return convertRussianToEnglish(key.toUpperCase());
    }
    
    private String convertRussianToEnglish(String text) {
        if (text == null || text.isEmpty()) return text;
        
        // Mapping Russian keyboard to English keyboard
        Map<Character, Character> russianToEnglish = new HashMap<>();
        russianToEnglish.put('Й', 'Q'); russianToEnglish.put('й', 'q');
        russianToEnglish.put('Ц', 'W'); russianToEnglish.put('ц', 'w');
        russianToEnglish.put('У', 'E'); russianToEnglish.put('у', 'e');
        russianToEnglish.put('К', 'R'); russianToEnglish.put('к', 'r');
        russianToEnglish.put('Е', 'T'); russianToEnglish.put('е', 't');
        russianToEnglish.put('Н', 'Y'); russianToEnglish.put('н', 'y');
        russianToEnglish.put('Г', 'U'); russianToEnglish.put('г', 'u');
        russianToEnglish.put('Ш', 'I'); russianToEnglish.put('ш', 'i');
        russianToEnglish.put('Щ', 'O'); russianToEnglish.put('щ', 'o');
        russianToEnglish.put('З', 'P'); russianToEnglish.put('з', 'p');
        russianToEnglish.put('Х', '['); russianToEnglish.put('х', '[');
        russianToEnglish.put('Ъ', ']'); russianToEnglish.put('ъ', ']');
        russianToEnglish.put('Ф', 'A'); russianToEnglish.put('ф', 'a');
        russianToEnglish.put('Ы', 'S'); russianToEnglish.put('ы', 's');
        russianToEnglish.put('В', 'D'); russianToEnglish.put('в', 'd');
        russianToEnglish.put('А', 'F'); russianToEnglish.put('а', 'f');
        russianToEnglish.put('П', 'G'); russianToEnglish.put('п', 'g');
        russianToEnglish.put('Р', 'H'); russianToEnglish.put('р', 'h');
        russianToEnglish.put('О', 'J'); russianToEnglish.put('о', 'j');
        russianToEnglish.put('Л', 'K'); russianToEnglish.put('л', 'k');
        russianToEnglish.put('Д', 'L'); russianToEnglish.put('д', 'l');
        russianToEnglish.put('Ж', ';'); russianToEnglish.put('ж', ';');
        russianToEnglish.put('Э', '\''); russianToEnglish.put('э', '\'');
        russianToEnglish.put('Я', 'Z'); russianToEnglish.put('я', 'z');
        russianToEnglish.put('Ч', 'X'); russianToEnglish.put('ч', 'x');
        russianToEnglish.put('С', 'C'); russianToEnglish.put('с', 'c');
        russianToEnglish.put('М', 'V'); russianToEnglish.put('м', 'v');
        russianToEnglish.put('И', 'B'); russianToEnglish.put('и', 'b');
        russianToEnglish.put('Т', 'N'); russianToEnglish.put('т', 'n');
        russianToEnglish.put('Ь', 'M'); russianToEnglish.put('ь', 'm');
        russianToEnglish.put('Б', ','); russianToEnglish.put('б', ',');
        russianToEnglish.put('Ю', '.'); russianToEnglish.put('ю', '.');
        
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append(russianToEnglish.getOrDefault(c, c));
        }
        return result.toString();
    }
    
    private boolean hasItemInInventory(Item item) {
        if (mc.player == null) return false;
        
        // Check main inventory
        for (ItemStack stack : mc.player.getInventory().main) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }
        
        // Check armor slots
        for (ItemStack stack : mc.player.getInventory().armor) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }
        
        // Check offhand
        ItemStack offhand = mc.player.getOffHandStack();
        if (!offhand.isEmpty() && offhand.getItem() == item) {
            return true;
        }
        
        return false;
    }

}