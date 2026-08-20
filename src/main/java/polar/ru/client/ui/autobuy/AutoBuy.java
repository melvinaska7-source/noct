package polar.ru.client.ui.autobuy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import polar.ru.api.QClient;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.api.utils.scissor.ScissorUtils;
import polar.ru.client.ui.autobuy.AutoBuyManager;
import polar.ru.client.ui.autobuy.ItemNames;

public class AutoBuy
extends Screen
implements QClient {
    private static final float WIDTH = 340.0f;
    private static final float HEIGHT = 226.0f;
    private static final float HEADER = 24.0f;
    private static final float PADDING = 8.0f;
    private static final float CELL = 20.0f;
    private static final float RIGHT_W = 118.0f;
    private static final float FIELD_HEIGHT = 16.0f;
    private final List<Item> allItems = new ArrayList<Item>();
    private final List<Item> filtered = new ArrayList<Item>();
    private String search = "";
    private String priceInput = "3000000";
    private boolean searchFocused;
    private boolean priceFocused;
    private static String savedPriceInput = "3000000";
    private Item selected;
    private Item hoveredItem;
    private float scroll;
    private float scrollTarget;
    private float openAnim;
    private float x;
    private float y;

    public AutoBuy() {
        super(Text.of((String)"AutoBuy"));
    }

    private Font font(int size) {
        return Fonts.getFont("suisse", size);
    }

    private void text(DrawContext ctx, String s2, float tx, float ty, int color, int size) {
        this.font(size).drawString(ctx.getMatrices(), s2, tx, ty, color);
    }

    private float width(String s2, int size) {
        return this.font(size).getStringWidth(s2);
    }

    protected void init() {
        this.allItems.clear();
        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) continue;
            this.allItems.add(item);
        }
        this.priceInput = savedPriceInput;
        this.applyFilter();
    }

    private void applyFilter() {
        this.filtered.clear();
        String q2 = this.search.toLowerCase().trim();
        String translated = ItemNames.toEnglishId(q2);
        for (Item item : this.allItems) {
            if (item == Items.AIR) continue;
            String itemName = item.getName().getString().toLowerCase();
            String itemId = Registries.ITEM.getId(item).getPath().toLowerCase();
            String ru = ItemNames.toRussian(item).toLowerCase();
            if (!q2.isEmpty() && !itemName.contains(q2) && !itemId.contains(q2) && !ru.contains(q2) && (translated.isEmpty() || !itemId.contains(translated) && !itemName.contains(translated))) continue;
            this.filtered.add(item);
        }
        this.scroll = 0.0f;
        this.scrollTarget = 0.0f;
    }

    private float gridX() {
        return this.x + 8.0f;
    }

    private float gridY() {
        return this.y + 24.0f + 8.0f - 2.0f;
    }

    private float gridW() {
        return 198.0f;
    }

    private float gridH() {
        return 166.0f;
    }

    private float listY() {
        return this.gridY() + 16.0f + 4.0f;
    }

    private float listH() {
        return this.gridH() - 20.0f;
    }

    private float rightX() {
        return this.x + 340.0f - 118.0f - 8.0f;
    }

    private float rightY() {
        return this.gridY();
    }

    private float rightH() {
        return this.gridH();
    }

    private float itemBoxY() {
        return this.rightY() + 18.0f;
    }

    private float priceLabelY() {
        return this.itemBoxY() + 32.0f;
    }

    private float priceFieldY() {
        return this.priceLabelY() + 13.0f;
    }

    private float buttonX() {
        return this.rightX() + 6.0f;
    }

    private float buttonW() {
        return 106.0f;
    }

    private float buttonH() {
        return 18.0f;
    }

    private float buttonY() {
        return this.rightY() + this.rightH() - this.buttonH() - 6.0f;
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.openAnim = MathHelper.lerp((float)Math.min(1.0f, delta * 0.35f), (float)this.openAnim, (float)1.0f);
        this.scroll = MathHelper.lerp((float)Math.min(1.0f, delta * 0.4f), (float)this.scroll, (float)this.scrollTarget);
        this.x = (float)this.width / 2.0f - 170.0f;
        this.y = (float)this.height / 2.0f - 113.0f;
        int theme = ColorUtils.getThemeColor();
        int dark = ColorUtils.darken(theme, 0.55f);
        RenderUtils.drawShadow(context.getMatrices(), this.x, this.y, 340.0f, 226.0f, 10.0f, 18.0f, ColorUtils.setAlphaColor(theme, (int)(70.0f * this.openAnim)));
        RenderUtils.drawLiquidGlass(context.getMatrices(), this.x, this.y, 340.0f, 226.0f, 10.0f, 1.0f, ColorUtils.rgba(255, 255, 255, (int)(255.0f * this.openAnim)));
        RenderUtils.drawRoundedRect(context.getMatrices(), this.x, this.y, 340.0f, 226.0f, 10.0f, ColorUtils.rgba(14, 16, 18, (int)(170.0f * this.openAnim)));
        RenderUtils.drawRoundedRectOutline(context.getMatrices(), this.x, this.y, 340.0f, 226.0f, 10.0f, 10.0f, 10.0f, 10.0f, 0.7f, ColorUtils.setAlphaColor(theme, 90), ColorUtils.setAlphaColor(theme, 40), ColorUtils.setAlphaColor(dark, 60), ColorUtils.setAlphaColor(dark, 30));
        this.renderHeader(context, mouseX, mouseY, theme, dark);
        this.renderGrid(context, mouseX, mouseY, theme, dark);
        this.renderRightPanel(context, mouseX, mouseY, theme, dark);
        if (this.hoveredItem != null) {
            this.renderTooltip(context, ItemNames.toRussian(this.hoveredItem), mouseX, mouseY, theme);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderTooltip(DrawContext context, String s2, int mouseX, int mouseY, int theme) {
        float w2 = this.width(s2, 13) + 10.0f;
        float h2 = 15.0f;
        float tx = mouseX + 8;
        float ty = mouseY - 4;
        if (tx + w2 > (float)this.width) {
            tx = (float)this.width - w2 - 2.0f;
        }
        RenderUtils.drawShadow(context.getMatrices(), tx, ty, w2, h2, 4.0f, 8.0f, ColorUtils.rgba(0, 0, 0, 160));
        RenderUtils.drawRoundedRect(context.getMatrices(), tx, ty, w2, h2, 4.0f, ColorUtils.rgba(18, 20, 22, 235));
        RenderUtils.drawRoundedRectOutline(context.getMatrices(), tx, ty, w2, h2, 4.0f, 4.0f, 4.0f, 4.0f, 0.6f, ColorUtils.setAlphaColor(theme, 140), ColorUtils.setAlphaColor(theme, 140), ColorUtils.setAlphaColor(theme, 140), ColorUtils.setAlphaColor(theme, 140));
        this.text(context, s2, tx + 5.0f, ty + (h2 - 9.0f) / 2.0f, ColorUtils.rgba(240, 240, 240, 245), 13);
    }

    private void renderHeader(DrawContext context, int mouseX, int mouseY, int theme, int dark) {
        RenderUtils.drawGradientRect(context.getMatrices(), this.x, this.y, 340.0f, 24.0f, 10.0f, 10.0f, 0.0f, 0.0f, ColorUtils.setAlphaColor(theme, 120), ColorUtils.setAlphaColor(dark, 90), ColorUtils.setAlphaColor(theme, 40), ColorUtils.setAlphaColor(dark, 30));
        float headSize = 14.0f;
        float headX = this.x + 8.0f;
        float headY = this.y + (24.0f - headSize) / 2.0f;
        RenderUtils.drawShadow(context.getMatrices(), headX, headY, headSize, headSize, 4.0f, 8.0f, ColorUtils.setAlphaColor(theme, 120));
        RenderUtils.drawPlayerHead(context.getMatrices(), mc.getSession() != null ? mc.getSession().getUsername() : "Steve", headX, headY, headSize, 4.0f, 1.0f, 0.0f);
        RenderUtils.drawRoundedRectOutline(context.getMatrices(), headX, headY, headSize, headSize, 4.0f, 4.0f, 4.0f, 4.0f, 0.6f, ColorUtils.setAlphaColor(theme, 160), ColorUtils.setAlphaColor(theme, 160), ColorUtils.setAlphaColor(dark, 120), ColorUtils.setAlphaColor(dark, 120));
        this.text(context, "autobuy section", headX + headSize + 6.0f, this.y + 7.5f, ColorUtils.rgba(235, 235, 235, 235), 13);
        float closeSize = 8.0f;
        float closeX = this.x + 340.0f - 8.0f - closeSize;
        float closeY = this.y + (24.0f - closeSize) / 2.0f;
        boolean closeHover = this.hovered(mouseX, mouseY, closeX - 3.0f, closeY - 3.0f, closeSize + 6.0f, closeSize + 6.0f);
        RenderUtils.drawRoundCircle(context.getMatrices(), closeX + closeSize / 2.0f, closeY + closeSize / 2.0f, closeSize, closeHover ? ColorUtils.setAlphaColor(theme, 220) : ColorUtils.rgba(120, 120, 120, 140));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderGrid(DrawContext context, int mouseX, int mouseY, int theme, int dark) {
        boolean caret;
        float gx = this.gridX();
        float gy = this.gridY();
        float gw = this.gridW();
        float listY = this.listY();
        float listH = this.listH();
        RenderUtils.drawRoundedRect(context.getMatrices(), gx, gy, gw, 16.0f, 4.0f, ColorUtils.rgba(0, 0, 0, 120));
        int outline = this.searchFocused ? ColorUtils.setAlphaColor(theme, 180) : ColorUtils.rgba(255, 255, 255, 35);
        RenderUtils.drawRoundedRectOutline(context.getMatrices(), gx, gy, gw, 16.0f, 4.0f, 4.0f, 4.0f, 4.0f, 0.6f, outline, outline, outline, outline);
        boolean bl = caret = System.currentTimeMillis() / 500L % 2L == 0L;
        String shown = this.search.isEmpty() && !this.searchFocused ? "Поиск предмета..." : this.search + (this.searchFocused && caret ? "|" : "");
        this.text(context, shown, gx + 6.0f, gy + 3.5f, this.search.isEmpty() && !this.searchFocused ? ColorUtils.rgba(180, 180, 180, 255) : ColorUtils.rgba(255, 255, 255, 255), 13);
        int columns = Math.max(1, (int)(gw / 20.0f));
        int rows = (int)Math.ceil((float)this.filtered.size() / (float)columns);
        float maxScroll = Math.max(0.0f, (float)rows * 20.0f - listH);
        this.scrollTarget = MathHelper.clamp((float)this.scrollTarget, (float)0.0f, (float)maxScroll);
        RenderUtils.drawRoundedRect(context.getMatrices(), gx, listY, gw, listH, 6.0f, ColorUtils.rgba(0, 0, 0, 70));
        this.hoveredItem = null;
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(gx, listY, gw, listH);
        try {
            float offsetX = (gw - (float)columns * 20.0f) / 2.0f;
            for (int i2 = 0; i2 < this.filtered.size(); ++i2) {
                boolean hover;
                int col = i2 % columns;
                int row = i2 / columns;
                float cx = gx + offsetX + (float)col * 20.0f;
                float cy = listY + (float)row * 20.0f - this.scroll;
                if (cy + 20.0f < listY - 4.0f || cy > listY + listH + 4.0f) continue;
                Item item = this.filtered.get(i2);
                boolean bl2 = hover = this.hovered(mouseX, mouseY, cx, cy, 20.0f, 20.0f) && (float)mouseY >= listY && (float)mouseY <= listY + listH;
                if (hover) {
                    this.hoveredItem = item;
                }
                if (item == this.selected) {
                    RenderUtils.drawRoundedRect(context.getMatrices(), cx + 1.0f, cy + 1.0f, 18.0f, 18.0f, 4.0f, ColorUtils.setAlphaColor(theme, 110));
                    RenderUtils.drawRoundedRectOutline(context.getMatrices(), cx + 1.0f, cy + 1.0f, 18.0f, 18.0f, 4.0f, 4.0f, 4.0f, 4.0f, 0.7f, ColorUtils.setAlphaColor(theme, 220), ColorUtils.setAlphaColor(theme, 220), ColorUtils.setAlphaColor(theme, 220), ColorUtils.setAlphaColor(theme, 220));
                } else if (hover) {
                    RenderUtils.drawRoundedRect(context.getMatrices(), cx + 1.0f, cy + 1.0f, 18.0f, 18.0f, 4.0f, ColorUtils.rgba(255, 255, 255, 28));
                }
                RenderUtils.drawHudItem(context, new ItemStack((ItemConvertible)item), cx + 2.0f, cy + 2.0f, 1.0f, 0.0f);
            }
        }
        finally {
            ScissorUtils.unset();
            ScissorUtils.pop();
        }
        if (maxScroll > 0.0f) {
            float barH = Math.max(14.0f, listH * (listH / ((float)rows * 20.0f)));
            float barY = listY + (listH - barH) * (this.scroll / maxScroll);
            RenderUtils.drawRoundedRect(context.getMatrices(), gx + gw - 3.0f, barY, 2.0f, barH, 1.0f, ColorUtils.setAlphaColor(theme, 190));
        }
    }

    private void renderRightPanel(DrawContext context, int mouseX, int mouseY, int theme, int dark) {
        float rx = this.rightX();
        float ry = this.rightY();
        float rh = this.rightH();
        RenderUtils.drawRoundedRect(context.getMatrices(), rx, ry, 118.0f, rh, 6.0f, ColorUtils.rgba(0, 0, 0, 80));
        RenderUtils.drawRoundedRectOutline(context.getMatrices(), rx, ry, 118.0f, rh, 6.0f, 6.0f, 6.0f, 6.0f, 0.6f, ColorUtils.rgba(255, 255, 255, 28), ColorUtils.rgba(255, 255, 255, 28), ColorUtils.rgba(255, 255, 255, 28), ColorUtils.rgba(255, 255, 255, 28));
        this.text(context, "Выбранный предмет", rx + 6.0f, ry + 6.0f, ColorUtils.rgba(200, 200, 200, 220), 12);
        float boxY = this.itemBoxY();
        RenderUtils.drawRoundedRect(context.getMatrices(), rx + 6.0f, boxY, 106.0f, 26.0f, 5.0f, ColorUtils.rgba(255, 255, 255, 18));
        if (this.selected != null) {
            RenderUtils.drawHudItem(context, new ItemStack((ItemConvertible)this.selected), rx + 11.0f, boxY + 5.0f, 1.0f, 0.0f);
            this.text(context, this.trim(ItemNames.toRussian(this.selected), 74.0f, 12), rx + 32.0f, boxY + 9.0f, ColorUtils.rgba(240, 240, 240, 240), 12);
        } else {
            this.text(context, "не выбран", rx + 11.0f, boxY + 9.0f, ColorUtils.rgba(150, 150, 150, 200), 12);
        }
        this.text(context, "Макс. цена", rx + 6.0f, this.priceLabelY(), ColorUtils.rgba(200, 200, 200, 220), 12);
        float fieldY = this.priceFieldY();
        RenderUtils.drawRoundedRect(context.getMatrices(), rx + 6.0f, fieldY, 106.0f, 16.0f, 4.0f, ColorUtils.rgba(0, 0, 0, 110));
        int pOutline = this.priceFocused ? ColorUtils.setAlphaColor(theme, 180) : ColorUtils.rgba(255, 255, 255, 30);
        RenderUtils.drawRoundedRectOutline(context.getMatrices(), rx + 6.0f, fieldY, 106.0f, 16.0f, 4.0f, 4.0f, 4.0f, 4.0f, 0.6f, pOutline, pOutline, pOutline, pOutline);
        boolean caret = System.currentTimeMillis() / 500L % 2L == 0L;
        long price = this.parsePrice();
        String priceDisplay = "$" + (price <= 0L ? "любая" : this.format(price)) + (this.priceFocused && caret ? "|" : "");
        this.text(context, priceDisplay, rx + 10.0f, fieldY + 4.0f, ColorUtils.rgba(240, 240, 240, 240), 12);
        String status = AutoBuyManager.isRunning() ? AutoBuyManager.getLastMessage() : (AutoBuyManager.getLastMessage().isEmpty() ? "Статус: ожидание" : AutoBuyManager.getLastMessage());
        this.text(context, this.trim(status, 106.0f, 11), rx + 6.0f, fieldY + 16.0f + 5.0f, ColorUtils.rgba(170, 170, 170, 210), 11);
        float bx = this.buttonX();
        float by = this.buttonY();
        float bw = this.buttonW();
        float bh = this.buttonH();
        boolean running = AutoBuyManager.isRunning();
        boolean enabled = running || this.selected != null;
        boolean hover = this.hovered(mouseX, mouseY, bx, by, bw, bh) && enabled;
        RenderUtils.drawShadow(context.getMatrices(), bx, by, bw, bh, 5.0f, hover ? 10.0f : 6.0f, ColorUtils.setAlphaColor(theme, hover ? 150 : 80));
        RenderUtils.drawGradientRect(context.getMatrices(), bx, by, bw, bh, 5.0f, ColorUtils.setAlphaColor(running ? ColorUtils.rgba(200, 60, 60, 255) : theme, enabled ? 235 : 90), ColorUtils.setAlphaColor(running ? ColorUtils.rgba(120, 30, 30, 255) : dark, enabled ? 235 : 90), true);
        String label = running ? "Стоп" : "Начать";
        this.text(context, label, bx + (bw - this.width(label, 13)) / 2.0f, by + (bh - 9.0f) / 2.0f, ColorUtils.rgba(255, 255, 255, enabled ? 255 : 150), 13);
    }

    private String trim(String textStr, float maxWidth, int size) {
        String result = textStr;
        while (this.width(result, size) > maxWidth && result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        return result.equals(textStr) ? textStr : result + "..";
    }

    private String format(long value) {
        return String.format("%,d", value).replace(' ', ',').replace(' ', ',');
    }

    private long parsePrice() {
        String digits = this.priceInput.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0L;
        }
        if (digits.length() > 17) {
            digits = digits.substring(0, 17);
        }
        try {
            return Long.parseLong(digits);
        }
        catch (NumberFormatException e2) {
            return 0L;
        }
    }

    private void setPrice(long value) {
        if (value < 0L) {
            value = 0L;
        }
        this.priceInput = Long.toString(value);
    }

    private boolean hovered(double mx, double my, float bx, float by, float bw, float bh) {
        return mx >= (double)bx && mx <= (double)(bx + bw) && my >= (double)by && my <= (double)(by + bh);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float closeSize = 8.0f;
        float closeX = this.x + 340.0f - 8.0f - closeSize;
        float closeY = this.y + (24.0f - closeSize) / 2.0f;
        if (this.hovered(mouseX, mouseY, closeX - 3.0f, closeY - 3.0f, closeSize + 6.0f, closeSize + 6.0f)) {
            this.close();
            return true;
        }
        this.searchFocused = this.hovered(mouseX, mouseY, this.gridX(), this.gridY(), this.gridW(), 16.0f);
        this.priceFocused = this.hovered(mouseX, mouseY, this.rightX() + 6.0f, this.priceFieldY(), 106.0f, 16.0f);
        if (this.hovered(mouseX, mouseY, this.gridX(), this.listY(), this.gridW(), this.listH())) {
            int columns = Math.max(1, (int)(this.gridW() / 20.0f));
            float offsetX = (this.gridW() - (float)columns * 20.0f) / 2.0f;
            int col = (int)((mouseX - (double)this.gridX() - (double)offsetX) / 20.0);
            int row = (int)((mouseY - (double)this.listY() + (double)this.scroll) / 20.0);
            int index = row * columns + col;
            if (col >= 0 && col < columns && index >= 0 && index < this.filtered.size() && mouseX >= (double)(this.gridX() + offsetX)) {
                this.selected = this.filtered.get(index);
                return true;
            }
        }
        if (this.hovered(mouseX, mouseY, this.buttonX(), this.buttonY(), this.buttonW(), this.buttonH())) {
            if (AutoBuyManager.isRunning()) {
                AutoBuyManager.stop("Статус: остановлено");
                return true;
            }
            if (this.selected != null) {
                AutoBuyManager.start(this.selected, this.parsePrice());
                this.close();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (this.hovered(mouseX, mouseY, this.rightX() + 6.0f, this.priceFieldY(), 106.0f, 16.0f)) {
            long step = AutoBuy.hasShiftDown() ? 1000000L : (AutoBuy.hasControlDown() ? 10000L : 100000L);
            this.setPrice(this.parsePrice() + (long)(vertical * (double)step));
            return true;
        }
        this.scrollTarget = (float)((double)this.scrollTarget - vertical * 22.0);
        return true;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (this.searchFocused) {
            this.search = this.search + chr;
            this.applyFilter();
            return true;
        }
        if (this.priceFocused) {
            if (Character.isDigit(chr)) {
                String digits = this.priceInput.replaceAll("[^0-9]", "");
                if (digits.length() < 17) {
                    this.priceInput = digits + chr;
                }
                return true;
            }
            if (chr == 'k' || chr == 'K' || chr == 'к' || chr == 'К') {
                this.setPrice(this.parsePrice() * 1000L);
                return true;
            }
            if (chr == 'm' || chr == 'M' || chr == 'м' || chr == 'М') {
                this.setPrice(this.parsePrice() * 1000000L);
                return true;
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        String clip;
        if (keyCode == 259) {
            if (this.searchFocused && !this.search.isEmpty()) {
                this.search = this.search.substring(0, this.search.length() - 1);
                this.applyFilter();
                return true;
            }
            if (this.priceFocused && !this.priceInput.isEmpty()) {
                this.priceInput = this.priceInput.substring(0, this.priceInput.length() - 1);
                return true;
            }
        }
        if (this.priceFocused && (keyCode == 265 || keyCode == 264)) {
            long step = AutoBuy.hasShiftDown() ? 1000000L : 100000L;
            this.setPrice(this.parsePrice() + (keyCode == 265 ? step : -step));
            return true;
        }
        if (Screen.isPaste((int)keyCode) && (clip = AutoBuy.mc.keyboard.getClipboard()) != null) {
            if (this.priceFocused) {
                this.setPrice(Long.parseLong("0" + clip.replaceAll("[^0-9]", "")));
                return true;
            }
            if (this.searchFocused) {
                this.search = this.search + clip;
                this.applyFilter();
                return true;
            }
        }
        if (keyCode == 257 || keyCode == 335) {
            if (this.priceFocused || this.searchFocused) {
                this.priceFocused = false;
                this.searchFocused = false;
                return true;
            }
            if (this.selected != null) {
                AutoBuyManager.start(this.selected, this.parsePrice());
                this.close();
                return true;
            }
        }
        if (keyCode == 256) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean shouldPause() {
        return false;
    }

    public void close() {
        savedPriceInput = this.priceInput;
        super.close();
    }
}

