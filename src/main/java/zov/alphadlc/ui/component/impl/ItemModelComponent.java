package zov.alphadlc.ui.component.impl;

import net.minecraft.client.util.math.MatrixStack;
import zov.alphadlc.module.settings.ItemModelSetting;
import zov.alphadlc.ui.component.Component;
import zov.alphadlc.util.cursor.CursorManager;
import zov.alphadlc.util.render.helper.HoverUtil;
import zov.alphadlc.util.render.msdf.Fonts;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;

import java.util.function.Consumer;

/**
 * Compact setting row. The model-heavy gallery is owned by ClickGuiFrame so it
 * can render and receive input above every panel.
 */
public class ItemModelComponent extends Component {
    private static final float ROW_HEIGHT = 18f;
    private static final float PADDING = 4.5f;

    private final ItemModelSetting setting;
    private final Consumer<ItemModelSetting> galleryOpener;

    public ItemModelComponent(ItemModelSetting setting, Consumer<ItemModelSetting> galleryOpener) {
        this.setting = setting;
        this.galleryOpener = galleryOpener;
        setHeight(ROW_HEIGHT + 2f);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        float alpha = Math.max(0f, Math.min(1f,
                getAlphaAnimSetting().getValue() * getAlphaAnim().getValue()));
        setHeight((ROW_HEIGHT + 2f) * getAlphaAnimSetting().getValue());
        if (alpha < 0.02f) return;

        float rowX = x + PADDING;
        float rowY = y + 1f;
        float rowW = width - PADDING * 2f;
        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, rowX, rowY, rowW, ROW_HEIGHT);
        if (hovered) CursorManager.requestHand();

        int alphaInt = (int) (255f * alpha);
        int border = hovered
                ? ColorProvider.setAlpha(ColorProvider.getColorClient(), (int) (150f * alpha))
                : ColorProvider.rgba(48, 66, 122, (int) (90f * alpha));
        DrawUtil.drawRound(rowX - 0.5f, rowY - 0.5f, rowW + 1f, ROW_HEIGHT + 1f,
                2.5f, border);
        DrawUtil.drawRound(rowX, rowY, rowW, ROW_HEIGHT, 2f,
                ColorProvider.setAlpha(ColorProvider.getColorInactiveButton(), alphaInt));

        DrawUtil.drawText(Fonts.SFREGULAR.get(), "Model", rowX + 4f, rowY + 5.1f,
                ColorProvider.setAlpha(ColorProvider.getColorText(), alphaInt), 7.5f);

        String value = fitValue(setting.getValue(), rowW - 42f);
        float valueWidth = Fonts.SFREGULAR.get().getWidth(value, 6.5f);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), value, rowX + rowW - valueWidth - 4f, rowY + 5.5f,
                ColorProvider.setAlpha(ColorProvider.getColorInactiveText(), alphaInt), 6.5f);
    }

    private String fitValue(String value, float maxWidth) {
        if (Fonts.SFREGULAR.get().getWidth(value, 6.5f) <= maxWidth) return value;
        String shortened = value;
        while (shortened.length() > 1
                && Fonts.SFREGULAR.get().getWidth(shortened + "...", 6.5f) > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "...";
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY,
                x + PADDING, y + 1f, width - PADDING * 2f, ROW_HEIGHT)) {
            galleryOpener.accept(setting);
        }
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
