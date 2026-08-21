package zov.alphadlc.util.render.esp;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zov.alphadlc.mixin.LivingEntityRendererAccessor;
import zov.alphadlc.module.list.render.GlowEsp;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.providers.ResourceProvider;

import java.util.ArrayList;
import java.util.List;

public class GlowEspRenderer {

    private static GlowEspRenderer instance;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final ShaderProgramKey KEY_KAWASE_DOWN  = key("glass_hands_kawase_down");
    private static final ShaderProgramKey KEY_KAWASE_UP    = key("glass_hands_kawase_up");
    private static final ShaderProgramKey KEY_FILL_GLOW    = key("glass_hands_fill_glow");
    private static final ShaderProgramKey KEY_FILL_OUTLINE = key("glass_hands_fill_outline");

    private static ShaderProgramKey key(String name) {
        return new ShaderProgramKey(ResourceProvider.getShaderIdentifier(name),
                VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
    }

    // maskBuffer: white silhouettes of players (used as bloom source + mask)
    private Framebuffer maskBuffer;
    private final List<Framebuffer> bloomBuffers = new ArrayList<>();

    private int width = -1, height = -1;
    private boolean hasMask = false;

    public static GlowEspRenderer getInstance() {
        if (instance == null) instance = new GlowEspRenderer();
        return instance;
    }

    /**
     * Call from EventWorldRender — renders white player silhouettes into maskBuffer.
     * Uses the same EventWorldRender MatrixStack so 3D positions are correct.
     */
    public void renderMask(GlowEsp module, MatrixStack worldMatrices, float tickDelta) {
        if (mc.world == null || mc.player == null) return;
        ensureBuffers();
        if (maskBuffer == null) return;

        // Clear mask buffer
        maskBuffer.setClearColor(0f, 0f, 0f, 0f);
        maskBuffer.clear();
        maskBuffer.beginWrite(false);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!shouldRender(player)) continue;
            renderSilhouette(worldMatrices, player, tickDelta);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        mc.getFramebuffer().beginWrite(true);
        hasMask = true;
    }

    /**
     * Call from GlowEsp.onHUD — runs bloom on mask and composites glow onto screen.
     */
    public void compositeIfReady(GlowEsp module) {
        if (!hasMask) return;
        hasMask = false;

        int iter = (int) module.glowRadius.getFloatValue();
        ensureBloomBuffers(iter);

        ShaderProgram downShader = mc.getShaderLoader().getOrCreateProgram(KEY_KAWASE_DOWN);
        ShaderProgram upShader   = mc.getShaderLoader().getOrCreateProgram(KEY_KAWASE_UP);
        if (downShader == null || upShader == null) return;

        // Kawase downsample
        int cur = maskBuffer.getColorAttachment();
        for (int i = 0; i < iter; i++) {
            Framebuffer b = bloomBuffers.get(i);
            b.setClearColor(0f, 0f, 0f, 0f); b.clear();
            b.beginWrite(true);
            RenderSystem.setShader(KEY_KAWASE_DOWN);
            RenderSystem.setShaderTexture(0, cur);
            kawaseUniforms(downShader, b.textureWidth, b.textureHeight, 1f + i);
            drawQuad();
            cur = b.getColorAttachment();
        }

        // Kawase upsample
        for (int i = iter - 1; i >= 1; i--) {
            Framebuffer b = bloomBuffers.get(i - 1);
            b.beginWrite(true);
            RenderSystem.setShader(KEY_KAWASE_UP);
            RenderSystem.setShaderTexture(0, cur);
            kawaseUniforms(upShader, b.textureWidth, b.textureHeight, 1f + i);
            setUniform(upShader, "color", 1f, 1f, 1f);
            drawQuad();
            cur = b.getColorAttachment();
        }

        // Composite onto main framebuffer
        mc.getFramebuffer().beginWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.disableDepthTest();

        ShaderProgram glowShader = mc.getShaderLoader().getOrCreateProgram(KEY_FILL_GLOW);
        if (glowShader != null) {
            boolean isAuto = module.autoColor.getValue();
            int gc1, gc2;
            if (!isAuto && module.rainbow.getValue()) {
                float speed = module.rainbowSpeed.getFloatValue();
                float hue = (System.currentTimeMillis() % (long)(10000.0 / speed)) / (10000f / speed);
                gc1 = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f) & 0xFFFFFF;
                gc2 = gc1; // same color so glow is uniform
            } else {
                gc1 = module.glowColor1.getValue();
                gc2 = module.glowColor2.getValue();
            }

            RenderSystem.setShader(KEY_FILL_GLOW);
            RenderSystem.setShaderTexture(0, cur);
            RenderSystem.setShaderTexture(1, maskBuffer.getColorAttachment());
            setUniform(glowShader, "glowColor1", rf(gc1), gf(gc1), bf(gc1));
            setUniform(glowShader, "glowColor2", rf(gc2), gf(gc2), bf(gc2));
            setUniform(glowShader, "exposure",   module.glowExposure.getFloatValue());
            setUniform(glowShader, "autoColor",  isAuto ? 1.0f : 0.0f);
            setUniform(glowShader, "saturation", module.saturation.getFloatValue());
            drawQuad();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        mc.getFramebuffer().beginWrite(true);

        if (module.outlineEnabled.getValue()) {
            renderOutline(module);
        }
    }

    public void invalidate() {
        hasMask = false;
    }

    private void renderOutline(GlowEsp module) {
        ShaderProgram outlineShader = mc.getShaderLoader().getOrCreateProgram(KEY_FILL_OUTLINE);
        if (outlineShader == null) return;

        boolean isAuto = module.autoColor.getValue();
        boolean isRainbow = !isAuto && module.rainbow.getValue();

        int oc;
        if (isRainbow) {
            float speed = module.rainbowSpeed.getFloatValue();
            float hue = (System.currentTimeMillis() % (long)(10000.0 / speed)) / (10000f / speed);
            oc = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f) | 0xFF000000;
        } else {
            oc = module.outlineColor.getValue();
        }
        java.awt.Color col = new java.awt.Color(oc, true);
        float a = col.getAlpha() / 255f;

        int fw = Math.max(1, mc.getWindow().getFramebufferWidth());
        int fh = Math.max(1, mc.getWindow().getFramebufferHeight());

        mc.getFramebuffer().beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(KEY_FILL_OUTLINE);
        RenderSystem.setShaderTexture(0, maskBuffer.getColorAttachment());
        if (isAuto && !bloomBuffers.isEmpty()) {
            RenderSystem.setShaderTexture(1, bloomBuffers.get(0).getColorAttachment());
        }

        setUniform(outlineShader, "colorMode", isAuto ? 2.0f : 0.0f);
        setUniform(outlineShader, "width",      module.outlineWidth.getFloatValue());
        setUniform(outlineShader, "texelSize",  1f / fw, 1f / fh);
        setUniform(outlineShader, "alpha",      (isAuto || isRainbow) ? 1.0f : a);
        setUniform(outlineShader, "saturation", module.saturation.getFloatValue());
        setUniform(outlineShader, "solidColor", col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f);
        drawQuad();

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        mc.getFramebuffer().beginWrite(true);
    }

    // ── player silhouette rendering ────────────────────────────────────────────

    private boolean shouldRender(PlayerEntity player) {
        if (player == null || !player.isAlive()) return false;
        if (player == mc.player) {
            GlowEsp module = GlowEsp.getInstance();
            if (module == null || !module.renderSelf.getValue()) return false;
            // hide self in first person
            if (mc.options.getPerspective().isFirstPerson()) return false;
        }
        return true;
    }

    private void renderSilhouette(MatrixStack matrices, PlayerEntity player, float tickDelta) {
        if (!(player instanceof AbstractClientPlayerEntity clientPlayer)) return;
        EntityRenderer rawRenderer = mc.getEntityRenderDispatcher().getRenderer((Entity) player);
        if (!(rawRenderer instanceof PlayerEntityRenderer renderer)) return;

        PlayerEntityRenderState state = renderer.createRenderState();
        renderer.updateRenderState(clientPlayer, state, tickDelta);

        PlayerEntityModel model = renderer.getModel();
        model.setAngles(state);

        matrices.push();

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Vec3d pos = player.getLerpedPos(tickDelta);
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);

        if (state.sleepingDirection != null) {
            float off = state.standingEyeHeight - 0.1f;
            matrices.translate(
                    (float)(-state.sleepingDirection.getOffsetX()) * off, 0f,
                    (float)(-state.sleepingDirection.getOffsetZ()) * off);
        }

        float scale = state.baseScale;
        matrices.scale(scale, scale, scale);

        LivingEntityRendererAccessor accessor = (LivingEntityRendererAccessor) renderer;
        accessor.alphadlc$setupTransforms(state, matrices, state.bodyYaw, scale);
        accessor.alphadlc$scale(state, matrices);

        matrices.scale(-1f, -1f, 1f);
        matrices.translate(0f, -1.501f, 0f);

        // Render with player's skin texture for autoColor support
        RenderSystem.setShaderTexture(0, clientPlayer.getSkinTextures().texture());
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        net.minecraft.client.model.ModelPart root = model.getRootPart();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        boolean slim = clientPlayer.getSkinTextures().model() == net.minecraft.client.util.SkinTextures.Model.SLIM;

        fillPartTextured(matrices, buf, root, model.head,     -4f, -8f, -4f, 8f, 8f,  8f,  8f, 8f, 16f, 16f);
        fillPartTextured(matrices, buf, root, model.body,     -4f,  0f, -2f, 8f, 12f, 4f,  20f, 20f, 28f, 32f);
        if (slim) {
            fillPartTextured(matrices, buf, root, model.rightArm, -2f, -2f, -2f, 3f, 12f, 4f,  44f, 16f, 47f, 28f);
            fillPartTextured(matrices, buf, root, model.leftArm,  -1f, -2f, -2f, 3f, 12f, 4f,  36f, 52f, 39f, 64f);
        } else {
            fillPartTextured(matrices, buf, root, model.rightArm, -3f, -2f, -2f, 4f, 12f, 4f,  44f, 16f, 48f, 28f);
            fillPartTextured(matrices, buf, root, model.leftArm,  -1f, -2f, -2f, 4f, 12f, 4f,  36f, 52f, 40f, 64f);
        }
        fillPartTextured(matrices, buf, root, model.rightLeg, -2f,  0f, -2f, 4f, 12f, 4f,  4f, 16f, 8f, 28f);
        fillPartTextured(matrices, buf, root, model.leftLeg,  -2f,  0f, -2f, 4f, 12f, 4f,  20f, 48f, 24f, 60f);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        matrices.pop();
    }

    /** Generate unique color per player based on UUID (for autoColor) */
    private int getPlayerColor(PlayerEntity player) {
        int hash = player.getUuid().hashCode();
        float hue = (hash & 0xFFFF) / 65535f;
        java.awt.Color color = java.awt.Color.getHSBColor(hue, 0.7f, 0.9f);
        return ColorProvider.rgba(color.getRed(), color.getGreen(), color.getBlue(), 255);
    }

    private void fillPartTextured(MatrixStack stack, BufferBuilder buf,
                                  net.minecraft.client.model.ModelPart root,
                                  net.minecraft.client.model.ModelPart part,
                                  float ox, float oy, float oz, float w, float h, float d,
                                  float u1, float v1, float u2, float v2) {
        stack.push();
        root.rotate(stack);
        part.rotate(stack);
        float s = 0.0625f; // 1/16 scale, no expand
        float x0 = ox*s, y0 = oy*s, z0 = oz*s;
        float x1 = (ox+w)*s, y1 = (oy+h)*s, z1 = (oz+d)*s;
        Matrix4f m = stack.peek().getPositionMatrix();

        // Texture coordinates normalized to 0-1
        float tu1 = u1 / 64f, tv1 = v1 / 64f;
        float tu2 = u2 / 64f, tv2 = v2 / 64f;

        // Render all 6 faces with texture coordinates
        quadTex(buf,m, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, tu1,tv1, tu2,tv2);
        quadTex(buf,m, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, tu1,tv1, tu2,tv2);
        quadTex(buf,m, x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0, tu1,tv1, tu2,tv2);
        quadTex(buf,m, x1,y0,z1, x1,y1,z1, x0,y1,z1, x0,y0,z1, tu1,tv1, tu2,tv2);
        quadTex(buf,m, x0,y0,z1, x0,y1,z1, x0,y1,z0, x0,y0,z0, tu1,tv1, tu2,tv2);
        quadTex(buf,m, x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1, tu1,tv1, tu2,tv2);
        stack.pop();
    }

    private void quadTex(BufferBuilder b, Matrix4f m,
                         float x1,float y1,float z1, float x2,float y2,float z2,
                         float x3,float y3,float z3, float x4,float y4,float z4,
                         float u1, float v1, float u2, float v2) {
        b.vertex(m,x1,y1,z1).texture(u1,v1).color(255,255,255,255);
        b.vertex(m,x2,y2,z2).texture(u1,v2).color(255,255,255,255);
        b.vertex(m,x3,y3,z3).texture(u2,v2).color(255,255,255,255);
        b.vertex(m,x4,y4,z4).texture(u2,v1).color(255,255,255,255);
    }

    private void fillPart(MatrixStack stack, BufferBuilder buf,
                          net.minecraft.client.model.ModelPart root,
                          net.minecraft.client.model.ModelPart part,
                          float ox, float oy, float oz, float w, float h, float d,
                          float expand, int color) {
        stack.push();
        root.rotate(stack);
        part.rotate(stack);
        float s = 0.0625f, e = expand * s;
        float x0 = ox*s-e, y0 = oy*s-e, z0 = oz*s-e;
        float x1 = (ox+w)*s+e, y1 = (oy+h)*s+e, z1 = (oz+d)*s+e;
        Matrix4f m = stack.peek().getPositionMatrix();
        int r = ColorProvider.red(color), g = ColorProvider.green(color),
                b = ColorProvider.blue(color), a = ColorProvider.alpha(color);
        quad(buf,m, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, r,g,b,a);
        quad(buf,m, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, r,g,b,a);
        quad(buf,m, x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0, r,g,b,a);
        quad(buf,m, x1,y0,z1, x1,y1,z1, x0,y1,z1, x0,y0,z1, r,g,b,a);
        quad(buf,m, x0,y0,z1, x0,y1,z1, x0,y1,z0, x0,y0,z0, r,g,b,a);
        quad(buf,m, x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1, r,g,b,a);
        stack.pop();
    }

    private void quad(BufferBuilder b, Matrix4f m,
                      float x1,float y1,float z1, float x2,float y2,float z2,
                      float x3,float y3,float z3, float x4,float y4,float z4,
                      int r,int g,int bl,int a) {
        b.vertex(m,x1,y1,z1).color(r,g,bl,a);
        b.vertex(m,x2,y2,z2).color(r,g,bl,a);
        b.vertex(m,x3,y3,z3).color(r,g,bl,a);
        b.vertex(m,x4,y4,z4).color(r,g,bl,a);
    }

    // ── framebuffer helpers ────────────────────────────────────────────────────

    private void ensureBuffers() {
        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();
        if (w == width && h == height && maskBuffer != null) return;
        if (maskBuffer != null) maskBuffer.delete();
        maskBuffer = new SimpleFramebuffer(w, h, true);
        setLinearFilter(maskBuffer);
        width = w; height = h;
        bloomBuffers.forEach(Framebuffer::delete);
        bloomBuffers.clear();
    }

    private void ensureBloomBuffers(int n) {
        int fw = mc.getWindow().getFramebufferWidth();
        int fh = mc.getWindow().getFramebufferHeight();
        if (bloomBuffers.size() != n) {
            bloomBuffers.forEach(Framebuffer::delete);
            bloomBuffers.clear();
            for (int i = 0; i < n; i++) {
                Framebuffer f = new SimpleFramebuffer(Math.max(2, fw>>(i+1)), Math.max(2, fh>>(i+1)), false);
                setLinearFilter(f); bloomBuffers.add(f);
            }
            return;
        }
        for (int i = 0; i < n; i++) {
            int w = Math.max(2, fw>>(i+1)), h = Math.max(2, fh>>(i+1));
            Framebuffer b = bloomBuffers.get(i);
            if (b.textureWidth != w || b.textureHeight != h) {
                b.delete(); b = new SimpleFramebuffer(w, h, false); setLinearFilter(b); bloomBuffers.set(i, b);
            }
        }
    }

    private void drawQuad() {
        float sw = Math.max(mc.getWindow().getScaledWidth(), 1);
        float sh = Math.max(mc.getWindow().getScaledHeight(), 1);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(0f, 0f, 0f).texture(0f,1f).color(1f,1f,1f,1f);
        b.vertex(0f, sh, 0f).texture(0f,0f).color(1f,1f,1f,1f);
        b.vertex(sw,sh, 0f).texture(1f,0f).color(1f,1f,1f,1f);
        b.vertex(sw, 0f, 0f).texture(1f,1f).color(1f,1f,1f,1f);
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    private void kawaseUniforms(ShaderProgram s, int w, int h, float off) {
        setUniform(s, "uSize",      (float)Math.max(1,w), (float)Math.max(1,h));
        setUniform(s, "uOffset",    off, off);
        setUniform(s, "uHalfPixel", 0.5f/Math.max(1,w), 0.5f/Math.max(1,h));
    }

    private void setLinearFilter(Framebuffer fb) {
        RenderSystem.bindTexture(fb.getColorAttachment());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.bindTexture(0);
    }

    private void setUniform(ShaderProgram s, String n, float v)                   { var u=s.getUniform(n); if(u!=null)u.set(v); }
    private void setUniform(ShaderProgram s, String n, float x, float y)          { var u=s.getUniform(n); if(u!=null)u.set(x,y); }
    private void setUniform(ShaderProgram s, String n, float x, float y, float z) { var u=s.getUniform(n); if(u!=null)u.set(x,y,z); }

    private float rf(int c) { return ((c>>16)&0xFF)/255f; }
    private float gf(int c) { return ((c>> 8)&0xFF)/255f; }
    private float bf(int c) { return  (c     &0xFF)/255f; }
}
