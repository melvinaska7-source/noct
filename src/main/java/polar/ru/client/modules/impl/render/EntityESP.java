package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WaterAnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.storages.implement.helpertstorages.Theme;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.ShaderUtils;
import polar.ru.api.utils.render.font.ReplaceSymbols;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.misc.NameProtect;
import polar.ru.client.modules.impl.misc.ScoreboardHP;
import polar.ru.client.modules.impl.render.SeeInvisibles;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.polar;

public class EntityESP
extends Module {
    public static EntityESP INSTANCE = new EntityESP();
    private static final float TAG_FROM_ENTITY_GAP = 0.0f;
    private static final int TAG_FONT_SIZE = 13;
    private static final int TAG_TEXT_COLOR = -1;
    private static final int TAG_HEALTH_COLOR = -43691;
    private static final int TAG_FRIEND_COLOR = -11141291;
    private static final float TAG_PANEL_RADIUS = 2.0f;
    private static final float TAG_PANEL_BLUR = 5.0f;
    private static final int TAG_PANEL_BG_COLOR = new Color(20, 20, 20, 100).getRGB();
    private static final float ARMOR_CELL_SIZE = 8.4f;
    private static final float ARMOR_ITEM_SCALE = 0.46f;
    private static final float ARMOR_CELL_GAP = 1.0f;
    private static final float PLAYER_HEAD_SIZE = 7.5f;
    private static final float PLAYER_HEAD_GAP = 3.0f;
    private static final float BOX_LINE_WIDTH = 1.5f;
    private static final float FILL_ALPHA = 0.23f;
    private static final float EPSILON = 0.001f;
    private static final long DONATE_CACHE_TTL_MS = 1000L;
    private static final long DONATE_CACHE_CLEANUP_MS = 2000L;
    private static final int MAX_ITEM_TAGS_PER_FRAME = 48;
    private static final int MAX_EFFECT_TAGS = 8;
    private final ListSetting elements = new ListSetting("Элементы", new BooleanSetting("Теги", true), new BooleanSetting("Броня", true));
    private final BooleanSetting show3DBox = new BooleanSetting("Боксы", true);
    private final BooleanSetting boxFilled = new BooleanSetting("Заполнить бокс", true);
    private final ModeSetting boxFillMode = new ModeSetting("Мод заливки", "Обычный", "Обычный", "Волны", "Нитки");
    private final FloatSetting waveSpeed = new FloatSetting("Скорость волн", 1.2f, 0.1f, 5.0f, 0.1f).visible(() -> this.boxFillMode.is("Волны"));
    private final FloatSetting waveScale = new FloatSetting("Размер волн", 1.0f, 1.0f, 3.0f, 0.1f).visible(() -> this.boxFillMode.is("Волны"));
    private final FloatSetting lineSpeed = new FloatSetting("Скорость линий", 1.4f, 0.1f, 5.0f, 0.1f).visible(() -> this.boxFillMode.getIndex() == 2);
    private final FloatSetting lineJitter = new FloatSetting("Прыжки линий", 0.55f, 0.0f, 1.5f, 0.01f).visible(() -> this.boxFillMode.getIndex() == 2);
    private final FloatSetting outline = new FloatSetting("Обводка", 1.1f, 0.1f, 5.0f, 0.1f).visible(this::isPostBoxMode);
    private final FloatSetting glow = new FloatSetting("Свечение", 1.0f, 0.0f, 5.0f, 0.1f).visible(this::isPostBoxMode);
    private final FloatSetting fill = new FloatSetting("Сила заливки", 0.6f, 0.0f, 1.0f, 0.01f).visible(this::isPostBoxMode);
    private final FloatSetting alpha = new FloatSetting("Прозрачность", 1.0f, 0.0f, 4.0f, 0.01f).visible(this::isPostBoxMode);
    private final BooleanSetting hurtTint = new BooleanSetting("Краснеть при ударе", true);
    private final BooleanSetting show2DBoxes = new BooleanSetting("2D Боксы", false);
    private final ModeSetting boxType2D = new ModeSetting("Тип 2D боксов", "Углы", "Обычный", "Углы").visible(() -> this.show2DBoxes.isState());
    private final BooleanSetting showHealthBar = new BooleanSetting("Полоска HP", false);
    private final Matrix4f lastProjectionMatrix = new Matrix4f();
    private final Quaternionf lastCameraRotation = new Quaternionf();
    private final Quaternionf lastInverseCameraRotation = new Quaternionf();
    private Vec3d lastCameraPos = Vec3d.ZERO;
    private float lastTickDelta;
    private int lastScaledWidth;
    private int lastScaledHeight;
    private boolean hasProjection;
    private Framebuffer maskBuffer;
    private final List<Framebuffer> bloomBuffers = new ArrayList<Framebuffer>();
    private final Map<UUID, DonateCache> donateCache = new HashMap<UUID, DonateCache>();
    private final Map<Integer, Float> entityHurtTintProgress = new HashMap<Integer, Float>();
    private long nextDonateCacheCleanupAt;
    private int maskWidth = -1;
    private int maskHeight = -1;
    private boolean hasShaderMask;
    private final Vector3f projectionScratch = new Vector3f();
    private final Vector4f clipScratch = new Vector4f();
    private final ProjectedPoint projectedPoint = new ProjectedPoint();
    private final ItemStack[] armorStacksScratch = new ItemStack[6];
    private final boolean[] armorHandScratch = new boolean[6];
    private int frameThemeColor = -1;
    private final BooleanSetting targetPlayers = new BooleanSetting("Игроки", true);
    private final BooleanSetting targetMobs = new BooleanSetting("Мобы", true);
    private final BooleanSetting targetAnimals = new BooleanSetting("Животные", true);
    private final BooleanSetting targetItems = new BooleanSetting("Предметы", true);
    private final ListSetting targets = new ListSetting("Отображать", this.targetPlayers, this.targetMobs, this.targetAnimals, this.targetItems);
    private final BooleanSetting viewHeldItems = new BooleanSetting("Предметы в руках", false);
    private final BooleanSetting viewEffects = new BooleanSetting("Зелья", false);
    private final BooleanSetting viewEnchants = new BooleanSetting("Зачарования", false);
    private final BooleanSetting viewDisplayName = new BooleanSetting("Визуальное имя предмета", false);

    public EntityESP() {
        super("NameTags", "Показывает игроков через стену", Module.ModuleCategory.RENDER);
        this.addSettings(this.targets, this.elements);
        this.addSettings(this.show3DBox, this.boxFilled, this.hurtTint, this.show2DBoxes, this.boxType2D, this.showHealthBar, this.viewHeldItems, this.viewEffects, this.viewEnchants, this.viewDisplayName);
    }

    @Override
    public void onDisable() {
        this.hasProjection = false;
        this.hasShaderMask = false;
        this.donateCache.clear();
        this.entityHurtTintProgress.clear();
        this.nextDonateCacheCleanupAt = 0L;
        if (this.maskBuffer != null) {
            this.maskBuffer.delete();
            this.maskBuffer = null;
        }
        for (Framebuffer fb : this.bloomBuffers) {
            fb.delete();
        }
        this.bloomBuffers.clear();
        super.onDisable();
    }

    @EventLink(priority=100)
    public void onRender3D(Event3DRender event) {
        this.hasProjection = true;
        this.lastProjectionMatrix.set((Matrix4fc)event.getProjectionMatrix());
        this.lastCameraPos = event.getCamera().getPos();
        this.lastCameraRotation.set((Quaternionfc)event.getCamera().getRotation());
        this.lastInverseCameraRotation.set((Quaternionfc)this.lastCameraRotation).conjugate();
        this.lastTickDelta = event.getTickDelta();
        this.lastScaledWidth = mc.getWindow().getScaledWidth();
        this.lastScaledHeight = mc.getWindow().getScaledHeight();
        this.frameThemeColor = this.getStableThemeColor();
        this.hasShaderMask = false;
        if (!this.show3DBox.isState() || EntityESP.mc.world == null || EntityESP.mc.player == null) {
            return;
        }
        MatrixStack matrices = event.getMatrices();
        float tickDelta = event.getTickDelta();
        boolean postMode = this.isPostBoxMode();
        boolean threadMode = this.isThreadMode();
        if (postMode) {
            this.ensureMaskBuffer();
            if (this.maskBuffer != null) {
                this.maskBuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                this.maskBuffer.clear();
                this.copyMainDepthToMask();
                this.maskBuffer.beginWrite(false);
                RenderSystem.disableBlend();
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask((boolean)false);
                RenderSystem.disableCull();
                RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
            }
        }
        for (Entity entity : EntityESP.mc.world.getEntities()) {
            if (!this.shouldProcess3DEntity(entity)) continue;
            if (postMode && this.maskBuffer != null) {
                this.drawPlayerMaskBox(matrices, entity, tickDelta);
                this.hasShaderMask = true;
                continue;
            }
            this.render3DBox(matrices, entity, tickDelta);
        }
        if (postMode && this.maskBuffer != null) {
            RenderSystem.disableBlend();
            RenderSystem.depthMask((boolean)true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            mc.getFramebuffer().beginWrite(true);
            if (this.show3DBox.isState()) {
                this.renderShaderBoxesWorldPass();
            }
        }
        if (threadMode) {
            for (Entity entity : EntityESP.mc.world.getEntities()) {
                if (!this.shouldProcess3DEntity(entity)) continue;
                this.renderThreadWeb(matrices, entity, tickDelta);
            }
        }
    }

    @EventLink(priority=100)
    public void onRender2D(EventRender.Default event) {
        if (!this.hasProjection || EntityESP.mc.world == null || EntityESP.mc.player == null) {
            return;
        }
        this.frameThemeColor = this.getStableThemeColor();
        boolean tagsEnabled = !this.elements.getSettings().isEmpty() && this.elements.getSettings().get(0).isState();
        boolean armorEnabled = this.elements.getSettings().size() > 1 && this.elements.getSettings().get(1).isState();
        boolean heldItemsEnabled = this.viewHeldItems.isState();
        boolean effectsEnabled = this.viewEffects.isState();
        boolean displayNameEnabled = this.viewDisplayName.isState();
        boolean boxes2DEnabled = this.show2DBoxes.isState();
        boolean healthBarEnabled = this.showHealthBar.isState();
        if (!(tagsEnabled || armorEnabled || heldItemsEnabled || effectsEnabled || boxes2DEnabled || healthBarEnabled)) {
            return;
        }
        Font font = tagsEnabled || heldItemsEnabled || effectsEnabled ? Fonts.getFont("sf_regular", 13) : null;
        int renderedItemTags = 0;
        for (Entity entity : EntityESP.mc.world.getEntities()) {
            LivingEntity livingEntity;
            Box interpolatedBox;
            ScreenRect rect;
            if (entity instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity)entity;
                if (!this.shouldProcess2DPlayer(player) || (rect = this.projectBox(interpolatedBox = this.getInterpolatedBox((Entity)player, this.lastTickDelta))) == null) continue;
                if (boxes2DEnabled) {
                    this.draw2DBox(event, (Entity)player, rect);
                }
                if (healthBarEnabled) {
                    this.draw2DHealthBar(event, (LivingEntity)player, rect);
                }
                if (tagsEnabled && font != null) {
                    this.drawTag(event, player, rect, font);
                }
                if (armorEnabled) {
                    this.drawArmor(event, player, rect, tagsEnabled);
                }
                if (heldItemsEnabled && font != null) {
                    this.drawHeldItems(event, (LivingEntity)player, rect, displayNameEnabled, font);
                }
                if (!effectsEnabled || font == null) continue;
                this.drawEffects(event, player, rect, font, tagsEnabled, armorEnabled);
                continue;
            }
            if (!tagsEnabled || font == null) continue;
            if (entity instanceof ItemEntity) {
                ItemEntity itemEntity = (ItemEntity)entity;
                if (!this.shouldProcessItem2D(itemEntity) || renderedItemTags >= 48 || !this.projectEntityAnchor((Entity)itemEntity, (double)itemEntity.getHeight() + 0.25, this.projectedPoint)) continue;
                this.drawDroppedItemTag(event, itemEntity, this.projectedPoint.x, this.projectedPoint.y, font);
                ++renderedItemTags;
                continue;
            }
            if (!(entity instanceof LivingEntity) || !this.shouldProcessLiving2D(livingEntity = (LivingEntity)entity) || (rect = this.projectBox(interpolatedBox = this.getInterpolatedBox((Entity)livingEntity, this.lastTickDelta))) == null) continue;
            if (boxes2DEnabled) {
                this.draw2DBox(event, (Entity)livingEntity, rect);
            }
            if (healthBarEnabled && livingEntity instanceof LivingEntity) {
                LivingEntity le = livingEntity;
                this.draw2DHealthBar(event, le, rect);
            }
            this.drawLivingTag(event, livingEntity, rect, font);
        }
    }

    /*
     * Unable to fully structure code
     */
    private void draw2DBox(EventRender.Default event, Entity entity, ScreenRect rect) {
        MatrixStack matrices = event.getContext().getMatrices();
        boolean isFriend = false;
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            if (polar.INSTANCE.friendStorage != null && polar.INSTANCE.friendStorage.isFriend(player.getName().getString())) {
                isFriend = true;
            }
        }
        int boxColor = isFriend ? -16711936 : this.frameThemeColor;
        int outlineColor = -15066598;
        float x = rect.minX();
        float y = rect.minY();
        float endX = rect.maxX();
        float endY = rect.maxY();
        boolean cornerMode = this.boxType2D.getIndex() == 1;
        if (cornerMode) {
            double distX = endX - x;
            double distY = endY - y;
            double sectX = distX * 0.2;
            double sectY = distY * 0.15;
            float t = 0.6f;
            this.drawScreenRect(matrices, x - t, y - t, x + (float)sectX, y + t, outlineColor);
            this.drawScreenRect(matrices, endX - (float)sectX, y - t, endX + t, y + t, outlineColor);
            this.drawScreenRect(matrices, x - t, endY - t, x + (float)sectX, endY + t, outlineColor);
            this.drawScreenRect(matrices, endX - (float)sectX, endY - t, endX + t, endY + t, outlineColor);
            this.drawScreenRect(matrices, x - t, y, x + t, y + (float)sectY, outlineColor);
            this.drawScreenRect(matrices, x - t, endY - (float)sectY, x + t, endY, outlineColor);
            this.drawScreenRect(matrices, endX - t, y, endX + t, y + (float)sectY, outlineColor);
            this.drawScreenRect(matrices, endX - t, endY - (float)sectY, endX + t, endY, outlineColor);
            float inner = 0.25f;
            this.drawScreenRect(matrices, x - inner, y - inner, x + (float)sectX - inner, y + inner, boxColor);
            this.drawScreenRect(matrices, endX - (float)sectX + inner, y - inner, endX + inner, y + inner, boxColor);
            this.drawScreenRect(matrices, x - inner, endY - inner, x + (float)sectX - inner, endY + inner, boxColor);
            this.drawScreenRect(matrices, endX - (float)sectX + inner, endY - inner, endX + inner, endY + inner, boxColor);
            this.drawScreenRect(matrices, x - inner, y + inner, x + inner, y + (float)sectY - inner, boxColor);
            this.drawScreenRect(matrices, x - inner, endY - (float)sectY + inner, x + inner, endY - inner, boxColor);
            this.drawScreenRect(matrices, endX - inner, y + inner, endX + inner, y + (float)sectY - inner, boxColor);
            this.drawScreenRect(matrices, endX - inner, endY - (float)sectY + inner, endX + inner, endY - inner, boxColor);
        } else {
            float t = 0.6f;
            this.drawScreenRect(matrices, x - t, y - t, endX + t, y + t, outlineColor);
            this.drawScreenRect(matrices, x - t, endY - t, endX + t, endY + t, outlineColor);
            this.drawScreenRect(matrices, x - t, y, x + t, endY, outlineColor);
            this.drawScreenRect(matrices, endX - t, y, endX + t, endY, outlineColor);
            float inner = 0.25f;
            this.drawScreenRect(matrices, x - inner, y - inner, endX + inner, y + inner, boxColor);
            this.drawScreenRect(matrices, x - inner, endY - inner, endX + inner, endY + inner, boxColor);
            this.drawScreenRect(matrices, x - inner, y, x + inner, endY, boxColor);
            this.drawScreenRect(matrices, endX - inner, y, endX + inner, endY, boxColor);
        }
    }

    /*
     * Unable to fully structure code
     */
    private void draw2DHealthBar(EventRender.Default event, LivingEntity entity, ScreenRect rect) {
        MatrixStack matrices = event.getContext().getMatrices();
        float hp = ScoreboardHP.getHealthWithAbsorption(entity);
        float maxHp = entity.getMaxHealth();
        float hpPercent = MathHelper.clamp((float)(hp / maxHp), (float)0.0f, (float)1.0f);
        float hpOffset = 3.0f;
        float barWidth = 1.0f;
        float out = 0.5f;
        float posX = rect.minX();
        float posY = rect.minY();
        float posW = rect.maxY();
        float barHeight = posW - posY;
        float filledHeight = barHeight * hpPercent;
        float barTop = posW - filledHeight;
        this.drawScreenRect(matrices, posX - hpOffset - out, posY - out, posX - hpOffset + barWidth + out, posW + out, -16777216);
        this.drawScreenRect(matrices, posX - hpOffset, posY, posX - hpOffset + barWidth, posW, -2147483648);
        boolean isFriend = false;
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            if (polar.INSTANCE.friendStorage != null && polar.INSTANCE.friendStorage.isFriend(player.getName().getString())) {
                isFriend = true;
            }
        }
        if (isFriend) {
            this.drawScreenRect(matrices, posX - hpOffset, barTop, posX - hpOffset + barWidth, posW, -16744442);
        } else {
            this.drawHealthBarGradient(matrices, posX - hpOffset, barTop, posX - hpOffset + barWidth, posW, hpPercent);
        }
    }

    private void drawHealthBarGradient(MatrixStack matrices, float left, float top, float right, float bottom, float hpPercent) {
        int color;
        if (hpPercent > 0.5f) {
            float t2 = (hpPercent - 0.5f) * 2.0f;
            int r2 = (int)(255.0f * (1.0f - t2));
            color = 0xFF000000 | r2 << 16 | 0xFF00;
        } else {
            float t3 = hpPercent * 2.0f;
            int g2 = (int)(255.0f * t3);
            color = 0xFFFF0000 | g2 << 8;
        }
        this.drawScreenRect(matrices, left, top, right, bottom, color);
    }

    private void drawScreenRect(MatrixStack matrices, float x1, float y1, float x2, float y2, int color) {
        RenderUtils.drawRoundedRect(matrices, x1, y1, x2 - x1, y2 - y1, 6.0f, color);
    }

    public List<PublicDonateSegment> getDonateSegmentsForStaffList(PlayerEntity player) {
        if (player == null) {
            return Collections.emptyList();
        }
        List<DonateSegment> internal = this.getDonateSegmentsFromTab(player);
        if (internal.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<PublicDonateSegment> result = new ArrayList<PublicDonateSegment>(internal.size());
        for (DonateSegment ds : internal) {
            result.add(new PublicDonateSegment(ds.text(), ds.color()));
        }
        return result;
    }

    private void drawTag(EventRender.Default event, PlayerEntity player, ScreenRect rect, Font font) {
        MatrixStack matrices = event.getContext().getMatrices();
        List<DonateSegment> donateSegments = this.getDonateSegmentsFromTab(player);
        String nameText = this.getProtectedName(player.getNameForScoreboard());
        float hp = ScoreboardHP.getHealthWithAbsorption((LivingEntity)player);
        String leftBracket = "";
        String hpText = Math.round(hp) + " hp";
        String rightBracket = "";
        boolean isFriend = polar.INSTANCE.friendStorage != null && polar.INSTANCE.friendStorage.isFriend(player.getName().getString());
        String friendSuffix = isFriend ? " [F]" : "";
        float donateWidth = 0.0f;
        for (DonateSegment segment : donateSegments) {
            donateWidth += font.getStringWidth(segment.text());
        }
        float totalWidth = donateWidth + font.getStringWidth(nameText) + font.getStringWidth(leftBracket) + font.getStringWidth(hpText) + font.getStringWidth(rightBracket) + font.getStringWidth(friendSuffix) + 7.5f + 3.0f - 9.0f;
        float boxHeight = 16.0f;
        float x2 = rect.centerX() - totalWidth * 0.5f;
        float y2 = this.getTagTopY(rect, boxHeight);
        float panelY = y2 - 0.5f;
        float panelHeight = boxHeight - 4.0f;
        this.drawDefaultTagPanel(matrices, x2 + 1.0f, panelY, totalWidth + 2.0f, panelHeight);
        float drawX = x2 - 7.5f + 7.5f + 3.0f;
        float textY = panelY + (panelHeight - font.getHeight()) / 2.0f;
        for (DonateSegment segment : donateSegments) {
            font.drawString(matrices, segment.text(), drawX, textY, segment.color());
            drawX += font.getStringWidth(segment.text());
        }
        font.drawString(matrices, nameText, drawX, textY, -1);
        font.drawString(matrices, leftBracket, drawX += font.getStringWidth(nameText), textY, -1);
        font.drawString(matrices, hpText, drawX += font.getStringWidth(leftBracket), textY, -43691);
        font.drawString(matrices, rightBracket, drawX += font.getStringWidth(hpText), textY, -1);
        drawX += font.getStringWidth(rightBracket);
        if (isFriend) {
            font.drawString(matrices, friendSuffix, drawX, textY, -11141291);
        }
    }

    private void drawArmor(EventRender.Default event, PlayerEntity player, ScreenRect rect, boolean tagsEnabled) {
        int i2;
        int count = 0;
        ItemStack offHand = player.getOffHandStack();
        if (!offHand.isEmpty()) {
            this.armorStacksScratch[count] = offHand;
            this.armorHandScratch[count++] = true;
        }
        for (ItemStack stack : player.getArmorItems()) {
            if (stack.isEmpty()) continue;
            this.armorStacksScratch[count] = stack;
            this.armorHandScratch[count++] = false;
        }
        ItemStack mainHand = player.getMainHandStack();
        if (!mainHand.isEmpty()) {
            this.armorStacksScratch[count] = mainHand;
            this.armorHandScratch[count++] = true;
        }
        if (count == 0) {
            return;
        }
        float iconSize = 9.0f;
        float gap = 1.0f;
        float totalWidth = (float)count * iconSize + (float)(count - 1) * gap;
        float startX = rect.centerX() - totalWidth * 0.5f;
        float y2 = tagsEnabled ? this.getTagTopY(rect, 14.0f) - 13.0f : rect.minY() - 13.0f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        for (i2 = 0; i2 < count; ++i2) {
            ItemStack stack = this.armorStacksScratch[i2];
            float itemX = startX + (float)i2 * (iconSize + gap);
            event.getContext().getMatrices().push();
            event.getContext().getMatrices().translate(itemX, y2, 0.0f);
            event.getContext().getMatrices().scale(0.5625f, 0.5625f, 1.0f);
            event.getContext().drawItem(stack, 0, 0);
            event.getContext().getMatrices().pop();
        }
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        for (i2 = 0; i2 < count; ++i2) {
            this.armorStacksScratch[i2] = ItemStack.EMPTY;
            this.armorHandScratch[i2] = false;
        }
    }

    private void drawHeldItems(EventRender.Default event, LivingEntity entity, ScreenRect rect, boolean displayNameEnabled, Font font) {
        MatrixStack matrices = event.getContext().getMatrices();
        ItemStack mainHand = entity.getMainHandStack();
        ItemStack offHand = entity.getOffHandStack();
        if (mainHand.isEmpty() && offHand.isEmpty()) {
            return;
        }
        float y2 = rect.maxY() + 3.0f;
        if (!mainHand.isEmpty()) {
            y2 = this.drawHeldItemLine(matrices, font, rect, y2, mainHand, displayNameEnabled);
        }
        if (!offHand.isEmpty()) {
            this.drawHeldItemLine(matrices, font, rect, y2, offHand, displayNameEnabled);
        }
    }

    private float drawHeldItemLine(MatrixStack matrices, Font font, ScreenRect rect, float y2, ItemStack stack, boolean displayNameEnabled) {
        String text = this.buildItemLabel(stack, displayNameEnabled);
        float width = font.getStringWidth(text);
        float x2 = rect.centerX() - width * 0.5f;
        float panelY = y2 - 0.5f;
        float panelHeight = 10.0f;
        this.drawDefaultTagPanel(matrices, x2 - 3.0f, panelY, width + 3.0f, panelHeight);
        float textY = panelY + (panelHeight - font.getHeight()) / 2.0f;
        font.drawString(matrices, text, x2, textY, -1);
        return y2 + 13.0f;
    }

    private void drawEffects(EventRender.Default event, PlayerEntity player, ScreenRect rect, Font font, boolean tagsEnabled, boolean armorEnabled) {
        Collection<StatusEffectInstance> effects = player.getStatusEffects();
        if (effects.isEmpty()) {
            return;
        }
        List<EffectLine> lines = new ArrayList();
        for (StatusEffectInstance effect : effects) {
            if (effect.getDuration() <= 20) continue;
            String baseName = I18n.translate((String)effect.getTranslationKey(), (Object[])new Object[0]);
            int amplifier = effect.getAmplifier() + 1;
            StringBuilder builder = new StringBuilder(baseName);
            if (amplifier > 1) {
                builder.append(' ').append(this.toRomanNumeral(amplifier));
            }
            builder.append(' ').append(this.formatDuration(effect.getDuration()));
            lines.add(new EffectLine(builder.toString(), this.getEffectColor(effect)));
        }
        if (lines.isEmpty()) {
            return;
        }
        if (lines.size() > 8) {
            lines = lines.subList(0, 8);
        }
        float lineHeight = font.getHeight() + 2.0f;
        float panelHeight = font.getHeight() + 1.0f;
        float anchorY = rect.minY() - 2.0f;
        if (armorEnabled) {
            anchorY = (tagsEnabled ? this.getTagTopY(rect, 14.0f) - 13.0f : rect.minY() - 13.0f) - 2.0f;
        } else if (tagsEnabled) {
            anchorY = this.getTagTopY(rect, 16.0f) - 2.0f;
        }
        float y2 = anchorY;
        MatrixStack matrices = event.getContext().getMatrices();
        for (EffectLine line : lines) {
            float width = font.getStringWidth(line.text());
            float x2 = rect.centerX() - width * 0.5f;
            float panelY = (y2 -= lineHeight) - 0.5f;
            this.drawDefaultTagPanel(matrices, x2 - 1.5f, panelY, width + 3.0f, panelHeight);
            float textY = panelY + (panelHeight - font.getHeight()) / 2.0f;
            font.drawString(matrices, line.text(), x2, textY, line.color());
        }
    }

    private String buildItemLabel(ItemStack stack, boolean displayNameEnabled) {
        Text name = displayNameEnabled ? stack.getName() : stack.getItem().getName(stack);
        String label = name != null ? name.getString() : stack.getName().getString();
        String enchants = this.getEnchantmentsText(stack);
        if (!enchants.isEmpty()) {
            label = (String)label + " " + enchants;
        }
        if (stack.getCount() > 1) {
            label = (String)label + " x" + stack.getCount();
        }
        return label;
    }

    private String getEnchantmentsText(ItemStack stack) {
        if (!this.viewEnchants.isState() || stack.isEmpty()) {
            return "";
        }
        ItemEnchantmentsComponent enchantments = (ItemEnchantmentsComponent)stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, (Object)ItemEnchantmentsComponent.DEFAULT);
        if (enchantments.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        enchantments.getEnchantments().forEach(entry -> {
            RegistryEntry registryEntry = (RegistryEntry)entry;
            int level = enchantments.getLevel((RegistryEntry)registryEntry);
            String name = ((Enchantment)registryEntry.value()).description().getString();
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(name);
            if (level > 1) {
                builder.append(' ').append(this.toRomanNumeral(level));
            }
        });
        return builder.toString();
    }

    private int getEffectColor(StatusEffectInstance effect) {
        if (effect.getEffectType() == null || effect.getEffectType().value() == null) {
            return -1;
        }
        int color = ((StatusEffect)effect.getEffectType().value()).getColor();
        return color != 0 ? 0xFF000000 | color : -1;
    }

    private String formatDuration(int duration) {
        int seconds = Math.max(0, duration / 20);
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return minutes + ":" + (String)(secs < 10 ? "0" + secs : Integer.toString(secs));
    }

    private String toRomanNumeral(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            default -> Integer.toString(value);
        };
    }

    private void drawLivingTag(EventRender.Default event, LivingEntity entity, ScreenRect rect, Font font) {
        String string;
        MatrixStack matrices = event.getContext().getMatrices();
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            string = this.getProtectedName(player.getDisplayName().getString());
        } else {
            string = entity.getDisplayName().getString();
        }
        String nameText = string;
        String hpText = Math.round(ScoreboardHP.getHealthWithAbsorption(entity)) + " hp";
        float totalWidth = font.getStringWidth(nameText) + font.getStringWidth(" ") + font.getStringWidth(hpText);
        float boxHeight = 14.0f;
        float x2 = rect.centerX() - totalWidth * 0.5f;
        float y2 = this.getTagTopY(rect, boxHeight);
        float panelY = y2 - 0.5f;
        float panelHeight = boxHeight - 4.0f;
        this.drawDefaultTagPanel(matrices, x2 - 1.0f, panelY, totalWidth + 2.0f, panelHeight);
        float textY = panelY + (panelHeight - font.getHeight()) / 2.0f;
        font.drawString(matrices, nameText, x2, textY, -1);
        font.drawString(matrices, hpText, x2 + font.getStringWidth(nameText) + font.getStringWidth(" "), textY, -43691);
    }

    private void drawDroppedItemTag(EventRender.Default event, ItemEntity itemEntity, float anchorX, float anchorY, Font font) {
        MatrixStack matrices = event.getContext().getMatrices();
        ItemStack stack = itemEntity.getStack();
        String countText = stack.getCount() + "x";
        List<DonateSegment> itemSegments = this.getStyledTextSegments(stack.getName(), this.getDroppedItemTextColor(stack));
        int countColor = ColorUtils.rgba(155, 155, 155, 255);
        float itemNameWidth = 0.0f;
        for (DonateSegment segment : itemSegments) {
            itemNameWidth += font.getStringWidth(segment.text());
        }
        float spaceWidth = font.getStringWidth(" ");
        float totalWidth = itemNameWidth + spaceWidth + font.getStringWidth(countText);
        float boxHeight = 14.0f;
        float x2 = anchorX - totalWidth * 0.5f;
        float y2 = anchorY - boxHeight - 2.0f;
        float panelY = y2 - 0.5f;
        float panelHeight = boxHeight - 3.0f;
        this.drawDefaultTagPanel(matrices, x2 - 2.0f, panelY, totalWidth + 4.0f, panelHeight);
        float drawX = x2;
        float textY = panelY + (panelHeight - font.getHeight()) / 2.0f;
        for (DonateSegment segment : itemSegments) {
            font.drawString(matrices, segment.text(), drawX, textY, segment.color());
            drawX += font.getStringWidth(segment.text());
        }
        font.drawString(matrices, countText, drawX + spaceWidth, textY, countColor);
    }

    private int getMinecraftItemNameColor(ItemStack stack) {
        Text name = stack.getName();
        if (name != null) {
            int[] discoveredColor = new int[]{0};
            boolean[] found = new boolean[]{false};
            name.visit((style, string) -> {
                if (!found[0] && style != null && style.getColor() != null) {
                    discoveredColor[0] = 0xFF000000 | style.getColor().getRgb();
                    found[0] = true;
                }
                return found[0] ? Optional.of(string) : Optional.empty();
            }, Style.EMPTY);
            if (found[0]) {
                return discoveredColor[0];
            }
        }
        return switch (stack.getRarity()) {
            default -> throw new MatchException(null, null);
            case Rarity.UNCOMMON -> ColorUtils.rgba(255, 255, 85, 255);
            case Rarity.RARE -> ColorUtils.rgba(85, 255, 255, 255);
            case Rarity.EPIC -> ColorUtils.rgba(255, 85, 255, 255);
            case Rarity.COMMON -> -1;
        };
    }

    private int getDroppedItemTextColor(ItemStack stack) {
        return this.getMinecraftItemNameColor(stack);
    }

    private boolean isNetheriteItem(Item item) {
        return Registries.ITEM.getId(item).getPath().contains("netherite");
    }

    private void drawDefaultTagPanel(MatrixStack matrices, float x2, float y2, float width, float height) {
        this.drawTagPanelNoShadow(matrices, x2, y2, width, height);
    }

    private void drawTagPanelNoShadow(MatrixStack matrices, float x2, float y2, float width, float height) {
        RenderUtils.drawBlur(matrices, x2, y2, width, height, 2.0f, 5.0f, ColorUtils.rgba(255, 255, 255, 255));
        RenderUtils.drawBlur(matrices, x2, y2, width, height, 2.0f, 5.0f, ColorUtils.rgba(0, 0, 0, 180));
        RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, 2.0f, TAG_PANEL_BG_COLOR);
    }

    private float getTagTopY(ScreenRect rect, float tagHeight) {
        return rect.minY() - tagHeight - 0.0f;
    }

    private String[] getNameVariants(PlayerEntity player) {
        String profileName = player.getGameProfile() != null ? player.getGameProfile().getName() : "";
        String scoreboardName = player.getNameForScoreboard();
        String protectedScoreboardName = this.getProtectedName(scoreboardName);
        String protectedProfileName = this.getProtectedName(profileName);
        String protectedPlainName = this.getProtectedName(player.getName().getString());
        return new String[]{player.getName().getString(), protectedPlainName, scoreboardName, protectedScoreboardName, profileName, protectedProfileName};
    }

    private String getProtectedName(String input) {
        NameProtect nameProtect;
        NameProtect nameProtect2 = nameProtect = ModuleClass.INSTANCE != null ? ModuleClass.nameProtect : null;
        if (nameProtect == null || !nameProtect.isEnable()) {
            return input;
        }
        return nameProtect.patch(input);
    }

    private int findAnyNameIndex(String text, String[] names) {
        if (text == null || text.isEmpty() || names == null) {
            return -1;
        }
        int best = -1;
        for (String name : names) {
            int idx;
            if (name == null || name.isEmpty() || (idx = this.indexOfIgnoreCase(text, name)) < 0 || best != -1 && idx >= best) continue;
            best = idx;
        }
        return best;
    }

    private int indexOfIgnoreCase(String text, String search) {
        if (text == null || search == null || search.isEmpty()) {
            return -1;
        }
        int limit = text.length() - search.length();
        for (int i2 = 0; i2 <= limit; ++i2) {
            if (!text.regionMatches(true, i2, search, 0, search.length())) continue;
            return i2;
        }
        return -1;
    }

    private void trimSegmentsToLength(List<DonateSegment> segments, int maxLength) {
        int remaining = Math.max(0, maxLength);
        ArrayList<DonateSegment> trimmed = new ArrayList<DonateSegment>();
        for (DonateSegment seg : segments) {
            if (remaining <= 0) break;
            String text = seg.text();
            if (text.length() <= remaining) {
                trimmed.add(seg);
                remaining -= text.length();
                continue;
            }
            trimmed.add(new DonateSegment(text.substring(0, remaining), seg.color()));
            remaining = 0;
        }
        segments.clear();
        segments.addAll(trimmed);
    }

    private List<DonateSegment> getDonateSegmentsFromTab(PlayerEntity player) {
        long now = System.currentTimeMillis();
        DonateCache cache = this.donateCache.computeIfAbsent(player.getUuid(), uuid -> new DonateCache());
        if (now < cache.nextUpdateAt) {
            return cache.segments;
        }
        ArrayList<DonateSegment> segments = new ArrayList<DonateSegment>();
        if (mc.getNetworkHandler() == null) {
            cache.segments = Collections.emptyList();
            cache.nextUpdateAt = now + 1000L;
            return cache.segments;
        }
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry == null) {
            cache.segments = Collections.emptyList();
            cache.nextUpdateAt = now + 1000L;
            return cache.segments;
        }
        Text displayName = entry.getDisplayName();
        if (displayName == null) {
            displayName = player.getDisplayName();
        }
        if (displayName == null) {
            cache.segments = Collections.emptyList();
            cache.nextUpdateAt = now + 1000L;
            return cache.segments;
        }
        String[] nameVariants = this.getNameVariants(player);
        boolean[] foundName = new boolean[]{false};
        displayName.visit((style, string) -> {
            String donatePart;
            if (foundName[0] || string == null || string.isEmpty()) {
                return Optional.empty();
            }
            String part = string.replace('\n', ' ').replace('\r', ' ');
            int nameIndex = this.findAnyNameIndex(part, nameVariants);
            String string2 = donatePart = nameIndex >= 0 ? part.substring(0, nameIndex) : part;
            if (!donatePart.isEmpty()) {
                int baseColor = style.getColor() != null ? style.getColor().getRgb() : 0xFFFFFF;
                this.appendColoredSegments(segments, donatePart, baseColor);
            }
            if (nameIndex >= 0) {
                foundName[0] = true;
            }
            return Optional.empty();
        }, Style.EMPTY);
        if (!foundName[0]) {
            segments.clear();
            Team team = player.getScoreboardTeam();
            if (team != null && team.getPrefix() != null) {
                this.appendTextSegments(segments, team.getPrefix());
            }
        }
        if (segments.isEmpty()) {
            cache.segments = Collections.emptyList();
            cache.nextUpdateAt = now + 1000L;
            this.cleanupDonateCache(now);
            return cache.segments;
        }
        StringBuilder combined = new StringBuilder();
        for (DonateSegment seg : segments) {
            combined.append(seg.text());
        }
        int donateNameIndex = this.findAnyNameIndex(combined.toString(), nameVariants);
        if (donateNameIndex >= 0) {
            if (donateNameIndex == 0) {
                cache.segments = Collections.emptyList();
                cache.nextUpdateAt = now + 1000L;
                this.cleanupDonateCache(now);
                return cache.segments;
            }
            this.trimSegmentsToLength(segments, donateNameIndex);
        }
        if (segments.isEmpty()) {
            cache.segments = Collections.emptyList();
            cache.nextUpdateAt = now + 1000L;
            this.cleanupDonateCache(now);
            return cache.segments;
        }
        StringBuilder textCheck = new StringBuilder();
        for (DonateSegment seg : segments) {
            textCheck.append(seg.text());
        }
        if (textCheck.toString().trim().isEmpty()) {
            cache.segments = Collections.emptyList();
            cache.nextUpdateAt = now + 1000L;
            this.cleanupDonateCache(now);
            return cache.segments;
        }
        DonateSegment last = (DonateSegment)segments.get(segments.size() - 1);
        if (!last.text().endsWith(" ")) {
            segments.set(segments.size() - 1, new DonateSegment(last.text() + " ", last.color()));
        }
        cache.segments = List.copyOf(segments);
        cache.nextUpdateAt = now + 1000L;
        this.cleanupDonateCache(now);
        return cache.segments;
    }

    private void appendTextSegments(List<DonateSegment> out, Text text) {
        text.visit((style, string) -> {
            if (string == null || string.isEmpty()) {
                return Optional.empty();
            }
            int baseColor = style.getColor() != null ? style.getColor().getRgb() : 0xFFFFFF;
            this.appendColoredSegments(out, string.replace('\n', ' ').replace('\r', ' '), baseColor);
            return Optional.empty();
        }, Style.EMPTY);
    }

    private List<DonateSegment> getStyledTextSegments(Text text, int fallbackColor) {
        ArrayList<DonateSegment> segments = new ArrayList<DonateSegment>();
        if (text != null) {
            this.appendTextSegments(segments, text);
        }
        if (segments.isEmpty() && text != null && !text.getString().isEmpty()) {
            segments.add(new DonateSegment(text.getString(), fallbackColor));
        }
        return segments;
    }

    private void appendColoredSegments(List<DonateSegment> out, String text, int baseColor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int currentColor = baseColor;
        StringBuilder chunk = new StringBuilder();
        int chunkColor = currentColor;
        int offset = 0;
        while (offset < text.length()) {
            int codePoint = text.codePointAt(offset);
            int charCount = Character.charCount(codePoint);
            if (codePoint == 167 && offset + charCount < text.length()) {
                this.flushSegment(out, chunk, chunkColor);
                char code = Character.toLowerCase(text.charAt(offset + charCount));
                Integer mappedColor = this.sectionColorToRgb(code);
                if (mappedColor != null) {
                    currentColor = mappedColor;
                } else if (code == 'r') {
                    currentColor = baseColor;
                }
                chunkColor = currentColor;
                offset += charCount + 1;
                continue;
            }
            String replacement = ReplaceSymbols.replaceCodePoint(codePoint);
            if (replacement != null) {
                this.flushSegment(out, chunk, chunkColor);
                int totalChars = Math.max(1, replacement.length());
                for (int i2 = 0; i2 < replacement.length(); ++i2) {
                    int gradientColor = ReplaceSymbols.getGradientColorForReplacement(codePoint, i2, totalChars, 1.0f, currentColor);
                    if (chunk.length() > 0 && chunkColor != gradientColor) {
                        this.flushSegment(out, chunk, chunkColor);
                    }
                    chunkColor = gradientColor;
                    chunk.append(replacement.charAt(i2));
                }
                offset += charCount;
                continue;
            }
            if (chunk.length() > 0 && chunkColor != currentColor) {
                this.flushSegment(out, chunk, chunkColor);
            }
            chunkColor = currentColor;
            chunk.appendCodePoint(codePoint);
            offset += charCount;
        }
        this.flushSegment(out, chunk, chunkColor);
    }

    private void flushSegment(List<DonateSegment> out, StringBuilder chunk, int color) {
        if (chunk.isEmpty()) {
            return;
        }
        out.add(new DonateSegment(chunk.toString(), color));
        chunk.setLength(0);
    }

    private Integer sectionColorToRgb(char code) {
        return switch (code) {
            case '0' -> 0;
            case '1' -> 170;
            case '2' -> 43520;
            case '3' -> 43690;
            case '4' -> 0xAA0000;
            case '5' -> 0xAA00AA;
            case '6' -> 0xFFAA00;
            case '7' -> 0xAAAAAA;
            case '8' -> 0x555555;
            case '9' -> 0x5555FF;
            case 'a' -> 0x55FF55;
            case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555;
            case 'd' -> 0xFF55FF;
            case 'e' -> 0xFFFF55;
            case 'f' -> 0xFFFFFF;
            default -> null;
        };
    }

    private void cleanupDonateCache(long now) {
        if (now < this.nextDonateCacheCleanupAt || EntityESP.mc.world == null) {
            return;
        }
        this.nextDonateCacheCleanupAt = now + 2000L;
        this.donateCache.entrySet().removeIf(entry -> EntityESP.mc.world.getPlayerByUuid((UUID)entry.getKey()) == null);
    }

    private Box getInterpolatedBox(Entity entity, float tickDelta) {
        double x2 = MathHelper.lerp((double)tickDelta, (double)entity.lastRenderX, (double)entity.getX());
        double y2 = MathHelper.lerp((double)tickDelta, (double)entity.lastRenderY, (double)entity.getY());
        double z2 = MathHelper.lerp((double)tickDelta, (double)entity.lastRenderZ, (double)entity.getZ());
        double ox = x2 - entity.getX();
        double oy = y2 - entity.getY();
        double oz = z2 - entity.getZ();
        return entity.getBoundingBox().offset(ox, oy, oz).expand(0.05);
    }

    private ScreenRect projectBox(Box box) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        boolean projectedAny = false;
        for (int xi = 0; xi < 2; ++xi) {
            for (int yi = 0; yi < 2; ++yi) {
                for (int zi = 0; zi < 2; ++zi) {
                    if (!this.projectToScreen(xi == 0 ? box.minX : box.maxX, yi == 0 ? box.minY : box.maxY, zi == 0 ? box.minZ : box.maxZ, this.projectedPoint)) continue;
                    projectedAny = true;
                    minX = Math.min(minX, (double)this.projectedPoint.x);
                    minY = Math.min(minY, (double)this.projectedPoint.y);
                    maxX = Math.max(maxX, (double)this.projectedPoint.x);
                    maxY = Math.max(maxY, (double)this.projectedPoint.y);
                }
            }
        }
        if (!projectedAny) {
            return null;
        }
        if (minX > (double)(mc.getWindow().getScaledWidth() + 300) || maxX < -300.0) {
            return null;
        }
        if (minY > (double)(mc.getWindow().getScaledHeight() + 300) || maxY < -300.0) {
            return null;
        }
        if (maxX - minX < 2.0 || maxY - minY < 2.0) {
            return null;
        }
        return new ScreenRect((float)minX, (float)minY, (float)maxX, (float)maxY);
    }

    private boolean projectToScreen(double worldX, double worldY, double worldZ, ProjectedPoint out) {
        this.projectionScratch.set((float)(worldX - this.lastCameraPos.x), (float)(worldY - this.lastCameraPos.y), (float)(worldZ - this.lastCameraPos.z));
        this.projectionScratch.rotate((Quaternionfc)this.lastInverseCameraRotation);
        this.clipScratch.set(this.projectionScratch.x, this.projectionScratch.y, this.projectionScratch.z, 1.0f);
        this.lastProjectionMatrix.transform(this.clipScratch);
        float w2 = this.clipScratch.w;
        if (w2 <= 1.0E-5f) {
            return false;
        }
        float ndcX = this.clipScratch.x / w2;
        float ndcY = this.clipScratch.y / w2;
        float ndcZ = this.clipScratch.z / w2;
        float screenX = (ndcX * 0.5f + 0.5f) * (float)this.lastScaledWidth;
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * (float)this.lastScaledHeight;
        if (Float.isNaN(screenX) || Float.isNaN(screenY)) {
            return false;
        }
        if (Float.isInfinite(screenX) || Float.isInfinite(screenY)) {
            return false;
        }
        out.x = screenX;
        out.y = screenY;
        out.z = ndcZ;
        return true;
    }

    private boolean projectEntityAnchor(Entity entity, double yOffset, ProjectedPoint out) {
        double x2 = MathHelper.lerp((double)this.lastTickDelta, (double)entity.lastRenderX, (double)entity.getX());
        double y2 = MathHelper.lerp((double)this.lastTickDelta, (double)entity.lastRenderY, (double)entity.getY()) + yOffset;
        double z2 = MathHelper.lerp((double)this.lastTickDelta, (double)entity.lastRenderZ, (double)entity.getZ());
        return this.projectToScreen(x2, y2, z2, out);
    }

    private boolean isInFirstPerson() {
        return mc != null && EntityESP.mc.gameRenderer != null && !EntityESP.mc.gameRenderer.getCamera().isThirdPerson();
    }

    private boolean shouldProcess3DEntity(Entity entity) {
        LivingEntity livingEntity;
        if (entity == null || entity.isRemoved() || entity instanceof ArmorStandEntity) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            return this.shouldProcessPlayer(player, false);
        }
        if (entity instanceof ItemEntity) {
            ItemEntity itemEntity = (ItemEntity)entity;
            return this.targetItems.isState() && itemEntity.isAlive();
        }
        if (!(entity instanceof LivingEntity) || !(livingEntity = (LivingEntity)entity).isAlive()) {
            return false;
        }
        if (this.isAnimalEntity(entity)) {
            return this.targetAnimals.isState();
        }
        if (this.isMobEntity(entity)) {
            return this.targetMobs.isState();
        }
        return false;
    }

    private boolean shouldProcess2DPlayer(PlayerEntity player) {
        return this.shouldProcessPlayer(player, true);
    }

    private boolean shouldProcessLiving2D(LivingEntity entity) {
        return this.shouldProcess3DEntity((Entity)entity);
    }

    private boolean shouldProcessItem2D(ItemEntity itemEntity) {
        return this.targetItems.isState() && itemEntity.isAlive();
    }

    private boolean shouldProcessPlayer(PlayerEntity player, boolean skipInvisible) {
        if (!this.targetPlayers.isState()) {
            return false;
        }
        if (player == null || !player.isAlive()) {
            return false;
        }
        if (player == EntityESP.mc.player && this.isInFirstPerson()) {
            return false;
        }
        return !skipInvisible || !player.isInvisible() || this.canRenderInvisiblePlayer(player);
    }

    private boolean isTargetEnabled(int index) {
        return this.targets.getSettings().size() > index && this.targets.getSettings().get(index).isState();
    }

    private boolean isAnimalEntity(Entity entity) {
        return entity instanceof AnimalEntity || entity instanceof WaterAnimalEntity || entity instanceof AmbientEntity;
    }

    private boolean isMobEntity(Entity entity) {
        return entity instanceof MobEntity && !this.isAnimalEntity(entity) && !(entity instanceof PlayerEntity);
    }

    private boolean canRenderInvisiblePlayer(PlayerEntity player) {
        SeeInvisibles seeInvisibles = ModuleClass.seeInvisibles;
        return seeInvisibles != null && seeInvisibles.shouldRenderInvisible(player);
    }

    private boolean isOutsideRenderDistance(Entity entity) {
        int viewDistanceChunks = (Integer)EntityESP.mc.options.getViewDistance().getValue();
        double maxDistance = Math.max(48.0, (double)viewDistanceChunks * 16.0 + 16.0);
        return entity.squaredDistanceTo(this.lastCameraPos) > maxDistance * maxDistance;
    }

    /*
     * Unable to fully structure code
     */
    private void render3DBox(MatrixStack matrices, Entity entity, float tickDelta) {
        Vec3d camera = EntityESP.mc.gameRenderer.getCamera().getPos();
        double x = MathHelper.lerp((double)tickDelta, (double)entity.lastRenderX, (double)entity.getX()) - camera.x;
        double y = MathHelper.lerp((double)tickDelta, (double)entity.lastRenderY, (double)entity.getY()) - camera.y;
        double z = MathHelper.lerp((double)tickDelta, (double)entity.lastRenderZ, (double)entity.getZ()) - camera.z;
        Box box = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ());
        matrices.push();
        matrices.translate(x, y, z);
        boolean isFriend = false;
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            if (polar.INSTANCE.friendStorage != null && polar.INSTANCE.friendStorage.isFriend(player.getName().getString())) {
                isFriend = true;
            }
        }
        int boxColor = isFriend ? ColorUtils.rgba(84, 255, 84, 255) : this.getStableThemeColor();
        boxColor = this.applyEntityHurtTint(entity, boxColor);
        float r = ColorUtils.redf(boxColor);
        float g = ColorUtils.greenf(boxColor);
        float b = ColorUtils.bluef(boxColor);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth((float)1.5f);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        if (this.boxFilled.isState()) {
            this.drawFilledBox(tessellator, matrix, box, r, g, b, 0.23f);
        }
        this.drawBoxOutline(tessellator, matrix, box, r, g, b, 1.0f);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private int applyEntityHurtTint(Entity entity, int baseColor) {
        if (!(entity instanceof LivingEntity) || !this.hurtTint.isState()) {
            this.entityHurtTintProgress.remove(entity.getId());
            return baseColor;
        }
        LivingEntity livingEntity = (LivingEntity)entity;
        float target = MathHelper.clamp((float)((float)livingEntity.hurtTime / 10.0f), 0.0f, 1.0f);
        float current = this.entityHurtTintProgress.getOrDefault(entity.getId(), Float.valueOf(0.0f)).floatValue();
        float speed = target > current ? 0.38f : 0.16f;
        current += (target - current) * speed;
        if (current <= 0.003f && target <= 0.0f) {
            this.entityHurtTintProgress.remove(entity.getId());
            return baseColor;
        }
        this.entityHurtTintProgress.put(entity.getId(), Float.valueOf(current));
        int hitColor = ColorUtils.rgba(255, 70, 70, 255);
        return ColorUtils.interpolateColor(baseColor, hitColor, current);
    }

    private void drawPlayerMaskBox(MatrixStack matrices, Entity entity, float tickDelta) {
        Vec3d camera = EntityESP.mc.gameRenderer.getCamera().getPos();
        double x2 = MathHelper.lerp((double)tickDelta, (double)entity.lastRenderX, (double)entity.getX()) - camera.x;
        double y2 = MathHelper.lerp((double)tickDelta, (double)entity.lastRenderY, (double)entity.getY()) - camera.y;
        double z2 = MathHelper.lerp((double)tickDelta, (double)entity.lastRenderZ, (double)entity.getZ()) - camera.z;
        Box box = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ());
        matrices.push();
        matrices.translate(x2, y2, z2);
        this.drawMaskBox(Tessellator.getInstance(), matrices.peek().getPositionMatrix(), box);
        matrices.pop();
    }

    private void drawMaskBox(Tessellator tessellator, Matrix4f matrix, Box box) {
        BufferBuilder b2 = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float minX = (float)box.minX;
        float minY = (float)box.minY;
        float minZ = (float)box.minZ;
        float maxX = (float)box.maxX;
        float maxY = (float)box.maxY;
        float maxZ = (float)box.maxZ;
        int white = -1;
        b2.vertex(matrix, minX, minY, minZ).color(white);
        b2.vertex(matrix, maxX, minY, minZ).color(white);
        b2.vertex(matrix, maxX, minY, maxZ).color(white);
        b2.vertex(matrix, minX, minY, maxZ).color(white);
        b2.vertex(matrix, minX, maxY, minZ).color(white);
        b2.vertex(matrix, minX, maxY, maxZ).color(white);
        b2.vertex(matrix, maxX, maxY, maxZ).color(white);
        b2.vertex(matrix, maxX, maxY, minZ).color(white);
        b2.vertex(matrix, minX, minY, minZ).color(white);
        b2.vertex(matrix, minX, maxY, minZ).color(white);
        b2.vertex(matrix, maxX, maxY, minZ).color(white);
        b2.vertex(matrix, maxX, minY, minZ).color(white);
        b2.vertex(matrix, minX, minY, maxZ).color(white);
        b2.vertex(matrix, maxX, minY, maxZ).color(white);
        b2.vertex(matrix, maxX, maxY, maxZ).color(white);
        b2.vertex(matrix, minX, maxY, maxZ).color(white);
        b2.vertex(matrix, minX, minY, minZ).color(white);
        b2.vertex(matrix, minX, minY, maxZ).color(white);
        b2.vertex(matrix, minX, maxY, maxZ).color(white);
        b2.vertex(matrix, minX, maxY, minZ).color(white);
        b2.vertex(matrix, maxX, minY, minZ).color(white);
        b2.vertex(matrix, maxX, maxY, minZ).color(white);
        b2.vertex(matrix, maxX, maxY, maxZ).color(white);
        b2.vertex(matrix, maxX, minY, maxZ).color(white);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)b2.end());
    }

    private void renderShaderBoxes() {
        if (!this.hasShaderMask || this.maskBuffer == null) {
            return;
        }
        boolean lineMode = this.isThreadMode();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.blockOverlay);
        if (shader == null) {
            return;
        }
        int color1 = this.getStableThemeColor();
        int color2 = this.isRainbowTheme() ? ColorUtils.getThemeColor(180) : color1;
        mc.getFramebuffer().beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.blockOverlay);
        RenderSystem.setShaderTexture((int)0, (int)this.maskBuffer.getColorAttachment());
        this.setUniform(shader, "texelSize", 1.0f / (float)Math.max(1, mc.getWindow().getFramebufferWidth()), 1.0f / (float)Math.max(1, mc.getWindow().getFramebufferHeight()));
        this.setUniform(shader, "color", ColorUtils.redf(color1), ColorUtils.greenf(color1), ColorUtils.bluef(color1));
        this.setUniform(shader, "color2", ColorUtils.redf(color2), ColorUtils.greenf(color2), ColorUtils.bluef(color2));
        this.setUniform(shader, "time", (float)(System.currentTimeMillis() % 100000L) / 1000.0f);
        this.setUniform(shader, "speed", this.waveSpeed.get());
        this.setUniform(shader, "scale", this.waveScale.get());
        this.setUniform(shader, "outline", this.outline.get());
        this.setUniform(shader, "glow", lineMode ? 0.0f : this.glow.get());
        this.setUniform(shader, "fill", lineMode ? 0.0f : this.fill.get());
        this.setUniform(shader, "alpha", lineMode ? 1.0f : this.alpha.get());
        this.setUniform(shader, "outlineOnly", lineMode ? 1.0f : 0.0f);
        this.drawFullscreenQuad();
        if (this.glow.get() > 0.001f) {
            int blurredMask = this.runKawaseBloom(Math.max(3, Math.min(8, 4 + Math.round(this.outline.get() * 0.7f))));
            ShaderProgram glowShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderHandsGlow);
            if (glowShader != null) {
                RenderSystem.blendFuncSeparate((GlStateManager.SrcFactor)GlStateManager.SrcFactor.SRC_ALPHA, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE, (GlStateManager.SrcFactor)GlStateManager.SrcFactor.ZERO, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE);
                RenderSystem.setShader((ShaderProgramKey)ShaderUtils.shaderHandsGlow);
                RenderSystem.setShaderTexture((int)0, (int)blurredMask);
                RenderSystem.setShaderTexture((int)1, (int)this.maskBuffer.getColorAttachment());
                this.setUniform(glowShader, "color", ColorUtils.redf(color1), ColorUtils.greenf(color1), ColorUtils.bluef(color1));
                this.setUniform(glowShader, "color2", ColorUtils.redf(color2), ColorUtils.greenf(color2), ColorUtils.bluef(color2));
                this.setUniform(glowShader, "exposure", 1.0f + this.glow.get() * 1.8f);
                this.drawFullscreenQuad();
            }
        }
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture((int)0, (int)0);
        RenderSystem.setShaderTexture((int)1, (int)0);
        mc.getFramebuffer().beginWrite(true);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderShaderBoxesWorldPass() {
        if (!this.isPostBoxMode()) {
            return;
        }
        Matrix4f savedProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        float width = Math.max(mc.getWindow().getScaledWidth(), 1);
        float height = Math.max(mc.getWindow().getScaledHeight(), 1);
        Matrix4f ortho = new Matrix4f().setOrtho(0.0f, width, height, 0.0f, -1000.0f, 1000.0f);
        RenderSystem.setProjectionMatrix((Matrix4f)ortho, (ProjectionType)ProjectionType.ORTHOGRAPHIC);
        try {
            this.renderShaderBoxes();
        }
        finally {
            RenderSystem.setProjectionMatrix((Matrix4f)savedProjection, (ProjectionType)ProjectionType.ORTHOGRAPHIC);
        }
    }

    private int runKawaseBloom(int iterations) {
        Framebuffer dst;
        int i2;
        this.ensureBloomBuffers(iterations);
        if (this.bloomBuffers.isEmpty()) {
            return this.maskBuffer.getColorAttachment();
        }
        int currentTexture = this.maskBuffer.getColorAttachment();
        ShaderProgram downShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderHandsKawaseDown);
        ShaderProgram upShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderHandsKawaseUp);
        if (downShader == null || upShader == null) {
            return currentTexture;
        }
        for (i2 = 0; i2 < iterations; ++i2) {
            dst = this.bloomBuffers.get(i2);
            dst.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            dst.clear();
            dst.beginWrite(true);
            RenderSystem.setShader((ShaderProgramKey)ShaderUtils.shaderHandsKawaseDown);
            RenderSystem.setShaderTexture((int)0, (int)currentTexture);
            this.setHandsKawaseUniforms(downShader, dst.textureWidth, dst.textureHeight, 1.0f + (float)i2);
            this.drawFullscreenQuad();
            currentTexture = dst.getColorAttachment();
        }
        for (i2 = iterations - 1; i2 >= 1; --i2) {
            dst = this.bloomBuffers.get(i2 - 1);
            dst.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            dst.clear();
            dst.beginWrite(true);
            RenderSystem.setShader((ShaderProgramKey)ShaderUtils.shaderHandsKawaseUp);
            RenderSystem.setShaderTexture((int)0, (int)currentTexture);
            this.setHandsKawaseUniforms(upShader, dst.textureWidth, dst.textureHeight, 1.0f + (float)i2);
            this.setUniform(upShader, "color", 1.0f, 1.0f, 1.0f);
            this.drawFullscreenQuad();
            currentTexture = dst.getColorAttachment();
        }
        mc.getFramebuffer().beginWrite(true);
        return currentTexture;
    }

    private void ensureMaskBuffer() {
        int w2 = mc.getWindow().getFramebufferWidth();
        int h2 = mc.getWindow().getFramebufferHeight();
        if (this.maskBuffer == null || this.maskWidth != w2 || this.maskHeight != h2) {
            if (this.maskBuffer != null) {
                this.maskBuffer.delete();
            }
            this.maskBuffer = new SimpleFramebuffer(w2, h2, true);
            this.maskWidth = w2;
            this.maskHeight = h2;
            for (Framebuffer fb : this.bloomBuffers) {
                fb.delete();
            }
            this.bloomBuffers.clear();
        }
    }

    private void ensureBloomBuffers(int iterations) {
        while (this.bloomBuffers.size() > iterations) {
            int last = this.bloomBuffers.size() - 1;
            this.bloomBuffers.get(last).delete();
            this.bloomBuffers.remove(last);
        }
        for (int i2 = 0; i2 < iterations; ++i2) {
            int w2 = Math.max(2, this.maskWidth >> i2 + 1);
            int h2 = Math.max(2, this.maskHeight >> i2 + 1);
            if (i2 >= this.bloomBuffers.size()) {
                this.bloomBuffers.add((Framebuffer)new SimpleFramebuffer(w2, h2, false));
                continue;
            }
            Framebuffer fb = this.bloomBuffers.get(i2);
            if (fb.textureWidth == w2 && fb.textureHeight == h2) continue;
            fb.delete();
            this.bloomBuffers.set(i2, (Framebuffer)new SimpleFramebuffer(w2, h2, false));
        }
    }

    private void copyMainDepthToMask() {
        if (this.maskBuffer == null) {
            return;
        }
        int readFbo = GL11.glGetInteger((int)36010);
        int drawFbo = GL11.glGetInteger((int)36006);
        int w2 = mc.getWindow().getFramebufferWidth();
        int h2 = mc.getWindow().getFramebufferHeight();
        GL30.glBindFramebuffer((int)36008, (int)EntityESP.mc.getFramebuffer().fbo);
        GL30.glBindFramebuffer((int)36009, (int)this.maskBuffer.fbo);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)w2, (int)h2, (int)0, (int)0, (int)w2, (int)h2, (int)256, (int)9728);
        GL30.glBindFramebuffer((int)36008, (int)readFbo);
        GL30.glBindFramebuffer((int)36009, (int)drawFbo);
    }

    private void setUniform(ShaderProgram shader, String name, float value) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private void setUniform(ShaderProgram shader, String name, float x2, float y2) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x2, y2);
        }
    }

    private void setUniform(ShaderProgram shader, String name, float x2, float y2, float z2) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x2, y2, z2);
        }
    }

    private void setHandsKawaseUniforms(ShaderProgram shader, int texWidth, int texHeight, float offset) {
        this.setUniform(shader, "uSize", Math.max(1, texWidth), Math.max(1, texHeight));
        this.setUniform(shader, "uOffset", offset, offset);
        this.setUniform(shader, "uHalfPixel", 0.5f / (float)Math.max(1, texWidth), 0.5f / (float)Math.max(1, texHeight));
    }

    private void drawFullscreenQuad() {
        float width = Math.max(mc.getWindow().getScaledWidth(), 1);
        float height = Math.max(mc.getWindow().getScaledHeight(), 1);
        BufferBuilder b2 = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b2.vertex(0.0f, 0.0f, 0.0f).texture(0.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        b2.vertex(0.0f, height, 0.0f).texture(0.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        b2.vertex(width, height, 0.0f).texture(1.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        b2.vertex(width, 0.0f, 0.0f).texture(1.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)b2.end());
    }

    private boolean isPostBoxMode() {
        return false;
    }

    private boolean isThreadMode() {
        return false;
    }

    private boolean isRainbowTheme() {
        if (polar.INSTANCE == null || polar.INSTANCE.themeStorage == null || polar.INSTANCE.themeStorage.getThemes() == null) {
            return false;
        }
        Theme theme = polar.INSTANCE.themeStorage.getThemes().getTheme();
        return theme != null && "Rainbow".equals(theme.getName());
    }

    private int getStableThemeColor() {
        if (polar.INSTANCE == null || polar.INSTANCE.themeStorage == null || polar.INSTANCE.themeStorage.getThemes() == null) {
            return ColorUtils.getThemeColor(0);
        }
        Theme theme = polar.INSTANCE.themeStorage.getThemes().getTheme();
        if (theme == null || theme.color == null || theme.color.length == 0) {
            return ColorUtils.getThemeColor(0);
        }
        return theme.color[0];
    }

    private void renderThreadWeb(MatrixStack matrices, Entity entity, float tickDelta) {
        Vec3d camera = EntityESP.mc.gameRenderer.getCamera().getPos();
        double x2 = MathHelper.lerp((double)tickDelta, (double)entity.lastRenderX, (double)entity.getX()) - camera.x;
        double y2 = MathHelper.lerp((double)tickDelta, (double)entity.lastRenderY, (double)entity.getY()) - camera.y;
        double z2 = MathHelper.lerp((double)tickDelta, (double)entity.lastRenderZ, (double)entity.getZ()) - camera.z;
        Box box = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ());
        matrices.push();
        matrices.translate(x2, y2, z2);
        this.drawAnimatedWeb(matrices.peek().getPositionMatrix(), box, entity.getId());
        matrices.pop();
    }

    private void drawAnimatedWeb(Matrix4f matrix, Box box, long seedBase) {
        int strandsPerFace = 5;
        int samples = 18;
        float t2 = (float)(System.currentTimeMillis() % 100000L) / 1000.0f * this.lineSpeed.get();
        float lineWidth = 0.0025f;
        float bendBase = 0.06f + this.lineJitter.get() * 0.2f;
        int baseAlpha = Math.max(20, Math.min(255, (int)(this.alpha.get() * 210.0f)));
        int themeColor = this.getStableThemeColor();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        this.drawFilledBoxInt(matrix, box, ColorUtils.setAlphaColor(themeColor, (int)(this.alpha.get() * this.fill.get() * 170.0f)));
        for (int face = 0; face < 6; ++face) {
            int[] neighbors = this.faceNeighbors(face);
            for (int strand = 0; strand < strandsPerFace; ++strand) {
                int key = face * 1000 + strand * 53;
                int adj = neighbors[strand % neighbors.length];
                double phase = (double)t2 * (0.95 + this.rand01(seedBase, key + 1) * 0.55) + (double)strand * 0.83 + (double)face * 1.11;
                double edgeT = this.clamp01(0.5 + Math.sin(phase * 1.37 + this.rand01(seedBase, key + 2) * 6.2831853) * 0.38);
                Vec3d pivot = this.edgePoint(box, face, adj, edgeT, 0.0015);
                Vec3d start = this.facePoint(box, face, this.clamp01(0.5 + (this.rand01(seedBase, key + 3) - 0.5) * 0.46), this.clamp01(0.5 + (this.rand01(seedBase, key + 4) - 0.5) * 0.46), 0.0015);
                Vec3d end = this.facePoint(box, adj, this.clamp01(0.5 + (this.rand01(seedBase, key + 5) - 0.5) * 0.46), this.clamp01(0.5 + (this.rand01(seedBase, key + 6) - 0.5) * 0.46), 0.0015);
                Vec3d[] basisA = this.faceBasis(face);
                Vec3d[] basisB = this.faceBasis(adj);
                Vec3d normalA = this.faceNormal(face);
                Vec3d normalB = this.faceNormal(adj);
                double bendA = (double)bendBase * (0.7 + this.rand01(seedBase, key + 7)) * Math.sin(phase * 1.9 + this.rand01(seedBase, key + 8) * 6.2831853);
                double bendB = (double)bendBase * (0.7 + this.rand01(seedBase, key + 9)) * Math.cos(phase * 1.7 + this.rand01(seedBase, key + 10) * 6.2831853);
                Vec3d dirA = pivot.subtract(start);
                Vec3d c1a = start.add(dirA.multiply(0.38)).add(basisA[0].multiply(bendA)).add(basisA[1].multiply(-bendA * 0.55));
                Vec3d c2a = start.add(dirA.multiply(0.76)).add(basisA[0].multiply(-bendA * 0.65)).add(basisA[1].multiply(bendA * 0.4));
                Vec3d dirB = end.subtract(pivot);
                Vec3d c1b = pivot.add(dirB.multiply(0.24)).add(basisB[0].multiply(bendB)).add(basisB[1].multiply(bendB * 0.45));
                Vec3d c2b = pivot.add(dirB.multiply(0.62)).add(basisB[0].multiply(-bendB * 0.7)).add(basisB[1].multiply(-bendB * 0.35));
                int alphaLine = Math.max(18, Math.min(255, (int)((double)baseAlpha * (0.74 + 0.26 * Math.sin(phase * 2.6)))));
                int color = ColorUtils.setAlphaColor(themeColor, alphaLine);
                this.drawBezierRibbon(matrix, start, c1a, c2a, pivot, normalA, samples, color, lineWidth);
                this.drawBezierRibbon(matrix, pivot, c1b, c2b, end, normalB, samples, color, lineWidth);
            }
        }
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private Vec3d cubicBezier(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t2) {
        double it = 1.0 - (double)t2;
        double it2 = it * it;
        double t22 = t2 * t2;
        return p0.multiply(it2 * it).add(p1.multiply(3.0 * it2 * (double)t2)).add(p2.multiply(3.0 * it * t22)).add(p3.multiply(t22 * (double)t2));
    }

    private void drawBezierRibbon(Matrix4f matrix, Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, Vec3d faceNormal, int samples, int color, float halfWidth) {
        Vec3d[] points = new Vec3d[samples + 1];
        for (int s2 = 0; s2 <= samples; ++s2) {
            float u2 = (float)s2 / (float)samples;
            points[s2] = this.cubicBezier(p0, p1, p2, p3, u2);
        }
        BufferBuilder quads = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i2 = 0; i2 < samples; ++i2) {
            Vec3d b2 = points[i2 + 1];
            Vec3d a2 = points[i2];
            Vec3d dir = b2.subtract(a2);
            if (dir.lengthSquared() < 1.0E-6) continue;
            Vec3d perp = faceNormal.crossProduct(dir).normalize().multiply((double)halfWidth);
            Vec3d aL = a2.add(perp);
            Vec3d aR = a2.subtract(perp);
            Vec3d bL = b2.add(perp);
            Vec3d bR = b2.subtract(perp);
            quads.vertex(matrix, (float)aL.x, (float)aL.y, (float)aL.z).color(color);
            quads.vertex(matrix, (float)aR.x, (float)aR.y, (float)aR.z).color(color);
            quads.vertex(matrix, (float)bR.x, (float)bR.y, (float)bR.z).color(color);
            quads.vertex(matrix, (float)bL.x, (float)bL.y, (float)bL.z).color(color);
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)quads.end());
    }

    private int[] faceNeighbors(int face) {
        int[] nArray;
        switch (face) {
            case 0: 
            case 1: {
                int[] nArray2 = new int[4];
                nArray2[0] = 2;
                nArray2[1] = 3;
                nArray2[2] = 4;
                nArray = nArray2;
                nArray2[3] = 5;
                break;
            }
            case 2: 
            case 3: {
                int[] nArray3 = new int[4];
                nArray3[0] = 0;
                nArray3[1] = 1;
                nArray3[2] = 4;
                nArray = nArray3;
                nArray3[3] = 5;
                break;
            }
            default: {
                int[] nArray4 = new int[4];
                nArray4[0] = 0;
                nArray4[1] = 1;
                nArray4[2] = 2;
                nArray = nArray4;
                nArray4[3] = 3;
            }
        }
        return nArray;
    }

    private Vec3d[] faceBasis(int face) {
        Vec3d[] class_243Array;
        switch (face) {
            case 0: 
            case 1: {
                Vec3d[] class_243Array2 = new Vec3d[2];
                class_243Array2[0] = new Vec3d(1.0, 0.0, 0.0);
                class_243Array = class_243Array2;
                class_243Array2[1] = new Vec3d(0.0, 0.0, 1.0);
                break;
            }
            case 2: 
            case 3: {
                Vec3d[] class_243Array3 = new Vec3d[2];
                class_243Array3[0] = new Vec3d(1.0, 0.0, 0.0);
                class_243Array = class_243Array3;
                class_243Array3[1] = new Vec3d(0.0, 1.0, 0.0);
                break;
            }
            default: {
                Vec3d[] class_243Array4 = new Vec3d[2];
                class_243Array4[0] = new Vec3d(0.0, 0.0, 1.0);
                class_243Array = class_243Array4;
                class_243Array4[1] = new Vec3d(0.0, 1.0, 0.0);
            }
        }
        return class_243Array;
    }

    private Vec3d faceNormal(int face) {
        return switch (face) {
            case 0 -> new Vec3d(0.0, 1.0, 0.0);
            case 1 -> new Vec3d(0.0, -1.0, 0.0);
            case 2 -> new Vec3d(0.0, 0.0, -1.0);
            case 3 -> new Vec3d(0.0, 0.0, 1.0);
            case 4 -> new Vec3d(-1.0, 0.0, 0.0);
            default -> new Vec3d(1.0, 0.0, 0.0);
        };
    }

    private Vec3d edgePoint(Box box, int faceA, int faceB, double t2, double inset) {
        double[] fixedB;
        double x2 = Double.NaN;
        double y2 = Double.NaN;
        double z2 = Double.NaN;
        double[] fixedA = this.faceFixedCoords(box, faceA, inset);
        if (!Double.isNaN(fixedA[0])) {
            x2 = fixedA[0];
        }
        if (!Double.isNaN(fixedA[1])) {
            y2 = fixedA[1];
        }
        if (!Double.isNaN(fixedA[2])) {
            z2 = fixedA[2];
        }
        if (!Double.isNaN((fixedB = this.faceFixedCoords(box, faceB, inset))[0])) {
            x2 = fixedB[0];
        }
        if (!Double.isNaN(fixedB[1])) {
            y2 = fixedB[1];
        }
        if (!Double.isNaN(fixedB[2])) {
            z2 = fixedB[2];
        }
        double tt = this.clamp01(t2);
        if (Double.isNaN(x2)) {
            x2 = this.lerp(box.minX, box.maxX, tt);
        }
        if (Double.isNaN(y2)) {
            y2 = this.lerp(box.minY, box.maxY, tt);
        }
        if (Double.isNaN(z2)) {
            z2 = this.lerp(box.minZ, box.maxZ, tt);
        }
        return new Vec3d(x2, y2, z2);
    }

    private double[] faceFixedCoords(Box box, int face, double inset) {
        double[] dArray;
        switch (face) {
            case 0: {
                double[] dArray2 = new double[3];
                dArray2[0] = Double.NaN;
                dArray2[1] = box.maxY - inset;
                dArray = dArray2;
                dArray2[2] = Double.NaN;
                break;
            }
            case 1: {
                double[] dArray3 = new double[3];
                dArray3[0] = Double.NaN;
                dArray3[1] = box.minY + inset;
                dArray = dArray3;
                dArray3[2] = Double.NaN;
                break;
            }
            case 2: {
                double[] dArray4 = new double[3];
                dArray4[0] = Double.NaN;
                dArray4[1] = Double.NaN;
                dArray = dArray4;
                dArray4[2] = box.minZ + inset;
                break;
            }
            case 3: {
                double[] dArray5 = new double[3];
                dArray5[0] = Double.NaN;
                dArray5[1] = Double.NaN;
                dArray = dArray5;
                dArray5[2] = box.maxZ - inset;
                break;
            }
            case 4: {
                double[] dArray6 = new double[3];
                dArray6[0] = box.minX + inset;
                dArray6[1] = Double.NaN;
                dArray = dArray6;
                dArray6[2] = Double.NaN;
                break;
            }
            default: {
                double[] dArray7 = new double[3];
                dArray7[0] = box.maxX - inset;
                dArray7[1] = Double.NaN;
                dArray = dArray7;
                dArray7[2] = Double.NaN;
            }
        }
        return dArray;
    }

    private Vec3d facePoint(Box box, int face, double u2, double v2, double inset) {
        u2 = this.clamp01(u2);
        v2 = this.clamp01(v2);
        return switch (face) {
            case 0 -> new Vec3d(this.lerp(box.minX, box.maxX, u2), box.maxY - inset, this.lerp(box.minZ, box.maxZ, v2));
            case 1 -> new Vec3d(this.lerp(box.minX, box.maxX, u2), box.minY + inset, this.lerp(box.minZ, box.maxZ, v2));
            case 2 -> new Vec3d(this.lerp(box.minX, box.maxX, u2), this.lerp(box.minY, box.maxY, v2), box.minZ + inset);
            case 3 -> new Vec3d(this.lerp(box.minX, box.maxX, u2), this.lerp(box.minY, box.maxY, v2), box.maxZ - inset);
            case 4 -> new Vec3d(box.minX + inset, this.lerp(box.minY, box.maxY, v2), this.lerp(box.minZ, box.maxZ, u2));
            default -> new Vec3d(box.maxX - inset, this.lerp(box.minY, box.maxY, v2), this.lerp(box.minZ, box.maxZ, u2));
        };
    }

    private double rand01(long seed, int salt) {
        long x2 = seed + -7046029254386353131L * ((long)salt + 1L);
        x2 ^= x2 >>> 30;
        x2 *= -4658895280553007687L;
        x2 ^= x2 >>> 27;
        x2 *= -7723592293110705685L;
        x2 ^= x2 >>> 31;
        return (double)(x2 & 0xFFFFFFL) / 1.6777216E7;
    }

    private double lerp(double a2, double b2, double t2) {
        return a2 + (b2 - a2) * t2;
    }

    private double clamp01(double v2) {
        return Math.max(0.0, Math.min(1.0, v2));
    }

    private void drawFilledBoxInt(Matrix4f matrix, Box box, int color) {
        BufferBuilder b2 = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(color);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)b2.end());
    }

    private void drawFilledBox(Tessellator tessellator, Matrix4f matrix, Box box, float r2, float g2, float b2, float a2) {
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float minX = (float)box.minX;
        float minY = (float)box.minY;
        float minZ = (float)box.minZ;
        float maxX = (float)box.maxX;
        float maxY = (float)box.maxY;
        float maxZ = (float)box.maxZ;
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private void drawBoxOutline(Tessellator tessellator, Matrix4f matrix, Box box, float r2, float g2, float b2, float a2) {
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth((float)1.5f);
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        float minX = (float)box.minX;
        float minY = (float)box.minY;
        float minZ = (float)box.minZ;
        float maxX = (float)box.maxX;
        float maxY = (float)box.maxY;
        float maxZ = (float)box.maxZ;
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private static class ProjectedPoint {
        private float x;
        private float y;
        private float z;

        private ProjectedPoint() {
        }
    }

    private record ScreenRect(float minX, float minY, float maxX, float maxY) {
        float centerX() {
            return (this.minX + this.maxX) * 0.5f;
        }

        float centerY() {
            return (this.minY + this.maxY) * 0.5f;
        }
    }

    private record DonateSegment(String text, int color) {
    }

    public record PublicDonateSegment(String text, int color) {
    }

    private record EffectLine(String text, int color) {
    }

    private static class DonateCache {
        private List<DonateSegment> segments = Collections.emptyList();
        private long nextUpdateAt;

        private DonateCache() {
        }
    }
}

