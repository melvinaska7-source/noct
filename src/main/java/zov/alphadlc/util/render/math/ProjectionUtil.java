package zov.alphadlc.util.render.math;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ProjectionUtil {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Vector2f project(double x, double y, double z) {
        return project(x, y, z, mc.getRenderTickCounter().getTickDelta(true));
    }

    public static Vector2f project(double x, double y, double z, float tickDelta) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d cameraPos = camera.getPos();

        Quaternionf cameraRotation = mc.getEntityRenderDispatcher().getRotation();
        cameraRotation = cameraRotation.conjugate(new Quaternionf());

        Vector3f result3f = new Vector3f(
                (float) (x - cameraPos.x),
                (float) (y - cameraPos.y),
                (float) (z - cameraPos.z)
        );

        result3f.rotate(cameraRotation);

        if (mc.options.getBobView().getValue()) {
            if (mc.getCameraEntity() instanceof AbstractClientPlayerEntity playerentity) {
                calculateViewBobbing(playerentity, result3f, tickDelta);
            }
        }

        double fov = mc.gameRenderer.getFov(camera, tickDelta, true);
        return calculateScreenPosition(result3f, fov);
    }

    public static Vector2f project(Vec3d vec3d) {
        return project(vec3d, mc.getRenderTickCounter().getTickDelta(true));
    }

    public static Vector2f project(Vec3d vec3d, float tickDelta) {
        return project(vec3d.x, vec3d.y, vec3d.z, tickDelta);
    }

    private static void calculateViewBobbing(AbstractClientPlayerEntity playerEntity, Vector3f result3f, float tickDelta) {
        float distanceDelta = playerEntity.distanceMoved - playerEntity.lastDistanceMoved;
        float distance = -(playerEntity.distanceMoved + distanceDelta * tickDelta);
        float stride = MathHelper.lerp(tickDelta, playerEntity.prevStrideDistance, playerEntity.strideDistance);
        float phase = distance * (float) Math.PI;

        float xRotation = Math.abs(MathHelper.cos(phase - 0.2F) * stride) * 5.0F;
        float zRotation = MathHelper.sin(phase) * stride * 3.0F;

        // Vanilla builds T * Rz * Rx. Apply it to the already camera-relative
        // point from right to left, exactly as MatrixStack transforms a vertex.
        result3f.rotate(new Quaternionf().rotationX((float) Math.toRadians(xRotation)));
        result3f.rotate(new Quaternionf().rotationZ((float) Math.toRadians(zRotation)));
        result3f.add(
                MathHelper.sin(phase) * stride * 0.5F,
                -Math.abs(MathHelper.cos(phase) * stride),
                0.0F
        );
    }

    private static Vector2f calculateScreenPosition(Vector3f result3f, double fov) {
        Window window = mc.getWindow();
        float width = window.getScaledWidth() / 2.0F;
        float height = window.getScaledHeight() / 2.0F;
        float x = result3f.x;
        float y = result3f.y;
        float z = result3f.z;

        float scaleFactor = height / (z * (float) Math.tan(Math.toRadians(fov / 2.0F)));
        if (z < 0.0F) {
            float screenX = -x * scaleFactor + width;
            float screenY = y * scaleFactor + height;
            return new Vector2f(screenX, screenY);
        }
        return new Vector2f(Float.MAX_VALUE, Float.MAX_VALUE);
    }
}