package polar.ru.client.modules.impl.misc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventPacket;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.features.PriceParser;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.script.DelayScript;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class AhHelper
extends Module {
    private static final long PULSE_MS = 900L;
    private static final float PULSE_MIN_ALPHA = 0.35f;
    private static final float PULSE_MAX_ALPHA = 1.0f;
    private static final long RECALC_MIN_MS = 90L;
    private static final long RECALC_IDLE_MS = 650L;
    private static final int IGNORE_CONTROL_ROW_Y = 104;
    private static final int COLOR_GREEN = -11796661;
    private static final int COLOR_RED = -46261;
    private static final Pattern NUM_PATTERN = Pattern.compile("(\\d{1,3}(?:[\\s,._]\\d{3})+|\\d+)");
    private static boolean tooltipInit;
    private static Method mGetTooltip;
    private static TooltipArg[] tooltipArgs;
    private static Field guiLeftField;
    private static Field guiTopField;
    private static boolean screenFieldsInit;
    public static AhHelper INSTANCE;
    private final PriceParser priceParser = new PriceParser();
    private final DelayScript script = new DelayScript();
    private Slot cheapestSlot;
    private Slot costEffectiveSlot;
    private int lastSyncId = -1;
    private long lastRecalcMs;
    private boolean dirty;
    private boolean recalcQueued;
    private final ModeSetting cheapestItemColorSetting = new ModeSetting("Самый дешевый предмет", "Зелёный", "Зелёный", "Красный");
    private final ModeSetting costEffectiveItemColorSetting = new ModeSetting("Экономичный предмет", "Красный", "Зелёный", "Красный");

    public AhHelper() {
        super("AhHelper", "Подсветка самых выгодных лотов на аукционе", Module.ModuleCategory.RENDER);
        this.addSettings(this.cheapestItemColorSetting, this.costEffectiveItemColorSetting);
        this.initScreenReflection();
    }

    @EventLink
    public void onPacket(EventPacket event) {
        if (event.getType() != EventPacket.Type.RECEIVE) {
            return;
        }
        if (event.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket) {
            Screen var_437_2 = AhHelper.mc.currentScreen;
            if (!(var_437_2 instanceof GenericContainerScreen)) {
                return;
            }
            GenericContainerScreen screen = (GenericContainerScreen)var_437_2;
            if (!this.isAuctionScreen(screen)) {
                return;
            }
            this.dirty = true;
            if (!this.recalcQueued) {
                this.recalcQueued = true;
                this.script.cleanup().addTickStep(0, () -> {
                    GenericContainerScreen s2;
                    this.recalcQueued = false;
                    Screen patt0$temp = AhHelper.mc.currentScreen;
                    if (patt0$temp instanceof GenericContainerScreen && this.isAuctionScreen(s2 = (GenericContainerScreen)patt0$temp)) {
                        this.recalc(s2);
                    }
                });
            }
        }
    }

    @EventLink
    public void onTick(EventUpdate event) {
        this.script.update(event);
        Screen var_437_2 = AhHelper.mc.currentScreen;
        if (!(var_437_2 instanceof GenericContainerScreen)) {
            this.resetCalcState();
            return;
        }
        GenericContainerScreen screen = (GenericContainerScreen)var_437_2;
        if (!this.isAuctionScreen(screen)) {
            this.resetCalcState();
            return;
        }
        long now = System.currentTimeMillis();
        if (!this.dirty && now - this.lastRecalcMs >= 650L) {
            this.recalc(screen);
        }
    }

    public void renderFromMixin(DrawContext context, int mouseX, int mouseY) {
        GenericContainerScreen screen;
        if (!this.isEnable()) {
            return;
        }
        Screen var_437_2 = AhHelper.mc.currentScreen;
        if (!(var_437_2 instanceof GenericContainerScreen) || !this.isAuctionScreen(screen = (GenericContainerScreen)var_437_2)) {
            this.resetCalcState();
            return;
        }
        long now = System.currentTimeMillis();
        this.ensureCalculated(screen, now);
        int guiLeft = this.getGuiLeft((HandledScreen<?>)screen);
        int guiTop = this.getGuiTop((HandledScreen<?>)screen);
        int cheapColor = this.pulsing(this.colorOf(this.cheapestItemColorSetting));
        int effColor = this.pulsing(this.colorOf(this.costEffectiveItemColorSetting));
        if (this.cheapestSlot != null) {
            this.highlightSlot(context, guiLeft, guiTop, this.cheapestSlot, cheapColor);
        }
        if (this.costEffectiveSlot != null) {
            this.highlightSlot(context, guiLeft, guiTop, this.costEffectiveSlot, effColor);
        }
    }

    private void ensureCalculated(GenericContainerScreen screen, long now) {
        int syncId = ((GenericContainerScreenHandler)screen.getScreenHandler()).syncId;
        if (syncId != this.lastSyncId) {
            this.lastSyncId = syncId;
            this.dirty = true;
            this.recalcQueued = false;
            this.cheapestSlot = null;
            this.costEffectiveSlot = null;
        }
        if (this.dirty && now - this.lastRecalcMs >= 90L) {
            this.recalc(screen);
        }
    }

    private void recalc(GenericContainerScreen screen) {
        if (AhHelper.mc.player == null) {
            this.resetCalcState();
            return;
        }
        long now = System.currentTimeMillis();
        DefaultedList slots = ((GenericContainerScreenHandler)screen.getScreenHandler()).slots;
        int n2 = slots.size();
        int[] prices = new int[n2];
        int[] counts = new int[n2];
        Slot bestCheap = null;
        int bestCheapPrice = Integer.MAX_VALUE;
        for (int i2 = 0; i2 < n2; ++i2) {
            int price;
            Slot slot = (Slot)slots.get(i2);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                prices[i2] = -1;
                counts[i2] = 0;
                continue;
            }
            if (slot.inventory == AhHelper.mc.player.getInventory()) {
                prices[i2] = -1;
                counts[i2] = 0;
                continue;
            }
            if (slot.y >= 104) {
                prices[i2] = -1;
                counts[i2] = 0;
                continue;
            }
            prices[i2] = price = this.getTotalPrice(stack);
            counts[i2] = Math.max(1, stack.getCount());
            if (price < 0 || price >= bestCheapPrice) continue;
            bestCheapPrice = price;
            bestCheap = slot;
        }
        Slot bestEff = null;
        double bestEffPpi = Double.POSITIVE_INFINITY;
        int bestEffTotal = Integer.MAX_VALUE;
        for (int i3 = 0; i3 < n2; ++i3) {
            boolean equal;
            Slot slot;
            int price = prices[i3];
            if (price < 0 || (slot = (Slot)slots.get(i3)) == bestCheap) continue;
            int count = Math.max(1, counts[i3]);
            double ppi = (double)price / (double)count;
            boolean better = ppi < bestEffPpi - 1.0E-9;
            boolean bl = equal = Math.abs(ppi - bestEffPpi) <= 1.0E-9;
            if (!better && (!equal || price >= bestEffTotal)) continue;
            bestEffPpi = ppi;
            bestEffTotal = price;
            bestEff = slot;
        }
        this.cheapestSlot = bestCheap;
        this.costEffectiveSlot = bestEff;
        this.dirty = false;
        this.lastRecalcMs = now;
    }

    private int getTotalPrice(ItemStack stack) {
        int price = -1;
        try {
            price = this.priceParser.getPrice(stack);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        if (price >= 0) {
            return price;
        }
        return this.extractTotalPriceFallback(stack);
    }

    private int extractTotalPriceFallback(ItemStack stack) {
        try {
            int count = Math.max(1, stack.getCount());
            List<String> lines = this.collectTooltipLines(stack);
            String blob = String.join((CharSequence)" ", lines);
            return this.findTotalPriceInText(blob, count);
        }
        catch (Throwable ignored) {
            return -1;
        }
    }

    private int findTotalPriceInText(String s2, int count) {
        if (s2 == null || s2.isEmpty()) {
            return -1;
        }
        String lower = s2.toLowerCase();
        Matcher m2 = NUM_PATTERN.matcher(s2);
        int bestScore = -1;
        long best = -1L;
        while (m2.find()) {
            long totalVal;
            int ce;
            int cs;
            String ctx;
            int start = m2.start(1);
            int end = m2.end(1);
            long val = this.parseDigitsToLong(m2.group(1));
            if (val <= 0L) continue;
            long mul = this.readSuffixMultiplier(lower, end);
            if (mul != 1L) {
                val *= mul;
            }
            if (!this.hasPriceKeyword(ctx = lower.substring(cs = Math.max(0, start - 52), ce = Math.min(lower.length(), end + 52)))) continue;
            boolean per = ctx.contains("за шт") || ctx.contains("/шт") || ctx.contains("шт.") || ctx.contains(" per ") || ctx.contains(" each ") || ctx.contains("за 1") || ctx.contains("за шту");
            boolean total = ctx.contains("всего") || ctx.contains("итого") || ctx.contains("total") || ctx.contains("сумм") || ctx.contains("общ");
            int score = 3;
            if (total) {
                score += 3;
            }
            if (per) {
                ++score;
            }
            if ((totalVal = per ? val * (long)count : val) <= 0L || score <= bestScore && (score != bestScore || totalVal <= best)) continue;
            bestScore = score;
            best = totalVal;
        }
        if (best <= 0L) {
            return -1;
        }
        if (best > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int)best;
    }

    private boolean hasPriceKeyword(String ctx) {
        return ctx.contains("цена") || ctx.contains("price") || ctx.contains("стоим") || ctx.contains("руб") || ctx.contains("монет") || ctx.contains("coins") || ctx.contains("коин") || ctx.contains("buy") || ctx.contains("куп") || ctx.contains("$") || ctx.contains("₽");
    }

    private long readSuffixMultiplier(String lower, int end) {
        char c1;
        if (end >= lower.length()) {
            return 1L;
        }
        char c0 = lower.charAt(end);
        char c2 = c1 = end + 1 < lower.length() ? lower.charAt(end + 1) : (char)'\u0000';
        if (c0 == 'k' || c0 == 'к') {
            return c1 == 'k' || c1 == 'к' ? 1000000L : 1000L;
        }
        if (c0 == 'm' || c0 == 'м') {
            return 1000000L;
        }
        return 1L;
    }

    private long parseDigitsToLong(String raw) {
        long v2 = 0L;
        for (int i2 = 0; i2 < raw.length(); ++i2) {
            char c2 = raw.charAt(i2);
            if (c2 < '0' || c2 > '9') continue;
            v2 = v2 * 10L + (long)(c2 - 48);
        }
        return v2;
    }

    private List<String> collectTooltipLines(ItemStack stack) {
        ArrayList<String> api = this.tryCollectTooltipApi(stack);
        if (api != null && !api.isEmpty()) {
            for (int i2 = 0; i2 < api.size(); ++i2) {
                api.set(i2, this.stripFormatting(api.get(i2)));
            }
            return api;
        }
        ArrayList<String> out = new ArrayList<String>(8);
        out.add(this.safeName(stack));
        for (int i3 = 0; i3 < out.size(); ++i3) {
            out.set(i3, this.stripFormatting(out.get(i3)));
        }
        return out;
    }

    private ArrayList<String> tryCollectTooltipApi(ItemStack stack) {
        try {
            if (!tooltipInit) {
                this.initTooltipApi(stack);
            }
            if (mGetTooltip == null || tooltipArgs == null) {
                return null;
            }
            Object[] args = new Object[tooltipArgs.length];
            for (int i2 = 0; i2 < tooltipArgs.length; ++i2) {
                args[i2] = tooltipArgs[i2].value(AhHelper.mc.player);
            }
            Object res = mGetTooltip.invoke(stack, args);
            if (!(res instanceof List)) {
                return null;
            }
            List list = (List)res;
            ArrayList<String> out = new ArrayList<String>(list.size());
            for (Object o2 : list) {
                if (o2 instanceof Text) {
                    Text t2 = (Text)o2;
                    out.add(t2.getString());
                    continue;
                }
                if (o2 == null) continue;
                out.add(String.valueOf(o2));
            }
            return out;
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private void initTooltipApi(ItemStack stack) {
        tooltipInit = true;
        if (stack == null) {
            return;
        }
        for (Method m2 : stack.getClass().getMethods()) {
            Class<?>[] pts;
            TooltipArg[] plan;
            if (!"getTooltip".equals(m2.getName()) || !List.class.isAssignableFrom(m2.getReturnType()) || (plan = this.buildTooltipPlan(pts = m2.getParameterTypes())) == null) continue;
            try {
                Object[] args = new Object[plan.length];
                for (int i2 = 0; i2 < plan.length; ++i2) {
                    args[i2] = plan[i2].value(AhHelper.mc.player);
                }
                Object res = m2.invoke(stack, args);
                if (!(res instanceof List)) continue;
                mGetTooltip = m2;
                tooltipArgs = plan;
                return;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        mGetTooltip = null;
        tooltipArgs = null;
    }

    private TooltipArg[] buildTooltipPlan(Class<?>[] pts) {
        if (pts == null) {
            return null;
        }
        TooltipArg[] out = new TooltipArg[pts.length];
        for (int i2 = 0; i2 < pts.length; ++i2) {
            Class<?> pt = pts[i2];
            if (pt == null) {
                return null;
            }
            if (AhHelper.mc.player != null && pt.isAssignableFrom(AhHelper.mc.player.getClass())) {
                out[i2] = TooltipArg.player();
                continue;
            }
            if (pt == Boolean.TYPE || pt == Boolean.class) {
                out[i2] = TooltipArg.boolFalse();
                continue;
            }
            if (pt == Integer.TYPE || pt == Integer.class) {
                out[i2] = TooltipArg.intZero();
                continue;
            }
            if (pt.isEnum()) {
                Object pick = this.pickEnum(pt, "NORMAL", "DEFAULT", "BASIC", "REGULAR");
                out[i2] = TooltipArg.fixed(pick);
                continue;
            }
            Object st = this.pickStatic(pt, "DEFAULT", "NORMAL", "BASIC", "REGULAR", "STANDARD");
            out[i2] = st != null ? TooltipArg.fixed(st) : (pt.isInterface() ? TooltipArg.proxy(pt) : TooltipArg.fixed(null));
        }
        return out;
    }

    private Object pickStatic(Class<?> type, String ... names) {
        try {
            for (String n2 : names) {
                try {
                    Field f2 = type.getField(n2);
                    if (!type.isAssignableFrom(f2.getType())) continue;
                    return f2.get(null);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private Object pickEnum(Class<?> enumType, String ... prefer) {
        try {
            Object[] cs = enumType.getEnumConstants();
            if (cs == null || cs.length == 0) {
                return null;
            }
            for (String p2 : prefer) {
                if (p2 == null) continue;
                for (Object c2 : cs) {
                    if (c2 == null || !p2.equalsIgnoreCase(String.valueOf(c2))) continue;
                    return c2;
                }
            }
            return cs[0];
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private String safeName(ItemStack stack) {
        try {
            return stack.getName().getString();
        }
        catch (Throwable ignored) {
            return "";
        }
    }

    private int pulsing(int color) {
        long now = System.currentTimeMillis();
        float t2 = (float)(now % 900L) / 900.0f;
        float wave = 0.5f - 0.5f * MathHelper.cos((float)(t2 * ((float)Math.PI * 2)));
        float alpha = MathHelper.clamp((float)(0.35f + 0.65f * wave), (float)0.0f, (float)1.0f);
        return ColorUtils.multAlpha(color, alpha);
    }

    private void highlightSlot(DrawContext context, int guiLeft, int guiTop, Slot slot, int color) {
        int x2 = guiLeft + slot.x;
        int y2 = guiTop + slot.y;
        RenderUtils.drawRoundedRect(context.getMatrices(), x2, y2, 16.0f, 16.0f, 0.0f, color);
    }

    private int colorOf(ModeSetting setting) {
        return setting.is("Красный") ? -46261 : -11796661;
    }

    private boolean isAuctionScreen(GenericContainerScreen screen) {
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        return title.contains("Аукцион") || title.contains("Аукционы") || title.contains("Поиск");
    }

    private void resetCalcState() {
        this.cheapestSlot = null;
        this.costEffectiveSlot = null;
        this.lastSyncId = -1;
        this.lastRecalcMs = 0L;
        this.dirty = false;
        this.recalcQueued = false;
        this.script.cleanup();
    }

    private String stripFormatting(String s2) {
        if (s2 == null || s2.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(s2.length());
        for (int i2 = 0; i2 < s2.length(); ++i2) {
            char c2 = s2.charAt(i2);
            if (c2 == '§') {
                ++i2;
                continue;
            }
            out.append(c2);
        }
        return out.toString();
    }

    private void initScreenReflection() {
        if (screenFieldsInit) {
            return;
        }
        screenFieldsInit = true;
        try {
            for (Field field : HandledScreen.class.getDeclaredFields()) {
                field.setAccessible(true);
                String name = field.getName();
                if (field.getType() != Integer.TYPE) continue;
                if (guiLeftField == null && (name.equals("x") || name.contains("Left") || name.equals("x"))) {
                    guiLeftField = field;
                    continue;
                }
                if (guiTopField != null || !name.equals("y") && !name.contains("Top") && !name.equals("y")) continue;
                guiTopField = field;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private int getGuiLeft(HandledScreen<?> screen) {
        try {
            if (guiLeftField != null) {
                return guiLeftField.getInt(screen);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return (mc.getWindow().getScaledWidth() - 176) / 2;
    }

    private int getGuiTop(HandledScreen<?> screen) {
        try {
            if (guiTopField != null) {
                return guiTopField.getInt(screen);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return (mc.getWindow().getScaledHeight() - 166) / 2;
    }

    static {
        INSTANCE = new AhHelper();
    }

    private static class TooltipArg {
        private final int kind;
        private final Object fixed;
        private final Class<?> iface;

        private TooltipArg(int kind, Object fixed, Class<?> iface) {
            this.kind = kind;
            this.fixed = fixed;
            this.iface = iface;
        }

        static TooltipArg fixed(Object v2) {
            return new TooltipArg(0, v2, null);
        }

        static TooltipArg player() {
            return new TooltipArg(1, null, null);
        }

        static TooltipArg boolFalse() {
            return new TooltipArg(2, null, null);
        }

        static TooltipArg intZero() {
            return new TooltipArg(3, null, null);
        }

        static TooltipArg proxy(Class<?> iface) {
            return new TooltipArg(4, null, iface);
        }

        Object value(Object player) {
            return switch (this.kind) {
                case 1 -> player;
                case 2 -> false;
                case 3 -> 0;
                case 4 -> this.makeProxy(this.iface);
                default -> this.fixed;
            };
        }

        private Object makeProxy(Class<?> iface) {
            try {
                return Proxy.newProxyInstance(iface.getClassLoader(), new Class[]{iface}, (p2, m2, a2) -> {
                    Class<?> rt = m2.getReturnType();
                    if (rt == Boolean.TYPE || rt == Boolean.class) {
                        return false;
                    }
                    if (rt == Integer.TYPE || rt == Integer.class) {
                        return 0;
                    }
                    if (rt == Float.TYPE || rt == Float.class) {
                        return Float.valueOf(0.0f);
                    }
                    if (rt == Double.TYPE || rt == Double.class) {
                        return 0.0;
                    }
                    return null;
                });
            }
            catch (Throwable ignored) {
                return null;
            }
        }
    }
}

