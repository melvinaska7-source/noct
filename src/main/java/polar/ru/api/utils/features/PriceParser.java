package polar.ru.api.utils.features;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class PriceParser {
    private static final Pattern NUM_PATTERN = Pattern.compile("(\\d{1,3}(?:[\\s,._]\\d{3})+|\\d+)");

    public int getPrice(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return -1;
        }
        LoreComponent lore = (LoreComponent)stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            return -1;
        }
        int count = Math.max(1, stack.getCount());
        int best = -1;
        for (Text line : lore.lines()) {
            int price = this.findTotalPriceInText(this.stripFormatting(line.getString()), count);
            if (price < 0 || best >= 0 && price >= best) continue;
            best = price;
        }
        return best;
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
}

