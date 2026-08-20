package polar.ru.client.ui.mainmenu;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import polar.ru.api.QClient;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.math.HoveringUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.api.utils.rpc.DiscordProfileCache;
import polar.ru.api.utils.scissor.ScissorUtils;
import polar.ru.client.ui.mainmenu.account.AccountGuiScreen;

public class MainMenu
extends Screen
implements QClient {
    private static final Identifier BACKGROUND = Identifier.of((String)"polar", (String)"textures/mainmenu/menu.png");
    private static final String site = "";
    private static final String discord = "";
    private static final int TRANSPARENT_ALPHA = 1;
    private static final float MENU_TEXT_Y_OFFSET = 0.5f;
    private static final float MENU_ICON_Y_OFFSET = 0.5f;
    private static final float MENU_PRIMARY_ICON_X_OFFSET = -5.0f;
    private static boolean sessionRestored = false;
    private final List<Button> buttons = new ArrayList<Button>();
    private final AnimationUtils logoHoverAnimation = new AnimationUtils(0.0f, 7.5f, Easings.CUBIC_OUT);
    private final AnimationUtils[] hoverButtonAnim = new AnimationUtils[]{new AnimationUtils(0.0f, 7.5f, Easings.CUBIC_OUT), new AnimationUtils(0.0f, 7.5f, Easings.CUBIC_OUT)};

    public MainMenu() {
        super((Text)Text.empty());
    }

    protected void init() {
        super.init();
        if (!sessionRestored) {
            sessionRestored = true;
            AccountGuiScreen.MANAGER.restoreLastSession();
        }
        float margin = 6.0f;
        float buttonWidth = 130.0f;
        float buttonHeight = 22.0f;
        float x2 = (float)this.width / 2.0f - buttonWidth / 2.0f;
        float baseY = (float)this.height / 2.0f - buttonHeight * 2.0f - margin * 2.0f + 70.0f;
        this.buttons.clear();
        this.buttons.add(new Button(x2, baseY, buttonWidth, buttonHeight, "Singleplayer", button -> mc.setScreen((Screen)new SelectWorldScreen((Screen)this))));
        this.buttons.add(new Button(x2, baseY + buttonHeight + margin, buttonWidth, buttonHeight, "Multiplayer", button -> mc.setScreen((Screen)new MultiplayerScreen((Screen)this))));
        this.buttons.add(new Button(x2, baseY + (buttonHeight + margin) * 2.0f, buttonWidth / 2.0f - margin / 2.0f, buttonHeight, "Options", button -> mc.setScreen((Screen)new OptionsScreen((Screen)this, MainMenu.mc.options))));
        this.buttons.add(new Button((float)this.width / 2.0f + margin / 2.0f, baseY + (buttonHeight + margin) * 2.0f, buttonWidth / 2.0f - margin / 2.0f, buttonHeight, "Accounts", button -> mc.setScreen((Screen)new AccountGuiScreen(this))));
        this.buttons.add(new Button(x2, baseY + (buttonHeight + margin) * 3.0f, buttonWidth, buttonHeight, "Exit", button -> mc.scheduleStop()).extra(true));
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        RenderUtils.drawImage(matrices, BACKGROUND, 0.0f, 0.0f, this.width, this.height, -1);
        float glassX = (float)this.width / 2.0f - 170.0f;
        float glassY = (float)this.height / 2.0f - 128.0f;
        Font textFont = MainMenu.font("moe3", 14);
        Font footerFont = MainMenu.font("moe3", 15);
        float fy = (float)this.height / 2.0f - 44.0f - 12.0f + 55.0f;
        float fx = (float)this.width / 2.0f - 65.0f;
        String username = DiscordProfileCache.getDisplayUsername();
        float helloWidth = MainMenu.width(textFont, "Hello, ") + MainMenu.width(textFont, username);
        float helloX = fx + 65.0f - helloWidth / 2.0f;
        MainMenu.draw(textFont, matrices, "Hello, ", helloX, fy, -1);
        MainMenu.draw(textFont, matrices, username, helloX + MainMenu.width(textFont, "Hello, "), fy, MainMenu.themeColor());
        this.renderLogo(matrices, mouseX, mouseY);
        String copyright = "© Lavanda Client 2026";
        MainMenu.drawCentered(footerFont, matrices, copyright, (float)this.width / 2.0f, (float)this.height - MainMenu.height(footerFont) - 4.0f, MainMenu.withAlpha(MainMenu.themeColor(90), 127));
        this.renderLinkButtons(matrices, mouseX, mouseY);
        for (Button button : this.buttons) {
            button.render(matrices, mouseX, mouseY, delta);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderLogo(MatrixStack matrices, int mouseX, int mouseY) {
        float logoHeight;
        Font logoFont = MainMenu.font("logo", 75);
        if (logoFont == null) {
            return;
        }
        String logo = "A";
        float logoX = (float)this.width / 2.0f;
        float logoY = Math.max(70.0f, (float)this.height / 2.0f - 90.0f);
        float logoWidth = logoFont.getWidth(logo);
        boolean hovered = HoveringUtils.isHovered(mouseX, mouseY, logoX - logoWidth / 2.0f, logoY, logoWidth, logoHeight = logoFont.getHeight());
        this.logoHoverAnimation.update(hovered ? 1.0f : 0.0f);
        float scale = 1.0f + 0.055f * MathHelper.clamp((float)this.logoHoverAnimation.getValue(), (float)0.0f, (float)1.0f);
        matrices.push();
        matrices.translate(logoX, logoY + logoHeight / 2.0f, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        logoFont.drawCenteredString(matrices, logo, 0.0f, -logoHeight / 2.0f, MainMenu.themeColor());
        matrices.pop();
    }

    private void renderLinkButtons(MatrixStack matrices, int mouseX, int mouseY) {
        Font mainMenu24 = MainMenu.font("icona", 24);
        if (mainMenu24 == null) {
            return;
        }
        for (int i2 = 0; i2 < 2; ++i2) {
            String icon = i2 == 0 ? "D" : "E";
            float xPos = (float)this.width - mainMenu24.getWidth("D") - 10.0f - (float)i2 * (7.5f + mainMenu24.getWidth(icon));
            float yPos = (float)this.height - mainMenu24.getHeight() + 3.5f;
            boolean hovered = HoveringUtils.isHovered(mouseX, mouseY, xPos, yPos, mainMenu24.getWidth(icon), mainMenu24.getHeight() + 1.0f);
            this.hoverButtonAnim[i2].update(hovered ? 1.0f : 0.0f);
            int color = ColorUtils.interpolateColor(ColorUtils.rgba(202, 215, 255, 255), ColorUtils.rgba(142, 151, 178, 255), this.hoverButtonAnim[i2].getValue());
            RenderUtils.drawBlur(matrices, xPos - 4.0f, yPos - 3.0f, mainMenu24.getWidth(icon) + 8.0f, mainMenu24.getHeight() + 7.0f, 5.0f, 7.0f, ColorUtils.rgba(255, 255, 255, 85));
            mainMenu24.draw(matrices, icon, xPos, yPos, color);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Font mainMenu24;
        for (Button menuButton : this.buttons) {
            if (!menuButton.mouseClicked(mouseX, mouseY, button)) continue;
            return true;
        }
        if (button == 0 && (mainMenu24 = MainMenu.font("icona", 24)) != null) {
            float yPos = (float)this.height - mainMenu24.getHeight() + 3.5f;
            float telegramX = (float)this.width - mainMenu24.getWidth("D") - 10.0f;
            float discordX = (float)this.width - mainMenu24.getWidth("D") - 10.0f - 7.5f - mainMenu24.getWidth("E");
            if (HoveringUtils.isHovered(mouseX, mouseY, telegramX, yPos, mainMenu24.getWidth("D"), mainMenu24.getHeight() + 1.0f)) {
                this.openLink(site);
                return true;
            }
            if (HoveringUtils.isHovered(mouseX, mouseY, discordX, yPos, mainMenu24.getWidth("E"), mainMenu24.getHeight() + 1.0f)) {
                this.openLink(discord);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openLink(String link) {
        try {
            Util.getOperatingSystem().open(URI.create(link));
        }
        catch (IllegalArgumentException illegalArgumentException) {
            // empty catch block
        }
    }

    private static void playClickSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play((SoundInstance)PositionedSoundInstance.master((RegistryEntry)SoundEvents.UI_BUTTON_CLICK, (float)1.0f));
        }
    }

    public boolean shouldPause() {
        return false;
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    private static Font font(String name, int size) {
        return Fonts.getFont(name, size);
    }

    private static void draw(Font font, MatrixStack matrices, String text, float x2, float y2, int color) {
        if (font != null) {
            font.draw(matrices, text, x2, y2, color);
        }
    }

    private static void drawCentered(Font font, MatrixStack matrices, String text, float x2, float y2, int color) {
        if (font != null) {
            font.drawCenteredString(matrices, text, x2, y2, color);
        }
    }

    private static float width(Font font, String text) {
        return font == null ? 0.0f : font.getWidth(text);
    }

    private static float height(Font font) {
        return font == null ? 0.0f : font.getHeight();
    }

    private static int themeColor() {
        try {
            return ColorUtils.getThemeColor();
        }
        catch (Exception ignored) {
            return -9073971;
        }
    }

    private static int themeColor(int index) {
        try {
            return ColorUtils.getThemeColor(index);
        }
        catch (Exception ignored) {
            return MainMenu.themeColor();
        }
    }

    private static int withAlpha(int color, int alpha) {
        return ColorUtils.setAlphaColor(color, MathHelper.clamp((int)alpha, (int)1, (int)255));
    }

    private static int themedFill(int themeColor, float brightness, int alpha) {
        return MainMenu.withAlpha(ColorUtils.darken(themeColor, brightness), alpha);
    }

    private static final class Button {
        private final float x;
        private final float y;
        private final float width;
        private final float height;
        private final String text;
        private final Pressable action;
        private final AnimationUtils colorAnimation = new AnimationUtils(0.0f, 7.5f, Easings.CUBIC_OUT);
        private final AnimationUtils borderAnimation = new AnimationUtils(0.0f, 7.5f, Easings.CUBIC_OUT);
        private boolean extra;
        private boolean iconOnly;

        private Button(float x2, float y2, float width, float height, String text, Pressable action) {
            this.x = x2;
            this.y = y2;
            this.width = width;
            this.height = height;
            this.text = text;
            this.action = action;
        }

        private Button extra(boolean extra) {
            this.extra = extra;
            return this;
        }

        private Button iconOnly(boolean iconOnly) {
            this.iconOnly = iconOnly;
            return this;
        }

        private void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            boolean hovered = HoveringUtils.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
            this.colorAnimation.update(hovered ? 1.0f : 0.0f);
            this.borderAnimation.update(hovered ? 1.0f : 0.0f);
            float colorProgress = MathHelper.clamp((float)this.colorAnimation.getValue(), (float)0.0f, (float)1.0f);
            float borderProgress = MathHelper.clamp((float)this.borderAnimation.getValue(), (float)0.0f, (float)1.0f);
            int themeTop = MainMenu.themeColor(45);
            int themeBottom = MainMenu.themeColor(180);
            int top = ColorUtils.interpolateColor(MainMenu.themedFill(themeTop, 0.2f, 222), MainMenu.themedFill(themeTop, 0.3f, 238), colorProgress);
            int bottom = ColorUtils.interpolateColor(MainMenu.themedFill(themeBottom, 0.14f, 228), MainMenu.themedFill(themeBottom, 0.24f, 242), colorProgress);
            RenderUtils.drawBlur(matrices, this.x, this.y, this.width, this.height, 6.0f, 8.0f, ColorUtils.rgba(255, 255, 255, 110));
            RenderUtils.drawRoundedRect(matrices, this.x, this.y, this.width, this.height, 6.0f, ColorUtils.rgba(10, 13, 20, hovered ? 155 : 138));
            this.renderIndex(matrices, colorProgress);
            if (this.iconOnly) {
                return;
            }
            Font labelFont = MainMenu.font("moe3", 15);
            float textWidth = MainMenu.width(labelFont, this.text + " ");
            float drawX = this.x + (this.width - textWidth) / 2.0f;
            float drawY = this.y + this.height / 2.0f - 2.3f + 0.5f;
            int labelColor = ColorUtils.interpolateColor(-1, MainMenu.withAlpha(MainMenu.themeColor(45), 255), colorProgress * 0.35f);
            MainMenu.draw(labelFont, matrices, this.text, drawX, drawY, labelColor);
        }

        private void renderIndex(MatrixStack matrices, float hoverProgress) {
            String index = switch (this.text) {
                case "Options" -> "A";
                case "Accounts" -> "B";
                case "Multiplayer" -> "C";
                case "Singleplayer" -> "D";
                default -> "";
            };
            if (index.isEmpty()) {
                return;
            }
            Font iconFont = MainMenu.font("icona", 18);
            if (iconFont == null) {
                return;
            }
            float iconWidth = iconFont.getWidth(index);
            float iconHeight = iconFont.getHeight();
            float slotCenterX = this.iconOnly ? this.x + this.width / 2.0f : this.x + 21.0f;
            float f2 = slotCenterX;
            if ("Singleplayer".equals(this.text) || "Multiplayer".equals(this.text)) {
                slotCenterX += -5.0f;
            }
            float iconX = slotCenterX - iconWidth / 2.0f;
            float iconY = this.y + this.height / 2.0f - iconHeight / 2.0f + 5.3f + 0.5f;
            int topColor = ColorUtils.interpolateColor(MainMenu.themeColor(45), MainMenu.themeColor(90), hoverProgress);
            int bottomColor = ColorUtils.interpolateColor(MainMenu.themeColor(180), MainMenu.themeColor(), hoverProgress);
            this.drawVerticalGradientIcon(matrices, iconFont, index, iconX, iconY, iconWidth, iconHeight, topColor, bottomColor);
        }

        private void drawVerticalGradientIcon(MatrixStack matrices, Font font, String icon, float x2, float y2, float width, float height, int topColor, int bottomColor) {
            float halfHeight = height / 2.0f;
            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(x2 - 1.0f, y2 - 2.0f, width + 2.0f, halfHeight + 2.0f);
            font.draw(matrices, icon, x2, y2, topColor);
            ScissorUtils.pop();
            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(x2 - 1.0f, y2 + halfHeight - 1.0f, width + 2.0f, halfHeight + 3.0f);
            font.draw(matrices, icon, x2, y2, bottomColor);
            ScissorUtils.pop();
        }

        private void drawDirectionalGradientOutline(MatrixStack matrices, float hoverProgress, float borderProgress) {
            float shimmer = (float)((Math.sin((double)System.currentTimeMillis() * 0.0016) + 1.0) * 0.5);
            int accentAlpha = (int)(112.0f + 42.0f * borderProgress + 12.0f * shimmer);
            int softAlpha = (int)(18.0f + 18.0f * borderProgress);
            int midAlpha = (int)(42.0f + 22.0f * borderProgress);
            int topLeft = MainMenu.withAlpha(MainMenu.themeColor(), accentAlpha);
            int topRight = MainMenu.withAlpha(MainMenu.themeColor(90), midAlpha);
            int bottomLeft = MainMenu.withAlpha(ColorUtils.darken(MainMenu.themeColor(180), 0.55f), softAlpha);
            int bottomRight = MainMenu.withAlpha(MainMenu.themeColor(180), accentAlpha);
            int shimmerColor = MainMenu.withAlpha(MainMenu.themeColor(45), (int)(24.0f + 26.0f * hoverProgress));
            topLeft = ColorUtils.interpolateColor(topLeft, shimmerColor, shimmer * 0.18f);
            bottomRight = ColorUtils.interpolateColor(bottomRight, shimmerColor, (1.0f - shimmer) * 0.18f);
            RenderUtils.drawRoundedRectOutline(matrices, this.x, this.y, this.width, this.height, 6.0f, 6.0f, 6.0f, 6.0f, 0.8f, topLeft, topRight, bottomLeft, bottomRight);
        }

        private boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
                MainMenu.playClickSound();
                this.action.onPress(this);
                return true;
            }
            return false;
        }
    }

    private static interface Pressable {
        public void onPress(Button var1);
    }
}

