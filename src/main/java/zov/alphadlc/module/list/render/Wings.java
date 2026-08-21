package zov.alphadlc.module.list.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ColorSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.friend.FriendRepository;
import zov.alphadlc.util.render.providers.ColorProvider;

@ModuleInformation(moduleName = "Wings", moduleDesc = "Объёмные драконьи крылья за спиной", moduleCategory = ModuleCategory.RENDER)
public class Wings extends Module {

    private static final WingPoint[] POINTS = {
            new WingPoint(0.04f,  0.10f, -0.05f, 0.00f, 0.36f, 1.00f), // 0 root
            new WingPoint(0.36f,  0.58f, -0.10f, 0.25f, 0.00f, 0.98f), // 1 elbow
            new WingPoint(0.73f,  0.42f,  0.02f, 0.53f, 0.12f, 1.00f), // 2 wrist
            new WingPoint(1.00f,  0.57f,  0.09f, 0.76f, 0.04f, 0.94f), // 3 first finger joint
            new WingPoint(1.28f,  0.55f,  0.13f, 1.00f, 0.06f, 0.72f), // 4 first finger tip
            new WingPoint(1.02f,  0.33f,  0.10f, 0.79f, 0.25f, 0.94f), // 5 second finger joint
            new WingPoint(1.31f,  0.11f,  0.14f, 1.00f, 0.36f, 0.70f), // 6 second finger tip
            new WingPoint(0.98f,  0.09f,  0.06f, 0.76f, 0.47f, 0.94f), // 7 third finger joint
            new WingPoint(1.19f, -0.30f,  0.08f, 0.91f, 0.72f, 0.68f), // 8 third finger tip
            new WingPoint(0.78f, -0.14f, -0.01f, 0.60f, 0.62f, 0.94f), // 9 fourth finger joint
            new WingPoint(0.87f, -0.65f, -0.04f, 0.67f, 1.00f, 0.66f), // 10 fourth finger tip
            new WingPoint(0.20f, -0.42f, -0.08f, 0.14f, 0.83f, 0.84f), // 11 inner trailing edge
            new WingPoint(1.00f,  0.32f,  0.11f, 0.77f, 0.28f, 0.82f), // 12 first scallop
            new WingPoint(1.01f, -0.05f,  0.08f, 0.78f, 0.54f, 0.80f), // 13 second scallop
            new WingPoint(0.82f, -0.35f,  0.00f, 0.63f, 0.78f, 0.78f)  // 14 third scallop
    };

    // Each fan terminates in an inset point, producing a genuinely scalloped trailing edge.
    private static final int[][] PANELS = {
            {0, 1, 2},
            {2, 3, 4}, {2, 4, 12}, {2, 12, 6}, {2, 6, 5},
            {2, 6, 13}, {2, 13, 8}, {2, 8, 7},
            {2, 8, 14}, {2, 14, 10}, {2, 10, 9},
            {0, 2, 9}, {0, 9, 10}, {0, 10, 11}
    };

    private static final int[] MEMBRANE_EDGE = {
            0, 1, 2, 3, 4, 12, 6, 13, 8, 14, 10, 11
    };

    private static final WingBone[] BONES = {
            new WingBone(0, 1, 0.052f, 0.044f),
            new WingBone(1, 2, 0.044f, 0.038f),
            new WingBone(2, 3, 0.032f, 0.025f),
            new WingBone(3, 4, 0.025f, 0.012f),
            new WingBone(2, 5, 0.030f, 0.023f),
            new WingBone(5, 6, 0.023f, 0.011f),
            new WingBone(2, 7, 0.028f, 0.021f),
            new WingBone(7, 8, 0.021f, 0.010f),
            new WingBone(2, 9, 0.027f, 0.020f),
            new WingBone(9, 10, 0.020f, 0.010f),
            new WingBone(0, 11, 0.030f, 0.012f)
    };

    private static final WingJoint[] JOINTS = {
            new WingJoint(0, 0.060f),
            new WingJoint(1, 0.055f),
            new WingJoint(2, 0.050f),
            new WingJoint(3, 0.032f),
            new WingJoint(5, 0.030f),
            new WingJoint(7, 0.028f),
            new WingJoint(9, 0.027f)
    };

    private static final float MEMBRANE_HALF_THICKNESS = 0.012f;

    private final ModeSetting mode = new ModeSetting("Режим", "Vanilla", "Vanilla", "Animated Fill", "Wave");
    private final ColorSetting color = new ColorSetting("Цвет", 0xFF7657FF);
    private final SliderSetting opacity = new SliderSetting("Прозрачность", 0.72, 0.10, 1.0, 0.05);
    private final SliderSetting shaderSpeed = new SliderSetting("Скорость", 1.0, 0.1, 3.0, 0.05)
            .setVisible(() -> !mode.is("Vanilla"));
    private final SliderSetting shaderScale = new SliderSetting("Масштаб узора", 1.35, 0.5, 3.0, 0.05)
            .setVisible(() -> mode.is("Animated Fill"));
    private final BooleanSetting self = new BooleanSetting("На себя", true);
    private final BooleanSetting players = new BooleanSetting("На игроков", false);
    private final SliderSetting size = new SliderSetting("Размер", 1.0, 0.75, 1.35, 0.05);

    private float selfBodyYaw;
    private boolean selfBodyYawInitialized;

    /** Called from WorldRenderer immediately before vanilla entities are drawn. */
    public void renderBeforeEntities(MatrixStack stack, Camera camera, float tickDelta,
                                     Iterable<Entity> visibleEntities) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;

        Vec3d cameraPos = camera.getPos();
        GlState state = GlState.capture();

        stack.push();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);

            if (self.getValue() && !mc.options.getPerspective().isFirstPerson() && mc.player.isAlive()) {
                renderWings(stack, mc.player, tickDelta, cameraPos);
            }

            if (players.getValue()) {
                for (Entity entity : visibleEntities) {
                    if (!(entity instanceof PlayerEntity player) || player == mc.player || !player.isAlive()) continue;
                    renderWings(stack, player, tickDelta, cameraPos);
                }
            }
        } finally {
            stack.pop();
            state.restore();
        }
    }

    private void renderWings(MatrixStack stack, PlayerEntity player, float tickDelta, Vec3d camera) {
        // Не рендерить крылья если игрок летит на элитре
        if (player.isGliding()) return;
        
        WingPose pose = resolvePose(player, tickDelta);
        if (pose == null) return;

        double x = MathHelper.lerp(tickDelta, player.prevX, player.getX()) - camera.x;
        double y = MathHelper.lerp(tickDelta, player.prevY, player.getY()) - camera.y;
        double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ()) - camera.z;
        float bodyYaw = resolveBodyYaw(player, tickDelta);
        float move = MathHelper.clamp(player.limbAnimator.getSpeed(tickDelta), 0f, 1f);
        float flap = (float) Math.sin((player.age + tickDelta) * pose.flapSpeed) * pose.flapAmplitude;
        float open = (pose.baseSpread + flap + move * pose.motionSpreadBoost) * pose.openMultiplier;
        float wingScale = size.getFloatValue() * pose.scaleMultiplier;

        int sourceColor = player != mc.player && FriendRepository.isFriend(player.getNameForScoreboard())
                ? FriendRepository.FRIEND_COLOR
                : color.getValue();
        int baseColor = setAlpha(sourceColor, Math.round(opacity.getFloatValue() * 255f));
        int coreColor = setAlpha(
                ColorProvider.interpolateColor(baseColor, 0xFFFFFFFF, 0.42f),
                Math.round(opacity.getFloatValue() * 255f));
        int glowColor = setAlpha(baseColor, Math.round(opacity.getFloatValue() * 68f));

        stack.push();
        stack.translate(x, y, z);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - bodyYaw));
        stack.translate(0f, pose.preTranslateY, pose.preTranslateZ);
        if (pose.pitchRotation != 0f) stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.pitchRotation));
        stack.translate(0f, pose.anchorY, pose.anchorZ);
        stack.scale(wingScale, wingScale, wingScale);

        renderWingSide(stack, -1f, open, baseColor, coreColor, glowColor, player, tickDelta, pose);
        renderWingSide(stack, 1f, open, baseColor, coreColor, glowColor, player, tickDelta, pose);
        stack.pop();
    }

    private void renderWingSide(MatrixStack stack, float side, float open,
                                int baseColor, int coreColor, int glowColor,
                                PlayerEntity player, float tickDelta, WingPose pose) {
        stack.push();
        stack.translate(side * pose.sideOffset, pose.sideYOffset, pose.sideZOffset);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * open));
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * pose.sideRoll));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.sidePitch));

        boolean textured = !mode.is("Vanilla");
        bindFillShader(player, tickDelta);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        drawWingMembranes(stack, side, 1.055f, glowColor, glowColor, textured);

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        drawWingMembranes(stack, side, 1.0f, baseColor, coreColor, textured);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        drawWingBones(stack, side, setAlpha(baseColor, Math.round(opacity.getFloatValue() * 42f)), 1.65f);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        drawWingBones(stack, side, setAlpha(coreColor, Math.round(opacity.getFloatValue() * 220f)), 1.0f);
        stack.pop();
    }

    private void bindFillShader(PlayerEntity player, float tickDelta) {
        float speed = shaderSpeed.getFloatValue();
        float time = (player.age + tickDelta) * 0.05f * speed;
        if (mode.is("Animated Fill")) {
            Chams.bindAnimatedFill(time, speed, shaderScale.getFloatValue(), 1.15f);
        } else if (mode.is("Wave")) {
            Chams.bindWave(time);
        } else {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        }
    }

    private void drawWingMembranes(MatrixStack stack, float side, float scale,
                                   int membraneColor, int coreColor, boolean textured) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.TRIANGLES,
                textured ? VertexFormats.POSITION_TEXTURE_COLOR : VertexFormats.POSITION_COLOR);

        for (int panelIndex = 0; panelIndex < PANELS.length; panelIndex++) {
            int[] panel = PANELS[panelIndex];
            float shade = (panelIndex & 1) == 0 ? 1.0f : 0.84f;
            emitMembraneTriangle(buffer, matrix, side, scale, panel, membraneColor, coreColor,
                    shade, MEMBRANE_HALF_THICKNESS, textured, false);
            emitMembraneTriangle(buffer, matrix, side, scale, panel, membraneColor, coreColor,
                    shade * 0.76f, -MEMBRANE_HALF_THICKNESS, textured, true);
        }

        int edgeColor = multiplyRgb(membraneColor, 0.62f);
        for (int i = 0; i < MEMBRANE_EDGE.length; i++) {
            WingPoint a = POINTS[MEMBRANE_EDGE[i]];
            WingPoint b = POINTS[MEMBRANE_EDGE[(i + 1) % MEMBRANE_EDGE.length]];
            emitMembraneEdge(buffer, matrix, side, scale, a, b, edgeColor, textured);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void emitMembraneTriangle(BufferBuilder buffer, Matrix4f matrix, float side, float scale,
                                      int[] panel, int membraneColor, int coreColor, float shade,
                                      float zOffset, boolean textured, boolean reverse) {
        for (int i = 0; i < 3; i++) {
            int pointIndex = panel[reverse ? 2 - i : i];
            WingPoint point = POINTS[pointIndex];
            int source = isHub(pointIndex) ? coreColor : membraneColor;
            int shaded = multiplyRgb(source, shade);
            shaded = setAlpha(shaded, Math.round(alpha(shaded) * point.alphaMul));
            vertex(buffer, matrix, side * point.x * scale, point.y * scale,
                    (point.z + zOffset) * scale, point.u, point.v, shaded, textured);
        }
    }

    private void emitMembraneEdge(BufferBuilder buffer, Matrix4f matrix, float side, float scale,
                                  WingPoint a, WingPoint b, int color, boolean textured) {
        int edgeAlpha = Math.round(alpha(color) * Math.min(a.alphaMul, b.alphaMul));
        int shaded = setAlpha(color, edgeAlpha);
        float ax = side * a.x * scale;
        float ay = a.y * scale;
        float azFront = (a.z + MEMBRANE_HALF_THICKNESS) * scale;
        float azBack = (a.z - MEMBRANE_HALF_THICKNESS) * scale;
        float bx = side * b.x * scale;
        float by = b.y * scale;
        float bzFront = (b.z + MEMBRANE_HALF_THICKNESS) * scale;
        float bzBack = (b.z - MEMBRANE_HALF_THICKNESS) * scale;

        vertex(buffer, matrix, ax, ay, azFront, a.u, a.v, shaded, textured);
        vertex(buffer, matrix, bx, by, bzFront, b.u, b.v, shaded, textured);
        vertex(buffer, matrix, bx, by, bzBack, b.u, b.v, shaded, textured);
        vertex(buffer, matrix, ax, ay, azFront, a.u, a.v, shaded, textured);
        vertex(buffer, matrix, bx, by, bzBack, b.u, b.v, shaded, textured);
        vertex(buffer, matrix, ax, ay, azBack, a.u, a.v, shaded, textured);
    }

    private void drawWingBones(MatrixStack stack, float side, int color, float radiusScale) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (WingBone bone : BONES) {
            emitBone(buffer, matrix, side, bone, color, radiusScale);
        }
        for (WingJoint joint : JOINTS) {
            emitJoint(buffer, matrix, side, joint, color, radiusScale);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void emitBone(BufferBuilder buffer, Matrix4f matrix, float side, WingBone bone,
                          int color, float radiusScale) {
        WingPoint a = POINTS[bone.from];
        WingPoint b = POINTS[bone.to];
        float ax = side * a.x, ay = a.y, az = a.z;
        float bx = side * b.x, by = b.y, bz = b.z;
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.0001f) return;
        dx /= length;
        dy /= length;
        dz /= length;

        // The first cross-section axis is perpendicular to both the bone and camera-depth axis.
        float ux = dy, uy = -dx, uz = 0f;
        float uLength = (float) Math.sqrt(ux * ux + uy * uy);
        if (uLength < 0.0001f) {
            ux = 1f;
            uy = 0f;
            uz = 0f;
        } else {
            ux /= uLength;
            uy /= uLength;
        }
        float vx = dy * uz - dz * uy;
        float vy = dz * ux - dx * uz;
        float vz = dx * uy - dy * ux;

        final int sides = 8;
        for (int i = 0; i < sides; i++) {
            float angle0 = (float) (Math.PI * 2.0 * i / sides);
            float angle1 = (float) (Math.PI * 2.0 * (i + 1) / sides);
            float c0 = (float) Math.cos(angle0), s0 = (float) Math.sin(angle0);
            float c1 = (float) Math.cos(angle1), s1 = (float) Math.sin(angle1);
            float ar = bone.fromRadius * radiusScale;
            float br = bone.toRadius * radiusScale;

            float a0x = ax + (ux * c0 + vx * s0) * ar;
            float a0y = ay + (uy * c0 + vy * s0) * ar;
            float a0z = az + (uz * c0 + vz * s0) * ar;
            float a1x = ax + (ux * c1 + vx * s1) * ar;
            float a1y = ay + (uy * c1 + vy * s1) * ar;
            float a1z = az + (uz * c1 + vz * s1) * ar;
            float b0x = bx + (ux * c0 + vx * s0) * br;
            float b0y = by + (uy * c0 + vy * s0) * br;
            float b0z = bz + (uz * c0 + vz * s0) * br;
            float b1x = bx + (ux * c1 + vx * s1) * br;
            float b1y = by + (uy * c1 + vy * s1) * br;
            float b1z = bz + (uz * c1 + vz * s1) * br;

            int faceColor = multiplyRgb(color, 0.72f + 0.28f * Math.max(0f, c0));
            vertex(buffer, matrix, a0x, a0y, a0z, faceColor);
            vertex(buffer, matrix, b0x, b0y, b0z, faceColor);
            vertex(buffer, matrix, b1x, b1y, b1z, faceColor);
            vertex(buffer, matrix, a0x, a0y, a0z, faceColor);
            vertex(buffer, matrix, b1x, b1y, b1z, faceColor);
            vertex(buffer, matrix, a1x, a1y, a1z, faceColor);

            int capColor = multiplyRgb(color, 0.68f);
            vertex(buffer, matrix, ax, ay, az, capColor);
            vertex(buffer, matrix, a1x, a1y, a1z, capColor);
            vertex(buffer, matrix, a0x, a0y, a0z, capColor);
            vertex(buffer, matrix, bx, by, bz, capColor);
            vertex(buffer, matrix, b0x, b0y, b0z, capColor);
            vertex(buffer, matrix, b1x, b1y, b1z, capColor);
        }
    }

    private void emitJoint(BufferBuilder buffer, Matrix4f matrix, float side, WingJoint joint,
                           int color, float radiusScale) {
        WingPoint point = POINTS[joint.point];
        float x = side * point.x, y = point.y, z = point.z;
        float r = joint.radius * radiusScale;
        float[][] vertices = {
                {x + r, y, z}, {x - r, y, z}, {x, y + r, z},
                {x, y - r, z}, {x, y, z + r}, {x, y, z - r}
        };
        int[][] faces = {
                {2, 0, 4}, {2, 4, 1}, {2, 1, 5}, {2, 5, 0},
                {3, 4, 0}, {3, 1, 4}, {3, 5, 1}, {3, 0, 5}
        };
        for (int i = 0; i < faces.length; i++) {
            int shaded = multiplyRgb(color, i < 4 ? 1.0f : 0.72f);
            for (int index : faces[i]) {
                float[] coords = vertices[index];
                vertex(buffer, matrix, coords[0], coords[1], coords[2], shaded);
            }
        }
    }

    private static boolean isHub(int pointIndex) {
        return pointIndex == 0 || pointIndex == 1 || pointIndex == 2;
    }

    private void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z,
                        float u, float v, int color, boolean textured) {
        if (textured) {
            buffer.vertex(matrix, x, y, z).texture(u, v).color(red(color), green(color), blue(color), alpha(color) / 255f);
        } else {
            buffer.vertex(matrix, x, y, z).color(red(color), green(color), blue(color), alpha(color) / 255f);
        }
    }

    private void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int color) {
        buffer.vertex(matrix, x, y, z).color(red(color), green(color), blue(color), alpha(color) / 255f);
    }

    private float resolveBodyYaw(PlayerEntity player, float tickDelta) {
        float target = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        if (player != mc.player) return target;
        if (!selfBodyYawInitialized || player.age < 2) {
            selfBodyYaw = target;
            selfBodyYawInitialized = true;
            return selfBodyYaw;
        }
        float delta = MathHelper.clamp(MathHelper.wrapDegrees(target - selfBodyYaw), -14f, 14f);
        selfBodyYaw += delta;
        return selfBodyYaw;
    }

    private WingPose resolvePose(PlayerEntity player, float tickDelta) {
        float pitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
        if (player.isGliding()) {
            float flightTicks = player.getGlidingTicks() + tickDelta;
            float flightProgress = MathHelper.clamp(flightTicks * flightTicks / 100f, 0f, 1f);
            return new WingPose(0.33f, 0.43f, 0.78f, 0.16f,
                    flightProgress * (-90f - pitch), 27f, 1.18f, 0.94f,
                    2.5f, 3.2f, 0.055f, 0f, 0.08f, -4f, -8f, 0.18f);
        }
        if (player.isTouchingWater()) return null;
        if (player.isSneaking()) {
            return new WingPose(0f, 0f, 0.96f, 0.20f,
                    18f, 20f, 0.92f, 0.98f,
                    3f, 5.5f, 0.06f, 0f, 0.03f, -10f, -5f, 0.13f);
        }
        return new WingPose(0f, 0f, 1.36f, 0.19f,
                0f, 22f, 1f, 1f,
                3.5f, 6.5f, 0.06f, 0f, 0.03f, -12f, -5f, 0.13f);
    }

    private static int setAlpha(int color, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static int multiplyRgb(int color, float factor) {
        int r = MathHelper.clamp(Math.round(((color >> 16) & 0xFF) * factor), 0, 255);
        int g = MathHelper.clamp(Math.round(((color >> 8) & 0xFF) * factor), 0, 255);
        int b = MathHelper.clamp(Math.round((color & 0xFF) * factor), 0, 255);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static int alpha(int color) { return (color >>> 24) & 0xFF; }
    private static float red(int color) { return ((color >>> 16) & 0xFF) / 255f; }
    private static float green(int color) { return ((color >>> 8) & 0xFF) / 255f; }
    private static float blue(int color) { return (color & 0xFF) / 255f; }

    @Override
    public void onDisable() {
        selfBodyYawInitialized = false;
        super.onDisable();
    }

    private record WingPoint(float x, float y, float z, float u, float v, float alphaMul) {}
    private record WingBone(int from, int to, float fromRadius, float toRadius) {}
    private record WingJoint(int point, float radius) {}

    private record WingPose(float preTranslateY, float preTranslateZ,
                            float anchorY, float anchorZ, float pitchRotation,
                            float baseSpread, float openMultiplier, float scaleMultiplier,
                            float motionSpreadBoost, float flapAmplitude,
                            float sideOffset, float sideYOffset, float sideZOffset,
                            float sideRoll, float sidePitch, float flapSpeed) {}

    private static final class GlState {
        private final boolean blend;
        private final boolean cull;
        private final boolean depthTest;
        private final boolean depthMask;
        private final int srcRgb;
        private final int dstRgb;
        private final int srcAlpha;
        private final int dstAlpha;
        private final float lineWidth;
        private final float[] shaderColor;
        private final ShaderProgram shader;

        private GlState(boolean blend, boolean cull, boolean depthTest, boolean depthMask,
                        int srcRgb, int dstRgb, int srcAlpha, int dstAlpha,
                        float lineWidth, float[] shaderColor, ShaderProgram shader) {
            this.blend = blend;
            this.cull = cull;
            this.depthTest = depthTest;
            this.depthMask = depthMask;
            this.srcRgb = srcRgb;
            this.dstRgb = dstRgb;
            this.srcAlpha = srcAlpha;
            this.dstAlpha = dstAlpha;
            this.lineWidth = lineWidth;
            this.shaderColor = shaderColor;
            this.shader = shader;
        }

        static GlState capture() {
            return new GlState(
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    RenderSystem.getShaderLineWidth(),
                    RenderSystem.getShaderColor().clone(),
                    RenderSystem.getShader());
        }

        void restore() {
            RenderSystem.depthMask(depthMask);
            if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            RenderSystem.lineWidth(lineWidth);
            RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
            if (shader != null) RenderSystem.setShader(shader); else RenderSystem.clearShader();
        }
    }
}
