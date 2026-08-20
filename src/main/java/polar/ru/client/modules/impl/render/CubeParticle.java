package polar.ru.client.modules.impl.render;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import polar.ru.api.QClient;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.client.modules.impl.render.TargetESP;

class CubeParticle
implements QClient {
    double x;
    double y;
    double z;
    double worldX;
    double worldY;
    double worldZ;
    long time;
    LivingEntity entity;
    boolean fading;
    long fadeStartTime;
    float vx;
    float vy;
    float vz;
    float rotX;
    float rotY;
    float rotZ;
    float rotSpeedX;
    float rotSpeedY;
    float rotSpeedZ;

    public CubeParticle(LivingEntity entity, double x2, double y2, double z2) {
        this.entity = entity;
        this.x = x2;
        this.y = y2;
        this.z = z2;
        this.time = System.currentTimeMillis();
        this.rotX = (float)(Math.random() * 360.0);
        this.rotY = (float)(Math.random() * 360.0);
        this.rotZ = (float)(Math.random() * 360.0);
        this.rotSpeedX = 1.4f + (float)Math.random() * 3.4f;
        this.rotSpeedY = 1.4f + (float)Math.random() * 3.4f;
        this.rotSpeedZ = 1.4f + (float)Math.random() * 3.4f;
        this.vx = (float)((Math.random() - 0.5) * 0.0022);
        this.vy = 0.031f + (float)Math.random() * 0.02f;
        this.vz = (float)((Math.random() - 0.5) * 0.0022);
    }

    public void update(float dt, long now, LivingEntity currentTarget) {
        float step = dt * 60.0f;
        this.rotX += this.rotSpeedX * step;
        this.rotY += this.rotSpeedY * step;
        this.rotZ += this.rotSpeedZ * step;
        if (!this.fading) {
            boolean targetLost;
            double shoulderHeight;
            this.x += (double)(this.vx * step);
            this.y += (double)(this.vy * step);
            this.z += (double)(this.vz * step);
            this.vx *= 0.992f;
            this.vz *= 0.992f;
            this.vy *= 0.989f;
            if (this.entity != null && this.y >= (shoulderHeight = Math.max(2.2, (double)this.entity.getHeight() * 1.85))) {
                this.y = shoulderHeight;
                this.beginFade(now);
                return;
            }
            boolean bl = targetLost = currentTarget == null || this.entity == null || !this.entity.isAlive() || this.entity != currentTarget;
            if (targetLost || now - this.time >= 560L) {
                this.beginFade(now);
            }
        }
    }

    public boolean shouldRemove(long now) {
        return this.fading && now - this.fadeStartTime >= 320L;
    }

    public int getRenderColor(int baseColor, int redColor, float hurtPC, long now) {
        float alpha = this.getAlpha(now);
        if (alpha <= 0.001f) {
            return 0;
        }
        int color = ColorUtils.replAlpha(baseColor, (int)(alpha * 255.0f));
        int hurt = ColorUtils.replAlpha(redColor, (int)(alpha * 255.0f));
        return TargetESP.INSTANCE.overCol(color, hurt, hurtPC);
    }

    public boolean appendCubeFaces(BufferBuilder faceBuilder, MatrixStack ms, Vec3d cam, float partialTicks, int color) {
        float alpha = (float)(color >> 24 & 0xFF) / 255.0f;
        if (alpha <= 0.001f) {
            return false;
        }
        Vec3d renderPos = this.getRenderPos(partialTicks);
        if (renderPos == null) {
            return false;
        }
        float fadeScale = this.fading ? MathHelper.lerp((float)MathHelper.clamp((float)((float)(System.currentTimeMillis() - this.fadeStartTime) / 320.0f), (float)0.0f, (float)1.0f), (float)1.0f, (float)0.45f) : 1.0f;
        float scale = 0.12f * fadeScale;
        ms.push();
        ms.translate(renderPos.x - cam.x, renderPos.y - cam.y, renderPos.z - cam.z);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.rotX));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotY));
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotZ));
        ms.scale(scale, scale, scale);
        Matrix4f m2 = ms.peek().getPositionMatrix();
        this.appendFaces(faceBuilder, m2, color);
        ms.pop();
        return true;
    }

    public boolean appendCubeLines(BufferBuilder lineBuilder, MatrixStack ms, Vec3d cam, float partialTicks, int color) {
        float alpha = (float)(color >> 24 & 0xFF) / 255.0f;
        if (alpha <= 0.001f) {
            return false;
        }
        Vec3d renderPos = this.getRenderPos(partialTicks);
        if (renderPos == null) {
            return false;
        }
        float fadeScale = this.fading ? MathHelper.lerp((float)MathHelper.clamp((float)((float)(System.currentTimeMillis() - this.fadeStartTime) / 320.0f), (float)0.0f, (float)1.0f), (float)1.0f, (float)0.45f) : 1.0f;
        float scale = 0.12f * fadeScale;
        ms.push();
        ms.translate(renderPos.x - cam.x, renderPos.y - cam.y, renderPos.z - cam.z);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.rotX));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotY));
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotZ));
        ms.scale(scale, scale, scale);
        Matrix4f m2 = ms.peek().getPositionMatrix();
        this.appendEdges(lineBuilder, m2, ColorUtils.replAlpha(color, Math.max(1, (int)((float)(color >> 24 & 0xFF) * 0.7f))));
        ms.pop();
        return true;
    }

    public boolean appendBloom(BufferBuilder builder, MatrixStack ms, Vec3d camPos, float camYaw, float camPitch, float partialTicks, int colorInt, long now) {
        float alpha = this.getAlpha(now);
        if (alpha <= 0.001f) {
            return false;
        }
        Vec3d renderPos = this.getRenderPos(partialTicks);
        if (renderPos == null) {
            return false;
        }
        float fadeScale = this.fading ? MathHelper.lerp((float)MathHelper.clamp((float)((float)(now - this.fadeStartTime) / 320.0f), (float)0.0f, (float)1.0f), (float)1.0f, (float)0.55f) : 1.0f;
        float glowScale = 0.95f * fadeScale;
        int ai2 = (int)(alpha * 0.15f * 255.0f);
        if (ai2 <= 0) {
            return false;
        }
        int r2 = colorInt >> 16 & 0xFF;
        int g2 = colorInt >> 8 & 0xFF;
        int b2 = colorInt & 0xFF;
        ms.push();
        ms.translate(renderPos.x - camPos.x, renderPos.y - camPos.y, renderPos.z - camPos.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));
        ms.scale(glowScale, glowScale, glowScale);
        Matrix4f m2 = ms.peek().getPositionMatrix();
        builder.vertex(m2, -0.5f, 0.5f, 0.0f).texture(0.0f, 1.0f).color(r2, g2, b2, ai2);
        builder.vertex(m2, 0.5f, 0.5f, 0.0f).texture(1.0f, 1.0f).color(r2, g2, b2, ai2);
        builder.vertex(m2, 0.5f, -0.5f, 0.0f).texture(1.0f, 0.0f).color(r2, g2, b2, ai2);
        builder.vertex(m2, -0.5f, -0.5f, 0.0f).texture(0.0f, 0.0f).color(r2, g2, b2, ai2);
        ms.pop();
        return true;
    }

    private void beginFade(long now) {
        if (this.fading) {
            return;
        }
        Vec3d renderPos = this.getRenderPos(1.0f);
        if (renderPos != null) {
            this.worldX = renderPos.x;
            this.worldY = renderPos.y;
            this.worldZ = renderPos.z;
        }
        this.fadeStartTime = now;
        this.fading = true;
        this.entity = null;
    }

    private float getAlpha(long now) {
        if (!this.fading) {
            float fadeIn = MathHelper.clamp((float)((float)(now - this.time) / 140.0f), (float)0.0f, (float)1.0f);
            float preFade = 1.0f - MathHelper.clamp((float)((float)(now - this.time - 440L) / 120.0f), (float)0.0f, (float)0.35f);
            return fadeIn * preFade;
        }
        return 1.0f - MathHelper.clamp((float)((float)(now - this.fadeStartTime) / 320.0f), (float)0.0f, (float)1.0f);
    }

    private Vec3d getRenderPos(float partialTicks) {
        if (this.fading || this.entity == null) {
            return new Vec3d(this.worldX, this.worldY, this.worldZ);
        }
        return new Vec3d(MathHelper.lerp((double)partialTicks, (double)this.entity.lastRenderX, (double)this.entity.getX()) + this.x, MathHelper.lerp((double)partialTicks, (double)this.entity.lastRenderY, (double)this.entity.getY()) + this.y, MathHelper.lerp((double)partialTicks, (double)this.entity.lastRenderZ, (double)this.entity.getZ()) + this.z);
    }

    private void appendFaces(BufferBuilder fb, Matrix4f m2, int color) {
        float min = -0.5f;
        float max = 0.5f;
        int fillColor = ColorUtils.replAlpha(color, Math.max(1, (int)((float)(color >> 24 & 0xFF) * 0.16f)));
        this.addFace(fb, m2, min, min, min, max, max, max, fillColor);
    }

    private void appendEdges(BufferBuilder buf, Matrix4f m2, int color) {
        for (byte[] edge : TargetESP.CUBE_EDGES) {
            buf.vertex(m2, (float)edge[0] * 0.5f, (float)edge[1] * 0.5f, (float)edge[2] * 0.5f).color(color);
            buf.vertex(m2, (float)edge[3] * 0.5f, (float)edge[4] * 0.5f, (float)edge[5] * 0.5f).color(color);
        }
    }

    private void addFace(BufferBuilder buf, Matrix4f m2, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        buf.vertex(m2, x1, y1, z1).color(color);
        buf.vertex(m2, x2, y1, z1).color(color);
        buf.vertex(m2, x2, y1, z2).color(color);
        buf.vertex(m2, x1, y1, z2).color(color);
        buf.vertex(m2, x1, y2, z1).color(color);
        buf.vertex(m2, x1, y2, z2).color(color);
        buf.vertex(m2, x2, y2, z2).color(color);
        buf.vertex(m2, x2, y2, z1).color(color);
        buf.vertex(m2, x1, y1, z1).color(color);
        buf.vertex(m2, x1, y2, z1).color(color);
        buf.vertex(m2, x2, y2, z1).color(color);
        buf.vertex(m2, x2, y1, z1).color(color);
        buf.vertex(m2, x1, y1, z2).color(color);
        buf.vertex(m2, x2, y1, z2).color(color);
        buf.vertex(m2, x2, y2, z2).color(color);
        buf.vertex(m2, x1, y2, z2).color(color);
        buf.vertex(m2, x1, y1, z1).color(color);
        buf.vertex(m2, x1, y1, z2).color(color);
        buf.vertex(m2, x1, y2, z2).color(color);
        buf.vertex(m2, x1, y2, z1).color(color);
        buf.vertex(m2, x2, y1, z1).color(color);
        buf.vertex(m2, x2, y2, z1).color(color);
        buf.vertex(m2, x2, y2, z2).color(color);
        buf.vertex(m2, x2, y1, z2).color(color);
    }
}

