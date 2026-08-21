package zov.alphadlc.util.replace;


import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class ReplaceUtil {

    public static Text replace(Text input, String target, String replacement) {
        if (input == null || target == null || replacement == null) return input;
        MutableText result = Text.empty().setStyle(input.getStyle());
        appendReplaced(result, input, target, replacement);
        return result;
    }

    private static void appendReplaced(MutableText result, Text current, String target, String replacement) {
        TextContent content = current.getContent();
        Style style = current.getStyle();

        if (content instanceof PlainTextContent.Literal literal) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(target), java.util.regex.Pattern.CASE_INSENSITIVE);
            String replaced = pattern.matcher(literal.string()).replaceAll(replacement);
            result.append(Text.literal(replaced).setStyle(style));
        } else if (!(content instanceof PlainTextContent.Literal)) {
            String str = content.toString();
            if (!str.isEmpty()) {
                result.append(MutableText.of(content).setStyle(style));
            }
        }

        for (Text sibling : current.getSiblings()) {
            appendReplaced(result, sibling, target, replacement);
        }
    }

    public static Text replaceWithFormatted(Text input, String target, Formatting color, String label) {
        if (input == null || target == null) return input;
        MutableText result = Text.empty().setStyle(input.getStyle());
        appendReplacedFormatted(result, input, target, color, label);
        return result;
    }

    private static void appendReplacedFormatted(MutableText result, Text current, String target, Formatting color, String label) {
        TextContent content = current.getContent();
        Style style = current.getStyle();

        if (content instanceof PlainTextContent.Literal literal) {
            String s = literal.string();
            String[] parts = s.split(java.util.regex.Pattern.quote(target), -1);
            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    result.append(Text.literal(parts[i]).setStyle(style));
                }
                if (i < parts.length - 1) {
                    result.append(buildGradientLabel(label, color));
                }
            }
        } else if (content != null && !(content instanceof PlainTextContent.Literal)) {
            String str = content.toString();
            if (!str.isEmpty()) {
                result.append(MutableText.of(content).setStyle(style));
            }
        }

        for (Text sibling : current.getSiblings()) {
            appendReplacedFormatted(result, sibling, target, color, label);
        }
    }

    private static MutableText buildGradientLabel(String label, Formatting color) {
        int targetRgb = formattingToRgb(color);
        int tr = (targetRgb >> 16) & 0xFF;
        int tg = (targetRgb >> 8) & 0xFF;
        int tb = targetRgb & 0xFF;

        int er = (int)(tr * 0.60);
        int eg = (int)(tg * 0.60);
        int eb = (int)(tb * 0.60);

        MutableText result = Text.empty();
        int len = label.length();
        for (int i = 0; i < len; i++) {
            float t = len == 1 ? 1f : (float) i / (len - 1);
            int r = tr + (int)((er - tr) * t);
            int g = tg + (int)((eg - tg) * t);
            int b = tb + (int)((eb - tb) * t);
            int rgb = (r << 16) | (g << 8) | b;
            result.append(Text.literal(String.valueOf(label.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(net.minecraft.util.math.ColorHelper.fullAlpha(rgb))));
        }
        return result;
    }

    private static int formattingToRgb(Formatting formatting) {
        return switch (formatting) {
            case BLACK -> 0x000000;
            case DARK_BLUE -> 0x0000AA;
            case DARK_GREEN -> 0x00AA00;
            case DARK_AQUA -> 0x00AAAA;
            case DARK_RED -> 0xAA0000;
            case DARK_PURPLE -> 0xAA00AA;
            case GOLD -> 0xFFAA00;
            case GRAY -> 0xAAAAAA;
            case DARK_GRAY -> 0x555555;
            case BLUE -> 0x5555FF;
            case GREEN -> 0x55FF55;
            case AQUA -> 0x55FFFF;
            case RED -> 0xFF5555;
            case LIGHT_PURPLE -> 0xFF55FF;
            case YELLOW -> 0xFFFF55;
            case WHITE -> 0xFFFFFF;
            default -> 0xFFFFFF;
        };
    }

    public static Text replaceLiteral(Text input, String target, String replacement) {
        if (input == null) return null;

        String full = input.getString();

        if (!full.toLowerCase().contains(target.toLowerCase()))
            return input;

        full = full.replaceAll("(?i)" + Pattern.quote(target), replacement);

        MutableText out = Text.empty();

        List<StyledChar> styledChars = flatten(input);
        int index = 0;

        for (int i = 0; i < full.length(); i++) {
            Style style = index < styledChars.size()
                    ? styledChars.get(index).style
                    : Style.EMPTY;

            out.append(Text.literal(String.valueOf(full.charAt(i))).setStyle(style));
            index++;
        }

        return out;
    }

    private static List<StyledChar> flatten(Text text) {
        List<StyledChar> list = new ArrayList<>();
        collect(text, list);
        return list;
    }

    private static void collect(Text text, List<StyledChar> list) {
        Style style = text.getStyle();

        if (text.getContent() instanceof PlainTextContent.Literal literal) {
            String s = literal.string();
            for (int i = 0; i < s.length(); i++) {
                list.add(new StyledChar(s.charAt(i), style));
            }
        }

        for (Text sibling : text.getSiblings()) {
            collect(sibling, list);
        }
    }

    private record StyledChar(char c, Style style) {}

    public static Text trimText(Text text, int length) {
        List<StyledChar> chars = flatten(text);
        MutableText result = Text.empty();
        for (int i = 0; i < Math.min(length, chars.size()); i++) {
            result.append(Text.literal(String.valueOf(chars.get(i).c())).setStyle(chars.get(i).style()));
        }
        return result;
    }

    public static String replaceSymbols(String string) {
        return string
                .replaceAll("ꔗ", Formatting.BLUE + "MODER")
                .replaceAll("ꔥ", Formatting.BLUE + "ST.MODER")
                .replaceAll("ꔡ", Formatting.LIGHT_PURPLE + "MODER+")
                .replaceAll("ꔀ", Formatting.GRAY + "ИГРОК")
                .replaceAll("ꔉ", Formatting.YELLOW + "HELPER")
                .replaceAll("◆", "@")
                .replaceAll("┃", "|")
                .replaceAll("ꔳ", Formatting.AQUA + "ML.ADMIN")
                .replaceAll("ꔅ", Formatting.RED + "Y" + Formatting.WHITE + "T")
                .replaceAll("ꔂ", Formatting.GOLD + "GOD")
                .replaceAll("ꔸ", Formatting.GOLD + "GOD")
                .replaceAll("ꕠ", Formatting.YELLOW + "D.HELPER")
                .replaceAll("ꕄ", Formatting.RED + "VAMPIRE")
                .replaceAll("ꔖ", Formatting.AQUA + "OVERLORD")
                .replaceAll("ꕈ", Formatting.GREEN + "COBRA")
                .replaceAll("ꔨ", Formatting.LIGHT_PURPLE + "DRAGON")
                .replaceAll("ꔤ", Formatting.RED + "IMPERATOR")
                .replaceAll("ꔠ", Formatting.GOLD + "MAGISTER")
                .replaceAll("ꔄ", Formatting.BLUE + "HERO")
                .replaceAll("ꔒ", Formatting.GREEN + "AVENGER")
                .replaceAll("ꕒ", Formatting.WHITE + "RABBIT")
                .replaceAll("ꔈ", Formatting.YELLOW + "TITAN")
                .replaceAll("ꕀ", Formatting.DARK_GREEN + "HYDRA")
                .replaceAll("ꔶ", Formatting.GOLD + "TIGER")
                .replaceAll("ꔲ", Formatting.DARK_PURPLE + "BULL")
                .replaceAll("ꕖ", Formatting.BLACK + "BUNNY")
                .replaceAll("ꕗꕘ", Formatting.YELLOW + "SPONSOR")
                .replaceAll("ꕉ", Formatting.GOLD + "PEGAS")
                .replaceAll("ꕆ", Formatting.GOLD + "PEGAS")
                .replaceAll("ꕓ", Formatting.DARK_GRAY + "GHOST")
                .replaceAll("ꔆ", Formatting.GRAY + "D.MODER")
                .replaceAll("ꔰ", Formatting.GRAY + "D.ML.ADMIN")
                .replaceAll("ꔐ", Formatting.DARK_BLUE + "D.GL.MODER")
                .replaceAll("ꔔ", Formatting.GRAY + "D.GL.MODER")
                .replaceAll("ꔢ", Formatting.GRAY + "D.ST.MODER")
                .replaceAll("ꕡ", Formatting.GOLD + "ST.HELPER")
                .replaceAll("ꕅ", Formatting.DARK_PURPLE + "MEDIA+")
                .replaceAll("ꔓ", Formatting.BLUE + "ML.MODER")
                .replaceAll("ꔩ", Formatting.DARK_PURPLE + "GL.MODER")
                .replaceAll("ꕗ", Formatting.DARK_RED + "D.ADMIN")
                .replaceAll("ꔘ", Formatting.BLUE + "D.ST.MODER")
                .replaceAll("ꔦ", Formatting.BLUE + "D.ML.ADMIN")
                .replaceAll("ꔁ", Formatting.DARK_PURPLE + "MEDIA")
                .replaceAll("\uD83D\uDD25", "@")
                .replaceAll("ᴀ", "A")
                .replaceAll("ʙ", "B")
                .replaceAll("ᴄ", "C")
                .replaceAll("ᴅ", "D")
                .replaceAll("ᴇ", "E")
                .replaceAll("ғ", "F")
                .replaceAll("ɢ", "G")
                .replaceAll("ʜ", "H")
                .replaceAll("ɪ", "I")
                .replaceAll("ᴊ", "J")
                .replaceAll("ᴋ", "K")
                .replaceAll("ʟ", "L")
                .replaceAll("ᴍ", "M")
                .replaceAll("ɴ", "N")
                .replaceAll("ꜱ", "S")
                .replaceAll("ᴏ", "O")
                .replaceAll("ᴘ", "P")
                .replaceAll("ǫ", "Q")
                .replaceAll("ʀ", "R")
                .replaceAll("ᴛ", "T")
                .replaceAll("ᴜ", "U")
                .replaceAll("ᴠ", "V")
                .replaceAll("ᴡ", "W")
                .replaceAll("ꜰ", "F")
                .replaceAll("ʏ", "Y")
                .replaceAll("ᴢ", "Z");
    }

    public static Text replaceSymbols(Text text) {
        if (text.getString().contains("ꔗ")) text = replaceWithFormatted(text, "ꔗ", Formatting.BLUE, "MODER");
        if (text.getString().contains("ꔥ")) text = replaceWithFormatted(text, "ꔥ", Formatting.BLUE, "ST.MODER");
        if (text.getString().contains("ꔡ")) text = replaceWithFormatted(text, "ꔡ", Formatting.LIGHT_PURPLE, "MODER+");
        if (text.getString().contains("ꔀ")) text = replaceWithFormatted(text, "ꔀ", Formatting.GRAY, "ИГРОК");
        if (text.getString().contains("ꔉ")) text = replaceWithFormatted(text, "ꔉ", Formatting.YELLOW, "HELPER");
        if (text.getString().contains("◆")) text = replace(text, "◆", "@");
        if (text.getString().contains("┃")) text = replace(text, "┃", "|");
        if (text.getString().contains("ꔳ")) text = replaceWithFormatted(text, "ꔳ", Formatting.AQUA, "ML.ADMIN");
        if (text.getString().contains("ꔅ")) text = replaceWithFormatted(text, "ꔅ", Formatting.RED, "YT");
        if (text.getString().contains("ꔂ")) text = replaceWithFormatted(text, "ꔂ", Formatting.GOLD, "GOD");
        if (text.getString().contains("ꔸ")) text = replaceWithFormatted(text, "ꔸ", Formatting.GOLD, "GOD");
        if (text.getString().contains("ꕠ")) text = replaceWithFormatted(text, "ꕠ", Formatting.YELLOW, "D.HELPER");
        if (text.getString().contains("ꕄ")) text = replaceWithFormatted(text, "ꕄ", Formatting.RED, "VAMPIRE");
        if (text.getString().contains("ꔖ")) text = replaceWithFormatted(text, "ꔖ", Formatting.AQUA, "OVERLORD");
        if (text.getString().contains("ꕈ")) text = replaceWithFormatted(text, "ꕈ", Formatting.GREEN, "COBRA");
        if (text.getString().contains("ꔨ")) text = replaceWithFormatted(text, "ꔨ", Formatting.LIGHT_PURPLE, "DRAGON");
        if (text.getString().contains("ꔤ")) text = replaceWithFormatted(text, "ꔤ", Formatting.RED, "IMPERATOR");
        if (text.getString().contains("ꔠ")) text = replaceWithFormatted(text, "ꔠ", Formatting.GOLD, "MAGISTER");
        if (text.getString().contains("ꔄ")) text = replaceWithFormatted(text, "ꔄ", Formatting.BLUE, "HERO");
        if (text.getString().contains("ꔒ")) text = replaceWithFormatted(text, "ꔒ", Formatting.GREEN, "AVENGER");
        if (text.getString().contains("ꕒ")) text = replaceWithFormatted(text, "ꕒ", Formatting.WHITE, "RABBIT");
        if (text.getString().contains("ꔈ")) text = replaceWithFormatted(text, "ꔈ", Formatting.YELLOW, "TITAN");
        if (text.getString().contains("ꕀ")) text = replaceWithFormatted(text, "ꕀ", Formatting.DARK_GREEN, "HYDRA");
        if (text.getString().contains("ꔶ")) text = replaceWithFormatted(text, "ꔶ", Formatting.GOLD, "TIGER");
        if (text.getString().contains("ꔲ")) text = replaceWithFormatted(text, "ꔲ", Formatting.DARK_PURPLE, "BULL");
        if (text.getString().contains("ꕖ")) text = replaceWithFormatted(text, "ꕖ", Formatting.GRAY, "BUNNY");
        if (text.getString().contains("ꕗꕘ")) text = replaceWithFormatted(text, "ꕗꕘ", Formatting.YELLOW, "SPONSOR");
        if (text.getString().contains("ꕉ")) text = replaceWithFormatted(text, "ꕉ", Formatting.GOLD, "PEGAS");
        if (text.getString().contains("ꕆ")) text = replaceWithFormatted(text, "ꕆ", Formatting.GOLD, "PEGAS");
        if (text.getString().contains("ꕓ")) text = replaceWithFormatted(text, "ꕓ", Formatting.DARK_GRAY, "GHOST");
        if (text.getString().contains("ꔆ")) text = replaceWithFormatted(text, "ꔆ", Formatting.GRAY, "D.MODER");
        if (text.getString().contains("ꔰ")) text = replaceWithFormatted(text, "ꔰ", Formatting.GRAY, "D.ML.ADMIN");
        if (text.getString().contains("ꔐ")) text = replaceWithFormatted(text, "ꔐ", Formatting.DARK_BLUE, "D.GL.MODER");
        if (text.getString().contains("ꔔ")) text = replaceWithFormatted(text, "ꔔ", Formatting.GRAY, "D.GL.MODER");
        if (text.getString().contains("ꔢ")) text = replaceWithFormatted(text, "ꔢ", Formatting.GRAY, "D.ST.MODER");
        if (text.getString().contains("ꕡ")) text = replaceWithFormatted(text, "ꕡ", Formatting.GOLD, "ST.HELPER");
        if (text.getString().contains("ꕅ")) text = replaceWithFormatted(text, "ꕅ", Formatting.DARK_PURPLE, "MEDIA+");
        if (text.getString().contains("ꔓ")) text = replaceWithFormatted(text, "ꔓ", Formatting.BLUE, "ML.MODER");
        if (text.getString().contains("ꔩ")) text = replaceWithFormatted(text, "ꔩ", Formatting.DARK_PURPLE, "GL.MODER");
        if (text.getString().contains("ꕗ")) text = replaceWithFormatted(text, "ꕗ", Formatting.DARK_RED, "D.ADMIN");
        if (text.getString().contains("ꔘ")) text = replaceWithFormatted(text, "ꔘ", Formatting.BLUE, "D.ST.MODER");
        if (text.getString().contains("ꔦ")) text = replaceWithFormatted(text, "ꔦ", Formatting.BLUE, "D.ML.ADMIN");
        if (text.getString().contains("ꔁ")) text = replaceWithFormatted(text, "ꔁ", Formatting.DARK_PURPLE, "MEDIA");
        if (text.getString().contains("\uD83D\uDD25")) text = replace(text, "\uD83D\uDD25", "@");

        if (text.getString().contains("ᴀ")) text = replace(text, "ᴀ", "A");
        if (text.getString().contains("ʙ")) text = replace(text, "ʙ", "B");
        if (text.getString().contains("ᴄ")) text = replace(text, "ᴄ", "C");
        if (text.getString().contains("ᴅ")) text = replace(text, "ᴅ", "D");
        if (text.getString().contains("ᴇ")) text = replace(text, "ᴇ", "E");
        if (text.getString().contains("ғ")) text = replace(text, "ғ", "F");
        if (text.getString().contains("ɢ")) text = replace(text, "ɢ", "G");
        if (text.getString().contains("ʜ")) text = replace(text, "ʜ", "H");
        if (text.getString().contains("ɪ")) text = replace(text, "ɪ", "I");
        if (text.getString().contains("ᴊ")) text = replace(text, "ᴊ", "J");
        if (text.getString().contains("ᴋ")) text = replace(text, "ᴋ", "K");
        if (text.getString().contains("ʟ")) text = replace(text, "ʟ", "L");
        if (text.getString().contains("ᴍ")) text = replace(text, "ᴍ", "M");
        if (text.getString().contains("ɴ")) text = replace(text, "ɴ", "N");
        if (text.getString().contains("ꜱ")) text = replace(text, "ꜱ", "S");
        if (text.getString().contains("ᴏ")) text = replace(text, "ᴏ", "O");
        if (text.getString().contains("ᴘ")) text = replace(text, "ᴘ", "P");
        if (text.getString().contains("ǫ")) text = replace(text, "ǫ", "Q");
        if (text.getString().contains("ʀ")) text = replace(text, "ʀ", "R");
        if (text.getString().contains("ᴛ")) text = replace(text, "ᴛ", "T");
        if (text.getString().contains("ᴜ")) text = replace(text, "ᴜ", "U");
        if (text.getString().contains("ᴠ")) text = replace(text, "ᴠ", "V");
        if (text.getString().contains("ᴡ")) text = replace(text, "ᴡ", "W");
        if (text.getString().contains("ꜰ")) text = replace(text, "ꜰ", "F");
        if (text.getString().contains("ʏ")) text = replace(text, "ʏ", "Y");
        if (text.getString().contains("ᴢ")) text = replace(text, "ᴢ", "Z");

        return text;
    }

    public static String toQwerty(String text) {
        return text
                .replace("й", "q")
                .replace("ц", "w")
                .replace("у", "e")
                .replace("к", "r")
                .replace("е", "t")
                .replace("н", "y")
                .replace("г", "u")
                .replace("ш", "i")
                .replace("щ", "o")
                .replace("з", "p")
                .replace("х", "[")
                .replace("ъ", "]")
                .replace("ф", "a")
                .replace("ы", "s")
                .replace("в", "d")
                .replace("а", "f")
                .replace("п", "g")
                .replace("р", "h")
                .replace("о", "j")
                .replace("л", "k")
                .replace("д", "l")
                .replace("ж", ";")
                .replace("э", "'")
                .replace("я", "z")
                .replace("ч", "x")
                .replace("с", "c")
                .replace("м", "v")
                .replace("и", "b")
                .replace("т", "n")
                .replace("ь", "m")
                .replace("б", ",")
                .replace("ю", ".")
                .replace("ё", "`")
                .replace("Й", "Q")
                .replace("Ц", "W")
                .replace("У", "E")
                .replace("К", "R")
                .replace("Е", "T")
                .replace("Н", "Y")
                .replace("Г", "U")
                .replace("Ш", "I")
                .replace("Щ", "O")
                .replace("З", "P")
                .replace("Х", "{")
                .replace("Ъ", "}")
                .replace("Ф", "A")
                .replace("Ы", "S")
                .replace("В", "D")
                .replace("А", "F")
                .replace("П", "G")
                .replace("Р", "H")
                .replace("О", "J")
                .replace("Л", "K")
                .replace("Д", "L")
                .replace("Ж", ":")
                .replace("Э", "\"")
                .replace("Я", "Z")
                .replace("Ч", "X")
                .replace("С", "C")
                .replace("М", "V")
                .replace("И", "B")
                .replace("Т", "N")
                .replace("Ь", "M")
                .replace("Б", "<")
                .replace("Ю", ">")
                .replace("Ё", "~");
    }
    
    public static String getDonatorRankSymbol(String rank) {
        return switch (rank.toLowerCase()) {
            case "admin" -> "ꔳ";
            case "moder" -> "ꔗ";
            case "yt" -> "ꔅ";
            case "pegas" -> "ꕉ";
            case "helper" -> "ꔉ";
            case "media" -> "ꔁ";
            case "god" -> "ꔂ";
            default -> "ꔀ";
        };
    }
}

