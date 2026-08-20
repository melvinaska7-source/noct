package polar.ru.client.modules.impl.render.pets;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import polar.ru.client.modules.impl.render.pets.DogBrain;

public final class DachshundModel
extends Model {
    private static final float TW = 60.0f;
    private static final float TH = 36.0f;
    private static final float S = 0.0625f;
    private final ModelPart root;
    private float headYaw;
    private float headPitch;
    private float bodyYaw;
    private float frontLeftLegX;
    private float frontRightLegX;
    private float backLeftLegX;
    private float backRightLegX;
    private float frontLeftLegY;
    private float frontRightLegY;
    private float backLeftLegY;
    private float backRightLegY;
    private float tailX;
    private float tailZ;
    private boolean lay;

    public DachshundModel(ModelPart root) {
        super(root, RenderLayer::getEntityTranslucent);
        this.root = root;
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        root.addChild("main", ModelPartBuilder.create(), ModelTransform.NONE);
        return TexturedModelData.of((ModelData)modelData, (int)64, (int)64);
    }

    public void setAngles(float ageInTicks, float partialTicks, DogBrain brain, int activeClickAnimation) {
        this.headYaw = -brain.getRenderYaw(partialTicks) * ((float)Math.PI / 180);
        this.headPitch = brain.getRenderPitch(partialTicks) * ((float)Math.PI / 180);
        this.bodyYaw = brain.getRenderBody(partialTicks);
        this.lay = brain.isLay();
        float swing = brain.getRenderLimbSwing(partialTicks);
        float amount = brain.getRenderLimbSwingAmount(partialTicks);
        this.frontLeftLegX = MathHelper.cos((float)(swing * 0.6662f)) * 1.4f * amount;
        this.frontRightLegX = MathHelper.cos((float)(swing * 0.6662f + (float)Math.PI)) * 1.4f * amount;
        this.backLeftLegX = MathHelper.cos((float)(swing * 0.6662f + (float)Math.PI)) * 1.4f * amount;
        this.backRightLegX = MathHelper.cos((float)(swing * 0.6662f)) * 1.4f * amount;
        if (this.lay) {
            this.frontLeftLegX = (float)Math.toRadians(-90.0);
            this.frontRightLegX = (float)Math.toRadians(-90.0);
            this.backLeftLegX = (float)Math.toRadians(90.0);
            this.backRightLegX = (float)Math.toRadians(90.0);
            this.frontLeftLegY = (float)Math.toRadians(-22.0);
            this.frontRightLegY = (float)Math.toRadians(22.0);
            this.backLeftLegY = (float)Math.toRadians(22.0);
            this.backRightLegY = (float)Math.toRadians(-22.0);
        } else {
            this.backRightLegY = 0.0f;
            this.backLeftLegY = 0.0f;
            this.frontRightLegY = 0.0f;
            this.frontLeftLegY = 0.0f;
        }
        this.tailX = (float)Math.toRadians(this.lay ? 45.0 : 22.0);
        this.tailZ = (float)Math.toRadians(-22.5) + (float)Math.toRadians(22.5) + (float)Math.cos(ageInTicks * 0.15f) * 0.3f;
        this.applyClickAnimation(brain.getHypeAnimationTicks(partialTicks), brain.getHypeAnimationProgress(partialTicks), brain, activeClickAnimation, partialTicks);
    }

    public void renderDachshund(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        matrices.push();
        matrices.translate(0.0, (double)(0.2f - (this.lay ? 0.3f : 0.0f)), 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.bodyYaw));
        this.renderHead(matrices, vertices, light, overlay);
        this.renderNeck(matrices, vertices, light, overlay);
        this.renderBody(matrices, vertices, light, overlay);
        this.renderLegs(matrices, vertices, light, overlay);
        this.renderTail(matrices, vertices, light, overlay);
        matrices.pop();
    }

    private void renderHead(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.0f, 0.65625f, -0.425f);
        ms.multiply(RotationAxis.POSITIVE_Y.rotation(this.headYaw));
        ms.multiply(RotationAxis.POSITIVE_X.rotation(this.headPitch));
        this.cube(ms, vc, light, overlay, -3.0f, -3.0f, -4.0f, 6.0f, 6.0f, 4.0f, 0, 0, false);
        this.cube(ms, vc, light, overlay, -1.5f, 0.0f, -7.0f, 3.0f, 3.0f, 3.0f, 21, 0, false);
        ms.push();
        ms.translate(0.1875f, 0.1875f, -0.125f);
        this.cube(ms, vc, light, overlay, 0.0f, -5.0f, -1.5f, 1.0f, 3.0f, 3.0f, 32, 4, false);
        this.cube(ms, vc, light, overlay, 0.0f, -5.5f, -0.75f, 1.0f, 1.0f, 1.5f, 34, 1, false);
        ms.pop();
        ms.push();
        ms.translate(-0.1875f, 0.1875f, -0.125f);
        this.cube(ms, vc, light, overlay, -1.0f, -5.0f, -1.5f, 1.0f, 3.0f, 3.0f, 32, 4, true);
        this.cube(ms, vc, light, overlay, -1.0f, -5.5f, -0.75f, 1.0f, 1.0f, 1.5f, 34, 1, true);
        ms.pop();
        ms.pop();
    }

    private void renderNeck(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.0f, 0.65625f, -0.3125f);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0f));
        this.cube(ms, vc, light, overlay, -2.95f, -1.0f, -4.0f, 5.9f, 5.0f, 6.0f, 15, 7, false);
        ms.pop();
    }

    private void renderBody(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.0f, 0.84375f, -0.3125f);
        ms.push();
        ms.translate(0.0f, 0.0f, 0.1875f);
        this.cube(ms, vc, light, overlay, -4.0f, -3.5f, -3.0f, 8.0f, 7.0f, 6.0f, 32, 13, false);
        ms.pop();
        ms.push();
        ms.translate(0.0f, -0.03125f, 0.34375f);
        this.cube(ms, vc, light, overlay, -3.0f, -3.0f, -0.5f, 6.0f, 6.0f, 11.0f, 3, 19, false);
        ms.pop();
        ms.pop();
    }

    private void renderLegs(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.09375f, 1.0f, -0.1875f);
        ms.multiply(RotationAxis.POSITIVE_Y.rotation(this.frontLeftLegY));
        ms.multiply(RotationAxis.POSITIVE_X.rotation(this.frontLeftLegX));
        this.cube(ms, vc, light, overlay, -1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, 42, 0, false);
        ms.pop();
        ms.push();
        ms.translate(-0.09375f, 1.0f, -0.1875f);
        ms.multiply(RotationAxis.POSITIVE_Y.rotation(this.frontRightLegY));
        ms.multiply(RotationAxis.POSITIVE_X.rotation(this.frontRightLegX));
        this.cube(ms, vc, light, overlay, -1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, 42, 0, true);
        ms.pop();
        ms.push();
        ms.translate(0.09375f, 1.0f, 0.5625f);
        ms.multiply(RotationAxis.POSITIVE_Y.rotation(this.backLeftLegY));
        ms.multiply(RotationAxis.POSITIVE_X.rotation(this.backLeftLegX));
        this.cube(ms, vc, light, overlay, -1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, 52, 0, false);
        ms.pop();
        ms.push();
        ms.translate(-0.09375f, 1.0f, 0.5625f);
        ms.multiply(RotationAxis.POSITIVE_Y.rotation(this.backRightLegY));
        ms.multiply(RotationAxis.POSITIVE_X.rotation(this.backRightLegX));
        this.cube(ms, vc, light, overlay, -1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, 52, 0, true);
        ms.pop();
    }

    private void renderTail(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.0f, 0.5625f, 0.625f);
        ms.multiply(RotationAxis.POSITIVE_X.rotation(this.tailX));
        ms.multiply(RotationAxis.POSITIVE_Z.rotation(this.tailZ));
        this.cube(ms, vc, light, overlay, -1.0f, 2.0f, -1.0f, 2.0f, 8.0f, 2.0f, 2, 12, false);
        ms.pop();
    }

    private void cube(MatrixStack ms, VertexConsumer vc, int light, int overlay, float ox, float oy, float oz, float w2, float h2, float d2, int u2, int v2, boolean mirror) {
        float x0 = ox * 0.0625f;
        float x1 = (ox + w2) * 0.0625f;
        float y0 = oy * 0.0625f;
        float y1 = (oy + h2) * 0.0625f;
        float z0 = oz * 0.0625f;
        float z1 = (oz + d2) * 0.0625f;
        if (mirror) {
            float t2 = x0;
            x0 = x1;
            x1 = t2;
        }
        Matrix4f m4 = ms.peek().getPositionMatrix();
        MatrixStack.Entry entry = ms.peek();
        this.quad(vc, m4, entry, light, overlay, x1, y0, z1, ((float)u2 + d2) / 60.0f, ((float)v2 + d2) / 36.0f, x1, y0, z0, (float)u2 / 60.0f, ((float)v2 + d2) / 36.0f, x1, y1, z0, (float)u2 / 60.0f, ((float)v2 + d2 + h2) / 36.0f, x1, y1, z1, ((float)u2 + d2) / 60.0f, ((float)v2 + d2 + h2) / 36.0f, 1.0f, 0.0f, 0.0f);
        this.quad(vc, m4, entry, light, overlay, x0, y0, z0, ((float)u2 + 2.0f * d2 + w2) / 60.0f, ((float)v2 + d2) / 36.0f, x0, y0, z1, ((float)u2 + d2 + w2) / 60.0f, ((float)v2 + d2) / 36.0f, x0, y1, z1, ((float)u2 + d2 + w2) / 60.0f, ((float)v2 + d2 + h2) / 36.0f, x0, y1, z0, ((float)u2 + 2.0f * d2 + w2) / 60.0f, ((float)v2 + d2 + h2) / 36.0f, -1.0f, 0.0f, 0.0f);
        this.quad(vc, m4, entry, light, overlay, x1, y0, z1, ((float)u2 + d2 + w2) / 60.0f, (float)v2 / 36.0f, x0, y0, z1, ((float)u2 + d2) / 60.0f, (float)v2 / 36.0f, x0, y0, z0, ((float)u2 + d2) / 60.0f, ((float)v2 + d2) / 36.0f, x1, y0, z0, ((float)u2 + d2 + w2) / 60.0f, ((float)v2 + d2) / 36.0f, 0.0f, -1.0f, 0.0f);
        this.quad(vc, m4, entry, light, overlay, x1, y1, z0, ((float)u2 + d2 + 2.0f * w2) / 60.0f, (float)v2 / 36.0f, x0, y1, z0, ((float)u2 + d2 + w2) / 60.0f, (float)v2 / 36.0f, x0, y1, z1, ((float)u2 + d2 + w2) / 60.0f, ((float)v2 + d2) / 36.0f, x1, y1, z1, ((float)u2 + d2 + 2.0f * w2) / 60.0f, ((float)v2 + d2) / 36.0f, 0.0f, 1.0f, 0.0f);
        this.quad(vc, m4, entry, light, overlay, x1, y0, z0, ((float)u2 + d2 + w2) / 60.0f, ((float)v2 + d2) / 36.0f, x0, y0, z0, ((float)u2 + d2) / 60.0f, ((float)v2 + d2) / 36.0f, x0, y1, z0, ((float)u2 + d2) / 60.0f, ((float)v2 + d2 + h2) / 36.0f, x1, y1, z0, ((float)u2 + d2 + w2) / 60.0f, ((float)v2 + d2 + h2) / 36.0f, 0.0f, 0.0f, -1.0f);
        this.quad(vc, m4, entry, light, overlay, x0, y0, z1, ((float)u2 + 2.0f * d2 + 2.0f * w2) / 60.0f, ((float)v2 + d2) / 36.0f, x1, y0, z1, ((float)u2 + 2.0f * d2 + w2) / 60.0f, ((float)v2 + d2) / 36.0f, x1, y1, z1, ((float)u2 + 2.0f * d2 + w2) / 60.0f, ((float)v2 + d2 + h2) / 36.0f, x0, y1, z1, ((float)u2 + 2.0f * d2 + 2.0f * w2) / 60.0f, ((float)v2 + d2 + h2) / 36.0f, 0.0f, 0.0f, 1.0f);
    }

    private void quad(VertexConsumer vc, Matrix4f m4, MatrixStack.Entry entry, int light, int overlay, float x0, float y0, float z0, float u0, float v0, float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2, float x3, float y3, float z3, float u3, float v3, float nx, float ny, float nz) {
        vc.vertex(m4, x0, y0, z0).texture(u0, v0).color(1.0f, 1.0f, 1.0f, 1.0f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x1, y1, z1).texture(u1, v1).color(1.0f, 1.0f, 1.0f, 1.0f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x2, y2, z2).texture(u2, v2).color(1.0f, 1.0f, 1.0f, 1.0f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x3, y3, z3).texture(u3, v3).color(1.0f, 1.0f, 1.0f, 1.0f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
    }

    private void applyClickAnimation(float ticks, float progress, DogBrain brain, int activeClickAnimation, float partialTicks) {
        if (activeClickAnimation == 1) {
            this.applyBurnoutAnimation(ticks, progress);
        } else if (activeClickAnimation == 2) {
            this.applyBurnout2Animation(ticks, brain.getBurnout2WarmupProgress(partialTicks), brain.getBurnout2FlipProgress(partialTicks));
        } else {
            this.applyHypeAnimation(ticks, progress);
        }
    }

    private void applyHypeAnimation(float ticks, float progress) {
        if (ticks <= 0.0f || progress <= 0.0f || progress >= 1.0f) {
            return;
        }
        float amount = MathHelper.sin((float)(progress * (float)Math.PI));
        float beat = MathHelper.sin((float)(ticks * 0.72f));
        this.frontLeftLegX += -0.78f * amount + 0.26f * beat * amount;
        this.frontRightLegX += -0.78f * amount - 0.26f * beat * amount;
        this.backLeftLegX += 0.18f * beat * amount;
        this.backRightLegX -= 0.18f * beat * amount;
    }

    private void applyBurnoutAnimation(float ticks, float progress) {
        if (ticks <= 0.0f || progress <= 0.0f || progress >= 1.0f) {
            return;
        }
        float amount = MathHelper.sin((float)(progress * (float)Math.PI));
        float spinPose = MathHelper.clamp((float)((progress - 0.1f) / 0.7f), (float)0.0f, (float)1.0f);
        spinPose = MathHelper.sin((float)(spinPose * (float)Math.PI));
        this.frontLeftLegX = -1.18f * spinPose * amount;
        this.frontRightLegX = -1.18f * spinPose * amount;
        this.backLeftLegX = 0.82f * spinPose * amount;
        this.backRightLegX = 0.82f * spinPose * amount;
    }

    private void applyBurnout2Animation(float ticks, float warmupProgress, float flipProgress) {
        if (ticks <= 0.0f) {
            return;
        }
        if (warmupProgress > 0.0f && flipProgress <= 0.0f) {
            float warmup = warmupProgress * warmupProgress * (3.0f - 2.0f * warmupProgress);
            float rev = MathHelper.sin((float)(ticks * 1.85f));
            this.frontLeftLegX = (-0.42f + 0.1f * rev) * warmup;
            this.frontRightLegX = (-0.42f - 0.1f * rev) * warmup;
            this.backLeftLegX = 0.95f * Math.max(0.0f, rev) * warmup;
            this.backRightLegX = 0.95f * Math.max(0.0f, -rev) * warmup;
            return;
        }
        this.applyBurnoutAnimation(ticks, flipProgress);
    }
}

