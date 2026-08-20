package polar.ru.client.ui.mainmenu.account;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.session.Session;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import polar.ru.api.QClient;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.math.HoveringUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.api.utils.scissor.ScissorUtils;
import polar.ru.client.ui.mainmenu.account.Account;
import polar.ru.client.ui.mainmenu.account.AccountManager;
import polar.ru.client.ui.mainmenu.account.generator.MainGenerator;
import polar.ru.mixin.IMinecraftClientAccessor;

public final class AccountGuiScreen
extends Screen
implements QClient {
    private static final Identifier BACKGROUND = Identifier.of((String)"polar", (String)"textures/mainmenu/menu.png");
    public static final AccountManager MANAGER = new AccountManager();
    private static final float PANEL_WIDTH = 400.0f;
    private static final float PANEL_HEIGHT = 160.0f;
    private static final float MARGIN = 10.0f;
    private static final float BUTTON_SIZE = 25.0f;
    private final Map<String, Float> textScrollPhase = new HashMap<String, Float>();
    private final Screen parent;
    private final TextField nicknameField = new TextField("Nickname", "A");
    private final TextField searchField = new TextField("Search", "B");
    private Account selected;
    private float scroll;
    private float targetScroll;
    private float maxScroll;

    public AccountGuiScreen(Screen parent) {
        super((Text)Text.empty());
        this.parent = parent;
        this.selectInitialAccount();
    }

    private void setSelected(Account account) {
        this.selected = account;
        MANAGER.saveLastSelected(account != null ? account.name() : null);
    }

    protected void init() {
        super.init();
        this.selectInitialAccount();
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        RenderUtils.drawImage(matrices, BACKGROUND, 0.0f, 0.0f, this.width, this.height, -1);
        float panelX = (float)this.width / 2.0f - 200.0f;
        float panelY = (float)this.height / 2.0f - 80.0f + 40.0f;
        float leftWidth = 150.94339f;
        float listX = panelX + leftWidth + 10.0f;
        float listWidth = 400.0f - leftWidth;
        float panelPart = 75.0f;
        this.scroll = MathHelper.lerp((float)(0.18f * delta), (float)this.scroll, (float)this.targetScroll);
        this.drawPanel(matrices, panelX, panelY, leftWidth, panelPart);
        this.drawPanel(matrices, panelX, panelY + panelPart + 10.0f, leftWidth, panelPart);
        this.drawPanel(matrices, listX, panelY, listWidth, 160.0f);
        this.drawAccountList(matrices, mouseX, mouseY, listX, panelY, listWidth, 160.0f);
        this.drawFieldsAndButtons(matrices, mouseX, mouseY, panelX, panelY, leftWidth, panelPart);
        Font footerFont = AccountGuiScreen.font("moe3", 15);
        AccountGuiScreen.drawCentered(footerFont, matrices, "© Lavanda Client 2026", (float)this.width / 2.0f, (float)this.height - AccountGuiScreen.height(footerFont) - 4.0f, AccountGuiScreen.withAlpha(AccountGuiScreen.themeColor(90), 127));
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawFieldsAndButtons(MatrixStack matrices, int mouseX, int mouseY, float panelX, float panelY, float leftWidth, float panelPart) {
        float fieldX = panelX + 10.0f;
        float fieldWidth = leftWidth - 20.0f;
        float fieldHeight = 27.0f;
        float fieldGap = 8.0f;
        float verticalPadding = (panelPart - fieldHeight * 2.0f - fieldGap) / 2.0f;
        float nicknameY = panelY + verticalPadding;
        float searchY = nicknameY + fieldHeight + fieldGap;
        this.drawInputBackground(matrices, fieldX, nicknameY, fieldWidth, fieldHeight, mouseX, mouseY);
        this.drawInputBackground(matrices, fieldX, searchY, fieldWidth, fieldHeight, mouseX, mouseY);
        this.nicknameField.setBounds(fieldX + 5.0f, nicknameY, fieldWidth - 10.0f, fieldHeight);
        this.searchField.setBounds(fieldX + 5.0f, searchY, fieldWidth - 10.0f, fieldHeight);
        this.nicknameField.draw(matrices);
        this.searchField.draw(matrices);
        float bottomY = panelY + panelPart + 20.0f;
        float buttonWidth = leftWidth - 20.0f;
        float smallWidth = buttonWidth / 2.0f - 6.0f;
        this.drawButton(matrices, fieldX, bottomY, buttonWidth, 25.0f, "Add", false, mouseX, mouseY);
        this.drawButton(matrices, fieldX, bottomY + 25.0f + 5.0f, smallWidth, 25.0f, "Generate", false, mouseX, mouseY);
        this.drawButton(matrices, fieldX + smallWidth + 12.0f, bottomY + 25.0f + 5.0f, smallWidth, 25.0f, "Clear all", true, mouseX, mouseY);
    }

    private void drawAccountList(MatrixStack matrices, int mouseX, int mouseY, float x2, float y2, float width, float height) {
        List<Account> accounts = this.filteredAccounts();
        float cardWidth = (width - 30.0f) / 2.0f;
        float currentOffset = 0.0f;
        int column = 0;
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(x2, y2 + 10.0f, width, height - 20.0f);
        for (Account account : accounts) {
            float accountX = x2 + 10.0f + (cardWidth + 10.0f) * (float)column;
            float accountY = y2 + 10.0f + this.scroll + currentOffset;
            this.drawAccount(matrices, account, accountX, accountY, cardWidth, 40.0f, mouseX, mouseY);
            if (++column <= 1) continue;
            column = 0;
            currentOffset += 50.0f;
        }
        if (accounts.isEmpty()) {
            Font font = AccountGuiScreen.font("moe3", 13);
            AccountGuiScreen.drawCentered(font, matrices, "No accounts", x2 + width / 2.0f, y2 + height / 2.0f - AccountGuiScreen.height(font) / 2.0f, AccountGuiScreen.withAlpha(AccountGuiScreen.themeColor(45), 180));
        }
        ScissorUtils.pop();
        if (column != 0) {
            currentOffset += 50.0f;
        }
        float contentHeight = currentOffset > 0.0f ? currentOffset - 10.0f : 0.0f;
        this.maxScroll = Math.min(0.0f, height - contentHeight - 20.0f);
        this.targetScroll = MathHelper.clamp((float)this.targetScroll, (float)this.maxScroll, (float)0.0f);
        this.scroll = MathHelper.clamp((float)this.scroll, (float)this.maxScroll, (float)0.0f);
    }

    private void drawAccount(MatrixStack matrices, Account account, float x2, float y2, float width, float height, int mouseX, int mouseY) {
        boolean hovered = HoveringUtils.isHovered(mouseX, mouseY, x2, y2, width, height);
        boolean current = this.isCurrent(account);
        boolean selectedAccount = this.selected == account;
        int background = ColorUtils.interpolateColor(AccountGuiScreen.withAlpha(ColorUtils.darken(AccountGuiScreen.themeColor(180), 0.13f), 166), AccountGuiScreen.withAlpha(ColorUtils.darken(AccountGuiScreen.themeColor(45), 0.2f), 196), hovered ? 1.0f : 0.0f);
        RenderUtils.drawBlur(matrices, x2, y2, width, height, 6.0f, 8.0f, ColorUtils.rgba(255, 255, 255, 120));
        RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, 8.0f, ColorUtils.rgba(10, 13, 20, 150));
        RenderUtils.drawPlayerHead(matrices, account.name(), x2 + 5.0f, y2 + 5.0f, height - 10.0f, 9.0f);
        Font nameFont = AccountGuiScreen.font("moe3", 14);
        Font dateFont = AccountGuiScreen.font("moe3", 12);
        float textX = x2 + height;
        float maxNameWidth = width - height - 26.0f;
        int nameColor = current ? AccountGuiScreen.themeColor() : -1;
        this.drawTextTruncated(nameFont, matrices, account.name(), textX, y2 + 9.0f, maxNameWidth, nameColor);
        AccountGuiScreen.draw(dateFont, matrices, account.creationDate().format(DateTimeFormatter.ofPattern("dd MMMM HH:mm", Locale.ENGLISH)), textX, y2 + 25.0f, AccountGuiScreen.withAlpha(-1, 76));
        Font iconFont = AccountGuiScreen.font("altmanager", 16);
        int favoriteColor = account.favorite() ? ColorUtils.rgba(255, 160, 102, 255) : AccountGuiScreen.withAlpha(-1, 95);
        AccountGuiScreen.draw(iconFont, matrices, "D", x2 + width - 16.0f, y2 + 8.5f, favoriteColor);
        AccountGuiScreen.draw(iconFont, matrices, "C", x2 + width - 16.0f, y2 + height - 16.0f, ColorUtils.rgba(255, 101, 104, 230));
    }

    private void drawPanel(MatrixStack matrices, float x2, float y2, float width, float height) {
        int top = AccountGuiScreen.withAlpha(ColorUtils.darken(AccountGuiScreen.themeColor(45), 0.17f), 190);
        int bottom = AccountGuiScreen.withAlpha(ColorUtils.darken(AccountGuiScreen.themeColor(180), 0.11f), 205);
        RenderUtils.drawBlur(matrices, x2, y2, width, height, 8.0f, 9.0f, ColorUtils.rgba(255, 255, 255, 112));
        RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, 8.0f, ColorUtils.rgba(10, 13, 20, 150));
    }

    private void drawInputBackground(MatrixStack matrices, float x2, float y2, float width, float height, int mouseX, int mouseY) {
        boolean hovered = HoveringUtils.isHovered(mouseX, mouseY, x2, y2, width, height);
        RenderUtils.drawBlur(matrices, x2, y2, width, height, 5.0f, 8.0f, ColorUtils.rgba(255, 255, 255, hovered ? 122 : 104));
        RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, 5.0f, ColorUtils.rgba(10, 13, 20, 150));
    }

    private void drawButton(MatrixStack matrices, float x2, float y2, float width, float height, String text, boolean danger, int mouseX, int mouseY) {
        boolean hovered = HoveringUtils.isHovered(mouseX, mouseY, x2, y2, width, height);
        int accent = danger ? ColorUtils.rgba(255, 101, 104, 255) : AccountGuiScreen.themeColor();
        int fill = AccountGuiScreen.withAlpha(ColorUtils.darken(accent, hovered ? 0.24f : 0.15f), hovered ? 205 : 150);
        RenderUtils.drawBlur(matrices, x2, y2, width, height, 6.0f, 8.5f, ColorUtils.rgba(255, 255, 255, hovered ? 124 : 102));
        RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, 6.0f, ColorUtils.rgba(10, 13, 20, 150));
        Font font = AccountGuiScreen.font("moe3", 16);
        AccountGuiScreen.drawCentered(font, matrices, text, x2 + width / 2.0f, y2 + height / 1.5f - AccountGuiScreen.height(font) / 2.0f + 2.0f, danger ? ColorUtils.rgba(255, 120, 123, 255) : AccountGuiScreen.withAlpha(AccountGuiScreen.themeColor(45), 255));
    }

    private void drawMainMenuOutline(MatrixStack matrices, float x2, float y2, float width, float height, float hoverProgress) {
        float shimmer = (float)((Math.sin((double)System.currentTimeMillis() * 0.0016) + 1.0) * 0.5);
        int accentAlpha = (int)(112.0f + 42.0f * hoverProgress + 12.0f * shimmer);
        int softAlpha = (int)(18.0f + 18.0f * hoverProgress);
        int midAlpha = (int)(42.0f + 22.0f * hoverProgress);
        int topLeft = AccountGuiScreen.withAlpha(AccountGuiScreen.themeColor(), accentAlpha);
        int topRight = AccountGuiScreen.withAlpha(AccountGuiScreen.themeColor(90), midAlpha);
        int bottomLeft = AccountGuiScreen.withAlpha(ColorUtils.darken(AccountGuiScreen.themeColor(180), 0.55f), softAlpha);
        int bottomRight = AccountGuiScreen.withAlpha(AccountGuiScreen.themeColor(180), accentAlpha);
        int shimmerColor = AccountGuiScreen.withAlpha(AccountGuiScreen.themeColor(45), (int)(24.0f + 26.0f * hoverProgress));
        topLeft = ColorUtils.interpolateColor(topLeft, shimmerColor, shimmer * 0.18f);
        bottomRight = ColorUtils.interpolateColor(bottomRight, shimmerColor, (1.0f - shimmer) * 0.18f);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float panelX = (float)this.width / 2.0f - 200.0f;
        float panelY = (float)this.height / 2.0f - 80.0f + 40.0f;
        float leftWidth = 150.94339f;
        float listX = panelX + leftWidth + 10.0f;
        float listWidth = 400.0f - leftWidth;
        float panelPart = 75.0f;
        float bottomY = panelY + panelPart + 20.0f;
        float buttonWidth = leftWidth - 20.0f;
        float smallWidth = buttonWidth / 2.0f - 6.0f;
        this.nicknameField.mouseClicked(mouseX, mouseY, button);
        this.searchField.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, panelX + 10.0f, bottomY, buttonWidth, 25.0)) {
            AccountGuiScreen.playClickSound();
            this.addAccount(this.nicknameField.text());
            return true;
        }
        if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, panelX + 10.0f, bottomY + 25.0f + 5.0f, smallWidth, 25.0)) {
            AccountGuiScreen.playClickSound();
            this.addAccount(MainGenerator.generate(), false);
            return true;
        }
        if (button == 0 && HoveringUtils.isHovered(mouseX, mouseY, panelX + 10.0f + smallWidth + 12.0f, bottomY + 25.0f + 5.0f, smallWidth, 25.0)) {
            AccountGuiScreen.playClickSound();
            MANAGER.clearAccounts();
            this.setSelected(null);
            return true;
        }
        if (HoveringUtils.isHovered(mouseX, mouseY, listX, panelY, listWidth, 160.0) && this.clickAccount(mouseX, mouseY, button, listX, panelY, listWidth)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickAccount(double mouseX, double mouseY, int button, float listX, float panelY, float listWidth) {
        List<Account> accounts = this.filteredAccounts();
        float cardWidth = (listWidth - 30.0f) / 2.0f;
        float currentOffset = 0.0f;
        int column = 0;
        for (Account account : accounts) {
            float x2 = listX + 10.0f + (cardWidth + 10.0f) * (float)column;
            float y2 = panelY + 10.0f + this.scroll + currentOffset;
            if (HoveringUtils.isHovered(mouseX, mouseY, x2 + cardWidth - 29.0f, y2 + 7.0f, 18.0, 16.0)) {
                account.toggleFavorite();
                MANAGER.save();
                AccountGuiScreen.playClickSound();
                return true;
            }
            if (HoveringUtils.isHovered(mouseX, mouseY, x2 + cardWidth - 20.0f, y2 + 24.0f, 16.0, 16.0) || button == 1 && HoveringUtils.isHovered(mouseX, mouseY, x2, y2, cardWidth, 40.0)) {
                MANAGER.removeAccount(account.name());
                if (this.selected == account) {
                    this.setSelected(MANAGER.stream().findFirst().orElse(null));
                }
                AccountGuiScreen.playClickSound();
                return true;
            }
            if (HoveringUtils.isHovered(mouseX, mouseY, x2, y2, cardWidth, 40.0)) {
                if (button == 0) {
                    AccountGuiScreen.setSession(account.name());
                    this.setSelected(account);
                    MANAGER.save();
                    AccountGuiScreen.playClickSound();
                }
                return true;
            }
            if (++column <= 1) continue;
            column = 0;
            currentOffset += 50.0f;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.targetScroll = MathHelper.clamp((float)(this.targetScroll + (float)verticalAmount * 17.5f), (float)this.maxScroll, (float)0.0f);
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nicknameField.keyPressed(keyCode)) {
            if (keyCode == 257) {
                this.addAccount(this.nicknameField.text());
                this.nicknameField.selected(false);
            }
            return true;
        }
        if (this.searchField.keyPressed(keyCode)) {
            if (keyCode == 257) {
                this.searchField.selected(false);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        boolean handled = this.nicknameField.charTyped(chr);
        handled = this.searchField.charTyped(chr) || handled;
        return handled || super.charTyped(chr, modifiers);
    }

    public void close() {
        MANAGER.save();
        if (mc != null) {
            mc.setScreen(this.parent);
        }
    }

    private void addAccount(String name) {
        this.addAccount(name, true);
    }

    private void addAccount(String name, boolean switchTo) {
        if (name == null) {
            return;
        }
        String normalized = name.trim();
        if (normalized.length() < 3 || normalized.length() > 16) {
            return;
        }
        Account account = MANAGER.getAccount(normalized).orElseGet(() -> {
            Account created = new Account(LocalDateTime.now(), normalized);
            MANAGER.addAccount(created);
            return created;
        });
        if (switchTo || this.selected == null) {
            this.setSelected(account);
            AccountGuiScreen.setSession(account.name());
        }
        this.nicknameField.cursorToEnd();
        MANAGER.save();
    }

    private void selectInitialAccount() {
        Account account;
        if (this.selected != null) {
            return;
        }
        String last = MANAGER.file().getLast();
        Optional<Account> lastAccount = MANAGER.getAccount(last);
        this.selected = account = lastAccount.orElseGet(() -> MANAGER.stream().findFirst().orElse(null));
        if (account != null && !last.isEmpty()) {
            AccountGuiScreen.setSession(account.name());
        }
    }

    private List<Account> filteredAccounts() {
        String search = this.searchField.text().trim().toLowerCase(Locale.ROOT);
        Comparator<Account> comparator = Comparator.comparing((Account account) -> !account.favorite()).thenComparing((Account account) -> account.creationDate(), Comparator.reverseOrder());
        return MANAGER.stream().filter(account -> search.isEmpty() || account.name().toLowerCase(Locale.ROOT).contains(search)).sorted(comparator).toList();
    }

    private boolean isCurrent(Account account) {
        return mc.getSession() != null && mc.getSession().getUsername().equalsIgnoreCase(account.name());
    }

    private static void setSession(String name) {
        ((IMinecraftClientAccessor)mc).setSession(AccountGuiScreen.createSession(name));
    }

    private static Session createSession(String name) {
        try {
            Constructor constructor = Session.class.getDeclaredConstructor(String.class, UUID.class, String.class, Optional.class, Optional.class, Session.AccountType.class);
            constructor.setAccessible(true);
            return (Session)constructor.newInstance(name, UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()), mc.getSession() == null ? "" : mc.getSession().getAccessToken(), Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
        }
        catch (Exception e2) {
            throw new RuntimeException(e2);
        }
    }

    private static void playClickSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play((SoundInstance)PositionedSoundInstance.master((RegistryEntry)SoundEvents.UI_BUTTON_CLICK, (float)1.0f));
        }
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
            return AccountGuiScreen.themeColor();
        }
    }

    private static int withAlpha(int color, int alpha) {
        return ColorUtils.setAlphaColor(color, MathHelper.clamp((int)alpha, (int)1, (int)255));
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

    private void drawTextTruncated(Font font, MatrixStack matrices, String text, float x2, float y2, float maxWidth, int color) {
        if (font == null || text == null || text.isEmpty() || maxWidth <= 0.0f) {
            return;
        }
        float totalWidth = font.getWidth(text);
        if (totalWidth <= maxWidth) {
            font.draw(matrices, text, x2, y2, color);
            return;
        }
        float overflow = totalWidth - maxWidth;
        float phase = this.textScrollPhase.getOrDefault(text, Float.valueOf(0.0f)).floatValue();
        phase += 0.003f;
        if (phase > 1.0f) {
            phase -= 1.0f;
        }
        this.textScrollPhase.put(text, Float.valueOf(phase));
        float pingPong = phase < 0.5f ? phase * 2.0f : 2.0f - phase * 2.0f;
        float eased = pingPong * pingPong * (3.0f - 2.0f * pingPong);
        float scrollOffset = overflow * eased;
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(x2, y2 - 2.0f, maxWidth, font.getHeight() + 4.0f);
        font.draw(matrices, text, x2 - scrollOffset, y2, color);
        ScissorUtils.pop();
    }

    private static final class TextField {
        private final String placeholder;
        private final String icon;
        private String text = "";
        private boolean selected;
        private int cursor;
        private float animatedCursorX;
        private float x;
        private float y;
        private float width;
        private float height;

        private TextField(String placeholder, String icon) {
            this.placeholder = placeholder;
            this.icon = icon;
        }

        private void setBounds(float x2, float y2, float width, float height) {
            this.x = x2;
            this.y = y2;
            this.width = width;
            this.height = height;
        }

        private void draw(MatrixStack matrices) {
            Font textFont = AccountGuiScreen.font("moe3", 16);
            Font iconFont = AccountGuiScreen.font("altmanager", 16);
            float drawX = this.x + 5.0f;
            if (iconFont != null) {
                AccountGuiScreen.draw(iconFont, matrices, this.icon, drawX, this.y + this.height / 2.0f - iconFont.getHeight() / 2.0f + 5.0f, AccountGuiScreen.themeColor());
                drawX += iconFont.getWidth(this.icon) + 8.0f;
            }
            boolean drawPlaceholder = this.text.isEmpty() && !this.selected;
            String renderedText = drawPlaceholder ? this.placeholder : this.text;
            String string = renderedText;
            if (!renderedText.isEmpty()) {
                int color = drawPlaceholder ? AccountGuiScreen.withAlpha(AccountGuiScreen.themeColor(45), 125) : -1;
                AccountGuiScreen.draw(textFont, matrices, renderedText, drawX, this.y + this.height / 2.0f - AccountGuiScreen.height(textFont) / 2.0f + 5.0f, color);
            }
            if (this.selected && textFont != null) {
                String beforeCursor = this.text.substring(0, MathHelper.clamp((int)this.cursor, (int)0, (int)this.text.length()));
                float cursorTargetX = drawX + AccountGuiScreen.width(textFont, beforeCursor) + 1.0f;
                if (this.animatedCursorX == 0.0f) {
                    this.animatedCursorX = cursorTargetX;
                }
                this.animatedCursorX = MathHelper.lerp((float)0.35f, (float)this.animatedCursorX, (float)cursorTargetX);
                float blink = (float)((Math.sin((double)System.currentTimeMillis() * 0.006) + 1.0) * 0.5);
                float cursorHeight = (this.height - 14.0f) * (0.88f + blink * 0.12f);
                float cursorY = this.y + (this.height - cursorHeight) / 2.0f;
                RenderUtils.drawRoundedRect(matrices, this.animatedCursorX, cursorY, 0.8f, cursorHeight, 0.0f, AccountGuiScreen.withAlpha(AccountGuiScreen.themeColor(), (int)(95.0f + 160.0f * blink)));
            }
        }

        private void mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                this.selected = HoveringUtils.isHovered(mouseX, mouseY, this.x - 5.0f, this.y, this.width + 10.0f, this.height);
                if (this.selected) {
                    this.cursorToEnd();
                }
            }
        }

        private boolean keyPressed(int keyCode) {
            if (!this.selected) {
                return false;
            }
            boolean control = InputUtil.isKeyPressed((long)QClient.mc.getWindow().getHandle(), (int)341) || InputUtil.isKeyPressed((long)QClient.mc.getWindow().getHandle(), (int)345);
            boolean bl = control;
            if (control && keyCode == 86) {
                this.insert(QClient.mc.keyboard.getClipboard());
            } else if (keyCode == 259) {
                if (this.cursor > 0) {
                    this.text = this.text.substring(0, this.cursor - 1) + this.text.substring(this.cursor);
                    --this.cursor;
                }
            } else if (keyCode == 261) {
                if (this.cursor < this.text.length()) {
                    this.text = this.text.substring(0, this.cursor) + this.text.substring(this.cursor + 1);
                }
            } else if (keyCode == 263) {
                this.cursor = Math.max(0, this.cursor - 1);
            } else if (keyCode == 262) {
                this.cursor = Math.min(this.text.length(), this.cursor + 1);
            } else if (keyCode == 268) {
                this.cursor = 0;
            } else if (keyCode == 269) {
                this.cursorToEnd();
            } else if (keyCode == 256) {
                this.selected = false;
            }
            return true;
        }

        private boolean charTyped(char chr) {
            if (!this.selected || this.text.length() >= 16 || !TextField.isAllowed(chr)) {
                return false;
            }
            this.insert(String.valueOf(chr));
            return true;
        }

        private void insert(String value) {
            if (value == null || value.isEmpty()) {
                return;
            }
            String sanitized = value.chars().filter(code -> TextField.isAllowed((char)code)).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
            if (sanitized.isEmpty()) {
                return;
            }
            int free = Math.max(0, 16 - this.text.length());
            if (sanitized.length() > free) {
                sanitized = sanitized.substring(0, free);
            }
            this.text = this.text.substring(0, this.cursor) + sanitized + this.text.substring(this.cursor);
            this.cursor += sanitized.length();
        }

        private static boolean isAllowed(char chr) {
            return Character.isLetterOrDigit(chr) || chr == '_' || chr == '-';
        }

        private String text() {
            return this.text;
        }

        private void selected(boolean selected) {
            this.selected = selected;
        }

        private void cursorToEnd() {
            this.cursor = this.text.length();
            this.animatedCursorX = 0.0f;
        }
    }
}

