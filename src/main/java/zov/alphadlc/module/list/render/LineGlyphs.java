package zov.alphadlc.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ColorSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.render.providers.ColorProvider;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleInformation(moduleName = "Line Glyphs", moduleDesc = "Пунктирные глифы-линии со светящимися точками", moduleCategory = ModuleCategory.RENDER)
public class LineGlyphs extends Module {

    private static final int SPAWN_ATTEMPTS_PER_TICK = 4;

    private final SliderSetting count = new SliderSetting("Количество", 70, 10, 200, 5);
    private final BooleanSetting slow = new BooleanSetting("Медленно", true);
    private final SliderSetting thickness = new SliderSetting("Толщина", 2.0f, 0.5f, 5.0f, 0.1f);
    private final SliderSetting dashLength = new SliderSetting("Длина дэша", 0.12f, 0.03f, 1.0f, 0.01f);
    private final SliderSetting dashGap = new SliderSetting("Промежуток", 0.08f, 0.02f, 1.0f, 0.01f);
    private final SliderSetting opacity = new SliderSetting("Прозрачность", 1.0f, 0.1f, 1.0f, 0.05f);
    private final SliderSetting spawnRadius = new SliderSetting("Радиус спавна", 16, 4, 48, 1);

    private final BooleanSetting glowDots = new BooleanSetting("Глоу-точки", true);
    private final SliderSetting glowSize = new SliderSetting("Размер точек", 0.12f, 0.03f, 0.4f, 0.01f)
            .setVisible(glowDots::getValue);

    private final ModeSetting colorMode = new ModeSetting("Режим цвета", "Клиентский",
            "Клиентский", "Радужный", "Кастом");
    private final ColorSetting customColor = new ColorSetting("Цвет", 0xFF7657FF)
            .setVisible(() -> colorMode.is("Кастом"));

    private final List<Glyph> glyphs = new ArrayList<>();
    private final Random random = new Random();

    private boolean registered = false;
    private final WorldRenderEvents.Last listener = context -> {
        if (isEnabled()) onRender(context.matrixStack(), context.camera());
    };

    @Override
    public void onEnable() {
        super.onEnable();
        glyphs.clear();
        if (!registered) {
            WorldRenderEvents.LAST.register(listener);
            registered = true;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        glyphs.clear();
    }

    @Subscribe
    private void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        glyphs.removeIf(Glyph::isDead);
        int cap = count.getIntValue();
        int attempts = SPAWN_ATTEMPTS_PER_TICK;
        while (attempts-- > 0 && glyphs.size() < cap) {
            glyphs.add(new Glyph(spawnPos(), 7 + random.nextInt(6)));
        }
        for (Glyph glyph : glyphs) glyph.tick();
    }

    private void onRender(MatrixStack matrix, Camera camera) {
        if (glyphs.isEmpty()) return;

        Vec3d cam = camera.getPos();
        Matrix4f base = matrix.peek().getPositionMatrix();

        float baseOpacity = opacity.getFloatValue();
        float halfWidth = thickness.getFloatValue() * 0.02f;
        float dash = dashLength.getFloatValue();
        float gap = dashGap.getFloatValue();
        float cullRadius = spawnRadius.getFloatValue() * 1.75f;
        double cullDistSq = cullRadius * cullRadius;
        boolean dots = glowDots.getValue();
        float dotSize = glowSize.getFloatValue();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        int glyphIndex = 0;
        for (Glyph glyph : glyphs) {
            if (glyph.nodes.size() < 2) {
                glyphIndex++;
                continue;
            }
            int[] first = glyph.nodes.get(0);
            double ddx = first[0] - cam.x;
            double ddy = first[1] - cam.y;
            double ddz = first[2] - cam.z;
            if (ddx * ddx + ddy * ddy + ddz * ddz > cullDistSq) {
                glyphIndex++;
                continue;
            }

            float alphaBase = glyph.alpha() * baseOpacity;
            if (alphaBase <= 0.01f) {
                glyphIndex++;
                continue;
            }

            double[] a = new double[3];
            double[] b = new double[3];
            int size = glyph.nodes.size();
            for (int i = 1; i < size; i++) {
                glyph.getNodeCoord(i - 1, a);
                glyph.getNodeCoord(i, b);
                float segAlpha = alphaBase * (0.35f + (float) i / size * 0.65f);
                int color = ColorProvider.setAlpha(baseColor(glyphIndex + i * 40), (int) (MathHelper.clamp(segAlpha, 0f, 1f) * 255));
                appendDashes(buffer, base,
                        a[0] - cam.x, a[1] - cam.y, a[2] - cam.z,
                        b[0] - cam.x, b[1] - cam.y, b[2] - cam.z,
                        halfWidth, dash, gap, color);
            }

            glyphIndex++;
        }

        // Рисуем только если есть что рисовать
        try {
            BuiltBuffer builtBuffer = buffer.end();
            if (builtBuffer != null) {
                BufferRenderer.drawWithGlobalProgram(builtBuffer);
            }
        } catch (IllegalStateException ignored) {
            // Buffer был пустой, ничего не делаем
        }

        if (dots) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, net.minecraft.util.Identifier.of("mre", "images/glow.png"));
            
            BufferBuilder dotBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            
            glyphIndex = 0;
            for (Glyph glyph : glyphs) {
                if (glyph.nodes.isEmpty()) {
                    glyphIndex++;
                    continue;
                }
                
                float alphaBase = glyph.alpha() * baseOpacity;
                if (alphaBase <= 0.01f) {
                    glyphIndex++;
                    continue;
                }
                
                double[] a = new double[3];
                int size = glyph.nodes.size();
                for (int i = 0; i < size; i++) {
                    glyph.getNodeCoord(i, a);
                    int color = baseColor(glyphIndex + i * 40);
                    appendGlowDot(dotBuffer, matrix, camera,
                            a[0] - cam.x, a[1] - cam.y, a[2] - cam.z,
                            dotSize, color, alphaBase);
                }
                glyphIndex++;
            }
            
            try {
                BuiltBuffer dotBuiltBuffer = dotBuffer.end();
                if (dotBuiltBuffer != null) {
                    BufferRenderer.drawWithGlobalProgram(dotBuiltBuffer);
                }
            } catch (IllegalStateException ignored) {
                // dotBuffer был пустой, ничего не делаем
            }
            RenderSystem.setShaderTexture(0, 0);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    // Пунктирный сегмент: короткие штрихи (два перпендикулярных «ребра») с промежутками.
    private void appendDashes(BufferBuilder buffer, Matrix4f matrix,
                              double x1, double y1, double z1,
                              double x2, double y2, double z2,
                              float halfWidth, float dash, float gap, int color) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6) return;
        dx /= len; dy /= len; dz /= len;

        double period = Math.max(0.01, dash + gap);
        for (double t = 0; t < len; t += period) {
            double t2 = Math.min(t + dash, len);
            double ax = x1 + dx * t, ay = y1 + dy * t, az = z1 + dz * t;
            double bx = x1 + dx * t2, by = y1 + dy * t2, bz = z1 + dz * t2;
            appendStick(buffer, matrix, ax, ay, az, bx, by, bz, dx, dy, dz, halfWidth, color);
        }
    }

    private void appendStick(BufferBuilder buffer, Matrix4f matrix,
                             double ax, double ay, double az,
                             double bx, double by, double bz,
                             double dx, double dy, double dz,
                             float halfWidth, int color) {
        // Координаты уже относительны камеры, значит камера — в начале координат.
        // Строим одну ленту, повёрнутую к камере: offset = dir × viewDir.
        // Это убирает алиасинг тонких фикс-ориентированных квадов ("пиксельность").
        double mx = (ax + bx) * 0.5, my = (ay + by) * 0.5, mz = (az + bz) * 0.5;
        double viewLen = Math.sqrt(mx * mx + my * my + mz * mz);
        double vx, vy, vz;
        if (viewLen < 1e-6) { vx = 0; vy = 0; vz = 1; }
        else { vx = mx / viewLen; vy = my / viewLen; vz = mz / viewLen; }

        double ox = dy * vz - dz * vy;
        double oy = dz * vx - dx * vz;
        double oz = dx * vy - dy * vx;
        double olen = Math.sqrt(ox * ox + oy * oy + oz * oz);
        if (olen < 1e-6) {
            // Направление совпало со взглядом — берём горизонтальный перпендикуляр.
            ox = -dz; oy = 0; oz = dx;
            olen = Math.sqrt(ox * ox + oz * oz);
            if (olen < 1e-6) { ox = 1; oy = 0; oz = 0; olen = 1; }
        }
        ox = ox / olen * halfWidth;
        oy = oy / olen * halfWidth;
        oz = oz / olen * halfWidth;

        quad(buffer, matrix, ax, ay, az, bx, by, bz, ox, oy, oz, color);
    }

    private void quad(BufferBuilder buffer, Matrix4f m,
                      double ax, double ay, double az,
                      double bx, double by, double bz,
                      double ox, double oy, double oz, int color) {
        buffer.vertex(m, (float) (ax + ox), (float) (ay + oy), (float) (az + oz)).color(color);
        buffer.vertex(m, (float) (ax - ox), (float) (ay - oy), (float) (az - oz)).color(color);
        buffer.vertex(m, (float) (bx - ox), (float) (by - oy), (float) (bz - oz)).color(color);
        buffer.vertex(m, (float) (bx + ox), (float) (by + oy), (float) (bz + oz)).color(color);
    }

    // Аддитивная камерная точка-глоу с glow.png текстурой
    private void appendGlowDot(BufferBuilder buffer, MatrixStack stack, Camera camera,
                               double x, double y, double z, float size, int color, float alpha) {
        stack.push();
        stack.translate(x, y, z);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        Matrix4f m = stack.peek().getPositionMatrix();

        int outer = ColorProvider.setAlpha(color, (int) (MathHelper.clamp(alpha * 0.3f, 0f, 1f) * 255));
        float outerSize = size * 3.0f;
        billboardQuadTextured(buffer, m, outerSize, outer);
        
        int inner = ColorProvider.setAlpha(color, (int) (MathHelper.clamp(alpha, 0f, 1f) * 255));
        billboardQuadTextured(buffer, m, size, inner);

        stack.pop();
    }

    private void billboardQuadTextured(BufferBuilder buffer, Matrix4f m, float size, int color) {
        float half = size / 2f;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        
        buffer.vertex(m, -half,  half, 0).texture(0f, 0f).color(r, g, b, a);
        buffer.vertex(m,  half,  half, 0).texture(1f, 0f).color(r, g, b, a);
        buffer.vertex(m,  half, -half, 0).texture(1f, 1f).color(r, g, b, a);
        buffer.vertex(m, -half, -half, 0).texture(0f, 1f).color(r, g, b, a);
    }

    private int baseColor(int index) {
        return switch (colorMode.getValue()) {
            case "Радужный" -> {
                float hue = ((System.currentTimeMillis() / 20f) + index * 8f) % 360f / 360f;
                yield 0xFF000000 | (Color.HSBtoRGB(hue, 0.7f, 1.0f) & 0x00FFFFFF);
            }
            case "Кастом" -> customColor.getValue();
            default -> ColorProvider.getColorClient();
        };
    }

    private int[] spawnPos() {
        double fov = mc.options.getFov().getValue();
        double yaw = Math.toRadians(mc.player.getYaw() + (random.nextDouble() - 0.5) * fov * 1.5);
        float radius = spawnRadius.getFloatValue();
        float minDistance = Math.max(4f, radius * 0.25f);
        double distance = minDistance + random.nextDouble() * (radius - minDistance);
        int dx = (int) (-(Math.sin(yaw) * distance));
        int dy = random.nextInt(12);
        int dz = (int) (Math.cos(yaw) * distance);
        Vec3d eye = mc.player.getEyePos();
        return new int[]{(int) eye.x + dx, (int) eye.y + dy, (int) eye.z + dz};
    }

    private int[] randomDirection() {
        return new int[]{random.nextInt(4) * 90, (random.nextInt(3) - 1) * 90};
    }

    private int[] nextDirection(int[] previous) {
        int nextB = switch (previous[1]) {
            case 0 -> random.nextBoolean() ? 90 : -90;
            case 90 -> random.nextBoolean() ? 0 : -90;
            default -> random.nextBoolean() ? 0 : 90;
        };
        int nextA = (previous[0] + 90 * (1 + random.nextInt(3))) % 360;
        if (nextA == previous[0]) nextA = (nextA + 90) % 360;
        return new int[]{nextA, nextB};
    }

    private int[] step(int[] position, int[] direction, int radius) {
        double yaw = Math.toRadians(direction[0]);
        double pitch = Math.toRadians(direction[1]);
        double horizontalRadius = radius;
        int deltaY = (int) (Math.sin(pitch) * horizontalRadius);
        if (pitch != 0) horizontalRadius = 0;
        int deltaX = (int) (-(Math.sin(yaw) * horizontalRadius));
        int deltaZ = (int) (Math.cos(yaw) * horizontalRadius);
        return new int[]{position[0] + deltaX, position[1] + deltaY, position[2] + deltaZ};
    }

    private class Glyph {
        final List<int[]> nodes = new ArrayList<>();
        int[] direction;
        int stepsLeft;
        int ticksLeft;
        int lastSet;
        boolean dying;
        int age;
        int dyingAge;

        Glyph(int[] spawn, int steps) {
            nodes.add(spawn);
            direction = randomDirection();
            stepsLeft = steps;
        }

        void tick() {
            age++;
            if (stepsLeft == 0) {
                if (!dying) {
                    dying = true;
                    dyingAge = age;
                }
                return;
            }

            if (ticksLeft > 0) {
                ticksLeft -= slow.getValue() ? 1 : 2;
                if (ticksLeft < 0) ticksLeft = 0;
                return;
            }

            direction = nextDirection(direction);
            lastSet = ticksLeft = random.nextInt(3);
            nodes.add(step(nodes.get(nodes.size() - 1), direction, Math.max(1, ticksLeft)));
            stepsLeft--;
        }

        float alpha() {
            float in = Math.min(1f, age / 6f);
            if (!dying) return in;
            float out = Math.max(0f, 1f - (age - dyingAge) / 8f);
            return Math.min(in, out);
        }

        boolean isDead() {
            return dying && (age - dyingAge) > 8;
        }

        void getNodeCoord(int index, double[] out) {
            int[] node = nodes.get(index);
            double x = node[0];
            double y = node[1];
            double z = node[2];

            if (index == nodes.size() - 1 && nodes.size() >= 2) {
                int[] previous = nodes.get(index - 1);
                float advance = lastSet > 0
                        ? Math.max(0f, Math.min(1f, 1f - (float) ticksLeft / lastSet))
                        : 1f;
                x = MathHelper.lerp(advance, previous[0], x);
                y = MathHelper.lerp(advance, previous[1], y);
                z = MathHelper.lerp(advance, previous[2], z);
            }

            out[0] = x;
            out[1] = y;
            out[2] = z;
        }
    }
}
