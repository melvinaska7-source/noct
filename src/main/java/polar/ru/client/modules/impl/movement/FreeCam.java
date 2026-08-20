package polar.ru.client.modules.impl.movement;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventMove;
import polar.ru.api.events.implement.EventMoveInput;
import polar.ru.api.events.implement.EventPacket;
import polar.ru.client.modules.Module;

public class FreeCam
extends Module {
    public static FreeCam INSTANCE = new FreeCam();
    public Vec3d pos;

    public FreeCam() {
        super("FreeCam", "Обзор местности за фейк игрока", Module.ModuleCategory.MOVEMENT);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (FreeCam.mc.player != null) {
            this.pos = FreeCam.mc.player.getPos();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (FreeCam.mc.player != null && this.pos != null) {
            FreeCam.mc.player.setPosition(this.pos);
        }
    }

    @EventLink
    public void onEvent(EventPacket event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerMoveC2SPacket) {
            event.cancel();
        } else if (packet instanceof PlayerRespawnS2CPacket || packet instanceof GameJoinS2CPacket) {
            this.toggle();
        }
    }

    @EventLink
    public void onEvent(Event3DRender event) {
        if (this.pos == null || FreeCam.mc.player == null) {
            return;
        }
        float width = FreeCam.mc.player.getWidth() / 2.0f;
        float height = FreeCam.mc.player.getHeight();
        Box box = new Box(this.pos.x - (double)width, this.pos.y, this.pos.z - (double)width, this.pos.x + (double)width, this.pos.y + (double)height, this.pos.z + (double)width);
        this.drawHitbox(event.getMatrices(), box, event.getCamera().getPos());
    }

    private void drawHitbox(MatrixStack matrices, Box box, Vec3d camera) {
        double x1 = box.minX - camera.x;
        double y1 = box.minY - camera.y;
        double z1 = box.minZ - camera.z;
        double x2 = box.maxX - camera.x;
        double y2 = box.maxY - camera.y;
        double z2 = box.maxZ - camera.z;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth((float)1.5f);
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        float r2 = 1.0f;
        float g2 = 1.0f;
        float b2 = 1.0f;
        float a2 = 1.0f;
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x2, (float)y1, (float)z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x2, (float)y1, (float)z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x2, (float)y1, (float)z2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x2, (float)y1, (float)z2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x1, (float)y2, (float)z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x1, (float)y2, (float)z2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x1, (float)y2, (float)z2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x1, (float)y2, (float)z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x1, (float)y2, (float)z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x2, (float)y1, (float)z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x2, (float)y1, (float)z2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, (float)x1, (float)y2, (float)z2).color(r2, g2, b2, a2);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @EventLink
    public void onEvent(EventMove event) {
        if (FreeCam.mc.player == null) {
            return;
        }
        FreeCam.mc.player.noClip = true;
        double speed = 1.0;
        double forward = FreeCam.mc.player.input.movementForward;
        double strafe = FreeCam.mc.player.input.movementSideways;
        double yaw = Math.toRadians(FreeCam.mc.player.getYaw());
        double motionX = 0.0;
        double motionZ = 0.0;
        if (forward != 0.0 || strafe != 0.0) {
            double angle = yaw + Math.atan2(-strafe, forward);
            motionX = -Math.sin(angle) * speed;
            motionZ = Math.cos(angle) * speed;
        }
        double motionY = 0.0;
        if (FreeCam.mc.options.jumpKey.isPressed()) {
            motionY = speed;
        } else if (FreeCam.mc.options.sneakKey.isPressed()) {
            motionY = -speed;
        }
        event.setMovePos(new Vec3d(motionX, motionY, motionZ));
    }

    @EventLink
    public void onEvent(EventMoveInput event) {
        if (FreeCam.mc.player == null) {
            return;
        }
        if (FreeCam.mc.player.getPose() == EntityPose.CROUCHING || FreeCam.mc.player.getPose() == EntityPose.SWIMMING) {
            event.setStrafe(event.getStrafe() * 5.0f);
        }
    }
}

