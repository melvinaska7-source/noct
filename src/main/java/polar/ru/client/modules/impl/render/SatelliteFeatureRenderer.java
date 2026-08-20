package polar.ru.client.modules.impl.render;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.AllayEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.AllayEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class SatelliteFeatureRenderer
extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static final Identifier ALLAY_TEXTURE = Identifier.ofVanilla((String)"textures/entity/allay/allay.png");
    private final AllayEntityModel model;
    private final AllayEntityRenderState allayState = new AllayEntityRenderState();

    public SatelliteFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context, EntityRendererFactory.Context rendererContext) {
        super(context);
        this.model = new AllayEntityModel(rendererContext.getPart(EntityModelLayers.ALLAY));
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, PlayerEntityRenderState playerState, float yawDegrees, float pitch) {
        float animationAge;
        matrices.push();
        float baseY = playerState.isInSneakingPose ? -1.3f : -1.5f;
        float idleBob = 0.0f;
        float idleYaw = 0.0f;
        float idleRoll = 0.0f;
        float idlePitch = 0.0f;
        this.allayState.age = animationAge = playerState.age;
        this.allayState.limbFrequency = playerState.limbFrequency;
        this.allayState.limbAmplitudeMultiplier = playerState.limbAmplitudeMultiplier;
        this.allayState.yawDegrees = yawDegrees;
        this.allayState.pitch = pitch;
        this.allayState.invisible = playerState.invisible;
        this.allayState.invisibleToPlayer = playerState.invisibleToPlayer;
        this.allayState.hasOutline = playerState.hasOutline;
        this.allayState.shaking = playerState.shaking;
        this.allayState.baby = false;
        this.allayState.touchingWater = playerState.touchingWater;
        this.allayState.bodyYaw = playerState.bodyYaw;
        this.allayState.baseScale = 1.0f;
        this.allayState.ageScale = 1.0f;
        this.allayState.pose = playerState.pose;
        this.allayState.deathTime = 0.0f;
        this.allayState.hurt = playerState.hurt;
        this.allayState.dancing = false;
        this.allayState.spinning = false;
        this.allayState.spinningAnimationTicks = 0.0f;
        this.allayState.itemHoldAnimationTicks = 0.0f;
        this.model.setAngles(this.allayState);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.model.getLayer(ALLAY_TEXTURE));
        this.model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}

