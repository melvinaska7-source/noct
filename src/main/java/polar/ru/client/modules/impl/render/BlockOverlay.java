package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.storages.implement.helpertstorages.Theme;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.ShaderUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.polar;

public class BlockOverlay
extends Module {
    public static BlockOverlay INSTANCE = new BlockOverlay();
    private final ModeSetting mode = new ModeSetting("Режим", "Шейдер", "Шейдер", "Нитки");
    private final ModeSetting shaderPreset = new ModeSetting("Пресет шейдера", "Cloud", "Cloud", "Balatro", "Plasma").visible(() -> this.mode.is("Шейдер"));
    private final FloatSetting waveSpeed = new FloatSetting("Скорость волн", 1.2f, 0.1f, 5.0f, 0.1f).visible(() -> this.mode.is("Шейдер"));
    private final FloatSetting waveScale = new FloatSetting("Частота волн", 1.0f, 1.0f, 3.0f, 0.1f).visible(() -> this.mode.is("Шейдер"));
    private final FloatSetting lineSpeed = new FloatSetting("Скорость нитей", 1.4f, 0.1f, 5.0f, 0.1f).visible(() -> this.mode.is("Нитки"));
    private final FloatSetting lineJitter = new FloatSetting("Изгиб нитей", 0.55f, 0.0f, 1.5f, 0.01f).visible(() -> this.mode.is("Нитки"));
    private final FloatSetting outline = new FloatSetting("Ширина обводки", 1.1f, 0.1f, 5.0f, 0.1f);
    private final FloatSetting glow = new FloatSetting("Сила свечения", 1.0f, 0.0f, 5.0f, 0.1f);
    private final FloatSetting fill = new FloatSetting("Заливка", 0.6f, 0.0f, 1.0f, 0.01f);
    private final FloatSetting alpha = new FloatSetting("Прозрачность", 1.0f, 0.0f, 1.0f, 0.01f);
    private final FloatSetting smooth = new FloatSetting("Плавность", 0.24f, 0.05f, 0.6f, 0.01f);
    private final BooleanSetting staticOutline = new BooleanSetting("Статичная обводка", true);
    private Framebuffer maskBuffer;
    private int fbWidth = -1;
    private int fbHeight = -1;
    private boolean hasMask;
    private BlockPos lastBlockPos;
    private Box displayBox;
    private Box targetBox;
    private int cachedThemeColor1 = -1;
    private int cachedThemeColor2 = -1;
    private float overlayAlpha;

    public BlockOverlay() {
        super("BlockOverlay", "Обводка блока", Module.ModuleCategory.RENDER);
        this.addSettings(this.mode, this.shaderPreset, this.waveSpeed, this.waveScale, this.lineSpeed, this.lineJitter, this.outline, this.glow, this.fill, this.alpha, this.smooth, this.staticOutline);
    }

    @Override
    public void onDisable() {
        this.hasMask = false;
        this.lastBlockPos = null;
        this.displayBox = null;
        this.targetBox = null;
        this.overlayAlpha = 0.0f;
        super.onDisable();
    }

    @EventLink(priority=-100)
    public void onRender3D(Event3DRender event) {
        if (mc == null || BlockOverlay.mc.world == null || BlockOverlay.mc.player == null) {
            return;
        }
        Box worldBox = this.getTargetedBlockBox();
        if (worldBox != null) {
            if (this.displayBox == null || this.targetBox == null || this.lastBlockPos == null) {
                this.displayBox = worldBox;
                this.targetBox = worldBox;
            } else {
                this.targetBox = worldBox;
                this.displayBox = this.lerpBox(this.displayBox, this.targetBox, this.smooth.get());
            }
            this.lastBlockPos = BlockPos.ofFloored((double)worldBox.minX, (double)worldBox.minY, (double)worldBox.minZ);
            this.overlayAlpha = this.lerpValue(this.overlayAlpha, 1.0f, Math.min(1.0f, this.smooth.get() * 1.35f));
        } else {
            if (this.displayBox == null || this.targetBox == null) {
                this.hasMask = false;
                this.lastBlockPos = null;
                this.overlayAlpha = 0.0f;
                return;
            }
            this.overlayAlpha = this.lerpValue(this.overlayAlpha, 0.0f, 0.18f);
            if (this.overlayAlpha <= 0.02f) {
                this.hasMask = false;
                this.lastBlockPos = null;
                this.displayBox = null;
                this.targetBox = null;
                this.overlayAlpha = 0.0f;
                return;
            }
        }
        this.updateCachedThemeColors();
        Vec3d cam = event.getCamera().getPos();
        Box localBox = this.displayBox.offset(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = event.getMatrices().peek().getPositionMatrix();
        if (this.mode.getIndex() == 1) {
            this.hasMask = false;
            this.drawAnimatedWeb(matrix, localBox);
            return;
        }
        this.ensureMaskBuffer();
        if (this.maskBuffer == null) {
            return;
        }
        this.hasMask = this.overlayAlpha > 0.02f;
        this.maskBuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        this.maskBuffer.clear();
        this.copyMainDepthToMask();
        this.maskBuffer.beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        this.drawMaskBox(matrix, localBox);
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        mc.getFramebuffer().beginWrite(false);
    }

    @EventLink(priority=200)
    public void onRender2D(EventRender.Default event) {
        if (!this.hasMask || this.maskBuffer == null || this.overlayAlpha <= 0.02f) {
            return;
        }
        if (this.mode.is("Нитки")) {
            return;
        }
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(this.getBlockOverlayShaderKey());
        if (shader == null) {
            return;
        }
        boolean lineMode = this.mode.is("Нитки");
        int color1 = this.cachedThemeColor1;
        int color2 = this.cachedThemeColor2;
        mc.getFramebuffer().beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader((ShaderProgramKey)this.getBlockOverlayShaderKey());
        RenderSystem.setShaderTexture((int)0, (int)this.maskBuffer.getColorAttachment());
        this.setUniform(shader, "texelSize", 1.0f / (float)Math.max(1, mc.getWindow().getFramebufferWidth()), 1.0f / (float)Math.max(1, mc.getWindow().getFramebufferHeight()));
        this.setUniform(shader, "color", ColorUtils.redf(color1), ColorUtils.greenf(color1), ColorUtils.bluef(color1));
        this.setUniform(shader, "color2", ColorUtils.redf(color2), ColorUtils.greenf(color2), ColorUtils.bluef(color2));
        this.setUniform(shader, "time", (float)(System.currentTimeMillis() % 100000L) / 1000.0f);
        this.setUniform(shader, "speed", this.waveSpeed.get());
        this.setUniform(shader, "scale", this.waveScale.get());
        this.setUniform(shader, "outline", this.outline.get());
        this.setUniform(shader, "glow", lineMode ? 0.0f : this.glow.get());
        this.setUniform(shader, "fill", lineMode ? 0.0f : this.fill.get());
        this.setUniform(shader, "alpha", (lineMode ? 1.0f : this.alpha.get()) * this.overlayAlpha);
        this.setUniform(shader, "outlineOnly", lineMode ? 1.0f : 0.0f);
        this.drawFullscreenQuad();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture((int)0, (int)0);
    }

    private void setUniform(ShaderProgram shader, String name, float value) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private void setUniform(ShaderProgram shader, String name, float x2, float y2) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x2, y2);
        }
    }

    private void setUniform(ShaderProgram shader, String name, float x2, float y2, float z2) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x2, y2, z2);
        }
    }

    private ShaderProgramKey getBlockOverlayShaderKey() {
        return switch (this.shaderPreset.getCurrent()) {
            case "Balatro" -> ShaderUtils.blockOverlayBalatro;
            case "Plasma" -> ShaderUtils.blockOverlayPlasma;
            default -> ShaderUtils.blockOverlay;
        };
    }

    private void ensureMaskBuffer() {
        int w2 = mc.getWindow().getFramebufferWidth();
        int h2 = mc.getWindow().getFramebufferHeight();
        if (this.maskBuffer == null || this.fbWidth != w2 || this.fbHeight != h2) {
            if (this.maskBuffer != null) {
                this.maskBuffer.delete();
            }
            this.maskBuffer = new SimpleFramebuffer(w2, h2, true);
            this.fbWidth = w2;
            this.fbHeight = h2;
        }
    }

    private Box getTargetedBlockBox() {
        BlockHitResult blockHit;
        block7: {
            block6: {
                HitResult hit = this.getOutlineHitResult();
                if (!(hit instanceof BlockHitResult)) break block6;
                blockHit = (BlockHitResult)hit;
                if (hit.getType() == HitResult.Type.BLOCK) break block7;
            }
            return null;
        }
        BlockPos pos = blockHit.getBlockPos();
        if (pos == null) {
            return null;
        }
        if (BlockOverlay.mc.world.getBlockState(pos).isAir()) {
            return null;
        }
        VoxelShape shape = BlockOverlay.mc.world.getBlockState(pos).getOutlineShape((BlockView)BlockOverlay.mc.world, pos);
        if (shape == null || shape.isEmpty()) {
            return null;
        }
        return shape.getBoundingBox().offset(pos).expand(0.002);
    }

    private HitResult getOutlineHitResult() {
        double reach;
        Vec3d direction;
        Vec3d end;
        if (BlockOverlay.mc.player == null || BlockOverlay.mc.world == null) {
            return BlockOverlay.mc.crosshairTarget;
        }
        Vec3d start = BlockOverlay.mc.player.getCameraPosVec(1.0f);
        BlockHitResult outlineHit = BlockOverlay.mc.world.raycast(new RaycastContext(start, end = start.add((direction = BlockOverlay.mc.player.getRotationVec(1.0f)).multiply(reach = 6.0)), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, (Entity)BlockOverlay.mc.player));
        return outlineHit.getType() == HitResult.Type.BLOCK ? outlineHit : BlockOverlay.mc.crosshairTarget;
    }

    private Box lerpBox(Box a2, Box b2, float t2) {
        return new Box(a2.minX + (b2.minX - a2.minX) * (double)t2, a2.minY + (b2.minY - a2.minY) * (double)t2, a2.minZ + (b2.minZ - a2.minZ) * (double)t2, a2.maxX + (b2.maxX - a2.maxX) * (double)t2, a2.maxY + (b2.maxY - a2.maxY) * (double)t2, a2.maxZ + (b2.maxZ - a2.maxZ) * (double)t2);
    }

    private float lerpValue(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    private void drawAnimatedWeb(Matrix4f matrix, Box box) {
        int strandsPerFace = 5;
        int samples = 18;
        float t2 = (float)(System.currentTimeMillis() % 100000L) / 1000.0f * this.lineSpeed.get();
        float lineWidth = 0.0025f;
        float bendBase = 0.06f + this.lineJitter.get() * 0.2f;
        int baseAlpha = Math.max(0, Math.min(255, (int)(this.alpha.get() * this.overlayAlpha * 210.0f)));
        int themeColor = this.cachedThemeColor1;
        long seed = this.lastBlockPos != null ? this.lastBlockPos.asLong() : 1L;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        this.drawFilledBox(matrix, box, ColorUtils.setAlphaColor(themeColor, (int)(this.alpha.get() * this.overlayAlpha * this.fill.get() * 170.0f)));
        for (int face = 0; face < 6; ++face) {
            int[] neighbors = this.faceNeighbors(face);
            for (int strand = 0; strand < strandsPerFace; ++strand) {
                int key = face * 1000 + strand * 53;
                int adj = neighbors[strand % neighbors.length];
                double phase = (double)t2 * (0.95 + this.rand01(seed, key + 1) * 0.55) + (double)strand * 0.83 + (double)face * 1.11;
                double edgeT = this.clamp01(0.5 + Math.sin(phase * 1.37 + this.rand01(seed, key + 2) * 6.2831853) * 0.38);
                Vec3d pivot = this.edgePoint(box, face, adj, edgeT, 0.0015);
                Vec3d start = this.facePoint(box, face, this.clamp01(0.5 + (this.rand01(seed, key + 3) - 0.5) * 0.46), this.clamp01(0.5 + (this.rand01(seed, key + 4) - 0.5) * 0.46), 0.0015);
                Vec3d end = this.facePoint(box, adj, this.clamp01(0.5 + (this.rand01(seed, key + 5) - 0.5) * 0.46), this.clamp01(0.5 + (this.rand01(seed, key + 6) - 0.5) * 0.46), 0.0015);
                Vec3d[] basisA = this.faceBasis(face);
                Vec3d[] basisB = this.faceBasis(adj);
                Vec3d normalA = this.faceNormal(face);
                Vec3d normalB = this.faceNormal(adj);
                double bendA = (double)bendBase * (0.7 + this.rand01(seed, key + 7)) * Math.sin(phase * 1.9 + this.rand01(seed, key + 8) * 6.2831853);
                double bendB = (double)bendBase * (0.7 + this.rand01(seed, key + 9)) * Math.cos(phase * 1.7 + this.rand01(seed, key + 10) * 6.2831853);
                Vec3d dirA = pivot.subtract(start);
                Vec3d c1a = start.add(dirA.multiply(0.38)).add(basisA[0].multiply(bendA)).add(basisA[1].multiply(-bendA * 0.55));
                Vec3d c2a = start.add(dirA.multiply(0.76)).add(basisA[0].multiply(-bendA * 0.65)).add(basisA[1].multiply(bendA * 0.4));
                Vec3d dirB = end.subtract(pivot);
                Vec3d c1b = pivot.add(dirB.multiply(0.24)).add(basisB[0].multiply(bendB)).add(basisB[1].multiply(bendB * 0.45));
                Vec3d c2b = pivot.add(dirB.multiply(0.62)).add(basisB[0].multiply(-bendB * 0.7)).add(basisB[1].multiply(-bendB * 0.35));
                int alphaLine = Math.max(18, Math.min(255, (int)((double)baseAlpha * (0.74 + 0.26 * Math.sin(phase * 2.6)))));
                int color = ColorUtils.setAlphaColor(themeColor, alphaLine);
                this.drawBezierRibbon(matrix, start, c1a, c2a, pivot, normalA, samples, color, lineWidth);
                this.drawBezierRibbon(matrix, pivot, c1b, c2b, end, normalB, samples, color, lineWidth);
            }
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void copyMainDepthToMask() {
        if (this.maskBuffer == null) {
            return;
        }
        int readFbo = GL11.glGetInteger((int)36010);
        int drawFbo = GL11.glGetInteger((int)36006);
        int w2 = mc.getWindow().getFramebufferWidth();
        int h2 = mc.getWindow().getFramebufferHeight();
        GL30.glBindFramebuffer((int)36008, (int)BlockOverlay.mc.getFramebuffer().fbo);
        GL30.glBindFramebuffer((int)36009, (int)this.maskBuffer.fbo);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)w2, (int)h2, (int)0, (int)0, (int)w2, (int)h2, (int)256, (int)9728);
        GL30.glBindFramebuffer((int)36008, (int)readFbo);
        GL30.glBindFramebuffer((int)36009, (int)drawFbo);
    }

    private Vec3d cubicBezier(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t2) {
        double it = 1.0 - (double)t2;
        double it2 = it * it;
        double t22 = t2 * t2;
        return p0.multiply(it2 * it).add(p1.multiply(3.0 * it2 * (double)t2)).add(p2.multiply(3.0 * it * t22)).add(p3.multiply(t22 * (double)t2));
    }

    private void drawBezierRibbon(Matrix4f matrix, Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, Vec3d faceNormal, int samples, int color, float halfWidth) {
        Vec3d[] points = new Vec3d[samples + 1];
        for (int s2 = 0; s2 <= samples; ++s2) {
            float u2 = (float)s2 / (float)samples;
            points[s2] = this.cubicBezier(p0, p1, p2, p3, u2);
        }
        BufferBuilder quads = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i2 = 0; i2 < samples; ++i2) {
            Vec3d b2 = points[i2 + 1];
            Vec3d a2 = points[i2];
            Vec3d dir = b2.subtract(a2);
            if (dir.lengthSquared() < 1.0E-6) continue;
            Vec3d perp = faceNormal.crossProduct(dir).normalize().multiply((double)halfWidth);
            Vec3d aL = a2.add(perp);
            Vec3d aR = a2.subtract(perp);
            Vec3d bL = b2.add(perp);
            Vec3d bR = b2.subtract(perp);
            quads.vertex(matrix, (float)aL.x, (float)aL.y, (float)aL.z).color(color);
            quads.vertex(matrix, (float)aR.x, (float)aR.y, (float)aR.z).color(color);
            quads.vertex(matrix, (float)bR.x, (float)bR.y, (float)bR.z).color(color);
            quads.vertex(matrix, (float)bL.x, (float)bL.y, (float)bL.z).color(color);
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)quads.end());
    }

    private void updateCachedThemeColors() {
        if (polar.INSTANCE == null || polar.INSTANCE.themeStorage == null || polar.INSTANCE.themeStorage.getThemes() == null) {
            this.cachedThemeColor1 = ColorUtils.getThemeColor(0);
            this.cachedThemeColor2 = ColorUtils.getThemeColor(180);
            return;
        }
        Theme theme = polar.INSTANCE.themeStorage.getThemes().getTheme();
        if (theme == null) {
            this.cachedThemeColor1 = ColorUtils.getThemeColor(0);
            this.cachedThemeColor2 = ColorUtils.getThemeColor(180);
            return;
        }
        if (!"Rainbow".equals(theme.getName())) {
            int base;
            this.cachedThemeColor1 = base = theme.color != null && theme.color.length > 0 ? theme.color[0] : ColorUtils.getThemeColor(0);
            this.cachedThemeColor2 = base;
        } else {
            this.cachedThemeColor1 = ColorUtils.getThemeColor();
            this.cachedThemeColor2 = ColorUtils.getThemeColor(180);
        }
    }

    private int[] faceNeighbors(int face) {
        int[] nArray;
        switch (face) {
            case 0: 
            case 1: {
                int[] nArray2 = new int[4];
                nArray2[0] = 2;
                nArray2[1] = 3;
                nArray2[2] = 4;
                nArray = nArray2;
                nArray2[3] = 5;
                break;
            }
            case 2: 
            case 3: {
                int[] nArray3 = new int[4];
                nArray3[0] = 0;
                nArray3[1] = 1;
                nArray3[2] = 4;
                nArray = nArray3;
                nArray3[3] = 5;
                break;
            }
            default: {
                int[] nArray4 = new int[4];
                nArray4[0] = 0;
                nArray4[1] = 1;
                nArray4[2] = 2;
                nArray = nArray4;
                nArray4[3] = 3;
            }
        }
        return nArray;
    }

    private Vec3d[] faceBasis(int face) {
        Vec3d[] class_243Array;
        switch (face) {
            case 0: 
            case 1: {
                Vec3d[] class_243Array2 = new Vec3d[2];
                class_243Array2[0] = new Vec3d(1.0, 0.0, 0.0);
                class_243Array = class_243Array2;
                class_243Array2[1] = new Vec3d(0.0, 0.0, 1.0);
                break;
            }
            case 2: 
            case 3: {
                Vec3d[] class_243Array3 = new Vec3d[2];
                class_243Array3[0] = new Vec3d(1.0, 0.0, 0.0);
                class_243Array = class_243Array3;
                class_243Array3[1] = new Vec3d(0.0, 1.0, 0.0);
                break;
            }
            default: {
                Vec3d[] class_243Array4 = new Vec3d[2];
                class_243Array4[0] = new Vec3d(0.0, 0.0, 1.0);
                class_243Array = class_243Array4;
                class_243Array4[1] = new Vec3d(0.0, 1.0, 0.0);
            }
        }
        return class_243Array;
    }

    private Vec3d faceNormal(int face) {
        return switch (face) {
            case 0 -> new Vec3d(0.0, 1.0, 0.0);
            case 1 -> new Vec3d(0.0, -1.0, 0.0);
            case 2 -> new Vec3d(0.0, 0.0, -1.0);
            case 3 -> new Vec3d(0.0, 0.0, 1.0);
            case 4 -> new Vec3d(-1.0, 0.0, 0.0);
            default -> new Vec3d(1.0, 0.0, 0.0);
        };
    }

    private Vec3d edgePoint(Box box, int faceA, int faceB, double t2, double inset) {
        double[] fixedB;
        double x2 = Double.NaN;
        double y2 = Double.NaN;
        double z2 = Double.NaN;
        double[] fixedA = this.faceFixedCoords(box, faceA, inset);
        if (!Double.isNaN(fixedA[0])) {
            x2 = fixedA[0];
        }
        if (!Double.isNaN(fixedA[1])) {
            y2 = fixedA[1];
        }
        if (!Double.isNaN(fixedA[2])) {
            z2 = fixedA[2];
        }
        if (!Double.isNaN((fixedB = this.faceFixedCoords(box, faceB, inset))[0])) {
            x2 = fixedB[0];
        }
        if (!Double.isNaN(fixedB[1])) {
            y2 = fixedB[1];
        }
        if (!Double.isNaN(fixedB[2])) {
            z2 = fixedB[2];
        }
        double tt = this.clamp01(t2);
        if (Double.isNaN(x2)) {
            x2 = this.lerp(box.minX, box.maxX, tt);
        }
        if (Double.isNaN(y2)) {
            y2 = this.lerp(box.minY, box.maxY, tt);
        }
        if (Double.isNaN(z2)) {
            z2 = this.lerp(box.minZ, box.maxZ, tt);
        }
        return new Vec3d(x2, y2, z2);
    }

    private double[] faceFixedCoords(Box box, int face, double inset) {
        double[] dArray;
        switch (face) {
            case 0: {
                double[] dArray2 = new double[3];
                dArray2[0] = Double.NaN;
                dArray2[1] = box.maxY - inset;
                dArray = dArray2;
                dArray2[2] = Double.NaN;
                break;
            }
            case 1: {
                double[] dArray3 = new double[3];
                dArray3[0] = Double.NaN;
                dArray3[1] = box.minY + inset;
                dArray = dArray3;
                dArray3[2] = Double.NaN;
                break;
            }
            case 2: {
                double[] dArray4 = new double[3];
                dArray4[0] = Double.NaN;
                dArray4[1] = Double.NaN;
                dArray = dArray4;
                dArray4[2] = box.minZ + inset;
                break;
            }
            case 3: {
                double[] dArray5 = new double[3];
                dArray5[0] = Double.NaN;
                dArray5[1] = Double.NaN;
                dArray = dArray5;
                dArray5[2] = box.maxZ - inset;
                break;
            }
            case 4: {
                double[] dArray6 = new double[3];
                dArray6[0] = box.minX + inset;
                dArray6[1] = Double.NaN;
                dArray = dArray6;
                dArray6[2] = Double.NaN;
                break;
            }
            default: {
                double[] dArray7 = new double[3];
                dArray7[0] = box.maxX - inset;
                dArray7[1] = Double.NaN;
                dArray = dArray7;
                dArray7[2] = Double.NaN;
            }
        }
        return dArray;
    }

    private Vec3d facePoint(Box box, int face, double u2, double v2, double inset) {
        u2 = this.clamp01(u2);
        v2 = this.clamp01(v2);
        return switch (face) {
            case 0 -> new Vec3d(this.lerp(box.minX, box.maxX, u2), box.maxY - inset, this.lerp(box.minZ, box.maxZ, v2));
            case 1 -> new Vec3d(this.lerp(box.minX, box.maxX, u2), box.minY + inset, this.lerp(box.minZ, box.maxZ, v2));
            case 2 -> new Vec3d(this.lerp(box.minX, box.maxX, u2), this.lerp(box.minY, box.maxY, v2), box.minZ + inset);
            case 3 -> new Vec3d(this.lerp(box.minX, box.maxX, u2), this.lerp(box.minY, box.maxY, v2), box.maxZ - inset);
            case 4 -> new Vec3d(box.minX + inset, this.lerp(box.minY, box.maxY, v2), this.lerp(box.minZ, box.maxZ, u2));
            default -> new Vec3d(box.maxX - inset, this.lerp(box.minY, box.maxY, v2), this.lerp(box.minZ, box.maxZ, u2));
        };
    }

    private double rand01(long seed, int salt) {
        long x2 = seed + -7046029254386353131L * ((long)salt + 1L);
        x2 ^= x2 >>> 30;
        x2 *= -4658895280553007687L;
        x2 ^= x2 >>> 27;
        x2 *= -7723592293110705685L;
        x2 ^= x2 >>> 31;
        return (double)(x2 & 0xFFFFFFL) / 1.6777216E7;
    }

    private double lerp(double a2, double b2, double t2) {
        return a2 + (b2 - a2) * t2;
    }

    private double clamp01(double v2) {
        return Math.max(0.0, Math.min(1.0, v2));
    }

    private void drawFilledBox(Matrix4f matrix, Box box, int color) {
        BufferBuilder b2 = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(color);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(color);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)b2.end());
    }

    private void drawMaskBox(Matrix4f matrix, Box box) {
        BufferBuilder b2 = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int white = -1;
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(white);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(white);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(white);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(white);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(white);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(white);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(white);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(white);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(white);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(white);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(white);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(white);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(white);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(white);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(white);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(white);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(white);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(white);
        b2.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(white);
        b2.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(white);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(white);
        b2.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(white);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(white);
        b2.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(white);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)b2.end());
    }

    private void drawFullscreenQuad() {
        BufferBuilder b2 = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float width = Math.max(mc.getWindow().getScaledWidth(), 1);
        float height = Math.max(mc.getWindow().getScaledHeight(), 1);
        b2.vertex(0.0f, 0.0f, 0.0f).texture(0.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        b2.vertex(0.0f, height, 0.0f).texture(0.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        b2.vertex(width, height, 0.0f).texture(1.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        b2.vertex(width, 0.0f, 0.0f).texture(1.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)b2.end());
    }
}

