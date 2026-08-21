package zov.alphadlc.module.list.render;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import zov.alphadlc.event.list.EventAttack;
import zov.alphadlc.event.list.EventPlayerUpdate;
import zov.alphadlc.event.list.EventPopTotem;
import zov.alphadlc.event.list.EventWorldRender;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeListSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.module.settings.impl.Theme;
import zov.alphadlc.module.settings.impl.ThemeManager;
import zov.alphadlc.util.render.math.MathUtil;
import zov.alphadlc.util.player.move.MoveUtil;

import java.util.ArrayList;
import java.util.List;


@ModuleInformation(moduleName = "Particles", moduleDesc = "Добавляет частицы при разных условиях", moduleCategory = ModuleCategory.RENDER)
public class Particles extends Module {

    private final ModeListSetting type = new ModeListSetting("Режим",
            new BooleanSetting("Доллары", true),
            new BooleanSetting("Сердечки", false),
            new BooleanSetting("Крестики", false),
            new BooleanSetting("Молнии", false),
            new BooleanSetting("Линии", false),
            new BooleanSetting("Сияние", false),
            new BooleanSetting("Тыковки", false),
            new BooleanSetting("Снежинки", false),
            new BooleanSetting("Взрыв", false)
    );

    private final ModeListSetting reason = new ModeListSetting("Добавлять при",
            new BooleanSetting("Бездействии", false),
            new BooleanSetting("Беге", false),
            new BooleanSetting("Ударе", true),
            new BooleanSetting("Падении перла", true),
            new BooleanSetting("Падении трезубца", true),
            new BooleanSetting("Падении стрелы", true),
            new BooleanSetting("Сносе тотема", true)
    );

    private final SliderSetting count = new SliderSetting("Количество", 10, 2, 40, 1);

    private final List<Particle> particles = new ArrayList<>();
    private boolean isPlayerTotem = false;
    private long lastUpdateTime = System.currentTimeMillis();

    public Particles() {
        super();
    }

    private boolean isPositionInBlock(Vec3d position) {
        BlockPos blockPos = BlockPos.ofFloored(position.x, position.y, position.z);
        if (mc.world.getBlockState(blockPos).isSolidBlock(mc.world, blockPos)) {
            return true;
        }
        return false;
    }

    @Subscribe
    public void onWorldRender(EventWorldRender event) {
        if (particles.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        particles.removeIf(particle -> currentTime - particle.startTime > particle.lifeTime);

        MatrixStack matrices = event.getMatrixStack();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        float camYaw = mc.gameRenderer.getCamera().getYaw();
        float camPitch = mc.gameRenderer.getCamera().getPitch();

        List<String> enabledTypes = type.getEnabledModules();
        if (enabledTypes.isEmpty()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        List<Particle> renderList = new ArrayList<>();
        for (Particle particle : particles) {
            particle.update();
            Vec3d pos = particle.position;
            double halfSize = particle.size * 0.5;
            Box aabb = new Box(pos.x - halfSize, pos.y - halfSize, pos.z - halfSize,
                    pos.x + halfSize, pos.y + halfSize, pos.z + halfSize);
            if (!mc.worldRenderer.frustum.isVisible(aabb)) continue;
            renderList.add(particle);
        }

        for (String modeName : enabledTypes) {
            Identifier texture = Identifier.of("mre", "images/" + getTexturePath(modeName));
            RenderSystem.setShaderTexture(0, texture);

            List<Particle> modeParticles = new ArrayList<>();
            for (Particle p : renderList) {
                if (modeName.equals(p.particleType)) modeParticles.add(p);
            }
            if (modeParticles.isEmpty()) continue;

            if ("Сияние".equals(modeName)) {
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                for (Particle particle : modeParticles) {
                    renderParticleBloom(buffer, matrices, particle, cameraPos, camYaw, camPitch, 2.0f, 0.15f, 1.0f);
                }
                BufferRenderer.drawWithGlobalProgram(buffer.end());

                buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                for (Particle particle : modeParticles) {
                    renderParticleBloom(buffer, matrices, particle, cameraPos, camYaw, camPitch, 1.0f, 0.35f, 1.0f);
                }
                BufferRenderer.drawWithGlobalProgram(buffer.end());

                buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                for (Particle particle : modeParticles) {
                    renderParticleBloom(buffer, matrices, particle, cameraPos, camYaw, camPitch, 0.4f, 1.0f, 1.5f);
                }
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            } else {
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                for (Particle particle : modeParticles) {
                    renderParticle(buffer, matrices, particle, cameraPos, camYaw, camPitch);
                }
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @Subscribe
    public void onPlayerUpdate(EventPlayerUpdate event) {
        long currentTime = System.currentTimeMillis();


        if (reason.isEnabled("Бездействии") && !MoveUtil.hasPlayerMovement()) {
            if (currentTime - lastUpdateTime > 50) {
                addIdleParticles();
                lastUpdateTime = currentTime;
            }
        }


        if (reason.isEnabled("Беге") && MoveUtil.hasPlayerMovement()) {
            addMovingParticles();
        }


        for (Entity entity : mc.world.getEntities()) {
            if (reason.isEnabled("Падении перла") && entity instanceof EnderPearlEntity pearl) {
                if (!pearl.isOnGround()) {
                    createProjectileParticles(pearl.getPos());
                }
            }
            if (reason.isEnabled("Падении трезубца") && entity instanceof TridentEntity trident) {
                if (isProjectileFlying(trident)) {
                    createProjectileParticles(trident.getPos());
                }
            }
            if (reason.isEnabled("Падении стрелы") && entity instanceof ArrowEntity arrow) {
                if (isProjectileFlying(arrow)) {
                    createProjectileParticles(arrow.getPos());
                }
            }
        }
    }

    @Subscribe
    public void onAttack(EventAttack event) {
        if (!reason.isEnabled("Ударе") || event.getEntity() == null) return;

        Entity target = event.getEntity();
        for (int i = 0; i < count.getIntValue(); i++) {
            double targetX = target.getX() + MathUtil.random(-0.4f, 0.4f);
            double targetY = target.getY() + MathUtil.random(-0.4f, target.getHeight() + 0.4f);
            double targetZ = target.getZ() + MathUtil.random(-0.4f, 0.4f);

            if (isPositionInBlock(new Vec3d(targetX, targetY, targetZ))) continue;

            float baseMx = (float) (MathUtil.random(-0.8f, 0.8f) * 2.0f);
            float baseMy = (float) MathUtil.random(-0.25f, 1.4f);
            float baseMz = (float) (MathUtil.random(-0.8f, 0.8f) * 2.0f);

            float smooth = 0.5f;
            long life = (long) MathUtil.random(1000, 1200);

            Vec3d velocity = new Vec3d(baseMx * 0.075f, baseMy * 0.075f, baseMz * 0.075f);
            addParticle(targetX, targetY, targetZ, velocity, getThemeColor(), 0.6f, life, smooth, 0.0007f);
        }
    }

    @Subscribe
    public void onPopTotem(EventPopTotem event) {
        if (!reason.isEnabled("Сносе тотема")) return;
        isPlayerTotem = (event.getPlayer() == mc.player);

        if (!isPlayerTotem) {
            Vec3d pos = event.getPlayer().getPos();
            double centerX = pos.x;
            double centerY = pos.y + event.getPlayer().getHeight() / 2.0;
            double centerZ = pos.z;

            for (int i = 0; i < count.getIntValue(); i++) {
                double theta = Math.random() * 2.0 * Math.PI;
                double phi = Math.random() * Math.PI;
                double speed = (Math.random() * 0.5 + 0.5) * 0.1;

                double vx = Math.sin(phi) * Math.cos(theta) * speed;
                double vy = Math.sin(phi) * Math.sin(theta) * speed;
                double vz = Math.cos(phi) * speed;

                double spawnX = centerX + MathUtil.random(-0.3f, 0.3f);
                double spawnY = centerY + MathUtil.random(-0.3f, 0.3f);
                double spawnZ = centerZ + MathUtil.random(-0.3f, 0.3f);

                if (isPositionInBlock(new Vec3d(spawnX, spawnY, spawnZ))) continue;

                int color = Math.random() < 0.7 ? 0xFF00FF00 : 0xFFFFFF00;
                float smooth = 2.0f;
                long life = (long) MathUtil.random(1500, 2000);

                addParticle(spawnX, spawnY, spawnZ, new Vec3d(vx, vy, vz), color, 0.6f, life, smooth, 0.00005f);
            }
        }
    }

    private void addIdleParticles() {
        Vec3d base = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getHeight() / 2.0, mc.player.getZ());

        for (int i = 0; i < count.getIntValue(); i++) {
            double distance = MathUtil.random(7, 35);
            double angle = Math.toRadians(MathUtil.random(0, 360));
            double height = MathUtil.random(-7, 25);

            Vec3d offset = new Vec3d(Math.cos(angle) * distance, height, Math.sin(angle) * distance);
            Vec3d spawnPos = base.add(offset);

            if (isPositionInBlock(spawnPos)) continue;

            long life = (long) MathUtil.random(1500, 2000);
            double speed = Math.random() < 0.8 ? MathUtil.random(0.015f, 0.03f) : 0.125f;
            double phi = Math.toRadians(MathUtil.random(0, 360));
            float smooth = 3;

            Vec3d velocity = new Vec3d(Math.cos(phi) * speed, MathUtil.random(-speed * 0.1f, speed * 0.1f), Math.sin(phi) * speed);
            addParticle(spawnPos.x, spawnPos.y, spawnPos.z, velocity, getThemeColor(), 0.6f, life, smooth, 0.00005f);
        }
    }

    private void addMovingParticles() {
        double speed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
        Vec3d direction;

        if (speed < 0.01) {
            direction = mc.player.getRotationVec(1.0f).multiply(-1);
        } else if (mc.player.isGliding()) {
            direction = mc.player.getVelocity().normalize().multiply(-1);
        } else {
            Vec3d motion = mc.player.getVelocity();
            direction = new Vec3d(-motion.x / speed, 0, -motion.z / speed);
        }

        double distanceBehind = (mc.player.isGliding() ? 1.2 : 0.5) + (speed > 0.1 ? speed * 1.5 : 0);
        double offsetX = MathUtil.random(-0.35f, 0.35f);
        double offsetZ = MathUtil.random(-0.35f, 0.35f);

        double posX = mc.player.getX() + direction.x * distanceBehind + offsetX;
        double posY = mc.player.isGliding()
                ? mc.player.getY() + mc.player.getHeight() / 2.0 + direction.y * distanceBehind + MathUtil.random(-0.35f, 0.35f)
                : mc.player.getY() + MathUtil.random(0.2f, mc.player.getHeight() + 0.1f);
        double posZ = mc.player.getZ() + direction.z * distanceBehind + offsetZ;

        if (!isPositionInBlock(new Vec3d(posX, posY, posZ))) {
            double baseSpeed = 0.075;
            Vec3d velocity = direction.multiply(baseSpeed).add(new Vec3d(MathUtil.random(-0.01f, 0.01f), MathUtil.random(-0.05f, 0.01f), MathUtil.random(-0.01f, 0.01f))).multiply(0.1f);
            long life = (long) MathUtil.random(1500, 2000);
            addParticle(posX, posY, posZ, velocity, getThemeColor(), 0.6f, life, 3, 0.00005f);
        }
    }

    private void createProjectileParticles(Vec3d position) {
        final int particleColor = getThemeColor();

        for (int i = 0; i < count.getIntValue(); i++) {
            final double distance = 0f;
            final double angle = Math.toRadians(MathUtil.random(0, 360));
            final double cosAngle = Math.cos(angle);
            final double sinAngle = Math.sin(angle);

            final double dx = cosAngle * distance;
            final double dz = sinAngle * distance;
            final double dy = MathUtil.random(0.1f, 0.35f);

            final Vec3d particlePos = new Vec3d(position.x + dx, position.y + dy, position.z + dz);
            if (isPositionInBlock(particlePos)) continue;

            final float life = (float) MathUtil.random(2400, 2800);
            final float speedMin = (float) MathUtil.random(0.015f, 0.0375f);
            final float speedMax = (float) MathUtil.random(0.05f, 0.075f);
            final double speedFinal = MathUtil.random(speedMin, speedMax);
            final double speedFinalY = speedFinal * 0.4;

            final double angleVel = Math.toRadians(MathUtil.random(0, 360));
            final double cosVel = Math.cos(angleVel);
            final double sinVel = Math.sin(angleVel);

            final double velX = cosVel * speedFinal;
            final double velZ = sinVel * speedFinal;
            final double velY = MathUtil.random(-speedFinalY, speedFinalY);

            addParticle(particlePos.x, particlePos.y, particlePos.z, new Vec3d(velX, velY, velZ), particleColor, 0.375f, (long) life, 2, 0.00005f);
        }
    }

    private boolean isProjectileFlying(Entity projectile) {
        if (projectile.isOnGround() || projectile.getVelocity().lengthSquared() <= 0.0001) return false;

        Vec3d pos = projectile.getPos();
        Vec3d motion = projectile.getVelocity().normalize().multiply(0.5);

        BlockPos currentPos = BlockPos.ofFloored(pos);
        BlockPos frontPos = BlockPos.ofFloored(pos.add(motion));

        return mc.world.getBlockState(currentPos).isAir() && mc.world.getBlockState(frontPos).isAir();
    }

    private void addParticle(double x, double y, double z, Vec3d velocity, int color, float size, long lifeTime, float smooth, double gravity) {
        List<String> enabledTypes = type.getEnabledModules();
        if (enabledTypes.isEmpty()) return;
        String particleType = enabledTypes.get((int)(Math.random() * enabledTypes.size()));
        Vec3d safePos = checkCollision(x, y, z, size);
        if (safePos != null) {
            particles.add(new Particle(safePos.x, safePos.y, safePos.z, velocity, color, size, lifeTime, smooth, gravity, particleType));
        }
    }

    private Vec3d checkCollision(double x, double y, double z, float size) {
        double half = size * 0.5;
        int minX = MathHelper.floor(x - half);
        int maxX = MathHelper.floor(x + half);
        int minY = MathHelper.floor(y - half);
        int maxY = MathHelper.floor(y + half);
        int minZ = MathHelper.floor(z - half);
        int maxZ = MathHelper.floor(z + half);

        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    BlockState state = mc.world.getBlockState(pos.set(bx, by, bz));
                    if (!state.isAir()) return null;
                }
            }
        }
        return new Vec3d(x, y, z);
    }

    private void renderParticle(BufferBuilder buffer, MatrixStack matrices, Particle particle, Vec3d cameraPos, float camYaw, float camPitch) {
        if (particle.alpha <= 0.001f || particle.size <= 0.0001f) return;

        matrices.push();


        double rx = particle.position.x - cameraPos.x;
        double ry = particle.position.y - cameraPos.y;
        double rz = particle.position.z - cameraPos.z;

        matrices.translate(rx, ry, rz);


        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));

        float size = particle.size;
        matrices.scale(size, size, size);

        Matrix4f matrix = matrices.peek().getPositionMatrix();


        int r = (particle.color >> 16) & 0xFF;
        int g = (particle.color >> 8) & 0xFF;
        int b = particle.color & 0xFF;
        int a = (int)(particle.alpha * 255);


        buffer.vertex(matrix, -0.5f,  0.5f, 0).texture(0f, 1f).color(r, g, b, a);
        buffer.vertex(matrix,  0.5f,  0.5f, 0).texture(1f, 1f).color(r, g, b, a);
        buffer.vertex(matrix,  0.5f, -0.5f, 0).texture(1f, 0f).color(r, g, b, a);
        buffer.vertex(matrix, -0.5f, -0.5f, 0).texture(0f, 0f).color(r, g, b, a);

        matrices.pop();
    }

    private void renderParticleBloom(BufferBuilder buffer, MatrixStack matrices, Particle particle, Vec3d cameraPos, float camYaw, float camPitch, float sizeMultiplier, float alphaMultiplier, float colorBoost) {
        if (particle.alpha <= 0.001f || particle.size <= 0.0001f) return;

        matrices.push();

        double rx = particle.position.x - cameraPos.x;
        double ry = particle.position.y - cameraPos.y;
        double rz = particle.position.z - cameraPos.z;

        matrices.translate(rx, ry, rz);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));

        float size = particle.size * sizeMultiplier;
        matrices.scale(size, size, size);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int r = Math.min(255, (int)(((particle.color >> 16) & 0xFF) * colorBoost));
        int g = Math.min(255, (int)(((particle.color >> 8) & 0xFF) * colorBoost));
        int b = Math.min(255, (int)((particle.color & 0xFF) * colorBoost));
        int a = (int)(particle.alpha * alphaMultiplier * 255);

        buffer.vertex(matrix, -0.5f,  0.5f, 0).texture(0f, 1f).color(r, g, b, a);
        buffer.vertex(matrix,  0.5f,  0.5f, 0).texture(1f, 1f).color(r, g, b, a);
        buffer.vertex(matrix,  0.5f, -0.5f, 0).texture(1f, 0f).color(r, g, b, a);
        buffer.vertex(matrix, -0.5f, -0.5f, 0).texture(0f, 0f).color(r, g, b, a);

        matrices.pop();
    }

    private int getThemeColor() {
        Theme theme = ThemeManager.getInstance().getCurrentTheme();
        return theme != null ? theme.color1 : 0xFFFFFFFF;
    }

    private static String getTexturePath(String displayName) {
        return switch (displayName) {
            case "Сердечки" -> "heart.png";
            case "Крестики" -> "cross.png";
            case "Молнии" -> "lightning.png";
            case "Линии" -> "line.png";
            case "Сияние" -> "firefly.png";
            case "Тыковки" -> "pumpkin.png";
            case "Снежинки" -> "snow.png";
            case "Взрыв" -> "star.png";
            default -> "dollar.png";
        };
    }

    @Override
    public void onDisable() {
        particles.clear();
        super.onDisable();
    }

    @Getter
    public static class Particle {
        Vec3d position, velocity;
        int color;
        float size, alpha = 1.0f, smoothFactor;
        long lifeTime, startTime;
        double gravity;
        String particleType;
        private long lastUpdateNs;
        private static MinecraftClient mc = MinecraftClient.getInstance();

        public Particle(double x, double y, double z, Vec3d velocity, int color, float size, long lifeTime, float smooth, double gravity, String particleType) {
            this.position = new Vec3d(x, y, z);
            this.velocity = velocity;
            this.color = color;
            this.size = size;
            this.lifeTime = lifeTime;
            this.startTime = System.currentTimeMillis();
            this.lastUpdateNs = System.nanoTime();
            this.smoothFactor = smooth;
            this.gravity = gravity;
            this.particleType = particleType;
        }

        public void update() {
            long nowNs = System.nanoTime();
            double deltaSec = (nowNs - lastUpdateNs) / 1_000_000_000.0;
            lastUpdateNs = nowNs;

            long now = System.currentTimeMillis();
            float progress = Math.min(1.0f, (float) (now - startTime) / lifeTime);
            double factor = Math.pow(1.0 - progress, smoothFactor);

            double vx = velocity.x;
            double vy = velocity.y;
            double vz = velocity.z;

            double newX = position.x;
            double newY = position.y;
            double newZ = position.z;


            newX += vx * factor * (deltaSec * 60);
            if (checkCollision(newX, position.y, position.z, size) == null) {
                vx = -vx * 0.8;
                newX = position.x;
            }

            newY += vy * factor * (deltaSec * 60);
            if (checkCollision(newX, newY, position.z, size) == null) {
                vy = -vy * 1.5;
                newY = position.y;
            }

            newZ += vz * factor * (deltaSec * 60);
            if (checkCollision(newX, newY, newZ, size) == null) {
                vz = -vz * 0.8;
                newZ = position.z;
            }

            position = new Vec3d(newX, newY, newZ);
            velocity = new Vec3d(vx * 0.9999, vy * 0.9999 - gravity, vz * 0.9999);
            alpha = 1.0f - progress;
        }

        private static Vec3d checkCollision(double x, double y, double z, float size) {
            if (mc == null || mc.world == null) return new Vec3d(x, y, z);

            double half = size * 0.5;
            int minX = MathHelper.floor(x - half);
            int maxX = MathHelper.floor(x + half);
            int minY = MathHelper.floor(y - half);
            int maxY = MathHelper.floor(y + half);
            int minZ = MathHelper.floor(z - half);
            int maxZ = MathHelper.floor(z + half);

            BlockPos.Mutable pos = new BlockPos.Mutable();
            for (int bx = minX; bx <= maxX; bx++) {
                for (int by = minY; by <= maxY; by++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        BlockState state = mc.world.getBlockState(pos.set(bx, by, bz));
                        if (!state.isAir() && state.isSolidBlock(mc.world, pos)) {
                            return null;
                        }
                    }
                }
            }
            return new Vec3d(x, y, z);
        }
    }
}

