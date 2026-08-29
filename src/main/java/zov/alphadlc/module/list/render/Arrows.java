package zov.alphadlc.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import zov.alphadlc.event.list.EventHUD;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.ColorSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.friend.FriendRepository;
import zov.alphadlc.util.render.providers.ColorProvider;

import java.util.List;

@ModuleInformation(moduleName = "Arrows", moduleDesc = "HUD-стрелки, указывающие на игроков", moduleCategory = ModuleCategory.RENDER)
public class Arrows extends Module {

    private static final Identifier ICON = Identifier.of("mre", "images/triangle.png");

    private final SliderSetting radius = new SliderSetting("Радиус", 50, 30, 120, 1);
    private final SliderSetting sizeSetting = new SliderSetting("Размер", 32, 16, 64, 1);
    private final ColorSetting color = new ColorSetting("Цвет", 0xFF9682FF);

    @Subscribe
    private void onHud(EventHUD e) {
        if (mc.world == null || mc.player == null) return;
        if (mc.options.hudHidden) return;
        if (!mc.options.getPerspective().equals(Perspective.FIRST_PERSON)) return;

        List<AbstractClientPlayerEntity> players = mc.world.getPlayers();
        if (players.isEmpty()) return;

        DrawContext ctx = e.getDrawContext();
        MatrixStack matrix = ctx.getMatrices();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        float cameraYaw = mc.gameRenderer.getCamera().getYaw();

        float middleW = mc.getWindow().getScaledWidth() / 2f;
        float middleH = mc.getWindow().getScaledHeight() / 2f;
        float posY = middleH - radius.getFloatValue();
        float size = sizeSetting.getFloatValue();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderTexture(0, ICON);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        // Обход без stream-аллокаций.
        for (int i = 0; i < players.size(); i++) {
            AbstractClientPlayerEntity player = players.get(i);
            if (player == mc.player || !player.isAlive() || player.isRemoved() || player.isSpectator() || player.isInvisible()) continue;

            int col = FriendRepository.isFriend(player.getNameForScoreboard())
                    ? 0xFF00FF00  // Зелёный цвет для друзей
                    : color.getValue();
            int baseCol = ColorProvider.setAlpha(col, ColorProvider.alpha(col) / 2);

            // Вычисляем угол к игроку
            float yaw = getRotation(player, tickDelta) - cameraYaw;

            matrix.push();
            matrix.translate(middleW, middleH, 0.0F);
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
            matrix.translate(-middleW, -middleH, 0.0F);
            Matrix4f m = matrix.peek().getPositionMatrix();

            buffer.vertex(m, middleW - size / 2f, posY + size, 0).texture(0f, 1f).color(baseCol);
            buffer.vertex(m, middleW + size / 2f, posY + size, 0).texture(1f, 1f).color(baseCol);
            buffer.vertex(m, middleW + size / 2f, posY, 0).texture(1f, 0f).color(col);
            buffer.vertex(m, middleW - size / 2f, posY, 0).texture(0f, 0f).color(col);

            matrix.pop();
        }

        BuiltBuffer builtBuffer = buffer.endNullable();
        if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private float getRotation(AbstractClientPlayerEntity entity, float tickDelta) {
        Vec3d diff = entity.getLerpedPos(tickDelta).subtract(mc.player.getLerpedPos(tickDelta));
        return (float) -(Math.atan2(diff.x, diff.z) * (180.0 / Math.PI));
    }
}
