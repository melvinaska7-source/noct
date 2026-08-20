package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.polar;

public class JumpCircle
extends Module {
    public static JumpCircle INSTANCE = new JumpCircle();
    private static final float MAX_LIFETIME_MS = 1850.0f;
    private static final float ROTATION_SPEED = 120.0f;
    private static final float PULSE_SPEED = 7.0f;
    private static final float PULSE_SCALE = 0.06f;
    private static final float PULSE_ALPHA = 0.12f;
    private static final int MAX_CIRCLES = 8;
    private final FloatSetting radius = new FloatSetting("Радиус", 1.85f, 0.5f, 4.0f, 0.1f);
    private final FloatSetting speed = new FloatSetting("Скорость", 1.2f, 1.0f, 5.0f, 0.1f);
    private final FloatSetting fadeSpeed = new FloatSetting("Скорость исчезновения", 1.5f, 1.0f, 5.0f, 0.5f);
    private final List<CircleData> circles = new ArrayList<CircleData>();
    private final Identifier circleTexture = Identifier.of((String)"polar", (String)"textures/jumpcircle/circle.png");
    private boolean wasOnGround = true;

    public JumpCircle() {
        super("JumpCircle", "Круг при прыжке", Module.ModuleCategory.RENDER);
        this.addSettings(this.radius, this.speed, this.fadeSpeed);
    }

    @Override
    public void onEnable() {
        if (JumpCircle.mc.player != null) {
            this.wasOnGround = JumpCircle.mc.player.isOnGround();
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.circles.clear();
        super.onDisable();
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (JumpCircle.mc.player == null || JumpCircle.mc.world == null) {
            return;
        }
        boolean isOnGround = JumpCircle.mc.player.isOnGround();
        if (this.wasOnGround && !isOnGround) {
            Vec3d pos = new Vec3d(JumpCircle.mc.player.getX(), Math.floor(JumpCircle.mc.player.getY()) + 0.001, JumpCircle.mc.player.getZ());
            this.circles.add(new CircleData(pos, System.currentTimeMillis()));
            while (this.circles.size() > 8) {
                this.circles.remove(0);
            }
        }
        this.wasOnGround = isOnGround;
        long now = System.currentTimeMillis();
        float lifeTimeMs = this.getLifeTimeMs();
        Iterator<CircleData> iterator = this.circles.iterator();
        while (iterator.hasNext()) {
            CircleData circle = iterator.next();
            if (now - circle.startTimeMs <= (long)lifeTimeMs) continue;
            iterator.remove();
        }
    }

    @EventLink
    public void onRender3D(Event3DRender event) {
        if (this.circles.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Vec3d cameraPos = event.getCamera().getPos();
        MatrixStack matrices = event.getMatrices();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)this.circleTexture);
        for (CircleData circle : this.circles) {
            float alpha;
            float progress = this.getProgress(now, circle);
            if (progress >= 1.0f || (alpha = this.getAlpha(progress)) <= 0.01f) continue;
            this.renderGlowCircle(matrices, cameraPos, circle, progress, alpha, now);
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    private float getLifeTimeMs() {
        return 1850.0f / Math.max(0.25f, this.speed.get());
    }

    private float getProgress(long now, CircleData circle) {
        return (float)(now - circle.startTimeMs) / this.getLifeTimeMs();
    }

    private float getAlpha(float progress) {
        float fade = MathHelper.clamp((float)(progress * this.fadeSpeed.get()), (float)0.0f, (float)1.0f);
        return 1.0f - fade;
    }

    private void renderGlowCircle(MatrixStack matrices, Vec3d cameraPos, CircleData circle, float progress, float alpha, long now) {
        float lifeTimeSec = (float)(now - circle.startTimeMs) / 1000.0f;
        float easedProgress = JumpCircle.easeOutCubic(progress);
        float scale = Math.min(easedProgress * this.radius.get(), this.radius.get());
        float rotation = lifeTimeSec * 120.0f * this.speed.get();
        rotation += (float)Math.sin((double)progress * Math.PI * 2.0) * 30.0f;
        float pulse = (float)Math.sin(lifeTimeSec * 7.0f * this.speed.get());
        float pulseScale = 1.0f + pulse * 0.06f;
        float pulseAlpha = MathHelper.clamp((float)(alpha * (1.0f + pulse * 0.12f)), (float)0.0f, (float)1.0f);
        float alphaBoost = MathHelper.clamp((float)(pulseAlpha * 1.25f), (float)0.0f, (float)1.0f);
        float finalScale = scale * pulseScale;
        int baseTheme = this.getStableThemeColor();
        int secondaryTheme = this.getStableThemeSecondaryColor();
        int colorA = ColorUtils.setAlphaColor(baseTheme, (int)(255.0f * alphaBoost));
        int colorB = ColorUtils.setAlphaColor(secondaryTheme, (int)(255.0f * alphaBoost));
        int darkA = ColorUtils.setAlphaColor(ColorUtils.darken(baseTheme, 0.65f), (int)(255.0f * MathHelper.clamp((float)(alphaBoost * 0.9f), (float)0.0f, (float)1.0f)));
        int darkB = ColorUtils.setAlphaColor(ColorUtils.darken(secondaryTheme, 0.65f), (int)(255.0f * MathHelper.clamp((float)(alphaBoost * 0.9f), (float)0.0f, (float)1.0f)));
        matrices.push();
        matrices.translate(circle.pos.x - cameraPos.x, circle.pos.y - cameraPos.y, circle.pos.z - cameraPos.z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float half = finalScale * 0.5f;
        float thickScale = finalScale * 1.08f;
        float thickHalf = thickScale * 0.5f;
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        this.addTexturedQuad(buffer, matrix, -half, -half, half, half, colorA, colorB);
        this.addTexturedQuad(buffer, matrix, -thickHalf, -thickHalf, thickHalf, thickHalf, darkA, darkB);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        matrices.pop();
    }

    private void addTexturedQuad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float x2, float y2, int colorA, int colorB) {
        int aR = colorA >> 16 & 0xFF;
        int aG = colorA >> 8 & 0xFF;
        int aB = colorA & 0xFF;
        int aA = colorA >> 24 & 0xFF;
        int bR = colorB >> 16 & 0xFF;
        int bG = colorB >> 8 & 0xFF;
        int bB = colorB & 0xFF;
        int bA = colorB >> 24 & 0xFF;
        buffer.vertex(matrix, x1, y1, 0.0f).texture(0.0f, 1.0f).color(aR, aG, aB, aA);
        buffer.vertex(matrix, x1, y2, 0.0f).texture(0.0f, 0.0f).color(bR, bG, bB, bA);
        buffer.vertex(matrix, x2, y2, 0.0f).texture(1.0f, 0.0f).color(bR, bG, bB, bA);
        buffer.vertex(matrix, x2, y1, 0.0f).texture(1.0f, 1.0f).color(aR, aG, aB, aA);
    }

    private int getStableThemeColor() {
        if (!polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor();
    }

    private int getStableThemeSecondaryColor() {
        if (!polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor(180);
    }

    private static float easeOutCubic(float t2) {
        float u2 = 1.0f - t2;
        return 1.0f - u2 * u2 * u2;
    }

    private record CircleData(Vec3d pos, long startTimeMs) {
    }
}

