package polar.ru.api.utils.draggable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import polar.ru.api.QClient;
import polar.ru.api.utils.draggable.Vec2i;
import polar.ru.api.utils.math.HoveringUtils;
import polar.ru.api.utils.math.MathUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.client.modules.Module;

public class Draggable
implements QClient {
    @Expose
    @SerializedName(value="x")
    private float xPos;
    @Expose
    @SerializedName(value="y")
    private float yPos;
    public float initialXVal;
    public float initialYVal;
    private float startX;
    private float startY;
    private boolean dragging;
    private float width;
    private float height;
    @Expose
    @SerializedName(value="name")
    private String name;
    private final Module module;
    private float targetXPos;
    private float targetYPos;
    private static final float CENTER_LINE_WIDTH = 1.0f;
    private static final float SNAP_THRESHOLD = 10.0f;
    private float lineAlpha = 0.0f;
    private long lastUpdateTime;
    private boolean snapToCenter;
    private boolean snapToCenterx;
    private boolean snapToCenter2x;
    private boolean snapToCenter3x;
    private boolean snapToCenter4x;
    private boolean snapToCenter5x;
    private boolean snapToCenter2;
    private boolean snapToCenter3;
    private boolean snapToCenter4;
    private boolean snapToCenter5;
    private static final float LERP_SPEED = 0.19f;
    private static final float MAX_TILT_DEGREES = 25.0f;
    private static final float TILT_FROM_MOUSE_DELTA = 4.0f;
    private static final float DRAG_TILT_LERP = 0.14f;
    private static final float RELEASE_TILT_LERP = 0.1f;
    private static final float TILT_DELTA_SMOOTHING = 0.18f;
    private static final float TILT_TARGET_SMOOTHING = 0.22f;
    private static final float TILT_DEADZONE = 0.18f;
    private static final float DRAG_SCALE_MULTIPLIER = 1.01f;
    private static final float DRAG_SCALE_LERP = 0.1f;
    private static final float RELEASE_SCALE_LERP = 0.02f;
    private float dragTiltDegrees;
    private float targetTiltDegrees;
    private float smoothedMouseDeltaX;
    private float lastDragMouseX;
    private boolean hasLastDragMouseX;
    private boolean tiltMatrixPushed;
    private float dragScale = 1.0f;
    private float targetScale = 1.0f;

    public Draggable(Module module, String name, float initialXVal, float initialYVal) {
        this.module = module;
        this.name = name;
        this.xPos = initialXVal;
        this.yPos = initialYVal;
        this.initialXVal = initialXVal;
        this.initialYVal = initialYVal;
    }

    public float getX() {
        return this.xPos;
    }

    public void setX(float x2) {
        this.xPos = x2;
    }

    public float getY() {
        return this.yPos;
    }

    public void setY(float y2) {
        this.yPos = y2;
    }

    private Vec2i getMouse(int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        Window window = client == null ? null : client.getWindow();
        double scaleFactor = window == null ? 1.0 : window.getScaleFactor();
        return new Vec2i((int)((double)mouseX * scaleFactor / 2.0), (int)((double)mouseY * scaleFactor / 2.0));
    }

    public final void onDraw(int mouseX, int mouseY, Window res, MatrixStack ms) {
        Vec2i fixed = this.getMouse(mouseX, mouseY);
        mouseX = fixed.getX();
        mouseY = fixed.getY();
        float centerX = (float)res.getScaledWidth() / 2.0f;
        float centerY = (float)res.getScaledHeight() / 2.0f;
        float centerX2 = (float)res.getScaledWidth() / 4.0f;
        float centerY2 = (float)res.getScaledHeight() / 4.0f;
        float centerX3 = (float)res.getScaledWidth() / 8.0f;
        float centerY3 = (float)res.getScaledHeight() / 8.0f;
        float centerX4 = (float)res.getScaledWidth() / 1.15f;
        float centerY4 = (float)res.getScaledHeight() / 1.15f;
        float centerX5 = (float)res.getScaledWidth() / 1.35f;
        float centerY5 = (float)res.getScaledHeight() / 1.35f;
        this.snapToCenter5 = false;
        this.snapToCenter4 = false;
        this.snapToCenter3 = false;
        this.snapToCenter2 = false;
        this.snapToCenter5x = false;
        this.snapToCenter4x = false;
        this.snapToCenter3x = false;
        this.snapToCenter2x = false;
        this.snapToCenterx = false;
        this.snapToCenter = false;
        if (this.dragging) {
            this.targetScale = 1.01f;
            if (this.hasLastDragMouseX) {
                float mouseDeltaX = (float)mouseX - this.lastDragMouseX;
                if (Math.abs(mouseDeltaX) < 0.18f) {
                    mouseDeltaX = 0.0f;
                }
                this.smoothedMouseDeltaX = MathUtils.lerp(this.smoothedMouseDeltaX, mouseDeltaX, 0.18f);
                float desiredTilt = Math.max(-25.0f, Math.min(25.0f, this.smoothedMouseDeltaX * 4.0f));
                this.targetTiltDegrees = MathUtils.lerp(this.targetTiltDegrees, desiredTilt, 0.22f);
            }
            this.lastDragMouseX = mouseX;
            this.hasLastDragMouseX = true;
            this.targetXPos = (float)mouseX - this.startX;
            this.targetYPos = (float)mouseY - this.startY;
            boolean snapped = false;
            if (Math.abs(this.targetXPos + this.width / 2.0f - centerX) < 10.0f) {
                this.targetXPos = centerX - this.width / 2.0f;
                this.snapToCenterx = true;
                snapped = true;
            }
            if (Math.abs(this.targetYPos + this.height / 2.0f - centerY) < 10.0f) {
                this.targetYPos = centerY - this.height / 2.0f;
                this.snapToCenter = true;
                snapped = true;
            }
            if (Math.abs(this.targetXPos + this.width / 2.0f - centerX2) < 10.0f) {
                this.targetXPos = centerX2 - this.width / 2.0f;
                this.snapToCenter2x = true;
                snapped = true;
            }
            if (Math.abs(this.targetYPos + this.height / 2.0f - centerY2) < 10.0f) {
                this.targetYPos = centerY2 - this.height / 2.0f;
                this.snapToCenter2 = true;
                snapped = true;
            }
            if (Math.abs(this.targetXPos + this.width / 2.0f - centerX3) < 10.0f) {
                this.targetXPos = centerX3 - this.width / 2.0f;
                this.snapToCenter3x = true;
                snapped = true;
            }
            if (Math.abs(this.targetYPos + this.height / 2.0f - centerY3) < 10.0f) {
                this.targetYPos = centerY3 - this.height / 2.0f;
                this.snapToCenter3 = true;
                snapped = true;
            }
            if (Math.abs(this.targetXPos + this.width / 2.0f - centerX4) < 10.0f) {
                this.targetXPos = centerX4 - this.width / 2.0f;
                this.snapToCenter4x = true;
                snapped = true;
            }
            if (Math.abs(this.targetYPos + this.height / 2.0f - centerY4) < 10.0f) {
                this.targetYPos = centerY4 - this.height / 2.0f;
                this.snapToCenter4 = true;
                snapped = true;
            }
            if (Math.abs(this.targetXPos + this.width / 2.0f - centerX5) < 10.0f) {
                this.targetXPos = centerX5 - this.width / 2.0f;
                this.snapToCenter5x = true;
                snapped = true;
            }
            if (Math.abs(this.targetYPos + this.height / 2.0f - centerY5) < 10.0f) {
                this.targetYPos = centerY5 - this.height / 2.0f;
                this.snapToCenter5 = true;
                snapped = true;
            }
            if (this.targetXPos + this.width > (float)res.getScaledWidth()) {
                this.targetXPos = (float)res.getScaledWidth() - this.width;
            }
            if (this.targetYPos + this.height > (float)res.getScaledHeight()) {
                this.targetYPos = (float)res.getScaledHeight() - this.height;
            }
            if (this.targetXPos < 0.0f) {
                this.targetXPos = 0.0f;
            }
            if (this.targetYPos < 0.0f) {
                this.targetYPos = 0.0f;
            }
            this.xPos = MathUtils.lerp(this.xPos, this.targetXPos, 0.19f);
            this.yPos = MathUtils.lerp(this.yPos, this.targetYPos, 0.19f);
            this.updateLineAlpha(snapped);
        } else {
            this.targetScale = 1.0f;
            this.targetTiltDegrees = 0.0f;
            this.smoothedMouseDeltaX = MathUtils.lerp(this.smoothedMouseDeltaX, 0.0f, 0.18f);
            this.hasLastDragMouseX = false;
            this.updateLineAlpha(false);
        }
        this.updateTilt();
        this.drawCenterLines(ms, res);
    }

    private void updateTilt() {
        float lerp = this.dragging ? 0.14f : 0.1f;
        this.dragTiltDegrees = MathUtils.lerp(this.dragTiltDegrees, this.targetTiltDegrees, lerp);
        if (!this.dragging && Math.abs(this.dragTiltDegrees) < 0.02f) {
            this.dragTiltDegrees = 0.0f;
        }
        float scaleLerp = this.dragging ? 0.1f : 0.02f;
        this.dragScale = MathUtils.lerp(this.dragScale, this.targetScale, scaleLerp);
        if (!this.dragging && Math.abs(this.dragScale - 1.0f) < 0.002f) {
            this.dragScale = 1.0f;
        }
    }

    public void beginRenderTilt(MatrixStack ms) {
        this.updateTilt();
        this.tiltMatrixPushed = false;
        if (Math.abs(this.dragTiltDegrees) < 0.05f && Math.abs(this.dragScale - 1.0f) < 0.002f) {
            return;
        }
        float centerX = this.xPos + this.width / 2.0f;
        float centerY = this.yPos + this.height / 2.0f;
        ms.push();
        ms.translate(centerX, centerY, 0.0f);
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.dragTiltDegrees));
        ms.scale(this.dragScale, this.dragScale, 1.0f);
        ms.translate(-centerX, -centerY, 0.0f);
        this.tiltMatrixPushed = true;
    }

    public void endRenderTilt(MatrixStack ms) {
        if (this.tiltMatrixPushed) {
            ms.pop();
            this.tiltMatrixPushed = false;
        }
    }

    private void updateLineAlpha(boolean active) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (float)(currentTime - this.lastUpdateTime) / 1000.0f;
        this.lastUpdateTime = currentTime;
        float fadeSpeed = 2.0f;
        float fadeOutSpeed = 2.0f;
        if (active) {
            this.lineAlpha += deltaTime * fadeSpeed;
            if (this.lineAlpha > 1.0f) {
                this.lineAlpha = 1.0f;
            }
        } else {
            this.lineAlpha -= deltaTime * fadeOutSpeed;
            if (this.lineAlpha < 0.0f) {
                this.lineAlpha = 0.0f;
            }
        }
    }

    private void drawCenterLines(MatrixStack ms, Window res) {
        if (this.lineAlpha > 0.0f) {
            float centerX = (float)res.getScaledWidth() / 2.0f;
            float centerY = (float)res.getScaledHeight() / 2.0f;
            float centerX2 = (float)res.getScaledWidth() / 4.0f;
            float centerY2 = (float)res.getScaledHeight() / 4.0f;
            float centerX3 = (float)res.getScaledWidth() / 8.0f;
            float centerY3 = (float)res.getScaledHeight() / 8.0f;
            float centerX4 = (float)res.getScaledWidth() / 1.15f;
            float centerY4 = (float)res.getScaledHeight() / 1.15f;
            float centerX5 = (float)res.getScaledWidth() / 1.35f;
            float centerY5 = (float)res.getScaledHeight() / 1.35f;
            int color = (int)(this.lineAlpha * 255.0f) << 24 | 0xFFFFFF;
            if (this.snapToCenterx) {
                RenderUtils.drawRoundedRect(ms, centerX - 0.33333334f, 0.0f, 1.0f, res.getScaledHeight(), 1.0f, color);
            }
            if (this.snapToCenter) {
                RenderUtils.drawRoundedRect(ms, 0.0f, centerY - 0.33333334f, res.getScaledWidth(), 1.0f, 1.0f, color);
            }
            if (this.snapToCenter2x) {
                RenderUtils.drawRoundedRect(ms, centerX2 - 0.33333334f, 0.0f, 1.0f, res.getScaledHeight(), 1.0f, color);
            }
            if (this.snapToCenter2) {
                RenderUtils.drawRoundedRect(ms, 0.0f, centerY2 - 0.33333334f, res.getScaledWidth(), 1.0f, 1.0f, color);
            }
            if (this.snapToCenter3x) {
                RenderUtils.drawRoundedRect(ms, centerX3 - 0.33333334f, 0.0f, 1.0f, res.getScaledHeight(), 1.0f, color);
            }
            if (this.snapToCenter3) {
                RenderUtils.drawRoundedRect(ms, 0.0f, centerY3 - 0.33333334f, res.getScaledWidth(), 1.0f, 1.0f, color);
            }
            if (this.snapToCenter4x) {
                RenderUtils.drawRoundedRect(ms, centerX4 - 0.33333334f, 0.0f, 1.0f, res.getScaledHeight(), 1.0f, color);
            }
            if (this.snapToCenter4) {
                RenderUtils.drawRoundedRect(ms, 0.0f, centerY4 - 0.33333334f, res.getScaledWidth(), 1.0f, 1.0f, color);
            }
            if (this.snapToCenter5x) {
                RenderUtils.drawRoundedRect(ms, centerX5 - 0.33333334f, 0.0f, 1.0f, res.getScaledHeight(), 1.0f, color);
            }
            if (this.snapToCenter5) {
                RenderUtils.drawRoundedRect(ms, 0.0f, centerY5 - 0.33333334f, res.getScaledWidth(), 1.0f, 1.0f, color);
            }
        }
    }

    public final boolean onClick(double mouseX, double mouseY, int button) {
        if (button == 0 && HoveringUtils.isInRegion(mouseX, mouseY, this.xPos, this.yPos, this.width, this.height)) {
            this.dragging = true;
            this.targetScale = 1.01f;
            this.startX = (int)(mouseX - (double)this.xPos);
            this.startY = (int)(mouseY - (double)this.yPos);
            this.smoothedMouseDeltaX = 0.0f;
            this.hasLastDragMouseX = false;
            this.lastUpdateTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public final void onRelease(int button) {
        if (button == 0) {
            this.dragging = false;
            this.targetScale = 1.0f;
            this.targetTiltDegrees = 0.0f;
            this.smoothedMouseDeltaX = 0.0f;
            this.hasLastDragMouseX = false;
        }
    }
    public void setWidth(float width) {
        this.width = width;
    }
    public float getWidth() {
        return this.width;
    }
    public void setHeight(float height) {
        this.height = height;
    }
    public float getHeight() {
        return this.height;
    }
    public String getName() {
        return this.name;
    }
    public Module getModule() {
        return this.module;
    }
}

