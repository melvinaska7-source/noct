package polar.ru.client.modules.impl.render.base.implement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.api.utils.scissor.ScissorUtils;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;
import polar.ru.client.modules.impl.render.base.implement.HudFx;
import polar.ru.polar;

public class StaffList
extends InterfaceProcessing {
    private static final float BASE_MIN_WIDTH = 64.0f;
    private static final float EXTRA_WIDTH = 0.0f;
    private static final float ROW_RIGHT_MARGIN = 25.0f;
    private static final float ROW_HEIGHT = 10.0f;
    private static final float HEADER_HEIGHT = 16.0f;
    private static final float HEADER_GAP = 0.2f;
    private static final float CONTENT_PAD_TOP = 6.0f;
    private static final float CONTENT_PAD_BOTTOM = 0.8f;
    private static final int STATUS_VANISH_COLOR = -47526;
    private static final int STATUS_GM3_COLOR = -9146;
    private static final int STATUS_ONLINE_COLOR = -10158216;
    private static final int STATUS_NEAR_COLOR = -10170369;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<String, StaffData> staffDataCache = new LinkedHashMap<String, StaffData>();
    private final Map<String, AnimationUtils> staffAnimations = new HashMap<String, AnimationUtils>();
    private final Set<String> activeStaff = new HashSet<String>();
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    private final Pattern botPattern = Pattern.compile("^\\d+$");
    private final Set<String> validStaffPrefixes = new HashSet<String>();
    private final AnimationUtils widthAnimation = new AnimationUtils(60.0f, 10.5f, Easings.QUAD_OUT);
    private final AnimationUtils heightAnimation = new AnimationUtils(16.0f, 10.5f, Easings.QUAD_OUT);
    private final AnimationUtils panelAlphaAnimation = HudFx.newAppearAnimation();
    private long lastStaffUpdate = 0L;
    private final List<String> visiblePlayers = new ArrayList<String>();
    private final Set<String> animationScratch = new HashSet<String>();
    private Font font12;
    private Font font13;
    private Font font14;
    private Font iconFont;

    private Font issue(int size) {
        return Fonts.getFont("suisse", size);
    }

    private Font icons(int size) {
        return Fonts.getFont("icon", size);
    }

    public StaffList(Draggable draggable) {
        super(draggable);
        this.validStaffPrefixes.addAll(Arrays.asList("mod", "der", "мод", "модер", "модератор", "moder", "moderator", "ml. moder", "мл. модер", "ml moder", "moder+", "модер+", "st. moder", "ст. модер", "st moder", "старший модер", "gl. moder", "гл. модер", "gl moder", "главный модер", "adm", "адм", "админ", "admin", "administrator", "ml. admin", "мл. админ", "ml admin", "владе", "owner", "wne", "supp", "ꜱupp", "support", "помо", "помощ", "помощник", "d. helper", "дежурный", "helper", "хелпер", "dev", "раз", "разработчик", "developer", "таф", "taf", "staff", "стафф", "сотрудник", "curat", "курато", "куратор", "yt", "ютуб", "youtube", "стажер", "trainee", "отри"));
    }

    private AnimationUtils getAnimation(String name) {
        return this.staffAnimations.computeIfAbsent(name, n2 -> new AnimationUtils(0.0f, 10.5f, Easings.QUAD_OUT));
    }

    private void initFonts() {
        if (this.font12 == null) {
            this.font12 = Fonts.getFont("suisse", 12);
            this.font13 = Fonts.getFont("suisse", 13);
            this.font14 = Fonts.getFont("suisse", 14);
            this.iconFont = Fonts.getFont("icon", 13);
        }
    }

    private boolean isBot(String name) {
        return this.botPattern.matcher(name).matches();
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        if (this.mc.player == null || this.mc.world == null) {
            return;
        }
        this.initFonts();
        long now = System.currentTimeMillis();
        if (now - this.lastStaffUpdate > 500L) {
            this.updateStaffCache();
            this.lastStaffUpdate = now;
        }
        this.updateAnimations();
        List<String> visible = this.getVisiblePlayers();
        boolean hasVisibleStaff = false;
        for (String name : visible) {
            AnimationUtils anim = this.getAnimation(name);
            if (!(anim.getValue() > 0.01f)) continue;
            hasVisibleStaff = true;
            break;
        }
        boolean isChatOpen = this.mc != null && this.mc.currentScreen instanceof ChatScreen;
        boolean shouldShowPanel = hasVisibleStaff || isChatOpen;
        this.panelAlphaAnimation.update(shouldShowPanel ? 1.0f : 0.0f);
        if (this.panelAlphaAnimation.getValue() <= 0.01f) {
            this.draggable.setWidth(0.0f);
            this.draggable.setHeight(0.0f);
            return;
        }
        this.renderDefaultStyle(eventRender);
        super.onRender(eventRender);
    }

    private boolean matchesStaffPrefix(String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String p2 : this.validStaffPrefixes) {
            if (!lower.contains(p2)) continue;
            return true;
        }
        return false;
    }

    private void updateStaffCache() {
        this.activeStaff.clear();
        String selfName = this.mc.player.getName().getString();
        for (Team team : this.mc.world.getScoreboard().getTeams()) {
            boolean isNear;
            String name;
            Collection players = team.getPlayerList();
            if (players.size() != 1 || !this.namePattern.matcher(name = (String)players.iterator().next()).matches() || this.isBot(name) || name.equals(selfName)) continue;
            PlayerListEntry info = this.mc.getNetworkHandler().getPlayerListEntry(name);
            boolean vanish = info == null;
            boolean isGM3 = info != null && info.getGameMode() == GameMode.SPECTATOR;
            Text prefixText = team.getPrefix();
            if (!this.matchesStaffPrefix(prefixText.getString()) && !vanish && !isGM3 && !polar.INSTANCE.staffStorage.isStaff(name)) continue;
            this.activeStaff.add(name);
            boolean bl = isNear = !vanish && !isGM3 && this.isPlayerNearby(name);
            String status = vanish ? "VANISH" : (isGM3 ? "GM3" : (isNear ? "NEAR" : "ONLINE"));
            StaffData existing = this.staffDataCache.computeIfAbsent(name, n2 -> new StaffData(status));
            existing.status = status;
            existing.segments = new ArrayList<PrefixSegment>();
            this.calculateWidths(existing, name);
        }
    }

    private boolean isPlayerNearby(String name) {
        if (this.mc.world == null) {
            return false;
        }
        for (AbstractClientPlayerEntity player : this.mc.world.getPlayers()) {
            if (!player.getName().getString().equals(name)) continue;
            return true;
        }
        return false;
    }

    private void calculateWidths(StaffData data, String name) {
        float avatarSize = 8.0f;
        float avatarPadding = 3.0f;
        data.prefixWidth12 = 0.0f;
        data.nameWidth12 = this.issue(12).getWidth(name) + avatarSize + avatarPadding;
    }

    private void updateAnimations() {
        this.animationScratch.clear();
        this.animationScratch.addAll(this.staffAnimations.keySet());
        this.animationScratch.addAll(this.activeStaff);
        for (String name : this.animationScratch) {
            this.getAnimation(name).update(this.activeStaff.contains(name) ? 1.0f : 0.0f);
        }
    }

    private List<String> getVisiblePlayers() {
        this.visiblePlayers.clear();
        for (Map.Entry<String, AnimationUtils> e2 : this.staffAnimations.entrySet()) {
            if (!(e2.getValue().getValue() > 0.01f)) continue;
            this.visiblePlayers.add(e2.getKey());
        }
        Collections.sort(this.visiblePlayers);
        return this.visiblePlayers;
    }

    private int getStatusColor(String status) {
        return switch (status) {
            case "VANISH" -> -47526;
            case "GM3" -> -9146;
            case "NEAR" -> -10170369;
            default -> -10158216;
        };
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "VANISH" -> "Vanish";
            case "GM3" -> "GM3";
            case "NEAR" -> "Near";
            default -> "Online";
        };
    }

    private void renderDefaultStyle(EventRender.Default eventRender) {
        float baseX = this.draggable.getX();
        float y2 = this.draggable.getY();
        MatrixStack matrices = eventRender.getContext().getMatrices();
        int colorTheme = this.getStableThemeColor();
        List<String> visible = this.getVisiblePlayers();
        float targetWidth = 64.0f;
        int visibleCount = 0;
        for (String name : visible) {
            StaffData data;
            AnimationUtils anim = this.getAnimation(name);
            if (anim.getValue() <= 0.01f || (data = this.staffDataCache.get(name)) == null) continue;
            ++visibleCount;
            String statusLabel = this.getStatusLabel(data.status);
            float statusWidth = this.issue(10).getWidth(statusLabel) + 4.0f;
            float rowWidth = data.nameWidth12 + 4.0f + statusWidth + 25.0f + 5.0f;
            if (!(rowWidth > targetWidth)) continue;
            targetWidth = rowWidth;
        }
        float targetHeight = 22.2f + (float)visibleCount * 10.0f + 0.8f;
        this.widthAnimation.update(targetWidth);
        this.heightAnimation.update(targetHeight);
        float width = this.widthAnimation.getValue() + 0.0f;
        float height = this.heightAnimation.getValue();
        float rightEdge = baseX + width;
        float x2 = baseX;
        float pivotX = x2 + width / 2.0f;
        float pivotY = y2 + height / 2.0f;
        float eased = HudFx.pushTransform(matrices, this.panelAlphaAnimation.getValue() * 0.5f + 0.5f, pivotX, pivotY);
        int panelAlphaMul = (int)(255.0f * eased);
        if (this.glassSettings.enabled.isState()) {
            this.glassSettings.drawGlass(matrices, x2, y2, width, height, colorTheme);
        } else if (this.isFlatStyle()) {
            int bgColor = ColorUtils.rgba(20, 20, 20, 255);
            RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, 6.0f, bgColor);
        } else {
            int shadowColor = ColorUtils.rgba(0, 0, 0, (int)(200.0f * eased));
            RenderUtils.drawShadow(matrices, x2 - 2.0f, y2 - 2.0f, width + 4.0f, height + 4.0f, 6.0f, shadowColor);
            int bgColor = ColorUtils.rgba(20, 20, 20, (int)(100.0f * eased));
            RenderUtils.drawBlur(matrices, x2, y2, width, height, 6.0f, 5.0f, ColorUtils.rgba(255, 255, 255, panelAlphaMul));
            RenderUtils.drawBlur(matrices, x2, y2, width, height, 6.0f, 5.0f, ColorUtils.rgba(0, 0, 0, (int)(180.0f * eased)));
            RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, 6.0f, bgColor);
            float blueLineWidth = width * 0.4f - 5.0f;
            float blueLineX = x2 + (width - blueLineWidth) / 2.0f + 13.0f;
            int themeLineColor = ColorUtils.setAlphaColor(colorTheme, panelAlphaMul);
            RenderUtils.drawRoundedRect(matrices, blueLineX, y2 - 1.5f, blueLineWidth, 3.5f, 1.0f, themeLineColor);
        }
        Font headerIconFont = this.icons(14);
        String headerIconGlyph = "F";
        float headerIconDrawX = rightEdge - 12.0f;
        float headerIconDrawY = y2 + 7.0f;
        float headerTextY = y2 + (22.2f - this.issue(14).getHeight()) / 2.0f;
        float headerIconY = y2 + (22.2f - this.icons(14).getHeight()) / 2.0f;
        this.issue(14).draw(matrices, "Staffs", x2 + 5.2f, headerTextY, ColorUtils.rgba(255, 255, 255, panelAlphaMul));
        if (this.isFlatStyle()) {
            float headerIconBgSize = 11.0f;
            float headerIconBgX = rightEdge - 14.0f;
            float headerIconBgY = y2 + (22.2f - headerIconBgSize) / 2.0f;
            int headerIconBgColor = ColorUtils.setAlphaColor(colorTheme, 63);
            RenderUtils.drawRoundedRect(matrices, headerIconBgX, headerIconBgY, headerIconBgSize, headerIconBgSize, 2.0f, headerIconBgColor);
        }
        this.icons(14).draw(matrices, "F", rightEdge - 12.0f, headerIconY, ColorUtils.setAlphaColor(colorTheme, panelAlphaMul));
        float offsetY = 22.2f;
        for (String name : visible) {
            StaffData data;
            AnimationUtils anim = this.getAnimation(name);
            float animValue = anim.getValue();
            if (animValue <= 0.01f || (data = this.staffDataCache.get(name)) == null) continue;
            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(x2, y2, width, height);
            int alpha = (int)(255.0f * animValue * eased);
            int textColor = ColorUtils.rgba(255, 255, 255, alpha);
            float avatarSize = 8.0f;
            float rowBgY = y2 + offsetY - 6.0f;
            float rowBgHeight = 16.8f;
            float avatarX = x2 + 4.0f;
            float avatarY = rowBgY + (rowBgHeight - avatarSize) / 2.0f;
            float nameX = avatarX + avatarSize + 3.0f;
            String statusLabel = this.getStatusLabel(data.status);
            float statusBoxWidth = Math.max(this.issue(10).getStringWidth(statusLabel) + 4.0f, 9.0f);
            float statusBoxX = nameX + this.issue(13).getStringWidth(name) + 4.0f;
            float rowEndX = statusBoxX + statusBoxWidth + 3.0f;
            float rowContentWidth = rowEndX - x2;
            int contentBgColor = ColorUtils.rgba(20, 20, 20, (int)(100.0f * eased));
            if (!this.isFlatStyle()) {
                RenderUtils.drawBlur(matrices, x2, rowBgY, rowContentWidth, rowBgHeight, 6.0f, 5.0f, ColorUtils.rgba(255, 255, 255, panelAlphaMul));
                RenderUtils.drawBlur(matrices, x2, rowBgY, rowContentWidth, rowBgHeight, 6.0f, 5.0f, ColorUtils.rgba(0, 0, 0, (int)(180.0f * eased)));
            }
            RenderUtils.drawRoundedRect(matrices, x2, rowBgY, rowContentWidth, rowBgHeight, 6.0f, contentBgColor);
            if (this.mc != null && this.mc.world != null) {
                PlayerListEntry playerEntry = null;
                if (this.mc.getNetworkHandler() != null) {
                    for (PlayerListEntry entry : this.mc.getNetworkHandler().getPlayerList()) {
                        if (!entry.getProfile().getName().equals(name)) continue;
                        playerEntry = entry;
                        break;
                    }
                }
                if (playerEntry != null) {
                    RenderUtils.drawPlayerHead(matrices, playerEntry.getProfile().getId(), avatarX, avatarY, avatarSize, 1.5f, animValue, 0.0f);
                } else {
                    float iconFallbackY = rowBgY + (rowBgHeight - this.icons(12).getHeight()) / 2.0f;
                    this.icons(12).draw(matrices, "e", avatarX, iconFallbackY, ColorUtils.setAlphaColor(colorTheme, alpha));
                }
            }
            float rowTextY = rowBgY + (rowBgHeight - this.issue(13).getHeight()) / 2.0f;
            float rowStatusY = rowBgY + (rowBgHeight - this.issue(12).getHeight()) / 2.0f;
            this.issue(13).draw(matrices, name, nameX, rowTextY, textColor);
            float statusBoxH = 9.5f;
            float statusBoxY = rowBgY + (rowBgHeight - statusBoxH) / 2.0f;
            RenderUtils.drawBlur(matrices, statusBoxX - 0.25f, statusBoxY, statusBoxWidth + 0.5f, statusBoxH, 1.5f, 5.0f, ColorUtils.rgba(255, 255, 255, 255));
            RenderUtils.drawBlur(matrices, statusBoxX - 0.25f, statusBoxY, statusBoxWidth + 0.5f, statusBoxH, 1.5f, 5.0f, ColorUtils.rgba(0, 0, 0, 180));
            int statusColor = this.getStatusColor(data.status);
            this.issue(12).drawCenteredString(matrices, statusLabel, statusBoxX + statusBoxWidth / 2.0f, rowStatusY, ColorUtils.setAlphaColor(statusColor, alpha));
            offsetY += 10.0f * animValue;
            ScissorUtils.pop();
            ScissorUtils.unset();
        }
        HudFx.popTransform(matrices);
        this.draggable.setWidth(width);
        this.draggable.setHeight(height);
    }

    private int getStableThemeColor() {
        if (!polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor();
    }

    private static class StaffData {
        String status;
        List<PrefixSegment> segments;
        float prefixWidth12;
        float nameWidth12;

        StaffData(String status) {
            this.status = status;
            this.segments = new ArrayList<PrefixSegment>();
        }
    }

    private static class PrefixSegment {
        final String text;
        final int color;
        float width12;

        PrefixSegment(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }
}

