package zov.alphadlc.mixin;

import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zov.alphadlc.AlphaDLC;
import zov.alphadlc.module.list.render.Ambience;

@Mixin(BackgroundRenderer.class)
public class FogMixin {

    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
    private static void onApplyFog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f color,
                                    float viewDistance, boolean thickenFog, float tickDelta,
                                    CallbackInfoReturnable<Fog> cir) {
        try {
            if (camera.getSubmersionType() != CameraSubmersionType.NONE) return;
            if (AlphaDLC.getInstance() == null || AlphaDLC.getInstance().getModuleStorage() == null) return;

            Ambience ambience = AlphaDLC.getInstance().getModuleStorage().get(Ambience.class);
            if (ambience == null || !ambience.isEnabled()) return;

            float edgeEnd = getSafeEdgeEnd(viewDistance);
            if (ambience.isFogEnabled()) {
                float density = MathHelper.clamp(ambience.getFogDensity(), 0.0f, 2.0f);
                float denseShrink = density > 1.0f
                        ? MathHelper.lerp(density - 1.0f, 1.0f, 0.45f)
                        : 1.0f;
                float fogEnd = Math.max(2.0f,
                        Math.min(ambience.getFogDistance(), edgeEnd) * denseShrink);
                fogEnd = Math.min(fogEnd, edgeEnd);
                float fogStart = fogEnd * (1.0f - Math.min(density, 1.0f));
                cir.setReturnValue(createFog(ambience.getFogColor(), color.w, fogStart, fogEnd));
            } else if (ambience.isSkyShaderEnabled() && ambience.isRemoveFogEnabled()) {
                // Keep almost the whole scene clear, but retain a short veil before unloaded terrain.
                float fogStart = edgeEnd * 0.84f;
                cir.setReturnValue(createFog(ambience.getFogColor(), color.w, fogStart, edgeEnd));
            }
        } catch (Exception ignored) {
        }
    }

    private static float getSafeEdgeEnd(float viewDistance) {
        float edgeBuffer = MathHelper.clamp(viewDistance * 0.08f, 8.0f, 24.0f);
        return Math.max(4.0f, viewDistance - edgeBuffer);
    }

    private static Fog createFog(int color, float alpha, float start, float end) {
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        return new Fog(start, end, FogShape.SPHERE, red, green, blue, alpha);
    }
}
