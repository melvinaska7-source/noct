package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class Projectile
extends Module {
    private final Font impactFont = Fonts.getFont("sf_regular", 14);
    public static Projectile INSTANCE = new Projectile();
    private static final Identifier BLOOM_TEXTURE = Identifier.of((String)"polar", (String)"textures/particle/bloom.png");
    private final FloatSetting size = new FloatSetting("Размер", 1.2f, 0.6f, 2.4f, 0.1f);
    private final List<ImpactPoint> impactPoints = new ArrayList<ImpactPoint>();
    private final Matrix4f lastProjectionMatrix = new Matrix4f();
    private final Quaternionf lastCameraRotation = new Quaternionf();
    private Vec3d lastCameraPos = Vec3d.ZERO;
    private boolean hasMatrices;

    public Projectile() {
        super("Projectile", "Траектория жемчуга эндера", Module.ModuleCategory.RENDER);
        this.addSettings(this.size);
    }

    @EventLink
    public void onRender3D(Event3DRender event) {
        if (Projectile.mc.world == null || Projectile.mc.player == null) {
            return;
        }
        this.impactPoints.clear();
        this.hasMatrices = true;
        this.lastProjectionMatrix.set((Matrix4fc)event.getProjectionMatrix());
        this.lastCameraPos = event.getCamera().getPos();
        this.lastCameraRotation.set((Quaternionfc)event.getCamera().getRotation());
        MatrixStack matrices = event.getMatrices();
        Camera camera = event.getCamera();
        Vec3d cameraPos = camera.getPos();
        Quaternionf cameraRotation = camera.getRotation();
        float tickDelta = event.getTickDelta();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)BLOOM_TEXTURE);
        Box searchBox = Projectile.mc.player.getBoundingBox().expand(128.0);
        for (EnderPearlEntity pearl : Projectile.mc.world.getEntitiesByClass(EnderPearlEntity.class, searchBox, Entity::isAlive)) {
            List<Vec3d> points = this.simulate(pearl, tickDelta);
            if (points.size() < 2) continue;
            float seconds = (float)(points.size() - 1) / 20.0f;
            Vec3d impactPos = points.get(points.size() - 1);
            this.impactPoints.add(new ImpactPoint(impactPos, seconds));
            float quadSize = this.size.get() * 0.2f;
            int color = ColorUtils.setAlphaColor(ColorUtils.getThemeColor(), 40);
            int r2 = color >> 16 & 0xFF;
            int g2 = color >> 8 & 0xFF;
            int b2 = color & 0xFF;
            int a2 = color >> 24 & 0xFF;
            matrices.push();
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            for (int i2 = 0; i2 < points.size() - 1; ++i2) {
                Vec3d start = points.get(i2);
                Vec3d end = points.get(i2 + 1);
                int samples = Math.max(2, Math.min(12, (int)Math.ceil(start.distanceTo(end) / (double)Math.max(quadSize * 1.75f, 0.08f))));
                for (int j2 = 0; j2 < samples; ++j2) {
                    Vec3d interp = start.lerp(end, (double)j2 / (double)samples);
                    matrices.push();
                    matrices.translate(interp.x, interp.y, interp.z);
                    matrices.multiply(cameraRotation);
                    Matrix4f matrix = matrices.peek().getPositionMatrix();
                    buffer.vertex(matrix, -quadSize, -quadSize, 0.0f).texture(0.0f, 0.0f).color(r2, g2, b2, a2);
                    buffer.vertex(matrix, -quadSize, quadSize, 0.0f).texture(0.0f, 1.0f).color(r2, g2, b2, a2);
                    buffer.vertex(matrix, quadSize, quadSize, 0.0f).texture(1.0f, 1.0f).color(r2, g2, b2, a2);
                    buffer.vertex(matrix, quadSize, -quadSize, 0.0f).texture(1.0f, 0.0f).color(r2, g2, b2, a2);
                    matrices.pop();
                }
            }
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
            matrices.pop();
            float markerSize = quadSize * 1.6f;
            int markerColor = ColorUtils.setAlphaColor(ColorUtils.getThemeColor(), 170);
            int mr = markerColor >> 16 & 0xFF;
            int mg = markerColor >> 8 & 0xFF;
            int mb = markerColor & 0xFF;
            int ma = markerColor >> 24 & 0xFF;
            float mx = (float)(impactPos.x - cameraPos.x);
            float my = (float)(impactPos.y - cameraPos.y + (double)0.03f);
            float mz = (float)(impactPos.z - cameraPos.z);
            matrices.push();
            matrices.translate(mx, my, mz);
            matrices.multiply(cameraRotation);
            Matrix4f markerMatrix = matrices.peek().getPositionMatrix();
            BufferBuilder marker = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            marker.vertex(markerMatrix, -markerSize, -markerSize, 0.0f).texture(0.0f, 0.0f).color(mr, mg, mb, ma);
            marker.vertex(markerMatrix, -markerSize, markerSize, 0.0f).texture(0.0f, 1.0f).color(mr, mg, mb, ma);
            marker.vertex(markerMatrix, markerSize, markerSize, 0.0f).texture(1.0f, 1.0f).color(mr, mg, mb, ma);
            marker.vertex(markerMatrix, markerSize, -markerSize, 0.0f).texture(1.0f, 0.0f).color(mr, mg, mb, ma);
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)marker.end());
            matrices.pop();
        }
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @EventLink
    public void onRender2D(EventRender.Default event) {
        if (!this.hasMatrices || this.impactPoints.isEmpty() || Projectile.mc.player == null) {
            return;
        }
        MatrixStack matrices = event.getContext().getMatrices();
        Font font = this.impactFont;
        if (font == null) {
            return;
        }
        int themeColor = ColorUtils.getThemeColor();
        for (ImpactPoint impact : this.impactPoints) {
            Vec3d screen = this.worldToScreen(impact.pos());
            if (screen == null) continue;
            String text = this.formatOneDecimal(impact.seconds()) + " сек";
            float textWidth = font.getStringWidth(text);
            float boxWidth = textWidth + 10.0f;
            float boxHeight = 12.5f;
            float x2 = (float)screen.x - boxWidth / 2.0f;
            float y2 = (float)screen.y - 6.0f;
            RenderUtils.drawDefaultHudThemedPanel(matrices, x2, y2, boxWidth, boxHeight, 3.0f, 3.5f, themeColor);
            float textY = y2 + (boxHeight - font.getHeight()) / 2.0f;
            font.drawString(matrices, text, x2 + 5.5f, textY, -1);
        }
    }

    private Vec3d worldToScreen(Vec3d worldPos) {
        if (mc == null || mc.getWindow() == null) {
            return null;
        }
        Vector3f relative = new Vector3f((float)(worldPos.x - this.lastCameraPos.x), (float)(worldPos.y - this.lastCameraPos.y), (float)(worldPos.z - this.lastCameraPos.z));
        Quaternionf invCameraRot = new Quaternionf((Quaternionfc)this.lastCameraRotation).conjugate();
        relative.rotate((Quaternionfc)invCameraRot);
        Vector4f clip = new Vector4f(relative.x, relative.y, relative.z, 1.0f);
        this.lastProjectionMatrix.transform(clip);
        float w2 = clip.w;
        if (w2 <= 1.0E-5f) {
            return null;
        }
        float ndcX = clip.x / w2;
        float ndcY = clip.y / w2;
        float ndcZ = clip.z / w2;
        float screenX = (ndcX * 0.5f + 0.5f) * (float)mc.getWindow().getScaledWidth();
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * (float)mc.getWindow().getScaledHeight();
        if (Float.isNaN(screenX) || Float.isNaN(screenY) || Float.isInfinite(screenX) || Float.isInfinite(screenY)) {
            return null;
        }
        if (screenX < -400.0f || screenY < -400.0f || screenX > (float)(mc.getWindow().getScaledWidth() + 400) || screenY > (float)(mc.getWindow().getScaledHeight() + 400)) {
            return null;
        }
        return new Vec3d((double)screenX, (double)screenY, (double)ndcZ);
    }

    private String formatOneDecimal(float value) {
        int scaled = Math.round(value * 10.0f);
        return scaled / 10 + "." + Math.abs(scaled % 10);
    }

    private List<Vec3d> simulate(EnderPearlEntity pearl, float tickDelta) {
        ArrayList<Vec3d> points = new ArrayList<Vec3d>();
        Vec3d pos = new Vec3d(MathHelper.lerp((double)tickDelta, (double)pearl.prevX, (double)pearl.getX()), MathHelper.lerp((double)tickDelta, (double)pearl.prevY, (double)pearl.getY()), MathHelper.lerp((double)tickDelta, (double)pearl.prevZ, (double)pearl.getZ()));
        Vec3d motion = pearl.getVelocity();
        points.add(pos);
        for (int i2 = 0; i2 < 300; ++i2) {
            Vec3d lastPos = pos;
            Vec3d nextPos = pos.add(motion);
            BlockHitResult hit = Projectile.mc.world.raycast(new RaycastContext(lastPos, nextPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)Projectile.mc.player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                points.add(hit.getPos());
                break;
            }
            points.add(nextPos);
            pos = nextPos;
            boolean inWater = Projectile.mc.world.getBlockState(BlockPos.ofFloored((Position)pos)).isOf(Blocks.WATER);
            double drag = inWater ? 0.8 : 0.99;
            motion = motion.multiply(drag).subtract(0.0, 0.03, 0.0);
            if (pos.y <= (double)Projectile.mc.world.getBottomY()) break;
        }
        return points;
    }

    private record ImpactPoint(Vec3d pos, float seconds) {
    }
}

