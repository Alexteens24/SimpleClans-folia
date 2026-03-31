package net.sacredlabyrinth.phaed.simpleclans.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

public final class LegacyColor {

    public static final char COLOR_CHAR = '\u00A7';
    public static final String ALL_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

    public static final String BLACK = code('0');
    public static final String DARK_BLUE = code('1');
    public static final String DARK_GREEN = code('2');
    public static final String DARK_AQUA = code('3');
    public static final String DARK_RED = code('4');
    public static final String DARK_PURPLE = code('5');
    public static final String GOLD = code('6');
    public static final String GRAY = code('7');
    public static final String DARK_GRAY = code('8');
    public static final String BLUE = code('9');
    public static final String GREEN = code('a');
    public static final String AQUA = code('b');
    public static final String RED = code('c');
    public static final String LIGHT_PURPLE = code('d');
    public static final String YELLOW = code('e');
    public static final String WHITE = code('f');
    public static final String MAGIC = code('k');
    public static final String BOLD = code('l');
    public static final String STRIKETHROUGH = code('m');
    public static final String UNDERLINE = code('n');
    public static final String ITALIC = code('o');
    public static final String RESET = code('r');

    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + COLOR_CHAR + "[0-9A-FK-OR]");
    private static final Pattern STRIP_HEX_PATTERN = Pattern.compile("(?i)" + COLOR_CHAR + "x(" + COLOR_CHAR + "[0-9A-F]){6}");

    private LegacyColor() {
    }

    public static @NotNull String getByChar(char character) {
        char lower = Character.toLowerCase(character);
        return ALL_CODES.indexOf(lower) >= 0 ? code(lower) : Character.toString(character);
    }

    public static @NotNull String translateAlternateColorCodes(char altColorChar, @NotNull String text) {
        char[] chars = text.toCharArray();
        for (int index = 0; index < chars.length - 1; index++) {
            if (chars[index] == altColorChar && ALL_CODES.indexOf(chars[index + 1]) >= 0) {
                chars[index] = COLOR_CHAR;
                chars[index + 1] = Character.toLowerCase(chars[index + 1]);
            }
        }
        return new String(chars);
    }

    public static @Nullable String stripColor(@Nullable String input) {
        if (input == null) {
            return null;
        }
        return STRIP_COLOR_PATTERN.matcher(STRIP_HEX_PATTERN.matcher(input).replaceAll("")).replaceAll("");
    }

    public static @NotNull String hexToColor(@NotNull String hex) {
        String normalized = hex.startsWith("#") ? hex.substring(1) : hex;
        if (normalized.length() != 6) {
            throw new IllegalArgumentException("Hex colors must have 6 characters");
        }

        StringBuilder builder = new StringBuilder(14);
        builder.append(COLOR_CHAR).append('x');
        for (char character : normalized.toLowerCase(Locale.ROOT).toCharArray()) {
            builder.append(COLOR_CHAR).append(character);
        }
        return builder.toString();
    }

    private static @NotNull String code(char code) {
        return String.valueOf(COLOR_CHAR) + code;
    }
}