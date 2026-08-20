package polar.ru.mixin;

import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profilers;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.api.QClient;
import polar.ru.api.events.EventInvoker;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.client.modules.impl.render.Chams;
import polar.ru.client.modules.impl.render.Removals;
import polar.ru.client.modules.impl.render.ShaderEsp;
import polar.ru.client.modules.impl.render.Sonar;

@Mixin(value={WorldRenderer.class})
public class WorldRendererMixin
implements QClient {
    @Inject(method={"renderParticles"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$renderParticles(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) {
            return;
        }
        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("Частицы")) {
            ci.cancel();
        }
    }

    @Inject(method={"renderWeather"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$renderWeather(FrameGraphBuilder frameGraphBuilder, Vec3d pos, float tickDelta, Fog fog, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) {
            return;
        }
        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("Погода")) {
            ci.cancel();
        }
    }

    @Inject(method={"addWeatherParticlesAndSound"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$addWeatherParticlesAndSound(Camera camera, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) {
            return;
        }
        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("Погода")) {
            ci.cancel();
        }
    }

    @Inject(method={"renderClouds"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$renderClouds(FrameGraphBuilder frameGraphBuilder, Matrix4f positionMatrix, Matrix4f projectionMatrix, CloudRenderMode renderMode, Vec3d cameraPos, float ticks, int color, float cloudHeight, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) {
            return;
        }
        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("Облака")) {
            ci.cancel();
        }
    }

    @Inject(method={"renderBlockEntities"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$renderBlockEntities(MatrixStack matrices, VertexConsumerProvider.Immediate mainConsumers, VertexConsumerProvider.Immediate translucentConsumers, Camera camera, float tickDelta, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) {
            return;
        }
        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("Блок-сущности")) {
            ci.cancel();
        }
    }

    @Inject(method={"render"}, at={@At(value="RETURN")})
    private void render(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        boolean renderSonar;
        Sonar sonar = ModuleClass.INSTANCE != null ? ModuleClass.sonar : null;
        boolean has3DListeners = EventInvoker.hasListeners(Event3DRender.class);
        boolean bl = renderSonar = sonar != null && sonar.isEnable();
        if (!has3DListeners && !renderSonar) {
            return;
        }
        Profilers.get().swap("polar_renderWorld");
        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(positionMatrix);
        com.mojang.blaze3d.systems.RenderSystem.backupProjectionMatrix();
        com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projectionMatrix, com.mojang.blaze3d.systems.ProjectionType.PERSPECTIVE);
        try {
            if (has3DListeners) {
                new Event3DRender(matrices, positionMatrix, projectionMatrix, camera, tickCounter.getTickDelta(false)).call();
            }
            if (renderSonar) {
                sonar.renderFromMixin(positionMatrix, projectionMatrix, camera.getPos());
            }
        } finally {
            com.mojang.blaze3d.systems.RenderSystem.restoreProjectionMatrix();
        }
    }

    @Inject(method={"drawEntityOutlinesFramebuffer"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$drawEntityOutlinesFramebuffer(CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) {
            return;
        }
        ShaderEsp shaderEsp = ModuleClass.shaderEsp;
        if (shaderEsp != null && shaderEsp.isEnable()) {
            ci.cancel();
            return;
        }
        Chams chams = ModuleClass.chams;
        if (chams != null && chams.shouldHideOutlineFramebuffer()) {
            ci.cancel();
        }
    }

    @Inject(method={"drawBlockOutline"}, at={@At(value="HEAD")}, cancellable=true)
    public void onDrawBlockOutline(CallbackInfo ci) {
        if (ModuleClass.blockOverlay.isEnable()) {
            ci.cancel();
        }
    }
}

