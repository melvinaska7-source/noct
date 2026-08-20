package polar.ru.client.modules.impl.render.pets;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.MathHelper;
import polar.ru.client.modules.impl.render.pets.DogBrain;

public final class PigModel
extends Model {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightBackLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;

    public PigModel(ModelPart root) {
        super(root, RenderLayer::getEntityTranslucent);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightBackLeg = root.getChild("right_hind_leg");
        this.leftBackLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        root.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0f, -4.0f, -8.0f, 8.0f, 8.0f, 8.0f).uv(16, 16).cuboid(-2.0f, 0.0f, -9.0f, 4.0f, 3.0f, 1.0f), ModelTransform.pivot((float)0.0f, (float)12.0f, (float)-6.0f));
        root.addChild("body", ModelPartBuilder.create().uv(28, 8).cuboid(-5.0f, -10.0f, -7.0f, 10.0f, 16.0f, 8.0f), ModelTransform.of((float)0.0f, (float)11.0f, (float)2.0f, (float)1.5707964f, (float)0.0f, (float)0.0f));
        root.addChild("right_hind_leg", ModelPartBuilder.create().uv(0, 16).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f), ModelTransform.pivot((float)-3.0f, (float)18.0f, (float)7.0f));
        root.addChild("left_hind_leg", ModelPartBuilder.create().uv(0, 16).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f), ModelTransform.pivot((float)3.0f, (float)18.0f, (float)7.0f));
        root.addChild("right_front_leg", ModelPartBuilder.create().uv(0, 16).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f), ModelTransform.pivot((float)-3.0f, (float)18.0f, (float)-5.0f));
        root.addChild("left_front_leg", ModelPartBuilder.create().uv(0, 16).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f), ModelTransform.pivot((float)3.0f, (float)18.0f, (float)-5.0f));
        return TexturedModelData.of((ModelData)modelData, (int)64, (int)32);
    }

    public void setAngles(float ageInTicks, float partialTicks, DogBrain brain, int activeClickAnimation) {
        this.head.setPivot(0.0f, 12.0f, -6.0f);
        this.body.setPivot(0.0f, 11.0f, 2.0f);
        this.rightBackLeg.setPivot(-3.0f, 18.0f, 7.0f);
        this.leftBackLeg.setPivot(3.0f, 18.0f, 7.0f);
        this.rightFrontLeg.setPivot(-3.0f, 18.0f, -5.0f);
        this.leftFrontLeg.setPivot(3.0f, 18.0f, -5.0f);
        this.head.roll = 0.0f;
        this.body.pitch = 1.5707964f;
        this.body.yaw = 0.0f;
        this.body.roll = 0.0f;
        this.head.pitch = brain.getRenderPitch(partialTicks) * ((float)Math.PI / 180);
        this.head.yaw = brain.getRenderYaw(partialTicks) * ((float)Math.PI / 360);
        float limbSwing = brain.getRenderLimbSwing(partialTicks);
        float limbSwingAmount = brain.getRenderLimbSwingAmount(partialTicks);
        this.rightBackLeg.pitch = MathHelper.cos((float)(limbSwing * 0.6662f)) * 1.4f * limbSwingAmount;
        this.leftBackLeg.pitch = MathHelper.cos((float)(limbSwing * 0.6662f + (float)Math.PI)) * 1.4f * limbSwingAmount;
        this.rightFrontLeg.pitch = MathHelper.cos((float)(limbSwing * 0.6662f + (float)Math.PI)) * 1.4f * limbSwingAmount;
        this.leftFrontLeg.pitch = MathHelper.cos((float)(limbSwing * 0.6662f)) * 1.4f * limbSwingAmount;
        this.applyIdleAnimation(brain.getIdleAnimationTicks(partialTicks));
        this.applyClickAnimation(brain.getHypeAnimationTicks(partialTicks), brain.getHypeAnimationProgress(partialTicks), brain, activeClickAnimation, partialTicks);
    }

    private void applyIdleAnimation(float ticks) {
        if (ticks <= 0.0f) {
            return;
        }
        float blend = MathHelper.clamp((float)(ticks / 18.0f), (float)0.0f, (float)1.0f);
        blend = blend * blend * (3.0f - 2.0f * blend);
        float cycle = ticks * 0.13f;
        float breathe = 0.5f + 0.5f * MathHelper.sin((float)cycle);
        float headBob = MathHelper.sin((float)(cycle * 1.7f + 0.4f));
        this.body.roll += 0.018f * headBob * blend;
        this.head.pitch += (0.05f + 0.06f * breathe) * blend;
        this.head.yaw += 0.08f * headBob * blend;
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
        float fastBeat = MathHelper.sin((float)(ticks * 1.44f));
        float side = MathHelper.sin((float)(ticks * 0.36f));
        this.body.roll += 0.18f * side * amount;
        this.head.pitch += (-0.24f + 0.18f * fastBeat) * amount;
        this.head.yaw += 0.24f * side * amount;
        this.head.roll += 0.16f * beat * amount;
        this.rightFrontLeg.pitch = -0.78f * amount + 0.26f * fastBeat * amount;
        this.leftFrontLeg.pitch = -0.78f * amount - 0.26f * fastBeat * amount;
        this.rightBackLeg.pitch += 0.18f * beat * amount;
        this.leftBackLeg.pitch -= 0.18f * beat * amount;
    }

    private void applyBurnoutAnimation(float ticks, float progress) {
        if (ticks <= 0.0f || progress <= 0.0f || progress >= 1.0f) {
            return;
        }
        float amount = MathHelper.sin((float)(progress * (float)Math.PI));
        float launch = MathHelper.clamp((float)(progress / 0.22f), (float)0.0f, (float)1.0f);
        launch = launch * launch * (3.0f - 2.0f * launch);
        float spinPose = MathHelper.clamp((float)((progress - 0.1f) / 0.7f), (float)0.0f, (float)1.0f);
        spinPose = MathHelper.sin((float)(spinPose * (float)Math.PI));
        float landing = MathHelper.clamp((float)((progress - 0.72f) / 0.28f), (float)0.0f, (float)1.0f);
        this.body.roll += (-0.42f * launch + 0.24f * landing) * amount;
        this.head.pitch += (-0.3f * launch + 0.18f * landing) * amount;
        this.rightFrontLeg.pitch = -1.18f * spinPose * amount;
        this.leftFrontLeg.pitch = -1.18f * spinPose * amount;
        this.rightBackLeg.pitch = 0.82f * spinPose * amount;
        this.leftBackLeg.pitch = 0.82f * spinPose * amount;
    }

    private void applyBurnout2Animation(float ticks, float warmupProgress, float flipProgress) {
        if (ticks <= 0.0f) {
            return;
        }
        if (warmupProgress > 0.0f && flipProgress <= 0.0f) {
            float warmup = warmupProgress * warmupProgress * (3.0f - 2.0f * warmupProgress);
            float rev = MathHelper.sin((float)(ticks * 1.85f));
            float revFast = MathHelper.sin((float)(ticks * 3.7f));
            this.body.roll += 0.035f * revFast * warmup;
            this.head.pitch += 0.16f * warmup;
            this.head.yaw += 0.08f * rev * warmup;
            this.rightFrontLeg.pitch = (-0.42f + 0.1f * rev) * warmup;
            this.leftFrontLeg.pitch = (-0.42f - 0.1f * rev) * warmup;
            this.rightBackLeg.pitch = 0.95f * Math.max(0.0f, rev) * warmup;
            this.leftBackLeg.pitch = 0.95f * Math.max(0.0f, -rev) * warmup;
            return;
        }
        this.applyBurnoutAnimation(ticks, flipProgress);
    }
}

