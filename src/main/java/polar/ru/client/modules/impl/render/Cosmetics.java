package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.storages.implement.helpertstorages.Theme;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.polar;

public class Cosmetics
extends Module {
    public static Cosmetics INSTANCE = new Cosmetics();
    private static final float PI_STEP = 0.06981317f;
    private static final float WING_SCALE = 1.0f;
    private static final float FLAP_SPEED = 1.6f;
    private static final float FLAP_AMPLITUDE = 25.0f;
    private static final int NIMBUS_ARMS = 2;
    private static final int NIMBUS_SEGMENTS = 17;
    private static final float NIMBUS_RADIUS = 0.45f;
    private static final float NIMBUS_BASE_SIZE = 0.23f;
    private static final double NIMBUS_STEP_RADIANS = 0.11;
    private static final int NIMBUS_MAX_ALPHA = 255;
    private static final int NIMBUS_ALPHA_FALLOFF = 9;
    private static final float NIMBUS_SPEED = 170.0f;
    private static final float CLASSIC_WING_DEFAULT_SPREAD = 8.0f;
    private static final int CLASSIC_WING_DEFAULT_ALPHA = 220;
    private static final ClassicWingPoint[] CLASSIC_WING_SHAPE = new ClassicWingPoint[]{new ClassicWingPoint(0.08f, 0.1f, 0.88f), new ClassicWingPoint(0.28f, 0.34f, 0.78f), new ClassicWingPoint(0.56f, 0.82f, 0.62f), new ClassicWingPoint(0.86f, 0.3f, 0.52f), new ClassicWingPoint(1.14f, 0.46f, 0.4f), new ClassicWingPoint(1.24f, 0.04f, 0.3f), new ClassicWingPoint(1.02f, -0.18f, 0.28f), new ClassicWingPoint(1.18f, -0.64f, 0.22f), new ClassicWingPoint(0.86f, -0.46f, 0.2f), new ClassicWingPoint(0.8f, -0.98f, 0.14f), new ClassicWingPoint(0.54f, -0.74f, 0.16f), new ClassicWingPoint(0.3f, -1.16f, 0.12f), new ClassicWingPoint(0.1f, -0.54f, 0.18f)};
    private final ListSetting cosmetics = new ListSetting("Косметика", new BooleanSetting("Нимб", true), new BooleanSetting("Крылья", true), new BooleanSetting("Крылья 2", false), new BooleanSetting("Китайская шляпа", true));
    private final BooleanSetting butterflyWingAnimation = new BooleanSetting("Анимация крыльев", true).visible(() -> this.cosmetics.is("Крылья"));
    private final FloatSetting butterflyWingSize = new FloatSetting("Размер", 1.0f, 0.65f, 1.8f, 0.05f).visible(() -> this.cosmetics.is("Крылья"));
    private final BooleanSetting classicWingAnimation = new BooleanSetting("Анимация крыльев", true).visible(() -> this.cosmetics.is("Крылья 2"));
    private final FloatSetting classicWingSize = new FloatSetting("Размер", 1.0f, 0.65f, 1.8f, 0.05f).visible(() -> this.cosmetics.is("Крылья 2"));
    private float selfClassicBodyYaw;
    private boolean selfClassicBodyYawInitialized;
    private boolean lastButterflySelected;
    private boolean lastClassicSelected;

    public Cosmetics() {
        super("Cosmetics", "Визуальные украшения", Module.ModuleCategory.RENDER);
        this.addSettings(this.cosmetics, this.butterflyWingAnimation, this.butterflyWingSize, this.classicWingAnimation, this.classicWingSize);
    }

    @Override
    public void onDisable() {
        this.selfClassicBodyYawInitialized = false;
        this.lastButterflySelected = false;
        this.lastClassicSelected = false;
        super.onDisable();
    }

    @EventLink
    public void onRender3D(Event3DRender event) {
        if (Cosmetics.mc.player == null || Cosmetics.mc.world == null) {
            return;
        }
        this.syncWingSelectionState();
        if (this.cosmetics.is("Нимб")) {
            this.renderNimbus(event);
        }
        boolean renderButterfly = this.cosmetics.is("Крылья");
        boolean renderClassic = this.cosmetics.is("Крылья 2");
        if (!renderButterfly && !renderClassic) {
            return;
        }
        float tickDelta = event.getTickDelta();
        MatrixStack matrices = event.getMatrices();
        Vec3d cameraPos = event.getCamera().getPos();
        for (PlayerEntity player : Cosmetics.mc.world.getPlayers()) {
            if (!this.shouldRenderCosmeticForPlayer(player) || player == Cosmetics.mc.player && Cosmetics.mc.options.getPerspective().isFirstPerson()) continue;
            if (renderButterfly) {
                this.renderButterflyWings(player, tickDelta, matrices, cameraPos);
            }
            if (!renderClassic) continue;
            this.renderClassicWings(player, tickDelta, matrices, cameraPos);
        }
    }

    private void renderButterflyWings(PlayerEntity player, float tickDelta, MatrixStack matrices, Vec3d cameraPos) {
        boolean horizontalPose;
        if (player.isGliding() || player.getPose() == EntityPose.SWIMMING || player.isInSwimmingPose()) {
            return;
        }
        Vec3d velocity = player.getVelocity();
        float bodyYaw = MathHelper.lerp((float)tickDelta, (float)player.prevBodyYaw, (float)player.bodyYaw);
        float yawRad = bodyYaw * ((float)Math.PI / 180);
        Vec3d forward = new Vec3d((double)(-MathHelper.sin((float)yawRad)), 0.0, (double)MathHelper.cos((float)yawRad));
        Vec3d sideways = new Vec3d(forward.z, 0.0, -forward.x);
        float forwardMove = (float)(velocity.x * forward.x + velocity.z * forward.z);
        float strafeMove = (float)(velocity.x * sideways.x + velocity.z * sideways.z);
        float verticalMove = (float)velocity.y;
        boolean animated = this.butterflyWingAnimation.isState();
        float smoothLean = animated ? MathHelper.clamp((float)(-forwardMove * 140.0f - verticalMove * 48.0f), (float)-24.0f, (float)26.0f) : 0.0f;
        float smoothStrafe = animated ? MathHelper.clamp((float)(strafeMove * 90.0f), (float)-10.0f, (float)10.0f) : 0.0f;
        float wingSpring = animated ? MathHelper.clamp((float)(Math.abs(forwardMove) * 0.95f + Math.abs(strafeMove) * 0.65f + Math.abs(verticalMove) * 0.75f), (float)0.0f, (float)1.7f) : 0.0f;
        float anim = ((float)player.age + tickDelta) * 0.22f * 1.6f + wingSpring * 0.4f;
        float sin = animated ? MathHelper.sin((float)anim) : 0.0f;
        float cos = animated ? MathHelper.cos((float)anim) : 0.0f;
        float spreadAngle = 18.0f + wingSpring * 5.0f;
        float pitchAngle = 13.0f + smoothLean * 0.3f + cos * 4.0f;
        float rollAngle = sin * 25.0f + smoothStrafe * 0.75f;
        EntityPose pose = player.getPose();
        boolean fallFlying = player.isGliding();
        boolean bl = horizontalPose = pose == EntityPose.SWIMMING || fallFlying;
        if (horizontalPose) {
            spreadAngle -= 4.0f;
            pitchAngle -= 6.0f;
            rollAngle *= 0.72f;
        }
        if (player.isSneaking()) {
            spreadAngle -= 3.0f;
            pitchAngle += 8.0f;
        }
        double px = MathHelper.lerp((double)tickDelta, (double)player.prevX, (double)player.getX()) - cameraPos.x;
        double py = MathHelper.lerp((double)tickDelta, (double)player.prevY, (double)player.getY()) - cameraPos.y;
        double pz = MathHelper.lerp((double)tickDelta, (double)player.prevZ, (double)player.getZ()) - cameraPos.z;
        matrices.push();
        matrices.translate(px, py, pz);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        this.applyBackPoseTransform(matrices, player, tickDelta, pose, fallFlying);
        int theme = this.resolveCosmeticThemeColor();
        int topColor = ColorUtils.setAlphaColor(theme, 132);
        int bottomColor = ColorUtils.setAlphaColor(ColorUtils.darken(theme, 0.85f), 102);
        int outlineColor = ColorUtils.setAlphaColor(ColorUtils.darken(theme, 0.58f), 214);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.blendFunc((int)770, (int)771);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        float butterflyScale = 1.0f * this.butterflyWingSize.get();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        this.renderButterflyWing(buffer, matrices, 1.0f, spreadAngle, pitchAngle, rollAngle, butterflyScale, topColor, bottomColor);
        this.renderButterflyWing(buffer, matrices, -1.0f, spreadAngle, pitchAngle, rollAngle, butterflyScale, topColor, bottomColor);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.lineWidth((float)1.9f);
        BufferBuilder outlineBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        this.renderButterflyWingOutline(outlineBuffer, matrices, 1.0f, spreadAngle, pitchAngle, rollAngle, butterflyScale, outlineColor);
        this.renderButterflyWingOutline(outlineBuffer, matrices, -1.0f, spreadAngle, pitchAngle, rollAngle, butterflyScale, outlineColor);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)outlineBuffer.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask((boolean)true);
        matrices.pop();
    }

    private void renderClassicWings(PlayerEntity player, float tickDelta, MatrixStack matrices, Vec3d cameraPos) {
        boolean horizontalPose;
        if (!player.isAlive() || player.isInvisible()) {
            return;
        }
        if (player.isGliding() || player.getPose() == EntityPose.SWIMMING || player.isInSwimmingPose()) {
            return;
        }
        double px = MathHelper.lerp((double)tickDelta, (double)player.prevX, (double)player.getX()) - cameraPos.x;
        double py = MathHelper.lerp((double)tickDelta, (double)player.prevY, (double)player.getY()) - cameraPos.y;
        double pz = MathHelper.lerp((double)tickDelta, (double)player.prevZ, (double)player.getZ()) - cameraPos.z;
        float bodyYaw = this.resolveClassicBodyYaw(player, tickDelta);
        Vec3d velocity = player.getVelocity();
        float yawRad = bodyYaw * ((float)Math.PI / 180);
        Vec3d forward = new Vec3d((double)(-MathHelper.sin((float)yawRad)), 0.0, (double)MathHelper.cos((float)yawRad));
        Vec3d sideways = new Vec3d(forward.z, 0.0, -forward.x);
        float forwardMove = (float)(velocity.x * forward.x + velocity.z * forward.z);
        float strafeMove = (float)(velocity.x * sideways.x + velocity.z * sideways.z);
        float verticalMove = (float)velocity.y;
        boolean animated = this.classicWingAnimation.isState();
        float smoothLean = animated ? MathHelper.clamp((float)(-forwardMove * 140.0f - verticalMove * 48.0f), (float)-24.0f, (float)26.0f) : 0.0f;
        float smoothStrafe = animated ? MathHelper.clamp((float)(strafeMove * 90.0f), (float)-10.0f, (float)10.0f) : 0.0f;
        float wingSpring = animated ? MathHelper.clamp((float)(Math.abs(forwardMove) * 0.95f + Math.abs(strafeMove) * 0.65f + Math.abs(verticalMove) * 0.75f), (float)0.0f, (float)1.7f) : 0.0f;
        float anim = ((float)player.age + tickDelta) * 0.22f * 1.6f + wingSpring * 0.4f;
        float sin = animated ? MathHelper.sin((float)anim) : 0.0f;
        float cos = animated ? MathHelper.cos((float)anim) : 0.0f;
        float spreadAngle = 18.0f + wingSpring * 5.0f;
        float pitchAngle = 13.0f + smoothLean * 0.3f + cos * 4.0f;
        float rollAngle = sin * 25.0f + smoothStrafe * 0.75f;
        EntityPose pose = player.getPose();
        boolean fallFlying = player.isGliding();
        boolean bl = horizontalPose = pose == EntityPose.SWIMMING || fallFlying;
        if (horizontalPose) {
            spreadAngle -= 4.0f;
            pitchAngle -= 6.0f;
            rollAngle *= 0.72f;
        }
        if (player.isSneaking()) {
            spreadAngle -= 3.0f;
            pitchAngle += 8.0f;
        }
        ClassicWingPose wingPose = this.resolveClassicWingPose(player, tickDelta, pose);
        float open = spreadAngle * wingPose.openMultiplier;
        float scale = wingPose.scaleMultiplier * this.classicWingSize.get();
        float animatedSidePitch = wingPose.sidePitch + pitchAngle * 0.18f;
        float animatedSideRoll = wingPose.sideRoll + rollAngle * 0.2f;
        int theme = this.resolveCosmeticThemeColor();
        int baseColor = ColorUtils.setAlphaColor(theme, 220);
        int glowColor = ColorUtils.setAlphaColor(ColorUtils.interpolate(theme, -1, 0.28f), Math.round(48.4f));
        int coreColor = ColorUtils.setAlphaColor(ColorUtils.interpolate(theme, -1, 0.55f), Math.round(57.199997f));
        int outlineColor = ColorUtils.setAlphaColor(ColorUtils.darken(theme, 0.62f), Math.round(136.4f));
        int ribsColor = ColorUtils.setAlphaColor(ColorUtils.interpolate(theme, -1, 0.28f), Math.round(44.0f));
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.blendFunc((int)770, (int)771);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        matrices.push();
        matrices.translate(px, py, pz);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
        if (wingPose.preTranslateY != 0.0f || wingPose.preTranslateZ != 0.0f) {
            matrices.translate(0.0f, wingPose.preTranslateY, wingPose.preTranslateZ);
        }
        if (wingPose.pitchRotation != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(wingPose.pitchRotation));
        }
        if (wingPose.rollRotation != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(wingPose.rollRotation));
        }
        matrices.translate(0.0f, wingPose.anchorY, wingPose.anchorZ);
        matrices.scale(scale, scale, scale);
        this.renderClassicWingSide(matrices, -1.0f, open, animatedSidePitch, animatedSideRoll, baseColor, glowColor, coreColor, outlineColor, ribsColor, wingPose);
        this.renderClassicWingSide(matrices, 1.0f, open, animatedSidePitch, animatedSideRoll, baseColor, glowColor, coreColor, outlineColor, ribsColor, wingPose);
        matrices.pop();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask((boolean)true);
    }

    private void renderNimbus(Event3DRender event) {
        if (Cosmetics.mc.player == null || Cosmetics.mc.world == null || Cosmetics.mc.options.getPerspective().isFirstPerson()) {
            return;
        }
        float tickDelta = event.getTickDelta();
        Vec3d camera = event.getCamera().getPos();
        double x2 = MathHelper.lerp((double)tickDelta, (double)Cosmetics.mc.player.prevX, (double)Cosmetics.mc.player.getX());
        double y2 = MathHelper.lerp((double)tickDelta, (double)Cosmetics.mc.player.prevY, (double)Cosmetics.mc.player.getY()) + (double)Cosmetics.mc.player.getHeight() + 0.1;
        double z2 = MathHelper.lerp((double)tickDelta, (double)Cosmetics.mc.player.prevZ, (double)Cosmetics.mc.player.getZ());
        int baseColor = this.resolveCosmeticThemeColor();
        long nowMs = System.currentTimeMillis();
        double radiansPerMillisecond = 0.0029670597283903604;
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)this.getNimbusTexture());
        MatrixStack matrices = event.getMatrices();
        for (int arm = 0; arm < 2; ++arm) {
            double baseAngle = radiansPerMillisecond * (double)nowMs + (double)arm * Math.PI;
            for (int segment = 0; segment < 17; ++segment) {
                double segmentAngle = baseAngle - (double)segment * 0.11;
                double offsetX = Math.cos(segmentAngle) * (double)0.45f;
                double offsetZ = Math.sin(segmentAngle) * (double)0.45f;
                float progress = (float)segment / (float)Math.max(1, 16);
                float size = 0.23f * (1.0f - progress * 0.7f);
                int alpha = MathHelper.clamp((int)(255 - segment * 9), (int)0, (int)255);
                int segmentColor = ColorUtils.setAlphaColor(baseColor, alpha);
                this.renderNimbusBillboard(matrices, event.getCamera().getYaw(), event.getCamera().getPitch(), x2 - camera.x + offsetX, y2 - camera.y, z2 - camera.z + offsetZ, size, segmentColor);
            }
        }
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    public void renderChinaHat(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, PlayerEntity player, ModelWithHead model) {
        if (!this.isEnable() || !this.cosmetics.is("Китайская шляпа")) {
            return;
        }
        if (Cosmetics.mc.player == null || Cosmetics.mc.world == null) {
            return;
        }
        if (!this.shouldRenderCosmeticForPlayer(player)) {
            return;
        }
        if (player == Cosmetics.mc.player && Cosmetics.mc.options.getPerspective().isFirstPerson()) {
            return;
        }
        double radius = player.getBoundingBox().maxX - player.getBoundingBox().minX;
        float offset = player.getEquippedStack(EquipmentSlot.HEAD).isEmpty() ? 0.415f : 0.48f;
        matrixStack.push();
        model.getHead().rotate(matrixStack);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.lineWidth((float)2.0f);
        GL11.glEnable((int)2848);
        GL11.glHint((int)3154, (int)4354);
        matrixStack.translate(0.0f, -offset, 0.0f);
        matrixStack.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(180.0f));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0f));
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        float y2 = 0.0f;
        int colorTheme = this.resolveCosmeticThemeColor();
        int coneColor = ColorUtils.setAlphaColor(colorTheme, 125);
        int outlineColor = ColorUtils.setAlphaColor(ColorUtils.darken(colorTheme, 0.5f), 180);
        for (int i2 = 0; i2 <= 180; ++i2) {
            float iPi = (float)i2 * 0.06981317f;
            float x2 = (float)((double)MathHelper.sin((float)iPi) * radius);
            float z2 = (float)((double)MathHelper.cos((float)iPi) * radius);
            buffer.vertex(matrix, x2, y2, z2).color(coneColor);
            buffer.vertex(matrix, 0.0f, 0.3f, 0.0f).color(colorTheme);
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.depthMask((boolean)false);
        buffer = tessellator.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
        float firstX = 0.0f;
        float firstZ = 0.0f;
        boolean firstSet = false;
        for (int i3 = 0; i3 <= 180; ++i3) {
            float iPi = (float)i3 * 0.06981317f;
            float x3 = (float)((double)MathHelper.sin((float)iPi) * radius);
            float z3 = (float)((double)MathHelper.cos((float)iPi) * radius);
            buffer.vertex(matrix, x3, y2, z3).color(outlineColor);
            if (firstSet) continue;
            firstX = x3;
            firstZ = z3;
            firstSet = true;
        }
        if (firstSet) {
            buffer.vertex(matrix, firstX, y2, firstZ).color(outlineColor);
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glDisable((int)2848);
        matrixStack.pop();
    }

    private Identifier getNimbusTexture() {
        return Identifier.of((String)"polar", (String)"textures/targetesp/bloom.png");
    }

    private void renderNimbusBillboard(MatrixStack matrices, float cameraYaw, float cameraPitch, double x2, double y2, double z2, float size, int color) {
        int a2 = color >> 24 & 0xFF;
        if (a2 <= 0) {
            return;
        }
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        float half = size * 0.5f;
        matrices.push();
        matrices.translate(x2, y2, z2);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cameraYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cameraPitch));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -half, -half, 0.0f).texture(0.0f, 1.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, -half, half, 0.0f).texture(0.0f, 0.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, half, half, 0.0f).texture(1.0f, 0.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, half, -half, 0.0f).texture(1.0f, 1.0f).color(r2, g2, b2, a2);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        matrices.pop();
    }

    private boolean shouldRenderCosmeticForPlayer(PlayerEntity player) {
        if (Cosmetics.mc.player == null) {
            return false;
        }
        if (player == Cosmetics.mc.player) {
            return true;
        }
        return polar.INSTANCE != null && polar.INSTANCE.friendStorage != null && polar.INSTANCE.friendStorage.isFriend(player.getName().getString());
    }

    private int getStableThemeColor() {
        if (polar.INSTANCE == null || polar.INSTANCE.themeStorage == null || polar.INSTANCE.themeStorage.getThemes() == null) {
            return ColorUtils.getThemeColor(0);
        }
        Theme theme = polar.INSTANCE.themeStorage.getThemes().getTheme();
        if (theme == null || theme.color == null || theme.color.length == 0) {
            return ColorUtils.getThemeColor(0);
        }
        return theme.color[0];
    }

    private int resolveCosmeticThemeColor() {
        if (polar.INSTANCE == null || polar.INSTANCE.themeStorage == null || polar.INSTANCE.themeStorage.getThemes() == null) {
            return ColorUtils.getThemeColor();
        }
        Theme theme = polar.INSTANCE.themeStorage.getThemes().getTheme();
        if (theme == null) {
            return ColorUtils.getThemeColor();
        }
        return "Rainbow".equals(theme.getName()) ? ColorUtils.getThemeColor() : this.getStableThemeColor();
    }

    private void syncWingSelectionState() {
        boolean butterfly = this.cosmetics.is("Крылья");
        boolean classic = this.cosmetics.is("Крылья 2");
        if (butterfly && classic) {
            if (butterfly != this.lastButterflySelected && classic == this.lastClassicSelected) {
                this.cosmetics.set("Крылья 2", false);
                classic = false;
            } else {
                this.cosmetics.set("Крылья", false);
                butterfly = false;
            }
        }
        this.lastButterflySelected = butterfly;
        this.lastClassicSelected = classic;
    }

    private void applyBackPoseTransform(MatrixStack matrices, PlayerEntity player, float tickDelta, EntityPose pose, boolean fallFlying) {
        if (fallFlying) {
            float pitch = player.getPitch(tickDelta);
            float clampedPitch = MathHelper.clamp((float)pitch, (float)-65.0f, (float)65.0f);
            matrices.translate(0.0f, 0.3f, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-(90.0f + clampedPitch)));
            matrices.translate(0.0f, -0.15f, 0.12f);
            return;
        }
        if (pose == EntityPose.SWIMMING) {
            float pitch = player.getPitch(tickDelta);
            float clampedPitch = MathHelper.clamp((float)pitch, (float)-65.0f, (float)65.0f);
            matrices.translate(0.0f, 0.3f, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-(90.0f + clampedPitch)));
            matrices.translate(0.0f, -0.15f, 0.12f);
            return;
        }
        if (player.isSneaking()) {
            matrices.translate(0.0f, 1.15f, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(24.0f));
            matrices.translate(0.0f, 0.0f, 0.08f);
        } else {
            matrices.translate(0.0f, 1.3f, 0.08f);
        }
    }

    private float resolveClassicBodyYaw(PlayerEntity player, float tickDelta) {
        float targetBodyYaw = MathHelper.lerpAngleDegrees((float)tickDelta, (float)player.prevBodyYaw, (float)player.bodyYaw);
        if (player != Cosmetics.mc.player) {
            return targetBodyYaw;
        }
        if (!this.selfClassicBodyYawInitialized) {
            this.selfClassicBodyYaw = targetBodyYaw;
            this.selfClassicBodyYawInitialized = true;
            return this.selfClassicBodyYaw;
        }
        float delta = MathHelper.wrapDegrees((float)(targetBodyYaw - this.selfClassicBodyYaw));
        this.selfClassicBodyYaw += MathHelper.clamp((float)delta, (float)-14.0f, (float)14.0f);
        return this.selfClassicBodyYaw;
    }

    private ClassicWingPose resolveClassicWingPose(PlayerEntity player, float tickDelta, EntityPose pose) {
        float pitch = player.getPitch(tickDelta);
        if (player.isGliding()) {
            float clampedPitch = MathHelper.clamp((float)pitch, (float)-65.0f, (float)65.0f);
            return new ClassicWingPose(1.18f, 0.1f, 0.0f, 0.0f, -(90.0f + clampedPitch), 0.0f, 0.76f, 0.92f, 0.1f, 0.58f, 0.05f, 0.0f, 0.06f, -5.0f, -2.0f, 0.13f);
        }
        if (pose == EntityPose.SWIMMING || player.isInSwimmingPose()) {
            float clampedPitch = MathHelper.clamp((float)pitch, (float)-65.0f, (float)65.0f);
            float bodyShiftY = player.isInSwimmingPose() ? 1.1f : 1.18f;
            float bodyShiftZ = player.isInSwimmingPose() ? 0.18f : 0.12f;
            return new ClassicWingPose(bodyShiftY, bodyShiftZ, 0.18f, 0.48f, -(90.0f + clampedPitch), 0.0f, 0.84f, 0.96f, 0.12f, 0.7f, 0.03f, 0.0f, 0.01f, -7.0f, -3.0f, 0.16f);
        }
        if (player.isSneaking()) {
            return new ClassicWingPose(0.0f, 0.0f, 0.96f, 0.1f, 18.0f, 0.0f, 1.0f, 1.0f, 0.18f, 4.5f, 0.06f, 0.0f, 0.02f, -11.0f, -4.0f, 0.12f);
        }
        return new ClassicWingPose(0.0f, 0.0f, 1.18f, 0.1f, 0.0f, 0.0f, 1.0f, 1.0f, 0.18f, 4.5f, 0.06f, 0.0f, 0.02f, -11.0f, -4.0f, 0.12f);
    }

    private void renderClassicWingSide(MatrixStack matrices, float side, float open, float sidePitch, float sideRoll, int baseColor, int glowColor, int coreColor, int outlineColor, int ribsColor, ClassicWingPose pose) {
        matrices.push();
        matrices.translate(side * pose.sideOffset, pose.sideYOffset, pose.sideZOffset);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * open));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * sideRoll));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sidePitch));
        RenderSystem.blendFunc((int)770, (int)1);
        this.drawClassicWingLayer(matrices, side, 1.22f, glowColor, ColorUtils.setAlphaColor(glowColor, 0));
        this.drawClassicWingLayer(matrices, side, 0.84f, coreColor, ColorUtils.setAlphaColor(coreColor, 0));
        RenderSystem.blendFunc((int)770, (int)771);
        this.drawClassicWingLayer(matrices, side, 1.0f, baseColor, ColorUtils.setAlphaColor(baseColor, 10));
        RenderSystem.blendFunc((int)770, (int)1);
        this.drawClassicWingOutline(matrices, side, 1.0f, outlineColor);
        this.drawClassicWingRibs(matrices, side, 0.96f, ribsColor);
        matrices.pop();
    }

    private void drawClassicWingLayer(MatrixStack matrices, float side, float scale, int rootColor, int edgeColor) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i2 = 0; i2 < CLASSIC_WING_SHAPE.length; ++i2) {
            ClassicWingPoint current = CLASSIC_WING_SHAPE[i2];
            ClassicWingPoint next = CLASSIC_WING_SHAPE[(i2 + 1) % CLASSIC_WING_SHAPE.length];
            this.vertex(buffer, matrix, 0.0f, 0.0f, 0.0f, rootColor);
            this.vertex(buffer, matrix, side * current.x * scale, current.y * scale, 0.0f, this.applyClassicWingPointAlpha(edgeColor, current.alphaMultiplier));
            this.vertex(buffer, matrix, side * next.x * scale, next.y * scale, 0.0f, this.applyClassicWingPointAlpha(edgeColor, next.alphaMultiplier));
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private void drawClassicWingOutline(MatrixStack matrices, float side, float scale, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        RenderSystem.lineWidth((float)1.35f);
        GL11.glEnable((int)2848);
        for (int i2 = 0; i2 < CLASSIC_WING_SHAPE.length; ++i2) {
            ClassicWingPoint current = CLASSIC_WING_SHAPE[i2];
            ClassicWingPoint next = CLASSIC_WING_SHAPE[(i2 + 1) % CLASSIC_WING_SHAPE.length];
            this.addLine(buffer, matrix, side * current.x * scale, current.y * scale, 0.0f, side * next.x * scale, next.y * scale, 0.0f, color);
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        GL11.glDisable((int)2848);
    }

    private void drawClassicWingRibs(MatrixStack matrices, float side, float scale, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        int[] ribIndices = new int[]{2, 4, 7, 9, 11};
        RenderSystem.lineWidth((float)0.9f);
        for (int ribIndex : ribIndices) {
            ClassicWingPoint point = CLASSIC_WING_SHAPE[ribIndex];
            this.vertex(buffer, matrix, 0.0f, 0.0f, 0.0f, ColorUtils.setAlphaColor(color, Math.max(8, (int)((float)(color >> 24 & 0xFF) * 0.75f))));
            this.vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0.0f, this.applyClassicWingPointAlpha(color, point.alphaMultiplier));
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private int applyClassicWingPointAlpha(int color, float multiplier) {
        int alpha = color >> 24 & 0xFF;
        return ColorUtils.setAlphaColor(color, Math.max(0, Math.min(255, (int)((float)alpha * multiplier))));
    }

    private void vertex(BufferBuilder buffer, Matrix4f matrix, float x2, float y2, float z2, int color) {
        buffer.vertex(matrix, x2, y2, z2).color(color);
    }

    private void renderButterflyWing(BufferBuilder buffer, MatrixStack matrices, float side, float spread, float pitch, float roll, float scale, int topColor, int bottomColor) {
        float root = 0.12f * scale;
        float topW = 1.52f * scale;
        float topH = 0.64f * scale;
        float lowW = 1.14f * scale;
        float lowH = 0.39f * scale;
        matrices.push();
        matrices.translate(0.15f * side, 0.0f, -0.17f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * spread));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * roll));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        this.addDoubleSidedGradientTriangle(buffer, matrix, side * root, 0.02f, -0.01f, side * (root + topW * 0.22f), topH * 0.98f, -0.06f, side * (root + topW * 0.88f), topH * 0.6f, -0.13f, topColor, bottomColor);
        this.addDoubleSidedGradientTriangle(buffer, matrix, side * root, 0.02f, -0.01f, side * (root + topW * 0.88f), topH * 0.6f, -0.13f, side * (root + topW), topH * 0.12f, -0.17f, topColor, bottomColor);
        this.addDoubleSidedGradientTriangle(buffer, matrix, side * root, -0.03f, -0.03f, side * (root + lowW * 0.26f), -lowH * 0.96f, -0.11f, side * (root + lowW * 0.84f), -lowH * 0.54f, -0.18f, bottomColor, topColor);
        this.addDoubleSidedGradientTriangle(buffer, matrix, side * root, -0.03f, -0.03f, side * (root + lowW * 0.84f), -lowH * 0.54f, -0.18f, side * (root + lowW), -lowH * 0.12f, -0.21f, bottomColor, topColor);
        matrices.pop();
    }

    private void renderButterflyWingOutline(BufferBuilder buffer, MatrixStack matrices, float side, float spread, float pitch, float roll, float scale, int outlineColor) {
        float root = 0.12f * scale;
        float topW = 1.52f * scale;
        float topH = 0.64f * scale;
        float lowW = 1.14f * scale;
        float lowH = 0.39f * scale;
        matrices.push();
        matrices.translate(0.15f * side, 0.0f, -0.17f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * spread));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * roll));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        this.addLine(buffer, matrix, side * root, 0.02f, -0.01f, side * (root + topW * 0.22f), topH * 0.98f, -0.06f, outlineColor);
        this.addLine(buffer, matrix, side * (root + topW * 0.22f), topH * 0.98f, -0.06f, side * (root + topW * 0.88f), topH * 0.6f, -0.13f, outlineColor);
        this.addLine(buffer, matrix, side * (root + topW * 0.88f), topH * 0.6f, -0.13f, side * (root + topW), topH * 0.12f, -0.17f, outlineColor);
        this.addLine(buffer, matrix, side * root, -0.03f, -0.03f, side * (root + lowW * 0.26f), -lowH * 0.96f, -0.11f, outlineColor);
        this.addLine(buffer, matrix, side * (root + lowW * 0.26f), -lowH * 0.96f, -0.11f, side * (root + lowW * 0.84f), -lowH * 0.54f, -0.18f, outlineColor);
        this.addLine(buffer, matrix, side * (root + lowW * 0.84f), -lowH * 0.54f, -0.18f, side * (root + lowW), -lowH * 0.12f, -0.21f, outlineColor);
        this.addLine(buffer, matrix, side * root, -0.01f, -0.02f, side * (root + topW * 0.6f), 0.08f, -0.08f, outlineColor);
        matrices.pop();
    }

    private void addDoubleSidedQuad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int r2, int g2, int b2, int a2) {
        this.addQuad(buffer, matrix, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, r2, g2, b2, a2);
        this.addQuad(buffer, matrix, x4, y4, z4, x3, y3, z3, x2, y2, z2, x1, y1, z1, r2, g2, b2, a2);
    }

    private void addDoubleSidedGradientQuad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int nearColor, int farColor) {
        int nr = nearColor >> 16 & 0xFF;
        int ng = nearColor >> 8 & 0xFF;
        int nb = nearColor & 0xFF;
        int na = nearColor >> 24 & 0xFF;
        int fr = farColor >> 16 & 0xFF;
        int fg = farColor >> 8 & 0xFF;
        int fb = farColor & 0xFF;
        int fa = farColor >> 24 & 0xFF;
        buffer.vertex(matrix, x1, y1, z1).color(nr, ng, nb, na);
        buffer.vertex(matrix, x2, y2, z2).color(fr, fg, fb, fa);
        buffer.vertex(matrix, x3, y3, z3).color(fr, fg, fb, fa);
        buffer.vertex(matrix, x4, y4, z4).color(nr, ng, nb, na);
        buffer.vertex(matrix, x4, y4, z4).color(nr, ng, nb, na);
        buffer.vertex(matrix, x3, y3, z3).color(fr, fg, fb, fa);
        buffer.vertex(matrix, x2, y2, z2).color(fr, fg, fb, fa);
        buffer.vertex(matrix, x1, y1, z1).color(nr, ng, nb, na);
    }

    private void addDoubleSidedGradientTriangle(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int nearColor, int farColor) {
        int nr = nearColor >> 16 & 0xFF;
        int ng = nearColor >> 8 & 0xFF;
        int nb = nearColor & 0xFF;
        int na = nearColor >> 24 & 0xFF;
        int fr = farColor >> 16 & 0xFF;
        int fg = farColor >> 8 & 0xFF;
        int fb = farColor & 0xFF;
        int fa = farColor >> 24 & 0xFF;
        buffer.vertex(matrix, x1, y1, z1).color(nr, ng, nb, na);
        buffer.vertex(matrix, x2, y2, z2).color(fr, fg, fb, fa);
        buffer.vertex(matrix, x3, y3, z3).color(fr, fg, fb, fa);
        buffer.vertex(matrix, x3, y3, z3).color(fr, fg, fb, fa);
        buffer.vertex(matrix, x2, y2, z2).color(fr, fg, fb, fa);
        buffer.vertex(matrix, x1, y1, z1).color(nr, ng, nb, na);
    }

    private void renderWingBoneLine(BufferBuilder buffer, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float thickness, int colorA, int colorB) {
        float vx1 = x1 - x0;
        float vy1 = y1 - y0;
        float len1 = Math.max(1.0E-4f, (float)Math.sqrt(vx1 * vx1 + vy1 * vy1));
        float nx1 = -vy1 / len1 * thickness;
        float ny1 = vx1 / len1 * thickness;
        int aR = colorA >> 16 & 0xFF;
        int aG = colorA >> 8 & 0xFF;
        int aB = colorA & 0xFF;
        int aA = colorA >> 24 & 0xFF;
        int bR = colorB >> 16 & 0xFF;
        int bG = colorB >> 8 & 0xFF;
        int bB = colorB & 0xFF;
        int bA = colorB >> 24 & 0xFF;
        this.addDoubleSidedQuad(buffer, matrix, x0 + nx1, y0 + ny1, z0, x0 - nx1, y0 - ny1, z0, x1 - nx1, y1 - ny1, z1, x1 + nx1, y1 + ny1, z1, aR, aG, aB, aA);
        float vx2 = x2 - x1;
        float vy2 = y2 - y1;
        float len2 = Math.max(1.0E-4f, (float)Math.sqrt(vx2 * vx2 + vy2 * vy2));
        float nx2 = -vy2 / len2 * thickness;
        float ny2 = vx2 / len2 * thickness;
        this.addDoubleSidedQuad(buffer, matrix, x1 + nx2, y1 + ny2, z1, x1 - nx2, y1 - ny2, z1, x2 - nx2, y2 - ny2, z2, x2 + nx2, y2 + ny2, z2, bR, bG, bB, bA);
    }

    private void addQuad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int r2, int g2, int b2, int a2) {
        buffer.vertex(matrix, x1, y1, z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2, y2, z2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x3, y3, z3).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x4, y4, z4).color(r2, g2, b2, a2);
    }

    private void addLine(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        int a2 = color >> 24 & 0xFF;
        buffer.vertex(matrix, x1, y1, z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2, y2, z2).color(r2, g2, b2, a2);
    }

    private static final class ClassicWingPose {
        private final float preTranslateY;
        private final float preTranslateZ;
        private final float anchorY;
        private final float anchorZ;
        private final float pitchRotation;
        private final float rollRotation;
        private final float openMultiplier;
        private final float scaleMultiplier;
        private final float motionSpreadBoost;
        private final float flapAmplitude;
        private final float sideOffset;
        private final float sideYOffset;
        private final float sideZOffset;
        private final float sideRoll;
        private final float sidePitch;
        private final float flapSpeed;

        private ClassicWingPose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ, float pitchRotation, float rollRotation, float openMultiplier, float scaleMultiplier, float motionSpreadBoost, float flapAmplitude, float sideOffset, float sideYOffset, float sideZOffset, float sideRoll, float sidePitch, float flapSpeed) {
            this.preTranslateY = preTranslateY;
            this.preTranslateZ = preTranslateZ;
            this.anchorY = anchorY;
            this.anchorZ = anchorZ;
            this.pitchRotation = pitchRotation;
            this.rollRotation = rollRotation;
            this.openMultiplier = openMultiplier;
            this.scaleMultiplier = scaleMultiplier;
            this.motionSpreadBoost = motionSpreadBoost;
            this.flapAmplitude = flapAmplitude;
            this.sideOffset = sideOffset;
            this.sideYOffset = sideYOffset;
            this.sideZOffset = sideZOffset;
            this.sideRoll = sideRoll;
            this.sidePitch = sidePitch;
            this.flapSpeed = flapSpeed;
        }
    }

    private static final class ClassicWingPoint {
        private final float x;
        private final float y;
        private final float alphaMultiplier;

        private ClassicWingPoint(float x2, float y2, float alphaMultiplier) {
            this.x = x2;
            this.y = y2;
            this.alphaMultiplier = alphaMultiplier;
        }
    }
}

