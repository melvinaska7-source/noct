package polar.ru.api.storages.implement;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import polar.ru.api.QClient;
import polar.ru.api.events.EventInvoker;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.cmd.waypoint.Waypoint;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.math.MathUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;

public class WaypointStorage
implements QClient {
    private static final Identifier ARROW_TEXTURE = Identifier.of((String)"polar", (String)"textures/arrows/arrow.png");
    private final AnimationUtils alphaAnimation = new AnimationUtils(0.0f, 8.5f, Easings.CUBIC_OUT);
    private float animatedYaw;
    private Waypoint activeWaypoint = null;

    public WaypointStorage() {
        EventInvoker.register(this);
    }

    public void set(Waypoint waypoint) {
        this.activeWaypoint = waypoint;
    }

    public void remove(Waypoint waypoint) {
        if (this.activeWaypoint != null && this.activeWaypoint.equals(waypoint)) {
            this.activeWaypoint = null;
        }
    }

    public void clear() {
        this.activeWaypoint = null;
    }

    public boolean isEmpty() {
        return this.activeWaypoint == null;
    }

    @EventLink
    public void onRender2D(EventRender.Default event) {
        if (WaypointStorage.mc.player == null || WaypointStorage.mc.world == null) {
            return;
        }
        this.alphaAnimation.update(this.activeWaypoint == null ? 0.0f : 1.0f);
        float alpha = MathHelper.clamp((float)this.alphaAnimation.getValue(), (float)0.0f, (float)1.0f);
        if (this.activeWaypoint == null || alpha <= 0.02f) {
            return;
        }
        float centerX = (float)mc.getWindow().getScaledWidth() * 0.5f;
        float centerY = (float)mc.getWindow().getScaledHeight() * 0.25f;
        float size = 20.0f;
        double deltaX = this.activeWaypoint.getX() - WaypointStorage.mc.player.getX();
        double deltaZ = this.activeWaypoint.getZ() - WaypointStorage.mc.player.getZ();
        int distance = (int)MathUtils.round(MathHelper.sqrt((float)((float)(deltaX * deltaX + deltaZ * deltaZ))));
        float targetYaw = (float)(-Math.toDegrees(Math.atan2(deltaX, deltaZ))) - WaypointStorage.mc.gameRenderer.getCamera().getYaw();
        this.animatedYaw = this.interpolateAngle(this.animatedYaw, targetYaw, 0.18f);
        int color = ColorUtils.applyAlpha(ColorUtils.getThemeColor(), alpha);
        Font font = Fonts.getFont("sf_regular", 12);
        if (font != null) {
            String distanceText = distance + "m.";
            font.draw(event.getContext().getMatrices(), distanceText, centerX - font.getWidth(distanceText) * 0.5f + 1.5f, centerY + 7.5f, ColorUtils.applyAlpha(-1, alpha));
        }
        event.getContext().getMatrices().push();
        event.getContext().getMatrices().translate(centerX, centerY, 0.0f);
        event.getContext().getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.animatedYaw));
        event.getContext().getMatrices().translate(-centerX, -centerY, 0.0f);
        float drawX = centerX - size * 0.5f;
        float drawY = centerY - size * 0.5f;
        RenderUtils.drawImage(event.getContext().getMatrices(), ARROW_TEXTURE, drawX, drawY, size, size, color);
        event.getContext().getMatrices().pop();
    }

    private float interpolateAngle(float current, float target, float factor) {
        float delta = MathHelper.wrapDegrees((float)(target - current));
        return current + delta * factor;
    }
    public Waypoint getActiveWaypoint() {
        return this.activeWaypoint;
    }
}

