package polar.ru.client.ui.autobuy;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import polar.ru.api.QClient;
import polar.ru.client.ui.autobuy.ItemNames;

public class AutoBuyManager
implements QClient {
    private static final Pattern PRICE_PATTERN = Pattern.compile("(?iu)(?:цена|стоимость|price|стоит)[:\\s]*\\$?\\s*([0-9][0-9\\s.,'\\u00a0\\u202f]*)\\s*\\$?");
    private static final Pattern MONEY_PATTERN = Pattern.compile("\\$\\s*([0-9][0-9\\s.,'\\u00a0\\u202f]*)|([0-9][0-9\\s.,'\\u00a0\\u202f]*)\\s*\\$");
    private static Item target;
    private static String targetName;
    private static long maxPrice;
    private static boolean running;
    private static Stage stage;
    private static int delay;
    private static int waited;
    private static int attempts;
    private static int currentPage;
    private static int clickedSlot;
    private static long clickedPrice;
    private static String lastMessage;
    private static final int MAX_ATTEMPTS = 30;
    private static final int MAX_PAGES = 10;
    private static final int GUI_TIMEOUT = 80;

    public static void start(Item item, long price) {
        if (item == null) {
            return;
        }
        target = item;
        targetName = ItemNames.toRussian(item);
        if (targetName.isEmpty()) {
            targetName = item.getName().getString();
        }
        maxPrice = price <= 0L ? Long.MAX_VALUE : price;
        running = true;
        stage = Stage.SEND;
        delay = 2;
        waited = 0;
        attempts = 0;
        currentPage = 0;
        clickedSlot = -1;
        lastMessage = "Статус: поиск " + targetName;
        AutoBuyManager.chat("§a[AutoBuy] §fЗапуск поиска: §b" + targetName + " §fдо §a$" + AutoBuyManager.fmt(maxPrice));
    }

    public static void stop(String message) {
        running = false;
        target = null;
        stage = Stage.SEND;
        delay = 0;
        waited = 0;
        attempts = 0;
        currentPage = 0;
        clickedSlot = -1;
        lastMessage = message;
    }

    public static void toggle(Item item, long price) {
        if (running) {
            AutoBuyManager.stop("Статус: остановлено");
        } else {
            AutoBuyManager.start(item, price);
        }
    }

    public static boolean isRunning() {
        return running;
    }

    public static String getLastMessage() {
        return lastMessage;
    }

    public static Item getTarget() {
        return target;
    }

    public static long getMaxPrice() {
        return maxPrice;
    }

    public static void setMaxPrice(long price) {
        maxPrice = price <= 0L ? Long.MAX_VALUE : price;
    }

    public static void onTick() {
        if (!running) {
            return;
        }
        if (AutoBuyManager.mc.player == null || mc.getNetworkHandler() == null || AutoBuyManager.mc.interactionManager == null) {
            AutoBuyManager.stop("Статус: нет подключения");
            return;
        }
        if (delay > 0) {
            --delay;
            return;
        }
        switch (stage.ordinal()) {
            case 0: {
                AutoBuyManager.doSend();
                break;
            }
            case 1: {
                AutoBuyManager.doWaitGui();
                break;
            }
            case 2: {
                AutoBuyManager.doScan();
                break;
            }
            case 3: {
                AutoBuyManager.doNextPage();
                break;
            }
            case 4: {
                AutoBuyManager.doAfterClick();
            }
        }
    }

    private static void doSend() {
        if (AutoBuyManager.mc.currentScreen instanceof HandledScreen) {
            AutoBuyManager.mc.player.closeHandledScreen();
            delay = 6;
            return;
        }
        if (attempts >= 30) {
            AutoBuyManager.chat("§c[AutoBuy] §fНичего не найдено дешевле §a$" + AutoBuyManager.fmt(maxPrice));
            AutoBuyManager.stop("Статус: не найдено (лимит)");
            return;
        }
        ++attempts;
        currentPage = 0;
        String query = targetName;
        String commandStr = "ah search " + query;
        try {
            mc.getNetworkHandler().sendChatCommand(commandStr);
        }
        catch (Exception e2) {
            try {
                mc.getNetworkHandler().sendChatMessage("/" + commandStr);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        AutoBuyManager.chat("§a[AutoBuy] §7/ah search " + query + " §8(попытка " + attempts + ")");
        lastMessage = "Статус: поиск #" + attempts;
        stage = Stage.WAIT_GUI;
        waited = 0;
        delay = 8;
    }

    private static void doWaitGui() {
        if (AutoBuyManager.mc.currentScreen instanceof GenericContainerScreen) {
            stage = Stage.SCAN;
            delay = 4;
            return;
        }
        if (++waited > 80) {
            lastMessage = "Статус: меню не открылось, повтор";
            stage = Stage.SEND;
            delay = 15;
        }
    }

    private static void doScan() {
        Screen var_437_2 = AutoBuyManager.mc.currentScreen;
        if (!(var_437_2 instanceof HandledScreen)) {
            stage = Stage.WAIT_GUI;
            waited = 0;
            return;
        }
        HandledScreen screen = (HandledScreen)var_437_2;
        ScreenHandler handler = screen.getScreenHandler();
        int containerSize = Math.max(0, handler.slots.size() - 36);
        int bestSlot = -1;
        long bestPrice = Long.MAX_VALUE;
        for (int i2 = 0; i2 < containerSize; ++i2) {
            long price;
            Slot slot = (Slot)handler.slots.get(i2);
            ItemStack stack = slot.getStack();
            if (!AutoBuyManager.matches(stack) || (price = AutoBuyManager.readPrice(stack)) <= 0L || price > maxPrice || price >= bestPrice) continue;
            bestPrice = price;
            bestSlot = i2;
        }
        if (bestSlot == -1) {
            int nextPageSlot = AutoBuyManager.findNextPageSlot(handler, containerSize);
            if (nextPageSlot != -1 && currentPage < 10) {
                AutoBuyManager.mc.interactionManager.clickSlot(handler.syncId, nextPageSlot, 0, SlotActionType.PICKUP, (PlayerEntity)AutoBuyManager.mc.player);
                lastMessage = "Статус: страница " + ++currentPage;
                stage = Stage.WAIT_GUI;
                waited = 0;
                delay = 8;
                return;
            }
            lastMessage = "Статус: нет предмета <= $" + AutoBuyManager.fmt(maxPrice) + ", повтор";
            stage = Stage.SEND;
            delay = 25;
            return;
        }
        clickedSlot = bestSlot;
        clickedPrice = bestPrice;
        AutoBuyManager.mc.interactionManager.clickSlot(handler.syncId, bestSlot, 0, SlotActionType.PICKUP, (PlayerEntity)AutoBuyManager.mc.player);
        lastMessage = "Статус: покупка за $" + AutoBuyManager.fmt(bestPrice);
        stage = Stage.AFTER_CLICK;
        delay = 8;
    }

    private static void doNextPage() {
        stage = Stage.WAIT_GUI;
        delay = 4;
    }

    private static int findNextPageSlot(ScreenHandler handler, int containerSize) {
        for (int i2 = 0; i2 < containerSize; ++i2) {
            String name;
            ItemStack stack = ((Slot)handler.slots.get(i2)).getStack();
            if (stack.isEmpty() || (name = AutoBuyManager.strip(stack.getName().getString()).toLowerCase()).contains("пред") || name.contains("prev") || name.contains("назад") || name.contains("←") || name.contains("<") || !name.contains("след") && !name.contains("next") && !name.contains("вперед") && !name.contains("→") && !name.contains(">")) continue;
            return i2;
        }
        return -1;
    }

    private static void doAfterClick() {
        Screen var_437_2 = AutoBuyManager.mc.currentScreen;
        if (var_437_2 instanceof HandledScreen) {
            HandledScreen screen = (HandledScreen)var_437_2;
            ScreenHandler handler = screen.getScreenHandler();
            int containerSize = Math.max(0, handler.slots.size() - 36);
            for (int i2 = 0; i2 < containerSize; ++i2) {
                String name;
                ItemStack stack = ((Slot)handler.slots.get(i2)).getStack();
                if (stack.isEmpty() || !(name = AutoBuyManager.strip(stack.getName().getString()).toLowerCase()).contains("подтверд") && !name.equals("да") && !name.contains("[Купить]") && !name.contains("confirm") && !name.contains("accept")) continue;
                AutoBuyManager.mc.interactionManager.clickSlot(handler.syncId, i2, 0, SlotActionType.PICKUP, (PlayerEntity)AutoBuyManager.mc.player);
                AutoBuyManager.chat("§a[AutoBuy] §fНажато 'Купить'!");
                delay = 8;
                return;
            }
        }
        AutoBuyManager.chat("§a[AutoBuy] §fУспешно куплено за §a$" + AutoBuyManager.fmt(clickedPrice) + "!");
        AutoBuyManager.stop("Статус: куплено за $" + AutoBuyManager.fmt(clickedPrice));
    }

    private static boolean matches(ItemStack stack) {
        if (stack.isEmpty() || target == null) {
            return false;
        }
        if (stack.getItem() == target) {
            return true;
        }
        String name = AutoBuyManager.strip(stack.getName().getString()).toLowerCase();
        return !targetName.isEmpty() && name.contains(targetName.toLowerCase());
    }

    private static long readPrice(ItemStack stack) {
        Text custom;
        LoreComponent lore = (LoreComponent)stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            long parsed;
            List<Text> lines = lore.lines();
            for (Text line : lines) {
                parsed = AutoBuyManager.parseLine(line.getString(), PRICE_PATTERN);
                if (parsed <= 0L) continue;
                return parsed;
            }
            for (Text line : lines) {
                parsed = AutoBuyManager.parseLine(line.getString(), MONEY_PATTERN);
                if (parsed <= 0L) continue;
                return parsed;
            }
        }
        if ((custom = (Text)stack.get(DataComponentTypes.CUSTOM_NAME)) != null) {
            long parsed = AutoBuyManager.parseLine(custom.getString(), PRICE_PATTERN);
            if (parsed > 0L) {
                return parsed;
            }
            return AutoBuyManager.parseLine(custom.getString(), MONEY_PATTERN);
        }
        return -1L;
    }

    private static long parseLine(String raw, Pattern pattern) {
        String clean = AutoBuyManager.strip(raw);
        Matcher matcher = pattern.matcher(clean);
        if (!matcher.find()) {
            return -1L;
        }
        String matchedGroup = matcher.group(1) != null ? matcher.group(1) : matcher.group(0);
        String digits = matchedGroup.replaceAll("[^0-9]", "");
        if (digits.isEmpty() || digits.length() > 18) {
            return -1L;
        }
        try {
            return Long.parseLong(digits);
        }
        catch (NumberFormatException e2) {
            return -1L;
        }
    }

    private static String strip(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("§.", "").replace(' ', ' ').replace(' ', ' ').trim();
    }

    private static String fmt(long value) {
        if (value == Long.MAX_VALUE) {
            return "∞";
        }
        return String.format("%,d", value).replace(' ', ',').replace(' ', ',');
    }

    private static void chat(String message) {
        if (AutoBuyManager.mc.player != null) {
            AutoBuyManager.mc.player.sendMessage(Text.of((String)message), false);
        }
    }

    static {
        targetName = "";
        stage = Stage.SEND;
        currentPage = 0;
        clickedSlot = -1;
        lastMessage = "";
    }

    private static enum Stage {
        SEND,
        WAIT_GUI,
        SCAN,
        NEXT_PAGE,
        AFTER_CLICK;

    }
}

