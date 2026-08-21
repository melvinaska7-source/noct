package zov.alphadlc.ui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import zov.alphadlc.module.settings.ItemModelSetting;
import zov.alphadlc.util.cursor.CursorManager;
import zov.alphadlc.util.render.helper.HoverUtil;
import zov.alphadlc.util.render.math.Scissor;
import zov.alphadlc.util.render.msdf.Fonts;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Screen-level, virtualized model picker used by Item Replacer.
 */
public class ItemModelGalleryPopup implements IComponent {
    private static final int COLUMNS = 6;
    private static final float WIDTH = 380f;
    private static final float MAX_HEIGHT = 292f;
    private static final float PADDING = 10f;
    private static final float CELL_GAP = 4f;
    private static final float CELL_HEIGHT = 45f;
    private static final float ROW_PITCH = CELL_HEIGHT + CELL_GAP;

    private final ItemModelSetting setting;
    private final SearchField searchField = new SearchField("Search models...");
    private final List<String> filteredModels = new ArrayList<>();

    private String appliedQuery = "";
    private String hoveredModel;
    private float x;
    private float y;
    private float width;
    private float height;
    private float gridX;
    private float gridY;
    private float gridWidth;
    private float gridHeight;
    private float scroll;

    public ItemModelGalleryPopup(ItemModelSetting setting) {
        this.setting = setting;
        filteredModels.addAll(setting.getModes());
        searchField.resetAppear();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        width = Math.min(WIDTH, screenWidth - 20f);
        height = Math.min(MAX_HEIGHT, screenHeight - 20f);
        x = (screenWidth - width) / 2f;
        y = (screenHeight - height) / 2f;

        updateFilter();

        DrawUtil.drawRound(0, 0, screenWidth, screenHeight, 0,
                ColorProvider.rgba(0, 0, 0, 95));
        DrawUtil.drawRoundBlur(x, y, width, height, 7f,
                ColorProvider.rgba(200, 200, 200, 255), 16f);
        DrawUtil.drawRound(x - 1f, y - 1f, width + 2f, height + 2f, 7.5f,
                ColorProvider.rgba(48, 66, 122, 145));
        DrawUtil.drawRound(x, y, width, height, 7f,
                ColorProvider.setAlpha(ColorProvider.getColorClickGui(), 235));

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Item Replacer models", x + PADDING, y + 9f,
                ColorProvider.getColorText(), 9f);
        String count = filteredModels.size() + " / " + setting.getModes().size();
        float countWidth = Fonts.SFREGULAR.get().getWidth(count, 7f);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), count, x + width - PADDING - countWidth, y + 10f,
                ColorProvider.getColorInactiveText(), 7f);

        searchField.setBounds(x + PADDING, y + 25f, width - PADDING * 2f, 18f);
        searchField.render(context, mouseX, mouseY, delta);

        gridX = x + PADDING;
        gridY = y + 50f;
        gridWidth = width - PADDING * 2f;
        gridHeight = Math.max(1f, height - 75f);
        hoveredModel = null;

        Scissor.push();
        Scissor.setFromComponentCoordinates(gridX, gridY, gridWidth, gridHeight);
        renderVisibleRows(context, mouseX, mouseY);
        Scissor.unset();
        Scissor.pop();

        renderScrollbar();
        renderFooter();
    }

    private void updateFilter() {
        String query = searchField.text.trim().toLowerCase(Locale.ROOT);
        if (query.equals(appliedQuery)) return;
        appliedQuery = query;
        filteredModels.clear();
        for (String model : setting.getModes()) {
            if (query.isEmpty() || model.toLowerCase(Locale.ROOT).contains(query)) {
                filteredModels.add(model);
            }
        }
        scroll = 0f;
    }

    private void renderVisibleRows(DrawContext context, int mouseX, int mouseY) {
        if (filteredModels.isEmpty()) {
            String empty = "No matching models";
            float textWidth = Fonts.SFREGULAR.get().getWidth(empty, 8f);
            DrawUtil.drawText(Fonts.SFREGULAR.get(), empty,
                    gridX + (gridWidth - textWidth) / 2f, gridY + gridHeight / 2f - 4f,
                    ColorProvider.getColorInactiveText(), 8f);
            return;
        }

        float maxScroll = maxScroll();
        scroll = MathHelper.clamp(scroll, 0f, maxScroll);
        float cellWidth = (gridWidth - CELL_GAP * (COLUMNS - 1)) / COLUMNS;
        int rowCount = rowCount();
        int firstRow = Math.max(0, (int) Math.floor(scroll / ROW_PITCH));
        int lastRow = Math.min(rowCount - 1,
                (int) Math.floor((scroll + gridHeight - 0.001f) / ROW_PITCH));

        for (int row = firstRow; row <= lastRow; row++) {
            float cellY = gridY + row * ROW_PITCH - scroll;
            for (int column = 0; column < COLUMNS; column++) {
                int index = row * COLUMNS + column;
                if (index >= filteredModels.size()) break;
                String model = filteredModels.get(index);
                float cellX = gridX + column * (cellWidth + CELL_GAP);
                renderCell(context, model, cellX, cellY, cellWidth, mouseX, mouseY);
            }
        }
    }

    private void renderCell(DrawContext context, String model, float cellX, float cellY,
                            float cellWidth, int mouseX, int mouseY) {
        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, cellX, cellY, cellWidth, CELL_HEIGHT)
                && HoverUtil.isHovered(mouseX, mouseY, gridX, gridY, gridWidth, gridHeight);
        boolean selected = setting.is(model);
        if (hovered) {
            hoveredModel = model;
            CursorManager.requestHand();
        }

        int outline = selected
                ? ColorProvider.setAlpha(ColorProvider.getColorClient(), 255)
                : hovered
                ? ColorProvider.rgba(100, 130, 220, 180)
                : ColorProvider.rgba(48, 66, 122, 90);
        float outlineSize = selected ? 1.5f : 0.75f;
        DrawUtil.drawRound(cellX - outlineSize, cellY - outlineSize,
                cellWidth + outlineSize * 2f, CELL_HEIGHT + outlineSize * 2f,
                4f, outline);
        DrawUtil.drawRound(cellX, cellY, cellWidth, CELL_HEIGHT, 3f,
                ColorProvider.setAlpha(hovered
                        ? ColorProvider.getColorButton()
                        : ColorProvider.getColorInactiveButton(), 205));

        ItemStack preview = setting.getPreviewStack(model);
        if (preview != null) {
            float iconSize = 31f;
            float scale = iconSize / 16f;
            context.getMatrices().push();
            context.getMatrices().translate(cellX + (cellWidth - iconSize) / 2f,
                    cellY + (CELL_HEIGHT - iconSize) / 2f, 100f);
            context.getMatrices().scale(scale, scale, 1f);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            context.drawItem(preview, 0, 0);
            context.getMatrices().pop();
        }
    }

    private void renderScrollbar() {
        float maxScroll = maxScroll();
        if (maxScroll <= 0f) return;
        float contentHeight = rowCount() * ROW_PITCH - CELL_GAP;
        float trackX = x + width - 4f;
        float thumbHeight = Math.max(18f, gridHeight * (gridHeight / contentHeight));
        float thumbY = gridY + (gridHeight - thumbHeight) * (scroll / maxScroll);
        DrawUtil.drawRound(trackX, gridY, 1.5f, gridHeight, 0.75f,
                ColorProvider.rgba(48, 66, 122, 100));
        DrawUtil.drawRound(trackX, thumbY, 1.5f, thumbHeight, 0.75f,
                ColorProvider.setAlpha(ColorProvider.getColorClient(), 210));
    }

    private void renderFooter() {
        String label = hoveredModel != null ? hoveredModel : "Esc or click outside to close";
        float textWidth = Fonts.SFREGULAR.get().getWidth(label, 7f);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), label,
                x + (width - textWidth) / 2f, y + height - 14f,
                hoveredModel != null
                        ? ColorProvider.getColorText()
                        : ColorProvider.getColorInactiveText(), 7f);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        searchField.mouseClicked(mouseX, mouseY, button);
        if (button != 0 || !HoverUtil.isHovered(mouseX, mouseY,
                gridX, gridY, gridWidth, gridHeight)) return;

        float cellWidth = (gridWidth - CELL_GAP * (COLUMNS - 1)) / COLUMNS;
        int row = (int) Math.floor((mouseY - gridY + scroll) / ROW_PITCH);
        int column = (int) Math.floor((mouseX - gridX) / (cellWidth + CELL_GAP));
        if (column < 0 || column >= COLUMNS) return;

        float localX = (float) (mouseX - gridX - column * (cellWidth + CELL_GAP));
        float localY = (float) (mouseY - gridY + scroll - row * ROW_PITCH);
        if (localX > cellWidth || localY > CELL_HEIGHT) return;

        int index = row * COLUMNS + column;
        if (index >= 0 && index < filteredModels.size()) {
            setting.setValue(filteredModels.get(index));
        }
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY,
                              double horizontalAmount, double verticalAmount) {
        if (HoverUtil.isHovered(mouseX, mouseY, gridX, gridY, gridWidth, gridHeight)) {
            scroll = MathHelper.clamp(scroll - (float) verticalAmount * 28f, 0f, maxScroll());
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        searchField.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        searchField.charTyped(chr, modifiers);
    }

    public boolean contains(double mouseX, double mouseY) {
        return HoverUtil.isHovered(mouseX, mouseY, x, y, width, height);
    }

    private int rowCount() {
        return (filteredModels.size() + COLUMNS - 1) / COLUMNS;
    }

    private float maxScroll() {
        float contentHeight = Math.max(0f, rowCount() * ROW_PITCH - CELL_GAP);
        return Math.max(0f, contentHeight - gridHeight);
    }
}
