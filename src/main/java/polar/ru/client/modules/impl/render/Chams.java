package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.ShaderUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.mixin.LivingEntityRendererAccessor;
import polar.ru.polar;

public class Chams
extends Module {
    public static final Chams INSTANCE = new Chams();
    public static final String TARGET_PLAYERS = "Игроков";
    public static final String TARGET_FRIENDS = "Друзей";
    public static final String TARGET_SELF = "Себя";
    private static final int DEFAULT_FILL_ALPHA = 130;
    private static final float DEFAULT_LINE_WIDTH = 0.5f;
    private static final float CLIENT_FILL_SATURATION = 1.18f;
    private static final float CLIENT_FILL_BRIGHTNESS = 1.12f;
    private static final float CLIENT_OUTLINE_SATURATION = 1.12f;
    private static final float CLIENT_OUTLINE_BRIGHTNESS = 1.08f;
    private static final float MIN_PULSE_ALPHA = 0.65f;
    private static final float PULSE_SWING = 0.35f;
    private static final int FRIEND_FILL_COLOR = new Color(85, 255, 85, 60).getRGB();
    private static final int FRIEND_OUTLINE_COLOR = new Color(100, 255, 100, 255).getRGB();
    private static final long OUTLINE_RETRY_DELAY_MS = 3000L;
    private final ListSetting rendering = new ListSetting("Отображать", new BooleanSetting("Игроков", true), new BooleanSetting("Друзей", true), new BooleanSetting("Себя", false));
    private final BooleanSetting waves = new BooleanSetting("Волны", true);
    private final FloatSetting waveSpeedX = new FloatSetting("Скорость X", 0.22f, 0.0f, 1.5f, 0.01f).visible(this.waves::isState);
    private final FloatSetting waveSpeedY = new FloatSetting("Скорость Y", 0.15f, 0.0f, 1.5f, 0.01f).visible(this.waves::isState);
    private final FloatSetting waveScale = new FloatSetting("Размер волн", 1.35f, 0.2f, 4.0f, 0.05f).visible(this.waves::isState);
    private final FloatSetting waveDensity = new FloatSetting("Плотность волн", 1.15f, 0.5f, 3.0f, 0.05f).visible(this.waves::isState);
    private final FloatSetting waveGlow = new FloatSetting("Сила волн", 1.0f, 0.2f, 3.0f, 0.05f).visible(this.waves::isState);
    private final BooleanSetting glow = new BooleanSetting("Свечение", true);
    private final FloatSetting glowIntensity = new FloatSetting("Сила свечения", 2.0f, 1.0f, 5.0f, 0.1f).visible(this.glow::isState);
    private final FloatSetting glowLayers = new FloatSetting("Слои свечения", 3.0f, 1.0f, 6.0f, 1.0f).visible(this.glow::isState);
    private final BooleanSetting pulse = new BooleanSetting("Пульсирование", false);
    private final FloatSetting pulseSpeed = new FloatSetting("Скорость пульсации", 2.0f, 0.5f, 5.0f, 0.1f).visible(this.pulse::isState);
    private final BooleanSetting hideOriginal = new BooleanSetting("Скрыть оригинал", false);
    private final BooleanSetting hideItemsAndCape = new BooleanSetting("Скрывать предметы и плащ", false);
    private final long startTime = System.currentTimeMillis();
    private boolean outlineAssistReady;
    private long nextOutlineRetryAt;

    private Chams() {
        super("Chams", "Чамсы по модели игрока", Module.ModuleCategory.RENDER);
        this.addSettings(this.rendering, this.waves, this.waveSpeedX, this.waveSpeedY, this.waveScale, this.waveDensity, this.waveGlow, this.glow, this.glowIntensity, this.glowLayers, this.pulse, this.pulseSpeed, this.hideOriginal, this.hideItemsAndCape);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.outlineAssistReady = false;
        this.nextOutlineRetryAt = 0L;
        this.tryEnsureOutlineProcessor();
    }

    @Override
    public void onDisable() {
        this.outlineAssistReady = false;
        this.nextOutlineRetryAt = 0L;
        super.onDisable();
    }

    @EventLink(priority=100)
    public void onRender3D(Event3DRender event) {
        if (!this.isEnable() || Chams.mc.world == null || Chams.mc.player == null) {
            return;
        }
        if (this.hasOutlineAssistTargets() && !this.outlineAssistReady && System.currentTimeMillis() >= this.nextOutlineRetryAt) {
            this.tryEnsureOutlineProcessor();
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        for (PlayerEntity player : Chams.mc.world.getPlayers()) {
            if (!this.affects(player) || player == Chams.mc.player && Chams.mc.options.getPerspective() == Perspective.FIRST_PERSON) continue;
            this.renderManualPlayer(event, player);
        }
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth((float)1.0f);
    }

    private void renderManualPlayer(Event3DRender event, PlayerEntity player) {
        if (!(player instanceof AbstractClientPlayerEntity)) {
            return;
        }
        AbstractClientPlayerEntity clientPlayer = (AbstractClientPlayerEntity)player;
        EntityRenderer rawRenderer = mc.getEntityRenderDispatcher().getRenderer((Entity)player);
        if (!(rawRenderer instanceof PlayerEntityRenderer)) {
            return;
        }
        PlayerEntityRenderer renderer = (PlayerEntityRenderer)rawRenderer;
        PlayerEntityRenderState state = renderer.createRenderState();
        renderer.updateRenderState(clientPlayer, state, event.getTickDelta());
        PlayerEntityModel model = (PlayerEntityModel)renderer.getModel();
        model.setAngles(state);
        MatrixStack matrices = event.getMatrices();
        matrices.push();
        this.setupModelMatrix(matrices, state, renderer, event.getCamera().getPos(), player, event.getTickDelta());
        int fillColor = this.resolveFillColor(player);
        int outlineColor = this.resolveOutlineColor(player);
        this.renderShaderFillModel(matrices, (BipedEntityModel<?>)model, 0.0f, fillColor);
        this.renderOutlineModel(matrices, (BipedEntityModel<?>)model, 0.0f, outlineColor);
        matrices.pop();
    }

    private void setupModelMatrix(MatrixStack matrices, PlayerEntityRenderState state, PlayerEntityRenderer renderer, Vec3d cameraPos, PlayerEntity player, float tickDelta) {
        Vec3d pos = player.getLerpedPos(tickDelta);
        double x2 = pos.x - cameraPos.x;
        double y2 = pos.y - cameraPos.y;
        double z2 = pos.z - cameraPos.z;
        matrices.translate(x2, y2, z2);
        if (state.sleepingDirection != null) {
            float eyeOffset = state.standingEyeHeight - 0.1f;
            matrices.translate((float)(-state.sleepingDirection.getOffsetX()) * eyeOffset, 0.0f, (float)(-state.sleepingDirection.getOffsetZ()) * eyeOffset);
        }
        float baseScale = state.baseScale;
        matrices.scale(baseScale, baseScale, baseScale);
        LivingEntityRendererAccessor accessor = (LivingEntityRendererAccessor)renderer;
        accessor.polar$setupTransforms((LivingEntityRenderState)state, matrices, state.bodyYaw, baseScale);
        matrices.scale(-1.0f, -1.0f, 1.0f);
        accessor.polar$scale((LivingEntityRenderState)state, matrices);
        matrices.translate(0.0f, -1.501f, 0.0f);
    }

    private void renderShaderFillModel(MatrixStack matrices, BipedEntityModel<?> model, float expand, int color) {
        if (!this.waves.isState()) {
            this.renderSolidFillModel(matrices, model, expand, color);
            return;
        }
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.chamsFill);
        if (shader == null) {
            return;
        }
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.chamsFill);
        this.setUniform(shader, "time", this.waves.isState() ? (float)(System.currentTimeMillis() - this.startTime) / 1000.0f : 0.0f);
        this.setUniform(shader, "speedX", this.waveSpeedX.get());
        this.setUniform(shader, "speedY", this.waveSpeedY.get());
        this.setUniform(shader, "scale", this.waveScale.get());
        this.setUniform(shader, "density", this.waveDensity.get());
        this.setUniform(shader, "glowStrength", this.waveGlow.get());
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        ModelPart root = model.getRootPart();
        this.renderFillPart(matrices, buffer, root, model.head, -4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, expand, color);
        this.renderFillPart(matrices, buffer, root, model.body, -4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, expand, color);
        this.renderFillPart(matrices, buffer, root, model.rightArm, -3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, color);
        this.renderFillPart(matrices, buffer, root, model.leftArm, -1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, color);
        this.renderFillPart(matrices, buffer, root, model.rightLeg, -2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, color);
        this.renderFillPart(matrices, buffer, root, model.leftLeg, -2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, color);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private void renderSolidFillModel(MatrixStack matrices, BipedEntityModel<?> model, float expand, int color) {
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        ModelPart root = model.getRootPart();
        this.renderSolidFillPart(matrices, buffer, root, model.head, -4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, expand, color);
        this.renderSolidFillPart(matrices, buffer, root, model.body, -4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, expand, color);
        this.renderSolidFillPart(matrices, buffer, root, model.rightArm, -3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, color);
        this.renderSolidFillPart(matrices, buffer, root, model.leftArm, -1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, color);
        this.renderSolidFillPart(matrices, buffer, root, model.rightLeg, -2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, color);
        this.renderSolidFillPart(matrices, buffer, root, model.leftLeg, -2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, color);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private void renderSolidFillPart(MatrixStack baseStack, BufferBuilder buffer, ModelPart root, ModelPart part, float offX, float offY, float offZ, float width, float height, float depth, float expand, int color) {
        baseStack.push();
        root.rotate(baseStack);
        part.rotate(baseStack);
        Matrix4f matrix = baseStack.peek().getPositionMatrix();
        float scale = 0.0625f;
        float expandScale = expand * scale;
        float minX = offX * scale - expandScale;
        float minY = offY * scale - expandScale;
        float minZ = offZ * scale - expandScale;
        float maxX = (offX + width) * scale + expandScale;
        float maxY = (offY + height) * scale + expandScale;
        float maxZ = (offZ + depth) * scale + expandScale;
        this.addSolidQuad(buffer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
        this.addSolidQuad(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, color);
        this.addSolidQuad(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, color);
        this.addSolidQuad(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, color);
        this.addSolidQuad(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, color);
        this.addSolidQuad(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, color);
        baseStack.pop();
    }

    private void renderFillPart(MatrixStack baseStack, BufferBuilder buffer, ModelPart root, ModelPart part, float offX, float offY, float offZ, float width, float height, float depth, float expand, int color) {
        baseStack.push();
        root.rotate(baseStack);
        part.rotate(baseStack);
        Matrix4f matrix = baseStack.peek().getPositionMatrix();
        float scale = 0.0625f;
        float expandScale = expand * scale;
        float minX = offX * scale - expandScale;
        float minY = offY * scale - expandScale;
        float minZ = offZ * scale - expandScale;
        float maxX = (offX + width) * scale + expandScale;
        float maxY = (offY + height) * scale + expandScale;
        float maxZ = (offZ + depth) * scale + expandScale;
        this.addQuad(buffer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
        this.addQuad(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, color);
        this.addQuad(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, color);
        this.addQuad(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, color);
        this.addQuad(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, color);
        this.addQuad(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, color);
        baseStack.pop();
    }

    private void addQuad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int color) {
        int r2 = ColorUtils.red(color);
        int g2 = ColorUtils.green(color);
        int b2 = ColorUtils.blue(color);
        int a2 = ColorUtils.alpha(color);
        float u1 = this.waveU(x1, y1, z1);
        float v1 = this.waveV(x1, y1, z1);
        float u2 = this.waveU(x2, y2, z2);
        float v2 = this.waveV(x2, y2, z2);
        float u3 = this.waveU(x3, y3, z3);
        float v3 = this.waveV(x3, y3, z3);
        float u4 = this.waveU(x4, y4, z4);
        float v4 = this.waveV(x4, y4, z4);
        buffer.vertex(matrix, x1, y1, z1).texture(u1, v1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2, y2, z2).texture(u2, v2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x3, y3, z3).texture(u3, v3).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x4, y4, z4).texture(u4, v4).color(r2, g2, b2, a2);
    }

    private void addSolidQuad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int color) {
        int r2 = ColorUtils.red(color);
        int g2 = ColorUtils.green(color);
        int b2 = ColorUtils.blue(color);
        int a2 = ColorUtils.alpha(color);
        buffer.vertex(matrix, x1, y1, z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2, y2, z2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x3, y3, z3).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x4, y4, z4).color(r2, g2, b2, a2);
    }

    private float waveU(float x2, float y2, float z2) {
        return x2 * 1.15f + z2 * 0.72f;
    }

    private float waveV(float x2, float y2, float z2) {
        return y2 * 1.05f - z2 * 0.38f + x2 * 0.18f;
    }

    private void renderOutlineModel(MatrixStack matrices, BipedEntityModel<?> model, float expand, int color) {
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        GL11.glEnable((int)2848);
        GL11.glHint((int)3154, (int)4354);
        RenderSystem.lineWidth((float)0.5f);
        if (this.glow.isState()) {
            RenderSystem.blendFuncSeparate((int)770, (int)1, (int)1, (int)0);
            int layers = Math.max(1, Math.round(this.glowLayers.get()));
            float intensity = Math.max(1.0f, this.glowIntensity.get());
            for (int index = layers; index >= 1; --index) {
                float layerExpand = expand + (float)index * 0.5f * intensity;
                float alphaMul = 1.0f / (float)(index + 1) * 0.7f;
                int alpha = Math.max(1, Math.min(255, Math.round((float)ColorUtils.alpha(color) * alphaMul)));
                this.drawOutlineParts(matrices, model, layerExpand, this.withAlpha(color, alpha));
            }
        }
        RenderSystem.defaultBlendFunc();
        this.drawOutlineParts(matrices, model, expand, color);
        GL11.glDisable((int)2848);
    }

    private void drawOutlineParts(MatrixStack matrices, BipedEntityModel<?> model, float expand, int color) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        ModelPart root = model.getRootPart();
        this.renderPartOutlineLines(matrices, buffer, root, model.head, -4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, expand, color);
        this.renderPartOutlineLines(matrices, buffer, root, model.body, -4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, expand, color);
        this.renderPartOutlineLines(matrices, buffer, root, model.rightArm, -3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, color);
        this.renderPartOutlineLines(matrices, buffer, root, model.leftArm, -1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, color);
        this.renderPartOutlineLines(matrices, buffer, root, model.rightLeg, -2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, color);
        this.renderPartOutlineLines(matrices, buffer, root, model.leftLeg, -2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, color);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private void renderPartOutlineLines(MatrixStack baseStack, BufferBuilder buffer, ModelPart root, ModelPart part, float offX, float offY, float offZ, float width, float height, float depth, float expand, int color) {
        baseStack.push();
        root.rotate(baseStack);
        part.rotate(baseStack);
        float scale = 0.0625f;
        float expandScale = expand * scale;
        float minX = offX * scale - expandScale;
        float minY = offY * scale - expandScale;
        float minZ = offZ * scale - expandScale;
        float maxX = (offX + width) * scale + expandScale;
        float maxY = (offY + height) * scale + expandScale;
        float maxZ = (offZ + depth) * scale + expandScale;
        Matrix4f matrix = baseStack.peek().getPositionMatrix();
        this.addLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, color);
        this.addLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, color);
        this.addLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, color);
        this.addLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, color);
        this.addLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, color);
        this.addLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        this.addLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        this.addLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, color);
        this.addLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, color);
        this.addLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, color);
        this.addLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, color);
        this.addLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, color);
        baseStack.pop();
    }

    private void addLine(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        int r2 = ColorUtils.red(color);
        int g2 = ColorUtils.green(color);
        int b2 = ColorUtils.blue(color);
        int a2 = ColorUtils.alpha(color);
        buffer.vertex(matrix, x1, y1, z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2, y2, z2).color(r2, g2, b2, a2);
    }

    private void setUniform(ShaderProgram shader, String name, float value) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    public boolean affects(PlayerEntity player) {
        if (!this.isEnable() || player == null || !player.isAlive()) {
            return false;
        }
        if (player == Chams.mc.player) {
            return this.rendering.is(TARGET_SELF) && Chams.mc.options.getPerspective() != Perspective.FIRST_PERSON;
        }
        if (this.isFriend(player)) {
            return this.rendering.is(TARGET_FRIENDS);
        }
        return this.rendering.is(TARGET_PLAYERS);
    }

    public boolean shouldHideBaseModel(PlayerEntity player) {
        return this.hideOriginal.isState() && this.affects(player);
    }

    public boolean shouldHideItemsAndCape(PlayerEntity player) {
        return this.hideItemsAndCape.isState() && this.affects(player);
    }

    public boolean shouldUseOutlineAssist(PlayerEntity player) {
        return this.affects(player);
    }

    public boolean shouldHideOutlineFramebuffer() {
        return this.isEnable() && this.hasOutlineAssistTargets();
    }

    public int resolveFillColor(PlayerEntity player) {
        return this.applyPulse(this.baseFillColor(player));
    }

    public int resolveOutlineColor(PlayerEntity player) {
        return this.applyPulse(this.baseOutlineColor(player));
    }

    private int baseFillColor(PlayerEntity player) {
        if (this.isFriend(player)) {
            return FRIEND_FILL_COLOR;
        }
        return this.vividWithAlpha(ColorUtils.getThemeColor(), 1.18f, 1.12f, 130);
    }

    private int baseOutlineColor(PlayerEntity player) {
        if (this.isFriend(player)) {
            return FRIEND_OUTLINE_COLOR;
        }
        return this.vividWithAlpha(ColorUtils.getThemeColor(), 1.12f, 1.08f, 255);
    }

    private int applyPulse(int color) {
        if (!this.pulse.isState()) {
            return color;
        }
        float elapsedSeconds = (float)(System.currentTimeMillis() - this.startTime) / 1000.0f;
        float pulseValue = (float)((Math.sin((double)(elapsedSeconds * this.pulseSpeed.get()) * Math.PI) + 1.0) * 0.5);
        float alphaMul = 0.65f + 0.35f * pulseValue;
        return ColorUtils.multAlpha(color, alphaMul);
    }

    private int vividWithAlpha(int color, float saturationBoost, float brightnessBoost, int alpha) {
        float[] hsb = Color.RGBtoHSB(ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), null);
        float saturation = MathHelper.clamp((float)(hsb[1] * saturationBoost), (float)0.0f, (float)1.0f);
        float brightness = MathHelper.clamp((float)(Math.max(hsb[2], 0.8f) * brightnessBoost), (float)0.0f, (float)1.0f);
        int rgb = Color.HSBtoRGB(hsb[0], saturation, brightness);
        return ColorUtils.rgba(ColorUtils.red(rgb), ColorUtils.green(rgb), ColorUtils.blue(rgb), alpha);
    }

    private int withAlpha(int color, int alpha) {
        return color & 0xFFFFFF | (alpha & 0xFF) << 24;
    }

    private boolean isFriend(PlayerEntity player) {
        return polar.INSTANCE != null && polar.INSTANCE.friendStorage != null && polar.INSTANCE.friendStorage.isFriend(player.getName().getString());
    }

    private boolean hasOutlineAssistTargets() {
        if (!this.isEnable() || Chams.mc.world == null || Chams.mc.player == null) {
            return false;
        }
        for (PlayerEntity player : Chams.mc.world.getPlayers()) {
            if (!this.shouldUseOutlineAssist(player)) continue;
            return true;
        }
        return false;
    }

    private boolean tryEnsureOutlineProcessor() {
        if (Chams.mc.worldRenderer == null) {
            this.outlineAssistReady = false;
            return false;
        }
        if (Chams.mc.worldRenderer.getEntityOutlinesFramebuffer() != null) {
            this.outlineAssistReady = true;
            return true;
        }
        try {
            Chams.mc.worldRenderer.loadEntityOutlinePostProcessor();
        }
        catch (Exception exception) {
            // empty catch block
        }
        boolean bl = this.outlineAssistReady = Chams.mc.worldRenderer.getEntityOutlinesFramebuffer() != null;
        if (!this.outlineAssistReady) {
            this.nextOutlineRetryAt = System.currentTimeMillis() + 3000L;
        }
        return this.outlineAssistReady;
    }
}

