package polar.ru.client.ui.emotes;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import polar.ru.api.QClient;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.client.modules.impl.render.Emotes;

public class EmotesScreen
extends Screen
implements QClient {
    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 2;
    private static final float BUTTON_WIDTH = 120.0f;
    private static final float BUTTON_HEIGHT = 60.0f;
    private static final float BUTTON_SPACING = 10.0f;
    private static final float BUTTON_RADIUS = 8.0f;
    private float startX;
    private float startY;
    private int hoveredButton = -1;
    private final List<EmoteButton> buttons = new ArrayList<EmoteButton>();

    public EmotesScreen() {
        super(Text.of((String)"Emotes"));
        this.initializeButtons();
    }

    private void initializeButtons() {
        Emotes.EmoteType[] emotes = Emotes.EmoteType.values();
        for (int i2 = 0; i2 < emotes.length; ++i2) {
            int row = i2 / 4;
            int col = i2 % 4;
            this.buttons.add(new EmoteButton(emotes[i2], row, col, i2));
        }
    }

    protected void init() {
        super.init();
        float totalWidth = 510.0f;
        float totalHeight = 130.0f;
        this.startX = ((float)this.width - totalWidth) / 2.0f;
        this.startY = ((float)this.height - totalHeight) / 2.0f;
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, -1073741824, -1073741824);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        this.hoveredButton = this.getHoveredButton(mouseX, mouseY);
        this.renderEmoteButtons(context, mouseX, mouseY);
        this.renderTitle(context);
        RenderSystem.disableBlend();
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderEmoteButtons(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrices = context.getMatrices();
        for (int i2 = 0; i2 < this.buttons.size(); ++i2) {
            int borderColor;
            EmoteButton button = this.buttons.get(i2);
            boolean hovered = i2 == this.hoveredButton;
            float x2 = this.startX + (float)button.col * 130.0f;
            float y2 = this.startY + (float)button.row * 70.0f;
            int bgColor = hovered ? ColorUtils.getThemeColor() : ColorUtils.rgba(50, 50, 70, 220);
            int n2 = borderColor = hovered ? -1 : ColorUtils.rgba(100, 100, 120, 255);
            if (hovered) {
                RenderUtils.drawShadow(matrices, x2, y2, 120.0f, 60.0f, 8.0f, ColorUtils.rgba(0, 0, 0, 100));
            }
            RenderUtils.drawGradientRect(matrices, x2, y2, 120.0f, 60.0f, 8.0f, bgColor, ColorUtils.darken(bgColor, 0.8f), true);
            String name = button.emote.getDisplayName();
            int textWidth = EmotesScreen.mc.textRenderer.getWidth(name);
            int textColor = hovered ? -1 : -3355444;
            TextRenderer PackResourceMetadata = EmotesScreen.mc.textRenderer;
            int n3 = (int)(x2 + 60.0f - (float)textWidth / 2.0f);
            Objects.requireNonNull(EmotesScreen.mc.textRenderer);
            context.drawText(PackResourceMetadata, name, n3, (int)(y2 + 30.0f - 9.0f / 2.0f), textColor, true);
            String icon = this.getEmoteIcon(button.emote);
            int iconWidth = EmotesScreen.mc.textRenderer.getWidth(icon);
            matrices.push();
            matrices.translate(x2 + 60.0f - (float)iconWidth / 2.0f, y2 + 10.0f, 0.0f);
            matrices.scale(1.5f, 1.5f, 1.0f);
            context.drawText(EmotesScreen.mc.textRenderer, icon, 0, 0, textColor, false);
            matrices.pop();
        }
    }

    private void renderTitle(DrawContext context) {
        String title = "✨ Выберите эмоцию ✨";
        int titleWidth = EmotesScreen.mc.textRenderer.getWidth(title);
        MatrixStack matrices = context.getMatrices();
        float titleX = (float)this.width / 2.0f - (float)titleWidth / 2.0f;
        float titleY = this.startY - 40.0f;
        float f2 = titleWidth + 20;
        Objects.requireNonNull(EmotesScreen.mc.textRenderer);
        RenderUtils.drawGradientRect(matrices, titleX - 10.0f, titleY - 5.0f, f2, 9 + 10, 5.0f, ColorUtils.rgba(40, 40, 60, 200), ColorUtils.rgba(20, 20, 40, 200), true);
        context.drawText(EmotesScreen.mc.textRenderer, title, (int)titleX, (int)titleY, -1, true);
    }

    private int getHoveredButton(int mouseX, int mouseY) {
        for (int i2 = 0; i2 < this.buttons.size(); ++i2) {
            EmoteButton button = this.buttons.get(i2);
            float x2 = this.startX + (float)button.col * 130.0f;
            float y2 = this.startY + (float)button.row * 70.0f;
            if (!((float)mouseX >= x2) || !((float)mouseX <= x2 + 120.0f) || !((float)mouseY >= y2) || !((float)mouseY <= y2 + 60.0f)) continue;
            return i2;
        }
        return -1;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.hoveredButton >= 0) {
            EmoteButton emoteButton = this.buttons.get(this.hoveredButton);
            Emotes.INSTANCE.playEmote(emoteButton.emote);
            this.close();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean shouldPause() {
        return false;
    }

    private String getEmoteIcon(Emotes.EmoteType emote) {
        return switch (emote) {
            default -> throw new MatchException(null, null);
            case Emotes.EmoteType.CRY -> "\ud83d\ude22";
            case Emotes.EmoteType.RUSSIAN_SQUAT -> "\ud83c\uddf7\ud83c\uddfa";
            case Emotes.EmoteType.FLOSS -> "\ud83d\udc83";
            case Emotes.EmoteType.TAKE_THE_L -> "\ud83e\udd26";
            case Emotes.EmoteType.ORANGE_JUSTICE -> "\ud83c\udf4a";
            case Emotes.EmoteType.GET_GRIDDY -> "\ud83d\udd7a";
            case Emotes.EmoteType.WAVE -> "\ud83d\udc4b";
            case Emotes.EmoteType.DAB -> "\ud83d\ude0e";
        };
    }

    private static class EmoteButton {
        final Emotes.EmoteType emote;
        final int row;
        final int col;
        final int index;

        EmoteButton(Emotes.EmoteType emote, int row, int col, int index) {
            this.emote = emote;
            this.row = row;
            this.col = col;
            this.index = index;
        }
    }
}

