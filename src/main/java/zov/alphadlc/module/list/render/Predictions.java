package zov.alphadlc.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import zov.alphadlc.event.list.EventHUD;
import zov.alphadlc.event.list.EventWorldRender;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.util.player.combat.RaytraceUtil;
import zov.alphadlc.util.player.other.WorldUtils;
import zov.alphadlc.util.render.math.ProjectionUtil;
import zov.alphadlc.util.render.msdf.Fonts;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;


@ModuleInformation(moduleName = "Predictions", moduleDesc = "Показывает траекторию полета предметов", moduleCategory = ModuleCategory.RENDER)
public class Predictions extends Module {

    private static final Identifier GLOW = Identifier.of("mre", "images/glow.png");

    private final ModeSetting renderMode = new ModeSetting("Режим рендера", "Default", "Default", "Glow");
    private final BooleanSetting walls = new BooleanSetting("Через стены", true);
    

    private final BooleanSetting showArrows = new BooleanSetting("Стрелы", true);
    private final BooleanSetting showTridents = new BooleanSetting("Трезубцы", true);
    private final BooleanSetting showThrownItems = new BooleanSetting("ЭндерЖемчуг", true);
    private final BooleanSetting showDroppedItems = new BooleanSetting("Предметы", false);

    private final List<Point> points = new ArrayList<>();

    @Subscribe
    public void onDraw(EventHUD e) {
        for (Point point : points) {
            Vector2f vec2f = ProjectionUtil.project(point.pos);
            int ticks = point.ticks;

            double time = ticks * 50 / 1000.0;
            String text = String.format("%.1f", time) + " сек";
            float textWidth = Fonts.SFREGULAR.get().getWidth(text, 7);

            float centerX = vec2f.getX();
            float centerY = vec2f.getY();

            float totalWidth = textWidth;
            float totalHeight = 5.75F;

            float rectX = centerX - totalWidth / 2f;
            float rectY = centerY - totalHeight / 2f;

            float textX = rectX;
            float textY = rectY + 5;

            DrawUtil.drawRound(textX - 7, textY - 2, totalWidth + 14.75f, totalHeight + 5, 0, ColorProvider.rgba(0, 0, 0, 120));
            e.getDrawContext().getMatrices().push();
            e.getDrawContext().getMatrices().translate(textX - 5, textY - 0.75f, 0);
            e.getDrawContext().getMatrices().scale(0.5f, 0.5f, 1);
            e.getDrawContext().drawItem(point.stack(), 0, 0);
            e.getDrawContext().getMatrices().scale(1, 1, 1);
            e.getDrawContext().getMatrices().translate(-(textX - 5), -(textY - 0.75f), 0);
            e.getDrawContext().getMatrices().pop();
            DrawUtil.drawText(
                    Fonts.SFREGULAR.get(),
                    text.replace(",", "."),
                    textX + 4.5f,
                    textY - 0.5f,
                    ColorProvider.rgba(255, 255, 255, 255), 6.75f
            );
        }
    }

    @Subscribe
    public void onWorldRender(EventWorldRender e) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        
        points.clear();
        float tickDelta = e.getTickDelta();
        long now = System.currentTimeMillis();
        MatrixStack matrices = e.getMatrixStack();
        Camera camera = mc.gameRenderer.getCamera();
        boolean depth = !walls.getValue();

        if (renderMode.is("Glow")) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.setShaderTexture(0, GLOW);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (Entity entity : getProjectiles()) {
                SimulatedPath simulated = simulatePath(entity, tickDelta);
                if (simulated.rawPoints.size() < 2) continue;

                List<Vec3d> smoothPath = smoothPath(simulated.rawPoints);
                if (smoothPath.size() < 2) continue;

                int startColor = ColorProvider.getColorClient();
                int endColor = ColorProvider.getThemeColorTwo();

                renderGlowPath(buffer, matrices, camera, smoothPath, startColor, endColor, now);

                if (simulated.hitPos != null) {
                    renderImpactGlow(buffer, matrices, camera, simulated.hitPos, endColor, now);
                    breakingBad(entity, simulated.hitPos, simulated.hitTicks);
                }
            }

            try {
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            } catch (Exception ex) {

            }

            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        } else {
            
            getProjectiles().forEach(entity -> {
                Vec3d motion = entity.getVelocity();
                Vec3d pos = entity.getPos();
                Vec3d prevPos;
                int ticks = 0;

                for (int i = 0; i < 300; i++) {
                    prevPos = pos;
                    pos = pos.add(motion);
                    motion = calculateMotion(entity, prevPos, motion);

                    HitResult result = RaytraceUtil.raycast(prevPos, pos, RaycastContext.ShapeType.COLLIDER, entity);
                    if (!result.getType().equals(HitResult.Type.MISS)) {
                        pos = result.getPos();
                    }

                    float alpha = MathHelper.clamp(i / 25.0f, 0, 1) * 255;
                    int baseColor = ColorProvider.setAlpha(ColorProvider.getColorClient(), alpha);

                    DrawUtil.drawLine(prevPos, pos, baseColor, 2, depth);

                    Vec3d finalPrevPos = prevPos, finalPos = pos;
                    boolean inEntity = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                            .filter(ent -> ent instanceof LivingEntity living && living != mc.player && living.isAlive())
                            .anyMatch(ent -> ent.getBoundingBox().expand(0.25).intersects(finalPrevPos, finalPos));
                    if (result.getType().equals(HitResult.Type.BLOCK) || pos.y < -128 || inEntity || result.getType().equals(HitResult.Type.ENTITY)) {
                        breakingBad(entity, pos, ticks);
                        break;
                    }
                    ticks++;
                }
            });
        }
    }

    private SimulatedPath simulatePath(Entity entity, float tickDelta) {
        List<Vec3d> path = new ArrayList<>();
        Vec3d renderPos = getInterpolatedPos(entity, tickDelta);
        Vec3d pos = entity.getPos();
        Vec3d motion = entity.getVelocity();
        Vec3d hitPos = null;
        int hitTicks = 0;

        addIfFar(path, renderPos);
        if (renderPos.squaredDistanceTo(pos) > 1.0E-5) {
            addIfFar(path, renderPos.lerp(pos, 0.35));
            addIfFar(path, renderPos.lerp(pos, 0.7));
        }
        addIfFar(path, pos);

        for (int i = 0; i < 220; i++) {
            Vec3d prevPos = pos;
            pos = pos.add(motion);
            motion = calculateMotion(entity, prevPos, motion);

            HitResult result = RaytraceUtil.raycast(prevPos, pos, RaycastContext.ShapeType.COLLIDER, entity);
            if (result.getType() != HitResult.Type.MISS) {
                pos = result.getPos();
            }

            addIfFar(path, pos);

            boolean hitEntity = hitsEntity(entity, prevPos, pos);
            if (result.getType() == HitResult.Type.BLOCK || result.getType() == HitResult.Type.ENTITY || hitEntity || pos.y < -128) {
                hitPos = pos;
                hitTicks = i + 1;
                break;
            }
        }

        return new SimulatedPath(path, hitPos, hitTicks);
    }

    private Vec3d getInterpolatedPos(Entity entity, float tickDelta) {
        return new Vec3d(
            MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
            MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
            MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ())
        );
    }

    private void addIfFar(List<Vec3d> list, Vec3d point) {
        if (list.isEmpty() || list.get(list.size() - 1).squaredDistanceTo(point) > 1.0E-6) {
            list.add(point);
        }
    }

    private List<Vec3d> smoothPath(List<Vec3d> raw) {
        if (raw.size() < 2) {
            return raw;
        }
        List<Vec3d> out = new ArrayList<>();
        out.add(raw.get(0));

        for (int i = 0; i < raw.size() - 1; i++) {
            Vec3d p0 = raw.get(Math.max(0, i - 1));
            Vec3d p1 = raw.get(i);
            Vec3d p2 = raw.get(i + 1);
            Vec3d p3 = raw.get(Math.min(raw.size() - 1, i + 2));

            double dist = p1.distanceTo(p2);
            int steps = MathHelper.clamp((int) Math.ceil(dist / 0.08), 3, 8);

            for (int j = 1; j <= steps; j++) {
                float t = (float) j / (float) steps;
                out.add(catmullRom(p0, p1, p2, p3, t));
            }
        }

        return out;
    }

    private Vec3d catmullRom(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t) {
        double t2 = t * t;
        double t3 = t2 * t;

        double x = 0.5 * (2.0 * p1.x + (-p0.x + p2.x) * t +
                (2.0 * p0.x - 5.0 * p1.x + 4.0 * p2.x - p3.x) * t2 +
                (-p0.x + 3.0 * p1.x - 3.0 * p2.x + p3.x) * t3);

        double y = 0.5 * (2.0 * p1.y + (-p0.y + p2.y) * t +
                (2.0 * p0.y - 5.0 * p1.y + 4.0 * p2.y - p3.y) * t2 +
                (-p0.y + 3.0 * p1.y - 3.0 * p2.y + p3.y) * t3);

        double z = 0.5 * (2.0 * p1.z + (-p0.z + p2.z) * t +
                (2.0 * p0.z - 5.0 * p1.z + 4.0 * p2.z - p3.z) * t2 +
                (-p0.z + 3.0 * p1.z - 3.0 * p2.z + p3.z) * t3);

        return new Vec3d(x, y, z);
    }

    private boolean hitsEntity(Entity projectile, Vec3d from, Vec3d to) {
        if (mc.world == null) {
            return false;
        }
        return !mc.world.getOtherEntities(projectile, new net.minecraft.util.math.Box(from, to).expand(0.3),
                ent -> ent instanceof LivingEntity living && living != mc.player && living.isAlive()).isEmpty();
    }

    private void renderGlowPath(BufferBuilder buffer, MatrixStack matrices, Camera camera, List<Vec3d> path, int startColor, int endColor, long now) {
        if (path.size() < 2) {
            return;
        }

        float anim = (float) (now % 100000L) / 1000.0f;
        int lastIndex = path.size() - 1;

        for (int i = 0; i < path.size(); i++) {
            Vec3d pos = path.get(i);
            float progress = (float) i / (float) lastIndex;
            float fade = 1.0f - progress;
            fade = fade * fade * (3.0f - 2.0f * fade);
            float shimmer = 1.0f + 0.025f * MathHelper.sin(anim * 5.0f + progress * 10.0f);
            float size = (0.065f + fade * 0.11f) * shimmer;

            int color = interpolateColor(startColor, endColor, progress);
            int bright = interpolateColor(color, -1, 0.3f);

            drawGlow(buffer, matrices, camera, pos, size * 2.7f, withAlpha(color, (int) (fade * 24.0f)));
            drawGlow(buffer, matrices, camera, pos, size * 1.55f, withAlpha(color, (int) (fade * 72.0f)));
            drawGlow(buffer, matrices, camera, pos, size * 0.78f, withAlpha(bright, (int) (fade * 150.0f)));
        }
    }

    private void renderImpactGlow(BufferBuilder buffer, MatrixStack matrices, Camera camera, Vec3d pos, int color, long now) {
        float pulse = 1.0f + 0.08f * MathHelper.sin((float) (now % 3000L) / 120.0f);
        int bright = interpolateColor(color, -1, 0.45f);

        drawGlow(buffer, matrices, camera, pos, 0.7f * pulse, withAlpha(color, 45));
        drawGlow(buffer, matrices, camera, pos, 0.42f * pulse, withAlpha(color, 115));
        drawGlow(buffer, matrices, camera, pos, 0.22f * pulse, withAlpha(bright, 220));
    }

    private void drawGlow(BufferBuilder buffer, MatrixStack matrices, Camera camera, Vec3d worldPos, float scale, int color) {
        Vec3d camPos = camera.getPos();
        matrices.push();
        matrices.translate(worldPos.x - camPos.x, worldPos.y - camPos.y, worldPos.z - camPos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        Matrix4f mat = matrices.peek().getPositionMatrix();
        buffer.vertex(mat, -scale, scale, 0.0f).texture(0.0f, 1.0f).color(color);
        buffer.vertex(mat, scale, scale, 0.0f).texture(1.0f, 1.0f).color(color);
        buffer.vertex(mat, scale, -scale, 0.0f).texture(1.0f, 0.0f).color(color);
        buffer.vertex(mat, -scale, -scale, 0.0f).texture(0.0f, 0.0f).color(color);

        matrices.pop();
    }

    private int withAlpha(int color, int alpha) {
        alpha = MathHelper.clamp(alpha, 0, 255);
        return alpha << 24 | color & 0xFFFFFF;
    }

    private int interpolateColor(int start, int end, float t) {
        t = MathHelper.clamp(t, 0.0f, 1.0f);
        int sr = start >> 16 & 0xFF;
        int sg = start >> 8 & 0xFF;
        int sb = start & 0xFF;
        int er = end >> 16 & 0xFF;
        int eg = end >> 8 & 0xFF;
        int eb = end & 0xFF;
        int r = (int) ((float) sr + (float) (er - sr) * t);
        int g = (int) ((float) sg + (float) (eg - sg) * t);
        int b = (int) ((float) sb + (float) (eb - sb) * t);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public List<Entity> getProjectiles() {
        return StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(e -> {
                    if (visible(e)) return false;
                    
                    if (e instanceof TridentEntity) {
                        return showTridents.getValue();
                    } else if (e instanceof PersistentProjectileEntity) {
                        return showArrows.getValue();
                    } else if (e instanceof ThrownItemEntity) {
                        return showThrownItems.getValue();
                    } else if (e instanceof ItemEntity) {
                        return showDroppedItems.getValue();
                    }
                    
                    return false;
                })
                .toList();
    }

    public Vec3d calculateMotion(Entity entity, Vec3d prevPos, Vec3d motion) {
        boolean isInWater = Objects.requireNonNull(mc.world).getBlockState(BlockPos.ofFloored(prevPos)).getFluidState().isIn(FluidTags.WATER);

        float multiply = switch (entity) {
            case TridentEntity i -> 0.99F;
            case PersistentProjectileEntity i when isInWater -> 0.6F;
            default -> isInWater ? 0.8F : 0.99F;
        };

        return motion.multiply(multiply).add(0, -entity.getFinalGravity(), 0);
    }

    private void breakingBad(Entity entity, Vec3d pos, int ticks) {
        switch (entity) {
            case ItemEntity item -> points.add(new Point(item.getStack(), pos, ticks));
            case ThrownItemEntity thrown -> points.add(new Point(thrown.getStack(), pos, ticks));
            case PersistentProjectileEntity persistent -> points.add(new Point(persistent.getItemStack(), pos, ticks));
            default -> {}
        }
    }

    private boolean visible(Entity entity) {
        boolean posChange = entity.getX() == entity.prevX && entity.getY() == entity.prevY && entity.getZ() == entity.prevZ;
        boolean itemEntityCheck = entity instanceof ItemEntity && (entity.isOnGround() || WorldUtils.isBoxInBlock(entity.getBoundingBox().expand(2), Blocks.WATER));
        return posChange || itemEntityCheck;
    }

    private record Point(ItemStack stack, Vec3d pos, int ticks) {}

    private static class SimulatedPath {
        private final List<Vec3d> rawPoints;
        private final Vec3d hitPos;
        private final int hitTicks;

        private SimulatedPath(List<Vec3d> rawPoints, Vec3d hitPos, int hitTicks) {
            this.rawPoints = rawPoints;
            this.hitPos = hitPos;
            this.hitTicks = hitTicks;
        }
    }
}

