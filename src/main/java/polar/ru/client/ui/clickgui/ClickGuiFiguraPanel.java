package polar.ru.client.ui.clickgui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.math.HoveringUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.api.utils.scissor.ScissorUtils;
import polar.ru.client.figura.FiguraAvatarManager;

public class ClickGuiFiguraPanel {
    private static final float ROW_HEIGHT = 20.0f;
    private static final float ROW_GAP = 10.0f;
    private static final float STATUS_HEIGHT = 11.0f;
    private static final float BUTTONS_HEIGHT = 20.0f;
    private static final float ACTION_CHIP_HEIGHT = 14.0f;
    private final List<FiguraAvatarManager.AvatarEntry> avatars = new ArrayList<FiguraAvatarManager.AvatarEntry>();
    private String status = "";
    private boolean applying;
    private int selectedIndex = -1;
    private int lastAvatarCount = -1;
    private float scroll;
    private long lastRefresh;

    public void refreshAvatars() {
        long now = System.currentTimeMillis();
        if (now - this.lastRefresh < 250L && !this.avatars.isEmpty()) {
            this.syncApplyingState();
            return;
        }
        this.lastRefresh = now;
        this.avatars.clear();
        if (!FiguraAvatarManager.isFiguraLoaded()) {
            if (!this.applying) {
                this.status = "Figura не загружена";
            }
            this.selectedIndex = -1;
            this.lastAvatarCount = 0;
            return;
        }
        this.avatars.addAll(FiguraAvatarManager.listInstalledAvatars());
        if (this.lastAvatarCount != this.avatars.size()) {
            this.scroll = 0.0f;
            this.lastAvatarCount = this.avatars.size();
        }
        if (this.selectedIndex >= this.avatars.size()) {
            this.selectedIndex = -1;
        }
        this.syncApplyingState();
        if (!this.applying && !FiguraAvatarManager.isApplying()) {
            if (this.avatars.isEmpty()) {
                this.status = "Нет моделей";
            } else if (this.isDefaultStatus(this.status)) {
                this.status = "Выбери и примени";
            }
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float panelX, float contentY, float contentHeight, int colorTheme, float alphaMul, int shadeColor) {
        float applyW;
        this.refreshAvatars();
        Font nameFont = Fonts.getFont("moe3", 14);
        Font smallFont = Fonts.getFont("suisse", 11);
        if (nameFont == null || smallFont == null) {
            return;
        }
        float listY = contentY + 11.0f;
        float listHeight = Math.max(40.0f, contentHeight - 11.0f - 20.0f - 4.0f);
        float buttonsY = contentY + contentHeight - 20.0f;
        smallFont.draw(context.getMatrices(), this.status, panelX + 8.0f, contentY + 2.0f, this.alpha(ColorUtils.rgba(180, 180, 190, 220), alphaMul));
        float totalListHeight = (float)this.avatars.size() * 30.0f;
        this.clampScroll(listHeight, totalListHeight);
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(panelX, listY, 470.0, listHeight + 1.0f);
        float rowY = listY + this.scroll;
        for (int i2 = 0; i2 < this.avatars.size(); ++i2) {
            FiguraAvatarManager.AvatarEntry entry = this.avatars.get(i2);
            if (rowY + 20.0f >= listY && rowY <= listY + listHeight) {
                boolean hovered = HoveringUtils.isHovered(mouseX, mouseY, panelX + 15.0f, rowY, 153.0, 20.0);
                boolean selected = i2 == this.selectedIndex;
                this.renderRow(context, panelX, rowY, entry.displayName(), selected, hovered, colorTheme, alphaMul, shadeColor, nameFont);
            }
            rowY += 30.0f;
        }
        ScissorUtils.pop();
        float chipH = 14.0f;
        float removeW = applyW = 75.0f;
        float applyX = panelX + 15.0f;
        float removeX = applyX + applyW + 3.0f;
        float chipY = buttonsY + 20.0f - chipH - 1.0f;
        boolean busy = this.applying || FiguraAvatarManager.isApplying();
        this.renderChip(context, smallFont, busy ? "..." : "Применить", applyX, chipY, applyW, chipH, mouseX, mouseY, colorTheme, alphaMul, busy);
        this.renderChip(context, smallFont, "Снять", removeX, chipY, removeW, chipH, mouseX, mouseY, colorTheme, alphaMul, false);
    }

    public boolean handleClick(double mouseX, double mouseY, int button, float panelX, float contentY, float contentHeight) {
        if (button != 0) {
            return false;
        }
        this.refreshAvatars();
        float listY = contentY + 11.0f;
        float listHeight = Math.max(40.0f, contentHeight - 11.0f - 20.0f - 4.0f);
        float buttonsY = contentY + contentHeight - 20.0f;
        float chipH = 14.0f;
        float applyW = 75.0f;
        float applyX = panelX + 15.0f;
        float removeX = applyX + applyW + 3.0f;
        float chipY = buttonsY + 20.0f - chipH - 1.0f;
        if (HoveringUtils.isHovered(mouseX, mouseY, applyX, chipY, applyW, chipH)) {
            return this.handleApplyClick();
        }
        if (HoveringUtils.isHovered(mouseX, mouseY, removeX, chipY, applyW, chipH)) {
            return this.handleRemoveClick();
        }
        if (!HoveringUtils.isHovered(mouseX, mouseY, panelX, listY, 470.0, listHeight)) {
            return false;
        }
        float rowY = listY + this.scroll;
        for (int i2 = 0; i2 < this.avatars.size(); ++i2) {
            if (rowY + 20.0f >= listY && rowY <= listY + listHeight && HoveringUtils.isHovered(mouseX, mouseY, panelX + 15.0f, rowY, 153.0, 20.0)) {
                this.selectedIndex = i2;
                if (!this.applying && !FiguraAvatarManager.isApplying()) {
                    this.status = "Выбрано: " + this.avatars.get(i2).displayName();
                }
                return true;
            }
            rowY += 30.0f;
        }
        return false;
    }

    public boolean handleScroll(double mouseX, double mouseY, float panelX, float contentY, float contentHeight, double verticalAmount) {
        float listY = contentY + 11.0f;
        float listHeight = Math.max(40.0f, contentHeight - 11.0f - 20.0f - 4.0f);
        if (!HoveringUtils.isHovered(mouseX, mouseY, panelX, listY, 470.0, listHeight)) {
            return false;
        }
        float totalListHeight = (float)this.avatars.size() * 30.0f;
        float maxScroll = Math.min(0.0f, listHeight - totalListHeight);
        this.scroll = Math.max(maxScroll, Math.min(0.0f, this.scroll + (float)(verticalAmount * 20.0)));
        return true;
    }

    private boolean handleApplyClick() {
        if (this.applying || FiguraAvatarManager.isApplying()) {
            return true;
        }
        if (!FiguraAvatarManager.isFiguraLoaded()) {
            this.status = "Figura не загружена";
            return true;
        }
        if (this.selectedIndex < 0 || this.selectedIndex >= this.avatars.size()) {
            this.status = "Выбери модель";
            return true;
        }
        FiguraAvatarManager.AvatarEntry selected = this.avatars.get(this.selectedIndex);
        this.applying = true;
        this.status = "Загрузка: " + selected.displayName();
        FiguraAvatarManager.applyAvatarAsync(selected, message -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                this.applying = false;
                return;
            }
            client.execute(() -> {
                this.status = message;
                this.applying = false;
            });
        });
        return true;
    }

    private boolean handleRemoveClick() {
        if (this.applying || FiguraAvatarManager.isApplying()) {
            return true;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        Runnable action = () -> {
            FiguraAvatarManager.removeAvatar();
            this.status = "Снято";
        };
        if (client == null) {
            action.run();
            return true;
        }
        if (client.isOnThread()) {
            action.run();
        } else {
            client.execute(action);
        }
        return true;
    }

    private void syncApplyingState() {
        this.applying = this.applying || FiguraAvatarManager.isApplying();
    }

    private boolean isDefaultStatus(String value) {
        return value == null || value.isBlank() || value.equals("Нет моделей") || value.equals("Выбери и примени") || value.equals("Figura не загружена") || value.startsWith("Выбрано: ");
    }

    private void renderRow(DrawContext context, float panelX, float rowY, String name, boolean selected, boolean hovered, int colorTheme, float alphaMul, int shadeColor, Font nameFont) {
        RenderUtils.drawBlur(context.getMatrices(), panelX + 15.0f, rowY - 0.5f, 153.0f, 21.0f, 3.0f, 8.0f, ColorUtils.rgba(0, 0, 0, 150));
        int bg = selected ? this.alpha(ColorUtils.setAlphaColor(colorTheme, 90), alphaMul) : (hovered ? this.alpha(ColorUtils.rgba(255, 255, 255, 18), alphaMul) : this.alpha(ColorUtils.rgba(0, 0, 0, 10), alphaMul));
        RenderUtils.drawRoundedRect(context.getMatrices(), panelX + 15.0f, rowY - 0.5f, 153.0f, 21.0f, 2.0f, bg);
        int textColor = selected ? this.alpha(-1, alphaMul) : this.alpha(ColorUtils.rgba(255, 255, 255, 200), alphaMul);
        this.drawClippedName(context, nameFont, name, panelX + 8.0f, rowY + 6.7f, textColor);
    }

    private void drawClippedName(DrawContext context, Font font, String name, float x2, float y2, int color) {
        Object text = name;
        float maxWidth = 139.0f;
        while (!((String)text).isEmpty() && font.getWidth((String)text) > maxWidth) {
            text = ((String)text).substring(0, ((String)text).length() - 1);
        }
        if (((String)text).length() < name.length() && ((String)text).length() > 1) {
            text = ((String)text).substring(0, ((String)text).length() - 1) + "…";
        }
        font.draw(context.getMatrices(), (String)text, x2, y2, color);
    }

    private void renderChip(DrawContext context, Font font, String label, float x2, float y2, float w2, float h2, int mouseX, int mouseY, int colorTheme, float alphaMul, boolean disabled) {
        boolean hovered;
        boolean bl = hovered = !disabled && HoveringUtils.isHovered(mouseX, mouseY, x2, y2, w2, h2);
        int bg = disabled ? this.alpha(ColorUtils.rgba(30, 30, 36, 180), alphaMul) : (hovered ? this.alpha(ColorUtils.setAlphaColor(colorTheme, 170), alphaMul) : this.alpha(ColorUtils.rgba(40, 40, 48, 220), alphaMul));
        RenderUtils.drawRoundedRect(context.getMatrices(), x2, y2, w2, h2, 2.0f, bg);
        float textX = x2 + (w2 - font.getWidth(label)) * 0.5f;
        float textY = y2 + h2 - font.getHeight() + 3.0f;
        font.draw(context.getMatrices(), label, textX, textY, this.alpha(disabled ? ColorUtils.rgba(180, 180, 190, 180) : -1, alphaMul));
    }

    private void clampScroll(float listHeight, float totalListHeight) {
        float maxScroll = Math.min(0.0f, listHeight - totalListHeight);
        this.scroll = Math.max(maxScroll, Math.min(0.0f, this.scroll));
    }

    private int alpha(int color, float alphaMul) {
        return ColorUtils.applyAlpha(color, alphaMul);
    }
}

