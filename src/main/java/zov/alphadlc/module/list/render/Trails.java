package zov.alphadlc.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeListSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.friend.FriendRepository;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.effects.MasEffectRenderer;

import java.awt.Color;
import java.util.*;

@ModuleInformation(moduleName = "Trails", moduleDesc = "Оставляет след за сущностями", moduleCategory = ModuleCategory.RENDER)
public class Trails extends Module {

    private static final int PRETTY_HARD_CAP = 1000;

    // Мультивыбор режимов: несколько сразу.
    private final ModeListSetting mode = new ModeListSetting("Режим",
            new BooleanSetting("Entity Trails", true),
            new BooleanSetting("Pearl Trail", false),
            new BooleanSetting("Красивый", false));

    private final SliderSetting duration = new SliderSetting("Длительность", 1000.0f, 500.0f, 2000.0f, 100.0f);
    private final SliderSetting lineWidth = new SliderSetting("Толщина", 2.0f, 0.5f, 5.0f, 0.1f);
    private final SliderSetting opacity = new SliderSetting("Непрозрачность", 100.0f, 0.0f, 255.0f, 5.0f);
    private final BooleanSetting firstPerson = new BooleanSetting("От первого лица", false);

    private final BooleanSetting targetSelf = new BooleanSetting("Себе", true);
    private final BooleanSetting targetFriends = new BooleanSetting("На друзьях", true);
    private final BooleanSetting targetPlayers = new BooleanSetting("На игроках", true);
    private final BooleanSetting targetMobs = new BooleanSetting("На мобах", false);

    // "Красивый" — декоративные палочки за игроком.
    private final SliderSetting prettyCount = new SliderSetting("Кол-во палочек", 2, 1, 10, 1)
            .setVisible(() -> mode.isEnabled("Красивый"));
    private final SliderSetting prettyLifetime = new SliderSetting("Длительность (мс)", 1800, 500, 5000, 100)
            .setVisible(() -> mode.isEnabled("Красивый"));
    private final SliderSetting prettySpread = new SliderSetting("Сближенность", 0.4f, 0.1f, 1.5f, 0.1f)
            .setVisible(() -> mode.isEnabled("Красивый"));
    private final SliderSetting prettyLineLength = new SliderSetting("Длина палочки", 0.3f, 0.1f, 1.0f, 0.05f)
            .setVisible(() -> mode.isEnabled("Красивый"));
    private final BooleanSetting prettyGlow = new BooleanSetting("Свечение палочек", true)
            .setVisible(() -> mode.isEnabled("Красивый"));
    private final BooleanSetting prettyRainbow = new BooleanSetting("Радужный цвет", false)
            .setVisible(() -> mode.isEnabled("Красивый"));

    private final Map<Integer, Deque<Point>> entityPoints = new HashMap<>();
    private final Map<Integer, Vec3d> lastPositions = new HashMap<>();
    private final Map<Integer, Long> lastPointTimes = new HashMap<>();
    private final Map<Integer, EntityType> entityTypes = new HashMap<>();
    private long lastEntityScan = 0L;

    // Фиксированный пул палочек (ring buffer поведение через ArrayDeque + hard cap).
    private final ArrayDeque<PrettyFig> prettyFigs = new ArrayDeque<>();
    private Vec3d lastPrettySpawnPos = null;
    private final Random random = new Random();

    private enum EntityType {
        SELF, FRIEND, PLAYER, MOB
    }

    public Trails() {
        super();
        WorldRenderEvents.LAST.register(this::onRender);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        clearData();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clearData();
    }

    private void clearData() {
        entityPoints.clear();
        lastPositions.clear();
        lastPointTimes.clear();
        entityTypes.clear();
        prettyFigs.clear();
        lastPrettySpawnPos = null;
        lastEntityScan = 0L;
    }

    @Subscribe
    private void onTick(EventTick e) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;
        if (mode.isEnabled("Красивый")) {
            updatePretty();
        } else if (!prettyFigs.isEmpty()) {
            prettyFigs.clear();
        }
    }

    private void updatePretty() {
        // Обновление существующих палочек.
        long now = System.currentTimeMillis();
        Iterator<PrettyFig> it = prettyFigs.iterator();
        while (it.hasNext()) {
            PrettyFig f = it.next();
            f.tick();
            // Каждая палочка живёт своё время (установленное при создании)
            if (now - f.spawnTime > f.lifeMs) it.remove();
        }

        // Проверяем, должны ли палочки отображаться (учитываем настройку "От первого лица")
        boolean shouldShow = firstPerson.getValue() || !mc.options.getPerspective().isFirstPerson();
        if (!shouldShow) {
            return; // Не спавним новые палочки, только обновляем существующие
        }

        Vec3d pos = mc.player.getPos();
        Vec3d vel = mc.player.getVelocity();
        
        // Проверяем горизонтальное движение (бег) - игнорируем Y (прыжки на месте)
        double horizontalSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        boolean isRunning = horizontalSpeed > 0.01; // Порог для определения бега
        
        // Спавним только если игрок бежит
        if (isRunning) {
            // Количество = сколько палочек спавнится за один тик
            int spawnPerTick = (int) prettyCount.getValue();
            
            // Ограничиваем спавн хард капом для производительности
            int canSpawn = Math.min(spawnPerTick, PRETTY_HARD_CAP - prettyFigs.size());
            
            for (int i = 0; i < canSpawn; i++) {
                spawnPretty(pos);
            }
        }

        // Жёсткий cap только для защиты производительности
        while (prettyFigs.size() > PRETTY_HARD_CAP) prettyFigs.pollFirst();
    }

    private void spawnPretty(Vec3d base) {
        // Получаем направление взгляда игрока
        float yaw = mc.player.getYaw();
        double yawRad = Math.toRadians(yaw);
        
        // Спавним позади игрока (противоположно направлению взгляда)
        double backwardX = Math.sin(yawRad); // Назад по X
        double backwardZ = -Math.cos(yawRad); // Назад по Z
        
        // Используем настройку "Сближенность" для контроля разброса
        float spreadMultiplier = prettySpread.getFloatValue();
        
        // Расстояние позади игрока (с учетом сближенности)
        double backDist = (0.3 + random.nextDouble() * 0.5) * spreadMultiplier;
        
        // Небольшой разброс влево-вправо относительно траектории (с учетом сближенности)
        double sideOffset = (random.nextDouble() - 0.5) * 0.4 * spreadMultiplier;
        double sideX = -Math.cos(yawRad) * sideOffset;
        double sideZ = -Math.sin(yawRad) * sideOffset;
        
        double ox = backwardX * backDist + sideX;
        double oz = backwardZ * backDist + sideZ;
        
        // Спавним на середине тела (0.5 = середина высоты игрока)
        // С небольшим вертикальным разбросом ±20% от середины
        double oy = mc.player.getHeight() * (0.5 + (random.nextDouble() - 0.5) * 0.2 * spreadMultiplier);
        
        Vec3d p = base.add(ox, oy, oz);
        Vec3d vel = new Vec3d(
                (random.nextDouble() - 0.5) * 0.02,
                0.01 + random.nextDouble() * 0.02,
                (random.nextDouble() - 0.5) * 0.02);
        
        // Каждая палочка запоминает своё время жизни
        long life = (long) prettyLifetime.getValue();
        // Случайный угол поворота от 0 до 360 градусов
        float rot = random.nextFloat() * 360f;
        float rotSpeed = (random.nextFloat() - 0.5f) * 6f;
        int color = prettyRainbow.getValue() 
            ? (0xFF000000 | (Color.HSBtoRGB((System.currentTimeMillis() % 3600) / 3600f + random.nextFloat() * 0.1f, 0.7f, 1.0f) & 0x00FFFFFF))
            : ColorProvider.getColorClient();
        
        prettyFigs.addLast(new PrettyFig(p, vel, life, rot, rotSpeed, color));
    }

    private void onRender(WorldRenderContext context) {
        if (!this.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || !mc.player.isAlive()) {
            if (!entityPoints.isEmpty()) clearData();
            return;
        }

        float tickDelta = context.tickCounter().getTickDelta(true);
        boolean selfVisible = firstPerson.getValue() || !mc.options.getPerspective().isFirstPerson();
        long currentTime = System.currentTimeMillis();

        boolean entityEnabled = mode.isEnabled("Entity Trails");
        boolean pearlEnabled = mode.isEnabled("Pearl Trail");
        boolean prettyEnabled = mode.isEnabled("Красивый");

        if (pearlEnabled) renderPearlTrails(context, tickDelta, currentTime);
        if (prettyEnabled) renderPretty(context, tickDelta);

        if (!entityEnabled) {
            if (!entityPoints.isEmpty()) {
                entityPoints.clear();
                lastPositions.clear();
                lastPointTimes.clear();
                entityTypes.clear();
            }
            return;
        }

        // Скан всех сущностей — не чаще раза в 15мс (совпадает с шагом добавления точек).
        boolean scanNow = currentTime - lastEntityScan >= 15;
        if (scanNow) {
            lastEntityScan = currentTime;
            for (Entity entity : mc.world.getEntities()) {
                if (entity == null || !entity.isAlive() || !(entity instanceof LivingEntity)) {
                    continue;
                }

                boolean isTarget = false;
                EntityType entityType = null;

                if (entity == mc.player) {
                    isTarget = targetSelf.getValue() && selfVisible;
                    entityType = EntityType.SELF;
                } else if (entity instanceof PlayerEntity player) {
                    boolean isFriend = FriendRepository.isFriend(player.getNameForScoreboard());
                    if (isFriend) {
                        isTarget = targetFriends.getValue();
                        entityType = EntityType.FRIEND;
                    } else {
                        isTarget = targetPlayers.getValue();
                        entityType = EntityType.PLAYER;
                    }
                } else if (entity instanceof MobEntity) {
                    isTarget = targetMobs.getValue();
                    entityType = EntityType.MOB;
                }

                if (!isTarget) {
                    removeEntityData(entity.getId());
                    continue;
                }

                entityTypes.put(entity.getId(), entityType);

                Vec3d currentPos = entity.getLerpedPos(tickDelta);
                long lastTime = lastPointTimes.getOrDefault(entity.getId(), 0L);
                boolean shouldAddPoint = currentTime - lastTime >= 15;

                if (shouldAddPoint) {
                    Vec3d lastPos = lastPositions.get(entity.getId());
                    if (lastPos != null) {
                        double distanceMoved = lastPos.distanceTo(currentPos);
                        Deque<Point> existing = entityPoints.get(entity.getId());
                        shouldAddPoint = distanceMoved > 0.01 || existing == null || existing.isEmpty();
                    }

                    if (shouldAddPoint) {
                        Deque<Point> points = entityPoints.computeIfAbsent(entity.getId(), k -> new ArrayDeque<>());
                        points.addFirst(new Point(currentPos, currentTime));
                        lastPointTimes.put(entity.getId(), currentTime);
                        lastPositions.put(entity.getId(), currentPos);
                    }
                }
            }
        }

        Iterator<Map.Entry<Integer, Deque<Point>>> iterator = entityPoints.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Deque<Point>> entry = iterator.next();
            Entity entity = mc.world.getEntityById(entry.getKey());

            if (entity == null || !entity.isAlive()) {
                iterator.remove();
                removeEntityData(entry.getKey());
                continue;
            }

            Deque<Point> points = entry.getValue();
            points.removeIf(point -> currentTime - point.timestamp > (float) duration.getValue());

            if (points.isEmpty()) {
                iterator.remove();
                removeEntityData(entry.getKey());
            }
        }

        for (Map.Entry<Integer, Deque<Point>> entry : entityPoints.entrySet()) {
            Deque<Point> points = entry.getValue();
            if (points.size() < 2) continue;

            Entity entity = mc.world.getEntityById(entry.getKey());
            if (entity != null && entity.isAlive()) {
                EntityType entityType = entityTypes.get(entry.getKey());
                renderTrail(points, context, currentTime, entity.getHeight(), entityType);
            }
        }
    }

    private void renderPretty(WorldRenderContext context, float tickDelta) {
        if (prettyFigs.isEmpty()) return;

        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        MatrixStack stack = context.matrixStack();
        boolean glow = prettyGlow.getValue();
        float lineLength = prettyLineLength.getFloatValue();
        float lineWidth = 0.05f; // Фиксированная толщина палочки
        long now = System.currentTimeMillis();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        // Используем glow.png текстуру для эффекта свечения
        RenderSystem.setShaderTexture(0, Identifier.of("mre", "images/glow.png"));
        
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        boolean hasAnyQuads = false;
        
        for (PrettyFig f : prettyFigs) {
            // Используем индивидуальное время жизни палочки
            float life = (float) (now - f.spawnTime) / f.lifeMs;
            if (life >= 1f) continue;
            float alpha = life < 0.2f ? life * 5f : 1f - (life - 0.2f) / 0.8f;
            if (alpha <= 0.01f) continue;

            double px = MathHelper.lerp(tickDelta, f.prevX, f.x) - camPos.x;
            double py = MathHelper.lerp(tickDelta, f.prevY, f.y) - camPos.y;
            double pz = MathHelper.lerp(tickDelta, f.prevZ, f.z) - camPos.z;

            float r = ColorProvider.red(f.color) / 255f;
            float g = ColorProvider.green(f.color) / 255f;
            float b = ColorProvider.blue(f.color) / 255f;

            stack.push();
            stack.translate(px, py, pz);
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f.rotation + f.rotSpeed * tickDelta));
            Matrix4f m = stack.peek().getPositionMatrix();

            // Сначала рисуем свечение (большой квад с текстурой glow.png)
            if (glow) {
                float glowSize = lineWidth * 4.0f; // Свечение в 4 раза больше
                float glowHeight = lineLength * 1.5f;
                putLineQuad(buffer, m, glowSize, glowHeight, r, g, b, alpha * 0.3f);
                hasAnyQuads = true;
            }
            
            // Потом рисуем основную палочку (яркий квад поверх свечения)
            putLineQuad(buffer, m, lineWidth, lineLength, r, g, b, alpha);
            hasAnyQuads = true;

            stack.pop();
        }

        // Рисуем только если есть хоть один квад
        if (hasAnyQuads) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, 0);
    }

    private void putLineQuad(BufferBuilder buffer, Matrix4f m, float width, float height, float r, float g, float b, float alpha) {
        int ri = (int) (r * 255), gi = (int) (g * 255), bi = (int) (b * 255), ai = (int) (MathHelper.clamp(alpha, 0f, 1f) * 255);
        float halfWidth = width / 2f;
        float halfHeight = height / 2f;
        
        // Рисуем вертикальный прямоугольник (палочку)
        buffer.vertex(m, -halfWidth,  halfHeight, 0).texture(0f, 0f).color(ri, gi, bi, ai);
        buffer.vertex(m,  halfWidth,  halfHeight, 0).texture(1f, 0f).color(ri, gi, bi, ai);
        buffer.vertex(m,  halfWidth, -halfHeight, 0).texture(1f, 1f).color(ri, gi, bi, ai);
        buffer.vertex(m, -halfWidth, -halfHeight, 0).texture(0f, 1f).color(ri, gi, bi, ai);
    }

    private void renderPearlTrails(WorldRenderContext context, float tickDelta, long now) {
        int color = 0xFF000000 | (ColorProvider.getColorClient() & 0x00FFFFFF);
        for (Entity entity : MinecraftClient.getInstance().world.getEntities()) {
            if (!(entity instanceof EnderPearlEntity pearl) || !pearl.isAlive()) continue;
            Vec3d position = pearl.getLerpedPos(tickDelta);
            Vec3d velocity = pearl.getVelocity();
            for (int i = 0; i < 4; i++) {
                Vec3d framePosition = position.subtract(velocity.multiply(i * 0.45));
                long frameAge = (now + i * 120L) % 600L;
                MasEffectRenderer.render("pearl_trail", context.matrixStack(), context.camera(),
                        framePosition, frameAge, 600L, 0.32f * (1.0f - i * 0.14f), color);
            }
        }
    }

    private void renderTrail(Deque<Point> points, WorldRenderContext context, long currentTime, float entityHeight, EntityType entityType) {
        MatrixStack matrixStack = context.matrixStack();
        Vec3d camPos = context.camera().getPos();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();

        RenderSystem.enableDepthTest();

        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();

        matrixStack.push();
        matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        int index = 0;
        for (Point point : points) {
            float lifeRatio = Math.min(1.0f, (float) ((currentTime - point.timestamp) / duration.getValue()));
            float alpha = (float) ((opacity.getValue() / 255.0f) * (1.0f - easeOutQuad(lifeRatio)));

            int color = getColorForEntity(entityType, index);
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;

            buffer.vertex(matrix, (float) point.pos.x, (float) point.pos.y, (float) point.pos.z).color(r, g, b, alpha);
            buffer.vertex(matrix, (float) point.pos.x, (float) (point.pos.y + entityHeight - 0.1D), (float) point.pos.z).color(r, g, b, alpha);
            index++;
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.lineWidth((float) lineWidth.getValue());

        BufferBuilder bufferLineBottom = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        index = 0;
        for (Point point : points) {
            float lifeRatio = Math.min(1.0f, (float) ((currentTime - point.timestamp) / duration.getValue()));
            float alpha = (float) ((opacity.getValue() / 255.0f) * (1.0f - easeOutQuad(lifeRatio)));

            int color = getColorForEntity(entityType, index);
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;

            bufferLineBottom.vertex(matrix, (float) point.pos.x, (float) point.pos.y, (float) point.pos.z).color(r, g, b, alpha);
            index++;
        }
        BufferRenderer.drawWithGlobalProgram(bufferLineBottom.end());

        BufferBuilder bufferLineTop = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        index = 0;
        for (Point point : points) {
            float lifeRatio = Math.min(1.0f, (float) ((currentTime - point.timestamp) / duration.getValue()));
            float alpha = (float) ((opacity.getValue() / 255.0f) * (1.0f - easeOutQuad(lifeRatio)));

            int color = getColorForEntity(entityType, index);
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;

            bufferLineTop.vertex(matrix, (float) point.pos.x, (float) (point.pos.y + entityHeight - 0.1D), (float) point.pos.z).color(r, g, b, alpha);
            index++;
        }
        BufferRenderer.drawWithGlobalProgram(bufferLineTop.end());

        matrixStack.pop();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private int getColorForEntity(EntityType entityType, int index) {
        int clientColor = ColorProvider.getColorClient();

        if (entityType == null) return clientColor;

        switch (entityType) {
            case PLAYER:
                return new Color(255, 85, 85).getRGB();
            case MOB:
                return new Color(255, 170, 0).getRGB();
            case FRIEND:
            case SELF:
            default:
                return clientColor;
        }
    }

    private float easeOutQuad(float x) {
        return 1 - (1 - x) * (1 - x);
    }

    private void removeEntityData(int entityId) {
        lastPositions.remove(entityId);
        lastPointTimes.remove(entityId);
        entityTypes.remove(entityId);
    }

    public static class Point {
        public final Vec3d pos;
        public final long timestamp;

        public Point(Vec3d pos, long timestamp) {
            this.pos = pos;
            this.timestamp = timestamp;
        }
    }

    private static final class PrettyFig {
        double x, y, z;
        double prevX, prevY, prevZ;
        double vx, vy, vz;
        final long spawnTime = System.currentTimeMillis();
        final long lifeMs; // Индивидуальное время жизни
        float rotation;
        final float rotSpeed;
        final int color;

        PrettyFig(Vec3d pos, Vec3d vel, long lifeMs, float rotation, float rotSpeed, int color) {
            this.x = pos.x; this.y = pos.y; this.z = pos.z;
            this.prevX = x; this.prevY = y; this.prevZ = z;
            this.vx = vel.x; this.vy = vel.y; this.vz = vel.z;
            this.lifeMs = lifeMs;
            this.rotation = rotation;
            this.rotSpeed = rotSpeed;
            this.color = color;
        }

        void tick() {
            prevX = x; prevY = y; prevZ = z;
            x += vx; y += vy; z += vz;
            vy *= 0.96;
            vx *= 0.98;
            vz *= 0.98;
            rotation += rotSpeed;
        }
    }
}
