package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.polar;

public class HandsSmoke
extends Module {
    public static HandsSmoke INSTANCE = new HandsSmoke();
    private final FloatSetting particleLifetime = new FloatSetting("Время жизни", 2.0f, 0.5f, 5.0f, 0.1f);
    private final FloatSetting riseSpeed = new FloatSetting("Скорость подъема", 0.05f, 0.01f, 0.2f, 0.01f);
    private final FloatSetting spreadAmount = new FloatSetting("Разброс", 0.1f, 0.0f, 0.5f, 0.01f);
    private final FloatSetting particleSize = new FloatSetting("Размер", 0.3f, 0.1f, 1.0f, 0.05f);
    private final FloatSetting density = new FloatSetting("Плотность", 0.7f, 0.1f, 1.0f, 0.1f);
    private final List<SmokeParticle> particles = new ArrayList<SmokeParticle>();
    private final Random random = new Random();
    private int tickCounter = 0;

    public HandsSmoke() {
        super("HandsSmoke", "Дым от рук", Module.ModuleCategory.RENDER);
        this.addSettings(this.particleLifetime, this.riseSpeed, this.spreadAmount, this.particleSize, this.density);
    }

    @Override
    public void onDisable() {
        this.particles.clear();
        super.onDisable();
    }

    @EventLink
    public void onRender(Event3DRender event) {
        if (HandsSmoke.mc.player == null || HandsSmoke.mc.world == null) {
            return;
        }
        ++this.tickCounter;
        if (this.tickCounter % 6 == 0) {
            this.spawnHandParticles(event.getTickDelta());
        }
        this.updateParticles();
        this.renderParticles(event.getMatrices(), event.getTickDelta());
    }

    private void spawnHandParticles(float partialTicks) {
        Vec3d playerPos = new Vec3d(MathHelper.lerp((double)partialTicks, (double)HandsSmoke.mc.player.prevX, (double)HandsSmoke.mc.player.getX()), MathHelper.lerp((double)partialTicks, (double)HandsSmoke.mc.player.prevY, (double)HandsSmoke.mc.player.getY()), MathHelper.lerp((double)partialTicks, (double)HandsSmoke.mc.player.prevZ, (double)HandsSmoke.mc.player.getZ()));
        float yaw = MathHelper.lerp((float)partialTicks, (float)HandsSmoke.mc.player.prevYaw, (float)HandsSmoke.mc.player.getYaw());
        float pitch = MathHelper.lerp((float)partialTicks, (float)HandsSmoke.mc.player.prevPitch, (float)HandsSmoke.mc.player.getPitch());
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double handHeight = (double)HandsSmoke.mc.player.getStandingEyeHeight() - 0.5;
        double rightX = playerPos.x + Math.cos(yawRad + 1.5707963267948966) * 0.4;
        double rightY = playerPos.y + handHeight;
        double rightZ = playerPos.z + Math.sin(yawRad + 1.5707963267948966) * 0.4;
        this.spawnParticle(new Vec3d(rightX, rightY, rightZ));
        double leftX = playerPos.x + Math.cos(yawRad - 1.5707963267948966) * 0.4;
        double leftY = playerPos.y + handHeight;
        double leftZ = playerPos.z + Math.sin(yawRad - 1.5707963267948966) * 0.4;
        this.spawnParticle(new Vec3d(leftX, leftY, leftZ));
    }

    private void spawnParticle(Vec3d pos) {
        float spread = this.spreadAmount.get();
        Vec3d velocity = new Vec3d(((double)this.random.nextFloat() - 0.5) * (double)spread, (double)this.riseSpeed.get(), ((double)this.random.nextFloat() - 0.5) * (double)spread);
        this.particles.add(new SmokeParticle(pos, velocity, System.currentTimeMillis()));
    }

    private void updateParticles() {
        long currentTime = System.currentTimeMillis();
        float maxLifetime = this.particleLifetime.get() * 1000.0f;
        Iterator<SmokeParticle> iterator = this.particles.iterator();
        while (iterator.hasNext()) {
            SmokeParticle particle = iterator.next();
            if ((float)(currentTime - particle.spawnTime) > maxLifetime) {
                iterator.remove();
                continue;
            }
            particle.pos = particle.pos.add(particle.velocity);
            double time = (double)(currentTime - particle.spawnTime) * 0.001;
            particle.velocity = new Vec3d(particle.velocity.x + Math.sin(time * 2.0) * 0.001, particle.velocity.y * 0.98, particle.velocity.z + Math.cos(time * 2.0) * 0.001);
            particle.size += 0.001f;
        }
    }

    private void renderParticles(MatrixStack matrixStack, float partialTicks) {
        if (this.particles.isEmpty()) {
            return;
        }
        this.startRendering();
        matrixStack.push();
        Vec3d view = HandsSmoke.mc.gameRenderer.getCamera().getPos();
        matrixStack.translate(-view.x, -view.y, -view.z);
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        long currentTime = System.currentTimeMillis();
        float maxLifetime = this.particleLifetime.get() * 1000.0f;
        int color1 = polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow") ? ColorUtils.getThemeColor(0) : polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        int color2 = polar.INSTANCE.themeStorage.getThemes().getTheme().color.length > 1 ? polar.INSTANCE.themeStorage.getThemes().getTheme().color[1] : color1;
        for (SmokeParticle particle : this.particles) {
            float age = (float)(currentTime - particle.spawnTime) / maxLifetime;
            float alpha = (1.0f - age) * this.density.get();
            if (alpha < 0.01f) continue;
            float colorMix = age * 0.6f + (float)Math.sin((double)age * Math.PI) * 0.4f;
            float r2 = MathHelper.lerp((float)colorMix, (float)ColorUtils.redf(color1), (float)ColorUtils.redf(color2));
            float g2 = MathHelper.lerp((float)colorMix, (float)ColorUtils.greenf(color1), (float)ColorUtils.greenf(color2));
            float b2 = MathHelper.lerp((float)colorMix, (float)ColorUtils.bluef(color1), (float)ColorUtils.bluef(color2));
            float brighten = age * 0.3f;
            r2 = Math.min(1.0f, r2 + brighten);
            g2 = Math.min(1.0f, g2 + brighten);
            b2 = Math.min(1.0f, b2 + brighten);
            this.renderParticle(matrix, particle, r2, g2, b2, alpha);
        }
        matrixStack.pop();
        this.stopRendering();
    }

    private void renderParticle(Matrix4f matrix, SmokeParticle particle, float r2, float g2, float b2, float alpha) {
        float size = particle.size * this.particleSize.get();
        Vec3d[] offsets = new Vec3d[]{new Vec3d((double)(-size), (double)(-size), 0.0), new Vec3d((double)size, (double)(-size), 0.0), new Vec3d((double)size, (double)size, 0.0), new Vec3d((double)(-size), (double)size, 0.0)};
        Vec3d camPos = HandsSmoke.mc.gameRenderer.getCamera().getPos();
        Vec3d toCamera = camPos.subtract(particle.pos).normalize();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (Vec3d offset : offsets) {
            double x2 = particle.pos.x + offset.x;
            double y2 = particle.pos.y + offset.y;
            double z2 = particle.pos.z + offset.z;
            buffer.vertex(matrix, (float)x2, (float)y2, (float)z2).color(r2, g2, b2, alpha);
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private void startRendering() {
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
    }

    private void stopRendering() {
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static class SmokeParticle {
        public Vec3d pos;
        public Vec3d velocity;
        public long spawnTime;
        public float size;

        public SmokeParticle(Vec3d pos, Vec3d velocity, long spawnTime) {
            this.pos = pos;
            this.velocity = velocity;
            this.spawnTime = spawnTime;
            this.size = 0.2f;
        }
    }
}

